package flare.client.app.ui

import okhttp3.OkHttpClient
import okhttp3.Request
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import flare.client.app.ui.navigation.Destination
import flare.client.app.ui.components.FlareBottomNav
import flare.client.app.ui.components.FlareSideNav
import flare.client.app.ui.components.FlareHomeBackground
import flare.client.app.ui.MainViewModel
import flare.client.app.ui.SettingsViewModel
import flare.client.app.ui.WizardViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import flare.client.app.ui.components.JournalScreen
import flare.client.app.ui.components.SwipeToDismissScreen
import flare.client.app.ui.HomeScreen
import flare.client.app.ui.ServersScreen
import flare.client.app.ui.SettingsScreen
import flare.client.app.ui.BasicSettingsScreen
import flare.client.app.ui.AdvancedSettingsScreen
import flare.client.app.ui.PingSettingsScreen
import flare.client.app.ui.RoutingScreen
import flare.client.app.ui.SubscriptionsScreen
import flare.client.app.ui.ThemeSettingsScreen
import flare.client.app.ui.LanguageSettingsScreen
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.notification.NotificationType
import flare.client.app.ui.components.ProfileJsonEditor
import flare.client.app.ui.components.ProfileSimpleEditor
import flare.client.app.ui.notification.ComposeNotificationHost
import flare.client.app.ui.components.dialogs.DataManagementDialog
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.R
import flare.client.app.data.SettingsManager
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val ROOT_TAB_EXIT_DURATION = 220
private const val ROOT_TAB_ENTER_DURATION = 260
private const val SETTINGS_EXIT_DURATION = 220
private const val SETTINGS_ENTER_DURATION = 220
private const val ROOT_TAB_BLUR = 0f
private const val SETTINGS_BLUR = 25f
private const val MORPH_DURATION = 450



private fun rootTabIndexForRoute(route: String?): Int? = when {
    route == Destination.Settings.route || route?.startsWith("settings/") == true -> 0
    route == Destination.Home.route -> 1
    route == Destination.Servers.route -> 2
    else -> null
}

private fun isSettingsDetailRoute(route: String?): Boolean = when (route) {
    Destination.AdvancedSettings.route,
    Destination.PingSettings.route,
    Destination.RoutingSettings.route,
    Destination.BasicSettings.route,
    Destination.SubscriptionsSettings.route,
    Destination.ThemeSettings.route,
    Destination.LanguageSettings.route,
    Destination.Journal.route -> true
    else -> false
}

private fun isSettingsRoute(route: String?): Boolean =
    route == Destination.Settings.route || isSettingsDetailRoute(route)

private fun isEditorRoute(route: String?): Boolean =
    route?.startsWith("editor/") == true

private fun NavBackStackEntry.route(): String? = destination.route

private fun rootTabEnterTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
    isLandscape: Boolean
): EnterTransition {
    val fromIndex = rootTabIndexForRoute(initial.route()) ?: return EnterTransition.None
    val toIndex = rootTabIndexForRoute(target.route()) ?: return EnterTransition.None
    if (fromIndex == toIndex) return EnterTransition.None

    val direction = if (toIndex > fromIndex) 1 else -1
    return if (isLandscape) {
        slideInVertically(
            animationSpec = tween(
                durationMillis = ROOT_TAB_ENTER_DURATION,
                easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
            ),
            initialOffsetY = { fullHeight -> fullHeight * direction }
        ) + fadeIn(animationSpec = tween(durationMillis = ROOT_TAB_ENTER_DURATION))
    } else {
        slideInHorizontally(
            animationSpec = tween(
                durationMillis = ROOT_TAB_ENTER_DURATION,
                easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
            ),
            initialOffsetX = { fullWidth -> fullWidth * direction }
        ) + fadeIn(animationSpec = tween(durationMillis = ROOT_TAB_ENTER_DURATION))
    }
}

private fun rootTabExitTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
    isLandscape: Boolean
): ExitTransition {
    val fromIndex = rootTabIndexForRoute(initial.route()) ?: return ExitTransition.None
    val toIndex = rootTabIndexForRoute(target.route()) ?: return ExitTransition.None
    if (fromIndex == toIndex) return ExitTransition.None

    val direction = if (toIndex > fromIndex) -1 else 1
    return if (isLandscape) {
        slideOutVertically(
            animationSpec = tween(
                durationMillis = ROOT_TAB_EXIT_DURATION,
                easing = { DecelerateInterpolator(1.5f).getInterpolation(it) }
            ),
            targetOffsetY = { fullHeight -> fullHeight * direction }
        ) + fadeOut(animationSpec = tween(durationMillis = ROOT_TAB_EXIT_DURATION))
    } else {
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = ROOT_TAB_EXIT_DURATION,
                easing = { DecelerateInterpolator(1.5f).getInterpolation(it) }
            ),
            targetOffsetX = { fullWidth -> fullWidth * direction }
        ) + fadeOut(animationSpec = tween(durationMillis = ROOT_TAB_EXIT_DURATION))
    }
}

private fun settingsForwardEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = SETTINGS_ENTER_DURATION,
            easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
        )
    ) + fadeIn(animationSpec = tween(durationMillis = SETTINGS_ENTER_DURATION))

private fun settingsForwardExitTransition(): ExitTransition = ExitTransition.None

private fun settingsBackEnterTransition(): EnterTransition = EnterTransition.None

private fun settingsBackExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = SETTINGS_EXIT_DURATION,
            easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
        )
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(
            durationMillis = SETTINGS_EXIT_DURATION,
            easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
        )
    ) + fadeOut(animationSpec = tween(durationMillis = SETTINGS_EXIT_DURATION))

private data class SettingsMorphRequest(
    val route: String,
    val originOffset: IntOffset,
    val originSize: IntSize
)

