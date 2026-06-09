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
    backgroundType: Int,
    isAnimationEnabled: Boolean,
    gradientSpeed: Float,
    isCustomColorEnabled: Boolean,
    accentColorKey: String,
    accentColor: Color,
    onBack: () -> Unit,
    onThemeClick: (Int) -> Unit,
    onBackgroundTypeClick: (Int) -> Unit,
    onAnimationToggle: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onCustomColorToggle: (Boolean) -> Unit,
    onColorKeySelect: (String) -> Unit,
    onUpdatePhotoClick: () -> Unit,
    isDownloadingPhoto: Boolean,
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
                                    SpeedSliderItem(
                                        value = gradientSpeed,
                                        accentColor = colors.accent,
                                        onValueChange = onSpeedChange
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
    hazeState: HazeState? = null,
    trailingContent: (@Composable () -> Unit)? = null
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
    val inactiveTrackColor = colors.textSecondary.copy(alpha = 0.2f)
    
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { 35.dp.toPx() }
    val thumbHeightPx = with(density) { 22.dp.toPx() }
    val trackHeightPx = with(density) { 12.dp.toPx() }
    
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    
    val valueRange = 0.1f..4.0f
    val currentFraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "sliderGlow"
    )

    fun updateValueFromOffset(xOffset: Float) {
        val width = canvasSize.width.toFloat()
        val usableWidth = width - thumbWidthPx
        val startX = thumbWidthPx / 2
        if (usableWidth > 0) {
            val fraction = ((xOffset - startX) / usableWidth).coerceIn(0f, 1f)
            val rawValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
            val roundedValue = (kotlin.math.round(rawValue * 100f) / 100f).coerceIn(valueRange.start, valueRange.endInclusive)
            onValueChange(roundedValue)
        }
    }

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
                text = String.format(java.util.Locale.US, "%.2fx", value),
                fontFamily = GeologicaMedium,
                fontSize = 16.sp,
                color = accentColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(valueRange) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        updateValueFromOffset(down.position.x)
                        
                        var pointer = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (!anyPressed) {
                                break
                            }
                            val change = event.changes.firstOrNull { it.id == pointer } ?: event.changes.first()
                            pointer = change.id
                            change.consume()
                            updateValueFromOffset(change.position.x)
                        }
                        isDragging = false
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            
            val startX = thumbWidthPx / 2
            val usableWidth = width - thumbWidthPx
            val thumbX = startX + usableWidth * currentFraction
            
            
            drawRoundRect(
                color = inactiveTrackColor,
                topLeft = Offset(startX, centerY - trackHeightPx / 2),
                size = Size(usableWidth, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2, trackHeightPx / 2)
            )
            
            
            val activeWidth = thumbX - startX
            if (activeWidth > 0f) {
                
                if (glowAlpha > 0f) {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        
                        val paintAmbient = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                8.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(
                                    (0.35f * glowAlpha * 255).toInt(),
                                    (accentColor.red * 255).toInt(),
                                    (accentColor.green * 255).toInt(),
                                    (accentColor.blue * 255).toInt()
                                )
                            )
                        }
                        nativeCanvas.drawRoundRect(
                            startX,
                            centerY - trackHeightPx / 2,
                            thumbX,
                            centerY + trackHeightPx / 2,
                            trackHeightPx / 2,
                            trackHeightPx / 2,
                            paintAmbient
                        )

                        val paintCore = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                3.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(
                                    (0.65f * glowAlpha * 255).toInt(),
                                    (accentColor.red * 255).toInt(),
                                    (accentColor.green * 255).toInt(),
                                    (accentColor.blue * 255).toInt()
                                )
                            )
                        }
                        nativeCanvas.drawRoundRect(
                            startX,
                            centerY - trackHeightPx / 2,
                            thumbX,
                            centerY + trackHeightPx / 2,
                            trackHeightPx / 2,
                            trackHeightPx / 2,
                            paintCore
                        )
                    }
                }
                
                
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(startX, centerY - trackHeightPx / 2),
                    size = Size(activeWidth, trackHeightPx),
                    cornerRadius = CornerRadius(trackHeightPx / 2, trackHeightPx / 2)
                )
            }
            
            
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val paintShadow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        4.dp.toPx(),
                        0f,
                        2.dp.toPx(),
                        android.graphics.Color.argb(45, 0, 0, 0)
                    )
                }
                nativeCanvas.drawRoundRect(
                    thumbX - thumbWidthPx / 2,
                    centerY - thumbHeightPx / 2,
                    thumbX + thumbWidthPx / 2,
                    centerY + thumbHeightPx / 2,
                    thumbWidthPx / 2,
                    thumbWidthPx / 2,
                    paintShadow
                )
            }
            
            
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(thumbX - thumbWidthPx / 2, centerY - thumbHeightPx / 2),
                size = Size(thumbWidthPx, thumbHeightPx),
                cornerRadius = CornerRadius(thumbWidthPx / 2, thumbWidthPx / 2)
            )
        }
    }
}
