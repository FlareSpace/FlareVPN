package flare.client.app.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flare_settings", Context.MODE_PRIVATE)

    var isFragmentationEnabled: Boolean
        get() = prefs.getBoolean("frag_enabled", false)
        set(value) = prefs.edit().putBoolean("frag_enabled", value).apply()

    var packetType: String
        get() {
            val type = prefs.getString("frag_packet_type", "fallback_delay") ?: "fallback_delay"
            if (type == "tlshello") {
                prefs.edit().putString("frag_packet_type", "fallback_delay").apply()
                return "fallback_delay"
            }
            return type
        }
        set(value) = prefs.edit().putString("frag_packet_type", value).apply()

    var fragmentInterval: String
        get() = prefs.getString("frag_interval", "350") ?: "350"
        set(value) = prefs.edit().putString("frag_interval", value).apply()

    var pingType: String
        get() = prefs.getString("ping_type", "via proxy GET") ?: "via proxy GET"
        set(value) = prefs.edit().putString("ping_type", value).apply()

    var pingTestUrl: String
        get() = prefs.getString("ping_test_url", "https://www.google.com/generate_204") ?: "https://www.google.com/generate_204"
        set(value) = prefs.edit().putString("ping_test_url", value).apply()

    var pingStyle: String
        get() = prefs.getString("ping_style", "time") ?: "time"
        set(value) = prefs.edit().putString("ping_style", value).apply()

    var mtu: String
        get() = prefs.getString("mtu", "1500") ?: "1500"
        set(value) = prefs.edit().putString("mtu", value).apply()

    var isSplitTunnelingEnabled: Boolean
        get() = prefs.getBoolean("split_tunneling_enabled", false)
        set(value) = prefs.edit().putBoolean("split_tunneling_enabled", value).apply()

    var splitTunnelingApps: Set<String>
        get() = prefs.getStringSet("split_tunneling_apps", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("split_tunneling_apps", value).apply()

    var splitTunnelingSites: Set<String>
        get() = prefs.getStringSet("split_tunneling_sites", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("split_tunneling_sites", value).apply()

    var splitTunnelingModeApps: String
        get() = prefs.getString("split_tunneling_mode_apps", "whitelist") ?: "whitelist"
        set(value) = prefs.edit().putString("split_tunneling_mode_apps", value).apply()

    var splitTunnelingModeSites: String
        get() = prefs.getString("split_tunneling_mode_sites", "blacklist") ?: "blacklist"
        set(value) = prefs.edit().putString("split_tunneling_mode_sites", value).apply()

    var tunStack: String
        get() = prefs.getString("tun_stack", "mixed") ?: "mixed"
        set(value) = prefs.edit().putString("tun_stack", value).apply()

    var isAutostartEnabled: Boolean
        get() = prefs.getBoolean("autostart_enabled", false)
        set(value) = prefs.edit().putBoolean("autostart_enabled", value).apply()

    var isSubIntervalEnabled: Boolean
        get() = prefs.getBoolean("sub_interval_enabled", true)
        set(value) = prefs.edit().putBoolean("sub_interval_enabled", value).apply()

    var isSubAutoUpdateEnabled: Boolean
        get() = prefs.getBoolean("sub_auto_update_enabled", false)
        set(value) = prefs.edit().putBoolean("sub_auto_update_enabled", value).apply()

    var subAutoUpdateInterval: String
        get() = prefs.getString("sub_auto_update_interval", "3600") ?: "3600"
        set(value) = prefs.edit().putString("sub_auto_update_interval", value).apply()

    var subUserAgent: String
        get() = prefs.getString("sub_user_agent", "Happ/3.21.1") ?: "Happ/3.21.1"
        set(value) = prefs.edit().putString("sub_user_agent", value).apply()

    var lastSubUpdateTime: Long
        get() = prefs.getLong("last_sub_update_time", 0L)
        set(value) = prefs.edit().putLong("last_sub_update_time", value).apply()

    var isMuxEnabled: Boolean
        get() = prefs.getBoolean("mux_enabled", false)
        set(value) = prefs.edit().putBoolean("mux_enabled", value).apply()

    var muxProtocol: String
        get() = prefs.getString("mux_protocol", "smux") ?: "h2mux"
        set(value) = prefs.edit().putString("mux_protocol", value).apply()

    var muxMaxStreams: String
        get() = prefs.getString("mux_max_streams", "8") ?: "8"
        set(value) = prefs.edit().putString("mux_max_streams", value).apply()

    var muxPadding: Boolean
        get() = prefs.getBoolean("mux_padding", false)
        set(value) = prefs.edit().putBoolean("mux_padding", value).apply()

    var remoteDnsUrl: String
        get() = prefs.getString("remote_dns_url", "") ?: ""
        set(value) = prefs.edit().putString("remote_dns_url", value).apply()

    var remoteDnsMode: String
        get() = prefs.getString("dns_mode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("dns_mode", value).apply()

    var lastWidgetPing: String
        get() = prefs.getString("last_widget_ping", "--") ?: "--"
        set(value) = prefs.edit().putString("last_widget_ping", value).apply()

    var lastWidgetPingProfileId: Long
        get() = prefs.getLong("last_widget_ping_profile_id", -1L)
        set(value) = prefs.edit().putLong("last_widget_ping_profile_id", value).apply()

    var isFakeIpEnabled: Boolean
        get() = prefs.getBoolean("fake_ip_enabled", false)
        set(value) = prefs.edit().putBoolean("fake_ip_enabled", value).apply()

    var themeMode: Int
        get() = prefs.getInt("theme_mode", 0)
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    var backgroundType: Int
        get() = prefs.getInt("background_type", if (isBackgroundGradientEnabled) 1 else 0)
        set(value) = prefs.edit().putInt("background_type", value).apply()

    var isBackgroundGradientEnabled: Boolean
        get() = prefs.getBoolean("bg_gradient_enabled", false)
        set(value) = prefs.edit().putBoolean("bg_gradient_enabled", value).apply()
    var isGradientAnimationEnabled: Boolean
        get() = prefs.getBoolean("bg_gradient_animation_enabled", false)
        set(value) = prefs.edit().putBoolean("bg_gradient_animation_enabled", value).apply()

    var gradientAnimationSpeed: Float
        get() = prefs.getFloat("bg_gradient_animation_speed", 0.6f)
        set(value) = prefs.edit().putFloat("bg_gradient_animation_speed", value).apply()

    var isStatusNotificationEnabled: Boolean
        get() = prefs.getBoolean("status_notification_enabled", true)
        set(value) = prefs.edit().putBoolean("status_notification_enabled", value).apply()

    var isNotificationSpeedEnabled: Boolean
        get() = prefs.getBoolean("notification_speed_enabled", true)
        set(value) = prefs.edit().putBoolean("notification_speed_enabled", value).apply()

    var photoSeed: String
        get() = prefs.getString("photo_seed", "default_seed") ?: "default_seed"
        set(value) = prefs.edit().putString("photo_seed", value).apply()

    var pendingNavScreen: String
        get() = prefs.getString("pending_nav_screen", "") ?: ""
        set(value) = prefs.edit().putString("pending_nav_screen", value).apply()

    var isBestProfileEnabled: Boolean
        get() = prefs.getBoolean("best_profile_enabled", false)
        set(value) = prefs.edit().putBoolean("best_profile_enabled", value).apply()

    var bestProfileInterval: String
        get() = prefs.getString("best_profile_interval", "1800") ?: "1800"
        set(value) = prefs.edit().putString("best_profile_interval", value).apply()

    var isBestProfileOnlyIfConnected: Boolean
        get() = prefs.getBoolean("best_profile_only_if_connected", true)
        set(value) = prefs.edit().putBoolean("best_profile_only_if_connected", value).apply()

    var isAdaptiveTunnelEnabled: Boolean
        get() = prefs.getBoolean("adaptive_tunnel_enabled", false)
        set(value) = prefs.edit().putBoolean("adaptive_tunnel_enabled", value).apply()

    var isBestProfileNotificationEnabled: Boolean
        get() = prefs.getBoolean("best_profile_notification_enabled", false)
        set(value) = prefs.edit().putBoolean("best_profile_notification_enabled", value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var isUpdateCheckEnabled: Boolean
        get() = prefs.getBoolean("update_check_enabled", true)
        set(value) = prefs.edit().putBoolean("update_check_enabled", value).apply()

    var updateCheckFrequency: String
        get() = prefs.getString("update_check_frequency", "weekly") ?: "weekly"
        set(value) = prefs.edit().putString("update_check_frequency", value).apply()

    var lastUpdateCheckTime: Long
        get() = prefs.getLong("last_update_check_time", 0L)
        set(value) = prefs.edit().putLong("last_update_check_time", value).apply()

    var isCustomColorEnabled: Boolean
        get() = prefs.getBoolean("custom_color_enabled", false)
        set(value) = prefs.edit().putBoolean("custom_color_enabled", value).apply()

    
    var accentColorKey: String
        get() = prefs.getString("accent_color_key", "default") ?: "default"
        set(value) = prefs.edit().putString("accent_color_key", value).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "auto") ?: "auto"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var isHwidEnabled: Boolean
        get() = prefs.getBoolean("hwid_enabled", true)
        set(value) = prefs.edit().putBoolean("hwid_enabled", value).apply()

    var isCoreLogEnabled: Boolean
        get() = prefs.getBoolean("core_log_enabled", false)
        set(value) = prefs.edit().putBoolean("core_log_enabled", value).apply()

    var isAppTriggerEnabled: Boolean
        get() = prefs.getBoolean("app_trigger_enabled", false)
        set(value) = prefs.edit().putBoolean("app_trigger_enabled", value).apply()

    var coreLogLevel: String
        get() = prefs.getString("core_log_level", "warn") ?: "warn"
        set(value) = prefs.edit().putString("core_log_level", value).apply()
    var isRoutingMainEnabled: Boolean
        get() = prefs.getBoolean("routing_main_enabled", false)
        set(value) = prefs.edit().putBoolean("routing_main_enabled", value).apply()

    var routingMainMode: String
        get() = prefs.getString("routing_main_mode", "direct") ?: "direct"
        set(value) = prefs.edit().putString("routing_main_mode", value).apply()

    var lastRoutingUpdateMain: Long
        get() = prefs.getLong("last_routing_update_main", 0L)
        set(value) = prefs.edit().putLong("last_routing_update_main", value).apply()

    
    var isRoutingMediaEnabled: Boolean
        get() = prefs.getBoolean("routing_media_enabled", false)
        set(value) = prefs.edit().putBoolean("routing_media_enabled", value).apply()

    var routingMediaMode: String
        get() = prefs.getString("routing_media_mode", "direct") ?: "direct"
        set(value) = prefs.edit().putString("routing_media_mode", value).apply()

    
    var isRoutingSocialEnabled: Boolean
        get() = prefs.getBoolean("routing_social_enabled", false)
        set(value) = prefs.edit().putBoolean("routing_social_enabled", value).apply()

    var routingSocialMode: String
        get() = prefs.getString("routing_social_mode", "direct") ?: "direct"
        set(value) = prefs.edit().putString("routing_social_mode", value).apply()

    
    var isRoutingAdsEnabled: Boolean
        get() = prefs.getBoolean("routing_ads_enabled", false)
        set(value) = prefs.edit().putBoolean("routing_ads_enabled", value).apply()

    var routingAdsMode: String
        get() = prefs.getString("routing_ads_mode", "block") ?: "block"
        set(value) = prefs.edit().putString("routing_ads_mode", value).apply()

    
    var isRoutingCnEnabled: Boolean
        get() = prefs.getBoolean("routing_cn_enabled", false)
        set(value) = prefs.edit().putBoolean("routing_cn_enabled", value).apply()

    var routingCnMode: String
        get() = prefs.getString("routing_cn_mode", "direct") ?: "direct"
        set(value) = prefs.edit().putString("routing_cn_mode", value).apply()

    var lastRoutingUpdateMedia: Long
        get() = prefs.getLong("last_routing_update_media", 0L)
        set(value) = prefs.edit().putLong("last_routing_update_media", value).apply()

    var lastRoutingUpdateSocial: Long
        get() = prefs.getLong("last_routing_update_social", 0L)
        set(value) = prefs.edit().putLong("last_routing_update_social", value).apply()

    var lastRoutingUpdateAds: Long
        get() = prefs.getLong("last_routing_update_ads", 0L)
        set(value) = prefs.edit().putLong("last_routing_update_ads", value).apply()

    var isRoutingGlobalEnabled: Boolean
        get() = prefs.getBoolean("is_routing_global_enabled", false)
        set(value) = prefs.edit().putBoolean("is_routing_global_enabled", value).apply()

    var routingGlobalMode: String
        get() = prefs.getString("routing_global_mode", "proxy") ?: "proxy"
        set(value) = prefs.edit().putString("routing_global_mode", value).apply()

    var lastRoutingUpdateGlobal: Long
        get() = prefs.getLong("last_routing_update_global", 0L)
        set(value) = prefs.edit().putLong("last_routing_update_global", value).apply()

    var lastRoutingUpdateCn: Long
        get() = prefs.getLong("last_routing_update_cn", 0L)
        set(value) = prefs.edit().putLong("last_routing_update_cn", value).apply()

    var lastReadNoticeId: Int
        get() = prefs.getInt("last_read_notice_id", 0)
        set(value) = prefs.edit().putInt("last_read_notice_id", value).apply()

    var needsToShowNotice: Boolean
        get() = prefs.getBoolean("needs_to_show_notice", false)
        set(value) = prefs.edit().putBoolean("needs_to_show_notice", value).apply()

    var noticeId: Int
        get() = prefs.getInt("notice_id", 0)
        set(value) = prefs.edit().putInt("notice_id", value).apply()

    var noticeTitleRu: String
        get() = prefs.getString("notice_title_ru", "") ?: ""
        set(value) = prefs.edit().putString("notice_title_ru", value).apply()

    var noticeTitleEn: String
        get() = prefs.getString("notice_title_en", "") ?: ""
        set(value) = prefs.edit().putString("notice_title_en", value).apply()

    var noticeTextRu: String
        get() = prefs.getString("notice_text_ru", "") ?: ""
        set(value) = prefs.edit().putString("notice_text_ru", value).apply()

    var noticeTextEn: String
        get() = prefs.getString("notice_text_en", "") ?: ""
        set(value) = prefs.edit().putString("notice_text_en", value).apply()

    var noticeActionTextRu: String
        get() = prefs.getString("notice_action_text_ru", "") ?: ""
        set(value) = prefs.edit().putString("notice_action_text_ru", value).apply()

    var noticeActionTextEn: String
        get() = prefs.getString("notice_action_text_en", "") ?: ""
        set(value) = prefs.edit().putString("notice_action_text_en", value).apply()

    var noticeActionUrl: String
        get() = prefs.getString("notice_action_url", "") ?: ""
        set(value) = prefs.edit().putString("notice_action_url", value).apply()

    var isResetChainOnDisconnect: Boolean
        get() = prefs.getBoolean("reset_chain_on_disconnect", false)
        set(value) = prefs.edit().putBoolean("reset_chain_on_disconnect", value).apply()

    var chainedProfileIdsString: String
        get() = prefs.getString("chained_profile_ids", "") ?: ""
        set(value) = prefs.edit().putString("chained_profile_ids", value).apply()

    var isTlsSpoofEnabled: Boolean
        get() = prefs.getBoolean("tls_spoof_enabled", false)
        set(value) = prefs.edit().putBoolean("tls_spoof_enabled", value).apply()

    var tlsSpoofDomain: String
        get() = prefs.getString("tls_spoof_domain", "google.com") ?: "google.com"
        set(value) = prefs.edit().putString("tls_spoof_domain", value).apply()

    var tlsSpoofMethod: String
        get() = prefs.getString("tls_spoof_method", "wrong-ack") ?: "wrong-ack"
        set(value) = prefs.edit().putString("tls_spoof_method", value).apply()

    var fingerprint: String
        get() = prefs.getString("fingerprint", "auto") ?: "auto"
        set(value) = prefs.edit().putString("fingerprint", value).apply()

    var isVirtualSubscriptionPinned: Boolean
        get() = prefs.getBoolean("virtual_sub_pinned", false)
        set(value) = prefs.edit().putBoolean("virtual_sub_pinned", value).apply()

    var virtualSubscriptionPinnedTime: Long
        get() = prefs.getLong("virtual_sub_pinned_time", 0L)
        set(value) = prefs.edit().putLong("virtual_sub_pinned_time", value).apply()
}



