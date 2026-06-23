package flare.client.app.ui.i18n

interface SettingsStrings {
    val fakeip_desc: String
    val fragment_desc: String
    val gvisorstack_desc: String
    val icmp_desc: String
    val settings_label_use_sub_interval: String
    val settings_desc_use_sub_interval: String
    val mixedstack_desc: String
    val mtu_desc: String
    val mux_desc: String
    val dns_preset_cloudflare: String
    val dns_preset_adguard: String
    val dns_preset_google: String
    val settings_advanced_title: String
    val settings_basic_title: String
    val settings_bg_effects_header: String
    val settings_bg_effect_label: String
    val settings_bg_effect_none: String
    val settings_bg_effect_gradient: String
    val settings_bg_effect_shapes: String
    val settings_bg_effect_photo: String
    val settings_bg_effect_update_photo: String
    val settings_effects_header: String
    val settings_effect_blur: String
    val settings_effect_liquid_glass: String
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
    val settings_font_system: String
    val settings_font_google_sans: String
    val settings_font_inter: String
    val settings_header_app: String
    val settings_header_user_agent: String
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
    val settings_label_change_launch_button_color: String
    val settings_launch_button_color_yes: String
    val settings_launch_button_color_no: String
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
    val settings_label_ping_timeout: String
    val settings_desc_ping_timeout: String
    val settings_ping_timeout_sec: String
    val settings_label_sub_update_timeout: String
    val settings_desc_sub_update_timeout: String
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
    val systemstack_desc: String
    val tcp_desc: String
    val viaproxy_desc: String
    val settings_header_data_mgmt: String
    val settings_label_data_mgmt: String
    val settings_desc_data_mgmt: String
    val settings_btn_data_mgmt: String
}

