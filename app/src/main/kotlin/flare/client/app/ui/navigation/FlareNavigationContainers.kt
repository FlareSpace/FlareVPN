package flare.client.app.ui.navigation

import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.chrisbanes.haze.hazeSource
import flare.client.app.ui.SettingsViewModel
import flare.client.app.ui.components.SwipeToDismissScreen

data class SettingsMorphRequest(
    val route: String,
    val originOffset: IntOffset,
    val originSize: IntSize
)

@Composable
fun TransitionBlurContainer(
    isActive: Boolean,
    enterBlur: Float,
    exitBlur: Float,
    enterDuration: Int,
    exitDuration: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val blur = remember { Animatable(if (isActive) enterBlur else exitBlur) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!initialized) {
            initialized = true
            if (isActive) {
                blur.snapTo(enterBlur)
                blur.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = enterDuration,
                        easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
                    )
                )
            } else {
                blur.snapTo(exitBlur)
            }
        } else if (isActive) {
            blur.snapTo(enterBlur)
            blur.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = enterDuration,
                    easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
                )
            )
        } else {
            blur.animateTo(
                targetValue = exitBlur,
                animationSpec = tween(
                    durationMillis = exitDuration,
                    easing = { DecelerateInterpolator(1.5f).getInterpolation(it) }
                )
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val radius = blur.value
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && radius > 0.5f) {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(radius, radius, android.graphics.Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                } else {
                    renderEffect = null
                }
            },
        content = content
    )
}

@Composable
fun SettingsDetailContainer(
    route: String,
    currentRoute: String?,
    morphRequest: SettingsMorphRequest?,
    onMorphFinished: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit = {},
    backgroundContentRight: (@Composable () -> Unit)? = null,
    onDismissLeft: (() -> Unit)? = null,
    backgroundContentLeft: (@Composable () -> Unit)? = null,
    appHazeState: dev.chrisbanes.haze.HazeState? = null,
    content: @Composable BoxScope.(dev.chrisbanes.haze.HazeState) -> Unit
) {
    val localHazeState = remember { dev.chrisbanes.haze.HazeState() }

    LaunchedEffect(morphRequest) {
        if (morphRequest != null) {
            onMorphFinished()
        }
    }
    SwipeToDismissScreen(
        onDismissRight = onBack,
        onDismissLeft = onDismissLeft,
        onSwipeDismissStart = { settingsViewModel.startSwipeDismiss() },
        backgroundContentRight = backgroundContentRight,
        backgroundContentLeft = backgroundContentLeft
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { if (appHazeState != null && flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) it.hazeSource(state = appHazeState) else it }
        ) {
            flare.client.app.ui.components.FlareHomeBackground(
                backgroundType = settingsViewModel.composeBackgroundType,
                isAnimationEnabled = false,
                animationSpeed = settingsViewModel.composeGradientSpeed,
                photoSeed = settingsViewModel.composePhotoSeed,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                    .let { if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) it.hazeSource(state = localHazeState) else it }
            )
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                content(localHazeState)
            }
        }
    }
}

@Composable
fun SettingsBackgroundContent(
    settingsViewModel: SettingsViewModel,
    sharedSettingsScrollState: androidx.compose.foundation.ScrollState,
    appHazeState: dev.chrisbanes.haze.HazeState
) {
    flare.client.app.ui.SettingsScreen(
        onBaseSettingsClick = {},
        onAdvancedSettingsClick = {},
        onRoutingSettingsClick = {},
        onPingSettingsClick = {},
        onSubscriptionsSettingsClick = {},
        onVpnSubscriptionClick = {},
        onThemeSettingsClick = {},
        onLanguageSettingsClick = {},
        isGradientEnabled = settingsViewModel.composeIsGradientEnabled,
        isAnimationEnabled = false,
        gradientSpeed = settingsViewModel.composeGradientSpeed,
        hazeState = appHazeState,
        scrollState = sharedSettingsScrollState
    )
}

