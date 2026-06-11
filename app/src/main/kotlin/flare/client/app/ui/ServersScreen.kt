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
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.*
import flare.client.app.ui.theme.FlareTheme


private val GeologicaMedium = FontFamily(Font(R.font.geologica_medium, FontWeight.Medium))
private val GeologicaRegular = FontFamily(Font(R.font.geologica_regular, FontWeight.Normal))

@Composable
fun ServersScreen(
    currentStep: WizardStep,
    selectedServerType: ServerType?,
    accentColor: Color,
    isFreeSuccess: Boolean = true,
    onFlareServersClick: () -> Unit,
    onCreateServerClick: () -> Unit,
    
    selectedTariff: TariffType?,
    onTariffSelect: (TariffType) -> Unit,
    
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
    
    
    setupStatus: String,
    setupProgress: Float,
    setupError: String?,
    
    onGoHomeClick: () -> Unit,
    onBack: () -> Unit,
    onNextClick: () -> Unit,
    isSshConfigValid: Boolean,
    hazeState: HazeState
) {
    BackHandler(enabled = currentStep != WizardStep.CARDS || selectedServerType != null || selectedTariff != null) {
        onBack()
    }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {

        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .hazeSource(state = hazeState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                                         accentColor = accentColor
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
                                    title = I18n.strings.tariff_plus_title,
                                    description = I18n.strings.tariff_plus_desc,
                                    price = I18n.strings.tariff_plus_price,
                                    isSelected = selectedTariff == TariffType.PLUS,
                                    accentColor = accentColor,
                                    onClick = { onTariffSelect(TariffType.PLUS) }
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
                                    status = I18n.strings.wizard_setup_free_title,
                                    accentColor = accentColor
                                )
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
                                                text = if (isFreeSuccess) I18n.strings.tariff_success_desc else I18n.strings.tariff_error_desc,
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
    }
}

@Composable
fun ServerActionCard(
    title: String,
    description: String,
    icon: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.15f) else FlareTheme.colors.bgItem.copy(alpha = 0.85f),
        animationSpec = tween(250),
        label = "actionCardBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "actionCardBorder"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = tween(200),
        label = "actionCardScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBgColor)
            .border(0.5.dp, animatedBorderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                fontFamily = GeologicaMedium,
                fontSize = 16.sp,
                color = FlareTheme.colors.textPrimary
            )
            Text(
                text = description,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp,
                color = FlareTheme.colors.textSecondary
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun SshConfigStep(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    ip: String,
    onIpChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    user: String,
    onUserChange: (String) -> Unit,
    pass: String,
    onPassChange: (String) -> Unit,
    onSshKeyClick: () -> Unit,
    hazeState: HazeState,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_ssh_title

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titleText,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 16.dp,
            paddingVertical = 20.dp,
            borderColor = accentColor.copy(alpha = 0.15f),
            borderWidth = 1.dp
        ) {
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_profile_name,
                value = profileName,
                onValueChange = onProfileNameChange,
                accentColor = accentColor,
                isValid = profileName.isNotBlank(),
                icon = R.drawable.ic_cloud,
                hint = I18n.strings.servers_ssh_profile_name_hint
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardIpPortField(
                ipValue = ip,
                onIpChange = onIpChange,
                portValue = port,
                onPortChange = onPortChange,
                accentColor = accentColor,
                icon = R.drawable.ic_language_filled
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_username,
                value = user,
                onValueChange = onUserChange,
                accentColor = accentColor,
                isValid = user.isNotBlank(),
                icon = R.drawable.ic_suitcase,
                hint = "root"
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_password,
                value = pass,
                onValueChange = onPassChange,
                accentColor = accentColor,
                isValid = pass.isNotBlank(),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                icon = R.drawable.ic_vpn_key,
                hint = "••••••••"
            )
        }
    }
}

