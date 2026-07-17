package flare.client.app.ui.i18n

interface CommonStrings {
    val collapse_all: String
    val app_monitor_active: String
    val app_name: String
    val btn_add: String
    val btn_apply: String
    val btn_cancel: String
    val label_warning: String
    val http_warning_message: String
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
    val feature_coming_soon: String
    val hint_add_first_profile: String
    val json_edit_success: String
    val label_add_profiles: String
    val label_and: String
    val label_config_editor: String
    val label_credentials: String
    val label_error: String
    val label_errors: String
    val label_expires: String
    val label_update_interval: String
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
    val manual_input_hint: String
    val manual_input_title: String
    val menu_delete_subscription: String
    val menu_edit_subscription: String
    val menu_file: String
    val menu_link: String
    val menu_manual_input: String
    val menu_qr_code: String
    val menu_update_subscription: String
    val menu_test: String
    val menu_pin_subscription: String
    val menu_unpin_subscription: String
    val menu_merge_subscription: String
    val sub_merged_profiles: String
    val subscription_qr_dialog_title: String
    val split_presets_applied: String
    val option_disable: String
    val option_enable: String
    val option_no: String
    val option_yes: String
    val option_auto: String
    val option_custom: String
    val permission_usage_stats_needed: String
    val profile_deleted_success: String
    val profile_qr_dialog_title: String
    val profile_qr_image_description: String
    val qr_camera_hint: String
    val search_apps_hint: String
    val split_mode_blacklist: String
    val split_mode_blacklist_tooltip: String
    val split_mode_whitelist: String
    val split_mode_whitelist_tooltip: String
    val split_tunneling_desc_default: String
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
    val tab_apps: String
    val tab_sites: String
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
    val vpn_active: String
    val vpn_disconnect: String
    val vpn_stopping: String
    val vpn_starting: String
    val vpn_error_permission_denied: String
    val vpn_error_permission_required: String
    val vpn_error_tunnel_creation: String
    val label_shadowsocks_dpi_bypass: String
    val label_shadowsocks_dpi_bypass_hint: String
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
    val status_disconnected: String
    val status_connected: String
    val status_connecting: String
    val status_disconnecting: String
    fun plural_apps(count: Int, vararg args: Any): String
    fun plural_sites(count: Int, vararg args: Any): String
}