@Composable
fun BasicSettingsBackgroundContent(
    settingsViewModel: SettingsViewModel,
    sharedBasicSettingsScrollState: androidx.compose.foundation.ScrollState,
    accentColor: () -> Int,
    appHazeState: dev.chrisbanes.haze.HazeState
) {
    flare.client.app.ui.BasicSettingsScreen(
        isSplitTunnelingEnabled = settingsViewModel.composeIsSplitTunnelingEnabled,
        onSplitTunnelingChange = {},
        splitTunnelingDesc = settingsViewModel.composeSplitTunnelingDesc,
        onChangeAppsClick = {},
        isChangeAppsLoading = settingsViewModel.composeIsChangeAppsLoading,
        isAutostartEnabled = settingsViewModel.composeIsAutostartEnabled,
        onAutostartChange = {},
        isStatusNotificationEnabled = settingsViewModel.composeIsStatusNotificationEnabled,
        onStatusNotificationChange = {},
        isNotificationSpeedEnabled = settingsViewModel.composeIsNotificationSpeedEnabled,
        onNotificationSpeedChange = {},
        isBestProfileNotifEnabled = settingsViewModel.composeIsBestProfileNotifEnabled,
        onBestProfileNotifChange = {},
        isCoreLogEnabled = settingsViewModel.composeIsCoreLogEnabled,
        onCoreLogChange = {},
        coreLogLevel = settingsViewModel.composeCoreLogLevel,
        onLogLevelClick = {},
        onViewJournalClick = {},
        isBestProfileEnabled = settingsViewModel.composeIsBestProfileEnabled,
        onBestProfileChange = {},
        bestProfileInterval = settingsViewModel.composeBestProfileInterval,
        onBestProfileIntervalChange = {},
        isBestProfileOnlyConnected = settingsViewModel.composeIsBestProfileOnlyConnected,
        onBestProfileOnlyConnectedClick = {},
        isAdaptiveTunnelEnabled = settingsViewModel.composeIsAdaptiveTunnelEnabled,
        onAdaptiveTunnelChange = {},
        isUpdateCheckEnabled = settingsViewModel.composeIsUpdateCheckEnabled,
        onUpdateCheckChange = {},
        updateFrequency = settingsViewModel.composeUpdateFrequency,
        onUpdateFrequencyClick = {},
        onDataManagementClick = {},
        scrollState = sharedBasicSettingsScrollState,
        accentColor = androidx.compose.ui.graphics.Color(accentColor()),
        onBack = {},
        hazeState = appHazeState
    )
}

@Composable
fun HomeBackgroundContent(
    vpnViewModel: flare.client.app.ui.viewmodel.VpnViewModel,
    profilesViewModel: flare.client.app.ui.viewmodel.ProfilesViewModel,
    settingsViewModel: SettingsViewModel,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    isClipboardLoading: () -> Boolean,
    isAnySubscriptionExpanded: () -> Boolean,
    accentColor: () -> Int,
    appHazeState: dev.chrisbanes.haze.HazeState
) {
    val connectionState by vpnViewModel.connectionState.collectAsState()
    val profiles by profilesViewModel.displayItems.collectAsState()
    val chainedProfileIds by vpnViewModel.chainedProfileIds.collectAsState()
    val backgroundListState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = homeListState.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = homeListState.firstVisibleItemScrollOffset
    )

    flare.client.app.ui.HomeScreen(
        connectionState = connectionState,
        profiles = profiles,
        chainedProfileIds = chainedProfileIds,
        onProfileChainToggle = { profile -> vpnViewModel.toggleProfileInChain(profile.id) },
        isClipboardLoading = isClipboardLoading(),
        isGestureNav = settingsViewModel.isGestureNav,
        isAnySubscriptionExpanded = isAnySubscriptionExpanded(),
        accentColor = accentColor(),
        pingStyle = settingsViewModel.composePingStyle,
        isGradientEnabled = settingsViewModel.composeIsGradientEnabled,
        backgroundType = settingsViewModel.composeBackgroundType,
        isAnimationEnabled = false,
        animationSpeed = settingsViewModel.composeGradientSpeed,
        isCustomColorEnabled = settingsViewModel.composeIsCustomColorEnabled,
        isChangeLaunchButtonColorEnabled = settingsViewModel.composeIsChangeLaunchButtonColorEnabled,
        listState = backgroundListState,
        onConnectClick = { vpnViewModel.connectOrDisconnect() },
        onProfileClick = { profile -> vpnViewModel.selectProfile(profile.id) },
        onProfileDelete = { profile -> profilesViewModel.deleteProfile(profile.id, profile.name) },
        onShareProfile = {},
        onQrProfile = {},
        onEditProfileJson = {},
        onEditProfileSimple = {},
        onProfileTest = {},
        onSubscriptionToggle = { sub -> profilesViewModel.toggleSubscriptionExpanded(sub.id) },
        onSubscriptionDelete = { id -> profilesViewModel.deleteSubscription(id) },
        onSubscriptionSpeedTest = { id -> profilesViewModel.speedTestSubscription(id) },
        onSubscriptionUpdate = { sub -> profilesViewModel.refreshSubscription(sub) },
        onEditSubscriptionJson = {},
        onSubscriptionPinToggle = { sub -> profilesViewModel.toggleSubscriptionPinned(sub.id) },
        onSubscriptionShare = {},
        onSubscriptionQr = {},
        onSubscriptionMerge = { id -> profilesViewModel.addSubscriptionToMerged(id) },
        onClipboardClick = {},
        onManualInputClick = {},
        onQrScanClick = {},
        onImportFileClick = {},
        onBack = {},
        onScroll = {},
        hazeState = appHazeState
    )
}

