package flare.client.app.singbox

import android.content.Context
import android.net.VpnService
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.os.ParcelFileDescriptor
import android.util.Log
import flare.client.app.data.SettingsManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.repository.ProfileRepository
import flare.client.app.data.parser.V2RayConfigConverter
import io.nekohasekai.libbox.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties

object SingBoxManager {

    private const val TAG = "SingBoxManager"
    private var boxService: CommandServer? = null
    private val mutex = Mutex()
    private var tunPfd: ParcelFileDescriptor? = null

    @Volatile
    private var currentVpnService: VpnService? = null

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
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var lastPermissionError: Boolean = false

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
                    val handler =
                            object : CommandServerHandler {
                                override fun serviceStop() {
                                    Log.i(TAG, "serviceStop called from sing-box core")
                                    val ctx = currentVpnService ?: appContext
                                    val intent = android.content.Intent(ctx, flare.client.app.service.FlareVpnService::class.java).apply {
                                        action = flare.client.app.service.FlareVpnService.ACTION_STOP
                                    }
                                    try {
                                        ctx.startService(intent)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to start FlareVpnService with ACTION_STOP", e)
                                    }
                                }
                                override fun serviceReload() {}
                                override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus()
                                override fun setSystemProxyEnabled(enabled: Boolean) {}
                                override fun writeDebugMessage(message: String?) {
                                    if (!message.isNullOrBlank()) Log.i(TAG, "[sb] $message")
                                }
                                override fun triggerNativeCrash() {}
                                override fun connectSSHAgent(): Int = 0
                            }

                    val platform =
                            object : PlatformInterface {
                                override fun autoDetectInterfaceControl(fd: Int) {
                                    currentVpnService?.protect(fd)
                                }

                                override fun clearDNSCache() {}

                                override fun findConnectionOwner(
                                        ipProtocol: Int,
                                        sourceAddress: String?,
                                        sourcePort: Int,
                                        destinationAddress: String?,
                                        destinationPort: Int
                                ): ConnectionOwner? = null

                                override fun getInterfaces(): NetworkInterfaceIterator? = null
                                override fun includeAllNetworks(): Boolean = false
                                override fun localDNSTransport(): LocalDNSTransport? = LocalResolver

