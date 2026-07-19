package flare.client.app.ui

import flare.client.app.ui.i18n.I18n

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import flare.client.app.R
import flare.client.app.data.auth.AuthManager
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.*
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.ui.components.servers.ServerActionCard
import flare.client.app.ui.components.servers.SshConfigStep
import flare.client.app.ui.components.servers.XrayConfigStep
import flare.client.app.ui.components.servers.ShadowsocksConfigStep
import flare.client.app.ui.components.servers.WireGuardConfigStep
import flare.client.app.ui.components.servers.SetupProgressStep
import flare.client.app.ui.components.servers.SetupSuccessStep
import flare.client.app.ui.components.servers.TariffCard
import flare.client.app.ui.components.servers.FlareProgressStep
import flare.client.app.ui.components.servers.WizardStepper
import flare.client.app.ui.components.servers.FreeAuthPromptStep
import flare.client.app.ui.subscription.AuthFlowSection
import flare.client.app.ui.subscription.SubscriptionViewModel
import flare.client.app.ui.subscription.TopUpDialog
import flare.client.app.util.GlassUtils
import androidx.compose.ui.platform.LocalContext



@Composable
fun ServersScreen(
    currentStep: WizardStep,
    selectedServerType: ServerType?,
    accentColor: Color,
    isFreeSuccess: Boolean = true,
    freeError: String? = null,
    onFlareServersClick: () -> Unit,
    onCreateServerClick: () -> Unit,
    
    selectedTariff: TariffType?,
    onTariffSelect: (TariffType) -> Unit,
    
    onFreeWithoutAuthClick: () -> Unit,
    onFreeWithAuthClick: () -> Unit,
    onFreeAuthSuccess: () -> Unit,
    onPremiumAuthSuccess: () -> Unit,
    
    authManager: AuthManager,
    subscriptionViewModel: SubscriptionViewModel,
    
    sshProfileName: String,
    onSshProfileNameChange: (String) -> Unit,
    sshIp: String,
    onSshIpChange: (String) -> Unit,
    sshPort: String,
    onSshPortChange: (String) -> Unit,
    sshUser: String,
    onSshUserChange: (String) -> Unit,
    sshPass: String,
    onSshPassChange: (String) -> Unit,
    onSshKeyClick: () -> Unit,
    
    selectedProtocol: SelectedProtocol,
    onProtocolXrayClick: () -> Unit,
    onProtocolHysteria2Click: () -> Unit,
    onProtocolShadowsocksClick: () -> Unit,
    onProtocolWireGuardClick: () -> Unit,
    
    xrayPort: String,
    onXrayPortChange: (String) -> Unit,
    xraySni: String,
    onXraySniChange: (String) -> Unit,
    obfsPassword: String,
    onObfsPasswordChange: (String) -> Unit,
    portHoppingEnabled: Boolean,
    onPortHoppingEnabledChange: (Boolean) -> Unit,
    portHoppingValue: String,
    onPortHoppingValueChange: (String) -> Unit,
    xrayTransport: String = "tcp",
    onXrayTransportChange: (String) -> Unit = {},
    isXrayTransportExpanded: Boolean = false,
    onXrayTransportExpandedChange: (Boolean) -> Unit = {},
    xrayHost: String = "",
    onXrayHostChange: (String) -> Unit = {},
    xrayPath: String = "",
    onXrayPathChange: (String) -> Unit = {},
    xrayServiceName: String = "",
    onXrayServiceNameChange: (String) -> Unit = {},
    xrayXhttpMode: String = "auto",
    onXrayXhttpModeChange: (String) -> Unit = {},
    isXrayXhttpModeExpanded: Boolean = false,
    onXrayXhttpModeExpandedChange: (Boolean) -> Unit = {},
    
    
    setupStatus: String,
    setupProgress: Float,
    setupError: String?,
    
    authError: String?,
    isAuthPolling: Boolean,
    onOpenTelegramAuthClick: () -> Unit,
    onOpenTelegramBuyClick: () -> Unit,
    onCompleteBuyClick: () -> Unit,
    
    onGoHomeClick: () -> Unit,
    onBack: () -> Unit,
    onNextClick: () -> Unit,
    isSshConfigValid: Boolean,
    hazeState: HazeState
) {
    var showTopUpDialog by remember { mutableStateOf(false) }
    var showTopUpMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    BackHandler(enabled = currentStep != WizardStep.CARDS || selectedServerType != null || selectedTariff != null) {
        onBack()
    }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {

        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .let { if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) it.hazeSource(state = hazeState) else it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(Color.Transparent)
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .padding(top = 67.dp, bottom = 112.dp)
                    .padding(horizontal = 20.dp)
            ) {
                
                val showStepper = currentStep in listOf(
                    WizardStep.SSH_CONFIG,
                    WizardStep.PROTOCOL,
                    WizardStep.XRAY_CONFIG,
                    WizardStep.PROGRESS,
                    WizardStep.SUCCESS
                )
                if (showStepper) {
                    val activeIndex = when (currentStep) {
                        WizardStep.SSH_CONFIG -> 0
                        WizardStep.PROTOCOL -> 1
                        WizardStep.XRAY_CONFIG -> 2
                        WizardStep.PROGRESS, WizardStep.SUCCESS -> 3
                        else -> 0
                    }
                    val stepLabels = listOf(
                        I18n.strings.wizard_step_ssh,
                        I18n.strings.wizard_step_protocol,
                        I18n.strings.wizard_step_settings,
                        I18n.strings.wizard_step_setup
                    )
                    WizardStepper(
                        activeIndex = activeIndex,
                        steps = stepLabels,
                        accentColor = accentColor,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                                .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300)))
                                .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "wizardStepAnimation"
                ) { step ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when (step) {
                            WizardStep.CARDS -> {
                                ServerActionCard(
                                    title = I18n.strings.servers_title_flare,
                                    description = I18n.strings.servers_desc_flare,
                                    icon = R.drawable.ic_cloud,
                                    isSelected = selectedServerType == ServerType.FLARE,
                                    accentColor = accentColor,
                                    onClick = onFlareServersClick
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ServerActionCard(
                                    title = I18n.strings.servers_title_create,
                                    description = I18n.strings.servers_desc_create,
                                    icon = R.drawable.ic_suitcase,
                                    isSelected = selectedServerType == ServerType.CUSTOM,
                                    accentColor = accentColor,
                                    onClick = onCreateServerClick
                                )
                            }
                            WizardStep.SSH_CONFIG -> {
                                SshConfigStep(
                                    profileName = sshProfileName,
                                    onProfileNameChange = onSshProfileNameChange,
                                    ip = sshIp,
                                    onIpChange = onSshIpChange,
                                    port = sshPort,
                                    onPortChange = onSshPortChange,
                                    user = sshUser,
                                    onUserChange = onSshUserChange,
                                    pass = sshPass,
                                    onPassChange = onSshPassChange,
                                    onSshKeyClick = onSshKeyClick,
                                    hazeState = hazeState,
                                    accentColor = accentColor
                                )
                            }
                            WizardStep.PROTOCOL -> {
                                Text(
                                    text = I18n.strings.servers_protocol_title,
                                    fontFamily = GeologicaRegular,
                                    fontSize = 13.sp,
                                    color = FlareTheme.colors.textSecondary,
                                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                                )
                                ServerActionCard(
                                    title = I18n.strings.servers_protocol_xray_title,
                                    description = I18n.strings.servers_protocol_xray_desc,
                                    icon = R.drawable.ic_mask,
                                    isSelected = selectedProtocol == SelectedProtocol.XRAY,
                                    accentColor = accentColor,
                                    onClick = onProtocolXrayClick
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ServerActionCard(
                                    title = I18n.strings.servers_protocol_hysteria2_title,
                                    description = I18n.strings.servers_protocol_hysteria2_desc,
                                    icon = R.drawable.ic_lightning,
                                    isSelected = selectedProtocol == SelectedProtocol.HYSTERIA2,
                                    accentColor = accentColor,
                                    onClick = onProtocolHysteria2Click
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ServerActionCard(
                                    title = I18n.strings.servers_protocol_shadowsocks_title,
                                    description = I18n.strings.servers_protocol_shadowsocks_desc,
                                    icon = R.drawable.ic_paper_plane,
                                    isSelected = selectedProtocol == SelectedProtocol.SHADOWSOCKS,
                                    accentColor = accentColor,
                                    onClick = onProtocolShadowsocksClick
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ServerActionCard(
                                    title = I18n.strings.servers_protocol_wireguard_title,
                                    description = I18n.strings.servers_protocol_wireguard_desc,
                                    icon = R.drawable.ic_chain,
                                    isSelected = selectedProtocol == SelectedProtocol.WIREGUARD,
                                    accentColor = accentColor,
                                    onClick = onProtocolWireGuardClick
                                )
                            }
                             WizardStep.XRAY_CONFIG -> {
                                 if (selectedProtocol == SelectedProtocol.SHADOWSOCKS) {
                                     ShadowsocksConfigStep(
                                         port = xrayPort,
                                         onPortChange = onXrayPortChange,
                                         sni = xraySni,
                                         onSniChange = onXraySniChange,
                                         accentColor = accentColor
                                     )
                                 } else if (selectedProtocol == SelectedProtocol.WIREGUARD) {
                                     WireGuardConfigStep(
                                         port = xrayPort,
                                         onPortChange = onXrayPortChange,
                                         accentColor = accentColor
                                     )
                                 } else {
                                      XrayConfigStep(
                                          selectedProtocol = selectedProtocol,
                                          port = xrayPort,
                                          onPortChange = onXrayPortChange,
                                          sni = xraySni,
                                          onSniChange = onXraySniChange,
                                          obfsPassword = obfsPassword,
                                          onObfsPasswordChange = onObfsPasswordChange,
                                          portHoppingEnabled = portHoppingEnabled,
                                          onPortHoppingEnabledChange = onPortHoppingEnabledChange,
                                          portHoppingValue = portHoppingValue,
                                          onPortHoppingValueChange = onPortHoppingValueChange,
                                          transport = xrayTransport,
                                          onTransportChange = onXrayTransportChange,
                                          isTransportExpanded = isXrayTransportExpanded,
                                          onTransportExpandedChange = onXrayTransportExpandedChange,
                                          transportHost = xrayHost,
                                          onTransportHostChange = onXrayHostChange,
                                          transportPath = xrayPath,
                                          onTransportPathChange = onXrayPathChange,
                                          serviceName = xrayServiceName,
                                          onServiceNameChange = onXrayServiceNameChange,
                                          xhttpMode = xrayXhttpMode,
                                          onXrayXhttpModeChange = onXrayXhttpModeChange,
                                          isXhttpModeExpanded = isXrayXhttpModeExpanded,
                                          onXhttpModeExpandedChange = onXrayXhttpModeExpandedChange,
                                          accentColor = accentColor,
                                          hazeState = hazeState
                                      )
                                 }
                             }
                            WizardStep.PROGRESS -> {
                                SetupProgressStep(
                                    status = setupStatus,
                                    progress = setupProgress,
                                    error = setupError,
                                    accentColor = accentColor,
                                    onBackClick = onBack
                                )
                            }
                            WizardStep.SUCCESS -> {
                                SetupSuccessStep(
                                    onGoHomeClick = onGoHomeClick,
                                    accentColor = accentColor
                                )
                            }
                            WizardStep.FLARE_TARIFFS -> {
                                Text(
                                    text = I18n.strings.servers_tariff_title,
                                    fontFamily = GeologicaRegular,
                                    fontSize = 13.sp,
                                    color = FlareTheme.colors.textSecondary,
                                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                                )
                                TariffCard(
                                    title = I18n.strings.tariff_free_title,
                                    description = I18n.strings.tariff_free_desc,
                                    price = I18n.strings.tariff_free_price,
                                    isSelected = selectedTariff == TariffType.FREE,
                                    accentColor = accentColor,
                                    onClick = { onTariffSelect(TariffType.FREE) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TariffCard(
                                    title = I18n.strings.tariff_premium_title,
                                    description = I18n.strings.tariff_premium_desc,
                                    price = I18n.strings.tariff_premium_price,
                                    isSelected = selectedTariff == TariffType.PREMIUM,
                                    accentColor = accentColor,
                                    onClick = { onTariffSelect(TariffType.PREMIUM) }
                                )
                                

                            }
                            WizardStep.FLARE_PROGRESS -> {
                                FlareProgressStep(
                                    status = setupStatus.ifEmpty { I18n.strings.wizard_setup_free_title },
                                    accentColor = accentColor
                                )
                            }
                            WizardStep.FLARE_FREE_AUTH_PROMPT -> {
                                FreeAuthPromptStep(
                                    accentColor = accentColor,
                                    onWithoutAuthClick = onFreeWithoutAuthClick,
                                    onWithAuthClick = onFreeWithAuthClick
                                )
                            }
                            WizardStep.FLARE_AUTH -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = I18n.strings.wizard_setup_auth_title,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 15.sp,
                                        color = FlareTheme.colors.textPrimary,
                                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
                                    )
                                    
                                    val onAuthSuccessCallback: () -> Unit = if (selectedTariff == TariffType.FREE) onFreeAuthSuccess else onPremiumAuthSuccess
                                    AuthFlowSection(
                                        authManager = authManager,
                                        onAuthSuccess = onAuthSuccessCallback
                                    )
                                }
                            }
                            WizardStep.FLARE_BUY -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = I18n.strings.wizard_setup_buy_title,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 15.sp,
                                        color = FlareTheme.colors.textPrimary,
                                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
                                    )

                                    FlareCard(
                                        cornerType = DisplayItem.CornerType.ALL,
                                        paddingHorizontal = 20.dp,
                                        paddingVertical = 28.dp,
                                        borderColor = accentColor.copy(alpha = 0.3f),
                                        borderWidth = 0.5.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_cloud_star),
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(64.dp).padding(bottom = 24.dp)
                                            )
                                            Text(
                                                text = I18n.strings.wizard_setup_buy_action,
                                                fontFamily = GeologicaMedium,
                                                fontSize = 22.sp,
                                                color = FlareTheme.colors.textPrimary,
                                                modifier = Modifier.padding(bottom = 12.dp),
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = I18n.strings.wizard_setup_buy_desc,
                                                fontFamily = GeologicaRegular,
                                                fontSize = 15.sp,
                                                color = FlareTheme.colors.textSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                            
                                            Spacer(modifier = Modifier.height(32.dp))
                                            
                                            
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                FlareButton(
                                                    text = I18n.strings.sub_connect_btn_text,
                                                    onClick = { showTopUpMenu = true },
                                                    accentColor = accentColor,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                FlareGlassMenu(
                                                    expanded = showTopUpMenu,
                                                    onDismissRequest = { showTopUpMenu = false },
                                                    items = listOf(
                                                        GlassUtils.MenuItem(0, I18n.strings.sub_topup_telegram) {
                                                            onOpenTelegramBuyClick()
                                                            showTopUpMenu = false
                                                        },
                                                        GlassUtils.MenuItem(1, I18n.strings.sub_topup_app) {
                                                            showTopUpDialog = true
                                                            showTopUpMenu = false
                                                        }
                                                    ),
                                                    hazeState = hazeState,
                                                    alignment = Alignment.TopCenter,
                                                    offset = androidx.compose.ui.unit.IntOffset(0, 0)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            FlareButton(
                                                text = I18n.strings.wizard_setup_buy_already_purchased,
                                                onClick = onCompleteBuyClick,
                                                accentColor = accentColor,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                            WizardStep.FLARE_SUCCESS -> {
                                val titleText = if (isFreeSuccess) I18n.strings.servers_subscription_added_title else I18n.strings.servers_subscription_failed_title
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = titleText,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 15.sp,
                                        color = FlareTheme.colors.textPrimary,
                                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
                                    )

                                    FlareCard(
                                        cornerType = DisplayItem.CornerType.ALL,
                                        paddingHorizontal = 20.dp,
                                        paddingVertical = 28.dp,
                                        borderColor = if (isFreeSuccess) FlareTheme.colors.connectedGreen.copy(alpha = 0.3f) else FlareTheme.colors.disconnectedRed.copy(alpha = 0.3f),
                                        borderWidth = 0.5.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isFreeSuccess) R.drawable.ic_check else R.drawable.ic_close),
                                                contentDescription = null,
                                                tint = if (isFreeSuccess) FlareTheme.colors.connectedGreen else FlareTheme.colors.disconnectedRed,
                                                modifier = Modifier.size(64.dp).padding(bottom = 24.dp)
                                            )
                                            
                                            Text(
                                                text = if (isFreeSuccess) I18n.strings.tariff_success_title else I18n.strings.tariff_error_title,
                                                fontFamily = GeologicaMedium,
                                                fontSize = 22.sp,
                                                color = FlareTheme.colors.textPrimary,
                                                modifier = Modifier.padding(bottom = 12.dp),
                                                textAlign = TextAlign.Center
                                            )
                                            
                                            Text(
                                                text = if (isFreeSuccess) I18n.strings.tariff_success_desc else (freeError ?: I18n.strings.tariff_error_desc),
                                                fontFamily = GeologicaRegular,
                                                fontSize = 15.sp,
                                                color = FlareTheme.colors.textSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                            
                                            Spacer(modifier = Modifier.height(32.dp))
                                            
                                            FlareButton(
                                                text = I18n.strings.onboarding_btn_go_main,
                                                onClick = onGoHomeClick,
                                                accentColor = accentColor,
                                                icon = R.drawable.ic_arrow_left
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        
        val showBackButton = currentStep != WizardStep.CARDS || selectedServerType != null
        FlareTopBar(
            title = I18n.strings.label_servers,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = if (showBackButton) onBack else null
        )
        
        
        if (showTopUpDialog) {
            TopUpDialog(
                viewModel = subscriptionViewModel,
                onDismiss = {
                    showTopUpDialog = false
                    subscriptionViewModel.resetPaymentState()
                },
                hazeState = hazeState
            )
        }
    }
}
