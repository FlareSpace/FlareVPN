package flare.client.app.ui.i18n

interface RoutingStrings {
    val routing_action_download: String
    val routing_badge_builtin: String
    val routing_badge_soon: String
    val routing_card_ads: String
    val routing_card_ads_desc: String
    val routing_card_cn: String
    val routing_card_cn_desc: String
    val routing_card_global: String
    val routing_card_global_desc: String
    val routing_card_media: String
    val routing_card_media_desc: String
    val routing_card_ru: String
    val routing_card_social: String
    val routing_card_social_desc: String
    val routing_desc_no_update: String
    val routing_last_update: String
    val routing_mode_block: String
    val routing_mode_direct: String
    val routing_mode_proxy: String
    val routing_status_downloaded: String
    val routing_status_updated: String
    val routing_success_generic: String
    val routing_update_error: String
    val routing_update_never: String
    val routing_update_success: String
    val rules_method_direct: String
    val rules_method_proxy: String
    val sites_hint: String
}

object RuRoutingStrings : RoutingStrings {
    override val routing_action_download : String = "Скачать"
    override val routing_badge_builtin : String = "Встроено"
    override val routing_badge_soon : String = "Скоро"
    override val routing_card_ads : String = "Антиреклама"
    override val routing_card_ads_desc : String = "Блокировка рекламных доменов (geosite-ads)"
    override val routing_card_cn : String = "Китай (CN)"
    override val routing_card_cn_desc : String = "Китайские сайты и IP-адреса"
    override val routing_card_global : String = "Глобальные правила"
    override val routing_card_global_desc : String = "Bypass China, Google, YouTube и др."
    override val routing_card_media : String = "Медиа и Стриминг"
    override val routing_card_media_desc : String = "YouTube, Netflix, Twitch, Disney+"
    override val routing_card_ru : String = "RU"
    override val routing_card_social : String = "Соцсети"
    override val routing_card_social_desc : String = "Telegram, Instagram, Facebook, TikTok"
    override val routing_desc_no_update : String = "Не требует обновления"
    override val routing_last_update : String = "%s"
    override val routing_mode_block : String = "Block"
    override val routing_mode_direct : String = "Direct"
    override val routing_mode_proxy : String = "Proxy"
    override val routing_status_downloaded : String = "Правило скачано"
    override val routing_status_updated : String = "Правило обновлено"
    override val routing_success_generic : String = "Правило %s успешно обновлено!"
    override val routing_update_error : String = "Ошибка обновления баз!"
    override val routing_update_never : String = "Никогда"
    override val routing_update_success : String = "База правил RU успешно обновлена!"
    override val rules_method_direct : String = "Напрямую"
    override val rules_method_proxy : String = "Через прокси"
    override val sites_hint : String = "site1.com\nsite2.com"
}

object EnRoutingStrings : RoutingStrings {
    override val routing_action_download : String = "Download"
    override val routing_badge_builtin : String = "Included"
    override val routing_badge_soon : String = "Soon"
    override val routing_card_ads : String = "Anti-Ads"
    override val routing_card_ads_desc : String = "Blocking ad domains (geosite-ads)"
    override val routing_card_cn : String = "China (CN)"
    override val routing_card_cn_desc : String = "Chinese sites and IP addresses"
    override val routing_card_global : String = "Global Rules"
    override val routing_card_global_desc : String = "Bypass China, Google, YouTube, etc."
    override val routing_card_media : String = "Media & Streaming"
    override val routing_card_media_desc : String = "YouTube, Netflix, Twitch, Disney+"
    override val routing_card_ru : String = "RU"
    override val routing_card_social : String = "Social Networks"
    override val routing_card_social_desc : String = "Telegram, Instagram, Facebook, TikTok"
    override val routing_desc_no_update : String = "No update needed"
    override val routing_last_update : String = "%s"
    override val routing_mode_block : String = "Block"
    override val routing_mode_direct : String = "Direct"
    override val routing_mode_proxy : String = "Proxy"
    override val routing_status_downloaded : String = "Rule downloaded"
    override val routing_status_updated : String = "Rule updated"
    override val routing_success_generic : String = "Rule %s updated successfully!"
    override val routing_update_error : String = "Failed to update rulesets!"
    override val routing_update_never : String = "Never"
    override val routing_update_success : String = "RU rulesets updated successfully!"
    override val rules_method_direct : String = "Direct"
    override val rules_method_proxy : String = "via Proxy"
    override val sites_hint : String = "site1.com\nsite2.com"
}
