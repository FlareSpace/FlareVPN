package flare.client.app.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


interface FlareStrings {
    val collapse_all: String
    val app_monitor_active: String
    val app_name: String
    val btn_add: String
    val btn_apply: String
    val btn_cancel: String
    val btn_clipboard: String
    val btn_download: String
    val btn_connect: String
    val btn_disconnect: String
    val btn_finish: String
    val btn_grant: String
    val btn_next: String
    val btn_save: String
    val btn_select_from_gallery: String
    val btn_share_link: String
    val desc_select_apps: String
    val dialog_apps_title: String
    val dialog_domens_title: String
    val edit_sub_name_hint: String
    val edit_sub_title: String
    val edit_sub_url_hint: String
    val empty_profiles_hint: String
    val error_apps_list_empty: String
    val error_camera_permission_denied: String
    val error_clipboard_empty: String
    val error_empty_name: String
    val error_import_failed: String
    val error_import_file_read: String
    val error_import_file_type: String
    val error_import_timeout: String
    val error_invalid_format: String
    val error_json: String
    val error_link_generation: String
    val error_open_settings: String
    val error_parsing: String
    val error_profile_qr_generation: String
    val error_qr_not_found_in_image: String
    val error_qr_scan_empty: String
    val error_subscription: String
    val error_subscription_empty: String
    val error_subscription_https_required: String
    val fakeip_desc: String
    val feature_coming_soon: String
    val fragment_desc: String
    val gvisorstack_desc: String
    val hint_add_first_profile: String
    val icmp_desc: String
    val journal_clear: String
    val journal_title: String
    val journal_waiting_logs: String
    val journal_copy_success: String
    val json_edit_success: String
    val label_add_profiles: String
    val label_and: String
    val label_config_editor: String
    val label_credentials: String
    val label_error: String
    val label_errors: String
    val label_expires: String
    val label_update_interval: String
    val settings_label_use_sub_interval: String
    val settings_desc_use_sub_interval: String
    val label_imported_profile: String
    val label_json_data: String
    val label_logs: String
    val label_mode: String
    val label_output: String
    val label_password: String
    val label_profile_name: String
    val label_seconds_short: String
    val label_selected: String
    val label_servers: String
    val label_speed_test: String
    val label_support: String
    val label_unknown: String
    val label_update: String
    val label_uuid: String
    val language_auto: String
    val language_en: String
    val language_restart_hint: String
    val language_ru: String
    val log_decoding_fragmentation: String
    val log_decoding_mtu_stack: String
    val log_decoding_tunnel_creation: String
    val manual_input_hint: String
    val manual_input_title: String
    val menu_delete_subscription: String
    val menu_edit_subscription: String
    val menu_file: String
    val menu_link: String
    val menu_manual_input: String
    val menu_qr_code: String
    val menu_update_subscription: String
    val menu_pin_subscription: String
    val menu_unpin_subscription: String
    val subscription_qr_dialog_title: String
    val mixedstack_desc: String
    val mtu_desc: String
    val mux_desc: String
    val notif_adaptive_tunnel_changed_body: String
    val notif_adaptive_tunnel_changed_title: String
    val notif_best_profile_body: String
    val notif_best_profile_title: String
    val notif_language_changed: String
    val notif_language_changed_auto: String
    val notif_theme_changed: String
    val notif_theme_changed_auto: String
    val notif_notifications_enabled: String
    val notif_profile_changed: String
    val onboarding_toast_notification_granted: String
    val onboarding_toast_notification_denied: String
    val onboarding_toast_battery_unrestricted: String
    val error_downloading_rule: String
    val split_presets_applied: String
    val wizard_step_ssh: String
    val wizard_step_protocol: String
    val wizard_step_settings: String
    val wizard_step_setup: String
    val notif_update_title: String
    val onboarding_battery_desc: String
    val onboarding_battery_title: String
    val onboarding_btn_go_main: String
    val onboarding_fragmentation_desc: String
    val onboarding_fragmentation_question: String
    val onboarding_fragmentation_title: String
    val onboarding_mux_desc: String
    val onboarding_mux_question: String
    val onboarding_mux_title: String
    val onboarding_notifications_desc: String
    val onboarding_notifications_error: String
    val onboarding_notifications_title: String
    val onboarding_permissions_title: String
    val onboarding_success_title: String
    val onboarding_usage_desc: String
    val onboarding_usage_title: String
    val onboarding_welcome_question: String
    val onboarding_welcome_title: String
    val onboarding_permissions_subtitle: String
    val onboarding_split_subtitle: String
    val onboarding_split_white_title: String
    val onboarding_split_white_desc: String
    val onboarding_split_black_title: String
    val onboarding_split_black_desc: String
    val onboarding_split_white_header: String
    val onboarding_split_black_header: String
    val onboarding_preset_ru_title: String
    val onboarding_preset_ru_desc: String
    val onboarding_preset_social_title: String
    val onboarding_preset_social_desc: String
    val onboarding_preset_ai_title: String
    val onboarding_preset_ai_desc: String
    val onboarding_success_desc: String
    val option_disable: String
    val option_enable: String
    val option_no: String
    val option_yes: String
    val option_auto: String
    val option_custom: String
    val dns_preset_cloudflare: String
    val dns_preset_adguard: String
    val dns_preset_google: String
    val permission_usage_stats_needed: String
    val profile_deleted_success: String
    val profile_qr_dialog_title: String
    val profile_qr_image_description: String
    val qr_camera_hint: String
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
    val search_apps_hint: String
    val servers_desc_create: String
    val servers_desc_flare: String

