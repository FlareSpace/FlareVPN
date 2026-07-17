package flare.client.app.ui.i18n

interface ServerStrings {
    val wizard_step_ssh: String
    val wizard_step_protocol: String
    val wizard_step_settings: String
    val wizard_step_setup: String
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
    val tariff_premium_title: String
    val tariff_free_desc: String
    val tariff_free_price: String
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
    val wizard_setup_configuring: String
    val wizard_setup_free_title: String
    val wizard_setup_auth_title: String
    val wizard_setup_auth_waiting: String
    val wizard_setup_auth_desc: String
    val wizard_setup_auth_error_title: String
    val wizard_setup_auth_retry: String
    val wizard_setup_auth_error_timeout: String
    val wizard_setup_auth_error_network: String
    val wizard_setup_buy_title: String
    val wizard_setup_buy_action: String
    val wizard_setup_buy_desc: String
    val wizard_setup_buy_open_tg: String
    val wizard_setup_buy_already_purchased: String
    val wizard_setup_free_auth_prompt_title: String
    val wizard_setup_free_auth_prompt_subtitle: String
    val wizard_setup_free_auth_prompt_desc: String
    val wizard_setup_free_auth_prompt_with: String
    val wizard_setup_free_auth_prompt_without: String
    val wizard_setup_free_status: String
    val wizard_setup_free_limit_exceeded: String
    val wizard_setup_free_telegram_required: String
    val wizard_setup_free_parse_error: String
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
}

