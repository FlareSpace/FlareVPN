package flare.client.app.ui.navigation.graph

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import flare.client.app.data.SettingsManager
import flare.client.app.ui.AdvancedSettingsScreen
import flare.client.app.ui.BasicSettingsScreen
import flare.client.app.ui.LanguageSettingsScreen
import flare.client.app.ui.PingSettingsScreen
import flare.client.app.ui.RoutingScreen
import flare.client.app.ui.SettingsViewModel
import flare.client.app.ui.SubscriptionsScreen
import flare.client.app.ui.ThemeSettingsScreen
import flare.client.app.ui.components.JournalScreen
import flare.client.app.ui.navigation.Destination
import flare.client.app.ui.navigation.SettingsDetailContainer
import flare.client.app.ui.navigation.SettingsMorphRequest
import flare.client.app.ui.navigation.SettingsBackgroundContent
import flare.client.app.ui.navigation.HomeBackgroundContent
import flare.client.app.ui.navigation.BasicSettingsBackgroundContent
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.notification.NotificationType
import flare.client.app.ui.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.flareSettingsGraph(
    navController: NavHostController,
    coroutineScope: CoroutineScope,
    rootPagerState: androidx.compose.foundation.pager.PagerState,
    currentRoute: () -> String?,
    pendingSettingsMorph: () -> SettingsMorphRequest?,
    onMorphFinished: () -> Unit,
    settingsViewModel: SettingsViewModel,
    routingViewModel: flare.client.app.ui.viewmodel.RoutingViewModel,
    profilesViewModel: flare.client.app.ui.viewmodel.ProfilesViewModel,
    vpnViewModel: flare.client.app.ui.viewmodel.VpnViewModel,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    settings: SettingsManager,
    onRestartRequired: () -> Unit,
    onChangeAppsClick: () -> Unit,
    onLogLevelClick: (String) -> Unit,
    onBestProfileOnlyConnectedClick: (Boolean) -> Unit,
    onUpdateFrequencyClick: (String) -> Unit,
    onPacketTypeClick: (String) -> Unit,
    onMuxProtocolClick: (String) -> Unit,
    onMuxPaddingClick: (Boolean) -> Unit,
    onTunStackClick: (String) -> Unit,
    onPingStyleClick: (String) -> Unit,
    onRoutingModeClick: (String, String) -> Unit,
    onUserAgentClick: (String) -> Unit,
    onThemeClick: (Int) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onFontSelect: (String) -> Unit,
    accentColor: () -> Int,
    isClipboardLoading: () -> Boolean,
    isAnySubscriptionExpanded: () -> Boolean,
    appHazeState: dev.chrisbanes.haze.HazeState,
    sharedBasicSettingsScrollState: ScrollState,
    onDataManagementClick: () -> Unit
) {
    val navigateHome = {
        navController.popBackStack(Destination.Home.route, inclusive = false)
        coroutineScope.launch { rootPagerState.scrollToPage(1) }
    }

    composable(Destination.BasicSettings.route) {
        SettingsDetailContainer(
            route = Destination.BasicSettings.route,
            currentRoute = currentRoute(),
            morphRequest = pendingSettingsMorph(),
            onMorphFinished = onMorphFinished,
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { SettingsBackgroundContent(settingsViewModel, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
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
                isChangeAppsLoading = settingsViewModel.composeIsChangeAppsLoading,
                isAutostartEnabled = settingsViewModel.composeIsAutostartEnabled,
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
                    vpnViewModel.startBestProfileJob()
                },
                bestProfileInterval = settingsViewModel.composeBestProfileInterval,
                onBestProfileIntervalChange = {
                    settings.bestProfileInterval = it
                    settingsViewModel.composeBestProfileInterval = it
                    vpnViewModel.startBestProfileJob()
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
                onDataManagementClick = onDataManagementClick,
                scrollState = sharedBasicSettingsScrollState,
                accentColor = Color(accentColor()),
                onBack = { navController.popBackStack() },
                hazeState = appHazeState
            )
        }
    }

    composable(Destination.AdvancedSettings.route) {
        SettingsDetailContainer(
            route = Destination.AdvancedSettings.route,
            currentRoute = currentRoute(),
            morphRequest = pendingSettingsMorph(),
            onMorphFinished = onMorphFinished,
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { SettingsBackgroundContent(settingsViewModel, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
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
            currentRoute = currentRoute(),
            morphRequest = pendingSettingsMorph(),
            onMorphFinished = onMorphFinished,
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { SettingsBackgroundContent(settingsViewModel, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
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
                pingTimeout = settingsViewModel.composePingTimeout,
                onPingTimeoutChange = { timeout ->
                    settings.pingTimeout = timeout
                    settingsViewModel.composePingTimeout = timeout
                },
                onBack = { navController.popBackStack() },
                accentColor = settingsViewModel.composeAccentColor,
                hazeState = appHazeState
            )
        }
    }

    composable(Destination.RoutingSettings.route) {
        SettingsDetailContainer(
            route = Destination.RoutingSettings.route,
            currentRoute = currentRoute(),
            morphRequest = pendingSettingsMorph(),
            onMorphFinished = onMorphFinished,
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { SettingsBackgroundContent(settingsViewModel, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
            hazeState = appHazeState
        ) {
            val routingRules by routingViewModel.routingRules.collectAsState()
            RoutingScreen(
                routingRules = routingRules,
                onBack = { navController.popBackStack() },
                onToggleRule = { id, enabled ->
                    routingViewModel.toggleRoutingRule(id, enabled)
                },
                onModeClick = onRoutingModeClick,
                onDownloadClick = { id -> routingViewModel.downloadRoutingRule(id) },
                accentColor = Color(accentColor()),
                hazeState = appHazeState
            )
        }
    }

    composable(Destination.SubscriptionsSettings.route) {
        SettingsDetailContainer(
            route = Destination.SubscriptionsSettings.route,
            currentRoute = currentRoute(),
            morphRequest = pendingSettingsMorph(),
            onMorphFinished = onMorphFinished,
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { SettingsBackgroundContent(settingsViewModel, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
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
                    profilesViewModel.startAutoUpdateJob()
                },
                isAutoUpdateEnabled = settingsViewModel.composeIsSubAutoUpdateEnabled,
                onAutoUpdateChange = { checked ->
                    settings.isSubAutoUpdateEnabled = checked
                    settingsViewModel.composeIsSubAutoUpdateEnabled = checked
                    if (checked) {
                        settings.isSubIntervalEnabled = false
                        settingsViewModel.composeIsSubIntervalEnabled = false
                    }
                    profilesViewModel.startAutoUpdateJob()
                },
                updateInterval = settingsViewModel.composeSubAutoUpdateInterval,
                onUpdateIntervalChange = {
                    settings.subAutoUpdateInterval = it
                    settingsViewModel.composeSubAutoUpdateInterval = it
                    profilesViewModel.startAutoUpdateJob()
                },
                subUpdateTimeout = settingsViewModel.composeSubUpdateTimeout,
                onSubUpdateTimeoutChange = {
                    settings.subUpdateTimeout = it
                    settingsViewModel.composeSubUpdateTimeout = it
                },
                userAgent = settingsViewModel.composeSubUserAgent,
                onUserAgentClick = onUserAgentClick,
                isHwidEnabled = settingsViewModel.composeIsHwidEnabled,
                onHwidChange = {
                    settings.isHwidEnabled = it
                    settingsViewModel.composeIsHwidEnabled = it
                },
                onBack = { navController.popBackStack() },
                accentColor = Color(accentColor()),
                hazeState = appHazeState
            )
        }
    }

    composable(Destination.ThemeSettings.route) {
        SettingsDetailContainer(
            route = Destination.ThemeSettings.route,
            currentRoute = currentRoute(),
            morphRequest = pendingSettingsMorph(),
            onMorphFinished = onMorphFinished,
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { SettingsBackgroundContent(settingsViewModel, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
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
                isChangeLaunchButtonColorEnabled = settingsViewModel.composeIsChangeLaunchButtonColorEnabled,
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
                isBlurEnabled = settingsViewModel.composeIsBlurEnabled,
                onBlurToggle = {
                    settings.isBlurEnabled = it
                    settingsViewModel.composeIsBlurEnabled = it
                },
                isLiquidGlassEnabled = settingsViewModel.composeIsLiquidGlassEnabled,
                onLiquidGlassToggle = {
                    settings.isLiquidGlassEnabled = it
                    settingsViewModel.composeIsLiquidGlassEnabled = it
                },
                onCustomColorToggle = {
                    settings.isCustomColorEnabled = it
                    settingsViewModel.composeIsCustomColorEnabled = it
                },
                onColorKeySelect = {
                    settings.accentColorKey = it
                    settingsViewModel.composeAccentColorKey = it
                },
                onChangeLaunchButtonColorToggle = {
                    settings.isChangeLaunchButtonColorEnabled = it
                    settingsViewModel.composeIsChangeLaunchButtonColorEnabled = it
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
                fontFamily = settingsViewModel.composeFontFamily,
                onFontSelect = onFontSelect,
                hazeState = appHazeState
            )
        }
    }

    composable(Destination.LanguageSettings.route) {
        SettingsDetailContainer(
            route = Destination.LanguageSettings.route,
            currentRoute = currentRoute(),
            morphRequest = pendingSettingsMorph(),
            onMorphFinished = onMorphFinished,
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { SettingsBackgroundContent(settingsViewModel, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
            hazeState = appHazeState
        ) {
            LanguageSettingsScreen(
                currentLanguage = settingsViewModel.composeAppLanguage,
                accentColor = Color(accentColor()),
                onBack = { navController.popBackStack() },
                onLanguageSelected = onLanguageSelected,
                hazeState = appHazeState
            )
        }
    }

    composable(Destination.Journal.route) {
        SettingsDetailContainer(
            route = Destination.Journal.route,
            currentRoute = currentRoute(),
            morphRequest = null,
            onMorphFinished = {},
            settingsViewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
            backgroundContentRight = { BasicSettingsBackgroundContent(settingsViewModel, sharedBasicSettingsScrollState, accentColor, appHazeState) },
            onDismissLeft = { navigateHome() },
            backgroundContentLeft = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) },
            hazeState = appHazeState
        ) {
            JournalScreen(
                logFile = java.io.File(navController.context.filesDir, "sing-box.log"),
                accentColor = Color(accentColor()),
                onBack = { navController.popBackStack() },
                hazeState = appHazeState
            )
        }
    }
}