object RuCommonStrings : CommonStrings {
    override val collapse_all : String = "Свернуть все"
    override val app_monitor_active : String = "Триггер активен!"
    override val app_name : String = "Flare"
    override val btn_add : String = "Добавить"
    override val btn_apply : String = "Применить"
    override val btn_cancel : String = "Отмена"
    override val label_warning : String = "Предупреждение"
    override val http_warning_message : String = "Это HTTP-ссылка, она может быть небезопасной. Вы уверены, что хотите добавить её?"
    override val btn_clipboard : String = "Буфер обмена"
    override val btn_download : String = "Скачать"
    override val btn_connect : String = "Подключить"
    override val btn_disconnect : String = "Отключить"
    override val btn_finish : String = "Завершить"
    override val btn_grant : String = "Выдать"
    override val btn_next : String = "Далее"
    override val btn_save : String = "Сохранить"
    override val btn_select_from_gallery : String = "Выбрать из галереи"
    override val btn_share_link : String = "Поделиться ссылкой"
    override val desc_select_apps : String = "Выберите приложения, которые будут использовать VPN."
    override val dialog_apps_title : String = "Приложения"
    override val dialog_domens_title : String = "Домены"
    override val edit_sub_name_hint : String = "Название подписки"
    override val edit_sub_title : String = "Редактирование подписки"
    override val edit_sub_url_hint : String = "URL Подписки"
    override val empty_profiles_hint : String = "Пока что нет добавленных профилей!"
    override val feature_coming_soon : String = "Функция скоро появится"
    override val hint_add_first_profile : String = "Используйте кнопку ниже для добавления или зажмите для выбора способа!"
    override val json_edit_success : String = "JSON %s был успешно изменен."
    override val label_add_profiles : String = "Добавить профили"
    override val label_and : String = " и "
    override val label_config_editor : String = "Редактор"
    override val label_credentials : String = "Данные"
    override val label_error : String = "Ошибка"
    override val label_errors : String = "Ошибки: "
    override val label_expires : String = "Истекает: %s"
    override val label_update_interval : String = "Обновление: %s"
    override val label_imported_profile : String = "Imported Profile"
    override val label_json_data : String = "Данные JSON"
    override val label_logs : String = "Журнал:"
    override val label_mode : String = "Режим"
    override val label_output : String = "Вывод: "
    override val label_password : String = "Пароль"
    override val label_profile_name : String = "Название профиля"
    override val label_seconds_short : String = "с"
    override val label_selected : String = "Выбрано"
    override val label_servers : String = "Серверы"
    override val label_speed_test : String = "Тест скорости"
    override val label_support : String = "Поддержка"
    override val label_unknown : String = "Неизвестно"
    override val label_update : String = "Обновить"
    override val label_uuid : String = "UUID"
    override val language_auto : String = "Авто (система)"
    override val language_en : String = "English"
    override val language_restart_hint : String = "Почти готово! Нужно перезапустить приложение."
    override val language_ru : String = "Русский"
    override val manual_input_hint : String = "vless://, vmess://, ss://, hysteria2:// (hy2://), hysteria:// (hy://) или ссылка на подписку"
    override val manual_input_title : String = "Ручной ввод"
    override val menu_delete_subscription : String = "Удалить"
    override val menu_edit_subscription : String = "Редактировать"
    override val menu_file : String = "Файл"
    override val menu_link : String = "Ссылка"
    override val menu_manual_input : String = "Ручной ввод"
    override val menu_qr_code : String = "QR-Код"
    override val menu_update_subscription : String = "Обновить"
    override val menu_test : String = "Тестировать"
    override val menu_pin_subscription : String = "Закрепить"
    override val menu_unpin_subscription : String = "Открепить"
    override val menu_merge_subscription : String = "Объединить"
    override val subscription_qr_dialog_title : String = "QR-код подписки"
    override val split_presets_applied : String = "Настройки раздельного туннелирования применены!"
    override val option_disable : String = "Отключить"
    override val option_enable : String = "Включить"
    override val option_no : String = "Нет"
    override val option_yes : String = "Да"
    override val option_auto : String = "Авто"
    override val option_custom : String = "Свой URL"
    override val permission_usage_stats_needed : String = "Для работы функции «Триггер» необходимо разрешение на доступ к статистике использования."
    override val profile_deleted_success : String = "Профиль %s успешно удален!"
    override val profile_qr_dialog_title : String = "QR-код профиля"
    override val profile_qr_image_description : String = "QR-код ссылки профиля"
    override val qr_camera_hint : String = "Наведите камеру на QR-код"
    override val search_apps_hint : String = "Поиск приложений..."
    override val split_mode_blacklist : String = "Черный список"
    override val split_mode_blacklist_tooltip : String = "В этом режиме все сайты и приложения работают через VPN, кроме выбранных."
    override val split_mode_whitelist : String = "Белый список"
    override val split_mode_whitelist_tooltip : String = "В этом режиме только выбранные сайты и приложения работают через VPN, остальные — напрямую."
    override val split_tunneling_desc_default : String = "Выберите сайты и приложения, которые работают через VPN или напрямую."
    override val startup_loading_profiles : String = "Загружаем профили и настройки..."
    override val sub_deleted_success : String = "Подписка %s была удалена!"
    override val sub_my_servers : String = "Мои сервера"
    override val sub_single_profiles : String = "Список профилей"
    override val sub_merged_profiles : String = "Объединенные профили"
    override val sub_update_error : String = "Не удалось обновить все подписки!"
    override val sub_update_error_single : String = "Не удалось обновить подписку!"
    override val sub_update_success : String = "%d подписок успешно обновлено."
    override val sub_update_success_single : String = "Подписка %s была успешно обновлена."
    override val success_link_copied : String = "Ссылка скопирована"
    override val success_profile_added : String = "Профиль %s успешно добавлен!"
    override val success_profiles_added : String = "Успешно добавлено профилей: %d"
    override val success_subscription_added : String = "Подписка %s успешно добавлена!"
    override val tab_apps : String = "Приложения"
    override val tab_sites : String = "Сайты"
    override val theme_auto : String = "Авто"
    override val theme_day : String = "День"
    override val theme_night : String = "Ночь"
    override val trigger_hint : String = "VPN автоматически подключается только при использовании определённых приложений."
    override val trigger_label : String = "Триггер"
    override val trigger_vpn_permission_channel : String = "Разрешение VPN для триггера"
    override val trigger_vpn_permission_text : String = "Откройте приложение и подтвердите разрешение, чтобы триггер мог запустить туннель."
    override val trigger_vpn_permission_title : String = "Триггеру нужно разрешение VPN"
    override val update_available_title : String = "Доступна новая версия Flare %s!"
    override val update_freq_daily : String = "Ежедневно"
    override val update_freq_monthly : String = "Ежемесячно"
    override val update_freq_weekly : String = "Еженедельно"
    override val server_manual_desc : String = "Введите данные для подключения"
    override val vpn_active : String = "VPN активен"
    override val vpn_disconnect : String = "Отключить"
    override val vpn_stopping : String = "Остановка служб"
    override val vpn_starting : String = "Запуск служб"
    override val vpn_error_permission_denied : String = "Разрешение VPN отклонено"
    override val vpn_error_permission_required : String = "Требуется разрешение для VPN. Пожалуйста, откройте приложение и подтвердите его."
    override val vpn_error_tunnel_creation : String = "Не удалось создать туннель"
    override val label_shadowsocks_dpi_bypass : String = "Плагины обхода DPI"
    override val label_shadowsocks_dpi_bypass_hint : String = "Маскировать Shadowsocks под HTTPS-трафик (WebSocket + TLS)"
    override val btn_done : String = "Готово"
    override val data_mgmt_title : String = "Управление данными"
    override val data_mgmt_export : String = "Экспорт"
    override val data_mgmt_export_desc : String = "Создать файл с копией ваших данных"
    override val data_mgmt_import : String = "Импорт"
    override val data_mgmt_import_desc : String = "Загрузить данные из существующего файла"
    override val data_mgmt_creating : String = "Создание копии..."
    override val data_mgmt_created : String = "Копия успешно создана!"
    override val data_mgmt_select_title : String = "Какую копию восстановить?"
    override val data_mgmt_restoring : String = "Восстановление копии..."
    override val data_mgmt_restored : String = "Копия успешно восстановлена"
    override val data_mgmt_no_backups : String = "Копии для восстановления не найдены"
    override val status_disconnected : String = "Отключено"
    override val status_connected : String = "Подключено"
    override val status_connecting : String = "Подключение..."
    override val status_disconnecting : String = "Отключение..."
    override fun plural_apps (count: Int, vararg args: Any): String {
        val res = when {
            count % 10 == 1 && count % 100 != 11 -> "приложение"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "приложения"
            else -> "приложений"
        }
        return res.format(*args)
    }
    override fun plural_sites (count: Int, vararg args: Any): String {
        val res = when {
            count % 10 == 1 && count % 100 != 11 -> "сайт"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "сайта"
            else -> "сайтов"
        }
        return res.format(*args)
    }
}