    val servers_protocol_shadowsocks_desc: String
    val servers_protocol_shadowsocks_title: String
    val servers_protocol_title: String
    val servers_protocol_wireguard_desc: String
    val servers_protocol_wireguard_title: String
    val servers_protocol_xray_desc: String
    val servers_protocol_xray_title: String
    val servers_setup_success: String
    val servers_setup_success_desc: String
    val servers_setup_title: String
    val servers_ssh_ip: String
    val servers_ssh_password: String
    val servers_ssh_port: String
    val servers_ssh_port_hint: String
    val servers_ssh_profile_name: String
    val servers_ssh_profile_name_hint: String
    val servers_ssh_username: String
    val servers_title_create: String
    val servers_title_flare: String
    val servers_ssh_title: String
    val servers_xray_title: String
    val servers_hysteria2_title: String
    val servers_shadowsocks_title: String
    val servers_setup_progress_title: String
    val servers_setup_success_title: String
    val servers_subscription_added_title: String
    val servers_subscription_failed_title: String
    val servers_tariff_title: String
    val tariff_free_title: String
    val tariff_plus_title: String
    val tariff_premium_title: String
    val tariff_free_desc: String
    val tariff_free_price: String
    val tariff_plus_desc: String
    val tariff_plus_price: String
    val tariff_premium_desc: String
    val tariff_premium_price: String
    val tariff_success_title: String
    val tariff_success_desc: String
    val tariff_error_title: String
    val tariff_error_desc: String
    val servers_xray_port_desc: String
    val servers_xray_port_label: String
    val servers_xray_setup_title: String
    val servers_xray_sni_desc: String
    val servers_xray_sni_label: String
    val settings_advanced_title: String
    val settings_basic_title: String
    val settings_bg_effects_header: String
    val settings_bg_effect_label: String
    val settings_bg_effect_none: String
    val settings_bg_effect_gradient: String
    val settings_bg_effect_shapes: String
    val settings_bg_effect_photo: String
    val settings_bg_effect_update_photo: String
    val settings_btn_advanced: String
    val settings_btn_base: String
    val settings_btn_change: String
    val settings_btn_change_font: String
    val settings_btn_journal: String
    val settings_color_material_you: String
    val settings_desc_adaptive_tunnel: String
    val settings_desc_best_profile: String
    val settings_desc_hwid: String
    val settings_desc_logging: String
    val settings_desc_test_url: String
    val settings_desc_update_check: String
    val settings_font_geologica: String
    val settings_header_app: String
    val settings_header_appearance: String
    val settings_header_autostart: String
    val settings_header_best_profile: String
    val settings_header_hwid: String
    val settings_header_logging: String
    val settings_header_notifications: String
    val settings_header_rules: String
    val settings_header_updates: String
    val settings_header_vpn: String
    val settings_hint_best_profile_interval: String
    val settings_hint_dns_url: String
    val settings_hint_test_url: String
    val settings_hint_update_interval: String
    val settings_item_language: String
    val settings_item_ping: String
    val settings_item_routing: String
    val settings_item_subscriptions: String
    val settings_item_theme: String
    val settings_label_adaptive_tunnel: String
    val settings_label_auto_update: String
    val settings_desc_auto_update: String
    val settings_label_autostart: String
    val settings_label_best_profile: String
    val settings_label_best_profile_interval: String
    val settings_label_best_profile_notif: String
    val settings_label_best_profile_only_connected: String
    val settings_label_core_log: String
    val settings_label_core_log_level: String
    val settings_label_custom_color: String
    val settings_label_dns_url: String
    val settings_label_enable_gradient: String
    val settings_label_fake_ip: String
    val settings_label_font: String
    val settings_label_fragment_interval: String
    val settings_label_fragment_size: String
    val settings_label_fragment_sleep: String
    val settings_label_fragmentation: String
    val settings_label_gradient_animation: String
    val settings_label_gradient_speed: String
    val settings_label_language: String
    val settings_label_mtu: String
    val settings_label_mtu_title: String
    val mtu_auto_btn: String
    val mtu_auto_warning: String
    val settings_label_mux: String
    val settings_label_mux_padding: String
    val settings_label_mux_protocol: String
    val settings_label_mux_streams: String
    val settings_label_noise_apply: String
    val settings_label_noise_delay: String
    val settings_label_noise_packet: String
    val settings_label_noise_type: String
    val settings_label_packet_type: String
    val settings_label_ping_display: String
    val settings_label_ping_style: String
    val settings_label_ping_type: String
    val settings_label_remote_dns: String
    val settings_label_rules_method: String
    val settings_header_chain: String
    val settings_label_reset_chain: String
    val settings_desc_reset_chain: String
    val settings_label_tls_spoof: String
    val settings_desc_tls_spoof: String
    val settings_label_tls_spoof_domain: String
    val settings_label_tls_spoof_method: String
    val settings_label_fingerprint: String
    val settings_item_tls_fingerprint: String
    val settings_desc_fingerprint: String
    val settings_label_send_hwid: String
    val settings_label_split_tunneling: String
    val settings_label_stack: String
    val settings_label_stack_title: String
    val settings_label_status: String
    val settings_label_status_notification: String
    val settings_label_notification_speed: String
    val settings_label_test_url: String
    val settings_label_theme: String
    val settings_label_update_check: String
    val settings_label_update_every: String
    val settings_label_update_frequency: String
    val settings_label_use: String
    val settings_label_use_fake_ip: String
    val settings_label_user_agent: String
    val settings_language_in_dev: String
    val settings_language_title: String
    val settings_ping_interval_min_warning: String
    val settings_ping_style_both: String
    val settings_ping_style_icon: String
    val settings_ping_style_time: String
    val settings_ping_title: String
    val settings_ping_type_get: String
    val settings_ping_type_icmp: String
    val settings_ping_type_tcp: String
    val settings_restart_tunnel_hint: String
    val settings_routing_title: String
    val settings_stack_header: String
    val settings_subscriptions_title: String
    val settings_theme_header: String
    val settings_theme_title: String
    val settings_title: String
    val simple_editor_alpn: String
    val simple_editor_basic: String
    val simple_editor_enable_tls: String
    val simple_editor_fingerprint: String
    val simple_editor_flow: String
    val simple_editor_packet_encoding: String
    val simple_editor_method: String
    val simple_editor_obfs: String
    val simple_editor_obfs_pass: String
    val simple_editor_pbk: String
    val simple_editor_port: String
    val simple_editor_reality: String
    val simple_editor_server: String
    val simple_editor_sid: String
    val simple_editor_sni: String
    val simple_editor_tag: String
    val simple_editor_title: String
    val simple_editor_tls: String
    val simple_editor_uuid_pwd: String
    val simple_editor_up_mbps: String
    val simple_editor_down_mbps: String
    val simple_editor_allow_insecure: String
    val simple_editor_hysteria_settings: String
    val simple_editor_hop_interval: String
    val sites_hint: String
    val split_mode_blacklist: String
    val split_mode_blacklist_tooltip: String
    val split_mode_whitelist: String
    val split_mode_whitelist_tooltip: String
    val split_tunneling_desc_default: String
    val ssh_error_config_write: String
    val ssh_error_generic: String
    val ssh_error_keys: String
    val ssh_error_port_not_listening: String
    val ssh_error_service_start: String
    val ssh_status_configuring: String
    val ssh_status_connecting: String
    val ssh_status_generating_client: String
    val ssh_status_generating_keys: String
    val ssh_status_installing_xray: String
    val ssh_status_restarting: String
    val ssh_status_waiting: String
    val startup_loading_profiles: String
    val sub_deleted_success: String
    val sub_my_servers: String
    val sub_single_profiles: String
    val sub_update_error: String
    val sub_update_error_single: String
    val sub_update_success: String
    val sub_update_success_single: String
    val success_link_copied: String
    val success_profile_added: String
    val success_profiles_added: String
    val success_subscription_added: String
    val systemstack_desc: String
    val tab_apps: String
    val tab_sites: String
    val tcp_desc: String
    val theme_auto: String
    val theme_day: String
    val theme_night: String
    val trigger_hint: String
    val trigger_label: String
    val trigger_vpn_permission_channel: String
    val trigger_vpn_permission_text: String
    val trigger_vpn_permission_title: String
    val update_available_title: String
    val update_freq_daily: String
    val update_freq_monthly: String
    val update_freq_weekly: String
    val server_manual_desc: String
    val viaproxy_desc: String
    val vpn_active: String
    val vpn_disconnect: String
    val vpn_stopping: String
    val vpn_starting: String
    val vpn_error_permission_denied: String
    val vpn_error_permission_required: String
    val vpn_error_tunnel_creation: String
    val wizard_setup_configuring: String
    val wizard_setup_free_title: String
    val wizard_setup_optimizing: String
    val wizard_setup_ready: String
    val wizard_setup_saving: String
    val wizard_setup_validating: String
    val wizard_xray_port_hint: String
    val wizard_xray_sni_hint: String
    val simple_editor_cert_pin: String
    val servers_protocol_hysteria2_title: String
    val servers_protocol_hysteria2_desc: String
    val ssh_status_installing_hysteria2: String
    val ssh_status_generating_cert: String
    val ssh_status_configuring_hysteria2: String
    val ssh_status_restarting_hysteria2: String
    val ssh_error_cert: String
    val ssh_error_port_not_listening_udp: String
    val ssh_error_service_start_hysteria2: String
    val servers_hysteria2_port_label: String
    val wizard_hysteria2_port_hint: String
    val servers_hysteria2_sni_label: String
    val wizard_hysteria2_sni_hint: String
    val servers_hysteria2_obfs_pass_label: String
    val wizard_hysteria2_obfs_pass_hint: String
    val servers_hysteria2_port_hopping_label: String
    val servers_hysteria2_port_hopping_auto: String
    val servers_hysteria2_port_hopping_manual: String
    val wizard_hysteria2_port_hopping_hint: String
    val ssh_status_installing_shadowsocks: String
    val ssh_status_configuring_shadowsocks: String
    val ssh_status_restarting_shadowsocks: String
    val ssh_error_service_start_shadowsocks: String
    val servers_shadowsocks_port_label: String
    val wizard_shadowsocks_port_hint: String
    val servers_shadowsocks_sni_label: String
    val wizard_shadowsocks_sni_hint: String
    val servers_wireguard_title: String
    val servers_wireguard_port_label: String
    val wizard_wireguard_port_hint: String
    val ssh_status_installing_wireguard: String
    val ssh_status_configuring_wireguard: String
    val ssh_status_restarting_wireguard: String
    val ssh_error_service_start_wireguard: String
    val label_shadowsocks_dpi_bypass: String
    val label_shadowsocks_dpi_bypass_hint: String
    val simple_editor_shadowtls_password: String
    val simple_editor_shadowtls_version: String
    val simple_editor_ss_network: String
    val simple_editor_ss_ws_path: String
    val simple_editor_ss_ws_host: String
    val simple_editor_tls_type: String
    val simple_editor_http_host: String
    val simple_editor_path: String
    val simple_editor_host: String
    val simple_editor_kcp_seed: String
    val simple_editor_mtu: String
    val simple_editor_tti: String
    val simple_editor_httpupgrade_host: String
    val simple_editor_httpupgrade_path: String
    val simple_editor_h2_host: String
    val simple_editor_h2_path: String
    val simple_editor_quic_security: String
    val simple_editor_quic_key: String
    val simple_editor_grpc_authority: String
    val simple_editor_grpc_service_name: String
    val simple_editor_mode: String
    val settings_header_data_mgmt: String
    val settings_label_data_mgmt: String
    val settings_desc_data_mgmt: String
    val settings_btn_data_mgmt: String
    val btn_done: String
    val data_mgmt_title: String
    val data_mgmt_export: String
    val data_mgmt_export_desc: String
    val data_mgmt_import: String
    val data_mgmt_import_desc: String
    val data_mgmt_creating: String
    val data_mgmt_created: String
    val data_mgmt_select_title: String
    val data_mgmt_restoring: String
    val data_mgmt_restored: String
    val data_mgmt_no_backups: String
    val error_profile_selection_required: String
    fun plural_apps(count: Int, vararg args: Any): String
    fun plural_sites(count: Int, vararg args: Any): String
    val status_disconnected: String
    val status_connected: String
    val status_connecting: String
    val status_disconnecting: String
}