@Composable
fun XrayConfigStep(
    selectedProtocol: SelectedProtocol,
    port: String,
    onPortChange: (String) -> Unit,
    sni: String,
    onSniChange: (String) -> Unit,
    obfsPassword: String,
    onObfsPasswordChange: (String) -> Unit,
    portHoppingEnabled: Boolean,
    onPortHoppingEnabledChange: (Boolean) -> Unit,
    portHoppingValue: String,
    onPortHoppingValueChange: (String) -> Unit,
    accentColor: Color
) {
    val isHy2 = selectedProtocol == SelectedProtocol.HYSTERIA2
    val portLabel = if (isHy2) I18n.strings.servers_hysteria2_port_label else I18n.strings.servers_xray_port_label
    val portHint = if (isHy2) I18n.strings.wizard_hysteria2_port_hint else I18n.strings.wizard_xray_port_hint
    val sniLabel = if (isHy2) I18n.strings.servers_hysteria2_sni_label else I18n.strings.servers_xray_sni_label
    val sniHint = if (isHy2) I18n.strings.wizard_hysteria2_sni_hint else I18n.strings.wizard_xray_sni_hint

    val titleText = if (isHy2) I18n.strings.servers_hysteria2_title else I18n.strings.servers_xray_title

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titleText,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 16.dp,
            paddingVertical = 20.dp,
            borderColor = accentColor.copy(alpha = 0.15f),
            borderWidth = 1.dp
        ) {
            FlareWizardInputField(
                title = portLabel,
                value = port,
                onValueChange = onPortChange,
                accentColor = accentColor,
                isValid = port.isNotBlank(),
                hint = portHint,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                icon = R.drawable.ic_port
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = sniLabel,
                value = sni,
                onValueChange = onSniChange,
                accentColor = accentColor,
                isValid = sni.isNotBlank(),
                hint = sniHint,
                icon = R.drawable.ic_language
            )

            if (isHy2) {
                Spacer(modifier = Modifier.height(20.dp))
                
                FlareWizardInputField(
                    title = I18n.strings.servers_hysteria2_obfs_pass_label,
                    value = obfsPassword,
                    onValueChange = onObfsPasswordChange,
                    accentColor = accentColor,
                    isValid = obfsPassword.isNotBlank(),
                    hint = I18n.strings.wizard_hysteria2_obfs_pass_hint,
                    icon = R.drawable.ic_vpn_key
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = I18n.strings.servers_hysteria2_port_hopping_label,
                        fontFamily = GeologicaMedium,
                        fontSize = 14.sp,
                        color = FlareTheme.colors.textPrimary
                    )
                    androidx.compose.material3.Switch(
                        checked = portHoppingEnabled,
                        onCheckedChange = onPortHoppingEnabledChange,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = FlareTheme.colors.textSecondary,
                            uncheckedTrackColor = FlareTheme.colors.bgItem.copy(alpha = 0.5f)
                        )
                    )
                }
                
                AnimatedVisibility(
                    visible = portHoppingEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FlareWizardInputField(
                                    title = I18n.strings.wizard_hysteria2_port_hopping_hint,
                                    value = portHoppingValue,
                                    onValueChange = onPortHoppingValueChange,
                                    accentColor = accentColor,
                                    isValid = portHoppingValue.isNotBlank(),
                                    hint = "e.g. 20000-50000",
                                    icon = R.drawable.ic_port
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            androidx.compose.material3.Button(
                                onClick = { 
                                    val start = (20000..40000).random()
                                    val end = start + 10000
                                    onPortHoppingValueChange("$start-$end")
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.15f),
                                    contentColor = accentColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 24.dp).height(50.dp)
                            ) {
                                Text(
                                    text = I18n.strings.servers_hysteria2_port_hopping_auto,
                                    fontFamily = GeologicaMedium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupProgressStep(
    status: String,
    progress: Float,
    error: String?,
    accentColor: Color,
    onBackClick: () -> Unit
) {
    val isError = error != null
    val titleText = I18n.strings.servers_setup_progress_title

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
            borderColor = if (isError) FlareTheme.colors.disconnectedRed.copy(alpha = 0.3f) else accentColor.copy(alpha = 0.15f),
            borderWidth = 1.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isError) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        tint = FlareTheme.colors.disconnectedRed,
                        modifier = Modifier.size(64.dp).padding(bottom = 24.dp)
                    )
                }

                Text(
                    text = status,
                    fontFamily = GeologicaMedium,
                    fontSize = 17.sp,
                    color = if (isError) FlareTheme.colors.disconnectedRed else FlareTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                if (!isError) {
                    val progressColor by animateColorAsState(
                        targetValue = if (progress >= 100f) FlareTheme.colors.connectedGreen else accentColor,
                        label = "progressColor"
                    )

                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    
                    
                    ThreeJumpingDots(
                        modifier = Modifier.padding(top = 24.dp),
                        dotSize = 10.dp,
                        dotColor = accentColor,
                        dotSpacing = 8.dp
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    FlareButton(
                        text = I18n.strings.btn_cancel,
                        onClick = onBackClick,
                        accentColor = accentColor,
                        icon = R.drawable.ic_arrow_left
                    )
                }
            }
        }
    }
}

