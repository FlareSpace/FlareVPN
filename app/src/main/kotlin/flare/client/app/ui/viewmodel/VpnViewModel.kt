package flare.client.app.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.R
import flare.client.app.data.SettingsManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.model.PingState
import flare.client.app.data.repository.ProfileRepository
import flare.client.app.util.PingHelper
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.supervisorScope
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
    private var healthCheckJob: kotlinx.coroutines.Job? = null
    private var recoveryJob: kotlinx.coroutines.Job? = null
    private var bestProfileJob: kotlinx.coroutines.Job? = null

    private var isReceiverRegistered = false

    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FlareVpnService.BROADCAST_STATE) {
                val connected = intent.getBooleanExtra(FlareVpnService.EXTRA_CONNECTED, false)
                val hasError = intent.getBooleanExtra(FlareVpnService.EXTRA_ERROR, false)
                val permissionRequired = intent.getBooleanExtra(FlareVpnService.EXTRA_PERMISSION_REQUIRED, false)

                if (connected) {
                    _connectionState.value = ConnectionState.CONNECTED
                    startHealthCheckJob()
                    startBestProfileJob()
                } else {
                    if (_connectionState.value == ConnectionState.CONNECTING && !hasError) {
                        return
                    }
                    _connectionState.value = ConnectionState.DISCONNECTED
                    handleDisconnection()
                    startBestProfileJob()
                    if (hasError) {
                        val settings = SettingsManager(context)
                        if (settings.isAdaptiveTunnelEnabled) {
                            startRecovery()
                        } else {
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
            startHealthCheckJob()
        }

        viewModelScope.launch {
            repository.getAllProfiles().collect { profiles ->
                _selectedProfileId.value = profiles.find { it.isSelected }?.id
            }
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

        startBestProfileJob()
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
        recoveryJob?.cancel()
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
        viewModelScope.launch {
            selectBestProfile()
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
            app.startService(intent)
        }
    }

    fun stopVpn(cancelRecovery: Boolean = false) {
        selectionJob?.cancel()
        if (cancelRecovery) recoveryJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTING
        handleDisconnection()
        val app = getApplication<Application>()
        app.startService(Intent(app, FlareVpnService::class.java).apply { action = FlareVpnService.ACTION_STOP })
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
        healthCheckJob?.cancel()
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

    private fun startHealthCheckJob() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        healthCheckJob?.cancel()
        if (!settings.isAdaptiveTunnelEnabled) return

        healthCheckJob = viewModelScope.launch(Dispatchers.IO) {
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            while (isActive) {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    val url = settings.pingTestUrl
                    try {
                        val proxyTag = java.net.URLEncoder.encode(flare.client.app.singbox.SingBoxManager.primaryProxyTag, "UTF-8")
                        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                        val checkUrl = "http://127.0.0.1:9092/proxies/$proxyTag/delay?url=$encodedUrl&timeout=10000"
                        val secret = flare.client.app.singbox.SingBoxManager.clashSecret
                        val request = okhttp3.Request.Builder()
                            .url(checkUrl)
                            .apply {
                                if (secret.isNotEmpty()) {
                                    header("Authorization", "Bearer $secret")
                                }
                            }
                            .build()
                        var isWorking = false
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                val delay = org.json.JSONObject(body).optInt("delay", -1)
                                if (delay > 0) {
                                    isWorking = true
                                }
                            }
                        }

                        if (!isWorking) {
                            Log.w("VpnViewModel", "Active Health Check failed: Proxy returned timeout or error")
                            startRecovery()
                        }
                    } catch (e: Exception) {
                        Log.e("VpnViewModel", "Active Health Check failed: Could not reach Clash API", e)
                        startRecovery()
                    }
                }
                delay(20000L) 
            }
        }
    }

    private fun startRecovery() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        if (!settings.isAdaptiveTunnelEnabled) return
        
        if (recoveryJob?.isActive == true) return
        
        recoveryJob = viewModelScope.launch {
            Log.i("VpnViewModel", "Starting adaptive tunnel recovery...")
            val selectedId = _selectedProfileId.value ?: return@launch
            
            stopVpn()
            delay(1000)
            startVpn()
            
            val connectDeadline = SystemClock.elapsedRealtime() + 10_000L
            while (SystemClock.elapsedRealtime() < connectDeadline) {
                if (_connectionState.value == ConnectionState.CONNECTED) break
                delay(500)
            }
            
            if (_connectionState.value == ConnectionState.CONNECTED) {
                delay(2000) 
                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val isWorking = withContext(Dispatchers.IO) {
                    try {
                        val proxyTag = java.net.URLEncoder.encode(flare.client.app.singbox.SingBoxManager.primaryProxyTag, "UTF-8")
                        val encodedUrl = java.net.URLEncoder.encode(settings.pingTestUrl, "UTF-8")
                        val checkUrl = "http://127.0.0.1:9092/proxies/$proxyTag/delay?url=$encodedUrl&timeout=10000"
                        val secret = flare.client.app.singbox.SingBoxManager.clashSecret
                        val request = okhttp3.Request.Builder()
                            .url(checkUrl)
                            .apply {
                                if (secret.isNotEmpty()) {
                                    header("Authorization", "Bearer $secret")
                                }
                            }
                            .build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                val delay = org.json.JSONObject(body).optInt("delay", -1)
                                delay > 0
                            } else {
                                false
                            }
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                if (isWorking) {
                    Log.i("VpnViewModel", "Recovery successful with current profile.")
                    return@launch
                }
            }
            
            Log.i("VpnViewModel", "Current profile failed during recovery. Finding best profile...")
            val allProfiles = repository.getAllProfiles().first()
            val currentProfile = allProfiles.find { it.id == selectedId } ?: return@launch
            val subId = currentProfile.subscriptionId ?: return@launch
            val profiles = allProfiles.filter { it.subscriptionId == subId }
            if (profiles.size <= 1) return@launch

            selectBestProfile()

            val newSelectedId = _selectedProfileId.value
            if (newSelectedId != null && newSelectedId != selectedId) {
                Log.i("VpnViewModel", "Switching to best profile: $newSelectedId during recovery.")
                delay(500)
                startVpn()
            } else {
                Log.w("VpnViewModel", "No working alternative profile found in current subscription.")
            }
        }
    }

    private suspend fun selectBestProfile() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        val selectedId = _selectedProfileId.value ?: return
        val allProfiles = repository.getAllProfiles().first()
        val selectedProfile = allProfiles.find { it.id == selectedId } ?: return
        val subId = selectedProfile.subscriptionId

        val profiles = allProfiles.filter { it.subscriptionId == subId }
        if (profiles.size <= 1) return

        val localPings = java.util.concurrent.ConcurrentHashMap<Long, PingState>()
        val loadingPings = profiles.associate { it.id to PingState.Loading }
        PingHelper.updatePingStates(loadingPings)

        val fullProfiles = repository.getProfilesByIds(profiles.map { it.id })
        val isProxy = settings.pingType.startsWith("via")

        if (!isProxy) {
            val method = if (settings.pingType == "TCP") "TCP" else "ICMP"
            supervisorScope {
                fullProfiles.forEach { profile ->
                    launch(Dispatchers.IO) {
                        try {
                            val (latency, error) = PingHelper.pingDirect(profile, method, settings.pingTimeout)
                            val state = PingState.Result(latency, latency < 0, error)
                            localPings[profile.id] = state
                            PingHelper.updatePingState(profile.id, state)
                        } catch (e: Exception) {
                            val state = PingState.Result(-1, true, e.message ?: "unknown error")
                            localPings[profile.id] = state
                            PingHelper.updatePingState(profile.id, state)
                        }
                    }
                }
            }
        } else {
            val isVpnRunning = flare.client.app.singbox.SingBoxManager.isRunning
            if (isVpnRunning) {
                supervisorScope {
                    PingHelper.pingProxyBatch(
                        context = app,
                        profiles = fullProfiles,
                        testUrl = settings.pingTestUrl,
                        timeoutSec = settings.pingTimeout
                    ) { id, latency, error ->
                        val state = PingState.Result(latency, latency < 0, error)
                        localPings[id] = state
                        PingHelper.updatePingState(id, state)
                    }
                }
            } else {
                supervisorScope {
                    fullProfiles.forEach { profile ->
                        launch(Dispatchers.IO) {
                            try {
                                val (latency, error) = PingHelper.pingDirect(profile, "TCP", settings.pingTimeout)
                                val state = PingState.Result(latency, latency < 0, error)
                                localPings[profile.id] = state
                                PingHelper.updatePingState(profile.id, state)
                            } catch (e: Exception) {
                                val state = PingState.Result(-1, true, e.message ?: "unknown error")
                                localPings[profile.id] = state
                                PingHelper.updatePingState(profile.id, state)
                            }
                        }
                    }
                }
            }
        }

        val deadline = SystemClock.elapsedRealtime() + 15_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val allDone = profiles.all { p ->
                localPings[p.id] is PingState.Result
            }
            if (allDone) break
            delay(500)
        }

        val bestPair = profiles
            .mapNotNull { p ->
                val state = localPings[p.id]
                if (state is PingState.Result && !state.isError && state.latency >= 0) {
                    p to state.latency
                } else null
            }
            .minByOrNull { it.second }

        val best = bestPair?.first
        if (best != null && best.id != _selectedProfileId.value) {
            selectProfile(best.id)
        }
    }

    fun startBestProfileJob() {
        bestProfileJob?.cancel()
        bestProfileJob = viewModelScope.launch {
            val settings = SettingsManager(getApplication())
            if (!settings.isBestProfileEnabled) return@launch
            while (isActive) {
                val shouldRun = if (settings.isBestProfileOnlyIfConnected) {
                    _connectionState.value == ConnectionState.CONNECTED
                } else true
                if (shouldRun) {
                    selectBestProfile()
                }
                
                val rawInterval = settings.bestProfileInterval.toLongOrNull() ?: 1800L
                val interval = if (rawInterval < 10L) 10L else rawInterval
                delay(interval * 1000L)
            }
        }
    }
}
