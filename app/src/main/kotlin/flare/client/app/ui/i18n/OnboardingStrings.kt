package flare.client.app.ui.i18n

interface OnboardingStrings {
    val onboarding_toast_notification_granted: String
    val onboarding_toast_notification_denied: String
    val onboarding_toast_battery_unrestricted: String
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
}

object RuOnboardingStrings : OnboardingStrings {
    override val onboarding_toast_notification_granted : String = "Разрешение на уведомления получено"
    override val onboarding_toast_notification_denied : String = "Уведомления отключены"
    override val onboarding_toast_battery_unrestricted : String = "Энергопотребление настроено"
    override val onboarding_battery_desc : String = "Что-бы приложение работало стабильно и Android его не закрывал нужно отключить экономию энергии для Flare"
    override val onboarding_battery_title : String = "Энергопотребление"
    override val onboarding_btn_go_main : String = "На главную"
    override val onboarding_fragmentation_desc : String = "Фрагментация помогает разделить пакет на несколько частей что помогает для обхода блокировок (DPI)"
    override val onboarding_fragmentation_question : String = "Хотели бы включить фрагментацию?"
    override val onboarding_fragmentation_title : String = "Фрагментация"
    override val onboarding_mux_desc : String = "Mux помогает ускорить соединение, но плохо подходит для обхода блокировок"
    override val onboarding_mux_question : String = "Хотели бы использовать Mux?"
    override val onboarding_mux_title : String = "Мультиплексирование"
    override val onboarding_notifications_desc : String = "Уведомления нужны чтобы приложение работало в фоне 24/7 а так же что-бы видеть актуальный статус туннеля"
    override val onboarding_notifications_error : String = "Разрешение на уведомления отклонено"
    override val onboarding_notifications_title : String = "Уведомления"
    override val onboarding_permissions_title : String = "Разрешения"
    override val onboarding_success_title : String = "Настройка успешно пройдена!"
    override val onboarding_usage_desc : String = "Это разрешение нужно для мониторинга приложений\\nЧтобы функция \\\"Триггер\\\" могла корректно работать"
    override val onboarding_usage_title : String = "Статистика использования"
    override val onboarding_welcome_question : String = "Хотите пройти первоначальную настройку?"
    override val onboarding_welcome_title : String = "Добро пожаловать в Flare!"
    override val onboarding_permissions_subtitle : String = "Необходимые разрешения для стабильной фоновой работы"
    override val onboarding_split_subtitle : String = "Выберите режим проксирования (можно пропустить)"
    override val onboarding_split_white_title : String = "Белый список"
    override val onboarding_split_white_desc : String = "Через прокси работают только выбранные приложения и сайты"
    override val onboarding_split_black_title : String = "Черный список"
    override val onboarding_split_black_desc : String = "Все работает через прокси кроме выбранных сайтов и приложений"
    override val onboarding_split_white_header : String = "Белый список: Что будет работать через прокси?"
    override val onboarding_split_black_header : String = "Черный список: Что НЕ будет работать через прокси?"
    override val onboarding_preset_ru_title : String = "Российские сервисы"
    override val onboarding_preset_ru_desc : String = "Госуслуги, Яндекс, банки, и другое"
    override val onboarding_preset_social_title : String = "Соцсети"
    override val onboarding_preset_social_desc : String = "Telegram, WhatsApp, и др"
    override val onboarding_preset_ai_title : String = "ИИ"
    override val onboarding_preset_ai_desc : String = "Gemini, Chat GPT, Claude и др"
    override val onboarding_success_desc : String = "Теперь Flare полностью настроен. Вы можете добавить профили и начать использование."
}

object EnOnboardingStrings : OnboardingStrings {
    override val onboarding_toast_notification_granted : String = "Notification permission granted"
    override val onboarding_toast_notification_denied : String = "Notifications disabled"
    override val onboarding_toast_battery_unrestricted : String = "Battery optimization configured"
    override val onboarding_battery_desc : String = "To keep the app stable and prevent Android from closing it, disable battery optimization for Flare"
    override val onboarding_battery_title : String = "Power consumption"
    override val onboarding_btn_go_main : String = "Go to Main"
    override val onboarding_fragmentation_desc : String = "Fragmentation helps split packets into parts to bypass blocks (DPI)"
    override val onboarding_fragmentation_question : String = "Would you like to enable fragmentation?"
    override val onboarding_fragmentation_title : String = "Fragmentation"
    override val onboarding_mux_desc : String = "Mux helps speed up connection, but is less effective against blocking"
    override val onboarding_mux_question : String = "Would you like to use Mux?"
    override val onboarding_mux_title : String = "Multiplexing"
    override val onboarding_notifications_desc : String = "Notifications are required for background work 24/7 and to see tunnel status"
    override val onboarding_notifications_error : String = "Notification permission denied"
    override val onboarding_notifications_title : String = "Notifications"
    override val onboarding_permissions_title : String = "Permissions"
    override val onboarding_success_title : String = "Setup completed successfully!"
    override val onboarding_usage_desc : String = "This permission is needed for application monitoring.\\nSo that the \\\"Trigger\\\" function can work correctly"
    override val onboarding_usage_title : String = "Usage Statistics"
    override val onboarding_welcome_question : String = "Do you want to run initial setup?"
    override val onboarding_welcome_title : String = "Welcome to Flare!"
    override val onboarding_permissions_subtitle : String = "Required permissions for stable background operation"
    override val onboarding_split_subtitle : String = "Select tunneling mode (can be skipped)"
    override val onboarding_split_white_title : String = "Whitelist"
    override val onboarding_split_white_desc : String = "Only selected apps and websites will go through the proxy"
    override val onboarding_split_black_title : String = "Blacklist"
    override val onboarding_split_black_desc : String = "Everything goes through the proxy except selected apps and websites"
    override val onboarding_split_white_header : String = "Whitelist: What will work through proxy?"
    override val onboarding_split_black_header : String = "Blacklist: What will NOT work through proxy?"
    override val onboarding_preset_ru_title : String = "Russian Services"
    override val onboarding_preset_ru_desc : String = "Gosuslugi, Yandex, banking apps, and more"
    override val onboarding_preset_social_title : String = "Social Networks"
    override val onboarding_preset_social_desc : String = "Telegram, WhatsApp, etc."
    override val onboarding_preset_ai_title : String = "AI Tools"
    override val onboarding_preset_ai_desc : String = "Gemini, ChatGPT, Claude, etc."
    override val onboarding_success_desc : String = "Now Flare is fully configured. You can add profiles and start using it."
}