@Composable
fun SetupSuccessStep(
    onGoHomeClick: () -> Unit,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_setup_success_title

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
            borderColor = FlareTheme.colors.connectedGreen.copy(alpha = 0.3f),
            borderWidth = 1.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = FlareTheme.colors.connectedGreen,
                    modifier = Modifier.size(64.dp).padding(bottom = 24.dp)
                )
                
                Text(
                    text = I18n.strings.servers_setup_success,
                    fontFamily = GeologicaMedium,
                    fontSize = 22.sp,
                    color = FlareTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = I18n.strings.servers_setup_success_desc,
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

@Composable
fun TariffCard(
    title: String,
    description: String,
    price: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.15f) else FlareTheme.colors.bgItem
    val borderColor = if (isSelected) accentColor else FlareTheme.colors.glassStroke

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                fontFamily = GeologicaMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else FlareTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp,
                color = FlareTheme.colors.textSecondary
            )
        }
        
        Text(
            text = price,
            fontFamily = GeologicaMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) accentColor else FlareTheme.colors.textPrimary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun FlareProgressStep(
    status: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = I18n.strings.servers_tariff_title,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
        )

        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 20.dp,
            paddingVertical = 36.dp,
            borderColor = accentColor.copy(alpha = 0.15f),
            borderWidth = 0.5.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = accentColor,
                        strokeWidth = 3.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = status,
                    fontFamily = GeologicaMedium,
                    fontSize = 17.sp,
                    color = FlareTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ThreeJumpingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotSpacing: Dp = 6.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jumpingDots")
    
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 0
                -12f at 300
                0f at 600
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 200
                -12f at 500
                0f at 800
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 400
                -12f at 700
                0f at 1000
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer { translationY = dot1Offset }
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer { translationY = dot2Offset }
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer { translationY = dot3Offset }
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
fun WizardStepper(
    activeIndex: Int,
    steps: List<String>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, stepTitle ->
            val isCompleted = index < activeIndex
            val isActive = index == activeIndex
            
            val circleColor by animateColorAsState(
                targetValue = when {
                    isActive -> accentColor
                    isCompleted -> accentColor.copy(alpha = 0.5f)
                    else -> FlareTheme.colors.textSecondary.copy(alpha = 0.15f)
                },
                animationSpec = tween(300),
                label = "circleColor"
            )

            val circleSize by animateDpAsState(
                targetValue = if (isActive) 24.dp else 20.dp,
                animationSpec = tween(300),
                label = "circleSize"
            )

            val textColor = when {
                isActive -> FlareTheme.colors.textPrimary
                isCompleted -> FlareTheme.colors.textSecondary
                else -> FlareTheme.colors.textSecondary.copy(alpha = 0.4f)
            }

            val textWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(circleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                fontFamily = GeologicaMedium,
                                fontSize = if (isActive) 11.sp else 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.White else FlareTheme.colors.textSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stepTitle,
                    fontFamily = GeologicaRegular,
                    fontSize = 10.sp,
                    fontWeight = textWeight,
                    color = textColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            if (index < steps.size - 1) {
                val lineColor by animateColorAsState(
                    targetValue = if (isCompleted) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.15f),
                    animationSpec = tween(300),
                    label = "lineColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 11.dp)
                        .height(2.dp)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
fun FlareWizardSelectField(
    title: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    optionTitles: List<String>,
    onOptionSelected: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    icon: Int? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        val borderColor = if (expanded) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
        val bgColor = if (expanded) accentColor.copy(alpha = 0.05f) else FlareTheme.colors.bgItem.copy(alpha = 0.5f)

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable { onExpandedChange(true) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = if (expanded) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 12.dp).size(20.dp)
                    )
                }

                Text(
                    text = value,
                    fontFamily = GeologicaMedium,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = null,
                    tint = FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            FlareGlassMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                items = options.mapIndexed { index, option ->
                    flare.client.app.util.GlassUtils.MenuItem(index, optionTitles[index]) {
                        onOptionSelected(option)
                    }
                },
                alignment = Alignment.CenterEnd
            )
        }
    }
}

@Composable
fun ShadowsocksConfigStep(
    port: String,
    onPortChange: (String) -> Unit,
    sni: String,
    onSniChange: (String) -> Unit,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_shadowsocks_title

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titleText,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 16.dp,
            paddingVertical = 20.dp,
            borderColor = accentColor.copy(alpha = 0.15f),
            borderWidth = 1.dp
        ) {
            FlareWizardInputField(
                title = I18n.strings.servers_shadowsocks_port_label,
                value = port,
                onValueChange = onPortChange,
                accentColor = accentColor,
                isValid = port.isNotBlank(),
                hint = I18n.strings.wizard_shadowsocks_port_hint,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                icon = R.drawable.ic_port
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            FlareWizardInputField(
                title = I18n.strings.servers_shadowsocks_sni_label,
                value = sni,
                onValueChange = onSniChange,
                accentColor = accentColor,
                isValid = sni.isNotBlank(),
                hint = I18n.strings.wizard_shadowsocks_sni_hint,
                icon = R.drawable.ic_language
            )
        }
    }
}

@Composable
fun WireGuardConfigStep(
    port: String,
    onPortChange: (String) -> Unit,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_wireguard_title

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titleText,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 16.dp,
            paddingVertical = 20.dp,
            borderColor = accentColor.copy(alpha = 0.15f),
            borderWidth = 1.dp
        ) {
            FlareWizardInputField(
                title = I18n.strings.servers_wireguard_port_label,
                value = port,
                onValueChange = onPortChange,
                accentColor = accentColor,
                isValid = port.isNotBlank(),
                hint = I18n.strings.wizard_wireguard_port_hint,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                icon = R.drawable.ic_port
            )
        }
    }
}
