package flare.client.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import flare.client.app.data.SettingsManager
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.manager.ThemeManager

class SettingsViewModel : ViewModel() {
    
    
    var composeBottomNavTabIndex by mutableStateOf(1)
    var composeBottomNavIsShrunk by mutableStateOf(false)
    var composeBottomNavIsShrunkToHome by mutableStateOf(false)
    var composeBottomNavIsVisible by mutableStateOf(true)
    var composeAppLanguage by mutableStateOf("auto")
    var composeIsSwipeDismissing by mutableStateOf(false)
    var isGestureNav by mutableStateOf(false)

    fun startSwipeDismiss() {
        composeIsSwipeDismissing = true
        viewModelScope.launch {
            delay(500)
            composeIsSwipeDismissing = false
        }
    }

    
    var composeThemeMode by mutableStateOf(0)
    var composeAppIcon by mutableStateOf("main")
    var composeAppearanceType by mutableStateOf(1)
    var composeSystemIsDark by mutableStateOf(false)
    var composeIsGradientEnabled by mutableStateOf(true)
    var composeBackgroundType by mutableStateOf(0)
    var composeIsAnimationEnabled by mutableStateOf(true)
    var composeGradientSpeed by mutableStateOf(1.0f)
    var composeIsBlurEnabled by mutableStateOf(true)
    var composeIsLiquidGlassEnabled by mutableStateOf(true)
    var composeIsCustomColorEnabled by mutableStateOf(false)
    var composeIsChangeLaunchButtonColorEnabled by mutableStateOf(false)
    var composeAccentColorKey by mutableStateOf("")
    var composeAccentColor by mutableStateOf(Color(ThemeManager.COLOR_DEFAULT))
    var composePhotoSeed by mutableStateOf("default_seed")
    var composeIsDownloadingPhoto by mutableStateOf(false)
    var composeFontFamily by mutableStateOf("geologica")
    
    
    var composeIsSplitTunnelingEnabled by mutableStateOf(false)
    var composeSplitTunnelingDesc by mutableStateOf("")
    var composeIsChangeAppsLoading by mutableStateOf(false)
    var composeIsAutostartEnabled by mutableStateOf(false)
    var composeIsStatusNotificationEnabled by mutableStateOf(false)
    var composeIsNotificationSpeedEnabled by mutableStateOf(true)
    var composeIsBestProfileNotifEnabled by mutableStateOf(false)
    var composeIsHwidEnabled by mutableStateOf(false)
    var composeIsOsSendEnabled by mutableStateOf(false)
    var composeIsModelSendEnabled by mutableStateOf(false)
    var composeIsCoreLogEnabled by mutableStateOf(false)
    var composeCoreLogLevel by mutableStateOf("warn")
    var composeIsBestProfileEnabled by mutableStateOf(false)
    var composeBestProfileInterval by mutableStateOf("1800")
    var composeIsBestProfileOnlyConnected by mutableStateOf(false)
    var composeIsAdaptiveTunnelEnabled by mutableStateOf(false)
    var composeIsUpdateCheckEnabled by mutableStateOf(true)
    var composeUpdateFrequency by mutableStateOf("weekly")

    
    var composeIsFragmentationEnabled by mutableStateOf(false)
    var composePacketType by mutableStateOf("disabled")
    var composeFragmentInterval by mutableStateOf("10")
    var composeIsMuxEnabled by mutableStateOf(false)
    var composeMuxProtocol by mutableStateOf("smux")
    var composeMuxMaxStreams by mutableStateOf("8")
    var composeMuxPadding by mutableStateOf(false)
    var composeRemoteDnsUrl by mutableStateOf("")
    var composeRemoteDnsMode by mutableStateOf("auto")
    var composeIsRemoteDnsDohEnabled by mutableStateOf(true)
    var composeIsFakeIpEnabled by mutableStateOf(false)
    var composeMtu by mutableStateOf("1500")
    var composeTunStack by mutableStateOf("system")
    var composeIsResetChainOnDisconnect by mutableStateOf(false)
    var composeIsTlsSpoofEnabled by mutableStateOf(false)
    var composeTlsSpoofDomain by mutableStateOf("google.com")
    var composeTlsSpoofMethod by mutableStateOf("wrong-ack")
    var composeFingerprint by mutableStateOf("auto")

    
    var composePingType by mutableStateOf("via proxy GET")
    var composePingTestUrl by mutableStateOf("http://cp.cloudflare.com/generate_204")
    var composePingStyle by mutableStateOf("time")
    var composePingTimeout by mutableStateOf(10)

    
    var composeIsSubIntervalEnabled by mutableStateOf(true)
    var composeIsSubAutoUpdateEnabled by mutableStateOf(false)
    var composeSubAutoUpdateInterval by mutableStateOf("3600")
    var composeSubUpdateTimeout by mutableStateOf(15)
    var composeSubUserAgent by mutableStateOf("Happ/3.21.1")