                                override fun openTun(options: TunOptions?): Int {
                                    Log.i(
                                            TAG,
                                            "openTun called, mtu=${options?.mtu}, autoRoute=${options?.autoRoute}"
                                    )

                                    val vpn = currentVpnService
                                    if (vpn == null) {
                                        Log.e(TAG, "openTun: currentVpnService is null")
                                        return -1
                                    }

                                    try {
                                        val builder = vpn.Builder().setSession("Flare")

                                        try {
                                            val settings = SettingsManager(vpn)
                                            val modeApps = settings.splitTunnelingModeApps
                                            if (settings.isSplitTunnelingEnabled && settings.splitTunnelingApps.isNotEmpty()) {
                                                for (pkg in settings.splitTunnelingApps) {
                                                    try {
                                                        if (modeApps == "whitelist") {
                                                            builder.addAllowedApplication(pkg)
                                                        } else {
                                                            builder.addDisallowedApplication(pkg)
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Failed to add application: $pkg", e)
                                                    }
                                                }
                                                if (modeApps == "blacklist") {
                                                    builder.addDisallowedApplication(vpn.packageName)
                                                }
                                            } else {
                                                builder.addDisallowedApplication(vpn.packageName)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to configure VPN apps", e)
                                        }

                                        options?.let { opts ->
                                            builder.setMtu(opts.mtu)

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                builder.setMetered(false)
                                            }

                                            val inet4 = opts.inet4Address
                                            while (inet4.hasNext()) {
                                                val addr = inet4.next()
                                                builder.addAddress(addr.address(), addr.prefix())
                                                Log.d(TAG, "openTun: added address ${addr.address()}/${addr.prefix()}")
                                            }

                                            val inet6 = opts.inet6Address
                                            while (inet6.hasNext()) {
                                                val addr = inet6.next()
                                                builder.addAddress(addr.address(), addr.prefix())
                                                Log.d(TAG, "openTun: added IPv6 address ${addr.address()}/${addr.prefix()}")
                                            }

                                            if (opts.autoRoute) {
                                                try {
                                                    val dnsServers = opts.dnsServerAddress
                                                    if (dnsServers != null && dnsServers.hasNext()) {
                                                        while (dnsServers.hasNext()) {
                                                            val dnsAddr = dnsServers.next()
                                                            if (!dnsAddr.isNullOrBlank()) {
                                                                builder.addDnsServer(dnsAddr as String)
                                                                Log.d(TAG, "openTun: added DNS server $dnsAddr")
                                                            }
                                                        }
                                                    } else {
                                                        Log.w(
                                                                TAG,
                                                                "openTun: dnsServerAddress is empty, using 1.1.1.1 fallback"
                                                        )
                                                        builder.addDnsServer("1.1.1.1")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Failed to get dnsServerAddress, using 1.1.1.1 fallback: ${e.message}")
                                                    builder.addDnsServer("1.1.1.1")
                                                }

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    val v4routes = opts.inet4RouteAddress
                                                    if (v4routes.hasNext()) {
                                                        while (v4routes.hasNext()) {
                                                            val r = v4routes.next()
                                                            builder.addRoute(r.address(), r.prefix())
                                                        }
                                                    } else {
                                                        builder.addRoute("0.0.0.0", 0)
                                                    }

                                                    val v6routes = opts.inet6RouteAddress
                                                    if (v6routes.hasNext()) {
                                                        while (v6routes.hasNext()) {
                                                            val r = v6routes.next()
                                                            builder.addRoute(r.address(), r.prefix())
                                                        }
                                                    } else {
                                                        builder.addRoute("::", 0)
                                                    }
                                                } else {
                                                    val v4range = opts.inet4RouteRange
                                                    if (v4range.hasNext()) {
                                                        while (v4range.hasNext()) {
                                                            val r = v4range.next()
                                                            builder.addRoute(r.address(), r.prefix())
                                                        }
                                                    } else {
                                                        builder.addRoute("0.0.0.0", 0)
                                                    }

                                                    val v6range = opts.inet6RouteRange
                                                    if (v6range.hasNext()) {
                                                        while (v6range.hasNext()) {
                                                            val r = v6range.next()
                                                            builder.addRoute(r.address(), r.prefix())
                                                        }
                                                    } else {
                                                        builder.addRoute("::", 0)
                                                    }
                                                }
                                            } else {
                                                builder.addRoute("0.0.0.0", 0)
                                                builder.addRoute("::", 0)
                                                builder.addDnsServer("1.1.1.1")
                                                builder.addDnsServer("8.8.8.8")
                                            }
                                        }
                                                ?: run {
                                                    builder.addAddress("172.19.0.1", 30)
                                                    builder.addAddress("fdfe:dcba:9876::1", 126)
                                                    builder.addRoute("0.0.0.0", 0)
                                                    builder.addRoute("::", 0)
                                                    builder.addDnsServer("1.1.1.1")
                                                    builder.addDnsServer("8.8.8.8")
                                                }

                                        val pfd = builder.establish()
                                        if (pfd == null) {
                                            lastPermissionError = true
                                            Log.e(TAG, "openTun: VPN permission missing (establish returned null)")
                                            return -1
                                        }

                                        tunPfd?.close()
                                        tunPfd = pfd

                                        Log.i(TAG, "openTun: established fd=${pfd.fd}")
                                        return pfd.fd
                                    } catch (e: Exception) {
                                        Log.e(TAG, "openTun failed: ${e.message}", e)
                                        return -1
                                    }
                                }

                                override fun startNeighborMonitor(listener: NeighborUpdateListener?) {}
                                override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {}
                                override fun registerMyInterface(name: String?) {}
                                override fun usePlatformShell(): Boolean = false
                                override fun checkPlatformShell() {}
                                override fun openShellSession(
                                    user: PlatformUser?,
                                    command: String?,
                                    environ: StringIterator?,
                                    term: String?,
                                    rows: Int,
                                    cols: Int
                                ): ShellSession? = null
                                override fun lookupUser(username: String?): PlatformUser? = null
                                override fun lookupSFTPServer(): String? = null
                                override fun readSystemSSHHostKey(): String? = null
                                override fun tailscaleHostname(): String = ""

                                override fun readWIFIState(): WIFIState? = null
                                override fun sendNotification(notification: Notification?) {}
                                override fun startDefaultInterfaceMonitor(
                                        listener: InterfaceUpdateListener?
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        val cm = currentVpnService?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
                                        networkCallback = object : ConnectivityManager.NetworkCallback() {
                                            private fun notifyNetworkChange(network: Network) {
                                                try {
                                                    val caps = cm.getNetworkCapabilities(network)
                                                    val props = cm.getLinkProperties(network)
                                                    val isExpensive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
                                                    val interfaceName = props?.interfaceName
                                                    var idx = -1
                                                    if (interfaceName != null) {
                                                        try {
                                                            val ni = java.net.NetworkInterface.getByName(interfaceName)
                                                            if (ni != null) idx = ni.index
                                                        } catch (e: Exception) {}
                                                    }
                                                    listener?.updateDefaultInterface(interfaceName ?: "", idx, isExpensive, false)
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Error in notifyNetworkChange", e)
                                                }
                                            }
                                            override fun onAvailable(network: Network) { notifyNetworkChange(network) }
                                            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { notifyNetworkChange(network) }
                                            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) { notifyNetworkChange(network) }
                                        }
                                        try {
                                            cm.registerDefaultNetworkCallback(networkCallback!!)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "registerDefaultNetworkCallback failed", e)
                                        }
                                    }
                                }
                                override fun closeDefaultInterfaceMonitor(
                                        listener: InterfaceUpdateListener?
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        val cm = currentVpnService?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
                                        networkCallback?.let {
                                            try {
                                                cm.unregisterNetworkCallback(it)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "unregisterNetworkCallback failed", e)
                                            }
                                        }
                                        networkCallback = null
                                    }
                                }
                                override fun systemCertificates(): StringIterator? = null
                                override fun underNetworkExtension(): Boolean = false
                                override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
                                override fun useProcFS(): Boolean = true
                            }

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
                
                
                kotlinx.coroutines.withTimeoutOrNull(3000) {
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
                        kotlinx.coroutines.withTimeoutOrNull(3000) {
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

    private var trafficJob: kotlinx.coroutines.Job? = null
    @Volatile private var currentUpSpeed: Long = 0L
    @Volatile private var currentDownSpeed: Long = 0L
    @Volatile private var activeCall: okhttp3.Call? = null
    private val trafficScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    fun startTrafficStream(context: Context) {
        val settings = SettingsManager(context)
        if (!settings.isStatusNotificationEnabled || !settings.isNotificationSpeedEnabled) {
            stopTrafficStream()
            return
        }
        if (trafficJob != null) return

        val secret = clashSecret
        stopTrafficStream()
        trafficJob = trafficScope.launch {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = okhttp3.Request.Builder()
                .url("http://127.0.0.1:9092/traffic")
                .apply {
                    if (secret.isNotEmpty()) {
                        header("Authorization", "Bearer $secret")
                    }
                }
                .build()

            var attempt = 0
            while (isActive) {
                try {
                    val call = client.newCall(request)
                    activeCall = call
                    call.execute().use { response ->
                        if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
                        val body = response.body
                        val reader = body.charStream().buffered()
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            if (line.isNotBlank()) {
                                try {
                                    val obj = JSONObject(line)
                                    currentUpSpeed = obj.optLong("up", 0L)
                                    currentDownSpeed = obj.optLong("down", 0L)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing traffic JSON: ${e.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    Log.w(TAG, "Traffic stream error (attempt ${++attempt}): ${e.message}")
                    currentUpSpeed = 0L
                    currentDownSpeed = 0L
                    delay(2000)
                } finally {
                    activeCall = null
                }
            }
        }
    }

    fun stopTrafficStream() {
        trafficJob?.cancel()
        trafficJob = null
        try {
            activeCall?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling active traffic call: ${e.message}")
        }
        activeCall = null
        currentUpSpeed = 0L
        currentDownSpeed = 0L
    }

    fun getTraffic(callback: (Long, Long) -> Unit) {
        callback(currentUpSpeed, currentDownSpeed)
    }

    

    

    fun patchConfig(configContent: String, context: Context): String {
        clashSecret = java.util.UUID.randomUUID().toString()
        val settings = SettingsManager(context)
        val logFilePath = logFile?.absolutePath
        var patchedConfig =
            if (logFilePath != null) {
                injectLogOutput(configContent, logFilePath, settings)
            } else configContent

        return injectAdvancedSettings(patchedConfig, context)
    }

    private fun injectLogOutput(configJson: String, logFilePath: String, settings: SettingsManager): String {
        return try {
            val obj = JSONObject(configJson)
            val log = obj.optJSONObject("log") ?: JSONObject()
            
            val level = settings.coreLogLevel
            if (settings.isCoreLogEnabled && level != "none") {
                log.put("disabled", false)
                log.put("level", level)
                log.put("output", logFilePath)
            } else {
                log.put("disabled", true)
                log.remove("level")
                log.remove("output")
            }
            
            obj.put("log", log)
            obj.toString().replace("\\/", "/")
        } catch (e: Exception) {
            Log.w(TAG, "injectLogOutput failed (non-fatal): ${e.message}")
            configJson
        }
    }

    private fun injectAdvancedSettings(configJson: String, context: Context): String {
        return try {
            val settings = SettingsManager(context)
            val obj = JSONObject(configJson)

            sanitizeOutboundTags(obj)

            
            run {
                val route = obj.optJSONObject("route")
                val ruleSets = route?.optJSONArray("rule_set")
                if (ruleSets != null) {
                    val filesDir = context.filesDir.absolutePath
                    for (i in 0 until ruleSets.length()) {
                        val rs = ruleSets.optJSONObject(i) ?: continue
                        val path = rs.optString("path", "")
                        if (path.isNotEmpty() && !path.startsWith("/") && !path.startsWith("http")) {
                            rs.put("path", "$filesDir/$path")
                        }
                    }
                }
            }

            val experimental = obj.optJSONObject("experimental") ?: JSONObject().also { obj.put("experimental", it) }
            val clashApi = experimental.optJSONObject("clash_api") ?: JSONObject().also { experimental.put("clash_api", it) }
            clashApi.put("external_controller", "127.0.0.1:9092")
            if (clashSecret.isNotEmpty()) {
                clashApi.put("secret", clashSecret)
            }

            val outboundsArr = obj.optJSONArray("outbounds") ?: JSONArray()
            primaryProxyTag = findPrimaryProxyTag(outboundsArr)

            run {
                val dns = obj.optJSONObject("dns")
                val servers = dns?.optJSONArray("servers")
                val outbounds = obj.optJSONArray("outbounds")
                if (dns != null && servers != null && outbounds != null) {
                    val hasProxyOutbound = (0 until outbounds.length()).any {
                        outbounds.optJSONObject(it)?.optString("tag") == "proxy"
                    }
                    if (!hasProxyOutbound) {
                        val realProxyTag = findPrimaryProxyTag(outbounds)
                        if (realProxyTag != "proxy") {
                            Log.i(TAG, "injectAdvancedSettings: fixing dns-remote detour 'proxy' → '$realProxyTag'")
                            for (i in 0 until servers.length()) {
                                val server = servers.optJSONObject(i) ?: continue
                                if (server.optString("detour") == "proxy") {
                                    server.put("detour", realProxyTag)
                                }
                            }
                        }
                    }
                }
            }

            if (settings.isSplitTunnelingEnabled && settings.splitTunnelingSites.isNotEmpty()) {
                val modeSites = settings.splitTunnelingModeSites
                val sites = settings.splitTunnelingSites.toList()
                val domainsToAdd = sites.toHashSet()
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

                    
                    val proxyTag = findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())

                    
                    val targetOutbound = if (modeSites == "whitelist") proxyTag else "direct"

                    
                    val actionRules = JSONArray()
                    val routingRules = mutableListOf<JSONObject>()
                    for (i in 0 until rules.length()) {
                        val rule = rules.optJSONObject(i) ?: continue
                        if (rule.has("action")) actionRules.put(rule)
                        else routingRules.add(rule)
                    }

                    
                    
                    var merged = false
                    for (rule in routingRules) {
                        val ruleOutbound = rule.optString("outbound", "")
                        val hasDomainField = rule.has("domain_suffix") || rule.has("domain")
                        
                        val isPureDomainRule = hasDomainField &&
                            !rule.has("rule_set") && !rule.has("ip_is_private") &&
                            !rule.has("port") && !rule.has("network") && !rule.has("process_name")
                        if (ruleOutbound == targetOutbound && isPureDomainRule) {
                            
                            val existing = rule.optJSONArray("domain_suffix") ?: JSONArray()
                            val existingSet = (0 until existing.length()).map { existing.optString(it) }.toSet()
                            domainsToAdd.forEach { if (it !in existingSet) existing.put(it) }
                            rule.put("domain_suffix", existing)
                            
                            if (rule.has("domain") && !rule.has("domain_suffix")) {
                                rule.put("domain_suffix", existing)
                            }
                            merged = true
                            Log.i(TAG, "injectAdvancedSettings: merged sites into existing '$targetOutbound' rule")
                            break
                        }
                    }

                    if (!merged) {
                        
                        val siteRule = JSONObject().apply {
                            put("domain_suffix", JSONArray().also { sites.forEach(it::put) })
                            put("outbound", targetOutbound)
                        }
                        routingRules.add(0, siteRule)
                        Log.i(TAG, "injectAdvancedSettings: created new '$targetOutbound' domain rule")
                    }

                    
                    
                    
                    
                    route.put("final", if (modeSites == "whitelist") "direct" else proxyTag)

                    
                    val newRules = JSONArray()
                    for (i in 0 until actionRules.length()) newRules.put(actionRules.opt(i))
                    routingRules.forEach { newRules.put(it) }
                    route.put("rules", newRules)

                    
                    val dns = obj.optJSONObject("dns")
                    if (dns != null) {
                        val dnsRules = dns.optJSONArray("rules") ?: JSONArray().also { dns.put("rules", it) }
                        val domainsArray = JSONArray().also { sites.forEach(it::put) }
                        val dnsRule = JSONObject().apply {
                            put("domain_suffix", domainsArray)
                            
                            put("server", if (modeSites == "whitelist") "dns-remote" else "dns-direct")
                        }
                        
                        val newDnsRules = JSONArray()
                        var dnsInserted = false
                        for (i in 0 until dnsRules.length()) {
                            val dr = dnsRules.optJSONObject(i) ?: continue
                            newDnsRules.put(dr)
                            if (!dnsInserted) {
                                newDnsRules.put(dnsRule)
                                dnsInserted = true
                            }
                        }
                        if (!dnsInserted) newDnsRules.put(dnsRule)
                        dns.put("rules", newDnsRules)
                    }

                    Log.i(TAG, "injectAdvancedSettings: sites split tunneling done, mode=$modeSites, proxyTag=$proxyTag, merged=$merged, sites=$sites")
                }
            }
 
            if (settings.isRoutingMainEnabled) {
                val mode = settings.routingMainMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    injectOrUpdateRuleSet(rules, route, "geoip-ru", targetOutbound, "geoip-ru.srs", context)
                    injectOrUpdateRuleSet(rules, route, "geosite-ru", targetOutbound, "geosite-ru.srs", context)
                    Log.i(TAG, "injectAdvancedSettings: Main routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isRoutingGlobalEnabled) {
                val mode = settings.routingGlobalMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    injectOrUpdateRuleSet(rules, route, "geosite-global", targetOutbound, "geosite-global.srs", context)
                    Log.i(TAG, "injectAdvancedSettings: Global routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isRoutingMediaEnabled) {
                val mode = settings.routingMediaMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    
                    listOf("youtube", "netflix", "twitch", "disney").forEach { tag ->
                        injectOrUpdateRuleSet(rules, route, "geosite-$tag", targetOutbound, "geosite-$tag.srs", context)
                    }
                    Log.i(TAG, "injectAdvancedSettings: Media routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isRoutingSocialEnabled) {
                val mode = settings.routingSocialMode
                val route = obj.optJSONObject("route")
                if (route != null) {
                    val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                    val proxyTag = findPrimaryProxyTag(obj.optJSONArray("outbounds") ?: JSONArray())
                    val targetOutbound = when (mode) {
                        "proxy" -> proxyTag
                        "block" -> "block"
                        else -> "direct"
                    }
                    
                    listOf("telegram", "facebook", "instagram", "twitter", "tiktok").forEach { tag ->
                        injectOrUpdateRuleSet(rules, route, "geosite-$tag", targetOutbound, "geosite-$tag.srs", context)
                    }
                    Log.i(TAG, "injectAdvancedSettings: Social routing, mode=$mode, target=$targetOutbound")
                }
            }

            if (settings.isTlsSpoofEnabled) {
                val domain = settings.tlsSpoofDomain.trim()
                val method = settings.tlsSpoofMethod.trim()
                if (domain.isNotEmpty()) {
                    val route = obj.optJSONObject("route")
                    if (route != null) {
                        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }
                        val spoofRule = JSONObject().apply {
                            put("action", "route-options")
                            put("tls_spoof", domain)
                            if (method.isNotEmpty()) {
                                put("tls_spoof_method", method)
                            }
                            put("protocol", JSONArray().put("tls"))
                        }

                        val newRules = JSONArray()
                        newRules.put(spoofRule)
                        for (i in 0 until rules.length()) {
                            newRules.put(rules.opt(i))
                        }
                        route.put("rules", newRules)
                        Log.i(TAG, "injectAdvancedSettings: TLS Spoof rule injected: domain=$domain, method=$method")
                    }
                }
            }

            val fingerprint = settings.fingerprint
            if (fingerprint != "auto") {
                val outbounds = obj.optJSONArray("outbounds")
                if (outbounds != null) {
                    for (i in 0 until outbounds.length()) {
                        val ob = outbounds.optJSONObject(i) ?: continue
                        
                        val type = ob.optString("type")
                        if (type == "hysteria" || type == "hysteria2" || type == "tuic") {
                            continue
                        }
                        
                        val tls = ob.optJSONObject("tls")
                        if (tls != null) {
                            var utls = tls.optJSONObject("utls")
                            if (utls == null) {
                                utls = JSONObject().apply { put("enabled", true) }
                                tls.put("utls", utls)
                            } else {
                                utls.put("enabled", true)
                            }
                            utls.put("fingerprint", fingerprint)
                            Log.i(TAG, "injectAdvancedSettings: set utls fingerprint to $fingerprint for outbound '${ob.optString("tag")}'")
                        }
                    }
                }
            }

            val dnsUrl = when (settings.remoteDnsMode) {
                "cloudflare_doh" -> "https://1.1.1.1/dns-query"
                "adguard_doh" -> "https://dns.adguard-dns.com/dns-query"
                "google_dot" -> "tls://dns.google"
                "custom" -> settings.remoteDnsUrl
                else -> ""
            }
            if (dnsUrl.isNotBlank()) {
                val dns = obj.optJSONObject("dns")
                if (dns != null) {
                    val servers = dns.optJSONArray("servers")
                    if (servers != null) {
                        for (i in 0 until servers.length()) {
                            val server = servers.optJSONObject(i)
                            if (server != null && server.optString("tag") == "dns-remote") {
                                server.remove("type")
                                server.remove("server")
                                server.remove("server_port")
                                server.remove("path")
                                server.remove("responses")
                                server.remove("domain_resolver")
                                
                                server.put("address", dnsUrl)
                                V2RayConfigConverter.migrateDnsServerObject(server)
                                Log.i(
                                        TAG,
                                        "injectAdvancedSettings: overridden dns-remote address to $dnsUrl"
                                )
                                break
                            }
                        }
                    }
                }
            }

            val mtuValue = settings.mtu.toIntOrNull() ?: 1500
            val stackValue = settings.tunStack
            val fakeIpEnabled = settings.isFakeIpEnabled
            val inbounds = obj.optJSONArray("inbounds")
            if (inbounds != null) {
                for (i in 0 until inbounds.length()) {
                    val inb = inbounds.optJSONObject(i) ?: continue
                    if (inb.optString("type") == "tun") {
                        inb.put("mtu", mtuValue)
                        inb.put("stack", stackValue)
                        Log.i(
                                TAG,
                                "injectAdvancedSettings: set TUN mtu=$mtuValue, stack=$stackValue"
                        )
                        break
                    }
                }
            }

            if (fakeIpEnabled) {
                val dns = obj.optJSONObject("dns")
                if (dns != null) {
                    val servers =
                            dns.optJSONArray("servers")
                                    ?: JSONArray().also { dns.put("servers", it) }

                    var hasFakeIp = false
                    for (i in 0 until servers.length()) {
                        if (servers.optJSONObject(i)?.optString("tag") == "dns-fakeip") {
                            hasFakeIp = true
                            break
                        }
                    }
                    if (!hasFakeIp) {
                        servers.put(
                                JSONObject().apply {
                                    put("type", "fakeip")
                                    put("tag", "dns-fakeip")
                                    put("inet4_range", "198.18.0.0/15")
                                    put("inet6_range", "fc00::/18")
                                }
                        )
                        Log.i(TAG, "injectAdvancedSettings: added dns-fakeip server")
                    }

                    val dnsRules =
                            dns.optJSONArray("rules") ?: JSONArray().also { dns.put("rules", it) }
                    var hasQueryTypeRule = false
                    for (i in 0 until dnsRules.length()) {
                        val r = dnsRules.optJSONObject(i) ?: continue
                        val qt = r.optJSONArray("query_type")
                        if (qt != null && r.optString("server") == "dns-fakeip") {
                            hasQueryTypeRule = true
                            break
                        }
                    }
                    if (!hasQueryTypeRule) {
                        dnsRules.put(
                                JSONObject().apply {
                                    put("query_type", JSONArray().put("A").put("AAAA"))
                                    put("server", "dns-fakeip")
                                }
                        )
                        Log.i(TAG, "injectAdvancedSettings: added fakeip query_type DNS rule")
                    }
                } else {
                    Log.w(
                            TAG,
                            "injectAdvancedSettings: no dns section found, skipping fakeip injection"
                    )
                }
            }

            val outbounds =
                    obj.optJSONArray("outbounds") ?: return obj.toString().replace("\\/", "/")

            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                val type = ob.optString("type")
                if (type != "urltest" && type != "selector" && type != "direct" && type != "block" && type != "dns") {
                    if (!ob.has("connect_timeout")) {
                        ob.put("connect_timeout", "5s")
                        Log.i(TAG, "injectAdvancedSettings: set connect_timeout to 5s for outbound '${ob.optString("tag")}'")
                    }
                }
            }

            
            if (settings.isFragmentationEnabled) {
                val intervalMs = settings.fragmentInterval.trim().toIntOrNull() ?: 10
                for (i in 0 until outbounds.length()) {
                    val ob = outbounds.optJSONObject(i) ?: continue
                    val tls = ob.optJSONObject("tls")
                    if (tls != null) {
                        tls.put("fragment", true)
                        tls.put("record_fragment", true)

                        if (settings.packetType != "disabled") {
                            tls.put("fragment_fallback_delay", "${intervalMs}ms")
                        }
                        
                        Log.i(TAG, "injectAdvancedSettings: fragment injected on '${ob.optString("tag")}'")
                    }
                }
            }

            if (settings.isMuxEnabled) {
                val maxStreams = settings.muxMaxStreams.toIntOrNull()?.coerceIn(1, 128) ?: 8
                val protocol = settings.muxProtocol.ifBlank { "smux" }
                val padding = settings.muxPadding

                for (i in 0 until outbounds.length()) {
                    val ob = outbounds.optJSONObject(i) ?: continue
                    val type = ob.optString("type")
                    if (type == "direct" || type == "block" || type == "dns" || type == "urltest" || type == "selector") continue
                    if (type == "hysteria" || type == "hysteria2") continue

                    val flow = ob.optString("flow", "")
                    val hasReality = ob.optJSONObject("tls")?.has("reality") ?: false

                    if (flow.contains("vision") || hasReality) {
                        continue
                    }

                    ob.put(
                            "multiplex",
                            JSONObject().apply {
                                put("enabled", true)
                                put("protocol", protocol)
                                put("max_connections", 4)
                                put("min_streams", 4)
                                put("max_streams", maxStreams)
                                if (protocol == "smux") {
                                    put("padding", padding)
                                }
                            }
                    )
                    Log.i(
                            TAG,
                            "injectAdvancedSettings: mux injected on '${ob.optString("tag")}' " +
                                    "(protocol=$protocol, max_streams=$maxStreams, padding=$padding)"
                    )
                }
            }

            obj.toString().replace("\\/", "/")
        } catch (e: Exception) {
            Log.e(TAG, "injectAdvancedSettings failed: ${e.message}", e)
            configJson
        }
    }

    private fun sanitizeOutboundTags(obj: JSONObject) {
        val outbounds = obj.optJSONArray("outbounds") ?: return
        val seenTags = mutableSetOf<String>()

        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val tag = outbound.optString("tag", "")
            if (tag.isEmpty()) continue

            if (seenTags.contains(tag)) {
                var counter = 1
                var newTag = "${tag}_$counter"
                while (seenTags.contains(newTag)) {
                    counter++
                    newTag = "${tag}_$counter"
                }
                outbound.put("tag", newTag)
                Log.w(TAG, "Sanitizer: Duplicate tag found '$tag', renamed to '$newTag'")
                seenTags.add(newTag)
            } else {
                seenTags.add(tag)
            }
        }
    }

    
    private fun findActionRulesCount(rules: JSONArray): Int {
        var count = 0
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            if (rule.has("action")) count++ else break
        }
        return count
    }

    
    private fun injectOrUpdateRuleSet(
        rules: JSONArray,
        route: JSONObject,
        ruleSetTag: String,
        targetOutbound: String,
        srsFileName: String,
        context: Context
    ) {
        
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val rs = rule.optJSONArray("rule_set") ?: continue
            for (j in 0 until rs.length()) {
                if (rs.optString(j) == ruleSetTag) {
                    
                    rule.put("outbound", targetOutbound)
                    Log.i(TAG, "injectOrUpdateRuleSet: updated existing '$ruleSetTag' → outbound=$targetOutbound")
                    ensureRuleSetDef(route, ruleSetTag, srsFileName, context)
                    return
                }
            }
        }
        
        val insertPos = findActionRulesCount(rules)
        val newRule = JSONObject().apply {
            put("rule_set", JSONArray().put(ruleSetTag))
            put("outbound", targetOutbound)
        }
        
        val rebuilt = JSONArray()
        for (i in 0 until insertPos) rebuilt.put(rules.opt(i))
        rebuilt.put(newRule)
        for (i in insertPos until rules.length()) rebuilt.put(rules.opt(i))
        
        while (rules.length() > 0) rules.remove(0)
        for (i in 0 until rebuilt.length()) rules.put(rebuilt.opt(i))

        ensureRuleSetDef(route, ruleSetTag, srsFileName, context)
        Log.i(TAG, "injectOrUpdateRuleSet: inserted '$ruleSetTag' at pos=$insertPos, outbound=$targetOutbound")
    }

    
    private fun injectOrUpdatePrivateIpRule(rules: JSONArray, targetOutbound: String) {
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            if (rule.optBoolean("ip_is_private", false)) {
                rule.put("outbound", targetOutbound)
                Log.i(TAG, "injectOrUpdatePrivateIpRule: updated existing → outbound=$targetOutbound")
                return
            }
        }
        val insertPos = findActionRulesCount(rules)
        val newRule = JSONObject().apply {
            put("ip_is_private", true)
            put("outbound", targetOutbound)
        }
        val rebuilt = JSONArray()
        for (i in 0 until insertPos) rebuilt.put(rules.opt(i))
        rebuilt.put(newRule)
        for (i in insertPos until rules.length()) rebuilt.put(rules.opt(i))
        while (rules.length() > 0) rules.remove(0)
        for (i in 0 until rebuilt.length()) rules.put(rebuilt.opt(i))
        Log.i(TAG, "injectOrUpdatePrivateIpRule: inserted at pos=$insertPos, outbound=$targetOutbound")
    }

    
    private fun injectOrUpdateProtocolRule(rules: JSONArray, protocol: String, targetOutbound: String) {
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val proto = rule.optJSONArray("protocol") ?: continue
            for (j in 0 until proto.length()) {
                if (proto.optString(j) == protocol) {
                    rule.put("outbound", targetOutbound)
                    Log.i(TAG, "injectOrUpdateProtocolRule: updated existing '$protocol' → outbound=$targetOutbound")
                    return
                }
            }
        }
        val insertPos = findActionRulesCount(rules)
        val newRule = JSONObject().apply {
            put("protocol", JSONArray().put(protocol))
            put("outbound", targetOutbound)
        }
        val rebuilt = JSONArray()
        for (i in 0 until insertPos) rebuilt.put(rules.opt(i))
        rebuilt.put(newRule)
        for (i in insertPos until rules.length()) rebuilt.put(rules.opt(i))
        while (rules.length() > 0) rules.remove(0)
        for (i in 0 until rebuilt.length()) rules.put(rebuilt.opt(i))
        Log.i(TAG, "injectOrUpdateProtocolRule: inserted '$protocol' at pos=$insertPos, outbound=$targetOutbound")
    }

    
    private fun ensureRuleSetDef(route: JSONObject, tag: String, srsFileName: String, context: Context) {
        val ruleSets = route.optJSONArray("rule_set") ?: JSONArray().also { route.put("rule_set", it) }
        for (i in 0 until ruleSets.length()) {
            if (ruleSets.optJSONObject(i)?.optString("tag") == tag) return
        }
        ruleSets.put(JSONObject().apply {
            put("tag", tag)
            put("type", "local")
            put("format", "binary")
            val filesDir = context.filesDir.absolutePath
            val absolutePath = if (srsFileName.startsWith("/")) srsFileName else "$filesDir/$srsFileName"
            put("path", absolutePath)
        })
        Log.i(TAG, "ensureRuleSetDef: added definition for '$tag'")
    }

    private fun findPrimaryProxyTag(outbounds: JSONArray): String {

        
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type", "")
            if (type == "urltest" || type == "selector") {
                val tag = ob.optString("tag", "")
                if (tag.isNotEmpty()) return tag
            }
        }
        
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type", "")
            val tag = ob.optString("tag", "")
            if (tag.isNotEmpty() && type != "direct" && type != "block" && type != "dns" && type != "dns-out") {
                return tag
            }
        }
        return "proxy"
    }

    fun getPrimaryOutbound(configJson: String): JSONObject? {
        try {
            val obj = JSONObject(configJson)
            val outbounds = obj.optJSONArray("outbounds") ?: return null
            val primaryTag = findPrimaryProxyTag(outbounds)
            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                if (ob.optString("tag") == primaryTag) {
                    return ob
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPrimaryOutbound failed: ${e.message}", e)
        }
        return null
    }

    fun generateChainedConfig(primaryConfig: String, chainedConfigs: List<String>): String {
        try {
            val obj = JSONObject(primaryConfig)
            val primaryOutbounds = obj.optJSONArray("outbounds") ?: return primaryConfig
            val primaryTag = findPrimaryProxyTag(primaryOutbounds)

            val newOutboundsList = JSONArray()

            
            val hop0Outbounds = mutableListOf<JSONObject>()
            for (i in 0 until primaryOutbounds.length()) {
                val ob = primaryOutbounds.optJSONObject(i) ?: continue
                hop0Outbounds.add(JSONObject(ob.toString()))
            }
            
            renameAndDetourHop(
                outbounds = hop0Outbounds,
                hopIndex = 0,
                targetMainTag = "chain_node_0",
                detourTag = null
            )
            
            for (ob in hop0Outbounds) {
                newOutboundsList.put(ob)
            }

            
            for (h in chainedConfigs.indices) {
                val chainedJson = chainedConfigs[h]
                val hopObj = JSONObject(chainedJson)
                val hopOutboundsArr = hopObj.optJSONArray("outbounds") ?: continue
                
                val hopOutbounds = mutableListOf<JSONObject>()
                for (i in 0 until hopOutboundsArr.length()) {
                    val ob = hopOutboundsArr.optJSONObject(i) ?: continue
                    hopOutbounds.add(JSONObject(ob.toString()))
                }
                
                val hopIndex = h + 1
                val isExitNode = (h == chainedConfigs.size - 1)
                
                val targetTag = if (isExitNode) primaryTag else "chain_node_$hopIndex"
                val detourTag = "chain_node_$h"
                
                renameAndDetourHop(
                    outbounds = hopOutbounds,
                    hopIndex = hopIndex,
                    targetMainTag = targetTag,
                    detourTag = detourTag
                )
                
                for (ob in hopOutbounds) {
                    newOutboundsList.put(ob)
                }
            }

            obj.put("outbounds", newOutboundsList)
            return obj.toString().replace("\\/", "/")
        } catch (e: Exception) {
            Log.e(TAG, "generateChainedConfig failed: ${e.message}", e)
            return primaryConfig
        }
    }

    private fun renameAndDetourHop(
        outbounds: List<JSONObject>,
        hopIndex: Int,
        targetMainTag: String,
        detourTag: String?
    ): String {
        val originalMainTag = findPrimaryProxyTag(JSONArray().apply { outbounds.forEach { put(it) } })
        
        val tagsToRename = mutableSetOf<String>()
        for (ob in outbounds) {
            val tag = ob.optString("tag")
            if (tag.isNotEmpty() && tag != "direct" && tag != "block" && tag != "dns" && tag != "dns-out") {
                tagsToRename.add(tag)
            }
        }
        
        val renamedMap = mutableMapOf<String, String>()
        for (tag in tagsToRename) {
            if (tag == originalMainTag) {
                renamedMap[tag] = targetMainTag
            } else {
                renamedMap[tag] = "${tag}_hop$hopIndex"
            }
        }
        
        val nonPhysicalTypes = setOf("direct", "block", "dns", "dns-out", "selector", "urltest")
        
        for (ob in outbounds) {
            val tag = ob.optString("tag")
            val type = ob.optString("type")
            
            if (renamedMap.containsKey(tag)) {
                ob.put("tag", renamedMap[tag])
            }
            
            if (detourTag != null && !nonPhysicalTypes.contains(type)) {
                ob.put("detour", detourTag)
            } else {
                ob.remove("detour")
            }
            
            val subOutbounds = ob.optJSONArray("outbounds")
            if (subOutbounds != null) {
                val newSubOutbounds = JSONArray()
                for (j in 0 until subOutbounds.length()) {
                    val subTag = subOutbounds.optString(j)
                    if (renamedMap.containsKey(subTag)) {
                        newSubOutbounds.put(renamedMap[subTag])
                    } else {
                        newSubOutbounds.put(subTag)
                    }
                }
                ob.put("outbounds", newSubOutbounds)
            }
        }
        
        return targetMainTag
    }

    suspend fun prepareConfigWithChaining(
        context: Context,
        baseConfigJson: String,
        settings: SettingsManager
    ): String = withContext(Dispatchers.IO) {
        val idsStr = settings.chainedProfileIdsString
        if (idsStr.isBlank()) {
            return@withContext baseConfigJson
        }
        
        val ids = idsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) {
            return@withContext baseConfigJson
        }
        
        try {
            val db = AppDatabase.getInstance(context)
            val repository = ProfileRepository(db.profileDao(), db.subscriptionDao())
            val profiles = repository.getProfilesByIds(ids)
            val profilesMap = profiles.associateBy { it.id }
            val orderedConfigs = ids.mapNotNull { id ->
                profilesMap[id]?.configJson
            }
            
            if (orderedConfigs.isEmpty()) {
                return@withContext baseConfigJson
            }
            
            return@withContext generateChainedConfig(baseConfigJson, orderedConfigs)
        } catch (e: Exception) {
            Log.e(TAG, "prepareConfigWithChaining failed: ${e.message}", e)
            return@withContext baseConfigJson
        }
    }
}
