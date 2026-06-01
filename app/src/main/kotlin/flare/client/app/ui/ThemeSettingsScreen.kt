package flare.client.app.ui

import flare.client.app.ui.i18n.I18n


import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import flare.client.app.R
import flare.client.app.ui.components.*


import flare.client.app.ui.theme.FlareTheme

@Composable
fun ThemeSettingsScreen(
    themeMode: Int,
    isGradientEnabled: Boolean,
    isAnimationEnabled: Boolean,
    gradientSpeed: Float,
    isCustomColorEnabled: Boolean,
    accentColorKey: String,
    accentColor: Color,
    onBack: () -> Unit,
    onThemeClick: (Int) -> Unit,
    onGradientToggle: (Boolean) -> Unit,
    onAnimationToggle: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onCustomColorToggle: (Boolean) -> Unit,
    onColorKeySelect: (String) -> Unit,
    hazeState: HazeState
) {
    val colors = FlareTheme.colors
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

                SettingsSectionHeader(I18n.strings.settings_theme_header)

                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    SettingsItem(
                        label = I18n.strings.settings_label_theme,
                        value = when (themeMode) {
                            1 -> I18n.strings.theme_day
                            2 -> I18n.strings.theme_night
                            else -> I18n.strings.theme_auto
                        },
                        accentColor = colors.accent,
                        menuItems = listOf(
                            I18n.strings.theme_auto,
                            I18n.strings.theme_day,
                            I18n.strings.theme_night
                        ).mapIndexed { i, opt ->
                            flare.client.app.util.GlassUtils.MenuItem(i, opt) {
                                onThemeClick(i)
                            }
                        },
                        hazeState = hazeState,
                        isTop = true,
                        isBottom = false
                    )

                    DividerItem()

                    SettingsToggleItem(
                        label = I18n.strings.settings_label_custom_color,
                        checked = isCustomColorEnabled,
                        accentColor = colors.accent,
                        onCheckedChange = onCustomColorToggle,
                        isBottom = !isCustomColorEnabled,
                        isMiddle = isCustomColorEnabled
                    )

                    AnimatedVisibility(visible = isCustomColorEnabled) {
                        Column {
                            DividerItem()
                            ColorPickerItem(
                                selectedKey = accentColorKey,
                                onKeySelect = onColorKeySelect
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                SettingsSectionHeader(I18n.strings.settings_bg_effects_header)

                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    SettingsToggleItem(
                        label = I18n.strings.settings_label_enable_gradient,
                        checked = isGradientEnabled,
                        accentColor = colors.accent,
                        onCheckedChange = onGradientToggle,
                        isTop = true,
                        isBottom = !isGradientEnabled
                    )

                    AnimatedVisibility(visible = isGradientEnabled) {
                        Column {
                            DividerItem()
                            SettingsToggleItem(
                                label = I18n.strings.settings_label_gradient_animation,
                                checked = isAnimationEnabled,
                                accentColor = colors.accent,
                                onCheckedChange = onAnimationToggle,
                                isMiddle = isAnimationEnabled,
                                isBottom = !isAnimationEnabled
                            )

                            AnimatedVisibility(visible = isAnimationEnabled) {
                                Column {
                                    DividerItem()
                                    SpeedSliderItem(
                                        value = gradientSpeed,
                                        accentColor = colors.accent,
                                        onValueChange = onSpeedChange
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                SettingsSectionHeader(I18n.strings.settings_label_font)

                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    SettingsItem(
                        label = I18n.strings.settings_btn_change_font,
                        value = I18n.strings.settings_font_geologica,
                        accentColor = colors.accent,
                        onClick = {  },
                        enabled = false,
                        isTop = true,
                        isBottom = true
                    )
                }
            }
        }

        FlareTopBar(
            title = I18n.strings.settings_theme_title,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBack
        )
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    FlareSectionHeader(text = text)
}


@Composable
fun SettingsItem(
    label: String,
    value: String,
    accentColor: Color,
    onClick: () -> Unit = {},
    menuItems: List<flare.client.app.util.GlassUtils.MenuItem>? = null,
    enabled: Boolean = true,
    isTop: Boolean = false,
    isBottom: Boolean = false,
    hazeState: HazeState? = null
) {
    val colors = FlareTheme.colors
    val backgroundColor = colors.bgItem.copy(alpha = 0.85f)
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .background(
                    backgroundColor,
                    shape = when {
                        isTop && isBottom -> RoundedCornerShape(20.dp)
                        isTop -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        isBottom -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                        else -> androidx.compose.ui.graphics.RectangleShape
                    }
                )
                .clickable(enabled = enabled) {
                    if (menuItems != null) {
                        menuExpanded = true
                    } else {
                        onClick()
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = GeologicaRegular,
                fontSize = 16.sp,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Box(contentAlignment = Alignment.CenterEnd) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = value,
                        fontFamily = GeologicaMedium,
                        fontSize = 16.sp,
                        color = if (enabled) accentColor else colors.textSecondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        if (menuItems != null) {
            FlareGlassMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = menuItems,
                hazeState = hazeState,
                alignment = Alignment.TopEnd
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    label: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    isTop: Boolean = false,
    isMiddle: Boolean = false,
    isBottom: Boolean = false
) {
    val colors = FlareTheme.colors
    val backgroundColor = colors.bgItem.copy(alpha = 0.85f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                backgroundColor,
                shape = when {
                    isTop && isBottom -> RoundedCornerShape(20.dp)
                    isTop -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    isBottom -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    isMiddle -> androidx.compose.ui.graphics.RectangleShape
                    else -> androidx.compose.ui.graphics.RectangleShape
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = GeologicaRegular,
            fontSize = 16.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        FlareGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            accentColor = accentColor
        )
    }
}


@Composable
fun DividerItem() {
    FlareDivider()
}


@Composable
fun ColorPickerItem(
    selectedKey: String,
    onKeySelect: (String) -> Unit
) {
    val colors = FlareTheme.colors
    val colorKeys = listOf(
        "material_you", "green", "purple", "red", "pink", "orange", "indigo", "cyan", "amber", "violet", "teal" 
    )

    val backgroundColor = colors.bgItem.copy(alpha = 0.85f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                backgroundColor,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .padding(16.dp)
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(colorKeys.size) { index ->
                val key = colorKeys[index]
                val isSelected = key == selectedKey


                val color = when (key) {
                    "material_you" -> colors.accent
                    "green" -> Color(0xFF34C759)
                    "purple" -> Color(0xFF9B59B6)
                    "red" -> Color(0xFFFF453A)
                    "pink" -> Color(0xFFFF375F)
                    "orange" -> Color(0xFFFF9F0A)
                    "indigo" -> Color(0xFF5E5CE6)
                    "cyan" -> Color(0xFF64D2FF)
                    "amber" -> Color(0xFFFFD60A)
                    "violet" -> Color(0xFFBF5AF2)
                    "teal" -> Color(0xFF30B0C7)
                    else -> colors.accent
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = color,
                            shape = if (isSelected) RoundedCornerShape(14.dp) else CircleShape
                        )
                        .then(
                            if (isSelected) Modifier.border(
                                2.5.dp, Color.White, RoundedCornerShape(14.dp)
                            ) else Modifier
                        )
                        .clickable { onKeySelect(key) },
                    contentAlignment = Alignment.Center
                ) {
                    if (key == "material_you") {
                        Icon(
                            painter = painterResource(R.drawable.ic_android),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp).padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedSliderItem(
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    val colors = FlareTheme.colors
    val backgroundColor = colors.bgItem.copy(alpha = 0.85f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                backgroundColor,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = I18n.strings.settings_label_gradient_speed,
                fontFamily = GeologicaRegular,
                fontSize = 16.sp,
                color = colors.textPrimary
            )
            Text(
                text = String.format(java.util.Locale.US, "%.1fx", value),
                fontFamily = GeologicaMedium,
                fontSize = 16.sp,
                color = accentColor
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.2f..2.0f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = colors.dividerColor
            )
        )
    }
}
