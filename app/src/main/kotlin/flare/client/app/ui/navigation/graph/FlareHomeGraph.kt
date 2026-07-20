package flare.client.app.ui.navigation.graph

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import flare.client.app.data.SettingsManager
import flare.client.app.data.auth.AuthManager
import flare.client.app.data.model.ProfileSummary
import flare.client.app.data.model.SubscriptionEntity
import flare.client.app.ui.HomeScreen
import flare.client.app.ui.SelectedProtocol
import flare.client.app.ui.ServerType
import flare.client.app.ui.ServersScreen
import flare.client.app.ui.SettingsScreen
import flare.client.app.ui.SettingsViewModel
import flare.client.app.ui.WizardStep
import flare.client.app.ui.WizardViewModel
import flare.client.app.ui.navigation.Destination
import flare.client.app.ui.navigation.ROOT_TAB_BLUR
import flare.client.app.ui.navigation.ROOT_TAB_ENTER_DURATION
import flare.client.app.ui.navigation.ROOT_TAB_EXIT_DURATION
import flare.client.app.ui.navigation.TransitionBlurContainer
import flare.client.app.ui.subscription.SubscriptionViewModel
import flare.client.app.ui.theme.FlareTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.flareHomeGraph(
    navController: NavHostController,
    coroutineScope: CoroutineScope,
    rootPagerState: PagerState,
    currentRoute: () -> String?,
    settingsViewModel: SettingsViewModel,
    vpnViewModel: flare.client.app.ui.viewmodel.VpnViewModel,
    profilesViewModel: flare.client.app.ui.viewmodel.ProfilesViewModel,
    wizardViewModel: WizardViewModel,
    accentColor: () -> Int,
    isClipboardLoading: () -> Boolean,
    isAnySubscriptionExpanded: () -> Boolean,
    homeListState: LazyListState,
    onShareProfile: (ProfileSummary) -> Unit,
    onQrProfile: (ProfileSummary) -> Unit,
    onShareSubscription: (SubscriptionEntity) -> Unit,
    onQrSubscription: (SubscriptionEntity) -> Unit,
    onEditSubscriptionClick: (SubscriptionEntity) -> Unit,
    onClipboardClick: () -> Unit,
    onManualInputClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onImportFileClick: () -> Unit,
    appHazeState: dev.chrisbanes.haze.HazeState,
    navigateToSettingsDetail: (String, android.view.View?) -> Unit,
    sharedSettingsScrollState: androidx.compose.foundation.ScrollState
) {
    composable(Destination.Home.route) {
        TransitionBlurContainer(
            isActive = currentRoute() == Destination.Home.route,
            enterBlur = ROOT_TAB_BLUR,
            exitBlur = ROOT_TAB_BLUR,
            enterDuration = ROOT_TAB_ENTER_DURATION,
            exitDuration = ROOT_TAB_EXIT_DURATION
        ) {
            @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.foundation.LocalOverscrollFactory provides null
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
                            onVpnSubscriptionClick = { navigateToSettingsDetail(Destination.VpnSubscription.route, it) },
                            onThemeSettingsClick = { navigateToSettingsDetail(Destination.ThemeSettings.route, it) },
                            onLanguageSettingsClick = { navigateToSettingsDetail(Destination.LanguageSettings.route, it) },
                            isGradientEnabled = settingsViewModel.composeIsGradientEnabled,
                            isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && rootPagerState.currentPage == 0,
                            gradientSpeed = settingsViewModel.composeGradientSpeed,
                            hazeState = appHazeState,
                            scrollState = sharedSettingsScrollState
                        )
                    }
                    1 -> {
                        val connectionState by vpnViewModel.connectionState.collectAsState()
                        val profiles by profilesViewModel.displayItems.collectAsState()
                        val chainedProfileIds by vpnViewModel.chainedProfileIds.collectAsState()

                        HomeScreen(
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
                            isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && rootPagerState.currentPage == 1,
                            animationSpeed = settingsViewModel.composeGradientSpeed,
                            isCustomColorEnabled = settingsViewModel.composeIsCustomColorEnabled,
                            isChangeLaunchButtonColorEnabled = settingsViewModel.composeIsChangeLaunchButtonColorEnabled,
                            listState = homeListState,
                            onConnectClick = { vpnViewModel.connectOrDisconnect() },
                            onProfileClick = { profile -> vpnViewModel.selectProfile(profile.id) },
                            onProfileDelete = { profile -> profilesViewModel.deleteProfile(profile.id, profile.name) },
                            onShareProfile = onShareProfile,
                            onQrProfile = onQrProfile,
                            onEditProfileJson = { profile ->
                                if (navController.currentDestination?.route == Destination.Home.route) {
                                    profilesViewModel.setEditingProfile(null)
                                    navController.navigate(Destination.JsonEditor.createRoute(profile.id, Destination.JsonEditor.TYPE_PROFILE))
                                }
                            },
                            onEditProfileSimple = { profile ->
                                if (navController.currentDestination?.route == Destination.Home.route) {
                                    profilesViewModel.setEditingProfile(null)
                                    navController.navigate(Destination.SimpleEditor.createRoute(profile.id))
                                }
                            },
                            onProfileTest = { profile -> profilesViewModel.speedTestProfile(listOf(profile)) },
                            onSubscriptionToggle = { sub -> profilesViewModel.toggleSubscriptionExpanded(sub.id) },
                            onSubscriptionDelete = { id -> profilesViewModel.deleteSubscription(id) },
                            onSubscriptionSpeedTest = { id -> profilesViewModel.speedTestSubscription(id) },
                            onSubscriptionUpdate = { sub -> profilesViewModel.refreshSubscription(sub) },
                            onEditSubscriptionJson = { sub -> onEditSubscriptionClick(sub) },
                            onSubscriptionPinToggle = { sub -> profilesViewModel.toggleSubscriptionPinned(sub.id) },
                            onSubscriptionShare = onShareSubscription,
                            onSubscriptionQr = onQrSubscription,
                            onSubscriptionMerge = { id -> profilesViewModel.addSubscriptionToMerged(id) },
                            onClipboardClick = onClipboardClick,
                            onManualInputClick = onManualInputClick,
                            onQrScanClick = onQrScanClick,
                            onImportFileClick = onImportFileClick,
                            onBack = { profilesViewModel.collapseAllSubscriptions() },
                            onScroll = {  },
                            hazeState = appHazeState
                        )
                    }
                    2 -> {
                        val context = LocalContext.current
                        val settings = remember { SettingsManager(context) }
                        val authManager = remember { AuthManager(context, settings) }
                        val application = context.applicationContext as android.app.Application
                        val subscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SubscriptionViewModel>(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    @Suppress("UNCHECKED_CAST")
                                    return SubscriptionViewModel(application, authManager, settings) as T
                                }
                            }
                        )
                        ServersScreen(
                            currentStep = wizardViewModel.composeWizardStep,
                            selectedServerType = wizardViewModel.composeSelectedServerType,
                            accentColor = Color(accentColor()),
                            isFreeSuccess = wizardViewModel.composeFreeSubscriptionSuccess,
                            freeError = wizardViewModel.composeFreeSubscriptionError,
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
                            onFreeWithoutAuthClick = { wizardViewModel.selectFreeWithoutAuth() },
                            onFreeWithAuthClick = { wizardViewModel.selectFreeWithAuth() },
                            onFreeAuthSuccess = { wizardViewModel.onFreeAuthSuccess() },
                            onPremiumAuthSuccess = { wizardViewModel.onPremiumAuthSuccess() },
                            authManager = authManager,
                            subscriptionViewModel = subscriptionViewModel,
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
                            xrayTransport = wizardViewModel.composeXrayTransport,
                            onXrayTransportChange = { wizardViewModel.composeXrayTransport = it },
                            isXrayTransportExpanded = wizardViewModel.composeIsXrayTransportExpanded,
                            onXrayTransportExpandedChange = { wizardViewModel.composeIsXrayTransportExpanded = it },
                            xrayHost = wizardViewModel.composeXrayHost,
                            onXrayHostChange = { wizardViewModel.composeXrayHost = it },
                            xrayPath = wizardViewModel.composeXrayPath,
                            onXrayPathChange = { wizardViewModel.composeXrayPath = it },
                            xrayServiceName = wizardViewModel.composeXrayServiceName,
                            onXrayServiceNameChange = { wizardViewModel.composeXrayServiceName = it },
                            xrayXhttpMode = wizardViewModel.composeXrayXhttpMode,
                            onXrayXhttpModeChange = { wizardViewModel.composeXrayXhttpMode = it },
                            isXrayXhttpModeExpanded = wizardViewModel.composeIsXrayXhttpModeExpanded,
                            onXrayXhttpModeExpandedChange = { wizardViewModel.composeIsXrayXhttpModeExpanded = it },
                            setupStatus = wizardViewModel.composeSetupStatus,
                            setupProgress = wizardViewModel.composeSetupProgress,
                            setupError = wizardViewModel.composeSetupError,
                            authError = wizardViewModel.composeAuthError,
                            isAuthPolling = wizardViewModel.composeIsAuthPolling,
                            onOpenTelegramAuthClick = { wizardViewModel.startAuthAndPoll() },
                            onOpenTelegramBuyClick = { wizardViewModel.openTelegramBuy() },
                            onCompleteBuyClick = { wizardViewModel.completeBuy() },
                            onGoHomeClick = { 
                                wizardViewModel.reset()
                                coroutineScope.launch { 
                                    rootPagerState.animateScrollToPage(
                                        page = 1,
                                        animationSpec = tween(durationMillis = ROOT_TAB_ENTER_DURATION)
                                    )
                                }
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
    }
}