object RuFlareStrings : FlareStrings {
    override val collapse_all: String = "Свернуть все"
    override val app_monitor_active: String = "Триггер активен!"
    override val app_name: String = "Flare"
    override val btn_add: String = "Добавить"
    override val btn_apply: String = "Применить"
    override val btn_cancel: String = "Отмена"
    override val btn_clipboard: String = "Буфер обмена"
    override val btn_download: String = "Скачать"
    override val btn_connect: String = "Подключить"
    override val btn_disconnect: String = "Отключить"
    override val btn_finish: String = "Завершить"
    override val btn_grant: String = "Выдать"
    override val btn_next: String = "Далее"
    override val btn_save: String = "Сохранить"
    override val btn_select_from_gallery: String = "Выбрать из галереи"
    override val btn_share_link: String = "Поделиться ссылкой"
    override val desc_select_apps: String = "Выберите приложения, которые будут использовать VPN."
    override val dialog_apps_title: String = "Приложения"
    override val dialog_domens_title: String = "Домены"
    override val edit_sub_name_hint: String = "Название подписки"
    override val edit_sub_title: String = "Редактирование подписки"
    override val edit_sub_url_hint: String = "URL Подписки"
    override val empty_profiles_hint: String = "Пока что нет добавленных профилей!"
    override val error_apps_list_empty: String = "Список пуст. Проверьте разрешение на список приложений в настройках."
    override val error_camera_permission_denied: String = "Разрешение на камеру отклонено"
    override val error_clipboard_empty: String = "Буфер обмена пуст"
    override val error_empty_name: String = "Название не может быть пустым"
    override val error_import_failed: String = "Не удалось добавить подписку или профиль!"
    override val error_import_file_read: String = "Не удалось прочитать файл"
    override val error_import_file_type: String = "Поддерживаются только файлы .txt и .json"
    override val error_import_timeout: String = "Не удалось добавить подписку или профиль! (таймаут 10 сек)"
    override val error_invalid_format: String = "Неверный формат. Поддерживаются: vless://, vmess://, ss://, trojan://, hysteria://, hy://, hysteria2://, hy2://, https:// и JSON"
    override val error_json: String = "Ошибка JSON: %s"
    override val error_link_generation: String = "Не удалось сгенерировать ссылку"
    override val error_open_settings: String = "Не удалось открыть настройки системы"
    override val error_parsing: String = "Ошибка парсинга: %s"
    override val error_profile_qr_generation: String = "Не удалось сгенерировать QR-код профиля"
    override val error_qr_not_found_in_image: String = "QR-код не найден на изображении"
    override val error_qr_scan_empty: String = "Не удалось распознать QR-код"
    override val error_subscription: String = "Ошибка подписки: %s"
    override val error_subscription_empty: String = "Подписка пуста"
    override val error_subscription_https_required: String = "Ссылка на подписку должна использовать HTTPS"
    override val fakeip_desc: String = "Мгновенно выдает системе «поддельный» адрес для домена, не дожидаясь ответа от DNS-сервера. Предотвращает утечку DNS"
    override val feature_coming_soon: String = "Функция скоро появится"
    override val fragment_desc: String = "Разделение больших пакетов данных на более мелкие части. Помогает в обходе блокировок (DPI)"
    override val gvisorstack_desc: String = "Высокая совместимость, поддерживает все настройки туннеля, среднее энергопотребление."
    override val hint_add_first_profile: String = "Используйте кнопку ниже для добавления или зажмите для выбора способа!"
    override val icmp_desc: String = "Проверяет доступен ли сервер, используется для базовой проверки связи с сервером."
    override val journal_clear: String = "Очистить"
    override val journal_title: String = "Журнал событий"
    override val journal_waiting_logs: String = "Ожидание новых событий…"
    override val journal_copy_success: String = "События успешно скопированы в буфер обмена!"
    override val json_edit_success: String = "JSON %s был успешно изменен."
    override val label_add_profiles: String = "Добавить профили"
    override val label_and: String = " и "
    override val label_config_editor: String = "Редактор конфига"
    override val label_credentials: String = "Данные"
    override val label_error: String = "Ошибка"
    override val label_errors: String = "Ошибки: "
    override val label_expires: String = "Истекает: %s"
    override val label_update_interval: String = "Обновление: %s"
    override val settings_label_use_sub_interval: String = "Интервал подписок"
    override val settings_desc_use_sub_interval: String = "Обновляет подписки по их заданному интервалу например: Обновление 1 ч."
    override val label_imported_profile: String = "Imported Profile"
    override val label_json_data: String = "Данные JSON"
    override val label_logs: String = "Журнал:"
    override val label_mode: String = "Режим"
    override val label_output: String = "Вывод: "
    override val label_password: String = "Пароль"
    override val label_profile_name: String = "Название профиля"
    override val label_seconds_short: String = "с"
    override val label_selected: String = "Выбрано"
    override val label_servers: String = "Сервера"
    override val label_speed_test: String = "Тест скорости"
    override val label_support: String = "Поддержка"
    override val label_unknown: String = "Неизвестно"
    override val label_update: String = "Обновить"
    override val label_uuid: String = "UUID"
    override val language_auto: String = "Авто (система)"
    override val language_en: String = "English"
    override val language_restart_hint: String = "Почти готово! Нужно перезапустить приложение."
    override val language_ru: String = "Русский"
    override val log_decoding_fragmentation: String = "Фрагментация включена"
    override val log_decoding_mtu_stack: String = "MTU %1\$s, STACK %2\$s"
    override val log_decoding_tunnel_creation: String = "Создание туннеля..."
    override val manual_input_hint: String = "vless://, vmess://, ss://, hysteria2:// (hy2://), hysteria:// (hy://) или ссылка на подписку"
    override val manual_input_title: String = "Ручной ввод"
    override val menu_delete_subscription: String = "Удалить"
    override val menu_edit_subscription: String = "Редактировать"
    override val menu_file: String = "Файл"
    override val menu_link: String = "Ссылка"
    override val menu_manual_input: String = "Ручной ввод"
    override val menu_qr_code: String = "QR-Код"
    override val menu_update_subscription: String = "Обновить"
    override val menu_pin_subscription: String = "Закрепить"
    override val menu_unpin_subscription: String = "Открепить"
    override val subscription_qr_dialog_title: String = "QR-код подписки"
    override val mixedstack_desc: String = "Средняя совместимость, поддерживает большую часть настроек туннеля, высокое энергопотребление."
    override val mtu_desc: String = "Максимальный размер одного пакета данных (в байтах), который может быть передан за один раз."
    override val mux_desc: String = "Объединяет несколько запросов в одно соединение. Снижает задержку на создание новых подключений и ускоряет загрузку."
    override val notif_adaptive_tunnel_changed_body: String = "Профиль был изменен на %1\$s после обрыва соединения."
    override val notif_adaptive_tunnel_changed_title: String = "Профиль изменен"
    override val notif_best_profile_body: String = "Выбор профиля был обновлен на %1\$s с пингом %2\$dms"
    override val notif_best_profile_title: String = "Обновление профиля"
    override val notif_language_changed: String = "Язык приложения изменен на %s"
    override val notif_language_changed_auto: String = "Язык приложения был изменен!"
    override val notif_theme_changed: String = "Тема приложения была успешно изменена!"
    override val notif_theme_changed_auto: String = "Тема приложения была изменена автоматически!"
    override val notif_notifications_enabled: String = "Уведомления успешно включены!"
    override val notif_profile_changed: String = "Данные профиля изменены!"
    override val onboarding_toast_notification_granted: String = "Разрешение на уведомления получено"
    override val onboarding_toast_notification_denied: String = "Уведомления отключены"
    override val onboarding_toast_battery_unrestricted: String = "Энергопотребление настроено"
    override val error_downloading_rule: String = "Ошибка загрузки %1\$s: %2\$s"
    override val split_presets_applied: String = "Настройки раздельного туннелирования применены!"
    override val wizard_step_ssh: String = "SSH"
    override val wizard_step_protocol: String = "Протокол"
    override val wizard_step_settings: String = "Настройки"
    override val wizard_step_setup: String = "Установка"
    override val notif_update_title: String = "Обновление Flare"
    override val onboarding_battery_desc: String = "Что-бы приложение работало стабильно и Android его не закрывал нужно отключить экономию энергии для Flare"
    override val onboarding_battery_title: String = "Энергопотребление"
    override val onboarding_btn_go_main: String = "На главную"
    override val onboarding_fragmentation_desc: String = "Фрагментация помогает разделить пакет на несколько частей что помогает для обхода блокировок (DPI)"
    override val onboarding_fragmentation_question: String = "Хотели бы включить фрагментацию?"
    override val onboarding_fragmentation_title: String = "Фрагментация"
    override val onboarding_mux_desc: String = "Mux помогает ускорить соединение, но плохо подходит для обхода блокировок"
    override val onboarding_mux_question: String = "Хотели бы использовать Mux?"
    override val onboarding_mux_title: String = "Мультиплексирование"
    override val onboarding_notifications_desc: String = "Уведомления нужны чтобы приложение работало в фоне 24/7 а так же что-бы видеть актуальный статус туннеля"
    override val onboarding_notifications_error: String = "Разрешение на уведомления отклонено"
    override val onboarding_notifications_title: String = "Уведомления"
    override val onboarding_permissions_title: String = "Разрешения"
    override val onboarding_success_title: String = "Настройка успешно пройдена!"
    override val onboarding_usage_desc: String = "Это разрешение нужно для мониторинга приложений\\nЧтобы функция \\\"Триггер\\\" могла корректно работать"
    override val onboarding_usage_title: String = "Статистика использования"
    override val onboarding_welcome_question: String = "Хотите пройти первоначальную настройку?"
    override val onboarding_welcome_title: String = "Добро пожаловать в Flare!"
    override val onboarding_permissions_subtitle: String = "Необходимые разрешения для стабильной фоновой работы"
    override val onboarding_split_subtitle: String = "Выберите режим проксирования (можно пропустить)"
    override val onboarding_split_white_title: String = "Белый список"
    override val onboarding_split_white_desc: String = "Через прокси работают только выбранные приложения и сайты"
    override val onboarding_split_black_title: String = "Черный список"
    override val onboarding_split_black_desc: String = "Все работает через прокси кроме выбранных сайтов и приложений"
    override val onboarding_split_white_header: String = "Белый список: Что будет работать через прокси?"
    override val onboarding_split_black_header: String = "Черный список: Что НЕ будет работать через прокси?"
    override val onboarding_preset_ru_title: String = "Российские сервисы"
    override val onboarding_preset_ru_desc: String = "Госуслуги, Яндекс, банки, и другое"
    override val onboarding_preset_social_title: String = "Соцсети"
    override val onboarding_preset_social_desc: String = "Telegram, WhatsApp, и др"
    override val onboarding_preset_ai_title: String = "ИИ"
    override val onboarding_preset_ai_desc: String = "Gemini, Chat GPT, Claude и др"
    override val onboarding_success_desc: String = "Теперь Flare полностью настроен. Вы можете добавить профили и начать использование."
    override val option_disable: String = "Отключить"
    override val option_enable: String = "Включить"
    override val option_no: String = "Нет"
    override val option_yes: String = "Да"
    override val option_auto: String = "Авто"
    override val option_custom: String = "Свой URL"
    override val dns_preset_cloudflare: String = "Cloudflare DoH"
    override val dns_preset_adguard: String = "AdGuard DNS (Антиреклама)"
    override val dns_preset_google: String = "Google DoT"
    override val permission_usage_stats_needed: String = "Для работы функции «Триггер» необходимо разрешение на доступ к статистике использования."
    override val profile_deleted_success: String = "Профиль %s успешно удален!"
    override val profile_qr_dialog_title: String = "QR-код профиля"
    override val profile_qr_image_description: String = "QR-код ссылки профиля"
    override val qr_camera_hint: String = "Наведите камеру на QR-код"
    override val routing_action_download: String = "Скачать"
    override val routing_badge_builtin: String = "Встроено"
    override val routing_badge_soon: String = "Скоро"
    override val routing_card_ads: String = "Антиреклама"
    override val routing_card_ads_desc: String = "Блокировка рекламных доменов (geosite-ads)"
    override val routing_card_cn: String = "Китай (CN)"
    override val routing_card_cn_desc: String = "Китайские сайты и IP-адреса"
    override val routing_card_global: String = "Глобальные правила"
    override val routing_card_global_desc: String = "Bypass China, Google, YouTube и др."
    override val routing_card_media: String = "Медиа и Стриминг"
    override val routing_card_media_desc: String = "YouTube, Netflix, Twitch, Disney+"
    override val routing_card_ru: String = "RU"
    override val routing_card_social: String = "Соцсети"
    override val routing_card_social_desc: String = "Telegram, Instagram, Facebook, TikTok"
    override val routing_desc_no_update: String = "Не требует обновления"
    override val routing_last_update: String = "%s"
    override val routing_mode_block: String = "Block"
    override val routing_mode_direct: String = "Direct"
    override val routing_mode_proxy: String = "Proxy"
    override val routing_status_downloaded: String = "Правило скачано"
    override val routing_status_updated: String = "Правило обновлено"
    override val routing_success_generic: String = "Правило %s успешно обновлено!"
    override val routing_update_error: String = "Ошибка обновления баз!"
    override val routing_update_never: String = "Никогда"
    override val routing_update_success: String = "Базы RU успешно обновлены!"
    override val rules_method_direct: String = "Напрямую"
    override val rules_method_proxy: String = "Через прокси"
    override val search_apps_hint: String = "Поиск приложений..."
    override val server_manual_desc: String = "Введите данные для подключения"
    override val servers_desc_create: String = "Ваш собственный сервер, который контролируете только вы."
    override val servers_desc_flare: String = "Flare Servers: анонимные, не хранят логи, работают быстро и доступны 24/7."