object RuSettingsStrings : SettingsStrings {
    override val fakeip_desc : String = "Мгновенно выдает системе «поддельный» адрес для домена, не дожидаясь ответа от DNS-сервера. Предотвращает утечку DNS"
    override val fragment_desc : String = "Разделение больших пакетов данных на более мелкие части. Помогает в обходе блокировок (DPI)"
    override val gvisorstack_desc : String = "Высокая совместимость, поддерживает все настройки туннеля, среднее энергопотребление."
    override val icmp_desc : String = "Проверяет доступен ли сервер, используется для базовой проверки связи с сервером."
    override val settings_label_use_sub_interval : String = "Интервал подписок"
    override val settings_desc_use_sub_interval : String = "Обновляет подписки по их заданному интервалу например: Обновление 1 ч."
    override val mixedstack_desc : String = "Средняя совместимость, поддерживает большую часть настроек туннеля, высокое энергопотребление."
    override val mtu_desc : String = "Максимальный размер одного пакета данных (в байтах), который может быть передан за один раз."
    override val mux_desc : String = "Объединяет несколько запросов в одно соединение. Снижает задержку на создание новых подключений и ускоряет загрузку."
    override val dns_preset_cloudflare : String = "Cloudflare DoH"
    override val dns_preset_adguard : String = "AdGuard DNS (Антиреклама)"
    override val dns_preset_google : String = "Google DoT"
    override val settings_advanced_title : String = "Расширенные настройки"
    override val settings_basic_title : String = "Базовые настройки"
    override val settings_bg_effects_header : String = "Фоновые эффекты"
    override val settings_bg_effect_label : String = "Эффект"
    override val settings_bg_effect_none : String = "Выключен"
    override val settings_bg_effect_gradient : String = "Градиент"
    override val settings_bg_effect_shapes : String = "Фигуры"
    override val settings_bg_effect_photo : String = "Фото"
    override val settings_bg_effect_update_photo : String = "Обновить фото"
    override val settings_effects_header : String = "Эффекты"
    override val settings_effect_blur : String = "Размытие"
    override val settings_effect_liquid_glass : String = "«Жидкое стекло»"
    override val settings_btn_advanced : String = "Расширенные"
    override val settings_btn_base : String = "Базовые"
    override val settings_btn_change : String = "Изменить"
    override val settings_btn_change_font : String = "Изменить шрифт"
    override val settings_btn_journal : String = "Журнал"
    override val settings_color_material_you : String = "Material You"
    override val settings_desc_adaptive_tunnel : String = "Автоматически восстанавливает соединение при обрыве или выбирает рабочий сервер"
    override val settings_desc_best_profile : String = "Данная функция выбирает лучший сервер в подписке с самым маленьким пингом"
    override val settings_desc_hwid : String = "HWID - идентификатор для привязки подписки к вашему устройству. Позволяет повторно импортировать подписку без расхода лимита устройств. Данные не передаются."
    override val settings_desc_logging : String = "Включение логов полезно для отладки, но может раскрыть ваши серверные адреса и ключи."
    override val settings_desc_test_url : String = "Данный параметр отвечает за то какая ссылка будет использована для проверки задержки"
    override val settings_desc_update_check : String = "Проверка обновлений помогает вам использовать актуальную версию Flare."
    override val settings_font_geologica : String = "Geologica"
    override val settings_font_system : String = "Системный"
    override val settings_font_google_sans : String = "Google Sans"
    override val settings_font_inter : String = "Inter"
    override val settings_header_app : String = "Настройки приложения"
    override val settings_header_user_agent : String = "Настройки User-Agent"
    override val settings_header_appearance : String = "Оформление"
    override val settings_header_autostart : String = "Автозапуск"
    override val settings_header_best_profile : String = "Управление профилями"
    override val settings_header_hwid : String = "Управление HWID"
    override val settings_header_logging : String = "Логирование"
    override val settings_header_notifications : String = "Уведомления"
    override val settings_header_rules : String = "Правила"
    override val settings_header_updates : String = "Обновление"
    override val settings_header_vpn : String = "Настройки VPN"
    override val settings_hint_best_profile_interval : String = "1800"
    override val settings_hint_dns_url : String = "Auto"
    override val settings_hint_test_url : String = "https://www.google.com/generate_204"
    override val settings_hint_update_interval : String = "3600"
    override val settings_item_language : String = "Язык"
    override val settings_item_ping : String = "Пинг"
    override val settings_item_routing : String = "Маршрутизация"
    override val settings_item_subscriptions : String = "Подписки"
    override val settings_item_theme : String = "Персонализация"
    override val settings_label_adaptive_tunnel : String = "Адаптивный туннель"
    override val settings_label_auto_update : String = "Автообновление"
    override val settings_desc_auto_update : String = "Принудительно обновляет все подписки с заданным интервалом например раз в 3600 секунд"
    override val settings_label_autostart : String = "Создавать туннель при запуске приложения"
    override val settings_label_best_profile : String = "Автовыбор профиля"
    override val settings_label_best_profile_interval : String = "Обновлять выбор каждые"
    override val settings_label_best_profile_notif : String = "Выбор лучшего профиля"
    override val settings_label_best_profile_only_connected : String = "При активном VPN"
    override val settings_label_core_log : String = "Журнал SingBox"
    override val settings_label_core_log_level : String = "Уровень логирования"
    override val settings_label_custom_color : String = "Свой цвет"
    override val settings_label_change_launch_button_color : String = "Изменить цвет кнопки запуска"
    override val settings_launch_button_color_yes : String = "Да"
    override val settings_launch_button_color_no : String = "Нет"
    override val settings_label_dns_url : String = "URL"
    override val settings_label_enable_gradient : String = "Градиент"
    override val settings_label_fake_ip : String = "Поддельная DNS (Fake IP)"
    override val settings_label_font : String = "Шрифт"
    override val settings_label_fragment_interval : String = "Тайм-аут"
    override val settings_label_fragment_size : String = "Размер"
    override val settings_label_fragment_sleep : String = "Задержка"
    override val settings_label_fragmentation : String = "Фрагментация"
    override val settings_label_gradient_animation : String = "Анимация градиента"
    override val settings_label_gradient_speed : String = "Скорость анимации"
    override val settings_label_language : String = "Язык приложения"
    override val settings_label_mtu : String = "MTU"
    override val settings_label_mtu_title : String = "Изменение MTU"
    override val mtu_auto_btn : String = "Авто"
    override val mtu_auto_warning : String = "Установлен оптимальный MTU: %s"
    override val settings_label_mux : String = "Mux"
    override val settings_label_mux_padding : String = "Добавить шум"
    override val settings_label_mux_protocol : String = "Способ"
    override val settings_label_mux_streams : String = "Кол-во потоков"
    override val settings_label_noise_apply : String = "Применить к ip"
    override val settings_label_noise_delay : String = "Задержка"
    override val settings_label_noise_packet : String = "Пакет"
    override val settings_label_noise_type : String = "Тип"
    override val settings_label_packet_type : String = "Откат"
    override val settings_label_ping_display : String = "Отображение пинга"
    override val settings_label_ping_style : String = "Стиль"
    override val settings_label_ping_type : String = "Тип пинга"
    override val settings_label_remote_dns : String = "Remote DNS"
    override val settings_label_rules_method : String = "Способ"
    override val settings_header_chain : String = "Управление цепью"
    override val settings_label_reset_chain : String = "Сбрасывать цепь после отключения"
    override val settings_desc_reset_chain : String = "Автоматически очищать цепочку прокси после остановки VPN."
    override val settings_label_tls_spoof : String = "TLS Spoof"
    override val settings_desc_tls_spoof : String = "Подмена SNI для обхода блокировок. Отправляет поддельный ClientHello с белым доменом перед настоящим."
    override val settings_label_tls_spoof_domain : String = "Домен"
    override val settings_label_tls_spoof_method : String = "Метод"
    override val settings_label_fingerprint : String = "Отпечаток"
    override val settings_item_tls_fingerprint : String = "TLS Отпечаток"
    override val settings_desc_fingerprint : String = "Маскирует ваш TLS трафик под выбранный браузер/клиент. Если выбрано Auto, отпечаток будет взят из конфигурации."
    override val settings_label_send_hwid : String = "Передавать HWID"
    override val settings_label_split_tunneling : String = "Раздельное туннелирование"
    override val settings_label_stack : String = "%s"
    override val settings_label_stack_title : String = "Использовать"
    override val settings_label_status : String = "Статус"
    override val settings_label_status_notification : String = "Уведомление с статусом"
    override val settings_label_notification_speed : String = "Показывать скорость"
    override val settings_label_test_url : String = "Test-URL"
    override val settings_label_theme : String = "Стиль"
    override val settings_label_update_check : String = "Проверка обновлений"
    override val settings_label_update_every : String = "Обновлять подписки каждые"
    override val settings_label_update_frequency : String = "Частота проверки"
    override val settings_label_use : String = "Использовать"
    override val settings_label_use_fake_ip : String = "Включить Fake IP"
    override val settings_label_user_agent : String = "User-Agent"
    override val settings_language_in_dev : String = "Смена языка (в разработке)"
    override val settings_language_title : String = "Язык"
    override val settings_ping_interval_min_warning : String = "Минимальный интервал — 10 секунд"
    override val settings_ping_style_both : String = "Время и значок"
    override val settings_ping_style_icon : String = "Значок"
    override val settings_ping_style_time : String = "Время"
    override val settings_ping_title : String = "Настройки пинга"
    override val settings_label_ping_timeout : String = "Тайм-аут"
    override val settings_desc_ping_timeout : String = "Устанавливает максимальное время ожидания ответа при проверке задержки серверов. Чем больше таймаут, тем дольше будет идти проверка."
    override val settings_ping_timeout_sec : String = "%d сек."
    override val settings_label_sub_update_timeout : String = "Тайм-аут автообновления"
    override val settings_desc_sub_update_timeout : String = "Устанавливает максимальное время ожидания ответа при обновлении подписки."
    override val settings_ping_type_get : String = "via proxy"
    override val settings_ping_type_icmp : String = "ICMP"
    override val settings_ping_type_tcp : String = "TCP"
    override val settings_restart_tunnel_hint : String = "Настройки будут применены при следующем создании туннеля."
    override val settings_routing_title : String = "Маршрутизация"
    override val settings_stack_header : String = "Сетевой стек"
    override val settings_subscriptions_title : String = "Подписки"
    override val settings_theme_header : String = "Тема"
    override val settings_theme_title : String = "Персонализация"
    override val settings_title : String = "Настройки"
    override val systemstack_desc : String = "Низкая совместимость, не поддерживает фрагментацию и еще некоторые настройки туннеля, низкое энергопотребление."
    override val tcp_desc : String = "Проверяет скорость открытия порта на сервере и готов ли он принимать соединения."
    override val viaproxy_desc : String = "Проверяет время полного прохождения HTTP запроса через прокси, тестирует реальную задержку, самый точный метод."
    override val settings_header_data_mgmt : String = "Управление данными"
    override val settings_label_data_mgmt : String = "Сохранение и восстановление"
    override val settings_desc_data_mgmt : String = "Вы можете сохранить все настройки, профили, подписки приложения"
    override val settings_btn_data_mgmt : String = "Перенести"
}