object RuServerStrings : ServerStrings {
    override val wizard_step_ssh : String = "SSH"
    override val wizard_step_protocol : String = "Протокол"
    override val wizard_step_settings : String = "Настройки"
    override val wizard_step_setup : String = "Установка"
    override val servers_desc_create : String = "Ваш собственный сервер, который контролируете только вы."
    override val servers_desc_flare : String = "FlareVPN — это быстрый, не хранящий логов и доступный 24/7 VPN."
    override val servers_protocol_shadowsocks_desc : String = "Энергоэффективный и быстрый протокол шифрования на базе SOCKS5 с методами AEAD. Обеспечивает высокую производительность и защиту данных при минимальной нагрузке."
    override val servers_protocol_shadowsocks_title : String = "ShadowSocks"
    override val servers_protocol_title : String = "Выберите протокол"
    override val servers_protocol_wireguard_desc : String = "Легковесный и современный протокол сетевого уровня на базе UDP с передовой криптографией. Обеспечивает мгновенное подключение и максимальную пропускную способность."
    override val servers_protocol_wireguard_title : String = "WireGuard"
    override val servers_protocol_xray_desc : String = "Xray с REALITY маскирует VPN-трафик под веб-трафик. Обладает высокой устойчивостью к обнаружению, обеспечивает высокую приватность и скорость."
    override val servers_protocol_xray_title : String = "Xray с REALITY"
    override val servers_setup_success : String = "Сервер был успешно создан!"
    override val servers_setup_success_desc : String = "Вы можете найти его в подписке «Мои серверы»."
    override val servers_setup_title : String = "Настройка сервера..."
    override val servers_ssh_ip : String = "IP-адрес"
    override val servers_ssh_password : String = "Пароль или ключ SSH"
    override val servers_ssh_port : String = "Порт SSH"
    override val servers_ssh_port_hint : String = "22"
    override val servers_ssh_profile_name : String = "Имя профиля"
    override val servers_ssh_profile_name_hint : String = "Мой сервер"
    override val servers_ssh_username : String = "Имя пользователя SSH"
    override val servers_title_create : String = "Создать свой сервер"
    override val servers_title_flare : String = "Серверы Flare"
    override val servers_ssh_title : String = "Параметры подключения SSH"
    override val servers_xray_title : String = "Параметры Xray"
    override val servers_hysteria2_title : String = "Параметры Hysteria 2"
    override val servers_shadowsocks_title : String = "Параметры Shadowsocks"
    override val servers_setup_progress_title : String = "Установка и настройка"
    override val servers_setup_success_title : String = "Установка завершена"
    override val servers_subscription_added_title : String = "Подписка добавлена"
    override val servers_subscription_failed_title : String = "Ошибка добавления"
    override val servers_tariff_title : String = "Выберите план"
    override val tariff_free_title : String = "Free"
    override val tariff_premium_title : String = "Premium"
    override val tariff_free_desc : String = "Те же быстрые серверы из Premium, но с ограничением трафика и устройств"
    override val tariff_free_price : String = "0р/мес"
    override val tariff_premium_desc : String = "Максимальная стабильность и скорость, безлимитный трафик"
    override val tariff_premium_price : String = "250р/мес"
    override val tariff_success_title : String = "Подписка Free добавлена"
    override val tariff_success_desc : String = "Вы найдёте её в списке."
    override val tariff_error_title : String = "Не удалось добавить подписку Free"
    override val tariff_error_desc : String = "Попробуйте позже."
    override val servers_xray_port_desc : String = "Порт, на котором будет работать ваш VPN-сервер. 443 — стандартный порт для маскировки под HTTPS."
    override val servers_xray_port_label : String = "Порт Xray"
    override val servers_xray_setup_title : String = "Настройка Xray"
    override val servers_xray_sni_desc : String = "Домен, под который будет маскироваться ваш трафик. Google.com — надежный вариант по умолчанию."
    override val servers_xray_sni_label : String = "SNI (Server Name Indication)"
    override val simple_editor_alpn : String = "ALPN"
    override val simple_editor_basic : String = "Базовые настройки"
    override val simple_editor_enable_tls : String = "Включить TLS"
    override val simple_editor_fingerprint : String = "Fingerprint"
    override val simple_editor_flow : String = "Flow"
    override val simple_editor_packet_encoding : String = "Packet Encoding"
    override val simple_editor_method : String = "Метод шифрования"
    override val simple_editor_obfs : String = "Obfs"
    override val simple_editor_obfs_pass : String = "Obfs Пароль"
    override val simple_editor_pbk : String = "Public Key"
    override val simple_editor_port : String = "Порт"
    override val simple_editor_reality : String = "Настройки Reality"
    override val simple_editor_server : String = "Сервер (Host или IP)"
    override val simple_editor_sid : String = "Short ID"
    override val simple_editor_sni : String = "SNI"
    override val simple_editor_tag : String = "Имя профиля"
    override val simple_editor_title : String = "Редактор"
    override val simple_editor_tls : String = "Настройки TLS"
    override val simple_editor_uuid_pwd : String = "UUID / Пароль"
    override val simple_editor_up_mbps : String = "Скорость отдачи (Up Mbps)"
    override val simple_editor_down_mbps : String = "Скорость загрузки (Down Mbps)"
    override val simple_editor_allow_insecure : String = "Разрешить небезопасный TLS (Insecure)"
    override val simple_editor_hysteria_settings : String = "Настройки Hysteria"
    override val simple_editor_hop_interval : String = "Интервал смены порта (Hop interval)"
    override val ssh_error_config_write : String = "Конфигурационный файл не был записан на сервер."
    override val ssh_error_generic : String = "Ошибка: %s"
    override val ssh_error_keys : String = "Не удалось получить ключи REALITY."
    override val ssh_error_port_not_listening : String = "Xray запущен, но не слушает порт %d!"
    override val ssh_error_service_start : String = "Сервис Xray не запустился (статус: %s)"
    override val ssh_status_configuring : String = "Настройка конфигурации..."
    override val ssh_status_connecting : String = "Подключение к серверу..."
    override val ssh_status_generating_client : String = "Генерация настроек клиента..."
    override val ssh_status_generating_keys : String = "Генерация ключей REALITY..."
    override val ssh_status_installing_xray : String = "Установка Xray..."
    override val ssh_status_restarting : String = "Перезапуск сервиса Xray..."
    override val ssh_status_waiting : String = "Ожидание запуска..."
    override val wizard_setup_configuring : String = "Настройка конфигурации..."
    override val wizard_setup_free_title : String = "Настраиваем вашу подписку..."
    override val wizard_setup_auth_title: String = "Авторизация"
    override val wizard_setup_auth_waiting: String = "Ожидание входа в Telegram..."
    override val wizard_setup_auth_desc: String = "Пожалуйста, перейдите в бота FlareVPN и подтвердите авторизацию. Этот экран обновится автоматически."
    override val wizard_setup_auth_error_title: String = "Ошибка авторизации"
    override val wizard_setup_auth_retry: String = "Повторить"
    override val wizard_setup_auth_error_timeout: String = "Время ожидания истекло. Попробуйте еще раз."
    override val wizard_setup_auth_error_network: String = "Ошибка соединения с сервером"
    override val wizard_setup_buy_title: String = "Покупка подписки"
    override val wizard_setup_buy_action: String = "Пополнение баланса"
    override val wizard_setup_buy_desc: String = "Пополните баланс любым удобным способом: криптовалютой прямо в приложении или через нашего официального Telegram-бота."
    override val wizard_setup_buy_open_tg: String = "Открыть Telegram"
    override val wizard_setup_buy_already_purchased: String = "Я уже купил(а)"
    override val wizard_setup_free_auth_prompt_title: String = "Бесплатная подписка"
    override val wizard_setup_free_auth_prompt_subtitle: String = "Выбор типа подписки"
    override val wizard_setup_free_auth_prompt_desc: String = "Вы можете добавить базовую бесплатную подписку без авторизации с лимитом 1.5 гб на 24 часа. С авторизацией вы можете добавить бесплатную подписку с лимитом 10 гб на 30 дней. Бесплатную подписку можно получать каждый месяц."
    override val wizard_setup_free_auth_prompt_with: String = "С авторизацией (10 ГБ / 30 дней)"
    override val wizard_setup_free_auth_prompt_without: String = "Без авторизации (1.5 ГБ / 24 часа)"
    override val wizard_setup_free_status: String = "Создание и настройка подписки..."
    override val wizard_setup_free_limit_exceeded: String = "Лимит бесплатных ключей превышен"
    override val wizard_setup_free_telegram_required: String = "Для получения ключа 10 ГБ необходимо войти через Telegram"
    override val wizard_setup_free_parse_error: String = "Ошибка парсинга"
    override val wizard_setup_optimizing : String = "Оптимизация..."
    override val wizard_setup_ready : String = "Сервер готов!"
    override val wizard_setup_saving : String = "Сохранение..."
    override val wizard_setup_validating : String = "Проверка данных..."
    override val wizard_xray_port_hint : String = "443 (по умолчанию)"
    override val wizard_xray_sni_hint : String = "google.com (по умолчанию)"
    override val simple_editor_cert_pin : String = "Отпечаток сертификата SHA-256"
    override val servers_protocol_hysteria2_title : String = "Hysteria 2"
    override val servers_protocol_hysteria2_desc : String = "Высокоскоростной протокол на базе UDP (QUIC) с маскировкой под HTTPS и встроенным обходом блокировок."
    override val ssh_status_installing_hysteria2 : String = "Установка Hysteria 2..."
    override val ssh_status_generating_cert : String = "Генерация TLS сертификата..."
    override val ssh_status_configuring_hysteria2 : String = "Настройка Hysteria 2..."
    override val ssh_status_restarting_hysteria2 : String = "Перезапуск сервиса Hysteria 2..."
    override val ssh_error_cert : String = "Не удалось сгенерировать TLS сертификат."
    override val ssh_error_port_not_listening_udp : String = "Hysteria 2 запущена, но не слушает порт %d (UDP)!"
    override val ssh_error_service_start_hysteria2 : String = "Сервис Hysteria 2 не запустился (статус: %s)"
    override val servers_hysteria2_port_label : String = "Порт Hysteria 2"
    override val wizard_hysteria2_port_hint : String = "443 (по умолчанию)"
    override val servers_hysteria2_sni_label : String = "Домен маскировки (SNI)"
    override val wizard_hysteria2_sni_hint : String = "google.com (по умолчанию)"
    override val servers_hysteria2_obfs_pass_label : String = "Obfs Пароль (Опционально)"
    override val wizard_hysteria2_obfs_pass_hint : String = "salamander_pass (по умолчанию)"
    override val servers_hysteria2_port_hopping_label : String = "Port Hopping"
    override val servers_hysteria2_port_hopping_auto : String = "Авто"
    override val servers_hysteria2_port_hopping_manual : String = "Вручную"
    override val wizard_hysteria2_port_hopping_hint : String = "Диапазон, например 20000-50000"
    override val ssh_status_installing_shadowsocks : String = "Установка Shadowsocks..."
    override val ssh_status_configuring_shadowsocks : String = "Настройка Shadowsocks..."
    override val ssh_status_restarting_shadowsocks : String = "Перезапуск сервиса Shadowsocks..."
    override val ssh_error_service_start_shadowsocks : String = "Сервис Shadowsocks не запустился (статус: %s)"
    override val servers_shadowsocks_port_label : String = "Порт Shadowsocks"
    override val wizard_shadowsocks_port_hint : String = "8388 (по умолчанию)"
    override val servers_shadowsocks_sni_label : String = "Домен маскировки (SNI)"
    override val wizard_shadowsocks_sni_hint : String = "google.com (по умолчанию)"
    override val servers_wireguard_title : String = "Параметры WireGuard"
    override val servers_wireguard_port_label : String = "Порт WireGuard"
    override val wizard_wireguard_port_hint : String = "51820 (по умолчанию)"
    override val ssh_status_installing_wireguard : String = "Установка WireGuard..."
    override val ssh_status_configuring_wireguard : String = "Настройка WireGuard..."
    override val ssh_status_restarting_wireguard : String = "Запуск службы WireGuard..."
    override val ssh_error_service_start_wireguard : String = "Не удалось запустить службу WireGuard. Статус: %s"
    override val simple_editor_shadowtls_password : String = "ShadowTLS Пароль"
    override val simple_editor_shadowtls_version : String = "ShadowTLS Версия"
    override val simple_editor_ss_network : String = "Сеть (Transport)"
    override val simple_editor_ss_ws_path : String = "WebSocket Путь"
    override val simple_editor_ss_ws_host : String = "WebSocket Host (Заголовок)"
    override val simple_editor_tls_type : String = "Тип TLS"
    override val simple_editor_http_host : String = "HTTP Host"
    override val simple_editor_path : String = "Путь"
    override val simple_editor_host : String = "Хост (Host)"
    override val simple_editor_kcp_seed : String = "KCP Seed"
    override val simple_editor_mtu : String = "MTU"
    override val simple_editor_tti : String = "TTI"
    override val simple_editor_httpupgrade_host : String = "HTTPUpgrade Host"
    override val simple_editor_httpupgrade_path : String = "HTTPUpgrade Путь"
    override val simple_editor_h2_host : String = "H2 Host"
    override val simple_editor_h2_path : String = "H2 Путь"
    override val simple_editor_quic_security : String = "QUIC Шифрование"
    override val simple_editor_quic_key : String = "QUIC Ключ"
    override val simple_editor_grpc_authority : String = "gRPC Authority"
    override val simple_editor_grpc_service_name : String = "gRPC serviceName"
    override val simple_editor_mode : String = "Режим"
}