    override val servers_protocol_shadowsocks_desc: String = "Энергоэффективный и быстрый протокол шифрования на базе SOCKS5 с методами AEAD. Обеспечивает высокую производительность и защиту данных при минимальной нагрузке."
    override val servers_protocol_shadowsocks_title: String = "ShadowSocks"
    override val servers_protocol_title: String = "Выберите протокол"
    override val servers_protocol_wireguard_desc: String = "Легковесный и современный протокол сетевого уровня на базе UDP с передовой криптографией. Обеспечивает мгновенное подключение и максимальную пропускную способность."
    override val servers_protocol_wireguard_title: String = "WireGuard"
    override val servers_protocol_xray_desc: String = "Xray с REALITY маскирует VPN-Трафик под веб-трафик. Высокая устойчивость к обнаружению, обеспечивает высокую приватность и скорость"
    override val servers_protocol_xray_title: String = "Xray с REALITY"
    override val servers_setup_success: String = "Сервер был успешно создан!"
    override val servers_setup_success_desc: String = "Вы можете найти его внутри подписки \"Мои сервера\""
    override val servers_setup_title: String = "Настройка сервера..."
    override val servers_ssh_ip: String = "IP-адрес"
    override val servers_ssh_password: String = "Пароль или ключ SSH"
    override val servers_ssh_port: String = "Порт SSH"
    override val servers_ssh_port_hint: String = "22"
    override val servers_ssh_profile_name: String = "Имя профиля"
    override val servers_ssh_profile_name_hint: String = "Мой сервер"
    override val servers_ssh_username: String = "Имя пользователя SSH"
    override val servers_title_create: String = "Создать свой сервер"
    override val servers_title_flare: String = "Сервера Flare"
    override val servers_ssh_title: String = "Параметры подключения SSH"
    override val servers_xray_title: String = "Параметры Xray"
    override val servers_hysteria2_title: String = "Параметры Hysteria 2"
    override val servers_shadowsocks_title: String = "Параметры Shadowsocks"
    override val servers_setup_progress_title: String = "Установка и настройка"
    override val servers_setup_success_title: String = "Установка завершена"
    override val servers_subscription_added_title: String = "Подписка добавлена"
    override val servers_subscription_failed_title: String = "Ошибка добавления"
    override val servers_tariff_title: String = "Выберите план"
    override val tariff_free_title: String = "Free"
    override val tariff_plus_title: String = "Plus"
    override val tariff_premium_title: String = "Premium"
    override val tariff_free_desc: String = "Средняя скорость, обеспечивает базовый доступ ко многим сервисам"
    override val tariff_free_price: String = "0р/мес"
    override val tariff_plus_desc: String = "Высокая скорость, низкая задержка, доступ ко всем сервисам, обход белых списков"
    override val tariff_plus_price: String = "200р/мес"
    override val tariff_premium_desc: String = "Максимальная стабильность и скорость, больше серверов"
    override val tariff_premium_price: String = "400р/мес"
    override val tariff_success_title: String = "Подписка Free добавлена"
    override val tariff_success_desc: String = "вы найдете ее в списке"
    override val tariff_error_title: String = "Не удалось добавить подписку Free"
    override val tariff_error_desc: String = "попробуйте позже"
    override val servers_xray_port_desc: String = "Порт, на котором будет работать ваш VPN сервеp. 443 — стандартный порт для маскировки под HTTPS."
    override val servers_xray_port_label: String = "Порт Xray"
    override val servers_xray_setup_title: String = "Настройка Xray"
    override val servers_xray_sni_desc: String = "Домен, под который будет маскироваться ваш трафик. Google.com — надежный вариант по умолчанию."
    override val servers_xray_sni_label: String = "SNI (Server Name Indication)"
    override val settings_advanced_title: String = "Расширенные настройки"
    override val settings_basic_title: String = "Базовые настройки"
    override val settings_bg_effects_header: String = "Фоновые эффекты"
    override val settings_bg_effect_label: String = "Эффект"
    override val settings_bg_effect_none: String = "Выключен"
    override val settings_bg_effect_gradient: String = "Градиент"
    override val settings_bg_effect_shapes: String = "Фигуры"
    override val settings_bg_effect_photo: String = "Фото"
    override val settings_bg_effect_update_photo: String = "Обновить фото"
    override val settings_btn_advanced: String = "Расширенные"
    override val settings_btn_base: String = "Базовые"
    override val settings_btn_change: String = "Изменить"
    override val settings_btn_change_font: String = "Изменить шрифт"
    override val settings_btn_journal: String = "Журнал"
    override val settings_color_material_you: String = "Material You"
    override val settings_desc_adaptive_tunnel: String = "Автоматически восстанавливает соединение при обрыве или выбирает рабочий сервер"
    override val settings_desc_best_profile: String = "Данная функция выбирает лучший сервер в подписке с самым маленьким пингом"
    override val settings_desc_hwid: String = "HWID - идентификатор для привязки подписки к вашему устройству. Позволяет повторно импортировать подписку без расхода лимита устройств. Данные не передаются."
    override val settings_desc_logging: String = "Включение логов полезно для отладки, но может раскрыть ваши серверные адреса и ключи."
    override val settings_desc_test_url: String = "Данный параметр отвечает за то какая ссылка будет использована для проверки задержки"
    override val settings_desc_update_check: String = "Проверка обновлений помогает вам использовать актуальную версию Flare."
    override val settings_font_geologica: String = "Geologica"
    override val settings_header_app: String = "Настройки приложения"
    override val settings_header_appearance: String = "Оформление"
    override val settings_header_autostart: String = "Автозапуск"
    override val settings_header_best_profile: String = "Управление профилями"
    override val settings_header_hwid: String = "Управление HWID"
    override val settings_header_logging: String = "Логирование"
    override val settings_header_notifications: String = "Уведомления"
    override val settings_header_rules: String = "Правила"
    override val settings_header_updates: String = "Обновление"
    override val settings_header_vpn: String = "Настройки VPN"
    override val settings_hint_best_profile_interval: String = "1800"
    override val settings_hint_dns_url: String = "Auto"
    override val settings_hint_test_url: String = "https://www.google.com/generate_204"
    override val settings_hint_update_interval: String = "3600"
    override val settings_item_language: String = "Язык"
    override val settings_item_ping: String = "Пинг"
    override val settings_item_routing: String = "Маршрутизация"
    override val settings_item_subscriptions: String = "Подписки"
    override val settings_item_theme: String = "Тема и шрифт"
    override val settings_label_adaptive_tunnel: String = "Адаптивный туннель"
    override val settings_label_auto_update: String = "Автообновление"
    override val settings_desc_auto_update: String = "Принудительно обновляет все подписки с заданным интервалом например раз в 3600 секунд"
    override val settings_label_autostart: String = "Создавать туннель при запуске приложения"
    override val settings_label_best_profile: String = "Автовыбор профиля"
    override val settings_label_best_profile_interval: String = "Обновлять выбор каждые"
    override val settings_label_best_profile_notif: String = "Выбор лучшего профиля"
    override val settings_label_best_profile_only_connected: String = "Только если туннель запущен"
    override val settings_label_core_log: String = "Журнал SingBox"
    override val settings_label_core_log_level: String = "Уровень логирования"
    override val settings_label_custom_color: String = "Свой цвет"
    override val settings_label_dns_url: String = "URL"
    override val settings_label_enable_gradient: String = "Градиент"
    override val settings_label_fake_ip: String = "Поддельная DNS (Fake IP)"
    override val settings_label_font: String = "Шрифт"
    override val settings_label_fragment_interval: String = "Тайм-аут"
    override val settings_label_fragment_size: String = "Размер"
    override val settings_label_fragment_sleep: String = "Задержка"
    override val settings_label_fragmentation: String = "Фрагментация"
    override val settings_label_gradient_animation: String = "Анимация градиента"
    override val settings_label_gradient_speed: String = "Скорость анимации"
    override val settings_label_language: String = "Язык приложения"
    override val settings_label_mtu: String = "MTU"
    override val settings_label_mtu_title: String = "Изменение MTU"
    override val mtu_auto_btn: String = "Авто"
    override val mtu_auto_warning: String = "Установлен оптимальный MTU: %s"
    override val settings_label_mux: String = "Mux"
    override val settings_label_mux_padding: String = "Добавить шум"
    override val settings_label_mux_protocol: String = "Способ"
    override val settings_label_mux_streams: String = "Кол-во потоков"
    override val settings_label_noise_apply: String = "Применить к ip"
    override val settings_label_noise_delay: String = "Задержка"
    override val settings_label_noise_packet: String = "Пакет"
    override val settings_label_noise_type: String = "Тип"
    override val settings_label_packet_type: String = "Откат"
    override val settings_label_ping_display: String = "Отображение пинга"
    override val settings_label_ping_style: String = "Стиль"
    override val settings_label_ping_type: String = "Тип пинга"
    override val settings_label_remote_dns: String = "Remote DNS"
    override val settings_label_rules_method: String = "Способ"
    override val settings_header_chain: String = "Управление цепью"
    override val settings_label_reset_chain: String = "Сбрасывать цепь после отключения"
    override val settings_desc_reset_chain: String = "Автоматически очищать цепочку прокси после остановки VPN."
    override val settings_label_tls_spoof: String = "TLS Spoof"
    override val settings_desc_tls_spoof: String = "Подмена SNI для обхода блокировок. Отправляет поддельный ClientHello с белым доменом перед настоящим."
    override val settings_label_tls_spoof_domain: String = "Домен"
    override val settings_label_tls_spoof_method: String = "Метод"
    override val settings_label_fingerprint: String = "Отпечаток"
    override val settings_item_tls_fingerprint: String = "TLS Отпечаток"
    override val settings_desc_fingerprint: String = "Маскирует ваш TLS трафик под выбранный браузер/клиент. Если выбрано Auto, отпечаток будет взят из конфигурации."
    override val settings_label_send_hwid: String = "Передавать HWID"
    override val settings_label_split_tunneling: String = "Раздельное туннелирование"
    override val settings_label_stack: String = "%s"
    override val settings_label_stack_title: String = "Использовать"
    override val settings_label_status: String = "Статус"
    override val settings_label_status_notification: String = "Уведомление с статусом"
    override val settings_label_notification_speed: String = "Показывать скорость"
    override val settings_label_test_url: String = "Test-URL"
    override val settings_label_theme: String = "Стиль"
    override val settings_label_update_check: String = "Проверка обновлений"
    override val settings_label_update_every: String = "Обновлять подписки каждые"
    override val settings_label_update_frequency: String = "Частота проверки"
    override val settings_label_use: String = "Использовать"
    override val settings_label_use_fake_ip: String = "Включить Fake IP"
    override val settings_label_user_agent: String = "User-Agent"
    override val settings_language_in_dev: String = "Смена языка (в разработке)"
    override val settings_language_title: String = "Язык"
    override val settings_ping_interval_min_warning: String = "Минимальный интервал — 10 секунд"
    override val settings_ping_style_both: String = "Время и значок"
    override val settings_ping_style_icon: String = "Значок"
    override val settings_ping_style_time: String = "Время"
    override val settings_ping_title: String = "Настройки пинга"
    override val settings_ping_type_get: String = "via proxy"
    override val settings_ping_type_icmp: String = "ICMP"
    override val settings_ping_type_tcp: String = "TCP"
    override val settings_restart_tunnel_hint: String = "Настройки будут применены при следующем создании туннеля."
    override val settings_routing_title: String = "Маршрутизация"
    override val settings_stack_header: String = "Сетевой стек"
    override val settings_subscriptions_title: String = "Подписки"
    override val settings_theme_header: String = "Тема"
    override val settings_theme_title: String = "Тема и шрифт"
    override val settings_title: String = "Настройки"
    override val simple_editor_alpn: String = "ALPN"
    override val simple_editor_basic: String = "Базовые настройки"
    override val simple_editor_enable_tls: String = "Включить TLS"
    override val simple_editor_fingerprint: String = "Fingerprint"
    override val simple_editor_flow: String = "Flow"
    override val simple_editor_packet_encoding: String = "Packet Encoding"
    override val simple_editor_method: String = "Метод шифрования"
    override val simple_editor_obfs: String = "Obfs"
    override val simple_editor_obfs_pass: String = "Obfs Пароль"
    override val simple_editor_pbk: String = "Public Key"
    override val simple_editor_port: String = "Порт"
    override val simple_editor_reality: String = "Настройки Reality"
    override val simple_editor_server: String = "Сервер (Host или IP)"
    override val simple_editor_sid: String = "Short ID"
    override val simple_editor_sni: String = "SNI"
    override val simple_editor_tag: String = "Имя профиля"
    override val simple_editor_title: String = "Редактор профиля"
    override val simple_editor_tls: String = "Настройки TLS"
    override val simple_editor_uuid_pwd: String = "UUID / Пароль"
    override val simple_editor_up_mbps: String = "Скорость отдачи (Up Mbps)"
    override val simple_editor_down_mbps: String = "Скорость загрузки (Down Mbps)"
    override val simple_editor_allow_insecure: String = "Разрешить небезопасный TLS (Insecure)"
    override val simple_editor_hysteria_settings: String = "Настройки Hysteria"
    override val simple_editor_hop_interval: String = "Интервал смены порта (Hop interval)"
    override val sites_hint: String = "site1.com\nsite2.com"
    override val split_mode_blacklist: String = "Черный список"
    override val split_mode_blacklist_tooltip: String = "В этом режиме все сайты и приложения работают через VPN кроме выбранных"
    override val split_mode_whitelist: String = "Белый список"
    override val split_mode_whitelist_tooltip: String = "В этом режиме только выбранные сайты и приложения работают через VPN, остальные напрямую"
    override val split_tunneling_desc_default: String = "Выберите сайты и приложения которые работают через VPN или напрямую."
    override val ssh_error_config_write: String = "Файл конфига не был записан на сервер"
    override val ssh_error_generic: String = "Ошибка: %s"
    override val ssh_error_keys: String = "Не удалось получить ключи REALITY."
    override val ssh_error_port_not_listening: String = "Xray запущен, но не слушает порт %d!"
    override val ssh_error_service_start: String = "Сервис Xray не запустился (статус: %s)"
    override val ssh_status_configuring: String = "Настройка конфигурации..."
    override val ssh_status_connecting: String = "Подключение к серверу..."
    override val ssh_status_generating_client: String = "Генерация настроек клиента..."
    override val ssh_status_generating_keys: String = "Генерация ключей REALITY..."
    override val ssh_status_installing_xray: String = "Установка Xray..."
    override val ssh_status_restarting: String = "Перезапуск сервиса Xray..."
    override val ssh_status_waiting: String = "Ожидание запуска..."
    override val startup_loading_profiles: String = "Загружаем профили и настройки..."
    override val sub_deleted_success: String = "Подписка %s была удалена!"
    override val sub_my_servers: String = "Мои сервера"
    override val sub_single_profiles: String = "Список профилей"
    override val sub_update_error: String = "Не удалось обновить все подписки!"
    override val sub_update_error_single: String = "Не удалось обновить подписку!"
    override val sub_update_success: String = "%d Подписок было успешно обновлено."
    override val sub_update_success_single: String = "Подписка %s была успешно обновлена."
    override val success_link_copied: String = "Ссылка скопирована"
    override val success_profile_added: String = "Профиль %s успешно добавлен!"
    override val success_profiles_added: String = "Успешно добавлено профилей: %d"
    override val success_subscription_added: String = "Подписка %s успешно добавлена!"
    override val systemstack_desc: String = "Низкая совместимость, не поддерживает фрагментацию и еще некоторые настройки туннеля, низкое энергопотребление."
    override val tab_apps: String = "Приложения"
    override val tab_sites: String = "Сайты"
    override val tcp_desc: String = "Проверяет скорость открытия порта на сервере и готов ли он принимать соединения."
    override val theme_auto: String = "Авто"
    override val theme_day: String = "День"
    override val theme_night: String = "Ночь"
    override val trigger_hint: String = "Данный параметр делает так, что VPN подключается только при использовании определенных приложений"
    override val trigger_label: String = "Триггер"
    override val trigger_vpn_permission_channel: String = "Разрешение VPN для триггера"
    override val trigger_vpn_permission_text: String = "Откройте приложение и подтвердите разрешение, чтобы триггер мог запустить туннель."
    override val trigger_vpn_permission_title: String = "Триггеру нужно разрешение VPN"
    override val update_available_title: String = "Доступна новая версия Flare %s!"
    override val update_freq_daily: String = "Ежедневно"
    override val update_freq_monthly: String = "Ежемесячно"
    override val update_freq_weekly: String = "Еженедельно"
    override val viaproxy_desc: String = "Проверяет время полного прохождения HTTP запроса через прокси, тестирует реальную задержку, самый точный метод."
    override val vpn_active: String = "VPN активен"
    override val vpn_disconnect: String = "Отключить"
    override val vpn_stopping: String = "Остановка служб"
    override val vpn_starting: String = "Запуск служб"
    override val vpn_error_permission_denied: String = "Разрешение VPN отклонено"
    override val vpn_error_permission_required: String = "Требуется разрешение на VPN. Пожалуйста, откройте приложение и подтвердите его."
    override val vpn_error_tunnel_creation: String = "Не удалось создать туннель"
    override val wizard_setup_configuring: String = "Настройка сервера..."
    override val wizard_setup_free_title: String = "Настраиваем вашу подписку..."
    override val wizard_setup_optimizing: String = "Оптимизация..."
    override val wizard_setup_ready: String = "Сервер готов!"
    override val wizard_setup_saving: String = "Сохранение..."
    override val wizard_setup_validating: String = "Проверка данных..."
    override val wizard_xray_port_hint: String = "443 (по умолчанию)"
    override val wizard_xray_sni_hint: String = "google.com (по умолчанию)"
    override val simple_editor_cert_pin: String = "Отпечаток сертификата SHA-256"
    override val servers_protocol_hysteria2_title: String = "Hysteria 2"
    override val servers_protocol_hysteria2_desc: String = "Высокоскоростной протокол на базе UDP (QUIC) с маскировкой под HTTPS и встроенным обходом блокировок."
    override val ssh_status_installing_hysteria2: String = "Установка Hysteria 2..."
    override val ssh_status_generating_cert: String = "Генерация TLS сертификата..."
    override val ssh_status_configuring_hysteria2: String = "Настройка Hysteria 2..."
    override val ssh_status_restarting_hysteria2: String = "Перезапуск сервиса Hysteria 2..."
    override val ssh_error_cert: String = "Не удалось сгенерировать TLS сертификат."
    override val ssh_error_port_not_listening_udp: String = "Hysteria 2 запущена, но не слушает порт %d (UDP)!"
    override val ssh_error_service_start_hysteria2: String = "Сервис Hysteria 2 не запустился (статус: %s)"
    override val servers_hysteria2_port_label: String = "Порт Hysteria 2"
    override val wizard_hysteria2_port_hint: String = "443 (по умолчанию)"
    override val servers_hysteria2_sni_label: String = "Домен маскировки (SNI)"
    override val wizard_hysteria2_sni_hint: String = "google.com (по умолчанию)"
    override val servers_hysteria2_obfs_pass_label: String = "Obfs Пароль (Опционально)"
    override val wizard_hysteria2_obfs_pass_hint: String = "salamander_pass (по умолчанию)"
    override val servers_hysteria2_port_hopping_label: String = "Port Hopping"
    override val servers_hysteria2_port_hopping_auto: String = "Авто"
    override val servers_hysteria2_port_hopping_manual: String = "Вручную"
    override val wizard_hysteria2_port_hopping_hint: String = "Диапазон, например 20000-50000"
    override val ssh_status_installing_shadowsocks: String = "Установка Shadowsocks..."
    override val ssh_status_configuring_shadowsocks: String = "Настройка Shadowsocks..."
    override val ssh_status_restarting_shadowsocks: String = "Перезапуск сервиса Shadowsocks..."
    override val ssh_error_service_start_shadowsocks: String = "Сервис Shadowsocks не запустился (статус: %s)"
    override val servers_shadowsocks_port_label: String = "Порт Shadowsocks"
    override val wizard_shadowsocks_port_hint: String = "8388 (по умолчанию)"
    override val servers_shadowsocks_sni_label: String = "Домен маскировки (SNI)"
    override val wizard_shadowsocks_sni_hint: String = "google.com (по умолчанию)"
    override val servers_wireguard_title: String = "Параметры WireGuard"
    override val servers_wireguard_port_label: String = "Порт WireGuard"
    override val wizard_wireguard_port_hint: String = "51820 (по умолчанию)"
    override val ssh_status_installing_wireguard: String = "Установка WireGuard..."
    override val ssh_status_configuring_wireguard: String = "Настройка WireGuard..."
    override val ssh_status_restarting_wireguard: String = "Запуск службы WireGuard..."
    override val ssh_error_service_start_wireguard: String = "Не удалось запустить службу WireGuard. Статус: %s"
    override val label_shadowsocks_dpi_bypass: String = "Плагины обхода DPI"
    override val label_shadowsocks_dpi_bypass_hint: String = "Маскировать Shadowsocks под HTTPS-трафик (WebSocket + TLS)"
    override val simple_editor_shadowtls_password: String = "ShadowTLS Пароль"
    override val simple_editor_shadowtls_version: String = "ShadowTLS Версия"
    override val simple_editor_ss_network: String = "Сеть (Transport)"
    override val simple_editor_ss_ws_path: String = "WebSocket Путь"
    override val simple_editor_ss_ws_host: String = "WebSocket Host (Заголовок)"
    override val simple_editor_tls_type: String = "Тип TLS"
    override val simple_editor_http_host: String = "HTTP Host"
    override val simple_editor_path: String = "Путь"
    override val simple_editor_host: String = "Хост (Host)"
    override val simple_editor_kcp_seed: String = "KCP Seed"
    override val simple_editor_mtu: String = "MTU"
    override val simple_editor_tti: String = "TTI"
    override val simple_editor_httpupgrade_host: String = "HTTPUpgrade Host"
    override val simple_editor_httpupgrade_path: String = "HTTPUpgrade Путь"
    override val simple_editor_h2_host: String = "H2 Host"
    override val simple_editor_h2_path: String = "H2 Путь"
    override val simple_editor_quic_security: String = "QUIC Шифрование"
    override val simple_editor_quic_key: String = "QUIC Ключ"
    override val simple_editor_grpc_authority: String = "gRPC Authority"
    override val simple_editor_grpc_service_name: String = "gRPC serviceName"
    override val simple_editor_mode: String = "Режим"
    override val settings_header_data_mgmt: String = "Управление данными"
    override val settings_label_data_mgmt: String = "Сохранение и восстановление"
    override val settings_desc_data_mgmt: String = "Вы можете сохранить все настройки, профили, подписки приложения"
    override val settings_btn_data_mgmt: String = "Перенести"
    override val btn_done: String = "Готово"
    override val data_mgmt_title: String = "Управление данными"
    override val data_mgmt_export: String = "Экспорт"
    override val data_mgmt_export_desc: String = "Создать файл с копией ваших данных"
    override val data_mgmt_import: String = "Импорт"
    override val data_mgmt_import_desc: String = "Загрузить данные из существующего файла"
    override val data_mgmt_creating: String = "Создание копии..."
    override val data_mgmt_created: String = "Копия успешно создана!"
    override val data_mgmt_select_title: String = "Какую копию восстановить?"
    override val data_mgmt_restoring: String = "Восстановление копии..."
    override val data_mgmt_restored: String = "Копия успешно восстановлена"
    override val data_mgmt_no_backups: String = "Копии для восстановления не найдены"
    override fun plural_apps(count: Int, vararg args: Any): String {
        val res = when {
            count % 10 == 1 && count % 100 != 11 -> "приложение"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "приложения"
            else -> "приложений"
        }
        return res.format(*args)
    }
    override val error_profile_selection_required: String = "Сначала выберите профиль!"
    override fun plural_sites(count: Int, vararg args: Any): String {
        val res = when {
            count % 10 == 1 && count % 100 != 11 -> "сайт"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "сайта"
            else -> "сайтов"
        }
        return res.format(*args)
    }
    override val status_disconnected: String = "Отключено"
    override val status_connected: String = "Подключено"
    override val status_connecting: String = "Подключение..."
    override val status_disconnecting: String = "Отключение..."
}

