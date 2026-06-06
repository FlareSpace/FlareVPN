package flare.client.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build
import android.app.PendingIntent
import android.net.VpnService
import flare.client.app.service.AppMonitorService
import flare.client.app.service.FlareVpnService
import flare.client.app.singbox.SingBoxManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.SettingsManager
import flare.client.app.R
import kotlinx.coroutines.*

class FlareTileService : TileService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateTileState()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        registerReceiver(vpnReceiver, IntentFilter(FlareVpnService.BROADCAST_STATE), flags)
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        try {
            unregisterReceiver(vpnReceiver)
        } catch (_: Exception) {}
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = SingBoxManager.isRunning
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Flare VPN"
        tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_tile_flare)
        tile.updateTile()
    }

    override fun onClick() {
        val isRunning = SingBoxManager.isRunning
        if (isRunning) {
            startService(Intent(this, FlareVpnService::class.java).apply { action = FlareVpnService.ACTION_STOP })
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(AppMonitorService.EXTRA_REQUEST_VPN_PERMISSION, true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
                return
            }

            serviceScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val profile = withContext(Dispatchers.IO) { db.profileDao().getSelectedProfile() }
                
                if (profile != null) {
                    val settings = SettingsManager(applicationContext)
                    val chainedConfig = SingBoxManager.prepareConfigWithChaining(applicationContext, profile.configJson, settings)
                    val configWithSettings = patchMtu(chainedConfig, settings.mtu, settings.tunStack)
                    val intent = Intent(applicationContext, FlareVpnService::class.java).apply {
                        action = FlareVpnService.ACTION_START
                        putExtra(FlareVpnService.EXTRA_CONFIG, configWithSettings)
                        putExtra(FlareVpnService.EXTRA_PROFILE_NAME, profile.name)
                    }
                    startService(intent)
                }
            }
        }
    }
    
    private fun patchMtu(json: String, newMtu: String, tunStack: String): String {
        return try {
            val obj = org.json.JSONObject(json)
            val inbounds = obj.optJSONArray("inbounds")
            if (inbounds != null) {
                for (i in 0 until inbounds.length()) {
                    val inbound = inbounds.optJSONObject(i)
                    if (inbound?.optString("type") == "tun") {
                        inbound.put("mtu", newMtu.toIntOrNull() ?: 1500)
                        inbound.put("stack", tunStack)
                    }
                }
            }
            obj.toString().replace("\\/", "/")
        } catch (e: Exception) {
            json
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