object EnServerStrings : ServerStrings {
    override val wizard_step_ssh : String = "SSH"
    override val wizard_step_protocol : String = "Protocol"
    override val wizard_step_settings : String = "Settings"
    override val wizard_step_setup : String = "Setup"
    override val servers_desc_create : String = "Your own server that only you control."
    override val servers_desc_flare : String = "FlareVPN is a fast, no-logs, and 24/7 available VPN."
    override val servers_protocol_shadowsocks_desc : String = "Energy-efficient and fast SOCKS5-based encryption protocol with AEAD ciphers. Provides high performance and data protection with minimal overhead."
    override val servers_protocol_shadowsocks_title : String = "ShadowSocks"
    override val servers_protocol_title : String = "Select protocol"
    override val servers_protocol_wireguard_desc : String = "Lightweight and modern UDP-based network-layer protocol with advanced cryptography. Provides instant connection and maximum throughput."
    override val servers_protocol_wireguard_title : String = "WireGuard"
    override val servers_protocol_xray_desc : String = "Xray with REALITY masks VPN traffic as web traffic. High detection resistance, provides high privacy and speed"
    override val servers_protocol_xray_title : String = "Xray with REALITY"
    override val servers_setup_success : String = "Server created successfully!"
    override val servers_setup_success_desc : String = "You can find it in the \\\"My Servers\\\" subscription."
    override val servers_setup_title : String = "Server setup..."
    override val servers_ssh_ip : String = "IP address"
    override val servers_ssh_password : String = "SSH Password or Key"
    override val servers_ssh_port : String = "SSH Port"
    override val servers_ssh_port_hint : String = "22"
    override val servers_ssh_profile_name : String = "Profile name"
    override val servers_ssh_profile_name_hint : String = "My server"
    override val servers_ssh_username : String = "SSH Username"
    override val servers_title_create : String = "Create your server"
    override val servers_title_flare : String = "Flare Servers"
    override val servers_ssh_title : String = "SSH Connection Details"
    override val servers_xray_title : String = "Xray Configuration"
    override val servers_hysteria2_title : String = "Hysteria 2 Configuration"
    override val servers_shadowsocks_title : String = "Shadowsocks Configuration"
    override val servers_setup_progress_title : String = "Installation & Setup"
    override val servers_setup_success_title : String = "Installation Completed"
    override val servers_subscription_added_title : String = "Subscription Added"
    override val servers_subscription_failed_title : String = "Setup Failed"
    override val servers_tariff_title : String = "Select Plan"
    override val tariff_free_title : String = "Free"
    override val tariff_premium_title : String = "Premium"
    override val tariff_free_desc : String = "Same fast Premium servers, but with traffic and device limits"
    override val tariff_free_price : String = "$0/mo"
    override val tariff_premium_desc : String = "Maximum stability and speed, unlimited traffic"
    override val tariff_premium_price : String = "$3.50/mo"
    override val tariff_success_title : String = "Free subscription added"
    override val tariff_success_desc : String = "You will find it in the list."
    override val tariff_error_title : String = "Failed to add Free subscription"
    override val tariff_error_desc : String = "Please try again later."
    override val servers_xray_port_desc : String = "Port for your VPN server. 443 — standard port for HTTPS masking."
    override val servers_xray_port_label : String = "Xray Port"
    override val servers_xray_setup_title : String = "Xray Setup"
    override val servers_xray_sni_desc : String = "Domain to mask your traffic under. Google.com — reliable default option."
    override val servers_xray_sni_label : String = "SNI (Server Name Indication)"
    override val simple_editor_alpn : String = "ALPN"
    override val simple_editor_basic : String = "Basic Settings"
    override val simple_editor_enable_tls : String = "Enable TLS"
    override val simple_editor_fingerprint : String = "Fingerprint"
    override val simple_editor_flow : String = "Flow"
    override val simple_editor_packet_encoding : String = "Packet Encoding"
    override val simple_editor_method : String = "Encryption method"
    override val simple_editor_obfs : String = "Obfs"
    override val simple_editor_obfs_pass : String = "Obfs Password"
    override val simple_editor_pbk : String = "Public Key"
    override val simple_editor_port : String = "Port"
    override val simple_editor_reality : String = "Reality Settings"
    override val simple_editor_server : String = "Server (Host or IP)"
    override val simple_editor_sid : String = "Short ID"
    override val simple_editor_sni : String = "SNI"
    override val simple_editor_tag : String = "Profile name"
    override val simple_editor_title : String = "Editor"
    override val simple_editor_tls : String = "TLS Settings"
    override val simple_editor_uuid_pwd : String = "UUID / Password"
    override val simple_editor_up_mbps : String = "Upload Speed (Up Mbps)"
    override val simple_editor_down_mbps : String = "Download Speed (Down Mbps)"
    override val simple_editor_allow_insecure : String = "Allow Insecure TLS"
    override val simple_editor_hysteria_settings : String = "Hysteria Settings"
    override val simple_editor_hop_interval : String = "Hop Interval"
    override val ssh_error_config_write : String = "Config file was not written to server"
    override val ssh_error_generic : String = "Error: %s"
    override val ssh_error_keys : String = "Failed to get REALITY keys."
    override val ssh_error_port_not_listening : String = "Xray is running but not listening on port %d!"
    override val ssh_error_service_start : String = "Xray service failed to start (status: %s)"
    override val ssh_status_configuring : String = "Configuring setup..."
    override val ssh_status_connecting : String = "Connecting to server..."
    override val ssh_status_generating_client : String = "Generating client settings..."
    override val ssh_status_generating_keys : String = "Generating REALITY keys..."
    override val ssh_status_installing_xray : String = "Installing Xray..."
    override val ssh_status_restarting : String = "Restarting Xray service..."
    override val ssh_status_waiting : String = "Waiting for startup..."
    override val wizard_setup_configuring : String = "Configuring setup..."
    override val wizard_setup_free_title : String = "Setting up your subscription..."
    override val wizard_setup_auth_title: String = "Authorization"
    override val wizard_setup_auth_waiting: String = "Waiting for Telegram login..."
    override val wizard_setup_auth_desc: String = "Please go to the FlareVPN bot and confirm authorization. This screen will update automatically."
    override val wizard_setup_auth_error_title: String = "Authorization Error"
    override val wizard_setup_auth_retry: String = "Retry"
    override val wizard_setup_auth_error_timeout: String = "Request timed out. Please try again."
    override val wizard_setup_auth_error_network: String = "Server connection error"
    override val wizard_setup_buy_title: String = "Subscription Purchase"
    override val wizard_setup_buy_action: String = "Top Up Balance"
    override val wizard_setup_buy_desc: String = "Top up your balance in any convenient way: with cryptocurrency directly in the app or via our official Telegram bot."
    override val wizard_setup_buy_open_tg: String = "Open Telegram"
    override val wizard_setup_buy_already_purchased: String = "I already purchased"
    override val wizard_setup_free_auth_prompt_title: String = "Free Subscription"
    override val wizard_setup_free_auth_prompt_subtitle: String = "Select subscription type"
    override val wizard_setup_free_auth_prompt_desc: String = "You can add a basic free subscription without authorization with a 1.5 GB limit for 24 hours. With authorization, you can add a free subscription with a 10 GB limit for 30 days. The free subscription can be obtained every month."
    override val wizard_setup_free_auth_prompt_with: String = "With authorization (10 GB / 30 days)"
    override val wizard_setup_free_auth_prompt_without: String = "Without authorization (1.5 GB / 24 hours)"
    override val wizard_setup_free_status: String = "Creating and setting up subscription..."
    override val wizard_setup_free_limit_exceeded: String = "Free keys limit exceeded"
    override val wizard_setup_free_telegram_required: String = "To get the 10 GB key, you must log in via Telegram"
    override val wizard_setup_free_parse_error: String = "Parse error"
    override val wizard_setup_optimizing : String = "Optimizing..."
    override val wizard_setup_ready : String = "Server ready!"
    override val wizard_setup_saving : String = "Saving..."
    override val wizard_setup_validating : String = "Validating data..."
    override val wizard_xray_port_hint : String = "443 (default)"
    override val wizard_xray_sni_hint : String = "google.com (default)"
    override val simple_editor_cert_pin : String = "SHA-256 certificate fingerprint"
    override val servers_protocol_hysteria2_title : String = "Hysteria 2"
    override val servers_protocol_hysteria2_desc : String = "High-speed UDP-based (QUIC) protocol with HTTPS masquerading and built-in censorship resistance."
    override val ssh_status_installing_hysteria2 : String = "Installing Hysteria 2..."
    override val ssh_status_generating_cert : String = "Generating TLS certificate..."
    override val ssh_status_configuring_hysteria2 : String = "Configuring Hysteria 2..."
    override val ssh_status_restarting_hysteria2 : String = "Restarting Hysteria 2 service..."
    override val ssh_error_cert : String = "Failed to generate TLS certificate."
    override val ssh_error_port_not_listening_udp : String = "Hysteria 2 is running but not listening on port %d (UDP)!"
    override val ssh_error_service_start_hysteria2 : String = "Hysteria 2 service failed to start (status: %s)"
    override val servers_hysteria2_port_label : String = "Hysteria 2 Port"
    override val wizard_hysteria2_port_hint : String = "443 (default)"
    override val servers_hysteria2_sni_label : String = "Masquerade Domain (SNI)"
    override val wizard_hysteria2_sni_hint : String = "google.com (default)"
    override val servers_hysteria2_obfs_pass_label : String = "Obfs Password (Optional)"
    override val wizard_hysteria2_obfs_pass_hint : String = "salamander_pass (default)"
    override val servers_hysteria2_port_hopping_label : String = "Port Hopping"
    override val servers_hysteria2_port_hopping_auto : String = "Auto"
    override val servers_hysteria2_port_hopping_manual : String = "Manual"
    override val wizard_hysteria2_port_hopping_hint : String = "Range, e.g. 20000-50000"
    override val ssh_status_installing_shadowsocks : String = "Installing Shadowsocks..."
    override val ssh_status_configuring_shadowsocks : String = "Configuring Shadowsocks..."
    override val ssh_status_restarting_shadowsocks : String = "Restarting Shadowsocks..."
    override val ssh_error_service_start_shadowsocks : String = "Shadowsocks service failed to start (status: %s)"
    override val servers_shadowsocks_port_label : String = "Shadowsocks Port"
    override val wizard_shadowsocks_port_hint : String = "8388 (default)"
    override val servers_shadowsocks_sni_label : String = "Masquerade Domain (SNI)"
    override val wizard_shadowsocks_sni_hint : String = "google.com (default)"
    override val servers_wireguard_title : String = "WireGuard Settings"
    override val servers_wireguard_port_label : String = "WireGuard Port"
    override val wizard_wireguard_port_hint : String = "51820 (default)"
    override val ssh_status_installing_wireguard : String = "Installing WireGuard..."
    override val ssh_status_configuring_wireguard : String = "Configuring WireGuard..."
    override val ssh_status_restarting_wireguard : String = "Starting WireGuard service..."
    override val ssh_error_service_start_wireguard : String = "WireGuard service failed to start (status: %s)"
    override val simple_editor_shadowtls_password : String = "ShadowTLS Password"
    override val simple_editor_shadowtls_version : String = "ShadowTLS Version"
    override val simple_editor_ss_network : String = "Network (Transport)"
    override val simple_editor_ss_ws_path : String = "WebSocket Path"
    override val simple_editor_ss_ws_host : String = "WebSocket Host (Header)"
    override val simple_editor_tls_type : String = "TLS Type"
    override val simple_editor_http_host : String = "HTTP Host"
    override val simple_editor_path : String = "Path"
    override val simple_editor_host : String = "Host"
    override val simple_editor_kcp_seed : String = "KCP Seed"
    override val simple_editor_mtu : String = "MTU"
    override val simple_editor_tti : String = "TTI"
    override val simple_editor_httpupgrade_host : String = "HTTPUpgrade Host"
    override val simple_editor_httpupgrade_path : String = "HTTPUpgrade Path"
    override val simple_editor_h2_host : String = "H2 Host"
    override val simple_editor_h2_path : String = "H2 Path"
    override val simple_editor_quic_security : String = "QUIC Security"
    override val simple_editor_quic_key : String = "QUIC Key"
    override val simple_editor_grpc_authority : String = "gRPC Authority"
    override val simple_editor_grpc_service_name : String = "gRPC serviceName"
    override val simple_editor_mode : String = "Mode"
}