object EnFlareStrings : FlareStrings {
    override val collapse_all: String = "Collapse all"
    override val app_monitor_active: String = "Trigger is active!"
    override val app_name: String = "Flare"
    override val btn_add: String = "Add"
    override val btn_apply: String = "Apply"
    override val btn_cancel: String = "Cancel"
    override val btn_clipboard: String = "Clipboard"
    override val btn_download: String = "Download"
    override val btn_connect: String = "Connect"
    override val btn_disconnect: String = "Disconnect"
    override val btn_finish: String = "Finish"
    override val btn_grant: String = "Grant"
    override val btn_next: String = "Next"
    override val btn_save: String = "Save"
    override val btn_select_from_gallery: String = "Choose from gallery"
    override val btn_share_link: String = "Share link"
    override val desc_select_apps: String = "Select apps that will use VPN."
    override val dialog_apps_title: String = "Apps"
    override val dialog_domens_title: String = "Domains"
    override val edit_sub_name_hint: String = "Subscription name"
    override val edit_sub_title: String = "Edit subscription"
    override val edit_sub_url_hint: String = "Subscription URL"
    override val empty_profiles_hint: String = "No profiles added yet!"
    override val error_apps_list_empty: String = "List is empty. Check app list permission in settings."
    override val error_camera_permission_denied: String = "Camera permission denied"
    override val error_clipboard_empty: String = "Clipboard is empty"
    override val error_empty_name: String = "Name cannot be empty"
    override val error_import_failed: String = "Failed to add subscription or profile!"
    override val error_import_file_read: String = "Failed to read file"
    override val error_import_file_type: String = "Only .txt and .json files are supported"
    override val error_import_timeout: String = "Failed to add subscription or profile! (timeout 10 sec)"
    override val error_invalid_format: String = "Invalid format. Supported: vless://, vmess://, ss://, trojan://, hysteria://, hy://, hysteria2://, hy2://, https:// and JSON"
    override val error_json: String = "JSON error: %s"
    override val error_link_generation: String = "Failed to generate link"
    override val error_open_settings: String = "Failed to open system settings"
    override val error_parsing: String = "Parsing error: %s"
    override val error_profile_qr_generation: String = "Failed to generate profile QR code"
    override val error_qr_not_found_in_image: String = "No QR code found in image"
    override val error_qr_scan_empty: String = "Failed to decode QR code"
    override val error_subscription: String = "Subscription error: %s"
    override val error_subscription_empty: String = "Subscription is empty"
    override val error_subscription_https_required: String = "Subscription URL must use HTTPS"
    override val fakeip_desc: String = "Instantly gives the system a \\\"fake\\\" address for a domain without waiting for DNS response. Prevents DNS leaks"
    override val feature_coming_soon: String = "Feature coming soon"
    override val fragment_desc: String = "Splitting large data packets into smaller parts. Helps bypass blocks (DPI)"
    override val gvisorstack_desc: String = "High compatibility, supports all settings, medium power usage."
    override val hint_add_first_profile: String = "Use the button below to add or long-press for more ways!"
    override val icmp_desc: String = "Checks if server is available, used for basic connection check."
    override val journal_clear: String = "Clear"
    override val journal_title: String = "Event Journal"
    override val journal_waiting_logs: String = "Waiting for new events…"
    override val journal_copy_success: String = "Events successfully copied to clipboard!"
    override val json_edit_success: String = "JSON %s was successfully modified."
    override val label_add_profiles: String = "Add profiles"
    override val label_and: String = " and "
    override val label_config_editor: String = "Config Editor"
    override val label_credentials: String = "Credentials"
    override val label_error: String = "Error"
    override val label_errors: String = "Errors: "
    override val label_expires: String = "Expires: %s"
    override val label_update_interval: String = "Update: %s"
    override val settings_label_use_sub_interval: String = "Use subscription intervals"
    override val settings_desc_use_sub_interval: String = "Updates subscriptions according to their custom intervals, e.g. Update: 1 h."
    override val label_imported_profile: String = "Imported Profile"
    override val label_json_data: String = "JSON data"
    override val label_logs: String = "Logs:"
    override val label_mode: String = "Mode"
    override val label_output: String = "Output: "
    override val label_password: String = "Password"
    override val label_profile_name: String = "Profile name"
    override val label_seconds_short: String = "s"
    override val label_selected: String = "Selected"
    override val label_servers: String = "Servers"
    override val label_speed_test: String = "Speed Test"
    override val label_support: String = "Support"
    override val label_unknown: String = "Unknown"
    override val label_update: String = "Update"
    override val label_uuid: String = "UUID"
    override val language_auto: String = "Auto (system)"
    override val language_en: String = "English"
    override val language_restart_hint: String = "Almost ready! Need to restart the app."
    override val language_ru: String = "Russian"
    override val log_decoding_fragmentation: String = "Fragmentation enabled"
    override val log_decoding_mtu_stack: String = "MTU %1\$s, STACK %2\$s"
    override val log_decoding_tunnel_creation: String = "Creating tunnel..."
    override val manual_input_hint: String = "vless://, vmess://, ss://, hysteria2:// (hy2://), hysteria:// (hy://) or subscription link"
    override val manual_input_title: String = "Manual input"
    override val menu_delete_subscription: String = "Delete"
    override val menu_edit_subscription: String = "Edit"
    override val menu_file: String = "File"
    override val menu_link: String = "Link"
    override val menu_manual_input: String = "Manual input"
    override val menu_qr_code: String = "QR-Code"
    override val menu_update_subscription: String = "Update"
    override val menu_pin_subscription: String = "Pin"
    override val menu_unpin_subscription: String = "Unpin"
    override val subscription_qr_dialog_title: String = "Subscription QR code"
    override val mixedstack_desc: String = "Average compatibility, supports most settings, high power usage."
    override val mtu_desc: String = "Maximum size of one data packet (in bytes) that can be sent at once."
    override val mux_desc: String = "Combines multiple requests into one connection. Reduces latency and speeds up loading."
    override val notif_adaptive_tunnel_changed_body: String = "Profile was changed to %1\$s after connection drop."
    override val notif_adaptive_tunnel_changed_title: String = "Profile changed"
    override val notif_best_profile_body: String = "Profile updated to %1\$s with ping %2\$dms"
    override val notif_best_profile_title: String = "Profile Update"
    override val notif_language_changed: String = "App language changed to %s"
    override val notif_language_changed_auto: String = "App language changed successfully!"
    override val notif_theme_changed: String = "App theme changed successfully!"
    override val notif_theme_changed_auto: String = "App theme changed automatically!"
    override val notif_notifications_enabled: String = "Notifications successfully enabled!"
    override val notif_profile_changed: String = "Profile data changed successfully!"
    override val onboarding_toast_notification_granted: String = "Notification permission granted"
    override val onboarding_toast_notification_denied: String = "Notifications disabled"
    override val onboarding_toast_battery_unrestricted: String = "Battery optimization configured"
    override val error_downloading_rule: String = "Failed to download %1\$s: %2\$s"
    override val split_presets_applied: String = "Split tunneling presets applied!"
    override val wizard_step_ssh: String = "SSH"
    override val wizard_step_protocol: String = "Protocol"
    override val wizard_step_settings: String = "Settings"
    override val wizard_step_setup: String = "Setup"
    override val notif_update_title: String = "Flare Update"
    override val onboarding_battery_desc: String = "To keep the app stable and prevent Android from closing it, disable battery optimization for Flare"
    override val onboarding_battery_title: String = "Power consumption"
    override val onboarding_btn_go_main: String = "Go to Main"
    override val onboarding_fragmentation_desc: String = "Fragmentation helps split packets into parts to bypass blocks (DPI)"
    override val onboarding_fragmentation_question: String = "Would you like to enable fragmentation?"
    override val onboarding_fragmentation_title: String = "Fragmentation"
    override val onboarding_mux_desc: String = "Mux helps speed up connection, but is less effective against blocking"
    override val onboarding_mux_question: String = "Would you like to use Mux?"
    override val onboarding_mux_title: String = "Multiplexing"
    override val onboarding_notifications_desc: String = "Notifications are required for background work 24/7 and to see tunnel status"
    override val onboarding_notifications_error: String = "Notification permission denied"
    override val onboarding_notifications_title: String = "Notifications"
    override val onboarding_permissions_title: String = "Permissions"
    override val onboarding_success_title: String = "Setup completed successfully!"
    override val onboarding_usage_desc: String = "This permission is needed for application monitoring.\\nSo that the \\\"Trigger\\\" function can work correctly"
    override val onboarding_usage_title: String = "Usage Statistics"
    override val onboarding_welcome_question: String = "Do you want to run initial setup?"
    override val onboarding_welcome_title: String = "Welcome to Flare!"
    override val onboarding_permissions_subtitle: String = "Required permissions for stable background operation"
    override val onboarding_split_subtitle: String = "Select tunneling mode (can be skipped)"
    override val onboarding_split_white_title: String = "Whitelist"
    override val onboarding_split_white_desc: String = "Only selected apps and websites will go through the proxy"
    override val onboarding_split_black_title: String = "Blacklist"
    override val onboarding_split_black_desc: String = "Everything goes through the proxy except selected apps and websites"
    override val onboarding_split_white_header: String = "Whitelist: What will work through proxy?"
    override val onboarding_split_black_header: String = "Blacklist: What will NOT work through proxy?"
    override val onboarding_preset_ru_title: String = "Russian Services"
    override val onboarding_preset_ru_desc: String = "Gosuslugi, Yandex, banking apps, and more"
    override val onboarding_preset_social_title: String = "Social Networks"
    override val onboarding_preset_social_desc: String = "Telegram, WhatsApp, etc."
    override val onboarding_preset_ai_title: String = "AI Tools"
    override val onboarding_preset_ai_desc: String = "Gemini, ChatGPT, Claude, etc."
    override val onboarding_success_desc: String = "Now Flare is fully configured. You can add profiles and start using it."
    override val option_disable: String = "Disable"
    override val option_enable: String = "Enable"
    override val option_no: String = "No"
    override val option_yes: String = "Yes"
    override val option_auto: String = "Auto"
    override val option_custom: String = "Custom URL"
    override val dns_preset_cloudflare: String = "Cloudflare DoH"
    override val dns_preset_adguard: String = "AdGuard DNS (Ad Block)"
    override val dns_preset_google: String = "Google DoT"
    override val permission_usage_stats_needed: String = "To use the «Trigger» feature, permission to access usage statistics is required."
    override val profile_deleted_success: String = "Profile %s deleted!"
    override val profile_qr_dialog_title: String = "Profile QR code"
    override val profile_qr_image_description: String = "Profile link QR code"
    override val qr_camera_hint: String = "Point the camera at a QR code"
    override val routing_action_download: String = "Download"
    override val routing_badge_builtin: String = "Included"
    override val routing_badge_soon: String = "Soon"
    override val routing_card_ads: String = "Anti-Ads"
    override val routing_card_ads_desc: String = "Blocking ad domains (geosite-ads)"
    override val routing_card_cn: String = "China (CN)"
    override val routing_card_cn_desc: String = "Chinese sites and IP addresses"
    override val routing_card_global: String = "Global Rules"
    override val routing_card_global_desc: String = "Bypass China, Google, YouTube, etc."
    override val routing_card_media: String = "Media & Streaming"
    override val routing_card_media_desc: String = "YouTube, Netflix, Twitch, Disney+"
    override val routing_card_ru: String = "RU"
    override val routing_card_social: String = "Social Networks"
    override val routing_card_social_desc: String = "Telegram, Instagram, Facebook, TikTok"
    override val routing_desc_no_update: String = "No update needed"
    override val routing_last_update: String = "%s"
    override val routing_mode_block: String = "Block"
    override val routing_mode_direct: String = "Direct"
    override val routing_mode_proxy: String = "Proxy"
    override val routing_status_downloaded: String = "Rule downloaded"
    override val routing_status_updated: String = "Rule updated"
    override val routing_success_generic: String = "Rule %s updated successfully!"
    override val routing_update_error: String = "Failed to update bases!"
    override val routing_update_never: String = "Never"
    override val routing_update_success: String = "RU bases updated successfully!"
    override val rules_method_direct: String = "Direct"
    override val rules_method_proxy: String = "via Proxy"
    override val search_apps_hint: String = "Search apps..."
    override val servers_desc_create: String = "Your own server that only you control."
    override val servers_desc_flare: String = "Flare Servers: anonymous, no logs, fast and available 24/7."

