package flare.client.app.ui

import flare.client.app.ui.i18n.I18n

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.model.DisplayItem
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.model.ProfileSummary
import flare.client.app.data.model.SubscriptionEntity
import flare.client.app.data.model.PingState
import flare.client.app.data.parser.ClipboardParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import flare.client.app.data.repository.ProfileRepository
import flare.client.app.data.SettingsManager
import flare.client.app.service.FlareVpnService
import flare.client.app.R
import android.util.Log
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withLock

data class RoutingRuleState(
    val id: String,
    val title: () -> String,
    val description: (() -> String)? = null,
    val isEnabled: Boolean,
    val mode: String,
    val lastUpdate: Long,
    val isBuiltin: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val fileNames: List<String>,
    val urls: List<String>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppDatabase.getInstance(application)
    }
    private val repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProfileRepository(db.profileDao(), db.subscriptionDao())
    }

    companion object {
        private const val VIRTUAL_SUB_ID = -1L
    }

    private var selectionJob: kotlinx.coroutines.Job? = null
    private val expandedSubs = MutableStateFlow<Set<Long>>(emptySet())
    private val _refreshingSubs = MutableStateFlow<Set<Long>>(emptySet())
    val refreshingSubs: StateFlow<Set<Long>> = _refreshingSubs.asStateFlow()

    private var autoUpdateJob: kotlinx.coroutines.Job? = null
    private var healthCheckJob: kotlinx.coroutines.Job? = null
    private var recoveryJob: kotlinx.coroutines.Job? = null

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING }
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _routingRules = MutableStateFlow<List<RoutingRuleState>>(emptyList())
    val routingRules: StateFlow<List<RoutingRuleState>> = _routingRules.asStateFlow()

    private val _pingStates = MutableStateFlow<Map<Long, PingState>>(emptyMap())

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    private val _chainedProfileIds = MutableStateFlow<List<Long>>(emptyList())
    val chainedProfileIds: StateFlow<List<Long>> = _chainedProfileIds.asStateFlow()

    private val _editingProfile = MutableStateFlow<ProfileEntity?>(null)
    val editingProfile: StateFlow<ProfileEntity?> = _editingProfile.asStateFlow()

    private val _editingSubscription = MutableStateFlow<SubscriptionEntity?>(null)
    val editingSubscription: StateFlow<SubscriptionEntity?> = _editingSubscription.asStateFlow()

    private val _importEvent = MutableSharedFlow<ImportEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val importEvent: SharedFlow<ImportEvent> = _importEvent.asSharedFlow()

    private val _displayItems = MutableStateFlow<List<DisplayItem>>(emptyList())
    val displayItems: StateFlow<List<DisplayItem>> = _displayItems.asStateFlow()

    private val _isStartupLoading = MutableStateFlow(true)
    val isStartupLoading: StateFlow<Boolean> = _isStartupLoading.asStateFlow()

    val isAnySubscriptionExpanded: StateFlow<Boolean> = _displayItems
        .map { items ->
            items.filterIsInstance<DisplayItem.SubscriptionItem>().any { it.isExpanded }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    data class NoticeState(
        val id: Int = 0,
        val needsToShow: Boolean = false,
        val titleRu: String = "",
        val titleEn: String = "",
        val textRu: String = "",
        val textEn: String = "",
        val actionTextRu: String = "",
        val actionTextEn: String = "",
        val actionUrl: String = ""
    )

    private val _noticeState = MutableStateFlow(NoticeState())
    val noticeState: StateFlow<NoticeState> = _noticeState.asStateFlow()

    private var noticeCheckJob: kotlinx.coroutines.Job? = null

    private val initMutex = kotlinx.coroutines.sync.Mutex()
    @Volatile
    private var isInitialized = false
    private var isReceiverRegistered = false
    private var displayItemsJob: kotlinx.coroutines.Job? = null

    sealed class ImportEvent {
        object Loading : ImportEvent()
        data class Success(val message: String) : ImportEvent()
        data class Error(val message: String) : ImportEvent()
        data class NeedPermission(val intent: Intent) : ImportEvent()
    }

    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FlareVpnService.BROADCAST_STATE) {
                val connected = intent.getBooleanExtra(FlareVpnService.EXTRA_CONNECTED, false)
                val hasError = intent.getBooleanExtra(FlareVpnService.EXTRA_ERROR, false)
                val permissionRequired = intent.getBooleanExtra(FlareVpnService.EXTRA_PERMISSION_REQUIRED, false)
                
                if (connected) {
                    _connectionState.value = ConnectionState.CONNECTED
                    startHealthCheckJob()
                } else {
                    
                    
                    if (_connectionState.value == ConnectionState.CONNECTING && !hasError) {
                        return
                    }
                    _connectionState.value = ConnectionState.DISCONNECTED
                    handleDisconnection()
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
                            flare.client.app.ui.notification.AppNotificationManager.showNotification(flare.client.app.ui.notification.NotificationType.ERROR, errorMsg, 4)
                        }
                    }
                }
            }
        }
    }

    fun initializeAsync() {
        if (isInitialized) return
        viewModelScope.launch {
            ensureInitialized()
        }
    }

    private suspend fun ensureInitialized() {
        if (isInitialized) return
        var shouldAutostart = false
        initMutex.withLock {
            if (isInitialized) return@withLock

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

            try {
                val jsonProfiles = repository.getJsonProfiles()
                for (profile in jsonProfiles) {
                    val currentDesc = profile.serverDescription
                    val newDesc = flare.client.app.data.parser.ProfileParsingHelper.parseTransportAndSecurityFromJson(profile.configJson)
                    if (newDesc != currentDesc) {
                        repository.updateProfile(profile.id, profile.name, profile.configJson, profile.protocol, newDesc)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to auto-repair JSON profiles server description: ${e.message}")
            }

            displayItemsJob?.cancel()
            displayItemsJob = combine(
                repository.getAllSubscriptions(),
                repository.getAllProfiles(),
                expandedSubs,
                _selectedProfileId,
                _pingStates,
                _refreshingSubs
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val subs = args[0] as List<SubscriptionEntity>
                val allProfiles = args[1] as List<ProfileSummary>
                val expanded = args[2] as Set<Long>
                val selId = args[3] as Long?
                val pings = args[4] as Map<Long, PingState>
                val refreshing = args[5] as Set<Long>

                val profilesBySub = allProfiles.groupBy { it.subscriptionId }
                val standalone = allProfiles.filter { it.subscriptionId == null }
                buildDisplayList(subs, standalone, profilesBySub, expanded, selId, pings, refreshing)
            }
                .onEach { items ->
                    _displayItems.value = items
                    _isStartupLoading.value = false
                }
                .launchIn(viewModelScope)

            if (flare.client.app.singbox.SingBoxManager.isRunning) {
                _connectionState.value = ConnectionState.CONNECTED
                startHealthCheckJob()
            } else if (settings.isAutostartEnabled) {
                shouldAutostart = true
            }

            viewModelScope.launch {
                _selectedProfileId.value = repository.getSelectedProfile()?.id
            }

            val chainedIdsStr = settings.chainedProfileIdsString
            if (chainedIdsStr.isNotBlank()) {
                _chainedProfileIds.value = chainedIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
            }

            if (settings.needsToShowNotice) {
                _noticeState.value = NoticeState(
                    id = settings.noticeId,
                    needsToShow = true,
                    titleRu = settings.noticeTitleRu,
                    titleEn = settings.noticeTitleEn,
                    textRu = settings.noticeTextRu,
                    textEn = settings.noticeTextEn,
                    actionTextRu = settings.noticeActionTextRu,
                    actionTextEn = settings.noticeActionTextEn,
                    actionUrl = settings.noticeActionUrl
                )
            }

            startAutoUpdateJob()
            startBestProfileJob()
            startUpdateCheckJob()
            startNoticeCheckJob()
            initRoutingRules()
            isInitialized = true
        }
        if (shouldAutostart) {
            val profile = repository.getSelectedProfile()
            if (profile != null) {
                startVpn()
            }
        }
    }

    private fun initRoutingRules() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        
        _routingRules.value = listOf(
            RoutingRuleState(
                id = "main",
                title = { I18n.strings.routing_card_ru },
                description = { "geoip-ru · geosite-ru" },
                isEnabled = settings.isRoutingMainEnabled,
                mode = settings.routingMainMode,
                lastUpdate = settings.lastRoutingUpdateMain,
                isBuiltin = true,
                fileNames = listOf("geoip-ru.srs", "geosite-ru.srs"),
                urls = listOf(
                    "https://github.com/SagerNet/sing-geoip/raw/rule-set/geoip-ru.srs",
                    "https://github.com/SagerNet/sing-geosite/raw/rule-set/geosite-category-ru.srs"
                )
            ),
            RoutingRuleState(
                id = "global",
                title = { I18n.strings.routing_card_global },
                description = { I18n.strings.routing_card_global_desc },
                isEnabled = settings.isRoutingGlobalEnabled,
                mode = settings.routingGlobalMode,
                lastUpdate = settings.lastRoutingUpdateGlobal,
                fileNames = listOf("geosite-global.srs"),
                urls = listOf("https://github.com/SagerNet/sing-geosite/raw/rule-set/geosite-geolocation-!cn.srs")
            ),
            RoutingRuleState(
                id = "media",
                title = { I18n.strings.routing_card_media },
                description = { I18n.strings.routing_card_media_desc },
                isEnabled = settings.isRoutingMediaEnabled,
                mode = settings.routingMediaMode,
                lastUpdate = settings.lastRoutingUpdateMedia,
                fileNames = listOf("geosite-youtube.srs", "geosite-netflix.srs", "geosite-twitch.srs", "geosite-disney.srs"),
                urls = listOf("geosite-youtube.srs", "geosite-netflix.srs", "geosite-twitch.srs", "geosite-disney.srs").map { "https://github.com/SagerNet/sing-geosite/raw/rule-set/$it" }
            ),
            RoutingRuleState(
                id = "social",
                title = { I18n.strings.routing_card_social },
                description = { I18n.strings.routing_card_social_desc },
                isEnabled = settings.isRoutingSocialEnabled,
                mode = settings.routingSocialMode,
                lastUpdate = settings.lastRoutingUpdateSocial,
                fileNames = listOf("geosite-telegram.srs", "geosite-facebook.srs", "geosite-instagram.srs", "geosite-twitter.srs", "geosite-tiktok.srs"),
                urls = listOf("geosite-telegram.srs", "geosite-facebook.srs", "geosite-instagram.srs", "geosite-twitter.srs", "geosite-tiktok.srs").map { "https://github.com/SagerNet/sing-geosite/raw/rule-set/$it" }
            ),
            RoutingRuleState(
                id = "ads",
                title = { I18n.strings.routing_card_ads },
                description = { I18n.strings.routing_card_ads_desc },
                isEnabled = settings.isRoutingAdsEnabled,
                mode = settings.routingAdsMode,
                lastUpdate = settings.lastRoutingUpdateAds,
                fileNames = listOf("geosite-ads.srs"),
                urls = listOf("https://github.com/SagerNet/sing-geosite/raw/rule-set/geosite-category-ads-all.srs")
            ),
            RoutingRuleState(
                id = "cn",
                title = { I18n.strings.routing_card_cn },
                description = { I18n.strings.routing_card_cn_desc },
                isEnabled = settings.isRoutingCnEnabled,
                mode = settings.routingCnMode,
                lastUpdate = settings.lastRoutingUpdateCn,
                fileNames = listOf("geoip-cn.srs", "geosite-cn.srs"),
                urls = listOf(
                    "https://github.com/SagerNet/sing-geoip/raw/rule-set/geoip-cn.srs",
                    "https://github.com/SagerNet/sing-geosite/raw/rule-set/geosite-cn.srs"
                )
            )
        )
    }

    fun toggleRoutingRule(ruleId: String, enabled: Boolean) {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        when (ruleId) {
            "main" -> settings.isRoutingMainEnabled = enabled
            "global" -> settings.isRoutingGlobalEnabled = enabled
            "media" -> settings.isRoutingMediaEnabled = enabled
            "social" -> settings.isRoutingSocialEnabled = enabled
            "ads" -> settings.isRoutingAdsEnabled = enabled
            "cn" -> settings.isRoutingCnEnabled = enabled
        }
        _routingRules.update { list ->
            list.map { if (it.id == ruleId) it.copy(isEnabled = enabled) else it }
        }
    }

    fun setRoutingRuleMode(ruleId: String, mode: String) {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        when (ruleId) {
            "main" -> settings.routingMainMode = mode
            "global" -> settings.routingGlobalMode = mode
            "media" -> settings.routingMediaMode = mode
            "social" -> settings.routingSocialMode = mode
            "ads" -> settings.routingAdsMode = mode
            "cn" -> settings.routingCnMode = mode
        }
        _routingRules.update { list ->
            list.map { if (it.id == ruleId) it.copy(mode = mode) else it }
        }
    }

    fun downloadRoutingRule(ruleId: String) {
        val app = getApplication<Application>()
        val rule = _routingRules.value.find { it.id == ruleId } ?: return
        if (rule.isDownloading) return

        _routingRules.update { list ->
            list.map { if (it.id == ruleId) it.copy(isDownloading = true, progress = 0) else it }
        }

        val fileNames = rule.fileNames
        val urls = rule.urls
        val totalFiles = fileNames.size
        val progressMap = java.util.concurrent.ConcurrentHashMap<Int, Int>()
        val completedFiles = java.util.concurrent.atomic.AtomicInteger(0)
        val hasError = java.util.concurrent.atomic.AtomicBoolean(false)

        for (i in 0 until totalFiles) {
            flare.client.app.singbox.GeoFileManager.downloadFile(
                app,
                urls[i],
                fileNames[i],
                onProgress = { p ->
                    if (hasError.get()) return@downloadFile
                    progressMap[i] = p
                    val totalProgress = if (totalFiles > 0) progressMap.values.sum() / totalFiles else 0
                    _routingRules.update { list ->
                        list.map { if (it.id == ruleId) it.copy(progress = totalProgress) else it }
                    }
                },
                onSuccess = {
                    if (hasError.get()) return@downloadFile
                    if (completedFiles.incrementAndGet() == totalFiles) {
                        val now = System.currentTimeMillis()
                        val settings = SettingsManager(app)
                        viewModelScope.launch {
                            when (ruleId) {
                                "main" -> settings.lastRoutingUpdateMain = now
                                "global" -> settings.lastRoutingUpdateGlobal = now
                                "media" -> settings.lastRoutingUpdateMedia = now
                                "social" -> settings.lastRoutingUpdateSocial = now
                                "ads" -> settings.lastRoutingUpdateAds = now
                                "cn" -> settings.lastRoutingUpdateCn = now
                            }
                            _routingRules.update { list ->
                                list.map { if (it.id == ruleId) it.copy(isDownloading = false, progress = 100, lastUpdate = now) else it }
                            }
                            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                                flare.client.app.ui.notification.NotificationType.SUCCESS,
                                I18n.strings.routing_success_generic.format(ruleId.uppercase()), 2
                            )
                        }
                    }
                },
                onError = { err ->
                    if (hasError.compareAndSet(false, true)) {
                        viewModelScope.launch {
                            _routingRules.update { list ->
                                list.map { if (it.id == ruleId) it.copy(isDownloading = false, progress = 0) else it }
                            }
                            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                                flare.client.app.ui.notification.NotificationType.ERROR,
                                I18n.strings.error_downloading_rule.format(ruleId.uppercase(), err), 3
                            )
                        }
                    }
                }
            )
        }
    }

    init {
        initializeAsync()
    }

    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        if (isReceiverRegistered) {
            app.unregisterReceiver(vpnReceiver)
            isReceiverRegistered = false
        }
    }

    fun toggleSubscriptionExpanded(subId: Long) = expandedSubs.update { if (subId in it) it - subId else it + subId }
    fun collapseAllSubscriptions() { expandedSubs.value = emptySet() }
    fun toggleSubscriptionPinned(subId: Long) {
        viewModelScope.launch {
            ensureInitialized()
            val app = getApplication<Application>()
            val settings = SettingsManager(app)
            if (subId == VIRTUAL_SUB_ID) {
                val wasPinned = settings.isVirtualSubscriptionPinned
                if (wasPinned) {
                    settings.isVirtualSubscriptionPinned = false
                    settings.virtualSubscriptionPinnedTime = 0L
                } else {
                    settings.isVirtualSubscriptionPinned = true
                    settings.virtualSubscriptionPinnedTime = System.currentTimeMillis()
                }
            } else {
                val subs = repository.getAllSubscriptions().first()
                val targetSub = subs.find { it.id == subId } ?: return@launch
                val wasPinned = targetSub.pinned > 0L
                val newPinValue = if (wasPinned) 0L else System.currentTimeMillis()
                repository.updateSubscriptionPinned(subId, newPinValue)
            }
        }
    }
    fun selectProfile(profileId: Long) {
        selectionJob?.cancel()
        recoveryJob?.cancel()
        selectionJob = viewModelScope.launch {
            ensureInitialized()
            
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
                Log.e("MainViewModel", "Failed to update widget: ${e.message}")
            }
            
            if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
                delay(300)
                startVpn()
            }
        }
     }
    fun deleteSubscription(subId: Long) {
        val subName = if (subId == VIRTUAL_SUB_ID) {
            I18n.strings.sub_single_profiles
        } else {
            val rawName = displayItems.value.filterIsInstance<DisplayItem.SubscriptionItem>().find { it.entity.id == subId }?.entity?.name ?: I18n.strings.label_unknown
            if (I18n.isMyServers(rawName)) {
                I18n.strings.sub_my_servers
            } else {
                rawName
            }
        }
        viewModelScope.launch {
            ensureInitialized()
            if (subId == VIRTUAL_SUB_ID) {
                repository.deleteStandaloneProfiles()
            } else {
                repository.deleteSubscriptionById(subId)
            }
            expandedSubs.update { it - subId }
            if (repository.getSelectedProfile() == null) _selectedProfileId.value = null
            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.SUCCESS,
                I18n.strings.sub_deleted_success.format(subName),
                3
            )
        }
    }
    fun speedTestSubscription(subId: Long) {
        viewModelScope.launch {
            ensureInitialized()
            val profiles = if (subId == VIRTUAL_SUB_ID) {
                repository.getAllProfiles().first().filter { it.subscriptionId == null }
            } else {
                repository.getAllProfiles().first().filter { it.subscriptionId == subId }
            }
            if (profiles.isEmpty()) return@launch
            speedTestProfile(profiles)
        }
    }

    fun speedTestProfile(profiles: List<ProfileSummary>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPings = _pingStates.value.toMutableMap()
            profiles.forEach { profile ->
                currentPings[profile.id] = PingState.Loading
            }
            _pingStates.value = currentPings

            val app = getApplication<Application>()
            val settings = SettingsManager(app)
            val isProxy = settings.pingType.startsWith("via")

            val fullProfiles = repository.getProfilesByIds(profiles.map { it.id })

            if (!isProxy) {
                val method = if (settings.pingType == "TCP") "TCP" else "ICMP"
                fullProfiles.forEach { profile ->
                    launch {
                        val (latency, error) = flare.client.app.util.PingHelper.pingDirect(profile, method)
                        _pingStates.update { it.toMutableMap().apply {
                            this[profile.id] = PingState.Result(latency, latency < 0, error)
                        } }
                    }
                }
            } else {
                flare.client.app.util.PingHelper.pingProxyBatch(
                    context = app,
                    profiles = fullProfiles,
                    testUrl = settings.pingTestUrl
                ) { id, latency, error ->
                    _pingStates.update { it.toMutableMap().apply {
                        this[id] = PingState.Result(latency, latency < 0, error)
                    } }
                }
            }
        }
    }
    fun showSubscriptionOptions(subId: Long) {}
    fun setEditingProfile(p: ProfileEntity?) { _editingProfile.value = p; _editingSubscription.value = null }
    fun setEditingSubscription(s: SubscriptionEntity?) { _editingSubscription.value = s; _editingProfile.value = null }
    suspend fun getProfileById(id: Long): ProfileEntity? = repository.getProfileById(id)

    fun fetchProfileForEditing(id: Long) {
        viewModelScope.launch {
            ensureInitialized()
            val profile = repository.getProfileById(id)
            _editingProfile.value = profile
        }
    }
    fun updateProfileConfig(id: Long, json: String) { viewModelScope.launch(Dispatchers.IO) { ensureInitialized(); repository.updateProfileConfig(id, json) } }
    fun updateProfile(id: Long, name: String, json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ensureInitialized()
            val protocol = try {
                val outbounds = org.json.JSONObject(json).optJSONArray("outbounds")
                outbounds?.optJSONObject(0)?.optString("type")
            } catch (_: Exception) { null }
            val desc = flare.client.app.data.parser.ProfileParsingHelper.parseTransportAndSecurityFromJson(json)
            repository.updateProfile(id, name, json, protocol, desc)
        }
    }
    fun updateProfileFull(profile: ProfileEntity) { viewModelScope.launch(Dispatchers.IO) { ensureInitialized(); repository.updateProfileFull(profile) } }
    fun deleteProfile(id: Long, name: String) {
        viewModelScope.launch {
            ensureInitialized()
            val wasSelected = _selectedProfileId.value == id
            repository.deleteProfile(id)
            if (wasSelected) {
                _selectedProfileId.value = null
                if (_connectionState.value != ConnectionState.DISCONNECTED) {
                    stopVpn(true)
                }
            }
            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.SUCCESS,
                I18n.strings.profile_deleted_success.format(name),
                3
            )
        }
    }
    fun updateSubscription(id: Long, name: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ensureInitialized()
            repository.updateSubscription(id, name, url)
        }
    }
    fun connectOrDisconnect() = if (_connectionState.value != ConnectionState.DISCONNECTED) stopVpn(true) else startVpn()
    fun startVpnFromUi() = startVpn()
    fun stopVpnFromUi(cancelRecovery: Boolean = true) = stopVpn(cancelRecovery)

    fun pingCurrentSubscription() {
        viewModelScope.launch {
            ensureInitialized()
            val selectedProfile = repository.getSelectedProfile()
            val subId = selectedProfile?.subscriptionId ?: VIRTUAL_SUB_ID
            speedTestSubscription(subId)
        }
    }

    fun selectBestProfileFromUi() {
        viewModelScope.launch {
            ensureInitialized()
            selectBestProfile()
        }
    }

    fun toggleProfileInChain(profileId: Long) {
        viewModelScope.launch {
            ensureInitialized()
            
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
    fun importFromClipboard(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ensureInitialized()
            _importEvent.emit(ImportEvent.Loading)
            try {
                val app = getApplication<Application>()
                val settings = SettingsManager(app)
                val hwid = if (settings.isHwidEnabled) getHwid() else null
                val model = android.os.Build.MODEL
                val osVersion = android.os.Build.VERSION.RELEASE
                
                kotlinx.coroutines.withTimeout(10000L) {
                    when (val result = ClipboardParser.parse(app, text, hwid, model, osVersion, settings.subUserAgent)) {
                        is ClipboardParser.ParseResult.SingleProfile -> {
                            repository.insertProfile(result.profile)
                            _importEvent.emit(ImportEvent.Success(I18n.strings.success_profile_added.format(result.profile.name)))
                        }
                        is ClipboardParser.ParseResult.MultipleProfiles -> {
                            result.profiles.forEach { profile ->
                                repository.insertProfile(profile)
                            }
                            _importEvent.emit(
                                ImportEvent.Success(
                                    I18n.strings.success_profiles_added.format(result.profiles.size
                                    )
                                )
                            )
                        }
                        is ClipboardParser.ParseResult.Subscription -> {
                            repository.insertSubscriptionWithProfiles(result.subscription, result.profiles)
                            _importEvent.emit(ImportEvent.Success(I18n.strings.success_subscription_added.format(result.subscription.name)))
                        }
                        is ClipboardParser.ParseResult.Error -> {
                            _importEvent.emit(ImportEvent.Error(result.message))
                        }
                        else -> {
                            _importEvent.emit(ImportEvent.Error(I18n.strings.error_import_failed))
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _importEvent.emit(ImportEvent.Error(I18n.strings.error_import_timeout))
            } catch (e: Exception) {
                _importEvent.emit(ImportEvent.Error(I18n.strings.error_import_failed))
            }
        }
    }

    private fun startVpn() {
        viewModelScope.launch {
            ensureInitialized()
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
            if (vpnIntent != null) { _importEvent.emit(ImportEvent.NeedPermission(vpnIntent)); return@launch }
            _connectionState.value = ConnectionState.CONNECTING
            val intent = Intent(app, FlareVpnService::class.java).apply {
                action = FlareVpnService.ACTION_START
                putExtra(FlareVpnService.EXTRA_CONFIG, configWithSettings)
                putExtra(FlareVpnService.EXTRA_PROFILE_NAME, profile.name)
            }
            app.startService(intent)
        }
    }

    private fun stopVpn(cancelRecovery: Boolean = false) {
        selectionJob?.cancel()
        if (cancelRecovery) recoveryJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTING
        handleDisconnection()
        val app = getApplication<Application>()
        app.startService(Intent(app, FlareVpnService::class.java).apply { action = FlareVpnService.ACTION_STOP })
    }


    private fun buildDisplayList(subs: List<SubscriptionEntity>, standalone: List<ProfileSummary>, profilesBySub: Map<Long?, List<ProfileSummary>>, expanded: Set<Long>, selId: Long?, pings: Map<Long, PingState>, refreshing: Set<Long>): List<DisplayItem> {
        val settings = SettingsManager(getApplication())
        val allSubs = subs.toMutableList()
        if (standalone.isNotEmpty()) {
            val virtualSub = SubscriptionEntity(
                id = VIRTUAL_SUB_ID,
                name = I18n.strings.sub_single_profiles,
                url = "",
                pinned = if (settings.isVirtualSubscriptionPinned) settings.virtualSubscriptionPinnedTime else 0L
            )
            allSubs.add(virtualSub)
        }

        val sortedSubs = allSubs.sortedWith { s1, s2 ->
            val p1 = s1.pinned
            val p2 = s2.pinned
            val isP1 = p1 > 0
            val isP2 = p2 > 0
            if (isP1 && isP2) {
                p1.compareTo(p2)
            } else if (isP1) {
                -1
            } else if (isP2) {
                1
            } else {
                if (s1.id == VIRTUAL_SUB_ID) 1
                else if (s2.id == VIRTUAL_SUB_ID) -1
                else s1.id.compareTo(s2.id)
            }
        }

        val actualExpanded = mutableSetOf<Long>()
        val items = mutableListOf<DisplayItem>()

        sortedSubs.forEach { sub ->
            if (sub.id == VIRTUAL_SUB_ID) {
                val isExpanded = VIRTUAL_SUB_ID in expanded
                if (isExpanded) {
                    actualExpanded.add(VIRTUAL_SUB_ID)
                }
                val isRefreshing = VIRTUAL_SUB_ID in refreshing
                items += DisplayItem.SubscriptionItem(sub, standalone, isExpanded, isRefreshing, if (isExpanded) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL)
                if (isExpanded) {
                    standalone.forEachIndexed { i, p ->
                        items += DisplayItem.ProfileItem(p, p.id == selId, pings[p.id] ?: PingState.None, if (i == standalone.size - 1) DisplayItem.CornerType.BOTTOM else DisplayItem.CornerType.NONE)
                    }
                }
            } else {
                val subProfiles = profilesBySub[sub.id] ?: emptyList()
                val isExpanded = sub.id in expanded
                if (isExpanded) {
                    actualExpanded.add(sub.id)
                }
                val isRefreshing = sub.id in refreshing
                items += DisplayItem.SubscriptionItem(sub, subProfiles, isExpanded, isRefreshing, if (isExpanded) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL)
                if (isExpanded) {
                    subProfiles.forEachIndexed { i, p ->
                        items += DisplayItem.ProfileItem(p, p.id == selId, pings[p.id] ?: PingState.None, if (i == subProfiles.size - 1) DisplayItem.CornerType.BOTTOM else DisplayItem.CornerType.NONE)
                    }
                }
            }
        }

        if (actualExpanded.size < expanded.size) {
            expandedSubs.value = actualExpanded
        }
        return items
    }

    fun startAutoUpdateJob() {
        autoUpdateJob?.cancel()

        autoUpdateJob = viewModelScope.launch {
            ensureInitialized()
            val settings = SettingsManager(getApplication())
            
            while (isActive) {
                if (!settings.isSubAutoUpdateEnabled && !settings.isSubIntervalEnabled) {
                    delay(10000L)
                    continue
                }
                
                if (settings.isSubIntervalEnabled) {
                    try {
                        val subs = repository.getAllSubscriptions().first()
                        val now = System.currentTimeMillis()
                        val toUpdate = mutableListOf<SubscriptionEntity>()
                        var minDelay = 30000L
                        
                        for (sub in subs) {
                            if (sub.updateInterval > 0) {
                                val nextUpdate = sub.lastUpdated + sub.updateInterval * 1000L
                                val delayForSub = nextUpdate - now
                                if (delayForSub <= 0) {
                                    toUpdate.add(sub)
                                } else {
                                    if (delayForSub < minDelay) {
                                        minDelay = delayForSub
                                    }
                                }
                            }
                        }
                        
                        if (toUpdate.isNotEmpty()) {
                            refreshSubscriptions(toUpdate)
                            delay(2000L)
                        } else {
                            val actualDelay = if (minDelay < 5000L) 5000L else minDelay
                            delay(actualDelay)
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Auto-update interval check failed: ${e.message}")
                        delay(30000L)
                    }
                } else if (settings.isSubAutoUpdateEnabled) {
                    val intervalRaw = settings.subAutoUpdateInterval.toLongOrNull() ?: 3600L
                    val interval = if (intervalRaw < 30L) 30L else intervalRaw
                    val lastUpdate = settings.lastSubUpdateTime
                    val now = System.currentTimeMillis()
                    val nextUpdate = lastUpdate + interval * 1000L
                    val delayTime = nextUpdate - now
                    if (delayTime > 0) {
                        val waitTime = if (delayTime > 30000L) 30000L else delayTime
                        delay(waitTime)
                    } else {
                        try {
                            refreshAllSubscriptions()
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Auto-update failed: ${e.message}")
                            delay(10000L)
                        }
                    }
                }
            }
        }
    }

    fun startHealthCheckJob() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        healthCheckJob?.cancel()
        if (!settings.isAdaptiveTunnelEnabled) return

        healthCheckJob = viewModelScope.launch(Dispatchers.IO) {
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            while (isActive) {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    val url = settings.pingTestUrl
                    try {
                        val proxyTag = java.net.URLEncoder.encode(flare.client.app.singbox.SingBoxManager.primaryProxyTag, "UTF-8")
                        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                        val checkUrl = "http://127.0.0.1:9092/proxies/$proxyTag/delay?url=$encodedUrl&timeout=5000"
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
                            Log.w("MainViewModel", "Active Health Check failed: Proxy returned timeout or error")
                            startRecovery()
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Active Health Check failed: Could not reach Clash API", e)
                        startRecovery()
                    }
                }
                delay(20000L) 
            }
        }
    }

    fun stopHealthCheckJob() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }

    private fun startRecovery() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        if (!settings.isAdaptiveTunnelEnabled) return
        
        if (recoveryJob?.isActive == true) return
        
        recoveryJob = viewModelScope.launch {
            Log.i("MainViewModel", "Starting adaptive tunnel recovery...")
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
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val isWorking = withContext(Dispatchers.IO) {
                    try {
                        val proxyTag = java.net.URLEncoder.encode(flare.client.app.singbox.SingBoxManager.primaryProxyTag, "UTF-8")
                        val encodedUrl = java.net.URLEncoder.encode(settings.pingTestUrl, "UTF-8")
                        val checkUrl = "http://127.0.0.1:9092/proxies/$proxyTag/delay?url=$encodedUrl&timeout=5000"
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
                    Log.i("MainViewModel", "Recovery successful with current profile.")
                    return@launch
                }
            }
            
            
            Log.i("MainViewModel", "Current profile failed during recovery. Finding best profile...")
            val allProfiles = repository.getAllProfiles().first()
            val currentProfile = allProfiles.find { it.id == selectedId } ?: return@launch
            val subId = currentProfile.subscriptionId ?: return@launch
            val profiles = allProfiles.filter { it.subscriptionId == subId }
            if (profiles.size <= 1) return@launch
            
            speedTestProfile(profiles)
            
            val deadline = SystemClock.elapsedRealtime() + 15_000L
            while (SystemClock.elapsedRealtime() < deadline) {
                val pings = _pingStates.value
                val allDone = profiles.all { p ->
                    val state = pings[p.id]
                    state is PingState.Result
                }
                if (allDone) break
                delay(500)
            }
            
            val pings = _pingStates.value
            val best = profiles
                .mapNotNull { p ->
                    val state = pings[p.id]
                    if (state is PingState.Result && !state.isError && state.latency >= 0) {
                        p to state.latency
                    } else null
                }
                .minByOrNull { it.second }?.first
                
            if (best != null && best.id != selectedId) {
                Log.i("MainViewModel", "Switching to best profile: ${best.name}")
                selectProfile(best.id)
                
                
                val title = I18n.strings.notif_adaptive_tunnel_changed_title
                val body = I18n.strings.notif_adaptive_tunnel_changed_body.format(best.name)
                flare.client.app.ui.notification.AppNotificationManager.showSystemNotification(app, title, body, isHighPriority = true)
            } else {
                Log.w("MainViewModel", "No working profile found during recovery.")
            }
        }
    }

    private var bestProfileJob: kotlinx.coroutines.Job? = null

    fun startBestProfileJob() {
        bestProfileJob?.cancel()

        bestProfileJob = viewModelScope.launch {
            ensureInitialized()
            val settings = SettingsManager(getApplication())
            if (!settings.isBestProfileEnabled) return@launch
            while (isActive) {
                val rawInterval = settings.bestProfileInterval.toLongOrNull() ?: 1800L
                val interval = if (rawInterval < 10L) 10L else rawInterval
                delay(interval * 1000L)
                val shouldRun = if (settings.isBestProfileOnlyIfConnected) {
                    _connectionState.value == ConnectionState.CONNECTED
                } else true
                if (shouldRun) {
                    selectBestProfile()
                }
            }
        }
    }

    private suspend fun selectBestProfile() {
        val settings = SettingsManager(getApplication())
        val selectedId = _selectedProfileId.value ?: return
        val allProfiles = repository.getAllProfiles().first()
        val selectedProfile = allProfiles.find { it.id == selectedId } ?: return
        val subId = selectedProfile.subscriptionId

        val profiles = allProfiles.filter { it.subscriptionId == subId }
        if (profiles.size <= 1) return

        speedTestProfile(profiles)

        val deadline = SystemClock.elapsedRealtime() + 15_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val pings = _pingStates.value
            val allDone = profiles.all { p ->
                val state = pings[p.id]
                state is PingState.Result
            }
            if (allDone) break
            delay(500)
        }

        val pings = _pingStates.value
        val bestPair = profiles
            .mapNotNull { p ->
                val state = pings[p.id]
                if (state is PingState.Result && !state.isError && state.latency >= 0) {
                    p to state.latency
                } else null
            }
            .minByOrNull { it.second }

        val best = bestPair?.first
        val latency = bestPair?.second

        if (best != null) {
            if (best.id != _selectedProfileId.value) {
                selectProfile(best.id)
            }
        }
    }

    suspend fun refreshSubscriptions(subsToUpdate: List<SubscriptionEntity>) = withContext(Dispatchers.IO) {
        if (subsToUpdate.isEmpty()) return@withContext
        var successCount = 0
        val selectedBefore = repository.getSelectedProfile()
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        val hwid = if (settings.isHwidEnabled) getHwid() else null
        val model = android.os.Build.MODEL
        val osVersion = android.os.Build.VERSION.RELEASE
        
        coroutineScope {
            val deferreds = subsToUpdate.map { sub ->
                async {
                    try {
                        _refreshingSubs.update { it + sub.id }
                        val result = withTimeoutOrNull(10000L) {
                            ClipboardParser.parse(app, sub.url, hwid, model, osVersion, settings.subUserAgent)
                        }
                        if (result is ClipboardParser.ParseResult.Subscription) {
                            repository.replaceSubscriptionProfiles(sub.id, result.profiles)
                            repository.updateSubscription(result.subscription.copy(id = sub.id))
                            true
                        } else false
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to refresh ${sub.name}", e)
                        false
                    } finally {
                        _refreshingSubs.update { it - sub.id }
                    }
                }
            }
            val results = deferreds.awaitAll()
            successCount = results.count { it }
        }
        if (selectedBefore != null) {
            val allAfter = repository.getAllProfiles().first()
            val restored = allAfter.find {
                it.uri == selectedBefore.uri &&
                it.name == selectedBefore.name &&
                it.subscriptionId == selectedBefore.subscriptionId
            }
            if (restored != null) {
                repository.selectProfile(restored.id)
                _selectedProfileId.value = restored.id
            } else {
                _selectedProfileId.value = null
            }
        }
        if (successCount > 0) {
            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.SUCCESS,
                I18n.strings.sub_update_success.format(successCount),
                4
            )
        }
    }

    suspend fun refreshAllSubscriptions() = withContext(Dispatchers.IO) {
        val subs = repository.getAllSubscriptions().first()
        if (subs.isEmpty()) return@withContext
        var successCount = 0
        val selectedBefore = repository.getSelectedProfile()
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        val hwid = if (settings.isHwidEnabled) getHwid() else null
        val model = android.os.Build.MODEL
        val osVersion = android.os.Build.VERSION.RELEASE
        
        coroutineScope {
            val deferreds = subs.map { sub ->
                async {
                    try {
                        _refreshingSubs.update { it + sub.id }
                        val result = withTimeoutOrNull(10000L) {
                            ClipboardParser.parse(app, sub.url, hwid, model, osVersion, settings.subUserAgent)
                        }
                        if (result is ClipboardParser.ParseResult.Subscription) {
                            repository.replaceSubscriptionProfiles(sub.id, result.profiles)
                            repository.updateSubscription(result.subscription.copy(id = sub.id))
                            true
                        } else false
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to refresh ${sub.name}", e)
                        false
                    } finally {
                        _refreshingSubs.update { it - sub.id }
                    }
                }
            }
            val results = deferreds.awaitAll()
            successCount = results.count { it }
        }
        if (selectedBefore != null) {
            val allAfter = repository.getAllProfiles().first()
            val restored = allAfter.find {
                it.uri == selectedBefore.uri &&
                it.name == selectedBefore.name &&
                it.subscriptionId == selectedBefore.subscriptionId
            }
            if (restored != null) {
                repository.selectProfile(restored.id)
                _selectedProfileId.value = restored.id
            } else {
                _selectedProfileId.value = null
            }
        }
        if (successCount > 0) {
            settings.lastSubUpdateTime = System.currentTimeMillis()
            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.SUCCESS,
                I18n.strings.sub_update_success.format(successCount),
                4
            )
        } else {
            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.ERROR,
                I18n.strings.sub_update_error,
                4
            )
            
            delay(60000L)
        }
    }

    fun refreshSubscription(sub: SubscriptionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _refreshingSubs.update { it + sub.id }
            val app = getApplication<Application>()
            val settings = SettingsManager(app)
            val hwid = if (settings.isHwidEnabled) getHwid() else null
            val model = android.os.Build.MODEL
            val osVersion = android.os.Build.VERSION.RELEASE
            try {
                val selectedBefore = repository.getSelectedProfile()
                val result = withTimeoutOrNull(10000L) {
                    ClipboardParser.parse(app, sub.url, hwid, model, osVersion, settings.subUserAgent)
                }
                if (result is ClipboardParser.ParseResult.Subscription) {
                    repository.replaceSubscriptionProfiles(sub.id, result.profiles)
                    repository.updateSubscription(result.subscription.copy(id = sub.id))
                    if (selectedBefore != null) {
                        if (selectedBefore.subscriptionId == sub.id) {
                            val allAfter = repository.getAllProfiles().first()
                            val restored = allAfter.find {
                                it.uri == selectedBefore.uri &&
                                it.name == selectedBefore.name &&
                                it.subscriptionId == sub.id
                            }
                            if (restored != null) {
                                repository.selectProfile(restored.id)
                                _selectedProfileId.value = restored.id
                            } else {
                                _selectedProfileId.value = null
                            }
                        } else {
                            _selectedProfileId.value = selectedBefore.id
                        }
                    }
                    flare.client.app.ui.notification.AppNotificationManager.showNotification(
                        flare.client.app.ui.notification.NotificationType.SUCCESS,
                        I18n.strings.sub_update_success_single.format(sub.name),
                        3
                    )
                } else {
                    flare.client.app.ui.notification.AppNotificationManager.showNotification(
                        flare.client.app.ui.notification.NotificationType.ERROR,
                        I18n.strings.sub_update_error_single,
                        3
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to refresh ${sub.name}", e)
                flare.client.app.ui.notification.AppNotificationManager.showNotification(
                    flare.client.app.ui.notification.NotificationType.ERROR,
                    I18n.strings.sub_update_error_single,
                    3
                )
            } finally {
                _refreshingSubs.update { it - sub.id }
            }
        }
    }

    fun addPrivateServer(uri: String, name: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val subName = I18n.strings.sub_my_servers
            val allSubs = repository.getAllSubscriptions().first()
            var sub = allSubs.find { I18n.isMyServers(it.name) }
            if (sub == null) {
                val newSub = SubscriptionEntity(
                    name = subName,
                    url = "",
                    total = Long.MAX_VALUE
                )
                val id = repository.insertSubscription(newSub)
                sub = newSub.copy(id = id)
            }
            
            val profile = ClipboardParser.buildProfileFromUri(app, uri, sub.id).copy(name = name)
            repository.insertProfile(profile)
            
            val allProfiles = repository.getAllProfiles().first()
            val savedProfile = allProfiles.find { it.uri == uri && it.subscriptionId == sub.id }
            if (savedProfile != null) {
                selectProfile(savedProfile.id)
            }
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

    private var updateCheckJob: kotlinx.coroutines.Job? = null

    fun startUpdateCheckJob() {
        val app = getApplication<Application>()
        updateCheckJob?.cancel()

        updateCheckJob = viewModelScope.launch(Dispatchers.IO) {
            ensureInitialized()
            val settings = SettingsManager(app)
            if (!settings.isUpdateCheckEnabled) return@launch

            
            val startupDelay = (15L + (Math.random() * 45).toLong()) * 1000L
            delay(startupDelay)

            if (isActive) {
                flare.client.app.util.VersionManager.checkUpdates(app)
                settings.lastUpdateCheckTime = System.currentTimeMillis()
            }

            
            while (isActive) {
                val intervalMs = when (settings.updateCheckFrequency) {
                    "daily" -> 24 * 3600 * 1000L
                    "weekly" -> 7 * 24 * 3600 * 1000L
                    "monthly" -> 30 * 24 * 3600 * 1000L
                    else -> 24 * 3600 * 1000L
                }
                delay(intervalMs)

                if (isActive) {
                    flare.client.app.util.VersionManager.checkUpdates(app)
                    settings.lastUpdateCheckTime = System.currentTimeMillis()
                }
            }
        }
    }

    fun startNoticeCheckJob() {
        val app = getApplication<Application>()
        noticeCheckJob?.cancel()

        noticeCheckJob = viewModelScope.launch(Dispatchers.IO) {
            ensureInitialized()
            val settings = SettingsManager(app)
            while (isActive) {
                try {
                    checkNoticeFromServer(app, settings)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error checking notice from server", e)
                }
                delay(3600 * 1000L) 
            }
        }
    }

    private suspend fun checkNoticeFromServer(context: Context, settings: SettingsManager) {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val url = "https://raw.githubusercontent.com/gitwelk/flareVPN/refs/heads/main/notice/notice.json"
        val requestBuilder = okhttp3.Request.Builder().url(url)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    val json = org.json.JSONObject(body)

                    val id = json.optInt("id", 0)
                    val show = json.optBoolean("show", false)
                    val minVersionCode = json.optInt("min_version_code", 0)
                    val maxVersionCode = json.optInt("max_version_code", Int.MAX_VALUE)

                    val currentVersionCode = flare.client.app.BuildConfig.VERSION_CODE

                    if (show && id > settings.lastReadNoticeId &&
                        currentVersionCode >= minVersionCode &&
                        currentVersionCode <= maxVersionCode) {

                        val titleObj = json.optJSONObject("title")
                        val titleRu = titleObj?.optString("ru") ?: (json.optString("title").takeIf { it.isNotBlank() } ?: "Объявление")
                        val titleEn = titleObj?.optString("en") ?: (json.optString("title").takeIf { it.isNotBlank() } ?: "Announcement")

                        val textObj = json.optJSONObject("text")
                        val textRu = textObj?.optString("ru") ?: json.optString("text")
                        val textEn = textObj?.optString("en") ?: json.optString("text")

                        val actionTextObj = json.optJSONObject("action_text")
                        val actionTextRu = actionTextObj?.optString("ru") ?: (json.optString("action_text").takeIf { it.isNotBlank() } ?: "Понятно")
                        val actionTextEn = actionTextObj?.optString("en") ?: (json.optString("action_text").takeIf { it.isNotBlank() } ?: "Got it")

                        val actionUrl = json.optString("action_url", "")

                        settings.noticeId = id
                        settings.noticeTitleRu = titleRu
                        settings.noticeTitleEn = titleEn
                        settings.noticeTextRu = textRu
                        settings.noticeTextEn = textEn
                        settings.noticeActionTextRu = actionTextRu
                        settings.noticeActionTextEn = actionTextEn
                        settings.noticeActionUrl = actionUrl
                        settings.needsToShowNotice = true

                        withContext(Dispatchers.Main) {
                            _noticeState.value = NoticeState(
                                id = id,
                                needsToShow = true,
                                titleRu = titleRu,
                                titleEn = titleEn,
                                textRu = textRu,
                                textEn = textEn,
                                actionTextRu = actionTextRu,
                                actionTextEn = actionTextEn,
                                actionUrl = actionUrl
                            )
                        }
                    } else if (!show || id <= settings.lastReadNoticeId) {
                        settings.needsToShowNotice = false
                        withContext(Dispatchers.Main) {
                            if (_noticeState.value.needsToShow) {
                                _noticeState.value = _noticeState.value.copy(needsToShow = false)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to check remote notice config: ${e.message}")
        }
    }

    fun dismissNotice() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        val currentNotice = _noticeState.value
        settings.lastReadNoticeId = currentNotice.id
        settings.needsToShowNotice = false
        _noticeState.value = _noticeState.value.copy(needsToShow = false)
    }

    private fun getHwid(): String {
        return android.provider.Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_hwid"
    }
}
