package flare.client.app.singbox

import android.content.Context
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import flare.client.app.data.SettingsManager
import io.nekohasekai.libbox.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

object SingBoxManager {

    private const val TAG = "SingBoxManager"
    private var boxService: CommandServer? = null
    private val mutex = Mutex()
    
    internal var tunPfd: ParcelFileDescriptor? = null
    internal var currentVpnService: VpnService? = null
    internal var networkCallback: ConnectivityManager.NetworkCallback? = null
    internal var lastPermissionError: Boolean = false

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var startTime: Long = 0L
        private set

    @Volatile
    var primaryProxyTag: String = "proxy"
        private set

    @Volatile
    var clashSecret: String = ""
        private set

    private var setupDone = false
    private var logFile: File? = null

    internal fun ensureSetup(context: Context) {
        if (setupDone) return
        try {
            val settings = SettingsManager(context)
            val version =
                    try {
                        Libbox.version()
                    } catch (e: Exception) {
                        "unknown: ${e.message}"
                    }
            Log.i(TAG, "sing-box libbox version: $version")

            val options =
                    SetupOptions().apply {
                        basePath = context.filesDir.absolutePath
                        workingPath = context.filesDir.absolutePath
                        tempPath = context.cacheDir.absolutePath
                        fixAndroidStack = true
                        logMaxLines = 500
                        crashReportSource = "core"
                    }
            Libbox.setup(options)

            val lf = File(context.filesDir, "sing-box.log")
            logFile = lf
            val shouldWriteCoreLogs = settings.isCoreLogEnabled && settings.coreLogLevel != "none"
            try {
                if (shouldWriteCoreLogs) {
                    lf.delete()
                }
            } catch (_: Exception) {}
            
            if (shouldWriteCoreLogs) {
                Log.i(TAG, "sing-box log file: ${lf.absolutePath}")
            }

            setupDone = true
            if (flare.client.app.BuildConfig.DEBUG) Log.i(TAG, "Libbox.setup() done")
        } catch (e: Exception) {
            Log.e(TAG, "Libbox.setup() failed: ${e.message}", e)
        }
    }

    suspend fun start(configContent: String, context: Context): Boolean {
        return mutex.withLock {
            if (isRunning) {
                Log.w(TAG, "sing-box is already running")
                return@withLock true
            }

            ensureSetup(context)
            LocalResolver.init(context)

            currentVpnService = context as? VpnService
            val appContext = context.applicationContext

            try {
                if (boxService == null) {
                    val handler = FlareCommandServerHandler(getVpnContext = { currentVpnService ?: appContext })
                    val platform = FlarePlatformInterface()

                    boxService = Libbox.newCommandServer(handler, platform)

                    try {
                        boxService?.start()
                        if (flare.client.app.BuildConfig.DEBUG) Log.i(TAG, "CommandServer started")
                    } catch (e: Exception) {
                        Log.e(TAG, "CommandServer.start() failed: ${e.message}", e)
                    }
                }

                val patchedConfig = patchConfig(configContent, context)

                Log.i(TAG, "Calling startOrReloadService…")
                lastPermissionError = false
                try {
                    boxService?.startOrReloadService(patchedConfig, OverrideOptions())
                    if (flare.client.app.BuildConfig.DEBUG) Log.i(TAG, "startOrReloadService completed")
                } catch (e: Exception) {
                    if (lastPermissionError) {
                        Log.e(TAG, "startOrReloadService failed due to VPN permission")
                        throw Exception("VPN_PERMISSION_MISSING")
                    }
                    Log.e(TAG, "startOrReloadService failed: ${e.message}", e)
                    throw e
                }

                isRunning = true
                startTime = SystemClock.elapsedRealtime()
                startTrafficStream(context)
                if (flare.client.app.BuildConfig.DEBUG) Log.i(TAG, "sing-box started via AAR")
                true
            } catch (e: Exception) {
                isRunning = false
                startTime = 0L
                stopTrafficStream()
                try {
                    tunPfd?.close()
                } catch (_: Exception) {}
                tunPfd = null

                if (e.message == "VPN_PERMISSION_MISSING") {
                    throw e
                }
                Log.e(TAG, "Failed to start sing-box: ${e.message}", e)
                false
            }
        }
    }

    suspend fun stop() {
        mutex.withLock {
            try {
                Log.i(TAG, "Stopping sing-box engine...")
                stopTrafficStream()
                
                withTimeoutOrNull(3000) {
                    boxService?.closeService()
                } ?: Log.w(TAG, "boxService.closeService() timed out, ignoring...")

                try {
                    tunPfd?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing tunPfd: ${e.message}")
                }
                tunPfd = null

                Log.i(TAG, "sing-box engine stopped successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping sing-box: ${e.message}", e)
            } finally {
                isRunning = false
                startTime = 0L
                stopTrafficStream()
                try {
                    tunPfd?.close()
                } catch (_: Exception) {}
                tunPfd = null
            }
            return@withLock
        }
    }

    suspend fun destroy() {
        mutex.withLock {
            stopTrafficStream()
            
            val bs = boxService
            boxService = null
            currentVpnService = null
            
            if (bs != null) {
                if (isRunning) {
                    try {
                        withTimeoutOrNull(3000) {
                            bs.closeService()
                        }
                    } catch (_: Exception) {}
                }
                try {
                    bs.close()
                } catch (_: Exception) {}
            }

            try {
                tunPfd?.close()
            } catch (_: Exception) {}
            tunPfd = null
            
            isRunning = false
            startTime = 0L
        }
    }

    fun startTrafficStream(context: Context) {
        SingBoxTrafficMonitor.startTrafficStream(context, clashSecret)
    }

    fun stopTrafficStream() {
        SingBoxTrafficMonitor.stopTrafficStream()
    }

    fun getTraffic(callback: (Long, Long) -> Unit) {
        SingBoxTrafficMonitor.getTraffic(callback)
    }

    fun patchConfig(configContent: String, context: Context): String {
        clashSecret = java.util.UUID.randomUUID().toString()
        val patched = SingBoxConfigPatcher.patchConfig(configContent, context, logFile?.absolutePath, clashSecret)
        
        val outboundsArr = try { org.json.JSONObject(patched).optJSONArray("outbounds") } catch (e: Exception) { null }
        primaryProxyTag = SingBoxConfigChainer.findPrimaryProxyTag(outboundsArr ?: org.json.JSONArray())
        
        return patched
    }

    fun getPrimaryOutbound(configJson: String) = SingBoxConfigChainer.getPrimaryOutbound(configJson)

    fun generateChainedConfig(primaryConfig: String, chainedConfigs: List<String>) = SingBoxConfigChainer.generateChainedConfig(primaryConfig, chainedConfigs)

    suspend fun prepareConfigWithChaining(context: Context, baseConfigJson: String, settings: SettingsManager) = SingBoxConfigChainer.prepareConfigWithChaining(context, baseConfigJson, settings)
}
