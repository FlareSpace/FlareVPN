package flare.client.app.ui

import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.focus.onFocusChanged
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import flare.client.app.R
import flare.client.app.ui.components.*
import flare.client.app.ui.theme.FlareTheme


private val GeologicaMedium = FontFamily(Font(R.font.geologica_medium, FontWeight.Medium))
private val GeologicaRegular = FontFamily(Font(R.font.geologica_regular, FontWeight.Normal))

@Composable
fun PingSettingsScreen(
    pingType: String,
    onPingTypeChange: (String) -> Unit,
    pingTestUrl: String,
    onPingTestUrlChange: (String) -> Unit,
    pingStyleValue: String,
    onPingStyleClick: (String) -> Unit,
    onBack: () -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState
) {
    val isDark = FlareTheme.colors.isDark

    val pingStyleOptions = listOf(
        I18n.strings.settings_ping_style_time to "time",
        I18n.strings.settings_ping_style_icon to "icon",
        I18n.strings.settings_ping_style_both to "both"
    )

    val pingStyleDisplayValue = pingStyleOptions.find { it.second == pingStyleValue }?.first ?: pingStyleValue

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
                FlareSectionHeader(text = I18n.strings.settings_label_ping_type)
                
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PingTypeButton(
                        text = I18n.strings.settings_ping_type_get,
                        iconRes = R.drawable.ic_cloud,
                        isSelected = pingType == "via proxy GET" || pingType == "via proxy HEAD",
                        onClick = { onPingTypeChange("via proxy GET") },
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = accentColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PingTypeButton(
                            text = I18n.strings.settings_ping_type_tcp,
                            iconRes = R.drawable.ic_chain,
                            isSelected = pingType == "TCP",
                            onClick = { onPingTypeChange("TCP") },
                            modifier = Modifier.weight(1f),
                            accentColor = accentColor
                        )
                        PingTypeButton(
                            text = I18n.strings.settings_ping_type_icmp,
                            iconRes = R.drawable.ic_lightning,
                            isSelected = pingType == "ICMP",
                            onClick = { onPingTypeChange("ICMP") },
                            modifier = Modifier.weight(1f),
                            accentColor = accentColor
                        )
                    }
                }

                val typeDesc = when (pingType) {
                    "via proxy GET", "via proxy HEAD" -> I18n.strings.viaproxy_desc
                    "TCP" -> I18n.strings.tcp_desc
                    "ICMP" -> I18n.strings.icmp_desc
                    else -> ""
                }

                Text(
                    text = typeDesc,
                    fontFamily = GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 24.dp)
                )

                FlareSectionHeader(text = I18n.strings.settings_label_test_url)
                
                
                var isUrlFocused by remember { mutableStateOf(false) }
                val urlBgColor by animateColorAsState(
                    targetValue = if (isUrlFocused) {
                        accentColor.copy(alpha = 0.08f).compositeOver(FlareTheme.colors.bgItem)
                    } else {
                        FlareTheme.colors.bgItem
                    },
                    animationSpec = tween(220),
                    label = "pingUrlBg"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(urlBgColor)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = pingTestUrl,
                            onValueChange = onPingTestUrlChange,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = GeologicaMedium,
                                fontSize = 16.sp,
                                color = accentColor,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isUrlFocused = it.isFocused },
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (pingTestUrl.isEmpty()) {
                                        Text(
                                            text = I18n.strings.settings_hint_test_url,
                                            fontFamily = GeologicaMedium,
                                            fontSize = 16.sp,
                                            color = FlareTheme.colors.textSecondary.copy(alpha = 0.5f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        innerTextField()
                                        
                                        Box(
                                            modifier = Modifier
                                                .width(IntrinsicSize.Max)
                                                .padding(top = 6.dp)
                                                .height(1.dp)
                                                .background(FlareTheme.colors.glassStroke)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                Text(
                    text = I18n.strings.settings_desc_test_url,
                    fontFamily = GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 24.dp)
                )

                FlareSectionHeader(text = I18n.strings.settings_label_ping_display)
                
                FlareSettingsValueItem(
                    title = I18n.strings.settings_label_ping_style,
                    value = pingStyleDisplayValue,
                    menuItems = pingStyleOptions.mapIndexed { i, opt ->
                        flare.client.app.util.GlassUtils.MenuItem(i, opt.first) {
                            onPingStyleClick(opt.second)
                        }
                    },
                    cornerType = flare.client.app.data.model.DisplayItem.CornerType.ALL,
                    accentColor = accentColor,
                    hazeState = hazeState
                )
            }
        }

        
        FlareTopBar(
            title = I18n.strings.settings_ping_title,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBack
        )
    }
}

@Composable
fun PingTypeButton(
    text: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    val backgroundColor = if (isSelected) FlareTheme.colors.bgProfileSelected else FlareTheme.colors.bgItem
    val textColor = if (isSelected) FlareTheme.colors.textProfileSelectedPrimary else FlareTheme.colors.textPrimary
    val iconColor = if (isSelected) accentColor else FlareTheme.colors.textSecondary

    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }

        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(4.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }

}