object EnSettingsStrings : SettingsStrings {
    override val fakeip_desc : String = "Instantly gives the system a \\\"fake\\\" address for a domain without waiting for DNS response. Prevents DNS leaks"
    override val fragment_desc : String = "Splitting large data packets into smaller parts. Helps bypass blocks (DPI)"
    override val gvisorstack_desc : String = "High compatibility, supports all settings, medium power usage."
    override val icmp_desc : String = "Checks if server is available, used for basic connection check."
    override val settings_label_use_sub_interval : String = "Use subscription intervals"
    override val settings_desc_use_sub_interval : String = "Updates subscriptions according to their custom intervals, e.g. Update: 1 h."
    override val mixedstack_desc : String = "Average compatibility, supports most settings, high power usage."
    override val mtu_desc : String = "Maximum size of one data packet (in bytes) that can be sent at once."
    override val mux_desc : String = "Combines multiple requests into one connection. Reduces latency and speeds up loading."
    override val dns_preset_cloudflare : String = "Cloudflare DoH"
    override val dns_preset_adguard : String = "AdGuard DNS (Ad Block)"
    override val dns_preset_google : String = "Google DoT"
    override val settings_advanced_title : String = "Advanced Settings"
    override val settings_basic_title : String = "Basic Settings"
    override val settings_bg_effects_header : String = "Background Effects"
    override val settings_bg_effect_label : String = "Effect"
    override val settings_bg_effect_none : String = "None"
    override val settings_bg_effect_gradient : String = "Gradient"
    override val settings_bg_effect_shapes : String = "Shapes"
    override val settings_bg_effect_photo : String = "Photo"
    override val settings_bg_effect_update_photo : String = "Update photo"
    override val settings_effects_header : String = "Effects"
    override val settings_effect_blur : String = "Blur"
    override val settings_effect_liquid_glass : String = "«Liquid glass»"
    override val settings_btn_advanced : String = "Advanced"
    override val settings_btn_base : String = "Basic"
    override val settings_btn_change : String = "Change"
    override val settings_btn_change_font : String = "Change font"
    override val settings_btn_journal : String = "Journal"
    override val settings_color_material_you : String = "Material You"
    override val settings_desc_adaptive_tunnel : String = "Automatically recovers connection on drop or selects working server"
    override val settings_desc_best_profile : String = "This function selects the server with lowest ping in the subscription"
    override val settings_desc_hwid : String = "HWID is an identifier to link your subscription to your device. Thanks to it, re-importing is not counted as a new connection. All data is stored locally and is not transmitted anywhere else."
    override val settings_desc_logging : String = "Enabling logs is useful for debugging but may expose your server addresses and keys."
    override val settings_desc_test_url : String = "This parameter determines which link will be used for delay testing"
    override val settings_desc_update_check : String = "Checking updates helps you stay on the latest Flare version."
    override val settings_font_geologica : String = "Geologica"
    override val settings_font_system : String = "System"
    override val settings_font_google_sans : String = "Google Sans"
    override val settings_font_inter : String = "Inter"
    override val settings_header_app : String = "App Settings"
    override val settings_header_user_agent : String = "User-Agent Settings"
    override val settings_header_appearance : String = "Appearance"
    override val settings_header_autostart : String = "Autostart"
    override val settings_header_best_profile : String = "Profile management"
    override val settings_header_hwid : String = "HWID Management"
    override val settings_header_logging : String = "Logging"
    override val settings_header_notifications : String = "Notifications"
    override val settings_header_rules : String = "Rules"
    override val settings_header_updates : String = "Updates"
    override val settings_header_vpn : String = "VPN Settings"
    override val settings_hint_best_profile_interval : String = "1800"
    override val settings_hint_dns_url : String = "Auto"
    override val settings_hint_test_url : String = "https://www.google.com/generate_204"
    override val settings_hint_update_interval : String = "3600"
    override val settings_item_language : String = "Language"
    override val settings_item_ping : String = "Ping"
    override val settings_item_routing : String = "Routing"
    override val settings_item_subscriptions : String = "Subscriptions"
    override val settings_item_theme : String = "Personalization"
    override val settings_label_adaptive_tunnel : String = "Adaptive Tunnel"
    override val settings_label_auto_update : String = "Auto-update"
    override val settings_desc_auto_update : String = "Forces updates of all subscriptions at the specified interval, for example, once every 3600 seconds"
    override val settings_label_autostart : String = "Create tunnel on app launch"
    override val settings_label_best_profile : String = "Select best profile"
    override val settings_label_best_profile_interval : String = "Update selection every"
    override val settings_label_best_profile_notif : String = "Best profile selection"
    override val settings_label_best_profile_only_connected : String = "Only when connected"
    override val settings_label_core_log : String = "SingBox Log"
    override val settings_label_core_log_level : String = "Log Level"
    override val settings_label_custom_color : String = "Custom color"
    override val settings_label_change_launch_button_color : String = "Change launch button color"
    override val settings_launch_button_color_yes : String = "Yes"
    override val settings_launch_button_color_no : String = "No"
    override val settings_label_dns_url : String = "URL"
    override val settings_label_enable_gradient : String = "Gradient"
    override val settings_label_fake_ip : String = "Fake IP"
    override val settings_label_font : String = "Font"
    override val settings_label_fragment_interval : String = "Timeout"
    override val settings_label_fragment_size : String = "Size"
    override val settings_label_fragment_sleep : String = "Sleep"
    override val settings_label_fragmentation : String = "Fragmentation"
    override val settings_label_gradient_animation : String = "Gradient animation"
    override val settings_label_gradient_speed : String = "Animation speed"
    override val settings_label_language : String = "App Language"
    override val settings_label_mtu : String = "MTU"
    override val settings_label_mtu_title : String = "Change MTU"
    override val mtu_auto_btn : String = "Auto"
    override val mtu_auto_warning : String = "Optimal MTU has been set: %s"
    override val settings_label_mux : String = "Mux"
    override val settings_label_mux_padding : String = "Add noise"
    override val settings_label_mux_protocol : String = "Method"
    override val settings_label_mux_streams : String = "Streams count"
    override val settings_label_noise_apply : String = "Apply to IP"
    override val settings_label_noise_delay : String = "Delay"
    override val settings_label_noise_packet : String = "Packet"
    override val settings_label_noise_type : String = "Type"
    override val settings_label_packet_type : String = "Fallback"
    override val settings_label_ping_display : String = "Ping Display"
    override val settings_label_ping_style : String = "Style"
    override val settings_label_ping_type : String = "Ping type"
    override val settings_label_remote_dns : String = "Remote DNS"
    override val settings_label_rules_method : String = "Method"
    override val settings_header_chain : String = "Chain Management"
    override val settings_label_reset_chain : String = "Reset chain after disconnection"
    override val settings_desc_reset_chain : String = "Automatically clear the proxy chain after stopping the VPN."
    override val settings_label_tls_spoof : String = "TLS Spoof"
    override val settings_desc_tls_spoof : String = "SNI spoofing to bypass blocks. Sends a forged ClientHello with a whitelisted domain before the real one."
    override val settings_label_tls_spoof_domain : String = "Domain"
    override val settings_label_tls_spoof_method : String = "Method"
    override val settings_label_fingerprint : String = "Fingerprint"
    override val settings_item_tls_fingerprint : String = "TLS Fingerprint"
    override val settings_desc_fingerprint : String = "Masks your TLS traffic as a selected browser/client. If Auto is selected, the fingerprint will be taken from the configuration."
    override val settings_label_send_hwid : String = "Send HWID"
    override val settings_label_split_tunneling : String = "Split Tunneling"
    override val settings_label_stack : String = "%s"
    override val settings_label_stack_title : String = "Use"
    override val settings_label_status : String = "Status"
    override val settings_label_status_notification : String = "Status notification"
    override val settings_label_notification_speed : String = "Show speed"
    override val settings_label_test_url : String = "Test URL"
    override val settings_label_theme : String = "Style"
    override val settings_label_update_check : String = "Check updates"
    override val settings_label_update_every : String = "Update subscriptions every"
    override val settings_label_update_frequency : String = "Check frequency"
    override val settings_label_use : String = "Use"
    override val settings_label_use_fake_ip : String = "Enable Fake IP"
    override val settings_label_user_agent : String = "User-Agent"
    override val settings_language_in_dev : String = "Language switching (in development)"
    override val settings_language_title : String = "Language"
    override val settings_ping_interval_min_warning : String = "Minimum interval is 10 seconds"
    override val settings_ping_style_both : String = "Time & Icon"
    override val settings_ping_style_icon : String = "Icon"
    override val settings_ping_style_time : String = "Time"
    override val settings_ping_title : String = "Ping Settings"
    override val settings_label_ping_timeout : String = "Timeout"
    override val settings_desc_ping_timeout : String = "Sets the maximum wait time for a response when checking server latency. A longer timeout will result in a longer checking process."
    override val settings_ping_timeout_sec : String = "%d sec"
    override val settings_label_sub_update_timeout : String = "Auto-update timeout"
    override val settings_desc_sub_update_timeout : String = "Sets the maximum wait time for a response when updating a subscription."
    override val settings_ping_type_get : String = "via proxy"
    override val settings_ping_type_icmp : String = "ICMP"
    override val settings_ping_type_tcp : String = "TCP"
    override val settings_restart_tunnel_hint : String = "Settings will be applied on next tunnel creation."
    override val settings_routing_title : String = "Routing"
    override val settings_stack_header : String = "Network Stack"
    override val settings_subscriptions_title : String = "Subscriptions"
    override val settings_theme_header : String = "Theme"
    override val settings_theme_title : String = "Personalization"
    override val settings_title : String = "Settings"
    override val systemstack_desc : String = "Low compatibility, doesn\\'t support fragmentation and some other settings, low power usage."
    override val tcp_desc : String = "Checks port opening speed and readiness to accept connections."
    override val viaproxy_desc : String = "Checks HTTP request time through proxy, tests real latency, most accurate method."
    override val settings_header_data_mgmt : String = "Data Management"
    override val settings_label_data_mgmt : String = "Backup & Restore"
    override val settings_desc_data_mgmt : String = "You can save all settings, profiles, and subscriptions of the app"
    override val settings_btn_data_mgmt : String = "Transfer"
}