@Composable
private fun TransitionBlurContainer(
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
private fun SettingsDetailContainer(
    route: String,
    currentRoute: String?,
    morphRequest: SettingsMorphRequest?,
    onMorphFinished: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit = {},
    backgroundContentRight: (@Composable () -> Unit)? = null,
    onDismissLeft: (() -> Unit)? = null,
    backgroundContentLeft: (@Composable () -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    content: @Composable BoxScope.() -> Unit
) {
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
            modifier = Modifier.fillMaxSize()
        ) {
            flare.client.app.ui.components.FlareHomeBackground(
                backgroundType = settingsViewModel.composeBackgroundType,
                isAnimationEnabled = false,
                animationSpeed = settingsViewModel.composeGradientSpeed,
                photoSeed = settingsViewModel.composePhotoSeed,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                    .let { if (hazeState != null) it.hazeSource(state = hazeState) else it }
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        }
    }
}

@Composable
fun FlareApp(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    wizardViewModel: WizardViewModel,
    accentColor: Int,
    accentEndColor: Int,
    onManualInputClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onShareProfile: (flare.client.app.data.model.ProfileSummary) -> Unit,
    onQrProfile: (flare.client.app.data.model.ProfileSummary) -> Unit,
    onShareSubscription: (flare.client.app.data.model.SubscriptionEntity) -> Unit,
    onQrSubscription: (flare.client.app.data.model.SubscriptionEntity) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onLogLevelClick: (String) -> Unit,
    onUpdateFrequencyClick: (String) -> Unit,
    onBestProfileOnlyConnectedClick: (Boolean) -> Unit,
    onUserAgentClick: (String) -> Unit,
    onRoutingModeClick: (String, String) -> Unit,
    onPacketTypeClick: (String) -> Unit,
    onMuxProtocolClick: (String) -> Unit,
    onMuxPaddingClick: (Boolean) -> Unit,
    onTunStackClick: (String) -> Unit,
    onPingStyleClick: (String) -> Unit,
    onThemeClick: (Int) -> Unit,
    onEditSubscriptionClick: (flare.client.app.data.model.SubscriptionEntity) -> Unit,
    onChangeAppsClick: () -> Unit,
    onViewJournalClick: (android.view.View) -> Unit,
    onClipboardClick: () -> Unit,
    isClipboardLoading: Boolean = false,
    showBottomNav: Boolean = true,
    requestedRootTabIndex: Int? = null,
    requestedRootTabNonce: Long = 0L,
    onRootTabRequestHandled: () -> Unit = {},
    onSelectedRootTabChanged: (Int) -> Unit = {},
    onDataManagementClick: () -> Unit,
    settings: SettingsManager,
    onRestartRequired: () -> Unit,
    isDark: Boolean = false,
    appHazeState: dev.chrisbanes.haze.HazeState
) {
    FlareTheme(
        isDark = isDark,
        accentColor = Color(accentColor),
        accentEndColor = Color(accentEndColor)
    ) {
        val navController = rememberNavController()
        val rootPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
        val coroutineScope = rememberCoroutineScope()
        val rootView = LocalView.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val context = LocalContext.current
        var isGestureNav by remember { mutableStateOf(false) }

        DisposableEffect(context) {
            fun checkGestureNav(): Boolean {
                var gestureEnabled = false
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        val mode = android.provider.Settings.Secure.getInt(
                            context.contentResolver,
                            "navigation_mode"
                        )
                        gestureEnabled = (mode == 2)
                    } catch (_: Exception) {}
                }
                if (!gestureEnabled) {
                    try {
                        val resources = context.resources
                        val resourceId = resources.getIdentifier(
                            "config_navBarInteractionMode",
                            "integer",
                            "android"
                        )
                        if (resourceId > 0) {
                            gestureEnabled = (resources.getInteger(resourceId) == 2)
                        }
                    } catch (_: Exception) {}
                }
                return gestureEnabled
            }

            isGestureNav = checkGestureNav()

            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    isGestureNav = checkGestureNav()
                }
            }

            try {
                context.contentResolver.registerContentObserver(
                    android.provider.Settings.Secure.getUriFor("navigation_mode"),
                    false,
                    observer
                )
            } catch (_: Exception) {}

            onDispose {
                try {
                    context.contentResolver.unregisterContentObserver(observer)
                } catch (_: Exception) {}
            }
        }

        val bottomPadding = if (isGestureNav) 22.dp else 38.dp
        val isAnySubscriptionExpanded by mainViewModel.isAnySubscriptionExpanded.collectAsState()
        var pendingSettingsMorph by remember { mutableStateOf<SettingsMorphRequest?>(null) }
        var showDataManagementDialog by remember { mutableStateOf(false) }
        val sharedBasicSettingsScrollState = androidx.compose.foundation.rememberScrollState()
        val homeListState = rememberLazyListState()

        
        
        val settingsBackground: @Composable () -> Unit = {
            SettingsScreen(
                onBaseSettingsClick = {},
                onAdvancedSettingsClick = {},
                onRoutingSettingsClick = {},
                onPingSettingsClick = {},
                onSubscriptionsSettingsClick = {},
                onThemeSettingsClick = {},
                onLanguageSettingsClick = {},
                isGradientEnabled = settingsViewModel.composeIsGradientEnabled,
                isAnimationEnabled = false,
                gradientSpeed = settingsViewModel.composeGradientSpeed,
                hazeState = appHazeState
            )
        }

        val basicSettingsBackground: @Composable () -> Unit = {
            BasicSettingsScreen(
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
                accentColor = Color(accentColor),
                onBack = {},
                hazeState = appHazeState
            )
        }

        val homeBackgroundContent: @Composable () -> Unit = {
            val connectionState by mainViewModel.connectionState.collectAsState()
            val profiles by mainViewModel.displayItems.collectAsState()
            val chainedProfileIds by mainViewModel.chainedProfileIds.collectAsState()
            val backgroundListState = rememberLazyListState(
                initialFirstVisibleItemIndex = homeListState.firstVisibleItemIndex,
                initialFirstVisibleItemScrollOffset = homeListState.firstVisibleItemScrollOffset
            )

            HomeScreen(
                connectionState = connectionState,
                profiles = profiles,
                chainedProfileIds = chainedProfileIds,
                onProfileChainToggle = {},
                isClipboardLoading = isClipboardLoading,
                isAnySubscriptionExpanded = isAnySubscriptionExpanded,
                accentColor = accentColor,
                pingStyle = settingsViewModel.composePingStyle,
                isGradientEnabled = settingsViewModel.composeIsGradientEnabled,
                backgroundType = settingsViewModel.composeBackgroundType,
                isAnimationEnabled = false,
                animationSpeed = settingsViewModel.composeGradientSpeed,
                isCustomColorEnabled = settingsViewModel.composeIsCustomColorEnabled,
                listState = backgroundListState,
                onConnectClick = {},
                onProfileClick = {},
                onProfileDelete = {},
                onShareProfile = {},
                onQrProfile = {},
                onEditProfileJson = {},
                onEditProfileSimple = {},
                onSubscriptionToggle = {},
                onSubscriptionDelete = {},
                onSubscriptionSpeedTest = {},
                onSubscriptionUpdate = {},
                onEditSubscriptionJson = {},
                onSubscriptionPinToggle = {},
                onSubscriptionShare = {},
                onSubscriptionQr = {},
                onClipboardClick = {},
                onManualInputClick = {},
                onQrScanClick = {},
                onImportFileClick = {},
                onBack = {},
                onScroll = {},
                hazeState = appHazeState
            )
        }


    LaunchedEffect(currentRoute, rootPagerState.currentPage, wizardViewModel.composeWizardStep) {
        val isOnServersScreen = currentRoute == Destination.Home.route && rootPagerState.currentPage == 2
        if (!isOnServersScreen) {
            settingsViewModel.composeBottomNavIsShrunk = false
            settingsViewModel.composeBottomNavIsShrunkToHome = false
        } else {
            settingsViewModel.composeBottomNavIsShrunkToHome = false
            if (wizardViewModel.composeWizardStep != WizardStep.CARDS) {
                settingsViewModel.composeBottomNavIsShrunk = true
            }
        }
    }


    
        val selectedIndex = if (currentRoute == Destination.Home.route) {
            rootPagerState.currentPage
        } else if (isSettingsDetailRoute(currentRoute)) {
            0
        } else {
            1
        }

    
    val isBottomNavVisible = when (currentRoute) {
        Destination.Home.route -> {
            if (rootPagerState.currentPage == 2) {
                when (wizardViewModel.composeWizardStep) {
                    WizardStep.CARDS -> true
                    WizardStep.SSH_CONFIG -> wizardViewModel.isSshConfigValid
                    WizardStep.PROTOCOL -> true
                    WizardStep.XRAY_CONFIG -> wizardViewModel.isXrayConfigValid
                    WizardStep.PROGRESS -> wizardViewModel.composeSetupProgress >= 100f
                    WizardStep.SUCCESS -> false
                    WizardStep.FLARE_TARIFFS -> wizardViewModel.composeSelectedTariff == TariffType.FREE
                    WizardStep.FLARE_PROGRESS -> false
                    WizardStep.FLARE_SUCCESS -> false
                }
            } else {
                true
            }
        }
        Destination.AdvancedSettings.route, Destination.PingSettings.route, 
        Destination.RoutingSettings.route, Destination.BasicSettings.route,
        Destination.SubscriptionsSettings.route, Destination.ThemeSettings.route,
        Destination.LanguageSettings.route -> true
        Destination.JsonEditor.route, Destination.SimpleEditor.route -> false
        else -> settingsViewModel.composeBottomNavIsVisible
    }

    
        LaunchedEffect(isBottomNavVisible) {
            settingsViewModel.composeBottomNavIsVisible = isBottomNavVisible
        }

        LaunchedEffect(selectedIndex) {
            onSelectedRootTabChanged(selectedIndex)
        }

        fun rememberMorphRequestFor(view: android.view.View, route: String): SettingsMorphRequest {
            val viewLocation = IntArray(2)
            view.getLocationInWindow(viewLocation)
            val rootLocation = IntArray(2)
            rootView.getLocationInWindow(rootLocation)
            return SettingsMorphRequest(
                route = route,
                originOffset = IntOffset(
                    x = viewLocation[0] - rootLocation[0],
                    y = viewLocation[1] - rootLocation[1]
                ),
                originSize = IntSize(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1))
            )
        }

        fun navigateToSettingsDetail(route: String, anchorView: android.view.View? = null) {
            pendingSettingsMorph = anchorView?.let { rememberMorphRequestFor(it, route) }
            navController.navigate(route)
        }

        LaunchedEffect(requestedRootTabNonce) {
            val requestedIndex = requestedRootTabIndex ?: return@LaunchedEffect
            if (currentRoute != Destination.Home.route) {
                navController.popBackStack(Destination.Home.route, inclusive = false)
            }
            rootPagerState.scrollToPage(requestedIndex)
            onRootTabRequestHandled()
        }

    Box(modifier = Modifier.fillMaxSize()) {
        FlareHomeBackground(
            backgroundType = settingsViewModel.composeBackgroundType,
            isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && (currentRoute == Destination.Home.route && (rootPagerState.currentPage == 1 || rootPagerState.currentPage == 2)),
            animationSpeed = settingsViewModel.composeGradientSpeed,
            photoSeed = settingsViewModel.composePhotoSeed,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .hazeSource(state = appHazeState)
        )

        val contentPaddingStart = if (isLandscape && isBottomNavVisible) 72.dp else 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = Destination.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = contentPaddingStart),
                enterTransition = {
                    when {
                        targetState.destination.route?.let(::isSettingsDetailRoute) == true ->
                            settingsForwardEnterTransition()
                        targetState.destination.route?.let(::isEditorRoute) == true ->
                            settingsForwardEnterTransition()
                        else -> EnterTransition.None
                    }
                },
                exitTransition = {
                    when {
                        targetState.destination.route?.let(::isSettingsDetailRoute) == true ->
                            settingsForwardExitTransition()
                        targetState.destination.route?.let(::isEditorRoute) == true ->
                            settingsForwardExitTransition()
                        else -> ExitTransition.None
                    }
                },
                popEnterTransition = {
                    when {
                        initialState.destination.route?.let(::isSettingsDetailRoute) == true -> {
                            if (settingsViewModel.composeIsSwipeDismissing) {
                                androidx.compose.animation.fadeIn(
                                    initialAlpha = 1f,
                                    animationSpec = androidx.compose.animation.core.tween(100)
                                )
                            } else {
                                settingsBackEnterTransition()
                            }
                        }
                        initialState.destination.route?.let(::isEditorRoute) == true -> {
                            if (settingsViewModel.composeIsSwipeDismissing) {
                                androidx.compose.animation.fadeIn(
                                    initialAlpha = 1f,
                                    animationSpec = androidx.compose.animation.core.tween(100)
                                )
                            } else {
                                settingsBackEnterTransition()
                            }
                        }
                        else -> EnterTransition.None
                    }
                },
                popExitTransition = {
                    when {
                        initialState.destination.route?.let(::isSettingsDetailRoute) == true -> {
                            if (settingsViewModel.composeIsSwipeDismissing) {
                                androidx.compose.animation.fadeOut(
                                    targetAlpha = 1f,
                                    animationSpec = androidx.compose.animation.core.tween(100)
                                )
                            } else {
                                settingsBackExitTransition()
                            }
                        }
                        initialState.destination.route?.let(::isEditorRoute) == true -> {
                            if (settingsViewModel.composeIsSwipeDismissing) {
                                androidx.compose.animation.fadeOut(
                                    targetAlpha = 1f,
                                    animationSpec = androidx.compose.animation.core.tween(100)
                                )
                            } else {
                                settingsBackExitTransition()
                            }
                        }
                        else -> ExitTransition.None
                    }
                }
            ) {
                composable(Destination.Home.route) {
                    TransitionBlurContainer(
                        isActive = currentRoute == Destination.Home.route,
                        enterBlur = ROOT_TAB_BLUR,
                        exitBlur = ROOT_TAB_BLUR,
                        enterDuration = ROOT_TAB_ENTER_DURATION,
                        exitDuration = ROOT_TAB_EXIT_DURATION
                    ) {
                        HorizontalPager(
                            state = rootPagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true
                        ) { page ->
                            when (page) {
                                0 -> {
                                    SettingsScreen(
                                        onBaseSettingsClick = { navigateToSettingsDetail(Destination.BasicSettings.route, it) },
                                        onAdvancedSettingsClick = { navigateToSettingsDetail(Destination.AdvancedSettings.route, it) },
                                        onRoutingSettingsClick = { navigateToSettingsDetail(Destination.RoutingSettings.route, it) },
                                        onPingSettingsClick = { navigateToSettingsDetail(Destination.PingSettings.route, it) },
                                        onSubscriptionsSettingsClick = { navigateToSettingsDetail(Destination.SubscriptionsSettings.route, it) },
                                        onThemeSettingsClick = { navigateToSettingsDetail(Destination.ThemeSettings.route, it) },
                                        onLanguageSettingsClick = { navigateToSettingsDetail(Destination.LanguageSettings.route, it) },
                                        isGradientEnabled = settingsViewModel.composeIsGradientEnabled,
                                        isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && rootPagerState.currentPage == 0,
                                        gradientSpeed = settingsViewModel.composeGradientSpeed,
                                        hazeState = appHazeState
                                    )
                                }
                                1 -> {
                                    val connectionState by mainViewModel.connectionState.collectAsState()
                                    val profiles by mainViewModel.displayItems.collectAsState()
                                    val chainedProfileIds by mainViewModel.chainedProfileIds.collectAsState()

                                    HomeScreen(
                                        connectionState = connectionState,
                                        profiles = profiles,
                                        chainedProfileIds = chainedProfileIds,
                                        onProfileChainToggle = { profile -> mainViewModel.toggleProfileInChain(profile.id) },
                                        isClipboardLoading = isClipboardLoading,
                                        isAnySubscriptionExpanded = isAnySubscriptionExpanded,
                                        accentColor = accentColor,
                                        pingStyle = settingsViewModel.composePingStyle,
                                        isGradientEnabled = settingsViewModel.composeIsGradientEnabled,
                                        backgroundType = settingsViewModel.composeBackgroundType,
                                        isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && rootPagerState.currentPage == 1,
                                        animationSpeed = settingsViewModel.composeGradientSpeed,
                                        isCustomColorEnabled = settingsViewModel.composeIsCustomColorEnabled,
                                        listState = homeListState,
                                        onConnectClick = { mainViewModel.connectOrDisconnect() },
                                        onProfileClick = { profile -> mainViewModel.selectProfile(profile.id) },
                                        onProfileDelete = { profile -> mainViewModel.deleteProfile(profile.id, profile.name) },
                                        onShareProfile = onShareProfile,
                                        onQrProfile = onQrProfile,
                                        onEditProfileJson = { profile ->
                                            mainViewModel.setEditingProfile(null)
                                            navController.navigate(Destination.JsonEditor.createRoute(profile.id, Destination.JsonEditor.TYPE_PROFILE))
                                        },
                                        onEditProfileSimple = { profile ->
                                            mainViewModel.setEditingProfile(null)
                                            navController.navigate(Destination.SimpleEditor.createRoute(profile.id))
                                        },
                                        onSubscriptionToggle = { sub -> mainViewModel.toggleSubscriptionExpanded(sub.id) },
                                        onSubscriptionDelete = { id -> mainViewModel.deleteSubscription(id) },
                                        onSubscriptionSpeedTest = { id -> mainViewModel.speedTestSubscription(id) },
                                        onSubscriptionUpdate = { sub -> mainViewModel.refreshSubscription(sub) },
                                        onEditSubscriptionJson = { sub -> onEditSubscriptionClick(sub) },
                                        onSubscriptionPinToggle = { sub -> mainViewModel.toggleSubscriptionPinned(sub.id) },
                                        onSubscriptionShare = onShareSubscription,
                                        onSubscriptionQr = onQrSubscription,
                                        onClipboardClick = onClipboardClick,
                                        onManualInputClick = onManualInputClick,
                                        onQrScanClick = onQrScanClick,
                                        onImportFileClick = onImportFileClick,
                                        onBack = { mainViewModel.collapseAllSubscriptions() },
                                        onScroll = {  },
                                        hazeState = appHazeState
                                    )
                                }
                                2 -> {
                                    ServersScreen(
                                        currentStep = wizardViewModel.composeWizardStep,
                                        selectedServerType = wizardViewModel.composeSelectedServerType,
                                        accentColor = Color(accentColor),
                                        isFreeSuccess = wizardViewModel.composeFreeSubscriptionSuccess,
                                        onFlareServersClick = { 
                                            val wasSelected = wizardViewModel.composeSelectedServerType == ServerType.FLARE
                                            wizardViewModel.composeSelectedServerType = if (wasSelected) null else ServerType.FLARE 
                                            settingsViewModel.composeBottomNavIsShrunk = if (wasSelected) !settingsViewModel.composeBottomNavIsShrunk else true
                                        },
                                        onCreateServerClick = { 
                                            val wasSelected = wizardViewModel.composeSelectedServerType == ServerType.CUSTOM
                                            wizardViewModel.composeSelectedServerType = if (wasSelected) null else ServerType.CUSTOM 
                                            settingsViewModel.composeBottomNavIsShrunk = if (wasSelected) !settingsViewModel.composeBottomNavIsShrunk else true
                                        },
                                        selectedTariff = wizardViewModel.composeSelectedTariff,
                                        onTariffSelect = { wizardViewModel.composeSelectedTariff = it },
                                        sshProfileName = wizardViewModel.composeSshProfileName,
                                        onSshProfileNameChange = { wizardViewModel.composeSshProfileName = it },
                                        sshIp = wizardViewModel.composeSshIp,
                                        onSshIpChange = { wizardViewModel.composeSshIp = it },
                                        sshPort = wizardViewModel.composeSshPort,
                                        onSshPortChange = { wizardViewModel.composeSshPort = it },
                                        sshUser = wizardViewModel.composeSshUser,
                                        onSshUserChange = { wizardViewModel.composeSshUser = it },
                                        sshPass = wizardViewModel.composeSshPassword,
                                        onSshPassChange = { wizardViewModel.composeSshPassword = it },
                                        onSshKeyClick = {  },
                                        selectedProtocol = wizardViewModel.composeSelectedProtocol,
                                        onProtocolXrayClick = { wizardViewModel.composeSelectedProtocol = SelectedProtocol.XRAY },
                                        onProtocolHysteria2Click = { wizardViewModel.composeSelectedProtocol = SelectedProtocol.HYSTERIA2 },
                                        onProtocolShadowsocksClick = { wizardViewModel.composeSelectedProtocol = SelectedProtocol.SHADOWSOCKS },
                                        onProtocolWireGuardClick = { wizardViewModel.composeSelectedProtocol = SelectedProtocol.WIREGUARD },
                                        xrayPort = wizardViewModel.composeXrayPort,
                                        onXrayPortChange = { wizardViewModel.composeXrayPort = it },
                                        xraySni = wizardViewModel.composeXraySni,
                                        onXraySniChange = { wizardViewModel.composeXraySni = it },
                                        obfsPassword = wizardViewModel.composeXrayObfsPassword,
                                        onObfsPasswordChange = { wizardViewModel.composeXrayObfsPassword = it },
                                        portHoppingEnabled = wizardViewModel.composeXrayPortHoppingEnabled,
                                        onPortHoppingEnabledChange = { wizardViewModel.composeXrayPortHoppingEnabled = it },
                                        portHoppingValue = wizardViewModel.composeXrayPortHoppingValue,
                                        onPortHoppingValueChange = { wizardViewModel.composeXrayPortHoppingValue = it },
                                        setupStatus = wizardViewModel.composeSetupStatus,
                                        setupProgress = wizardViewModel.composeSetupProgress,
                                        setupError = wizardViewModel.composeSetupError,
                                        onGoHomeClick = { 
                                            wizardViewModel.reset()
                                            coroutineScope.launch { rootPagerState.animateScrollToPage(1) }
                                        },
                                        onBack = {
                                            if (wizardViewModel.composeWizardStep == WizardStep.SUCCESS || wizardViewModel.composeWizardStep == WizardStep.FLARE_SUCCESS) {
                                                wizardViewModel.reset()
                                                settingsViewModel.composeBottomNavIsShrunk = false
                                            } else if (wizardViewModel.composeWizardStep != WizardStep.CARDS) {
                                                wizardViewModel.previousStep()
                                                if (wizardViewModel.composeWizardStep == WizardStep.CARDS) {
                                                    settingsViewModel.composeBottomNavIsShrunk = false
                                                    wizardViewModel.composeSelectedServerType = null
                                                    wizardViewModel.composeSelectedTariff = null
                                                }
                                            } else if (wizardViewModel.composeSelectedServerType != null) {
                                                wizardViewModel.composeSelectedServerType = null
                                                settingsViewModel.composeBottomNavIsShrunk = false
                                                wizardViewModel.composeSelectedTariff = null
                                            }
                                        },
                                        onNextClick = { wizardViewModel.nextStep() },
                                        isSshConfigValid = wizardViewModel.isSshConfigValid,
                                        hazeState = appHazeState
                                    )
                                }
                            }
                        }
                    }
                }

                composable(Destination.BasicSettings.route) {
                SettingsDetailContainer(
                    route = Destination.BasicSettings.route,
                    currentRoute = currentRoute,
                    morphRequest = pendingSettingsMorph,
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = settingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                    BasicSettingsScreen(
                        isSplitTunnelingEnabled = settingsViewModel.composeIsSplitTunnelingEnabled,
                        onSplitTunnelingChange = {
                            settings.isSplitTunnelingEnabled = it
                            settingsViewModel.composeIsSplitTunnelingEnabled = it
                            onRestartRequired()
                        },
                        splitTunnelingDesc = settingsViewModel.composeSplitTunnelingDesc,
                        onChangeAppsClick = onChangeAppsClick,
                        isChangeAppsLoading = settingsViewModel.composeIsChangeAppsLoading,                        isAutostartEnabled = settingsViewModel.composeIsAutostartEnabled,
                        onAutostartChange = {
                            settings.isAutostartEnabled = it
                            settingsViewModel.composeIsAutostartEnabled = it
                        },
                        isStatusNotificationEnabled = settingsViewModel.composeIsStatusNotificationEnabled,
                        onStatusNotificationChange = {
                            settings.isStatusNotificationEnabled = it
                            settingsViewModel.composeIsStatusNotificationEnabled = it
                            if (it) {
                                AppNotificationManager.showNotification(
                                    NotificationType.SUCCESS,
                                    I18n.strings.notif_notifications_enabled,
                                    3
                                )
                            }
                        },
                        isNotificationSpeedEnabled = settingsViewModel.composeIsNotificationSpeedEnabled,
                        onNotificationSpeedChange = {
                            settings.isNotificationSpeedEnabled = it
                            settingsViewModel.composeIsNotificationSpeedEnabled = it
                        },
                        isBestProfileNotifEnabled = settingsViewModel.composeIsBestProfileNotifEnabled,
                        onBestProfileNotifChange = {
                            settings.isBestProfileNotificationEnabled = it
                            settingsViewModel.composeIsBestProfileNotifEnabled = it
                        },
                        isCoreLogEnabled = settingsViewModel.composeIsCoreLogEnabled,
                        onCoreLogChange = {
                            settings.isCoreLogEnabled = it
                            settingsViewModel.composeIsCoreLogEnabled = it
                            onRestartRequired()
                        },
                        coreLogLevel = settingsViewModel.composeCoreLogLevel,
                        onLogLevelClick = onLogLevelClick,
                        onViewJournalClick = {
                            navController.navigate(Destination.Journal.route)
                        },
                        isBestProfileEnabled = settingsViewModel.composeIsBestProfileEnabled,
                        onBestProfileChange = {
                            settings.isBestProfileEnabled = it
                            settingsViewModel.composeIsBestProfileEnabled = it
                        },
                        bestProfileInterval = settingsViewModel.composeBestProfileInterval,
                        onBestProfileIntervalChange = {
                            settings.bestProfileInterval = it
                            settingsViewModel.composeBestProfileInterval = it
                        },
                        isBestProfileOnlyConnected = settingsViewModel.composeIsBestProfileOnlyConnected,
                        onBestProfileOnlyConnectedClick = onBestProfileOnlyConnectedClick,
                        isAdaptiveTunnelEnabled = settingsViewModel.composeIsAdaptiveTunnelEnabled,
                        onAdaptiveTunnelChange = {
                            settings.isAdaptiveTunnelEnabled = it
                            settingsViewModel.composeIsAdaptiveTunnelEnabled = it
                        },
                        isUpdateCheckEnabled = settingsViewModel.composeIsUpdateCheckEnabled,
                        onUpdateCheckChange = {
                            settings.isUpdateCheckEnabled = it
                            settingsViewModel.composeIsUpdateCheckEnabled = it
                        },
                        updateFrequency = settingsViewModel.composeUpdateFrequency,
                        onUpdateFrequencyClick = onUpdateFrequencyClick,
                        onDataManagementClick = { showDataManagementDialog = true },
                        scrollState = sharedBasicSettingsScrollState,
                        accentColor = Color(accentColor),
                        onBack = { navController.popBackStack() },
                        hazeState = appHazeState
                    )
                }
            }

            composable(Destination.AdvancedSettings.route) {
                SettingsDetailContainer(
                    route = Destination.AdvancedSettings.route,
                    currentRoute = currentRoute,
                    morphRequest = pendingSettingsMorph,
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = settingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                    AdvancedSettingsScreen(
                        isFragmentationEnabled = settingsViewModel.composeIsFragmentationEnabled,
                        onFragmentationChange = {
                            settings.isFragmentationEnabled = it
                            settingsViewModel.composeIsFragmentationEnabled = it
                            onRestartRequired()
                        },
                        packetType = settingsViewModel.composePacketType,
                        onPacketTypeClick = onPacketTypeClick,
                        fragmentInterval = settingsViewModel.composeFragmentInterval,
                        onFragmentIntervalChange = {
                            settings.fragmentInterval = it
                            settingsViewModel.composeFragmentInterval = it
                            onRestartRequired()
                        },
                        isMuxEnabled = settingsViewModel.composeIsMuxEnabled,
                        onMuxChange = {
                            settings.isMuxEnabled = it
                            settingsViewModel.composeIsMuxEnabled = it
                            onRestartRequired()
                        },
                        muxProtocol = settingsViewModel.composeMuxProtocol,
                        onMuxProtocolClick = onMuxProtocolClick,
                        muxMaxStreams = settingsViewModel.composeMuxMaxStreams,
                        onMuxMaxStreamsChange = {
                            settings.muxMaxStreams = it
                            settingsViewModel.composeMuxMaxStreams = it
                            onRestartRequired()
                        },
                        muxPadding = settingsViewModel.composeMuxPadding,
                        onMuxPaddingClick = onMuxPaddingClick,
                        remoteDnsMode = settingsViewModel.composeRemoteDnsMode,
                        onRemoteDnsModeClick = {
                            settings.remoteDnsMode = it
                            settingsViewModel.composeRemoteDnsMode = it
                            onRestartRequired()
                        },
                        remoteDnsUrl = settingsViewModel.composeRemoteDnsUrl,
                        onRemoteDnsUrlChange = {
                            settings.remoteDnsUrl = it
                            settingsViewModel.composeRemoteDnsUrl = it
                            onRestartRequired()
                        },
                        isFakeIpEnabled = settingsViewModel.composeIsFakeIpEnabled,
                        onFakeIpChange = {
                            settings.isFakeIpEnabled = it
                            settingsViewModel.composeIsFakeIpEnabled = it
                            onRestartRequired()
                        },
                        mtu = settingsViewModel.composeMtu,
                        onMtuChange = {
                            settings.mtu = it
                            settingsViewModel.composeMtu = it
                            onRestartRequired()
                        },
                        tunStack = settingsViewModel.composeTunStack,
                        onTunStackClick = onTunStackClick,
                        isResetChainOnDisconnect = settingsViewModel.composeIsResetChainOnDisconnect,
                        onResetChainOnDisconnectChange = {
                            settings.isResetChainOnDisconnect = it
                            settingsViewModel.composeIsResetChainOnDisconnect = it
                        },
                        isTlsSpoofEnabled = settingsViewModel.composeIsTlsSpoofEnabled,
                        onTlsSpoofChange = {
                            settings.isTlsSpoofEnabled = it
                            settingsViewModel.composeIsTlsSpoofEnabled = it
                            onRestartRequired()
                        },
                        tlsSpoofDomain = settingsViewModel.composeTlsSpoofDomain,
                        onTlsSpoofDomainChange = {
                            settings.tlsSpoofDomain = it
                            settingsViewModel.composeTlsSpoofDomain = it
                            onRestartRequired()
                        },
                        tlsSpoofMethod = settingsViewModel.composeTlsSpoofMethod,
                        onTlsSpoofMethodClick = {
                            settings.tlsSpoofMethod = it
                            settingsViewModel.composeTlsSpoofMethod = it
                            onRestartRequired()
                        },
                        fingerprint = settingsViewModel.composeFingerprint,
                        onFingerprintClick = {
                            settings.fingerprint = it
                            settingsViewModel.composeFingerprint = it
                            onRestartRequired()
                        },
                        accentColor = settingsViewModel.composeAccentColor,
                        onBack = { navController.popBackStack() },
                        hazeState = appHazeState
                    )
                }
            }

            composable(Destination.PingSettings.route) {
                SettingsDetailContainer(
                    route = Destination.PingSettings.route,
                    currentRoute = currentRoute,
                    morphRequest = pendingSettingsMorph,
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = settingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                PingSettingsScreen(
                    pingType = settingsViewModel.composePingType,
                    onPingTypeChange = { type -> 
                        settings.pingType = type
                        settingsViewModel.composePingType = type
                    },
                    pingTestUrl = settingsViewModel.composePingTestUrl,
                    onPingTestUrlChange = { url -> 
                        settings.pingTestUrl = url
                        settingsViewModel.composePingTestUrl = url
                    },
                    pingStyleValue = settingsViewModel.composePingStyle,
                    onPingStyleClick = onPingStyleClick,
                    onBack = { navController.popBackStack() },
                    accentColor = settingsViewModel.composeAccentColor,
                    hazeState = appHazeState
                )
                }
            }

            composable(Destination.RoutingSettings.route) {
                SettingsDetailContainer(
                    route = Destination.RoutingSettings.route,
                    currentRoute = currentRoute,
                    morphRequest = pendingSettingsMorph,
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = settingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                    val routingRules by mainViewModel.routingRules.collectAsState()
                    RoutingScreen(
                        routingRules = routingRules,
                        onBack = { navController.popBackStack() },
                        onToggleRule = { id, enabled -> 
                            mainViewModel.toggleRoutingRule(id, enabled)
                        },
                        onModeClick = onRoutingModeClick,
                        onDownloadClick = { id -> mainViewModel.downloadRoutingRule(id) },
                        accentColor = Color(accentColor),
                        hazeState = appHazeState
                    )
                }
            }

            composable(Destination.SubscriptionsSettings.route) {
                SettingsDetailContainer(
                    route = Destination.SubscriptionsSettings.route,
                    currentRoute = currentRoute,
                    morphRequest = pendingSettingsMorph,
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = settingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                SubscriptionsScreen(
                    isSubIntervalEnabled = settingsViewModel.composeIsSubIntervalEnabled,
                    onSubIntervalChange = { checked ->
                        settings.isSubIntervalEnabled = checked
                        settingsViewModel.composeIsSubIntervalEnabled = checked
                        if (checked) {
                            settings.isSubAutoUpdateEnabled = false
                            settingsViewModel.composeIsSubAutoUpdateEnabled = false
                        }
                        mainViewModel.startAutoUpdateJob()
                    },
                    isAutoUpdateEnabled = settingsViewModel.composeIsSubAutoUpdateEnabled,
                    onAutoUpdateChange = { checked ->
                        settings.isSubAutoUpdateEnabled = checked
                        settingsViewModel.composeIsSubAutoUpdateEnabled = checked
                        if (checked) {
                            settings.isSubIntervalEnabled = false
                            settingsViewModel.composeIsSubIntervalEnabled = false
                        }
                        mainViewModel.startAutoUpdateJob()
                    },
                    updateInterval = settingsViewModel.composeSubAutoUpdateInterval,
                    onUpdateIntervalChange = {
                        settings.subAutoUpdateInterval = it
                        settingsViewModel.composeSubAutoUpdateInterval = it
                        mainViewModel.startAutoUpdateJob()
                    },
                    userAgent = settingsViewModel.composeSubUserAgent,
                    onUserAgentClick = onUserAgentClick,
                    isHwidEnabled = settingsViewModel.composeIsHwidEnabled,
                    onHwidChange = {
                        settings.isHwidEnabled = it
                        settingsViewModel.composeIsHwidEnabled = it
                    },
                    onBack = { navController.popBackStack() },
                    accentColor = Color(accentColor),
                    hazeState = appHazeState
                )
                }
            }

            composable(Destination.ThemeSettings.route) {
                SettingsDetailContainer(
                    route = Destination.ThemeSettings.route,
                    currentRoute = currentRoute,
                    morphRequest = pendingSettingsMorph,
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = settingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                ThemeSettingsScreen(
                    themeMode = settingsViewModel.composeThemeMode,
                    backgroundType = settingsViewModel.composeBackgroundType,
                    isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled,
                    gradientSpeed = settingsViewModel.composeGradientSpeed,
                    isCustomColorEnabled = settingsViewModel.composeIsCustomColorEnabled,
                    accentColorKey = settingsViewModel.composeAccentColorKey,
                    accentColor = settingsViewModel.composeAccentColor,
                    onBack = { navController.popBackStack() },
                    onThemeClick = onThemeClick,
                    onBackgroundTypeClick = {
                        settings.backgroundType = it
                        settingsViewModel.composeBackgroundType = it
                        
                        val isGradient = it == 1
                        settings.isBackgroundGradientEnabled = isGradient
                        settingsViewModel.composeIsGradientEnabled = isGradient
                    },
                    onAnimationToggle = {
                        settings.isGradientAnimationEnabled = it
                        settingsViewModel.composeIsAnimationEnabled = it
                    },
                    onSpeedChange = {
                        settings.gradientAnimationSpeed = it
                        settingsViewModel.composeGradientSpeed = it
                    },
                    onCustomColorToggle = {
                        settings.isCustomColorEnabled = it
                        settingsViewModel.composeIsCustomColorEnabled = it
                    },
                    onColorKeySelect = {
                        settings.accentColorKey = it
                        settingsViewModel.composeAccentColorKey = it
                    },
                    isDownloadingPhoto = settingsViewModel.composeIsDownloadingPhoto,
                    onUpdatePhotoClick = {
                        coroutineScope.launch {
                            settingsViewModel.composeIsDownloadingPhoto = true
                            
                            kotlinx.coroutines.delay(50)
                            try {
                                val newSeed = (1..1000000).random().toString()
                                val tags = listOf("nature", "landscape", "city", "neon", "abstract", "architecture", "space")
                                val randomTag = tags.random()
                                val url = "https://loremflickr.com/1080/1920/$randomTag?lock=$newSeed"
                                android.util.Log.d("FlareVPN", "Downloading photo with OkHttp3: $url")
                                
                                val downloaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val client = okhttp3.OkHttpClient.Builder()
                                            .followRedirects(true)
                                            .followSslRedirects(true)
                                            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                                            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                                            .build()

                                        val request = okhttp3.Request.Builder()
                                            .url(url)
                                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                            .header("Cache-Control", "no-cache, no-store, must-revalidate")
                                            .header("Pragma", "no-cache")
                                            .build()

                                        val response = client.newCall(request).execute()
                                        android.util.Log.d("FlareVPN", "Response code: ${response.code}")
                                        
                                        if (!response.isSuccessful) {
                                            response.close()
                                            return@withContext false
                                        }
                                        
                                        val body = response.body
                                        if (body == null) {
                                            response.close()
                                            return@withContext false
                                        }
                                        
                                        val context = navController.context
                                        val outFile = java.io.File(context.filesDir, "background_photo.jpg")
                                        
                                        outFile.outputStream().use { out ->
                                            body.byteStream().copyTo(out)
                                        }
                                        response.close()
                                        
                                        
                                        context.filesDir.listFiles()?.forEach { file ->
                                            if (file.name.startsWith("bg_") && file.name.endsWith(".jpg")) {
                                                file.delete()
                                            }
                                        }
                                        
                                        android.util.Log.d("FlareVPN", "Saved: ${outFile.length()} bytes")
                                        true
                                    } catch (e: Exception) {
                                        android.util.Log.e("FlareVPN", "OkHttp3 error downloading photo", e)
                                        false
                                    }
                                }
                                
                                if (downloaded) {
                                    settings.photoSeed = newSeed
                                    settingsViewModel.composePhotoSeed = newSeed
                                    android.util.Log.d("FlareVPN", "Seed updated: $newSeed")
                                    AppNotificationManager.showNotification(
                                        NotificationType.SUCCESS,
                                        "Фон успешно обновлен",
                                        3
                                    )
                                } else {
                                    AppNotificationManager.showNotification(
                                        NotificationType.ERROR,
                                        "Ошибка загрузки фото. Проверьте интернет.",
                                        3
                                    )
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FlareVPN", "Download failed", e)
                                AppNotificationManager.showNotification(
                                    NotificationType.ERROR,
                                    "Неизвестная ошибка",
                                    3
                                )
                            } finally {
                                settingsViewModel.composeIsDownloadingPhoto = false
                            }
                        }
                    },
                    hazeState = appHazeState
                )
                }
            }

            composable(Destination.LanguageSettings.route) {
                SettingsDetailContainer(
                    route = Destination.LanguageSettings.route,
                    currentRoute = currentRoute,
                    morphRequest = pendingSettingsMorph,
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = settingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                LanguageSettingsScreen(
                    currentLanguage = settingsViewModel.composeAppLanguage,
                    accentColor = Color(accentColor),
                    onBack = { navController.popBackStack() },
                    onLanguageSelected = onLanguageSelected,
                    hazeState = appHazeState
                )
                }
            }
            
                composable(Destination.Journal.route) {
                SettingsDetailContainer(
                    route = Destination.Journal.route,
                    currentRoute = currentRoute,
                    morphRequest = null,
                    onMorphFinished = {},
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    backgroundContentRight = basicSettingsBackground,
                    onDismissLeft = {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                            coroutineScope.launch { rootPagerState.scrollToPage(1) }
                        },
                    backgroundContentLeft = homeBackgroundContent
                ,
                    hazeState = appHazeState
                ) {
                    JournalScreen(
                        logFile = java.io.File(navController.context.filesDir, "sing-box.log"),
                        accentColor = Color(accentColor),
                        onBack = { navController.popBackStack() },
                        hazeState = appHazeState
                    )
                }
            }

                composable(Destination.JsonEditor.route) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    
                    val profile by mainViewModel.editingProfile.collectAsState()
                    
                    LaunchedEffect(id) {
                        if (profile == null || profile?.id != id) {
                            mainViewModel.fetchProfileForEditing(id)
                        }
                    }

                    SwipeToDismissScreen(
                        onDismissRight = {
                            mainViewModel.setEditingProfile(null)
                            navController.popBackStack()
                        },
                        onSwipeDismissStart = { settingsViewModel.startSwipeDismiss() },
                        backgroundContentRight = homeBackgroundContent
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FlareHomeBackground(
                                backgroundType = settingsViewModel.composeBackgroundType,
                                isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && (currentRoute == Destination.JsonEditor.route),
                                animationSpeed = settingsViewModel.composeGradientSpeed,
                                photoSeed = settingsViewModel.composePhotoSeed,
                                modifier = Modifier.fillMaxSize()
                            )
                            profile?.let { p ->
                                val profileScheme = p.protocol?.takeIf { it.isNotBlank() }
                                    ?: try {
                                        val outbounds = org.json.JSONObject(p.configJson).optJSONArray("outbounds")
                                        outbounds?.optJSONObject(0)?.optString("type")
                                    } catch (_: Exception) { null }?.takeIf { it.isNotBlank() }
                                    ?: runCatching {
                                        java.net.URI(p.uri).scheme ?: ""
                                    }.getOrDefault("")
                                ProfileJsonEditor(
                                    initialName = p.name,
                                    initialContent = p.configJson,
                                    accentColor = Color(accentColor),
                                    initialScheme = profileScheme,
                                    onSave = { name: String, json: String ->
                                        mainViewModel.updateProfile(p.id, name, json)
                                        mainViewModel.setEditingProfile(null)
                                        AppNotificationManager.showNotification(
                                            NotificationType.SUCCESS,
                                            I18n.strings.notif_profile_changed,
                                            3
                                        )
                                        navController.popBackStack()
                                    },
                                    onBack = {
                                        mainViewModel.setEditingProfile(null)
                                        navController.popBackStack()
                                    },
                                    hazeState = appHazeState
                                )
                            }
                        }
                    }
                }

                composable(Destination.SimpleEditor.route) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    val profile by mainViewModel.editingProfile.collectAsState()

                    LaunchedEffect(id) {
                        if (profile == null || profile?.id != id) {
                            mainViewModel.fetchProfileForEditing(id)
                        }
                    }

                    SwipeToDismissScreen(
                        onDismissRight = {
                            mainViewModel.setEditingProfile(null)
                            navController.popBackStack()
                        },
                        onSwipeDismissStart = { settingsViewModel.startSwipeDismiss() },
                        backgroundContentRight = homeBackgroundContent
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FlareHomeBackground(
                                backgroundType = settingsViewModel.composeBackgroundType,
                                isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && (currentRoute == Destination.SimpleEditor.route),
                                animationSpeed = settingsViewModel.composeGradientSpeed,
                                photoSeed = settingsViewModel.composePhotoSeed,
                                modifier = Modifier.fillMaxSize()
                            )
                            profile?.let { p ->
                                ProfileSimpleEditor(
                                    profile = p,
                                    onSave = { updatedProfile ->
                                        mainViewModel.updateProfileFull(updatedProfile)
                                        mainViewModel.setEditingProfile(null)
                                        AppNotificationManager.showNotification(
                                            NotificationType.SUCCESS,
                                            I18n.strings.notif_profile_changed,
                                            3
                                        )
                                        navController.popBackStack()
                                    },
                                    onBack = {
                                        mainViewModel.setEditingProfile(null)
                                        navController.popBackStack()
                                    },
                                    accentColor = Color(accentColor),
                                    hazeState = appHazeState
                                )
                            }
                        }
                    }
                }
            }
        }

        val isDimmingActive = showBottomNav && !isLandscape && isBottomNavVisible && !settingsViewModel.composeBottomNavIsShrunk
        val dimmingAlpha by animateFloatAsState(
            targetValue = if (isDimmingActive) 1f else 0f,
            animationSpec = tween(durationMillis = 350),
            label = "bottomNavDimmingAlpha"
        )

        if (dimmingAlpha > 0.01f) {
            val isDark = FlareTheme.colors.isDark
            val dimmingColor = FlareTheme.colors.bgDark
            
            
            val dimmingHeight = bottomPadding + 66.dp
            
            val (baseBrush, glowBrush) = if (isDark) {
                
                val base = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.45f to dimmingColor.copy(alpha = 0.45f),
                    1.0f to dimmingColor.copy(alpha = 0.88f)
                )
                val glow = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    1.0f to Color.White.copy(alpha = 0.05f) 
                )
                base to glow
            } else {
                val base = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.4f to dimmingColor.copy(alpha = 0.4f),
                    1.0f to dimmingColor.copy(alpha = 0.95f)
                )
                base to null
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(dimmingHeight)
                    .graphicsLayer { alpha = dimmingAlpha }
                    .background(brush = baseBrush)
                    .let {
                        if (glowBrush != null) {
                            it.background(brush = glowBrush)
                        } else {
                            it
                        }
                    }
            )
        }

        if (showBottomNav) {
            if (isLandscape) {
                FlareSideNav(
                    modifier = Modifier.align(Alignment.CenterStart),
                    selectedIndex = selectedIndex,
                    onTabSelected = { index ->
                        if (currentRoute != Destination.Home.route) {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                        }
                        coroutineScope.launch { 
                            if (kotlin.math.abs(rootPagerState.currentPage - index) > 1) {
                                rootPagerState.scrollToPage(index)
                            } else {
                                rootPagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    isVisible = isBottomNavVisible,
                    onDoubleTapPill = {
                        if (selectedIndex == 1) {
                            val newShrunk = !settingsViewModel.composeBottomNavIsShrunk
                            settingsViewModel.composeBottomNavIsShrunk = newShrunk
                            settingsViewModel.composeBottomNavIsShrunkToHome = newShrunk
                        }
                    },
                    accentColorStart = Color(accentColor),
                    accentColorEnd = Color(accentEndColor),
                    hazeState = appHazeState
                )
            } else {
                FlareBottomNav(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = bottomPadding),
                    selectedIndex = selectedIndex,
                    onTabSelected = { index ->
                        if (currentRoute != Destination.Home.route) {
                            navController.popBackStack(Destination.Home.route, inclusive = false)
                        }
                        coroutineScope.launch { 
                            if (kotlin.math.abs(rootPagerState.currentPage - index) > 1) {
                                rootPagerState.scrollToPage(index)
                            } else {
                                rootPagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    isVisible = isBottomNavVisible,
                    isShrunk = settingsViewModel.composeBottomNavIsShrunk,
                    isShrunkToHome = settingsViewModel.composeBottomNavIsShrunkToHome,
                    onArrowClick = {
                        wizardViewModel.nextStep()
                        
                        if (wizardViewModel.composeWizardStep == WizardStep.CARDS) {
                            settingsViewModel.composeBottomNavIsShrunk = false
                        }
                    },
                    onDoubleTapPill = {
                        if (selectedIndex == 1) {
                            val newShrunk = !settingsViewModel.composeBottomNavIsShrunk
                            settingsViewModel.composeBottomNavIsShrunk = newShrunk
                            settingsViewModel.composeBottomNavIsShrunkToHome = newShrunk
                        }
                    },
                    accentColorStart = Color(accentColor),
                    accentColorEnd = Color(accentEndColor),
                    hazeState = appHazeState
                )
            }
        }

        ComposeNotificationHost(
            accentColor = Color(accentColor),
            hazeState = appHazeState
        )

        if (showDataManagementDialog) {
            DataManagementDialog(
                onDismissRequest = { showDataManagementDialog = false },
                accentColor = accentColor,
                hazeState = appHazeState,
                onRestartRequired = onRestartRequired
            )
        }
    }
}
}
