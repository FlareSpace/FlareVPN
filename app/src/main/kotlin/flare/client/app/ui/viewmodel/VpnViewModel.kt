package flare.client.app.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.R
import flare.client.app.data.SettingsManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.repository.ProfileRepository
import flare.client.app.service.FlareVpnService
import flare.client.app.ui.i18n.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppDatabase.getInstance(application)
    }
    private val repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProfileRepository(db.profileDao(), db.subscriptionDao())
    }

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    private val _chainedProfileIds = MutableStateFlow<List<Long>>(emptyList())
    val chainedProfileIds: StateFlow<List<Long>> = _chainedProfileIds.asStateFlow()

    private val _vpnPermissionIntent = MutableSharedFlow<Intent>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val vpnPermissionIntent: SharedFlow<Intent> = _vpnPermissionIntent.asSharedFlow()

    private var selectionJob: kotlinx.coroutines.Job? = null

    private var isReceiverRegistered = false

    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FlareVpnService.BROADCAST_STATE) {
                val connected = intent.getBooleanExtra(FlareVpnService.EXTRA_CONNECTED, false)
                val hasError = intent.getBooleanExtra(FlareVpnService.EXTRA_ERROR, false)
                val permissionRequired = intent.getBooleanExtra(FlareVpnService.EXTRA_PERMISSION_REQUIRED, false)

                if (connected) {
                    _connectionState.value = ConnectionState.CONNECTED
                } else {
                    if (_connectionState.value == ConnectionState.CONNECTING && !hasError) {
                        return
                    }
                    _connectionState.value = ConnectionState.DISCONNECTED
                    handleDisconnection()
                    if (hasError) {
                        val settings = SettingsManager(context)
                        if (!settings.isAdaptiveTunnelEnabled) {
                            val errorMessage = intent.getStringExtra(FlareVpnService.EXTRA_ERROR_MESSAGE)
                            val errorMsg = if (permissionRequired) {
                                I18n.strings.vpn_error_permission_required
                            } else if (!errorMessage.isNullOrBlank()) {
                                errorMessage
                            } else {
                                I18n.strings.vpn_error_tunnel_creation
                            }
                            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                                flare.client.app.ui.notification.NotificationType.ERROR, errorMsg, 4)
                        }
                    }
                }

                val newProfileId = intent.getLongExtra(FlareVpnService.EXTRA_SELECTED_PROFILE_ID, -1L)
                if (newProfileId != -1L) {
                    _selectedProfileId.value = newProfileId
                }
            }
        }
    }

    init {
        initialize()
    }

    private fun initialize() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        
        if (!isReceiverRegistered) {
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_NOT_EXPORTED
            } else {
                0
            }
            app.registerReceiver(vpnReceiver, IntentFilter(FlareVpnService.BROADCAST_STATE), flags)
            isReceiverRegistered = true
        }

        if (flare.client.app.singbox.SingBoxManager.isRunning) {
            _connectionState.value = ConnectionState.CONNECTED
        }

        viewModelScope.launch {
            
            
            val selected = repository.getSelectedProfile()?.id
            _selectedProfileId.value = selected
            val shouldAutostart = settings.isAutostartEnabled && !flare.client.app.singbox.SingBoxManager.isRunning
            if (shouldAutostart && selected != null) {
                delay(500)
                startVpn()
            }
        }

        val chainedIdsStr = settings.chainedProfileIdsString
        if (chainedIdsStr.isNotBlank()) {
            _chainedProfileIds.value = chainedIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        if (isReceiverRegistered) {
            app.unregisterReceiver(vpnReceiver)
            isReceiverRegistered = false
        }
    }

    fun selectProfile(profileId: Long) {
        selectionJob?.cancel()
        val app = getApplication<Application>()
        try {
            app.startService(Intent(app, FlareVpnService::class.java).apply {
                action = FlareVpnService.ACTION_STOP_MONITORING
            })
        } catch (_: IllegalStateException) { }
        selectionJob = viewModelScope.launch {
            val currentChain = _chainedProfileIds.value.toMutableList()
            if (currentChain.contains(profileId)) {
                currentChain.remove(profileId)
                _chainedProfileIds.value = currentChain
                val app = getApplication<Application>()
                val settings = SettingsManager(app)
                settings.chainedProfileIdsString = currentChain.joinToString(",")
            }

            repository.selectProfile(profileId)
            _selectedProfileId.value = profileId

            val app = getApplication<Application>()
            try {
                flare.client.app.widget.FlareWidgetProvider.updateAllWidgets(app)
            } catch (e: Exception) {
                Log.e("VpnViewModel", "Failed to update widget: ${e.message}")
            }

            if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
                delay(300)
                startVpn()
            }
        }
    }

    fun connectOrDisconnect() = if (_connectionState.value != ConnectionState.DISCONNECTED) stopVpn(true) else startVpn()
    
    fun startVpnFromUi() = startVpn()
    
    fun stopVpnFromUi(cancelRecovery: Boolean = true) = stopVpn(cancelRecovery)
    
    fun selectBestProfileFromUi() {
        val app = getApplication<Application>()
        try {
            app.startService(Intent(app, FlareVpnService::class.java).apply {
                action = FlareVpnService.ACTION_RECOVER
            })
        } catch (e: IllegalStateException) {
            Log.e("VpnViewModel", "selectBestProfileFromUi: startService failed: ${e.message}")
        }
    }

    fun startVpn() {
        viewModelScope.launch {
            val profile = repository.getSelectedProfile()
            if (profile == null) {
                flare.client.app.ui.notification.AppNotificationManager.showNotification(
                    type = flare.client.app.ui.notification.NotificationType.WARNING,
                    text = I18n.strings.error_profile_selection_required,
                    durationSec = 2
                )
                return@launch
            }
            val app = getApplication<Application>()
            val settings = SettingsManager(app)
            
            if (!settings.isStatusNotificationEnabled) {
                flare.client.app.ui.notification.AppNotificationManager.showNotification(
                    type = flare.client.app.ui.notification.NotificationType.WARNING,
                    text = I18n.strings.settings_notif_disabled_warning,
                    durationSec = 6
                )
            }
            
            val chainedConfig = flare.client.app.singbox.SingBoxManager.prepareConfigWithChaining(app, profile.configJson, settings)
            val configWithSettings = patchMtu(chainedConfig, settings.mtu, settings.tunStack)

            val vpnIntent = VpnService.prepare(app)
            if (vpnIntent != null) {
                _vpnPermissionIntent.emit(vpnIntent)
                return@launch
            }
            _connectionState.value = ConnectionState.CONNECTING
            val intent = Intent(app, FlareVpnService::class.java).apply {
                action = FlareVpnService.ACTION_START
                putExtra(FlareVpnService.EXTRA_CONFIG, configWithSettings)
                putExtra(FlareVpnService.EXTRA_PROFILE_NAME, profile.name)
            }
            try {
                app.startService(intent)
            } catch (e: IllegalStateException) {
                Log.e("VpnViewModel", "startService failed (app in background): ${e.message}")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    fun stopVpn(cancelRecovery: Boolean = false) {
        selectionJob?.cancel()
        if (cancelRecovery) {
            val app = getApplication<Application>()
            try {
                app.startService(Intent(app, FlareVpnService::class.java).apply {
                    action = FlareVpnService.ACTION_STOP_MONITORING
                })
            } catch (_: IllegalStateException) { }
        }
        _connectionState.value = ConnectionState.DISCONNECTING
        handleDisconnection()
        val app = getApplication<Application>()
        try {
            app.startService(Intent(app, FlareVpnService::class.java).apply { action = FlareVpnService.ACTION_STOP })
        } catch (e: IllegalStateException) {
            Log.e("VpnViewModel", "stopService failed (app in background): ${e.message}")
        }
    }

    fun toggleProfileInChain(profileId: Long) {
        viewModelScope.launch {
            if (_selectedProfileId.value == profileId) {
                return@launch
            }

            val currentList = _chainedProfileIds.value.toMutableList()
            if (currentList.contains(profileId)) {
                currentList.remove(profileId)
            } else {
                currentList.add(profileId)
            }
            _chainedProfileIds.value = currentList
            val app = getApplication<Application>()
            val settings = SettingsManager(app)
            settings.chainedProfileIdsString = currentList.joinToString(",")

            if (_selectedProfileId.value == null && currentList.isNotEmpty()) {
                val firstId = currentList.first()
                currentList.remove(firstId)
                _chainedProfileIds.value = currentList
                settings.chainedProfileIdsString = currentList.joinToString(",")
                selectProfile(firstId)
            }
        }
    }

    fun handleDisconnection() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        if (settings.isResetChainOnDisconnect) {
            _chainedProfileIds.value = emptyList()
            settings.chainedProfileIdsString = ""
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
