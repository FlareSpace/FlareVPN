package flare.client.app.service

import flare.client.app.ui.i18n.I18n

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import flare.client.app.R
import flare.client.app.singbox.GeoFileManager
import flare.client.app.singbox.SingBoxManager
import io.nekohasekai.libbox.Libbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class FlareVpnService : VpnService() {

    companion object {
        private const val TAG = "FlareVpnService"
        const val ACTION_START = "flare.client.app.START_VPN"
        const val ACTION_STOP = "flare.client.app.STOP_VPN"
        const val EXTRA_CONFIG = "flare.client.app.CONFIG_JSON"
        const val EXTRA_PROFILE_NAME = "flare.client.app.PROFILE_NAME"
        const val BROADCAST_STATE = "flare.client.app.VPN_STATE"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_ERROR = "error"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_PERMISSION_REQUIRED = "permission_required"
        private const val NOTIF_CHANNEL = "flare_vpn"
        private const val NOTIF_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandMutex = Mutex()
    private var statsJob: kotlinx.coroutines.Job? = null
    private var profileName: String = "Flare Profile"
    @Volatile
    private var isDeinitialized = false
    @Volatile
    private var latestStartId = -1

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) {
            Log.w(TAG, "onStartCommand: intent or action is null, stopping service (startId=$startId)")
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIF_ID)
            stopSelf()
            return START_NOT_STICKY
        }
        val action = intent.action
        val configJson = intent.getStringExtra(EXTRA_CONFIG)
        val name = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Flare Profile"

        latestStartId = startId

        if (action == ACTION_START) {
            android.widget.Toast.makeText(this, I18n.strings.vpn_starting, android.widget.Toast.LENGTH_SHORT).show()
        } else if (action == ACTION_STOP) {
            android.widget.Toast.makeText(this, I18n.strings.vpn_stopping, android.widget.Toast.LENGTH_SHORT).show()
        }

        serviceScope.launch {
            commandMutex.withLock {
                if (action == ACTION_START && startId != latestStartId) {
                    Log.i(TAG, "Skipping obsolete ACTION_START (startId=$startId, latest=$latestStartId)")
                    return@withLock
                }
                when (action) {
                    ACTION_START -> {
                        val vpnIntent = VpnService.prepare(this@FlareVpnService)
                        if (vpnIntent != null) {
                            broadcastState(false, error = true, permissionRequired = true)
                            stopSelf()
                            return@withLock
                        }

                        if (configJson != null) {
                            profileName = name
                            startVpnInternal(configJson, startId)
                        }
                    }
                    ACTION_STOP -> stopVpnInternal(startId)
                }
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        Log.i(TAG, "onRevoke called")
        super.onRevoke()
        serviceScope.launch {
            commandMutex.withLock {
                stopVpnInternal()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy called")
        val wasDeinitialized = isDeinitialized
        isDeinitialized = true
        broadcastState(false)
        
        serviceScope.cancel()
        
        CoroutineScope(Dispatchers.IO).launch {
            commandMutex.withLock {
                if (!wasDeinitialized) {
                    kotlinx.coroutines.withTimeoutOrNull(5000) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            SingBoxManager.stop()
                        }
                    }
                }
                kotlinx.coroutines.withTimeoutOrNull(5000) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        SingBoxManager.destroy()
                    }
                }
            }
        }
    }

    private suspend fun startVpnInternal(configJson: String, startId: Int) {
        isDeinitialized = false
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notification)

        try {
            GeoFileManager.ensureGeoFiles(this)
            SingBoxManager.ensureSetup(this)
            
            if (startId != latestStartId) {
                Log.i(TAG, "Aborting startVpnInternal before config patch (startId=$startId, latest=$latestStartId)")
                return
            }

            try {
                val patchedConfig = SingBoxManager.patchConfig(configJson, this)
                Libbox.checkConfig(patchedConfig)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown validation error"
                Log.e(TAG, "Config validation FAILED: $errorMsg")
                stopVpnOnError(startId, errorMessage = errorMsg)
                return
            }

            if (startId != latestStartId) {
                Log.i(TAG, "Aborting startVpnInternal before tunnel stop (startId=$startId, latest=$latestStartId)")
                return
            }

            if (SingBoxManager.isRunning) {
                Log.i(TAG, "Stopping active tunnel for configuration switch/reload")
                SingBoxManager.stop()
            }

            if (startId != latestStartId) {
                Log.i(TAG, "Aborting startVpnInternal before engine start (startId=$startId, latest=$latestStartId)")
                return
            }

            val started = try {
                SingBoxManager.start(configJson, this)
            } catch (e: Exception) {
                val isPermission = e.message == "VPN_PERMISSION_MISSING"
                stopVpnOnError(startId, permissionRequired = isPermission)
                return
            }

            if (!started) {
                stopVpnOnError(startId)
                return
            }

            broadcastState(true)
            startStatsPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            stopVpnOnError(startId)
        }
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            val settings = flare.client.app.data.SettingsManager(this@FlareVpnService)
            var wasNotificationShown = true
            while (isActive) {
                val statusEnabled = settings.isStatusNotificationEnabled
                val speedEnabled = settings.isNotificationSpeedEnabled

                if (statusEnabled) {
                    if (speedEnabled) {
                        SingBoxManager.startTrafficStream(this@FlareVpnService)
                    } else {
                        SingBoxManager.stopTrafficStream()
                    }

                    SingBoxManager.getTraffic { up, down ->
                        if (isActive && SingBoxManager.isRunning) {
                            updateNotification(up, down)
                        }
                    }
                    wasNotificationShown = true
                } else {
                    SingBoxManager.stopTrafficStream()
                    if (wasNotificationShown) {
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.cancel(NOTIF_ID)
                        wasNotificationShown = false
                    }
                }
                delay(1000)
            }
        }
    }

    private fun updateNotification(up: Long, down: Long) {
        val settings = flare.client.app.data.SettingsManager(this)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (settings.isNotificationSpeedEnabled) {
            manager.notify(NOTIF_ID, buildNotification(formatSpeed(up), formatSpeed(down)))
        } else {
            manager.notify(NOTIF_ID, buildNotification(null, null))
        }
    }

    private fun formatSpeed(bytes: Long): String {
        return if (bytes < 1024) {
            "$bytes B/s"
        } else if (bytes < 1024 * 1024) {
            String.format("%.1f KB/s", bytes / 1024.0)
        } else {
            String.format("%.1f MB/s", bytes / (1024.0 * 1024.0))
        }
    }

    private suspend fun stopVpnInternal(startId: Int = -1) {
        if (isDeinitialized) {
            Log.i(TAG, "stopVpnInternal: already deinitialized (startId=$startId), ensuring stopSelf and broadcast")
            stopSelf()
            broadcastState(false)
            return
        }
        isDeinitialized = true
        Log.i(TAG, "stopVpnInternal: begin (startId=$startId)")
        statsJob?.cancel()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID)
        
        kotlinx.coroutines.withTimeoutOrNull(5000) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                SingBoxManager.stop()
            }
        } ?: Log.e(TAG, "SingBoxManager.stop() timed out inside FlareVpnService!")
        
        Log.i(TAG, "stopVpnInternal: engine stopped")
        
        
        broadcastState(false)
        
        stopSelf()
    }

    private suspend fun stopVpnOnError(
        startId: Int,
        errorMessage: String? = null,
        permissionRequired: Boolean = false
    ) {
        if (isDeinitialized) {
            Log.i(TAG, "stopVpnOnError: already deinitialized (startId=$startId), ensuring stopSelf and broadcast")
            stopSelf()
            broadcastState(false, error = true, permissionRequired = permissionRequired, errorMessage = errorMessage)
            return
        }
        isDeinitialized = true
        Log.i(TAG, "stopVpnOnError: startId=$startId, error=$errorMessage, permission=$permissionRequired")
        statsJob?.cancel()
        broadcastState(false, error = true, permissionRequired = permissionRequired, errorMessage = errorMessage)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID)
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            SingBoxManager.stop()
        }
        stopSelf()
    }

    private fun broadcastState(connected: Boolean, error: Boolean = false, permissionRequired: Boolean = false, errorMessage: String? = null) {
        sendBroadcast(
                Intent(BROADCAST_STATE).apply {
                    putExtra(EXTRA_CONNECTED, connected)
                    putExtra(EXTRA_ERROR, error)
                    putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
                    putExtra(EXTRA_PERMISSION_REQUIRED, permissionRequired)
                    `package` = packageName
                }
        )
        try {
            flare.client.app.widget.FlareWidgetProvider.updateAllWidgets(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets in broadcastState: ${e.message}")
        }
    }

    private fun buildNotification(upStr: String? = null, downStr: String? = null): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(NOTIF_CHANNEL) == null) {
            manager.createNotificationChannel(
                    NotificationChannel(
                            NOTIF_CHANNEL,
                            "Flare VPN",
                            NotificationManager.IMPORTANCE_LOW
                    )
            )
        }

        val mainIntent = Intent(this, flare.client.app.ui.MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent =
                PendingIntent.getService(
                        this,
                        0,
                        Intent(this, FlareVpnService::class.java).apply { action = ACTION_STOP },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val contentText = if (upStr != null && downStr != null) {
            "$upStr ↑ $downStr ↓"
        } else {
            I18n.strings.vpn_active
        }

        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
                .setContentTitle(profileName)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_vpn_key)
                .setContentIntent(mainPendingIntent)
                .addAction(R.drawable.ic_vpn_key, I18n.strings.vpn_disconnect, stopIntent)
                .setOngoing(true)
                .build()
    }
}
