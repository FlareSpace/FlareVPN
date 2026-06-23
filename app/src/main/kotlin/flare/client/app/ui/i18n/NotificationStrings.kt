package flare.client.app.ui.i18n

interface NotificationStrings {
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
    val notif_update_title: String
}

object RuNotificationStrings : NotificationStrings {
    override val notif_adaptive_tunnel_changed_body : String = "Профиль был изменен на %1\$s после обрыва соединения."
    override val notif_adaptive_tunnel_changed_title : String = "Профиль изменен"
    override val notif_best_profile_body : String = "Выбор профиля был обновлен на %1\$s с пингом %2\$dms"
    override val notif_best_profile_title : String = "Обновление профиля"
    override val notif_language_changed : String = "Язык приложения изменен на %s"
    override val notif_language_changed_auto : String = "Язык приложения был изменен!"
    override val notif_theme_changed : String = "Тема приложения была успешно изменена!"
    override val notif_theme_changed_auto : String = "Тема приложения была изменена автоматически!"
    override val notif_notifications_enabled : String = "Уведомления успешно включены!"
    override val notif_profile_changed : String = "Данные профиля изменены!"
    override val notif_update_title : String = "Обновление Flare"
}

object EnNotificationStrings : NotificationStrings {
    override val notif_adaptive_tunnel_changed_body : String = "Profile was changed to %1\$s after connection drop."
    override val notif_adaptive_tunnel_changed_title : String = "Profile changed"
    override val notif_best_profile_body : String = "Profile updated to %1\$s with ping %2\$dms"
    override val notif_best_profile_title : String = "Profile Update"
    override val notif_language_changed : String = "App language changed to %s"
    override val notif_language_changed_auto : String = "App language changed successfully!"
    override val notif_theme_changed : String = "App theme changed successfully!"
    override val notif_theme_changed_auto : String = "App theme changed automatically!"
    override val notif_notifications_enabled : String = "Notifications successfully enabled!"
    override val notif_profile_changed : String = "Profile data changed successfully!"
    override val notif_update_title : String = "Flare Update"
}
