package flare.client.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.util.Log
import android.widget.RemoteViews
import flare.client.app.R
import flare.client.app.data.SettingsManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.model.ProfileEntity
import flare.client.app.service.AppMonitorService
import flare.client.app.service.FlareVpnService
import flare.client.app.singbox.SingBoxManager
import flare.client.app.ui.MainActivity
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.i18n.RuFlareStrings
import kotlinx.coroutines.*
import org.json.JSONObject

class FlareWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "FlareWidgetProvider"
        const val ACTION_TOGGLE_VPN = "flare.client.app.WIDGET_TOGGLE_VPN"
        const val ACTION_PING_PROFILE = "flare.client.app.WIDGET_PING_PROFILE"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, FlareWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, FlareWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgetState(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive action: ${intent.action}")

        if (intent.action == FlareVpnService.BROADCAST_STATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, FlareWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            updateWidgetState(context, appWidgetManager, appWidgetIds)
        } else if (intent.action == ACTION_TOGGLE_VPN) {
            handleVpnToggle(context)
        } else if (intent.action == ACTION_PING_PROFILE) {
            handlePingProfile(context)
        }
    }

    private fun updateWidgetState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        widgetScope.launch {
            val settings = SettingsManager(context.applicationContext)
            I18n.updateLocale(settings.appLanguage)

            val db = AppDatabase.getInstance(context.applicationContext)
            val profile = withContext(Dispatchers.IO) {
                try {
                    db.profileDao().getSelectedProfile()
                } catch (e: Exception) {
                    null
                }
            }

            val profilePrefix = if (I18n.strings == RuFlareStrings) "Профиль: " else "Profile: "
            val noneSelectedText = if (I18n.strings == RuFlareStrings) "не выбран" else "Not Selected"
            val profileLabel = if (profile != null) "$profilePrefix${profile.name}" else "$profilePrefix$noneSelectedText"

            val isRunning = SingBoxManager.isRunning
            val selectedProfileId = profile?.id ?: -1L
            val widgetPing = if (settings.lastWidgetPingProfileId == selectedProfileId) {
                settings.lastWidgetPing
            } else {
                "--"
            }

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.flare_widget)
                views.setTextViewText(R.id.txt_profile, profileLabel)
                views.setTextViewText(R.id.txt_ping, widgetPing)

                
                val statusText: String
                val statusDotColor: Int
                val powerButtonTint: Int

                if (isRunning) {
                    statusText = I18n.strings.status_connected
                    statusDotColor = Color.parseColor("#FF34C759") 
                    powerButtonTint = Color.parseColor("#FF34C759") 
                } else {
                    statusText = I18n.strings.status_disconnected
                    statusDotColor = Color.parseColor("#FF8E8E93") 
                    powerButtonTint = Color.parseColor("#FF8E8E93") 
                }

                views.setTextViewText(R.id.txt_status, statusText)
                
                
                views.setInt(R.id.img_status_dot, "setColorFilter", statusDotColor)

                
                views.setInt(R.id.btn_connect, "setColorFilter", powerButtonTint)

                
                views.setInt(R.id.btn_ping, "setColorFilter", Color.parseColor("#FF8E8E93"))

                
                val toggleIntent = Intent(context, FlareWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_VPN
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_connect, pendingIntent)

                
                val pingIntent = Intent(context, FlareWidgetProvider::class.java).apply {
                    action = ACTION_PING_PROFILE
                }
                val pingPendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    pingIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_ping, pingPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun handleVpnToggle(context: Context) {
        val isRunning = SingBoxManager.isRunning
        val appContext = context.applicationContext
        if (isRunning) {
            val stopIntent = Intent(appContext, FlareVpnService::class.java).apply {
                action = FlareVpnService.ACTION_STOP
            }
            appContext.startService(stopIntent)
        } else {
            val vpnIntent = VpnService.prepare(appContext)
            if (vpnIntent != null) {
                
                val mainIntent = Intent(appContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(AppMonitorService.EXTRA_REQUEST_VPN_PERMISSION, true)
                }
                appContext.startActivity(mainIntent)
                return
            }

            
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val thisWidget = ComponentName(appContext, FlareWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val settings = SettingsManager(appContext)
            I18n.updateLocale(settings.appLanguage)
            
            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(appContext.packageName, R.layout.flare_widget)
                views.setTextViewText(R.id.txt_status, I18n.strings.status_connecting)
                views.setInt(R.id.img_status_dot, "setColorFilter", Color.parseColor("#FFFF9500")) 
                views.setInt(R.id.btn_connect, "setColorFilter", Color.parseColor("#FFFF9500"))
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            widgetScope.launch {
                val db = AppDatabase.getInstance(appContext)
                val profile = withContext(Dispatchers.IO) {
                    try {
                        db.profileDao().getSelectedProfile()
                    } catch (e: Exception) {
                        null
                    }
                }

                if (profile != null) {
                    val chainedConfig = SingBoxManager.prepareConfigWithChaining(appContext, profile.configJson, settings)
                    val configWithSettings = patchMtu(chainedConfig, settings.mtu, settings.tunStack)
                    val startIntent = Intent(appContext, FlareVpnService::class.java).apply {
                        action = FlareVpnService.ACTION_START
                        putExtra(FlareVpnService.EXTRA_CONFIG, configWithSettings)
                        putExtra(FlareVpnService.EXTRA_PROFILE_NAME, profile.name)
                    }
                    appContext.startService(startIntent)
                } else {
                    
                    val ids = appWidgetManager.getAppWidgetIds(thisWidget)
                    updateWidgetState(appContext, appWidgetManager, ids)
                }
            }
        }
    }

    private fun handlePingProfile(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            val settings = SettingsManager(appContext)
            val db = AppDatabase.getInstance(appContext)
            val profile = withContext(Dispatchers.IO) {
                try {
                    db.profileDao().getSelectedProfile()
                } catch (e: Exception) {
                    null
                }
            }
            if (profile == null) {
                settings.lastWidgetPing = "--"
                settings.lastWidgetPingProfileId = -1L
                updateAllWidgets(appContext)
                return@launch
            }

            
            settings.lastWidgetPing = "..."
            settings.lastWidgetPingProfileId = profile.id
            updateAllWidgets(appContext)

            val isProxy = settings.pingType.startsWith("via")
            val (latency, error) = withContext(Dispatchers.IO) {
                if (isProxy) {
                    var resultLatency = -1L
                    var resultError: String? = "Timeout"
                    try {
                        flare.client.app.util.PingHelper.pingProxyBatch(appContext, listOf(profile), settings.pingTestUrl) { _, lat, err ->
                            resultLatency = lat
                            resultError = err
                        }
                    } catch (e: Exception) {
                        resultError = e.message ?: "Error"
                    }
                    resultLatency to resultError
                } else {
                    val method = if (settings.pingType == "ICMP") "ICMP" else "TCP"
                    flare.client.app.util.PingHelper.pingDirect(profile, method)
                }
            }

            val pingText = if (latency >= 0) {
                "$latency ms"
            } else {
                error ?: "Error"
            }

            settings.lastWidgetPing = pingText
            settings.lastWidgetPingProfileId = profile.id
            updateAllWidgets(appContext)
        }
    }

    private fun patchMtu(json: String, newMtu: String, tunStack: String): String {
        return try {
            val obj = JSONObject(json)
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
}