object EnCommonStrings : CommonStrings {
    override val collapse_all : String = "Collapse all"
    override val app_monitor_active : String = "Trigger is active!"
    override val app_name : String = "Flare"
    override val btn_add : String = "Add"
    override val btn_apply : String = "Apply"
    override val btn_cancel : String = "Cancel"
    override val label_warning : String = "Warning"
    override val http_warning_message : String = "This is an HTTP link, it may be insecure. Are you sure you want to add it?"
    override val btn_clipboard : String = "Clipboard"
    override val btn_download : String = "Download"
    override val btn_connect : String = "Connect"
    override val btn_disconnect : String = "Disconnect"
    override val btn_finish : String = "Finish"
    override val btn_grant : String = "Grant"
    override val btn_next : String = "Next"
    override val btn_save : String = "Save"
    override val btn_select_from_gallery : String = "Choose from gallery"
    override val btn_share_link : String = "Share link"
    override val desc_select_apps : String = "Select apps that will use VPN."
    override val dialog_apps_title : String = "Apps"
    override val dialog_domens_title : String = "Domains"
    override val edit_sub_name_hint : String = "Subscription name"
    override val edit_sub_title : String = "Edit subscription"
    override val edit_sub_url_hint : String = "Subscription URL"
    override val empty_profiles_hint : String = "No profiles added yet!"
    override val feature_coming_soon : String = "Feature coming soon"
    override val hint_add_first_profile : String = "Use the button below to add or long-press for more ways!"
    override val json_edit_success : String = "JSON %s was successfully modified."
    override val label_add_profiles : String = "Add profiles"
    override val label_and : String = " and "
    override val label_config_editor : String = "Editor"
    override val label_credentials : String = "Credentials"
    override val label_error : String = "Error"
    override val label_errors : String = "Errors: "
    override val label_expires : String = "Expires: %s"
    override val label_update_interval : String = "Update: %s"
    override val label_imported_profile : String = "Imported Profile"
    override val label_json_data : String = "JSON data"
    override val label_logs : String = "Logs:"
    override val label_mode : String = "Mode"
    override val label_output : String = "Output: "
    override val label_password : String = "Password"
    override val label_profile_name : String = "Profile name"
    override val label_seconds_short : String = "s"
    override val label_selected : String = "Selected"
    override val label_servers : String = "Servers"
    override val label_speed_test : String = "Speed Test"
    override val label_support : String = "Support"
    override val label_unknown : String = "Unknown"
    override val label_update : String = "Update"
    override val label_uuid : String = "UUID"
    override val language_auto : String = "Auto (system)"
    override val language_en : String = "English"
    override val language_restart_hint : String = "Almost ready! Need to restart the app."
    override val language_ru : String = "Russian"
    override val manual_input_hint : String = "vless://, vmess://, ss://, hysteria2:// (hy2://), hysteria:// (hy://) or subscription link"
    override val manual_input_title : String = "Manual input"
    override val menu_delete_subscription : String = "Delete"
    override val menu_edit_subscription : String = "Edit"
    override val menu_file : String = "File"
    override val menu_link : String = "Link"
    override val menu_manual_input : String = "Manual input"
    override val menu_qr_code : String = "QR-Code"
    override val menu_update_subscription : String = "Update"
    override val menu_test : String = "Test"
    override val menu_pin_subscription : String = "Pin"
    override val menu_unpin_subscription : String = "Unpin"
    override val menu_merge_subscription : String = "Merge"
    override val subscription_qr_dialog_title : String = "Subscription QR code"
    override val split_presets_applied : String = "Split tunneling presets applied!"
    override val option_disable : String = "Disable"
    override val option_enable : String = "Enable"
    override val option_no : String = "No"
    override val option_yes : String = "Yes"
    override val option_auto : String = "Auto"
    override val option_custom : String = "Custom URL"
    override val permission_usage_stats_needed : String = "To use the 'Trigger' feature, permission to access usage statistics is required."
    override val profile_deleted_success : String = "Profile %s deleted!"
    override val profile_qr_dialog_title : String = "Profile QR code"
    override val profile_qr_image_description : String = "Profile link QR code"
    override val qr_camera_hint : String = "Point the camera at a QR code"
    override val search_apps_hint : String = "Search apps..."
    override val split_mode_blacklist : String = "Blacklist"
    override val split_mode_blacklist_tooltip : String = "In this mode, all websites and apps work through VPN except the selected ones"
    override val split_mode_whitelist : String = "Whitelist"
    override val split_mode_whitelist_tooltip : String = "In this mode, only selected websites and apps work through VPN, others go directly"
    override val split_tunneling_desc_default : String = "Select sites and apps that work through VPN or directly."
    override val startup_loading_profiles : String = "Loading profiles and settings..."
    override val sub_deleted_success : String = "Subscription %s deleted!"
    override val sub_my_servers : String = "My servers"
    override val sub_single_profiles : String = "List of profiles"
    override val sub_merged_profiles : String = "Merged Profiles"
    override val sub_update_error : String = "Failed to update all subscriptions!"
    override val sub_update_error_single : String = "Failed to update subscription!"
    override val sub_update_success : String = "%d subscriptions updated successfully."
    override val sub_update_success_single : String = "Subscription %s updated successfully."
    override val success_link_copied : String = "Link copied"
    override val success_profile_added : String = "Profile %s added successfully!"
    override val success_profiles_added : String = "Profiles added successfully: %d"
    override val success_subscription_added : String = "Subscription %s added successfully!"
    override val tab_apps : String = "Apps"
    override val tab_sites : String = "Sites"
    override val theme_auto : String = "Auto"
    override val theme_day : String = "Day"
    override val theme_night : String = "Night"
    override val trigger_hint : String = "This parameter makes the VPN connect only when using certain apps"
    override val trigger_label : String = "Trigger"
    override val trigger_vpn_permission_channel : String = "Trigger VPN permission"
    override val trigger_vpn_permission_text : String = "Open the app and confirm the VPN permission so Trigger can start the tunnel."
    override val trigger_vpn_permission_title : String = "Trigger needs VPN permission"
    override val update_available_title : String = "New version Flare %s available!"
    override val update_freq_daily : String = "Daily"
    override val update_freq_monthly : String = "Monthly"
    override val update_freq_weekly : String = "Weekly"
    override val server_manual_desc : String = "Enter connection details"
    override val vpn_active : String = "VPN active"
    override val vpn_disconnect : String = "Disconnect"
    override val vpn_stopping : String = "Stopping services"
    override val vpn_starting : String = "Starting services"
    override val vpn_error_permission_denied : String = "VPN permission denied"
    override val vpn_error_permission_required : String = "VPN permission required. Please open the app and confirm."
    override val vpn_error_tunnel_creation : String = "Failed to create tunnel"
    override val label_shadowsocks_dpi_bypass : String = "Bypass DPI (Plugins)"
    override val label_shadowsocks_dpi_bypass_hint : String = "Mask Shadowsocks as HTTPS traffic (WebSocket + TLS)"
    override val btn_done : String = "Done"
    override val data_mgmt_title : String = "Data Management"
    override val data_mgmt_export : String = "Export"
    override val data_mgmt_export_desc : String = "Create a file with a copy of your data"
    override val data_mgmt_import : String = "Import"
    override val data_mgmt_import_desc : String = "Load data from an existing file"
    override val data_mgmt_creating : String = "Creating backup..."
    override val data_mgmt_created : String = "Backup created successfully!"
    override val data_mgmt_select_title : String = "Which backup to restore?"
    override val data_mgmt_restoring : String = "Restoring backup..."
    override val data_mgmt_restored : String = "Backup restored successfully"
    override val data_mgmt_no_backups : String = "No backups found for restoration"
    override val status_disconnected : String = "Disconnected"
    override val status_connected : String = "Connected"
    override val status_connecting : String = "Connecting..."
    override val status_disconnecting : String = "Disconnecting..."
    override fun plural_apps (count: Int, vararg args: Any): String {
        val res = when (count) {
            1 -> "app"
            else -> "apps"
        }
        return res.format(*args)
    }
    override fun plural_sites (count: Int, vararg args: Any): String {
        val res = when (count) {
            1 -> "site"
            else -> "sites"
        }
        return res.format(*args)
    }
}
