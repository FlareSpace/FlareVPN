package flare.client.app.ui.navigation

sealed class Destination(val route: String) {
    
    object Home : Destination("home")
    object Servers : Destination("servers")
    object Settings : Destination("settings")

    
    object AdvancedSettings : Destination("settings/advanced")
    object PingSettings : Destination("settings/ping")
    object RoutingSettings : Destination("settings/routing")
    object BasicSettings : Destination("settings/basic")
    object SubscriptionsSettings : Destination("settings/subscriptions")
    object ThemeSettings : Destination("settings/theme")
    object LanguageSettings : Destination("settings/language")

    
    object Journal : Destination("journal")
    
    
    object JsonEditor : Destination("editor/json/{id}/{type}") {
        fun createRoute(id: Long, type: String) = "editor/json/$id/$type"
        const val TYPE_PROFILE = "profile"
        const val TYPE_SUBSCRIPTION = "subscription"
    }
    
    object SimpleEditor : Destination("editor/simple/{id}") {
        fun createRoute(id: Long) = "editor/simple/$id"
    }
}
