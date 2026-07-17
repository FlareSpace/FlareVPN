package flare.client.app.ui.i18n

interface ErrorStrings {
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
    val error_downloading_rule: String
    val error_profile_selection_required: String
}

object RuErrorStrings : ErrorStrings {
    override val error_apps_list_empty : String = "Список пуст. Проверьте разрешение на список приложений в настройках."
    override val error_camera_permission_denied : String = "Разрешение на камеру отклонено"
    override val error_clipboard_empty : String = "Буфер обмена пуст"
    override val error_empty_name : String = "Название не может быть пустым"
    override val error_import_failed : String = "Не удалось добавить подписку или профиль!"
    override val error_import_file_read : String = "Не удалось прочитать файл"
    override val error_import_file_type : String = "Поддерживаются только файлы .txt и .json"
    override val error_import_timeout : String = "Не удалось добавить подписку или профиль (тайм-аут 10 секунд)."
    override val error_invalid_format : String = "Неверный формат. Поддерживаются: vless://, vmess://, ss://, trojan://, hysteria://, hy://, hysteria2://, hy2://, https:// и JSON"
    override val error_json : String = "Ошибка JSON: %s"
    override val error_link_generation : String = "Не удалось сгенерировать ссылку"
    override val error_open_settings : String = "Не удалось открыть настройки системы"
    override val error_parsing : String = "Ошибка парсинга: %s"
    override val error_profile_qr_generation : String = "Не удалось сгенерировать QR-код профиля"
    override val error_qr_not_found_in_image : String = "QR-код не найден на изображении"
    override val error_qr_scan_empty : String = "Не удалось распознать QR-код"
    override val error_subscription : String = "Ошибка подписки: %s"
    override val error_subscription_empty : String = "Подписка пуста"
    override val error_subscription_https_required : String = "Ссылка на подписку должна использовать HTTPS"
    override val error_downloading_rule : String = "Ошибка загрузки %1\$s: %2\$s"
    override val error_profile_selection_required : String = "Сначала выберите профиль!"
}

object EnErrorStrings : ErrorStrings {
    override val error_apps_list_empty : String = "List is empty. Check app list permission in settings."
    override val error_camera_permission_denied : String = "Camera permission denied"
    override val error_clipboard_empty : String = "Clipboard is empty"
    override val error_empty_name : String = "Name cannot be empty"
    override val error_import_failed : String = "Failed to add subscription or profile!"
    override val error_import_file_read : String = "Failed to read file"
    override val error_import_file_type : String = "Only .txt and .json files are supported"
    override val error_import_timeout : String = "Failed to add subscription or profile (10-second timeout)."
    override val error_invalid_format : String = "Invalid format. Supported: vless://, vmess://, ss://, trojan://, hysteria://, hy://, hysteria2://, hy2://, https:// and JSON"
    override val error_json : String = "JSON error: %s"
    override val error_link_generation : String = "Failed to generate link"
    override val error_open_settings : String = "Failed to open system settings"
    override val error_parsing : String = "Parsing error: %s"
    override val error_profile_qr_generation : String = "Failed to generate profile QR code"
    override val error_qr_not_found_in_image : String = "No QR code found in image"
    override val error_qr_scan_empty : String = "Failed to decode QR code"
    override val error_subscription : String = "Subscription error: %s"
    override val error_subscription_empty : String = "Subscription is empty"
    override val error_subscription_https_required : String = "Subscription URL must use HTTPS"
    override val error_downloading_rule : String = "Failed to download %1\$s: %2\$s"
    override val error_profile_selection_required : String = "Please select a profile first!"
}