    fun syncAll(settings: SettingsManager) {
        syncBasic(settings)
        syncAdvanced(settings)
        syncPing(settings)
        syncSub(settings)
        syncTheme(settings)
    }

    fun syncBasic(settings: SettingsManager) {
        composeIsSplitTunnelingEnabled = settings.isSplitTunnelingEnabled
        
        composeIsAutostartEnabled = settings.isAutostartEnabled
        composeIsStatusNotificationEnabled = settings.isStatusNotificationEnabled
        composeIsNotificationSpeedEnabled = settings.isNotificationSpeedEnabled
        composeIsBestProfileNotifEnabled = settings.isBestProfileNotificationEnabled
        composeIsHwidEnabled = settings.isHwidEnabled
        composeIsOsSendEnabled = settings.isOsSendEnabled
        composeIsModelSendEnabled = settings.isModelSendEnabled
        composeIsCoreLogEnabled = settings.isCoreLogEnabled
        composeCoreLogLevel = settings.coreLogLevel
        composeIsBestProfileEnabled = settings.isBestProfileEnabled
        composeBestProfileInterval = settings.bestProfileInterval
        composeIsBestProfileOnlyConnected = settings.isBestProfileOnlyIfConnected
        composeIsAdaptiveTunnelEnabled = settings.isAdaptiveTunnelEnabled
        composeIsUpdateCheckEnabled = settings.isUpdateCheckEnabled
        composeUpdateFrequency = settings.updateCheckFrequency
        composeAppLanguage = settings.appLanguage
    }

    fun syncAdvanced(settings: SettingsManager) {
        composeIsFragmentationEnabled = settings.isFragmentationEnabled
        composePacketType = settings.packetType
        composeFragmentInterval = settings.fragmentInterval
        composeIsMuxEnabled = settings.isMuxEnabled
        composeMuxProtocol = settings.muxProtocol
        composeMuxMaxStreams = settings.muxMaxStreams
        composeMuxPadding = settings.muxPadding
        composeRemoteDnsUrl = settings.remoteDnsUrl
        composeRemoteDnsMode = settings.remoteDnsMode
        composeIsRemoteDnsDohEnabled = settings.isRemoteDnsDohEnabled
        composeIsFakeIpEnabled = settings.isFakeIpEnabled
        composeMtu = settings.mtu
        composeTunStack = settings.tunStack
        composeIsResetChainOnDisconnect = settings.isResetChainOnDisconnect
        composeIsTlsSpoofEnabled = settings.isTlsSpoofEnabled
        composeTlsSpoofDomain = settings.tlsSpoofDomain
        composeTlsSpoofMethod = settings.tlsSpoofMethod
        composeFingerprint = settings.fingerprint
    }

    fun syncPing(settings: SettingsManager) {
        composePingType = settings.pingType
        composePingTestUrl = settings.pingTestUrl
        composePingStyle = settings.pingStyle
        composePingTimeout = settings.pingTimeout
    }

    fun syncSub(settings: SettingsManager) {
        composeIsSubIntervalEnabled = settings.isSubIntervalEnabled
        composeIsSubAutoUpdateEnabled = settings.isSubAutoUpdateEnabled
        composeSubAutoUpdateInterval = settings.subAutoUpdateInterval
        composeSubUpdateTimeout = settings.subUpdateTimeout
        composeSubUserAgent = settings.subUserAgent
    }

    fun syncTheme(settings: SettingsManager) {
        composeThemeMode = settings.themeMode
        composeAppIcon = settings.appIcon
        composeAppearanceType = settings.appearanceType
        composeIsGradientEnabled = settings.isBackgroundGradientEnabled
        composeBackgroundType = settings.backgroundType
        composeIsAnimationEnabled = settings.isGradientAnimationEnabled
        composeGradientSpeed = settings.gradientAnimationSpeed
        composeIsBlurEnabled = settings.isBlurEnabled
        composeIsLiquidGlassEnabled = settings.isLiquidGlassEnabled
        composeIsCustomColorEnabled = settings.isCustomColorEnabled
        composeIsChangeLaunchButtonColorEnabled = settings.isChangeLaunchButtonColorEnabled
        composeAccentColorKey = settings.accentColorKey
        composePhotoSeed = settings.photoSeed
        composeFontFamily = settings.fontFamily
    }
}
