package flare.client.app.ui

import flare.client.app.ui.i18n.I18n


import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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


private val GeologicaMedium = FontFamily(Font(R.font.geologica_medium, FontWeight.Medium))
private val GeologicaRegular = FontFamily(Font(R.font.geologica_regular, FontWeight.Normal))

@Composable
fun BasicSettingsScreen(
    
    isSplitTunnelingEnabled: Boolean,
    onSplitTunnelingChange: (Boolean) -> Unit,
    splitTunnelingDesc: String,
    onChangeAppsClick: () -> Unit,
    isChangeAppsLoading: Boolean,
    
    
    isAutostartEnabled: Boolean,
    onAutostartChange: (Boolean) -> Unit,
    
    
    isStatusNotificationEnabled: Boolean,
    onStatusNotificationChange: (Boolean) -> Unit,
    isNotificationSpeedEnabled: Boolean,
    onNotificationSpeedChange: (Boolean) -> Unit,
    isBestProfileNotifEnabled: Boolean,
    onBestProfileNotifChange: (Boolean) -> Unit,
    
    
    isCoreLogEnabled: Boolean,
    onCoreLogChange: (Boolean) -> Unit,
    coreLogLevel: String,
    onLogLevelClick: (String) -> Unit,
    onViewJournalClick: (android.view.View) -> Unit,
    
    
    isBestProfileEnabled: Boolean,
    onBestProfileChange: (Boolean) -> Unit,
    bestProfileInterval: String,
    onBestProfileIntervalChange: (String) -> Unit,
    isBestProfileOnlyConnected: Boolean,
    onBestProfileOnlyConnectedClick: (Boolean) -> Unit,
    
    
    isAdaptiveTunnelEnabled: Boolean,
    onAdaptiveTunnelChange: (Boolean) -> Unit,
    
    
    isUpdateCheckEnabled: Boolean,
    onUpdateCheckChange: (Boolean) -> Unit,
    updateFrequency: String,
    onUpdateFrequencyClick: (String) -> Unit,
    
    onDataManagementClick: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState(),
    accentColor: Color,
    onBack: () -> Unit,
    hazeState: HazeState
) {
    val logLevelOptions = listOf("debug", "info", "warn", "error", "fatal", "none")
    
    val bestProfileOnlyConnectedOptions = listOf(
        I18n.strings.option_enable to true,
        I18n.strings.option_disable to false
    )

    val updateFrequencyOptions = listOf(
        I18n.strings.update_freq_daily to "daily",
        I18n.strings.update_freq_weekly to "weekly",
        I18n.strings.update_freq_monthly to "monthly"
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
                
                
                FlareSectionHeader(I18n.strings.settings_label_split_tunneling)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_split_tunneling,
                        checked = isSplitTunnelingEnabled,
                        onCheckedChange = onSplitTunnelingChange,
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = if (isSplitTunnelingEnabled) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                    )
                    
                    AnimatedVisibility(visible = isSplitTunnelingEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FlareTheme.colors.bgItem)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = splitTunnelingDesc,
                                fontFamily = GeologicaRegular,
                                fontSize = 14.sp,
                                color = FlareTheme.colors.textPrimary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.material3.Button(
                                    onClick = { onChangeAppsClick() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    ),
                                    enabled = !isChangeAppsLoading
                                ) {
                                    if (isChangeAppsLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = I18n.strings.settings_btn_change,
                                            fontFamily = GeologicaMedium,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                FlareSectionHeader(I18n.strings.settings_header_autostart)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_autostart,
                        checked = isAutostartEnabled,
                        onCheckedChange = onAutostartChange,
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = DisplayItem.CornerType.ALL
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                FlareSectionHeader(I18n.strings.settings_header_notifications)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_status,
                        checked = isStatusNotificationEnabled,
                        onCheckedChange = onStatusNotificationChange,
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = DisplayItem.CornerType.TOP
                    )
                    AnimatedVisibility(visible = isStatusNotificationEnabled) {
                        FlareSettingsToggleItem(
                            title = I18n.strings.settings_label_notification_speed,
                            checked = isNotificationSpeedEnabled,
                            onCheckedChange = onNotificationSpeedChange,
                            hazeState = hazeState,
                            accentColor = accentColor,
                            cornerType = DisplayItem.CornerType.NONE
                        )
                    }
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_best_profile_notif,
                        checked = isBestProfileNotifEnabled,
                        onCheckedChange = onBestProfileNotifChange,
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = DisplayItem.CornerType.BOTTOM
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                


                
                FlareSectionHeader(I18n.strings.settings_header_logging)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_core_log,
                        checked = isCoreLogEnabled,
                        onCheckedChange = onCoreLogChange,
                        hazeState = hazeState,
                        description = I18n.strings.settings_desc_logging,
                        accentColor = accentColor,
                        cornerType = if (isCoreLogEnabled) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                    )
                    
                    AnimatedVisibility(visible = isCoreLogEnabled) {
                        Column {
                            FlareSettingsValueItem(
                                title = I18n.strings.settings_label_core_log_level,
                                value = coreLogLevel,
                                menuItems = logLevelOptions.mapIndexed { i, level ->
                                    flare.client.app.util.GlassUtils.MenuItem(i, level.replaceFirstChar { it.uppercase() }) {
                                        onLogLevelClick(level)
                                    }
                                },
                                hazeState = hazeState,
                                accentColor = accentColor,
                                cornerType = DisplayItem.CornerType.NONE
                            )
                            FlareSettingsItem(
                                title = I18n.strings.settings_btn_journal,
                                onClick = { view -> onViewJournalClick(view) },
                                hazeState = hazeState,
                                cornerType = DisplayItem.CornerType.BOTTOM
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                FlareSectionHeader(I18n.strings.settings_header_best_profile)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_best_profile,
                        checked = isBestProfileEnabled,
                        onCheckedChange = onBestProfileChange,
                        hazeState = hazeState,
                        description = I18n.strings.settings_desc_best_profile,
                        accentColor = accentColor,
                        cornerType = if (isBestProfileEnabled) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                    )
                    
                    AnimatedVisibility(visible = isBestProfileEnabled) {
                        Column {
                            FlareSettingsInputItem(
                                title = I18n.strings.settings_label_best_profile_interval,
                                value = bestProfileInterval,
                                onValueChange = onBestProfileIntervalChange,
                                hazeState = hazeState,
                                suffix = I18n.strings.label_seconds_short,
                                accentColor = accentColor,
                                cornerType = DisplayItem.CornerType.NONE
                            )
                            FlareSettingsValueItem(
                                title = I18n.strings.settings_label_best_profile_only_connected,
                                value = if (isBestProfileOnlyConnected) I18n.strings.option_enable else I18n.strings.option_disable,
                                menuItems = bestProfileOnlyConnectedOptions.mapIndexed { i, opt ->
                                    flare.client.app.util.GlassUtils.MenuItem(i, opt.first) {
                                        onBestProfileOnlyConnectedClick(opt.second)
                                    }
                                },
                                hazeState = hazeState,
                                accentColor = accentColor,
                                cornerType = DisplayItem.CornerType.BOTTOM
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_adaptive_tunnel,
                        checked = isAdaptiveTunnelEnabled,
                        onCheckedChange = onAdaptiveTunnelChange,
                        hazeState = hazeState,
                        description = I18n.strings.settings_desc_adaptive_tunnel,
                        accentColor = accentColor,
                        cornerType = DisplayItem.CornerType.ALL
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                FlareSectionHeader(I18n.strings.settings_header_updates)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_update_check,
                        checked = isUpdateCheckEnabled,
                        onCheckedChange = onUpdateCheckChange,
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = if (isUpdateCheckEnabled) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                    )
                    
                    AnimatedVisibility(visible = isUpdateCheckEnabled) {
                        val displayValue = updateFrequencyOptions.find { it.second == updateFrequency }?.first ?: I18n.strings.update_freq_daily
                        FlareSettingsValueItem(
                            title = I18n.strings.settings_label_update_frequency,
                            value = displayValue,
                            menuItems = updateFrequencyOptions.mapIndexed { i, opt ->
                                flare.client.app.util.GlassUtils.MenuItem(i, opt.first) {
                                    onUpdateFrequencyClick(opt.second)
                                }
                            },
                            hazeState = hazeState,
                            accentColor = accentColor,
                            cornerType = DisplayItem.CornerType.BOTTOM
                        )
                    }
                }
                Text(
                    text = I18n.strings.settings_desc_update_check,
                    fontFamily = GeologicaRegular,
                    fontSize = 13.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlareSectionHeader(I18n.strings.settings_header_data_mgmt)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsItem(
                        title = I18n.strings.settings_label_data_mgmt,
                        value = I18n.strings.settings_btn_data_mgmt,
                        hazeState = hazeState,
                        cornerType = DisplayItem.CornerType.ALL,
                        onClick = { onDataManagementClick() }
                    )
                }
                Text(
                    text = I18n.strings.settings_desc_data_mgmt,
                    fontFamily = GeologicaRegular,
                    fontSize = 13.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                )
            }
        }

        
        FlareTopBar(
            title = I18n.strings.settings_basic_title,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBack
        )
    }
}
