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
import androidx.compose.runtime.Composable
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
fun SubscriptionsScreen(
    isSubIntervalEnabled: Boolean,
    onSubIntervalChange: (Boolean) -> Unit,
    isAutoUpdateEnabled: Boolean,
    onAutoUpdateChange: (Boolean) -> Unit,
    updateInterval: String,
    onUpdateIntervalChange: (String) -> Unit,
    userAgent: String,
    onUserAgentClick: (String) -> Unit,
    isHwidEnabled: Boolean,
    onHwidChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState
) {
    val isDark = FlareTheme.colors.isDark

    val standardUserAgents = listOf("Happ/3.21.1", "FlareVPN/1.2.0", "v2rayNG/2.1.5", "v2rayTUN/5.23.73", "sing-box")

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
                    .padding(top = 80.dp, bottom = 160.dp)
                    .padding(horizontal = 20.dp)
            ) {
                FlareSectionHeader(text = I18n.strings.settings_label_auto_update)
                
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_use_sub_interval,
                        checked = isSubIntervalEnabled,
                        onCheckedChange = onSubIntervalChange,
                        hazeState = hazeState,
                        description = I18n.strings.settings_desc_use_sub_interval,
                        accentColor = accentColor,
                        cornerType = DisplayItem.CornerType.TOP
                    )
                    
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_auto_update,
                        checked = isAutoUpdateEnabled,
                        onCheckedChange = onAutoUpdateChange,
                        hazeState = hazeState,
                        description = I18n.strings.settings_desc_auto_update,
                        accentColor = accentColor,
                        cornerType = if (isAutoUpdateEnabled) DisplayItem.CornerType.NONE else DisplayItem.CornerType.BOTTOM
                    )
                    
                    AnimatedVisibility(visible = isAutoUpdateEnabled) {
                        FlareSettingsInputItem(
                            title = I18n.strings.settings_label_update_every,
                            value = updateInterval,
                            onValueChange = onUpdateIntervalChange,
                            hazeState = hazeState,
                            accentColor = accentColor,
                            cornerType = DisplayItem.CornerType.BOTTOM
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FlareSectionHeader(text = I18n.strings.settings_header_app)
                
                val isCustomUa = userAgent !in standardUserAgents
                val displayedUaValue = if (isCustomUa) "custom" else userAgent

                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsValueItem(
                        title = I18n.strings.settings_label_user_agent,
                        value = displayedUaValue,
                        menuItems = (standardUserAgents + "custom").mapIndexed { i, agent ->
                            flare.client.app.util.GlassUtils.MenuItem(i, agent) {
                                onUserAgentClick(agent)
                            }
                        },
                        hazeState = hazeState,
                        accentColor = accentColor,
                        cornerType = if (isCustomUa) DisplayItem.CornerType.TOP else DisplayItem.CornerType.ALL
                    )

                    AnimatedVisibility(
                        visible = isCustomUa,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val customUaValue = if (userAgent == "custom") "" else userAgent
                        FlareSettingsInputItem(
                            title = "Custom U-A",
                            value = customUaValue,
                            onValueChange = { newValue ->
                                onUserAgentClick(newValue.ifEmpty { "custom" })
                            },
                            hint = "Flare/1.2.0",
                            hazeState = hazeState,
                            accentColor = accentColor,
                            cornerType = DisplayItem.CornerType.BOTTOM
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FlareSectionHeader(text = I18n.strings.settings_header_hwid)
                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    FlareSettingsToggleItem(
                        title = I18n.strings.settings_label_send_hwid,
                        checked = isHwidEnabled,
                        onCheckedChange = onHwidChange,
                        hazeState = hazeState,
                        description = I18n.strings.settings_desc_hwid,
                        accentColor = accentColor,
                        cornerType = DisplayItem.CornerType.ALL
                    )
                }
            }
        }

        
        FlareTopBar(
            title = I18n.strings.settings_subscriptions_title,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBack
        )
    }
}
