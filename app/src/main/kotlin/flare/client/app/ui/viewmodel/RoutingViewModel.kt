package flare.client.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.data.SettingsManager
import flare.client.app.ui.i18n.I18n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

class RoutingViewModel(application: Application) : AndroidViewModel(application) {

    private val _routingRules = MutableStateFlow<List<RoutingRuleState>>(emptyList())
    val routingRules: StateFlow<List<RoutingRuleState>> = _routingRules.asStateFlow()

    init {
        initRoutingRules()
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
}