    override val servers_protocol_shadowsocks_desc: String = "Energy-efficient and fast SOCKS5-based encryption protocol with AEAD ciphers. Provides high performance and data protection with minimal overhead."
    override val servers_protocol_shadowsocks_title: String = "ShadowSocks"
    override val servers_protocol_title: String = "Select protocol"
    override val servers_protocol_wireguard_desc: String = "Lightweight and modern UDP-based network-layer protocol with advanced cryptography. Provides instant connection and maximum throughput."
    override val servers_protocol_wireguard_title: String = "WireGuard"
    override val servers_protocol_xray_desc: String = "Xray with REALITY masks VPN traffic as web traffic. High detection resistance, provides high privacy and speed"
    override val servers_protocol_xray_title: String = "Xray with REALITY"
    override val servers_setup_success: String = "Server created successfully!"
    override val servers_setup_success_desc: String = "You can find it inside \\\"My servers\\\" subscription"
    override val servers_setup_title: String = "Server setup..."
    override val servers_ssh_ip: String = "IP address"
    override val servers_ssh_password: String = "SSH Password or Key"
    override val servers_ssh_port: String = "SSH Port"
    override val servers_ssh_port_hint: String = "22"
    override val servers_ssh_profile_name: String = "Profile name"
    override val servers_ssh_profile_name_hint: String = "My server"
    override val servers_ssh_username: String = "SSH Username"
    override val servers_title_create: String = "Create your server"
    override val servers_title_flare: String = "Flare Servers"
    override val servers_ssh_title: String = "SSH Connection Details"
    override val servers_xray_title: String = "Xray Configuration"
    override val servers_hysteria2_title: String = "Hysteria 2 Configuration"
    override val servers_shadowsocks_title: String = "Shadowsocks Configuration"
    override val servers_setup_progress_title: String = "Installation & Setup"
    override val servers_setup_success_title: String = "Installation Completed"
    override val servers_subscription_added_title: String = "Subscription Added"
    override val servers_subscription_failed_title: String = "Setup Failed"
    override val servers_tariff_title: String = "Select Plan"
    override val tariff_free_title: String = "Free"
    override val tariff_plus_title: String = "Plus"
    override val tariff_premium_title: String = "Premium"
    override val tariff_free_desc: String = "Average speed, provides basic access to many services"
    override val tariff_free_price: String = "0$/mon"
    override val tariff_plus_desc: String = "High speed, security, access to all services"
    override val tariff_plus_price: String = "3$/mon"
    override val tariff_premium_desc: String = "Maximum stability and speed, more servers"
    override val tariff_premium_price: String = "6$/mon"
    override val tariff_success_title: String = "Free subscription added"
    override val tariff_success_desc: String = "you will find it in the list"
    override val tariff_error_title: String = "Failed to add Free subscription"
    override val tariff_error_desc: String = "please try again later"
    override val servers_xray_port_desc: String = "Port for your VPN server. 443 — standard port for HTTPS masking."
    override val servers_xray_port_label: String = "Xray Port"
    override val servers_xray_setup_title: String = "Xray Setup"
    override val servers_xray_sni_desc: String = "Domain to mask your traffic under. Google.com — reliable default option."
    override val servers_xray_sni_label: String = "SNI (Server Name Indication)"
    override val settings_advanced_title: String = "Advanced Settings"
    override val settings_basic_title: String = "Basic Settings"
    override val settings_bg_effects_header: String = "Background Effects"
    override val settings_bg_effect_label: String = "Effect"
    override val settings_bg_effect_none: String = "None"
    override val settings_bg_effect_gradient: String = "Gradient"
    override val settings_bg_effect_shapes: String = "Shapes"
    override val settings_bg_effect_photo: String = "Photo"
    override val settings_bg_effect_update_photo: String = "Update photo"
    override val settings_btn_advanced: String = "Advanced"
    override val settings_btn_base: String = "Basic"
    override val settings_btn_change: String = "Change"
    override val settings_btn_change_font: String = "Change font"
    override val settings_btn_journal: String = "Journal"
    override val settings_color_material_you: String = "Material You"
    override val settings_desc_adaptive_tunnel: String = "Automatically recovers connection on drop or selects working server"
    override val settings_desc_best_profile: String = "This function selects the server with lowest ping in the subscription"
    override val settings_desc_hwid: String = "HWID is an identifier to link your subscription to your device. Thanks to it, re-importing is not counted as a new connection. All data is stored locally and is not transmitted anywhere else."
    override val settings_desc_logging: String = "Enabling logs is useful for debugging but may expose your server addresses and keys."
    override val settings_desc_test_url: String = "This parameter determines which link will be used for delay testing"
    override val settings_desc_update_check: String = "Checking updates helps you stay on the latest Flare version."
    override val settings_font_geologica: String = "Geologica"
    override val settings_header_app: String = "App Settings"
    override val settings_header_appearance: String = "Appearance"
    override val settings_header_autostart: String = "Autostart"
    override val settings_header_best_profile: String = "Profile management"
    override val settings_header_hwid: String = "HWID Management"
    override val settings_header_logging: String = "Logging"
    override val settings_header_notifications: String = "Notifications"
    override val settings_header_rules: String = "Rules"
    override val settings_header_updates: String = "Updates"
    override val settings_header_vpn: String = "VPN Settings"
    override val settings_hint_best_profile_interval: String = "1800"
    override val settings_hint_dns_url: String = "Auto"
    override val settings_hint_test_url: String = "https://www.google.com/generate_204"
    override val settings_hint_update_interval: String = "3600"
    override val settings_item_language: String = "Language"
    override val settings_item_ping: String = "Ping"
    override val settings_item_routing: String = "Routing"
    override val settings_item_subscriptions: String = "Subscriptions"
    override val settings_item_theme: String = "Theme & Font"
    override val settings_label_adaptive_tunnel: String = "Adaptive Tunnel"
    override val settings_label_auto_update: String = "Auto-update"
    override val settings_desc_auto_update: String = "Forces updates of all subscriptions at the specified interval, for example, once every 3600 seconds"
    override val settings_label_autostart: String = "Create tunnel on app launch"
    override val settings_label_best_profile: String = "Select best profile"
    override val settings_label_best_profile_interval: String = "Update selection every"
    override val settings_label_best_profile_notif: String = "Best profile selection"
    override val settings_label_best_profile_only_connected: String = "Only if tunnel is running"
    override val settings_label_core_log: String = "SingBox Log"
    override val settings_label_core_log_level: String = "Log Level"
    override val settings_label_custom_color: String = "Custom color"
    override val settings_label_dns_url: String = "URL"
    override val settings_label_enable_gradient: String = "Gradient"
    override val settings_label_fake_ip: String = "Fake IP"
    override val settings_label_font: String = "Font"
    override val settings_label_fragment_interval: String = "Timeout"
    override val settings_label_fragment_size: String = "Size"
    override val settings_label_fragment_sleep: String = "Sleep"
    override val settings_label_fragmentation: String = "Fragmentation"
    override val settings_label_gradient_animation: String = "Gradient animation"
    override val settings_label_gradient_speed: String = "Animation speed"
    override val settings_label_language: String = "App Language"
    override val settings_label_mtu: String = "MTU"
    override val settings_label_mtu_title: String = "Change MTU"
    override val mtu_auto_btn: String = "Auto"
    override val mtu_auto_warning: String = "Optimal MTU has been set: %s"
    override val settings_label_mux: String = "Mux"
    override val settings_label_mux_padding: String = "Add noise"
    override val settings_label_mux_protocol: String = "Method"
    override val settings_label_mux_streams: String = "Streams count"
    override val settings_label_noise_apply: String = "Apply to IP"
    override val settings_label_noise_delay: String = "Delay"
    override val settings_label_noise_packet: String = "Packet"
    override val settings_label_noise_type: String = "Type"
    override val settings_label_packet_type: String = "Fallback"
    override val settings_label_ping_display: String = "Ping Display"
    override val settings_label_ping_style: String = "Style"
    override val settings_label_ping_type: String = "Ping type"
    override val settings_label_remote_dns: String = "Remote DNS"
    override val settings_label_rules_method: String = "Method"
    override val settings_header_chain: String = "Chain Management"
    override val settings_label_reset_chain: String = "Reset chain after disconnection"
    override val settings_desc_reset_chain: String = "Automatically clear the proxy chain after stopping the VPN."
    override val settings_label_tls_spoof: String = "TLS Spoof"
    override val settings_desc_tls_spoof: String = "SNI spoofing to bypass blocks. Sends a forged ClientHello with a whitelisted domain before the real one."
    override val settings_label_tls_spoof_domain: String = "Domain"
    override val settings_label_tls_spoof_method: String = "Method"
    override val settings_label_fingerprint: String = "Fingerprint"
    override val settings_item_tls_fingerprint: String = "TLS Fingerprint"
    override val settings_desc_fingerprint: String = "Masks your TLS traffic as a selected browser/client. If Auto is selected, the fingerprint will be taken from the configuration."
    override val settings_label_send_hwid: String = "Send HWID"
    override val settings_label_split_tunneling: String = "Split Tunneling"
    override val settings_label_stack: String = "%s"
    override val settings_label_stack_title: String = "Use"
    override val settings_label_status: String = "Status"
    override val settings_label_status_notification: String = "Status notification"
    override val settings_label_notification_speed: String = "Show speed"
    override val settings_label_test_url: String = "Test URL"
    override val settings_label_theme: String = "Style"
    override val settings_label_update_check: String = "Check updates"
    override val settings_label_update_every: String = "Update subscriptions every"
    override val settings_label_update_frequency: String = "Check frequency"
    override val settings_label_use: String = "Use"
    override val settings_label_use_fake_ip: String = "Enable Fake IP"
    override val settings_label_user_agent: String = "User-Agent"
    override val settings_language_in_dev: String = "Language switching (in development)"
    override val settings_language_title: String = "Language"
    override val settings_ping_interval_min_warning: String = "Minimum interval is 10 seconds"
    override val settings_ping_style_both: String = "Time & Icon"
    override val settings_ping_style_icon: String = "Icon"
    override val settings_ping_style_time: String = "Time"
    override val settings_ping_title: String = "Ping Settings"
    override val settings_ping_type_get: String = "via proxy"
    override val settings_ping_type_icmp: String = "ICMP"
    override val settings_ping_type_tcp: String = "TCP"
    override val settings_restart_tunnel_hint: String = "Settings will be applied on next tunnel creation."
    override val settings_routing_title: String = "Routing"
    override val settings_stack_header: String = "Network Stack"
    override val settings_subscriptions_title: String = "Subscriptions"
    override val settings_theme_header: String = "Theme"
    override val settings_theme_title: String = "Theme & Font"
    override val settings_title: String = "Settings"
    override val simple_editor_alpn: String = "ALPN"
    override val simple_editor_basic: String = "Basic Settings"
    override val simple_editor_enable_tls: String = "Enable TLS"
    override val simple_editor_fingerprint: String = "Fingerprint"
    override val simple_editor_flow: String = "Flow"
    override val simple_editor_packet_encoding: String = "Packet Encoding"
    override val simple_editor_method: String = "Encryption method"
    override val simple_editor_obfs: String = "Obfs"
    override val simple_editor_obfs_pass: String = "Obfs Password"
    override val simple_editor_pbk: String = "Public Key"
    override val simple_editor_port: String = "Port"
    override val simple_editor_reality: String = "Reality Settings"
    override val simple_editor_server: String = "Server (Host or IP)"
    override val simple_editor_sid: String = "Short ID"
    override val simple_editor_sni: String = "SNI"
    override val simple_editor_tag: String = "Profile name"
    override val simple_editor_title: String = "Profile Editor"
    override val simple_editor_tls: String = "TLS Settings"
    override val simple_editor_uuid_pwd: String = "UUID / Password"
    override val simple_editor_up_mbps: String = "Upload Speed (Up Mbps)"
    override val simple_editor_down_mbps: String = "Download Speed (Down Mbps)"
    override val simple_editor_allow_insecure: String = "Allow Insecure TLS"
    override val simple_editor_hysteria_settings: String = "Hysteria Settings"
    override val simple_editor_hop_interval: String = "Hop Interval"
    override val sites_hint: String = "site1.com\nsite2.com"
    override val split_mode_blacklist: String = "Blacklist"
    override val split_mode_blacklist_tooltip: String = "In this mode, all websites and apps work through VPN except the selected ones"
    override val split_mode_whitelist: String = "Whitelist"
    override val split_mode_whitelist_tooltip: String = "In this mode, only selected websites and apps work through VPN, others go directly"
    override val split_tunneling_desc_default: String = "Select sites and apps that work through VPN or directly."
    override val ssh_error_config_write: String = "Config file was not written to server"
    override val ssh_error_generic: String = "Error: %s"
    override val ssh_error_keys: String = "Failed to get REALITY keys."
    override val ssh_error_port_not_listening: String = "Xray is running but not listening on port %d!"
    override val ssh_error_service_start: String = "Xray service failed to start (status: %s)"
    override val ssh_status_configuring: String = "Configuring setup..."
    override val ssh_status_connecting: String = "Connecting to server..."
    override val ssh_status_generating_client: String = "Generating client settings..."
    override val ssh_status_generating_keys: String = "Generating REALITY keys..."
    override val ssh_status_installing_xray: String = "Installing Xray..."
    override val ssh_status_restarting: String = "Restarting Xray service..."
    override val ssh_status_waiting: String = "Waiting for startup..."
    override val startup_loading_profiles: String = "Loading profiles and settings..."
    override val sub_deleted_success: String = "Subscription %s deleted!"
    override val sub_my_servers: String = "My servers"
    override val sub_single_profiles: String = "List of profiles"
    override val sub_update_error: String = "Failed to update all subscriptions!"
    override val sub_update_error_single: String = "Failed to update subscription!"
    override val sub_update_success: String = "%d Subscriptions updated successfully."
    override val sub_update_success_single: String = "Subscription %s updated successfully."
    override val success_link_copied: String = "Link copied"
    override val success_profile_added: String = "Profile %s added successfully!"
    override val success_profiles_added: String = "Profiles added successfully: %d"
    override val success_subscription_added: String = "Subscription %s added successfully!"
    override val systemstack_desc: String = "Low compatibility, doesn\\'t support fragmentation and some other settings, low power usage."
    override val tab_apps: String = "Apps"
    override val tab_sites: String = "Sites"
    override val tcp_desc: String = "Checks port opening speed and readiness to accept connections."
    override val theme_auto: String = "Auto"
    override val theme_day: String = "Day"
    override val theme_night: String = "Night"
    override val trigger_hint: String = "This parameter makes the VPN connect only when using certain apps"
    override val trigger_label: String = "Trigger"
    override val trigger_vpn_permission_channel: String = "Trigger VPN permission"
    override val trigger_vpn_permission_text: String = "Open the app and confirm the VPN permission so Trigger can start the tunnel."
    override val trigger_vpn_permission_title: String = "Trigger needs VPN permission"
    override val update_available_title: String = "New version Flare %s available!"
    override val update_freq_daily: String = "Daily"
    override val update_freq_monthly: String = "Monthly"
    override val update_freq_weekly: String = "Weekly"
    override val server_manual_desc: String = "Enter connection details"
    override val viaproxy_desc: String = "Checks HTTP request time through proxy, tests real latency, most accurate method."
    override val vpn_active: String = "VPN active"
    override val vpn_disconnect: String = "Disconnect"
    override val vpn_stopping: String = "Stopping services"
    override val vpn_starting: String = "Starting services"
    override val vpn_error_permission_denied: String = "VPN permission denied"
    override val vpn_error_permission_required: String = "VPN permission required. Please open the app and confirm."
    override val vpn_error_tunnel_creation: String = "Failed to create tunnel"
    override val wizard_setup_configuring: String = "Configuring server..."
    override val wizard_setup_free_title: String = "Setting up your subscription..."
    override val wizard_setup_optimizing: String = "Optimizing..."
    override val wizard_setup_ready: String = "Server ready!"
    override val wizard_setup_saving: String = "Saving..."
    override val wizard_setup_validating: String = "Validating data..."
    override val wizard_xray_port_hint: String = "443 (default)"
    override val wizard_xray_sni_hint: String = "google.com (default)"
    override val simple_editor_cert_pin: String = "SHA-256 certificate fingerprint"
    override val servers_protocol_hysteria2_title: String = "Hysteria 2"
    override val servers_protocol_hysteria2_desc: String = "High-speed UDP-based (QUIC) protocol with HTTPS masquerading and built-in censorship resistance."
    override val ssh_status_installing_hysteria2: String = "Installing Hysteria 2..."
    override val ssh_status_generating_cert: String = "Generating TLS certificate..."
    override val ssh_status_configuring_hysteria2: String = "Configuring Hysteria 2..."
    override val ssh_status_restarting_hysteria2: String = "Restarting Hysteria 2 service..."
    override val ssh_error_cert: String = "Failed to generate TLS certificate."
    override val ssh_error_port_not_listening_udp: String = "Hysteria 2 is running but not listening on port %d (UDP)!"
    override val ssh_error_service_start_hysteria2: String = "Hysteria 2 service failed to start (status: %s)"
    override val servers_hysteria2_port_label: String = "Hysteria 2 Port"
    override val wizard_hysteria2_port_hint: String = "443 (default)"
    override val servers_hysteria2_sni_label: String = "Masquerade Domain (SNI)"
    override val wizard_hysteria2_sni_hint: String = "google.com (default)"
    override val servers_hysteria2_obfs_pass_label: String = "Obfs Password (Optional)"
    override val wizard_hysteria2_obfs_pass_hint: String = "salamander_pass (default)"
    override val servers_hysteria2_port_hopping_label: String = "Port Hopping"
    override val servers_hysteria2_port_hopping_auto: String = "Auto"
    override val servers_hysteria2_port_hopping_manual: String = "Manual"
    override val wizard_hysteria2_port_hopping_hint: String = "Range, e.g. 20000-50000"
    override val ssh_status_installing_shadowsocks: String = "Installing Shadowsocks..."
    override val ssh_status_configuring_shadowsocks: String = "Configuring Shadowsocks..."
    override val ssh_status_restarting_shadowsocks: String = "Restarting Shadowsocks..."
    override val ssh_error_service_start_shadowsocks: String = "Shadowsocks service failed to start (status: %s)"
    override val servers_shadowsocks_port_label: String = "Shadowsocks Port"
    override val wizard_shadowsocks_port_hint: String = "8388 (default)"
    override val servers_shadowsocks_sni_label: String = "Masquerade Domain (SNI)"
    override val wizard_shadowsocks_sni_hint: String = "google.com (default)"
    override val servers_wireguard_title: String = "WireGuard Settings"
    override val servers_wireguard_port_label: String = "WireGuard Port"
    override val wizard_wireguard_port_hint: String = "51820 (default)"
    override val ssh_status_installing_wireguard: String = "Installing WireGuard..."
    override val ssh_status_configuring_wireguard: String = "Configuring WireGuard..."
    override val ssh_status_restarting_wireguard: String = "Starting WireGuard service..."
    override val ssh_error_service_start_wireguard: String = "WireGuard service failed to start (status: %s)"
    override val label_shadowsocks_dpi_bypass: String = "Bypass DPI (Plugins)"
    override val label_shadowsocks_dpi_bypass_hint: String = "Mask Shadowsocks as HTTPS traffic (WebSocket + TLS)"
    override val simple_editor_shadowtls_password: String = "ShadowTLS Password"
    override val simple_editor_shadowtls_version: String = "ShadowTLS Version"
    override val simple_editor_ss_network: String = "Network (Transport)"
    override val simple_editor_ss_ws_path: String = "WebSocket Path"
    override val simple_editor_ss_ws_host: String = "WebSocket Host (Header)"
    override val simple_editor_tls_type: String = "TLS Type"
    override val simple_editor_http_host: String = "HTTP Host"
    override val simple_editor_path: String = "Path"
    override val simple_editor_host: String = "Host"
    override val simple_editor_kcp_seed: String = "KCP Seed"
    override val simple_editor_mtu: String = "MTU"
    override val simple_editor_tti: String = "TTI"
    override val simple_editor_httpupgrade_host: String = "HTTPUpgrade Host"
    override val simple_editor_httpupgrade_path: String = "HTTPUpgrade Path"
    override val simple_editor_h2_host: String = "H2 Host"
    override val simple_editor_h2_path: String = "H2 Path"
    override val simple_editor_quic_security: String = "QUIC Security"
    override val simple_editor_quic_key: String = "QUIC Key"
    override val simple_editor_grpc_authority: String = "gRPC Authority"
    override val simple_editor_grpc_service_name: String = "gRPC serviceName"
    override val simple_editor_mode: String = "Mode"
    override val settings_header_data_mgmt: String = "Data Management"
    override val settings_label_data_mgmt: String = "Backup & Restore"
    override val settings_desc_data_mgmt: String = "You can save all settings, profiles, and subscriptions of the app"
    override val settings_btn_data_mgmt: String = "Transfer"
    override val btn_done: String = "Done"
    override val data_mgmt_title: String = "Data Management"
    override val data_mgmt_export: String = "Export"
    override val data_mgmt_export_desc: String = "Create a file with a copy of your data"
    override val data_mgmt_import: String = "Import"
    override val data_mgmt_import_desc: String = "Load data from an existing file"
    override val data_mgmt_creating: String = "Creating backup..."
    override val data_mgmt_created: String = "Backup created successfully!"
    override val data_mgmt_select_title: String = "Which backup to restore?"
    override val data_mgmt_restoring: String = "Restoring backup..."
    override val data_mgmt_restored: String = "Backup restored successfully"
    override val data_mgmt_no_backups: String = "No backups found for restoration"
    override fun plural_apps(count: Int, vararg args: Any): String {
        val res = when (count) {
            1 -> "app"
            else -> "apps"
        }
        return res.format(*args)
    }
    override val error_profile_selection_required: String = "Please select a profile first!"
    override fun plural_sites(count: Int, vararg args: Any): String {
        val res = when (count) {
            1 -> "site"
            else -> "sites"
        }
        return res.format(*args)
    }
    override val status_disconnected: String = "Disconnected"
    override val status_connected: String = "Connected"
    override val status_connecting: String = "Connecting..."
    override val status_disconnecting: String = "Disconnecting..."
}

object I18n {
    var strings: FlareStrings by mutableStateOf(RuFlareStrings)

    fun updateLocale(locale: String) {
        val lang = if (locale.lowercase() == "auto") {
            java.util.Locale.getDefault().language
        } else {
            locale.lowercase()
        }
        
        strings = when (lang) {
            "ru" -> RuFlareStrings
            "en" -> EnFlareStrings
            else -> EnFlareStrings
        }
    }

    fun isMyServers(name: String?): Boolean {
        if (name == null) return false
        return name == RuFlareStrings.sub_my_servers || name == EnFlareStrings.sub_my_servers
    }
}