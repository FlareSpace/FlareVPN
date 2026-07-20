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
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import flare.client.app.ui.navigation.settingsForwardEnterTransition
import flare.client.app.ui.navigation.settingsForwardExitTransition
import flare.client.app.ui.navigation.settingsBackEnterTransition
import flare.client.app.ui.navigation.settingsBackExitTransition
import flare.client.app.ui.navigation.isSettingsDetailRoute
import flare.client.app.ui.navigation.isEditorRoute
import flare.client.app.ui.navigation.SettingsMorphRequest
import flare.client.app.ui.navigation.TransitionBlurContainer
import flare.client.app.ui.navigation.ROOT_TAB_BLUR
import flare.client.app.ui.navigation.ROOT_TAB_ENTER_DURATION
import flare.client.app.ui.navigation.ROOT_TAB_EXIT_DURATION
import flare.client.app.ui.navigation.graph.flareSettingsGraph
import flare.client.app.ui.navigation.graph.flareHomeGraph
import flare.client.app.ui.navigation.graph.flareEditorGraph
import flare.client.app.ui.components.FlareBottomNav
import flare.client.app.ui.components.FlareSideNav
import flare.client.app.ui.navigation.Destination
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





@Composable
fun FlareApp(
    mainViewModel: MainViewModel,
    vpnViewModel: flare.client.app.ui.viewmodel.VpnViewModel,
    profilesViewModel: flare.client.app.ui.viewmodel.ProfilesViewModel,
    routingViewModel: flare.client.app.ui.viewmodel.RoutingViewModel,
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
    onFontSelect: (String) -> Unit,
    onAppIconSelect: (String) -> Unit,
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
    onRequestNotificationPermission: () -> Unit,
    isDark: Boolean = false,
    appHazeState: dev.chrisbanes.haze.HazeState
) {
    FlareTheme(
        isDark = isDark,
        accentColor = Color(accentColor),
        accentEndColor = Color(accentEndColor),
        isBlurEnabled = settingsViewModel.composeIsBlurEnabled,
        isLiquidGlassEnabled = settingsViewModel.composeIsLiquidGlassEnabled,
        fontKey = settingsViewModel.composeFontFamily
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
            settingsViewModel.isGestureNav = isGestureNav

            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    isGestureNav = checkGestureNav()
                    settingsViewModel.isGestureNav = isGestureNav
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
        val isAnySubscriptionExpanded by profilesViewModel.isAnySubscriptionExpanded.collectAsState()
        var pendingSettingsMorph by remember { mutableStateOf<SettingsMorphRequest?>(null) }
        var showDataManagementDialog by remember { mutableStateOf(false) }
        val sharedBasicSettingsScrollState = androidx.compose.foundation.rememberScrollState()
        val sharedSettingsScrollState = androidx.compose.foundation.rememberScrollState()
        val homeListState = rememberLazyListState()

        LaunchedEffect(currentRoute, rootPagerState.currentPage) {
            val isBasicSettingsOrJournal = currentRoute == Destination.BasicSettings.route || currentRoute == Destination.Journal.route
            if (!isBasicSettingsOrJournal && sharedBasicSettingsScrollState.value != 0) {
                sharedBasicSettingsScrollState.scrollTo(0)
            }

            val isSettingsRoute = when (currentRoute) {
                Destination.BasicSettings.route,
                Destination.AdvancedSettings.route,
                Destination.RoutingSettings.route,
                Destination.PingSettings.route,
                Destination.SubscriptionsSettings.route,
                Destination.VpnSubscription.route,
                Destination.ThemeSettings.route,
                Destination.LanguageSettings.route,
                Destination.Journal.route -> true
                Destination.Home.route -> rootPagerState.currentPage == 0
                else -> false
            }
            if (!isSettingsRoute && sharedSettingsScrollState.value != 0) {
                sharedSettingsScrollState.scrollTo(0)
            }
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
                    WizardStep.FLARE_TARIFFS -> wizardViewModel.composeSelectedTariff != null
                    WizardStep.FLARE_AUTH -> false
                    WizardStep.FLARE_BUY -> false
                    WizardStep.FLARE_PROGRESS -> false
                    WizardStep.FLARE_SUCCESS -> false
                    WizardStep.FLARE_FREE_AUTH_PROMPT -> false
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
            if (navController.currentDestination?.route != Destination.Home.route) {
                return
            }
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
                .let { if (FlareTheme.effects.isBlurEnabled) it.hazeSource(state = appHazeState) else it }
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
                flareHomeGraph(
                    navController = navController,
                    coroutineScope = coroutineScope,
                    rootPagerState = rootPagerState,
                    currentRoute = { currentRoute },
                    settingsViewModel = settingsViewModel,
                    vpnViewModel = vpnViewModel,
                    profilesViewModel = profilesViewModel,
                    wizardViewModel = wizardViewModel,
                    accentColor = { accentColor },
                    isClipboardLoading = { isClipboardLoading },
                    isAnySubscriptionExpanded = { isAnySubscriptionExpanded },
                    homeListState = homeListState,
                    onShareProfile = onShareProfile,
                    onQrProfile = onQrProfile,
                    onShareSubscription = onShareSubscription,
                    onQrSubscription = onQrSubscription,
                    onEditSubscriptionClick = onEditSubscriptionClick,
                    onClipboardClick = onClipboardClick,
                    onManualInputClick = onManualInputClick,
                    onQrScanClick = onQrScanClick,
                    onImportFileClick = onImportFileClick,
                    appHazeState = appHazeState,
                    navigateToSettingsDetail = { route, anchor -> navigateToSettingsDetail(route, anchor) },
                    sharedSettingsScrollState = sharedSettingsScrollState
                )

                flareSettingsGraph(
                    navController = navController,
                    coroutineScope = coroutineScope,
                    rootPagerState = rootPagerState,
                    currentRoute = { currentRoute },
                    pendingSettingsMorph = { pendingSettingsMorph },
                    onMorphFinished = { pendingSettingsMorph = null },
                    settingsViewModel = settingsViewModel,
                    routingViewModel = routingViewModel,
                    profilesViewModel = profilesViewModel,
                    vpnViewModel = vpnViewModel,
                    homeListState = homeListState,
                    settings = settings,
                    onRestartRequired = onRestartRequired,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onChangeAppsClick = onChangeAppsClick,
                    onLogLevelClick = onLogLevelClick,
                    onBestProfileOnlyConnectedClick = onBestProfileOnlyConnectedClick,
                    onUpdateFrequencyClick = onUpdateFrequencyClick,
                    onPacketTypeClick = onPacketTypeClick,
                    onMuxProtocolClick = onMuxProtocolClick,
                    onMuxPaddingClick = onMuxPaddingClick,
                    onTunStackClick = onTunStackClick,
                    onPingStyleClick = onPingStyleClick,
                    onRoutingModeClick = onRoutingModeClick,
                    onUserAgentClick = onUserAgentClick,
                    onThemeClick = onThemeClick,
                    onLanguageSelected = onLanguageSelected,
                    onFontSelect = onFontSelect,
                    onAppIconSelect = onAppIconSelect,
                    accentColor = { accentColor },
                    isClipboardLoading = { isClipboardLoading },
                    isAnySubscriptionExpanded = { isAnySubscriptionExpanded },
                    appHazeState = appHazeState,
                    sharedBasicSettingsScrollState = sharedBasicSettingsScrollState,
                    sharedSettingsScrollState = sharedSettingsScrollState,
                    onDataManagementClick = { showDataManagementDialog = true }
                )

                flareEditorGraph(
                    navController = navController,
                    currentRoute = { currentRoute },
                    settingsViewModel = settingsViewModel,
                    profilesViewModel = profilesViewModel,
                    vpnViewModel = vpnViewModel,
                    homeListState = homeListState,
                    accentColor = { accentColor },
                    isClipboardLoading = { isClipboardLoading },
                    isAnySubscriptionExpanded = { isAnySubscriptionExpanded },
                    appHazeState = appHazeState
                )
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
                                rootPagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = tween(durationMillis = ROOT_TAB_ENTER_DURATION)
                                )
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
                                rootPagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = tween(durationMillis = ROOT_TAB_ENTER_DURATION)
                                )
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
