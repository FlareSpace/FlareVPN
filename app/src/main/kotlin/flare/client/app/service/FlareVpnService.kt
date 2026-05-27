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
import kotlinx.coroutines.runBlocking

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

        if (action == ACTION_START) {
            android.widget.Toast.makeText(this, I18n.strings.vpn_starting, android.widget.Toast.LENGTH_SHORT).show()
        } else if (action == ACTION_STOP) {
            android.widget.Toast.makeText(this, I18n.strings.vpn_stopping, android.widget.Toast.LENGTH_SHORT).show()
        }

        serviceScope.launch {
            commandMutex.withLock {
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
        runBlocking {
            commandMutex.withLock {
                stopVpnInternal()
                SingBoxManager.destroy()
            }
        }
        serviceScope.cancel()
    }

    private suspend fun startVpnInternal(configJson: String, startId: Int) {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notification)

        try {
            GeoFileManager.ensureGeoFiles(this)
            SingBoxManager.ensureSetup(this)
            
            try {
                val patchedConfig = SingBoxManager.patchConfig(configJson, this)
                Libbox.checkConfig(patchedConfig)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown validation error"
                Log.e(TAG, "Config validation FAILED: $errorMsg")
                stopVpnOnError(startId, errorMessage = errorMsg)
                return
            }

            if (SingBoxManager.isRunning) {
                Log.i(TAG, "Stopping active tunnel for configuration switch/reload")
                SingBoxManager.stop()
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
        val settings = flare.client.app.data.SettingsManager(this)
        if (!settings.isStatusNotificationEnabled) return

        statsJob?.cancel()
        statsJob = serviceScope.launch {
            while (isActive) {
                SingBoxManager.getTraffic { up, down ->
                    if (isActive && SingBoxManager.isRunning) {
                        updateNotification(up, down)
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
        Log.i(TAG, "stopVpnInternal: begin (startId=$startId)")
        statsJob?.cancel()
        broadcastState(false)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID)
        SingBoxManager.stop()
        Log.i(TAG, "stopVpnInternal: engine stopped")
        stopSelf()
    }

    private suspend fun stopVpnOnError(
        startId: Int,
        errorMessage: String? = null,
        permissionRequired: Boolean = false
    ) {
        Log.i(TAG, "stopVpnOnError: startId=$startId, error=$errorMessage, permission=$permissionRequired")
        statsJob?.cancel()
        broadcastState(false, error = true, permissionRequired = permissionRequired, errorMessage = errorMessage)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID)
        SingBoxManager.stop()
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
