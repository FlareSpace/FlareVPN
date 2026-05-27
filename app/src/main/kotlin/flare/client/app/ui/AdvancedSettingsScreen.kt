package flare.client.app.ui

import flare.client.app.ui.i18n.I18n


import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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



@Composable
fun AdvancedSettingsScreen(
    
    isFragmentationEnabled: Boolean,
    onFragmentationChange: (Boolean) -> Unit,
    packetType: String,
    onPacketTypeClick: (String) -> Unit,
    fragmentInterval: String,
    onFragmentIntervalChange: (String) -> Unit,

    
    isMuxEnabled: Boolean,
    onMuxChange: (Boolean) -> Unit,
    muxProtocol: String,
    onMuxProtocolClick: (String) -> Unit,
    muxMaxStreams: String,
    onMuxMaxStreamsChange: (String) -> Unit,
    muxPadding: Boolean,
    onMuxPaddingClick: (Boolean) -> Unit,

    
    remoteDnsUrl: String,
    onRemoteDnsUrlChange: (String) -> Unit,

    
    isFakeIpEnabled: Boolean,
    onFakeIpChange: (Boolean) -> Unit,

    
    mtu: String,
    onMtuChange: (String) -> Unit,

    
    tunStack: String,
    onTunStackClick: (String) -> Unit,

    isResetChainOnDisconnect: Boolean,
    onResetChainOnDisconnectChange: (Boolean) -> Unit,

    accentColor: Color,
    onBack: () -> Unit,
    hazeState: HazeState
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isTestingMtu by remember { mutableStateOf(false) }

    val packetTypeOptions = listOf(
        I18n.strings.option_enable to "tlshello",
        I18n.strings.option_disable to "disabled"
    )

    val muxProtocolOptions = listOf("smux", "yamux", "h2mux")

    val muxPaddingOptions = listOf(
        I18n.strings.option_yes to true,
        I18n.strings.option_no to false
    )

    val tunStackOptions = listOf(
        I18n.strings.settings_label_stack.format("mixed") to "mixed",
        I18n.strings.settings_label_stack.format("gvisor") to "gvisor",
        I18n.strings.settings_label_stack.format("system") to "system"
    )

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
                    .padding(top = 80.dp, bottom = 160.dp)
                    .padding(horizontal = 20.dp)
            ) {
                
                
                FlareSectionHeader(I18n.strings.settings_label_fragmentation)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_fragmentation,
                        checked = isFragmentationEnabled,
                        onCheckedChange = onFragmentationChange,
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = if (isFragmentationEnabled) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                    )
                    
                    AnimatedVisibility(visible = isFragmentationEnabled) {
                        Column {
                            FlareSettingsValueItem(
                                title = I18n.strings.settings_label_packet_type,
                                value = if (packetType != "disabled") I18n.strings.option_enable else I18n.strings.option_disable,
                                menuItems = packetTypeOptions.mapIndexed { i, opt ->
                                    flare.client.app.util.GlassUtils.MenuItem(i, opt.first) {
                                        onPacketTypeClick(opt.second)
                                    }
                                },
                                hazeState = hazeState,
                                accentColor = accentColor,
                                cornerType = DisplayItem.CornerType.NONE
                            )
                            
                            AnimatedVisibility(visible = packetType != "disabled") {
                                FlareSettingsInputItem(
                                    title = I18n.strings.settings_label_fragment_interval,
                                    value = fragmentInterval,
                                    onValueChange = onFragmentIntervalChange,
                                    hazeState = hazeState,
                                    accentColor = accentColor,
                                    cornerType = DisplayItem.CornerType.BOTTOM
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = I18n.strings.fragment_desc,
                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp, start = 4.dp)
                )

                
                FlareSectionHeader(I18n.strings.settings_label_mux)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_mux,
                        checked = isMuxEnabled,
                        onCheckedChange = onMuxChange,
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = if (isMuxEnabled) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                    )
                    
                    AnimatedVisibility(visible = isMuxEnabled) {
                        Column {
                            FlareSettingsValueItem(
                                title = I18n.strings.settings_label_mux_protocol,
                                value = muxProtocol,
                                menuItems = muxProtocolOptions.mapIndexed { i, opt ->
                                    flare.client.app.util.GlassUtils.MenuItem(i, opt) {
                                        onMuxProtocolClick(opt)
                                    }
                                },
                                hazeState = hazeState,
                                accentColor = accentColor,
                                cornerType = DisplayItem.CornerType.NONE
                            )
                            FlareSettingsInputItem(
                                title = I18n.strings.settings_label_mux_streams,
                                value = muxMaxStreams,
                                onValueChange = onMuxMaxStreamsChange,
                                hazeState = hazeState,
                                accentColor = accentColor,
                                cornerType = DisplayItem.CornerType.NONE
                            )
                            FlareSettingsValueItem(
                                title = I18n.strings.settings_label_mux_padding,
                                value = if (muxPadding) I18n.strings.option_yes else I18n.strings.option_no,
                                menuItems = muxPaddingOptions.mapIndexed { i, opt ->
                                    flare.client.app.util.GlassUtils.MenuItem(i, opt.first) {
                                        onMuxPaddingClick(opt.second)
                                    }
                                },
                                hazeState = hazeState,
                                accentColor = accentColor,
                                cornerType = DisplayItem.CornerType.BOTTOM
                            )
                        }
                    }
                }
                Text(
                    text = I18n.strings.mux_desc,
                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp, start = 4.dp)
                )

                
                FlareSectionHeader(I18n.strings.settings_label_remote_dns)
                FlareSettingsInputItem(
                    title = I18n.strings.settings_label_dns_url,
                    value = remoteDnsUrl,
                    onValueChange = onRemoteDnsUrlChange,
                    hazeState = hazeState,
                    hint = I18n.strings.settings_hint_dns_url,
                    accentColor = accentColor,
                    cornerType = DisplayItem.CornerType.ALL
                )
                Spacer(modifier = Modifier.height(24.dp))

                
                FlareSectionHeader(I18n.strings.settings_label_fake_ip)
                FlareSettingsToggleItem(
                    title = I18n.strings.settings_label_use_fake_ip,
                    checked = isFakeIpEnabled,
                    onCheckedChange = onFakeIpChange,
                    hazeState = hazeState,
                    accentColor = accentColor,
                    cornerType = DisplayItem.CornerType.ALL
                )
                Text(
                    text = I18n.strings.fakeip_desc,
                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp, start = 4.dp)
                )

                
                FlareSectionHeader(I18n.strings.settings_header_chain)
                FlareSettingsToggleItem(
                    title = I18n.strings.settings_label_reset_chain,
                    checked = isResetChainOnDisconnect,
                    onCheckedChange = onResetChainOnDisconnectChange,
                    hazeState = hazeState,
                    accentColor = accentColor,
                    cornerType = DisplayItem.CornerType.ALL
                )
                Text(
                    text = I18n.strings.settings_desc_reset_chain,
                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp, start = 4.dp)
                )

                FlareSectionHeader(I18n.strings.settings_label_mtu_title)
                FlareSettingsInputItem(
                    title = I18n.strings.settings_label_mtu,
                    value = mtu,
                    onValueChange = onMtuChange,
                    hazeState = hazeState,
                    hint = "1500",
                    accentColor = accentColor,
                    cornerType = DisplayItem.CornerType.ALL,
                    action = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(
                                    BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                                    RoundedCornerShape(50.dp)
                                )
                                .clickable(enabled = !isTestingMtu) {
                                    isTestingMtu = true
                                    scope.launch {
                                        val discoveredMtu = withContext(Dispatchers.IO) {
                                            val startTime = System.currentTimeMillis()
                                            var optimalMtu = 1280
                                            try {
                                                val target = "8.8.8.8"
                                                val payloads = listOf(1472, 1372)
                                                var maxPayloadSucceeded = -1
                                                for (payload in payloads) {
                                                    var process: Process? = null
                                                    try {
                                                        process = Runtime.getRuntime().exec(
                                                            arrayOf("ping", "-c", "1", "-W", "1", "-s", payload.toString(), "-M", "do", target)
                                                        )
                                                        val exitCode = process.waitFor()
                                                        if (exitCode == 0) {
                                                            maxPayloadSucceeded = payload
                                                            break
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    } finally {
                                                        process?.destroy()
                                                    }
                                                }
                                                if (maxPayloadSucceeded != -1) {
                                                    val physicalMtu = maxPayloadSucceeded + 28
                                                    optimalMtu = (physicalMtu - 80).coerceAtLeast(1280)
                                                } else {
                                                    throw Exception("Pings failed")
                                                }
                                            } catch (e: Exception) {
                                                try {
                                                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                                                    val activeNetwork = connectivityManager.activeNetwork
                                                    val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                                                    val detectedMtu = linkProperties?.mtu ?: 1500
                                                    optimalMtu = (detectedMtu - 80).coerceAtLeast(1280)
                                                } catch (ex: Exception) {
                                                    optimalMtu = 1420
                                                }
                                            }
                                            val elapsed = System.currentTimeMillis() - startTime
                                            if (elapsed < 1500) {
                                                delay(1500 - elapsed)
                                            }
                                            optimalMtu
                                        }
                                        onMtuChange(discoveredMtu.toString())
                                        isTestingMtu = false
                                        val warningText = I18n.strings.mtu_auto_warning.format(discoveredMtu.toString())
                                        flare.client.app.ui.notification.AppNotificationManager.showNotification(
                                            type = flare.client.app.ui.notification.NotificationType.WARNING,
                                            text = warningText,
                                            durationSec = 3
                                        )
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isTestingMtu,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "mtuAutoLoading"
                            ) { loading ->
                                if (loading) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = accentColor,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Text(
                                        text = I18n.strings.mtu_auto_btn,
                                        fontFamily = flare.client.app.ui.components.GeologicaMedium,
                                        fontSize = 11.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                )
                Text(
                    text = I18n.strings.mtu_desc,
                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp, start = 4.dp)
                )

                
                FlareSectionHeader(I18n.strings.settings_stack_header)
                FlareSettingsValueItem(
                    title = I18n.strings.settings_label_stack_title,
                    value = I18n.strings.settings_label_stack.format(tunStack),
                    menuItems = tunStackOptions.mapIndexed { i, opt ->
                        flare.client.app.util.GlassUtils.MenuItem(i, opt.first) {
                            onTunStackClick(opt.second)
                        }
                    },
                    hazeState = hazeState,
                    accentColor = accentColor,
                    cornerType = DisplayItem.CornerType.ALL
                )
                Text(
                    text = when (tunStack) {
                        "mixed"  -> I18n.strings.mixedstack_desc
                        "gvisor" -> I18n.strings.gvisorstack_desc
                        else     -> I18n.strings.systemstack_desc
                    },
                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp, start = 4.dp)
                )
            }
        }

        
        val isDark = FlareTheme.colors.isDark
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .hazeEffect(state = hazeState) {
                    blurRadius = 24.dp
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FlareTheme.colors.headerGradientStart, FlareTheme.colors.headerGradientEnd)
                    )
                )
                .statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp)
                .padding(top = 2.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null,
                    tint = FlareTheme.colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = I18n.strings.settings_advanced_title,
                fontFamily = flare.client.app.ui.components.GeologicaMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = FlareTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
