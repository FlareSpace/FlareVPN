package flare.client.app.ui

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.FlareSectionHeader
import flare.client.app.ui.components.FlareSettingsItem
import flare.client.app.ui.components.FlareHomeBackground
import flare.client.app.ui.components.FlareTopBar
import flare.client.app.ui.theme.FlareTheme


import flare.client.app.ui.i18n.I18n



@Composable
fun SettingsScreen(
    onBaseSettingsClick: (android.view.View) -> Unit,
    onAdvancedSettingsClick: (android.view.View) -> Unit,
    onRoutingSettingsClick: (android.view.View) -> Unit,
    onPingSettingsClick: (android.view.View) -> Unit,
    onSubscriptionsSettingsClick: (android.view.View) -> Unit,
    onVpnSubscriptionClick: (android.view.View) -> Unit,
    onThemeSettingsClick: (android.view.View) -> Unit,
    onLanguageSettingsClick: (android.view.View) -> Unit,
    isGradientEnabled: Boolean,
    isAnimationEnabled: Boolean,
    gradientSpeed: Float,
    hazeState: HazeState,
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    val strings = I18n.strings

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .let { if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) it.hazeSource(state = hazeState) else it }
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
                FlareSectionHeader(text = strings.settings_header_vpn)
                
                FlareSettingsItem(
                    title = strings.settings_btn_base,
                    icon = R.drawable.ic_settings_wrench_filled,
                    cornerType = DisplayItem.CornerType.TOP,
                    onClick = onBaseSettingsClick,
                    iconBgColor = Color(0xFF34C759)
                )
                FlareSettingsItem(
                    title = strings.settings_btn_advanced,
                    icon = R.drawable.ic_settings_box_filled,
                    cornerType = DisplayItem.CornerType.NONE,
                    onClick = onAdvancedSettingsClick,
                    iconBgColor = Color(0xFFFFB300)
                )
                FlareSettingsItem(
                    title = strings.settings_item_routing,
                    icon = R.drawable.ic_routing_filled,
                    cornerType = DisplayItem.CornerType.BOTTOM,
                    onClick = onRoutingSettingsClick,
                    iconBgColor = Color(0xFF007AFF)
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlareSectionHeader(text = strings.settings_header_app)
                
                FlareSettingsItem(
                    title = strings.settings_item_ping,
                    icon = R.drawable.ic_speedometer_filled,
                    cornerType = DisplayItem.CornerType.TOP,
                    onClick = onPingSettingsClick,
                    iconBgColor = Color(0xFF9B51E0)
                )
                FlareSettingsItem(
                    title = strings.settings_item_subscriptions,
                    icon = R.drawable.ic_settings_subscriptions_filled,
                    cornerType = DisplayItem.CornerType.BOTTOM,
                    onClick = onSubscriptionsSettingsClick,
                    iconBgColor = Color(0xFFFF3B30)
                )

                Spacer(modifier = Modifier.height(24.dp))



                FlareSectionHeader(text = strings.settings_header_appearance)
                
                FlareSettingsItem(
                    title = strings.settings_item_theme,
                    icon = R.drawable.ic_settings_brush_filled,
                    cornerType = DisplayItem.CornerType.TOP,
                    onClick = onThemeSettingsClick,
                    iconBgColor = Color(0xFF56CCF2)
                )
                FlareSettingsItem(
                    title = strings.settings_item_language,
                    icon = R.drawable.ic_language_filled,
                    cornerType = DisplayItem.CornerType.BOTTOM,
                    onClick = onLanguageSettingsClick,
                    iconBgColor = Color(0xFFFF9500)
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlareSectionHeader(text = "FlareVPN")
                
                FlareSettingsItem(
                    title = strings.sub_manage_title,
                    icon = R.drawable.ic_settings_dashboard,
                    cornerType = DisplayItem.CornerType.ALL,
                    onClick = onVpnSubscriptionClick,
                    iconBgColor = Color(0xFF5856D6)
                )
            }
        }

        
        FlareTopBar(
            title = strings.settings_title,
            hazeState = hazeState,
            scrollState = scrollState
        )
    }
}
