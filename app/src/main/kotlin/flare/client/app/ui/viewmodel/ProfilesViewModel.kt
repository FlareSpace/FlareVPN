package flare.client.app.ui.viewmodel

import android.app.Application
import android.util.Log
import flare.client.app.util.PingHelper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.data.SettingsManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.model.DisplayItem
import flare.client.app.data.model.PingState
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.model.ProfileSummary
import flare.client.app.data.model.SubscriptionEntity
import flare.client.app.data.parser.ClipboardParser
import flare.client.app.data.repository.ProfileRepository
import flare.client.app.ui.i18n.I18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ProfilesViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppDatabase.getInstance(application)
    }
    private val repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProfileRepository(db.profileDao(), db.subscriptionDao())
    }

    companion object {
        const val VIRTUAL_SUB_ID = -1L
        const val MERGED_SUB_ID = -2L
    }

    private val settings by lazy { SettingsManager(application) }
    private val mergedSubIds = MutableStateFlow<Set<Long>>(emptySet())

    private val expandedSubs = MutableStateFlow<Set<Long>>(emptySet())
    private val _refreshingSubs = MutableStateFlow<Set<Long>>(emptySet())
    val refreshingSubs: StateFlow<Set<Long>> = _refreshingSubs.asStateFlow()

    val pingStates: StateFlow<Map<Long, PingState>> = PingHelper.pingStates

    private val _displayItems = MutableStateFlow<List<DisplayItem>>(emptyList())
    val displayItems: StateFlow<List<DisplayItem>> = _displayItems.asStateFlow()

    private val _isStartupLoading = MutableStateFlow(true)
    val isStartupLoading: StateFlow<Boolean> = _isStartupLoading.asStateFlow()

    val isAnySubscriptionExpanded: StateFlow<Boolean> = _displayItems
        .map { items ->
            items.filterIsInstance<DisplayItem.SubscriptionItem>().any { it.isExpanded }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _editingProfile = MutableStateFlow<ProfileEntity?>(null)
    val editingProfile: StateFlow<ProfileEntity?> = _editingProfile.asStateFlow()

    private val _editingSubscription = MutableStateFlow<SubscriptionEntity?>(null)
    val editingSubscription: StateFlow<SubscriptionEntity?> = _editingSubscription.asStateFlow()

    sealed class ImportEvent {
        object Loading : ImportEvent()
        data class Success(val message: String) : ImportEvent()
        data class Error(val message: String) : ImportEvent()
        data class NeedPermission(val intent: android.content.Intent) : ImportEvent()
    }

    private val _importEvent = MutableSharedFlow<ImportEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val importEvent: SharedFlow<ImportEvent> = _importEvent.asSharedFlow()

    
    
    private val _selectedProfileId = MutableStateFlow<Long?>(null)

    private var displayItemsJob: kotlinx.coroutines.Job? = null
    private var autoUpdateJob: kotlinx.coroutines.Job? = null

    init {
        initialize()
    }

    private fun initialize() {
        mergedSubIds.value = settings.mergedSubscriptionIds.mapNotNull { it.toLongOrNull() }.toSet()

        viewModelScope.launch {
            repository.getAllProfiles().collect { profiles ->
                _selectedProfileId.value = profiles.find { it.isSelected }?.id
            }
        }

        viewModelScope.launch {
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
                Log.e("ProfilesViewModel", "Failed to auto-repair JSON profiles server description: ${e.message}")
            }

            displayItemsJob?.cancel()
            displayItemsJob = combine(
                repository.getAllSubscriptions(),
                repository.getAllProfiles(),
                expandedSubs,
                _selectedProfileId,
                pingStates,
                _refreshingSubs,
                mergedSubIds
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val subs = args[0] as List<SubscriptionEntity>
                @Suppress("UNCHECKED_CAST")
                val allProfiles = args[1] as List<ProfileSummary>
                @Suppress("UNCHECKED_CAST")
                val expanded = args[2] as Set<Long>
                val selId = args[3] as Long?
                @Suppress("UNCHECKED_CAST")
                val pings = args[4] as Map<Long, PingState>
                @Suppress("UNCHECKED_CAST")
                val refreshing = args[5] as Set<Long>
                @Suppress("UNCHECKED_CAST")
                val merged = args[6] as Set<Long>

                val profilesBySub = allProfiles.groupBy { it.subscriptionId }
                val standalone = allProfiles.filter { it.subscriptionId == null }
                
                val activeMergedIds = merged.intersect(subs.map { it.id }.toSet())
                val mergedProfiles = allProfiles.filter { it.subscriptionId in activeMergedIds }

                buildDisplayList(subs, standalone, mergedProfiles, activeMergedIds, profilesBySub, expanded, selId, pings, refreshing)
            }
                .onEach { items ->
                    _displayItems.value = items
                    _isStartupLoading.value = false
                }
                .launchIn(viewModelScope)
                
            startAutoUpdateJob()
        }
    }

    private fun buildDisplayList(
        subs: List<SubscriptionEntity>,
        standalone: List<ProfileSummary>,
        mergedProfiles: List<ProfileSummary>,
        activeMergedIds: Set<Long>,
        profilesBySub: Map<Long?, List<ProfileSummary>>,
        expanded: Set<Long>,
        selId: Long?,
        pings: Map<Long, PingState>,
        refreshing: Set<Long>
    ): List<DisplayItem> {
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
        if (activeMergedIds.isNotEmpty()) {
            val virtualMergedSub = SubscriptionEntity(
                id = MERGED_SUB_ID,
                name = I18n.strings.sub_merged_profiles,
                url = "",
                pinned = if (settings.isMergedSubscriptionPinned) settings.mergedSubscriptionPinnedTime else 0L
            )
            allSubs.add(virtualMergedSub)
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
                val isV1 = s1.id == VIRTUAL_SUB_ID || s1.id == MERGED_SUB_ID
                val isV2 = s2.id == VIRTUAL_SUB_ID || s2.id == MERGED_SUB_ID
                if (isV1 && isV2) {
                    s1.id.compareTo(s2.id)
                } else if (isV1) {
                    1
                } else if (isV2) {
                    -1
                } else {
                    s1.id.compareTo(s2.id)
                }
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
                val totalProfiles = standalone.size
                val loadingProfiles = standalone.count { pings[it.id] is PingState.Loading }
                val checkedProfiles = standalone.count { pings[it.id] is PingState.Result }
                val isPinging = loadingProfiles > 0
                val pingProgressText = if (isPinging) "$checkedProfiles/$totalProfiles" else ""
                items += DisplayItem.SubscriptionItem(
                    sub, standalone, isExpanded, isRefreshing, isPinging, pingProgressText,
                    if (isExpanded) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                )
                if (isExpanded) {
                    standalone.forEachIndexed { i, p ->
                        items += DisplayItem.ProfileItem(p, p.id == selId, pings[p.id] ?: PingState.None, if (i == standalone.size - 1) DisplayItem.CornerType.BOTTOM else DisplayItem.CornerType.NONE)
                    }
                }
            } else if (sub.id == MERGED_SUB_ID) {
                val isExpanded = MERGED_SUB_ID in expanded
                if (isExpanded) {
                    actualExpanded.add(MERGED_SUB_ID)
                }
                val isRefreshing = MERGED_SUB_ID in refreshing
                val totalProfiles = mergedProfiles.size
                val loadingProfiles = mergedProfiles.count { pings[it.id] is PingState.Loading }
                val checkedProfiles = mergedProfiles.count { pings[it.id] is PingState.Result }
                val isPinging = loadingProfiles > 0
                val pingProgressText = if (isPinging) "$checkedProfiles/$totalProfiles" else ""
                items += DisplayItem.SubscriptionItem(
                    sub, mergedProfiles, isExpanded, isRefreshing, isPinging, pingProgressText,
                    if (isExpanded) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                )
                if (isExpanded) {
                    mergedProfiles.forEachIndexed { i, p ->
                        items += DisplayItem.ProfileItem(p, p.id == selId, pings[p.id] ?: PingState.None, if (i == mergedProfiles.size - 1) DisplayItem.CornerType.BOTTOM else DisplayItem.CornerType.NONE)
                    }
                }
            } else {
                val subProfiles = profilesBySub[sub.id] ?: emptyList()
                val isExpanded = sub.id in expanded
                if (isExpanded) {
                    actualExpanded.add(sub.id)
                }
                val isRefreshing = sub.id in refreshing
                val totalProfiles = subProfiles.size
                val loadingProfiles = subProfiles.count { pings[it.id] is PingState.Loading }
                val checkedProfiles = subProfiles.count { pings[it.id] is PingState.Result }
                val isPinging = loadingProfiles > 0
                val pingProgressText = if (isPinging) "$checkedProfiles/$totalProfiles" else ""
                items += DisplayItem.SubscriptionItem(
                    sub, subProfiles, isExpanded, isRefreshing, isPinging, pingProgressText,
                    if (isExpanded) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                )
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
                        Log.e("ProfilesViewModel", "Auto-update interval check failed: ${e.message}")
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
                            Log.e("ProfilesViewModel", "Auto-update failed: ${e.message}")
                            delay(10000L)
                        }
                    }
                }
            }
        }
    }

    suspend fun refreshSubscriptions(subsToUpdate: List<SubscriptionEntity>) = withContext(Dispatchers.IO) {
        if (subsToUpdate.isEmpty()) return@withContext
        var successCount = 0
        val selectedBefore = repository.getSelectedProfile()
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        val hwid = settings.getHardwareId()
        val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
        
        coroutineScope {
            val deferreds = subsToUpdate.map { sub ->
                async {
                    try {
                        _refreshingSubs.update { it + sub.id }
                        val result = withTimeoutOrNull(settings.subUpdateTimeout * 1000L + 2000L) {
                            ClipboardParser.parse(app, sub.url, hwid, model, osVersion, settings.subUserAgent, settings.subUpdateTimeout)
                        }
                        if (result is ClipboardParser.ParseResult.Subscription) {
                            repository.replaceSubscriptionProfiles(sub.id, result.profiles)
                            repository.updateSubscription(result.subscription.copy(id = sub.id, pinned = sub.pinned))
                            true
                        } else {
                            repository.updateSubscription(sub.copy(lastUpdated = System.currentTimeMillis()))
                            false
                        }
                    } catch (e: Exception) {
                        Log.e("ProfilesViewModel", "Failed to refresh ${sub.name}", e)
                        try {
                            repository.updateSubscription(sub.copy(lastUpdated = System.currentTimeMillis()))
                        } catch (dbEx: Exception) {
                            Log.e("ProfilesViewModel", "Failed to update db lastUpdated for ${sub.name}", dbEx)
                        }
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
        val hwid = settings.getHardwareId()
        val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
        
        coroutineScope {
            val deferreds = subs.map { sub ->
                async {
                    try {
                        _refreshingSubs.update { it + sub.id }
                        val result = withTimeoutOrNull(settings.subUpdateTimeout * 1000L + 2000L) {
                            ClipboardParser.parse(app, sub.url, hwid, model, osVersion, settings.subUserAgent, settings.subUpdateTimeout)
                        }
                        if (result is ClipboardParser.ParseResult.Subscription) {
                            repository.replaceSubscriptionProfiles(sub.id, result.profiles)
                            repository.updateSubscription(result.subscription.copy(id = sub.id, pinned = sub.pinned))
                            true
                        } else {
                            repository.updateSubscription(sub.copy(lastUpdated = System.currentTimeMillis()))
                            false
                        }
                    } catch (e: Exception) {
                        Log.e("ProfilesViewModel", "Failed to refresh ${sub.name}", e)
                        try {
                            repository.updateSubscription(sub.copy(lastUpdated = System.currentTimeMillis()))
                        } catch (dbEx: Exception) {
                            Log.e("ProfilesViewModel", "Failed to update db lastUpdated for ${sub.name}", dbEx)
                        }
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
            settings.lastSubUpdateTime = System.currentTimeMillis()
            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.ERROR,
                I18n.strings.sub_update_error,
                4
            )
        }
    }

    fun refreshSubscription(sub: SubscriptionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _refreshingSubs.update { it + sub.id }
            val app = getApplication<Application>()
            val settings = SettingsManager(app)
            val hwid = settings.getHardwareId()
            val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
            try {
                val selectedBefore = repository.getSelectedProfile()
                val result = withTimeoutOrNull(settings.subUpdateTimeout * 1000L + 2000L) {
                    ClipboardParser.parse(app, sub.url, hwid, model, osVersion, settings.subUserAgent, settings.subUpdateTimeout)
                }
                if (result is ClipboardParser.ParseResult.Subscription) {
                    repository.replaceSubscriptionProfiles(sub.id, result.profiles)
                    repository.updateSubscription(result.subscription.copy(id = sub.id, pinned = sub.pinned))
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
                Log.e("ProfilesViewModel", "Failed to refresh ${sub.name}", e)
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

    fun toggleSubscriptionExpanded(subId: Long) = expandedSubs.update { if (subId in it) it - subId else it + subId }
    fun collapseAllSubscriptions() { expandedSubs.value = emptySet() }
    
    fun toggleSubscriptionPinned(subId: Long) {
        viewModelScope.launch {
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
            } else if (subId == MERGED_SUB_ID) {
                val wasPinned = settings.isMergedSubscriptionPinned
                if (wasPinned) {
                    settings.isMergedSubscriptionPinned = false
                    settings.mergedSubscriptionPinnedTime = 0L
                } else {
                    settings.isMergedSubscriptionPinned = true
                    settings.mergedSubscriptionPinnedTime = System.currentTimeMillis()
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

    fun deleteSubscription(subId: Long) {
        val subName = when (subId) {
            VIRTUAL_SUB_ID -> I18n.strings.sub_single_profiles
            MERGED_SUB_ID -> I18n.strings.sub_merged_profiles
            else -> {
                val rawName = displayItems.value.filterIsInstance<DisplayItem.SubscriptionItem>().find { it.entity.id == subId }?.entity?.name ?: I18n.strings.label_unknown
                if (I18n.isMyServers(rawName)) {
                    I18n.strings.sub_my_servers
                } else {
                    rawName
                }
            }
        }
        viewModelScope.launch {
            if (subId == VIRTUAL_SUB_ID) {
                repository.deleteStandaloneProfiles()
            } else if (subId == MERGED_SUB_ID) {
                clearMergedSubscriptions()
            } else {
                repository.deleteSubscriptionById(subId)
            }
            expandedSubs.update { it - subId }
            flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.SUCCESS,
                I18n.strings.sub_deleted_success.format(subName),
                3
            )
        }
    }

    fun speedTestSubscription(subId: Long) {
        viewModelScope.launch {
            val profiles = when (subId) {
                VIRTUAL_SUB_ID -> {
                    repository.getAllProfiles().first().filter { it.subscriptionId == null }
                }
                MERGED_SUB_ID -> {
                    val activeMerged = mergedSubIds.value
                    repository.getAllProfiles().first().filter { it.subscriptionId in activeMerged }
                }
                else -> {
                    repository.getAllProfiles().first().filter { it.subscriptionId == subId }
                }
            }
            if (profiles.isEmpty()) return@launch
            speedTestProfile(profiles)
        }
    }

    fun addSubscriptionToMerged(subId: Long) {
        val current = mergedSubIds.value
        val next = current + subId
        mergedSubIds.value = next
        settings.mergedSubscriptionIds = next.map { it.toString() }.toSet()
        
        val subName = displayItems.value.filterIsInstance<DisplayItem.SubscriptionItem>().find { it.entity.id == subId }?.entity?.name ?: ""
        
        flare.client.app.ui.notification.AppNotificationManager.showNotification(
            flare.client.app.ui.notification.NotificationType.SUCCESS,
            I18n.strings.success_subscription_added.format(subName),
            3
        )
    }

    fun clearMergedSubscriptions() {
        mergedSubIds.value = emptySet()
        settings.mergedSubscriptionIds = emptySet()
    }

    fun speedTestProfile(profiles: List<ProfileSummary>) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadingPings = profiles.associate { it.id to PingState.Loading }
            PingHelper.updatePingStates(loadingPings)

            val app = getApplication<Application>()
            val settings = SettingsManager(app)
            val isProxy = settings.pingType.startsWith("via")

            val fullProfiles = repository.getProfilesByIds(profiles.map { it.id })

            if (!isProxy) {
                val method = if (settings.pingType == "TCP") "TCP" else "ICMP"
                fullProfiles.forEach { profile ->
                    launch {
                        val (latency, error) = PingHelper.pingDirect(profile, method, settings.pingTimeout)
                        PingHelper.updatePingState(profile.id, PingState.Result(latency, latency < 0, error))
                    }
                }
            } else {
                PingHelper.pingProxyBatch(
                    context = app,
                    profiles = fullProfiles,
                    testUrl = settings.pingTestUrl,
                    timeoutSec = settings.pingTimeout
                ) { id, latency, error ->
                    PingHelper.updatePingState(id, PingState.Result(latency, latency < 0, error))
                }
            }
        }
    }

    fun setEditingProfile(p: ProfileEntity?) { _editingProfile.value = p; _editingSubscription.value = null }
    fun setEditingSubscription(s: SubscriptionEntity?) { _editingSubscription.value = s; _editingProfile.value = null }

    suspend fun getProfileById(id: Long): ProfileEntity? = repository.getProfileById(id)

    fun pingCurrentSubscription() {
        val selected = _selectedProfileId.value ?: return
        viewModelScope.launch {
            val allProfiles = repository.getAllProfiles().first()
            val profile = allProfiles.find { it.id == selected } ?: return@launch
            val subId = profile.subscriptionId
            if (subId != null) {
                speedTestSubscription(subId)
            } else {
                speedTestSubscription(VIRTUAL_SUB_ID)
            }
        }
    }

    fun fetchProfileForEditing(id: Long) {
        viewModelScope.launch {
            val profile = repository.getProfileById(id)
            _editingProfile.value = profile
        }
    }

    fun updateProfileConfig(id: Long, json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateProfileConfig(id, json)
            } catch (e: Exception) {
                Log.e("ProfilesViewModel", "updateProfileConfig failed: ${e.message}")
            }
        }
    }
    
    fun updateProfile(id: Long, name: String, json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val protocol = try {
                    val outbounds = org.json.JSONObject(json).optJSONArray("outbounds")
                    outbounds?.optJSONObject(0)?.optString("type")
                } catch (_: Exception) { null }
                val desc = flare.client.app.data.parser.ProfileParsingHelper.parseTransportAndSecurityFromJson(json)
                repository.updateProfile(id, name, json, protocol, desc)
            } catch (e: Exception) {
                Log.e("ProfilesViewModel", "updateProfile failed: ${e.message}")
            }
        }
    }

    fun updateProfileFull(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateProfileFull(profile)
            } catch (e: Exception) {
                Log.e("ProfilesViewModel", "updateProfileFull failed: ${e.message}")
            }
        }
    }
    
    fun deleteProfile(id: Long, name: String) {
        viewModelScope.launch {
            try {
                repository.deleteProfile(id)
                flare.client.app.ui.notification.AppNotificationManager.showNotification(
                    flare.client.app.ui.notification.NotificationType.SUCCESS,
                    I18n.strings.profile_deleted_success.format(name),
                    3
                )
            } catch (e: Exception) {
                Log.e("ProfilesViewModel", "deleteProfile failed: ${e.message}")
            }
        }
    }

    fun updateSubscription(id: Long, name: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateSubscription(id, name, url)
            } catch (e: Exception) {
                Log.e("ProfilesViewModel", "updateSubscription failed: ${e.message}")
            }
        }
    }

    fun importFromClipboard(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _importEvent.emit(ImportEvent.Loading)
            try {
                val app = getApplication<Application>()
                val settings = SettingsManager(app)
                val hwid = settings.getHardwareId()
                val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
                
                kotlinx.coroutines.withTimeout(settings.subUpdateTimeout * 1000L + 2000L) {
                    when (val result = ClipboardParser.parse(app, text, hwid, model, osVersion, settings.subUserAgent, settings.subUpdateTimeout)) {
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
                                    I18n.strings.success_profiles_added.format(result.profiles.size)
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
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _importEvent.emit(ImportEvent.Error(I18n.strings.error_import_timeout))
            } catch (e: Exception) {
                _importEvent.emit(ImportEvent.Error(I18n.strings.error_import_failed))
            }
        }
    }


}
