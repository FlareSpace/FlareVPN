package flare.client.app.ui.i18n

interface FlareStrings : 
    CommonStrings, 
    ErrorStrings, 
    OnboardingStrings, 
    SettingsStrings, 
    ServerStrings, 
    RoutingStrings, 
    NotificationStrings, 
    JournalStrings

object RuFlareStrings : FlareStrings,
    CommonStrings by RuCommonStrings,
    ErrorStrings by RuErrorStrings,
    OnboardingStrings by RuOnboardingStrings,
    SettingsStrings by RuSettingsStrings,
    ServerStrings by RuServerStrings,
    RoutingStrings by RuRoutingStrings,
    NotificationStrings by RuNotificationStrings,
    JournalStrings by RuJournalStrings

object EnFlareStrings : FlareStrings,
    CommonStrings by EnCommonStrings,
    ErrorStrings by EnErrorStrings,
    OnboardingStrings by EnOnboardingStrings,
    SettingsStrings by EnSettingsStrings,
    ServerStrings by EnServerStrings,
    RoutingStrings by EnRoutingStrings,
    NotificationStrings by EnNotificationStrings,
    JournalStrings by EnJournalStrings