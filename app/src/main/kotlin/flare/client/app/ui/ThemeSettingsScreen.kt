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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    appearanceType: Int,
    backgroundType: Int,
    isAnimationEnabled: Boolean,
    gradientSpeed: Float,
    isCustomColorEnabled: Boolean,
    accentColorKey: String,
    accentColor: Color,
    isChangeLaunchButtonColorEnabled: Boolean,
    onBack: () -> Unit,
    onThemeClick: (Int) -> Unit,
    onAppearanceTypeClick: (Int) -> Unit,
    onBackgroundTypeClick: (Int) -> Unit,
    onAnimationToggle: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onCustomColorToggle: (Boolean) -> Unit,
    onColorKeySelect: (String) -> Unit,
    onChangeLaunchButtonColorToggle: (Boolean) -> Unit,
    onUpdatePhotoClick: () -> Unit,
    isDownloadingPhoto: Boolean,
    isBlurEnabled: Boolean,
    onBlurToggle: (Boolean) -> Unit,
    isLiquidGlassEnabled: Boolean,
    onLiquidGlassToggle: (Boolean) -> Unit,
    fontFamily: String,
    onFontSelect: (String) -> Unit,
    appIcon: String,
    onAppIconSelect: (String) -> Unit,
    hazeState: HazeState
) {
    val colors = FlareTheme.colors
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
                            SettingsToggleItem(
                                label = I18n.strings.settings_label_change_launch_button_color,
                                checked = isChangeLaunchButtonColorEnabled,
                                accentColor = colors.accent,
                                onCheckedChange = onChangeLaunchButtonColorToggle,
                                isMiddle = true
                            )
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
                    SettingsItem(
                        label = I18n.strings.settings_bg_effect_label,
                        value = when (backgroundType) {
                            1 -> I18n.strings.settings_bg_effect_gradient
                            2 -> I18n.strings.settings_bg_effect_shapes
                            3 -> I18n.strings.settings_bg_effect_photo
                            else -> I18n.strings.settings_bg_effect_none
                        },
                        accentColor = colors.accent,
                        menuItems = listOf(
                            I18n.strings.settings_bg_effect_none,
                            I18n.strings.settings_bg_effect_gradient,
                            I18n.strings.settings_bg_effect_shapes,
                            I18n.strings.settings_bg_effect_photo
                        ).mapIndexed { i, opt ->
                            flare.client.app.util.GlassUtils.MenuItem(i, opt) {
                                onBackgroundTypeClick(i)
                            }
                        },
                        hazeState = hazeState,
                        isTop = true,
                        isBottom = backgroundType == 0 || backgroundType == 2
                    )

                    AnimatedVisibility(visible = backgroundType == 1) {
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
                                    flare.client.app.ui.components.FlareSliderItem(
                                        label = I18n.strings.settings_label_gradient_speed,
                                        valueText = String.format(java.util.Locale.US, "%.2fx", gradientSpeed),
                                        value = gradientSpeed,
                                        valueRange = 0.1f..4.0f,
                                        step = 0.1f,
                                        accentColor = colors.accent,
                                        onValueChange = onSpeedChange,
                                        isBottom = true
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = backgroundType == 3) {
                        Column {
                            DividerItem()
                            SettingsItem(
                                label = I18n.strings.settings_bg_effect_update_photo,
                                value = "",
                                accentColor = colors.accent,
                                onClick = {
                                    if (!isDownloadingPhoto) {
                                        onUpdatePhotoClick()
                                    }
                                },
                                isBottom = true,
                                hazeState = hazeState,
                                trailingContent = {
                                    FlareGlassButton(
                                        onClick = {
                                            if (!isDownloadingPhoto) {
                                                onUpdatePhotoClick()
                                            }
                                        },
                                        enabled = !isDownloadingPhoto
                                    ) {
                                        if (isDownloadingPhoto) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = colors.accent,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_refresh),
                                                contentDescription = null,
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionHeader(I18n.strings.settings_effects_header)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    SettingsToggleItem(
                        label = I18n.strings.settings_effect_blur,
                        checked = isBlurEnabled,
                        accentColor = colors.accent,
                        onCheckedChange = onBlurToggle,
                        isTop = true,
                        isBottom = false,
                        isMiddle = true
                    )
                    DividerItem()
                    SettingsToggleItem(
                        label = I18n.strings.settings_effect_liquid_glass,
                        checked = isLiquidGlassEnabled,
                        accentColor = colors.accent,
                        onCheckedChange = onLiquidGlassToggle,
                        isTop = false,
                        isBottom = true,
                        isMiddle = false
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionHeader(I18n.strings.settings_header_interface)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    SettingsItem(
                        label = I18n.strings.settings_ui_type_label,
                        value = when (appearanceType) {
                            1 -> I18n.strings.settings_ui_type_new
                            2 -> I18n.strings.settings_ui_type_minimal
                            else -> I18n.strings.settings_ui_type_standard
                        },
                        accentColor = colors.accent,
                        menuItems = listOf(
                            flare.client.app.util.GlassUtils.MenuItem(0, I18n.strings.settings_ui_type_standard) {
                                onAppearanceTypeClick(0)
                            },
                            flare.client.app.util.GlassUtils.MenuItem(1, I18n.strings.settings_ui_type_new) {
                                onAppearanceTypeClick(1)
                            },
                            flare.client.app.util.GlassUtils.MenuItem(2, I18n.strings.settings_ui_type_minimal) {
                                onAppearanceTypeClick(2)
                            }
                        ),
                        hazeState = hazeState,
                        enabled = true,
                        isTop = true,
                        isBottom = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                SettingsSectionHeader(I18n.strings.settings_label_font)

                Column(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                    SettingsItem(
                        label = I18n.strings.settings_label_font,
                        value = when (fontFamily) {
                            "system" -> I18n.strings.settings_font_system
                            "google_sans" -> I18n.strings.settings_font_google_sans
                            "inter" -> I18n.strings.settings_font_inter
                            else -> I18n.strings.settings_font_geologica
                        },
                        accentColor = colors.accent,
                        menuItems = listOf(
                            flare.client.app.util.GlassUtils.MenuItem(0, I18n.strings.settings_font_geologica) {
                                onFontSelect("geologica")
                            },
                            flare.client.app.util.GlassUtils.MenuItem(1, I18n.strings.settings_font_system) {
                                onFontSelect("system")
                            },
                            flare.client.app.util.GlassUtils.MenuItem(2, I18n.strings.settings_font_google_sans) {
                                onFontSelect("google_sans")
                            },
                            flare.client.app.util.GlassUtils.MenuItem(3, I18n.strings.settings_font_inter) {
                                onFontSelect("inter")
                            }
                        ),
                        hazeState = hazeState,
                        enabled = true,
                        isTop = true,
                        isBottom = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionHeader(I18n.strings.settings_header_app_icon)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.bgItem.copy(alpha = 0.85f))
                        .padding(vertical = 16.dp)
                ) {
                    val iconsList = listOf(
                        Triple("main", I18n.strings.settings_app_icon_main, R.drawable.ic_launcher_foreground_new),
                        Triple("monochrome", I18n.strings.settings_app_icon_monochrome, R.drawable.ic_launcher_monochrome),
                        Triple("softplush", I18n.strings.settings_app_icon_softplush, R.drawable.ic_launcher_softplush),
                        Triple("blueprint", I18n.strings.settings_app_icon_blueprint, R.drawable.ic_launcher_blueprint)
                    )

                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(iconsList.size) { index ->
                            val (key, label, resId) = iconsList[index]
                            val isSelected = key == appIcon
                            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(76.dp)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onAppIconSelect(key) }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .then(
                                            if (isSelected) Modifier.border(
                                                2.5.dp, colors.accent, RoundedCornerShape(16.dp)
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(resId),
                                        contentDescription = label,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = label,
                                    fontFamily = GeologicaRegular,
                                    fontSize = 12.sp,
                                    color = if (isSelected) colors.accent else colors.textPrimary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        FlareSubScreenTopBar(
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
    hazeState: HazeState? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val colors = FlareTheme.colors
    val backgroundColor = colors.bgItem.copy(alpha = 0.85f)
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

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
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) {
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
                if (trailingContent != null) {
                    trailingContent()
                } else {
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
        "material_you", "green", "purple", "red", "pink", "orange", "indigo", "cyan", "amber", "violet", "teal",
        "lime", "candy_blue", "sunset", "lavender"
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
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
 
 
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
                    "lime" -> Color(0xFFC6FF34)
                    "candy_blue" -> Color(0xFFB2D5E5)
                    "sunset" -> Color(0xFFFF5E62)
                    "lavender" -> Color(0xFFD1B3FF)
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
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onKeySelect(key) }
                        ),
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

