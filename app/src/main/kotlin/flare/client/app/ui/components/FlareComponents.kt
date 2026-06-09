package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.theme.FlareTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import dev.chrisbanes.haze.HazeProgressive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius



val GeologicaMedium = FontFamily(Font(R.font.geologica_medium, FontWeight.Medium))
val GeologicaRegular = FontFamily(Font(R.font.geologica_regular, FontWeight.Normal))

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(80f),
            FontVariation.grade(0)
        )
    )
)

@Composable
fun RollingTimer(
    time: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val density = LocalDensity.current
        val baseWidth = with(density) { fontSize.toDp() * 0.65f }
        val colonWidth = with(density) { fontSize.toDp() * 0.35f }

        time.indices.forEach { i ->
            val char = time[i]
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn() togetherWith
                            slideOutVertically { -it } + fadeOut())
                        .using(SizeTransform(clip = false))
                },
                contentAlignment = Alignment.Center,
                label = "timer_digit_$i"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    fontFamily = GeologicaMedium,
                    fontSize = fontSize,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(if (digit == ':') colonWidth else baseWidth)
                )
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FlareTopBar(
    title: String,
    hazeState: HazeState,
    scrollState: ScrollState? = null,
    lazyListState: LazyListState? = null,
    onBack: (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val isDark = FlareTheme.colors.isDark
    android.util.Log.d("FlareTopBar", "isDark = $isDark, colors.bgItem = ${FlareTheme.colors.bgItem}")
    
    val scrollOffset = when {
        scrollState != null -> scrollState.value
        lazyListState != null -> {
            if (lazyListState.firstVisibleItemIndex > 0) 500 else lazyListState.firstVisibleItemScrollOffset
        }
        else -> 0
    }
    
    val density = LocalDensity.current
    val maxScrollPx = with(density) { 30.dp.toPx() }
    val scrollProgress = (scrollOffset / maxScrollPx).coerceIn(0f, 1f)
    
    
    val lineColor = if (isDark) {
        Color.White.copy(alpha = 0.12f * scrollProgress)
    } else {
        Color.Black.copy(alpha = 0.08f * scrollProgress)
    }
    
    val baseStyle = HazeMaterials.ultraThin()
    val lightTint = baseStyle.tints.firstOrNull()?.color ?: Color.White.copy(alpha = 0.30f)
    val darkTint = Color(0xFF1A1A1A).copy(alpha = 0.30f)
    val hazeStyle = HazeStyle(
        blurRadius  = baseStyle.blurRadius,
        tints       = listOf(HazeTint(color = if (isDark) darkTint else lightTint)),
        noiseFactor = baseStyle.noiseFactor
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .hazeEffect(state = hazeState, style = hazeStyle) {
                alpha = scrollProgress
            }
            .drawBehind {
                if (scrollProgress > 0f) {
                    val strokeWidth = 1.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .statusBarsPadding()
            .padding(horizontal = if (onBack != null) 8.dp else 20.dp)
            .padding(top = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            FlareGlassButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null,
                    tint = FlareTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 8.dp else 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontFamily = GeologicaMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = FlareTheme.colors.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                subtitle()
            }
        }
        
        if (actions != null) {
            actions()
        }
    }
}

@Composable
fun FlareSectionHeader(text: String) {
    Text(
        text = text,
        fontFamily = GeologicaMedium,
        fontSize = 14.sp,
        color = FlareTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun FlareDivider(
    modifier: Modifier = Modifier,
    hasIcon: Boolean = true,
    dividerOffset: androidx.compose.ui.unit.Dp = if (hasIcon) 56.dp else 16.dp
) {
    val dividerBgColor = FlareTheme.colors.bgItem.copy(alpha = 0.85f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(dividerBgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = dividerOffset)
                .background(FlareTheme.colors.dividerColor)
        )
    }
}

@Composable
fun FlareGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = FlareTheme.colors.accent
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "thumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color.Gray.copy(alpha = 0.2f),
        label = "trackColor"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "switchGlowAlpha"
    )

    val thumbColor = Color.White

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val cornersRadiusPx = 14.dp.toPx()
                        
                        
                        val paintAmbient = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                8.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(
                                    (0.28f * glowAlpha * 255).toInt(),
                                    (accentColor.red * 255).toInt(),
                                    (accentColor.green * 255).toInt(),
                                    (accentColor.blue * 255).toInt()
                                )
                            )
                        }
                        nativeCanvas.drawRoundRect(
                            0f,
                            0f,
                            size.width,
                            size.height,
                            cornersRadiusPx,
                            cornersRadiusPx,
                            paintAmbient
                        )

                        
                        val paintCore = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                3.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(
                                    (0.55f * glowAlpha * 255).toInt(),
                                    (accentColor.red * 255).toInt(),
                                    (accentColor.green * 255).toInt(),
                                    (accentColor.blue * 255).toInt()
                                )
                            )
                        }
                        nativeCanvas.drawRoundRect(
                            0f,
                            0f,
                            size.width,
                            size.height,
                            cornersRadiusPx,
                            cornersRadiusPx,
                            paintCore
                        )
                    }
                }
            }
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .background(thumbColor, CircleShape)
                .then(
                    if (checked) {
                        Modifier.background(
                            Brush.radialGradient(
                                colors = listOf(Color.White, Color.White.copy(alpha = 0.3f), Color.Transparent),
                                radius = 40f
                            ),
                            CircleShape
                        )
                    } else Modifier
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlareCard(
    modifier: Modifier = Modifier,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    paddingHorizontal: androidx.compose.ui.unit.Dp = 16.dp,
    paddingVertical: androidx.compose.ui.unit.Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: ((androidx.compose.ui.geometry.Offset) -> Unit)? = null,
    showRipple: Boolean = false,
    borderColor: Color = Color.Transparent,
    borderWidth: androidx.compose.ui.unit.Dp = 0.dp,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val defaultBgColor = FlareTheme.colors.bgItem.copy(alpha = 0.85f)
    val resolvedBgColor = if (backgroundColor != Color.Unspecified) backgroundColor else defaultBgColor
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val shape = when (cornerType) {
        DisplayItem.CornerType.ALL -> RoundedCornerShape(cornerRadius)
        DisplayItem.CornerType.TOP -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        DisplayItem.CornerType.BOTTOM -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
        DisplayItem.CornerType.NONE -> androidx.compose.ui.graphics.RectangleShape
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(resolvedBgColor)
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape)
                else Modifier
            )
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick?.invoke() },
                            onLongPress = { offset -> 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onLongClick?.invoke(offset) 
                            }
                        )
                    }
                } else Modifier
            )
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        content = content
    )
}

@Composable
fun FlareWizardInputField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    isValid: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    accentColor: Color = FlareTheme.colors.accent,
    icon: Int? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        val borderColor = if (isFocused || isValid) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
        val bgColor = if (isFocused || isValid) accentColor.copy(alpha = 0.05f) else FlareTheme.colors.bgItem.copy(alpha = 0.5f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (isFocused || isValid) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 12.dp).size(20.dp)
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        fontFamily = GeologicaRegular,
                        fontSize = 15.sp,
                        color = FlareTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                }
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = GeologicaMedium,
                        fontSize = 15.sp,
                        color = FlareTheme.colors.textPrimary
                    ),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
        }
    }
}

@Composable
fun FlareWizardIpPortField(
    ipValue: String,
    onIpChange: (String) -> Unit,
    portValue: String,
    onPortChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = FlareTheme.colors.accent,
    icon: Int? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val isValid = ipValue.isNotBlank() && portValue.isNotBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = I18n.strings.servers_ssh_ip,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        val borderColor = if (isFocused || isValid) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
        val bgColor = if (isFocused || isValid) accentColor.copy(alpha = 0.05f) else FlareTheme.colors.bgItem.copy(alpha = 0.5f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (isFocused || isValid) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp).size(20.dp)
                )
            }

            BasicTextField(
                value = ipValue,
                onValueChange = onIpChange,
                textStyle = TextStyle(
                    fontFamily = GeologicaMedium,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textPrimary
                ),
                cursorBrush = SolidColor(accentColor),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (icon != null) 12.dp else 16.dp,
                        end = 16.dp
                    )
                    .onFocusChanged { if (it.isFocused) isFocused = true else if (!it.hasFocus) isFocused = false }
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(FlareTheme.colors.dividerColor)
            )

            BasicTextField(
                value = portValue,
                onValueChange = onPortChange,
                textStyle = TextStyle(
                    fontFamily = GeologicaMedium,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(accentColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 8.dp)
                    .onFocusChanged { if (it.isFocused) isFocused = true else if (!it.hasFocus) isFocused = false }
            )
        }
    }
}

@Composable
fun GlassIconContainer(
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = FlareTheme.colors.isDark
    val borderAlphaStart = if (isDark) 0.35f else 0.45f
    val borderAlphaEnd = if (isDark) 0.05f else 0.08f

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        iconBgColor,
                        iconBgColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlphaStart),
                        Color.White.copy(alpha = borderAlphaEnd),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.15f else 0.22f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 40f
                    )
                )
        )
        content()
    }
}

@Composable
fun FlareGlassContainer(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    radius: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = FlareTheme.colors.isDark
    
    Box(
        modifier = modifier
            
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(
                            12.dp.toPx(),
                            0f,
                            4.dp.toPx(),
                            android.graphics.Color.argb(if (isDark) 75 else 20, 0, 0, 0)
                        )
                    }
                    val radiusPx = radius.toPx()
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        radiusPx,
                        radiusPx,
                        paint
                    )
                }
            }
            
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    } else {
                        listOf(
                            Color(0xFFFFFFFF).copy(alpha = 0.85f),
                            Color(0xFFF2F2F7).copy(alpha = 0.60f)
                        )
                    }
                ),
                shape = shape
            )
            
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.60f),
                            Color(0x0D000000)
                        )
                    }
                ),
                shape = shape
            )
            .clip(shape)
    ) {
        
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.18f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 15f
                    )
                )
        )
        
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun FlareSettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    value: String? = null,
    showArrow: Boolean = true,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true,
    onClick: (android.view.View) -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    var anchorView: android.view.View? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        AndroidView(
            factory = { context ->
                android.view.View(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0f },
            update = { anchorView = it }
        )

        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            onClick = { anchorView?.let { onClick(it) } },
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = FlareTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            
                            if (showTooltip && hazeState != null) {
                                FlareGlassTooltip(
                                    text = description,
                                    hazeState = hazeState,
                                    onDismiss = { showTooltip = false }
                                )
                            }
                        }
                    }
                }

                if (value != null) {
                    Text(
                        text = value,
                        fontFamily = GeologicaRegular,
                        fontSize = 14.sp,
                        color = FlareTheme.colors.textSecondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                if (showArrow) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = FlareTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

            }
        }


        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}


@Composable
fun FlareSettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    accentColor: Color = FlareTheme.colors.accent,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = FlareTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = accentColor
                                )
                            }
                            
                            if (showTooltip && hazeState != null) {
                                FlareGlassTooltip(
                                    text = description,
                                    hazeState = hazeState,
                                    onDismiss = { showTooltip = false }
                                )
                            }
                        }
                    }
                }

                FlareGlassSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    accentColor = accentColor
                )
            }
        }

        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}

@Composable
fun FlareSettingsValueItem(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
    menuItems: List<flare.client.app.util.GlassUtils.MenuItem>? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    accentColor: Color = FlareTheme.colors.accent,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true
) {
    var showTooltip by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            onClick = {
                if (menuItems != null) {
                    menuExpanded = true
                } else {
                    onClick?.invoke()
                }
            },
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = FlareTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = accentColor
                                )
                            }
                            
                            if (showTooltip && hazeState != null) {
                                FlareGlassTooltip(
                                    text = description,
                                    hazeState = hazeState,
                                    onDismiss = { showTooltip = false }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = value,
                    fontFamily = GeologicaMedium,
                    fontSize = 16.sp,
                    color = accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = FlareTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )

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

        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}

@Composable
fun FlareSettingsInputItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    hint: String = "",
    suffix: String = "",
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    accentColor: Color = FlareTheme.colors.accent,
    isValid: Boolean = false,
    showBorder: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true,
    action: @Composable (RowScope.() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val baseCardColor = FlareTheme.colors.bgItem.copy(alpha = 0.85f)
    val cardBgColor by animateColorAsState(
        targetValue = if (isFocused) {
            accentColor.copy(alpha = 0.08f).compositeOver(baseCardColor)
        } else {
            baseCardColor
        },
        animationSpec = tween(220),
        label = "inputHighlightBg"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else FlareTheme.colors.textPrimary,
        animationSpec = tween(220),
        label = "inputTitleColor"
    )

    val currentBorderColor = if (isFocused || isValid) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
    val currentBorderWidth = if (showBorder) 1.5.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            modifier = Modifier.fillMaxSize(),
            borderColor = if (showBorder) currentBorderColor else Color.Transparent,
            borderWidth = currentBorderWidth,
            backgroundColor = cardBgColor,
            cornerRadius = 20.dp,
            onClick = { focusRequester.requestFocus() }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = titleColor,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = accentColor
                                )
                            }
                            
                            if (showTooltip && hazeState != null) {
                                FlareGlassTooltip(
                                    text = description,
                                    hazeState = hazeState,
                                    onDismiss = { showTooltip = false }
                                )
                            }
                        }
                    }

                    if (isValid) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = FlareTheme.colors.connectedGreen,
                            modifier = Modifier.padding(start = 8.dp).size(16.dp)
                        )
                    }

                    if (action != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        action()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                        .widthIn(min = 80.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = GeologicaMedium,
                            fontSize = 16.sp,
                            color = accentColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        ),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .fillMaxWidth(),
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = hint,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 16.sp,
                                        color = FlareTheme.colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                if (suffix.isNotEmpty()) {
                    Text(
                        text = suffix,
                        fontFamily = GeologicaMedium,
                        fontSize = 16.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        if (!showBorder && cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}

@Composable
fun FlareGlassTooltip(
    text: String,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null
) {
    androidx.compose.ui.window.Popup(
        onDismissRequest = onDismiss,
        offset = IntOffset(0, 16),
        alignment = Alignment.TopCenter,
        properties = androidx.compose.ui.window.PopupProperties(
            focusable = true,
            clippingEnabled = true
        )
    ) {
        val isDark = FlareTheme.colors.isDark
        val textColor = FlareTheme.colors.textPrimary
        val shape = RoundedCornerShape(18.dp)

        var animStarted by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animStarted = true
        }

        val alpha by animateFloatAsState(
            targetValue = if (animStarted) 1f else 0f,
            animationSpec = tween(150),
            label = "alpha"
        )
        val scale by animateFloatAsState(
            targetValue = if (animStarted) 1f else 0.95f,
            animationSpec = tween(150, easing = LinearOutSlowInEasing),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .widthIn(max = 280.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                }
                .clip(shape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .flareGlass(
                        isDark = isDark,
                        radius = 18f,
                        intensity = 1.6f,
                        index = 1.5f,
                        glassHeight = 0.1f
                    )
                    .let {
                        if (hazeState != null) {
                            it.hazeEffect(state = hazeState) { blurRadius = 3.dp }
                        } else {
                            it.background(
                                if (isDark) Color(0x661A1C1E) else Color(0x99FFFFFF)
                            )
                        }
                    }
            )
            
            Text(
                text = text,
                fontFamily = GeologicaRegular,
                fontSize = 14.sp,
                color = textColor,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
fun FlareAnimatedPercentText(
    progress: Int,
    modifier: Modifier = Modifier,
    color: Color = FlareTheme.colors.textSecondary,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = progress,
            transitionSpec = {
                if (targetState > initialState) {
                    (androidx.compose.animation.slideInVertically { height -> height } + androidx.compose.animation.fadeIn())
                        .togetherWith(androidx.compose.animation.slideOutVertically { height -> -height } + androidx.compose.animation.fadeOut())
                } else {
                    (androidx.compose.animation.slideInVertically { height -> -height } + androidx.compose.animation.fadeIn())
                        .togetherWith(androidx.compose.animation.slideOutVertically { height -> height } + androidx.compose.animation.fadeOut())
                }.using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "percentAnimation"
        ) { targetProgress ->
            Text(
                text = "$targetProgress",
                fontFamily = GeologicaMedium,
                fontSize = fontSize,
                color = color
            )
        }
        Text(
            text = "%",
            fontFamily = GeologicaRegular,
            fontSize = fontSize,
            color = color
        )
    }
}

@Composable
fun FlareRoutingCard(
    rule: flare.client.app.ui.RoutingRuleState,
    onToggle: (Boolean) -> Unit,
    onModeClick: (String) -> Unit,
    onDownloadClick: () -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState? = null
) {
    FlareCard(
        modifier = Modifier.padding(bottom = 12.dp),
        cornerType = DisplayItem.CornerType.ALL,
        paddingVertical = 16.dp
    ) {
        var menuExpanded by remember { mutableStateOf(false) }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title(),
                    fontFamily = GeologicaMedium,
                    fontSize = 17.sp,
                    color = FlareTheme.colors.textPrimary
                )
                val description = rule.description?.invoke() ?: ""
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        fontFamily = GeologicaRegular,
                        fontSize = 12.sp,
                        color = FlareTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            FlareGlassSwitch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                accentColor = accentColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rule.isDownloading) {
                CircularProgressIndicator(
                    progress = { rule.progress / 100f },
                    modifier = Modifier.size(16.dp).padding(start = 4.dp),
                    color = accentColor,
                    strokeWidth = 2.dp,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                FlareAnimatedPercentText(
                    progress = rule.progress,
                    modifier = Modifier.padding(start = 6.dp)
                )
            } else {
                val isDownloaded = flare.client.app.singbox.GeoFileManager.isFileDownloaded(
                    androidx.compose.ui.platform.LocalContext.current,
                    rule.fileNames.first()
                )
                
                val iconRes = if (isDownloaded) R.drawable.ic_refresh else R.drawable.ic_download
                
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = FlareTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val updateText = if (isDownloaded) {
                    if (rule.lastUpdate == 0L) {
                        if (rule.isBuiltin) I18n.strings.routing_badge_builtin
                        else I18n.strings.routing_update_never
                    } else {
                        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(rule.lastUpdate))
                    }
                } else {
                    I18n.strings.routing_action_download
                }

                Text(
                    text = updateText,
                    fontFamily = GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { menuExpanded = true }
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modeText = when (rule.mode) {
                        "proxy" -> I18n.strings.routing_mode_proxy
                        "block" -> I18n.strings.routing_mode_block
                        "direct" -> I18n.strings.routing_mode_direct
                        else -> I18n.strings.routing_mode_direct
                    }
                    Text(
                        text = modeText,
                        fontFamily = GeologicaMedium,
                        fontSize = 13.sp,
                        color = accentColor
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(13.dp)
                            .padding(start = 3.dp)
                            .graphicsLayer(rotationZ = 90f)
                    )
                }

                val modes = listOf(
                    "proxy" to I18n.strings.routing_mode_proxy,
                    "direct" to I18n.strings.routing_mode_direct,
                    "block" to I18n.strings.routing_mode_block
                )
                
                FlareGlassMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = modes.mapIndexed { i, mode ->
                        flare.client.app.util.GlassUtils.MenuItem(i, mode.second) {
                            onModeClick(mode.first)
                            menuExpanded = false
                        }
                    },
                    hazeState = hazeState,
                    alignment = Alignment.TopEnd
                )
            }
        }
    }
}

private const val LIQUID_GLASS_AGSL = """
    uniform shader img;
    uniform float2 resolution;
    uniform float2 center;
    uniform float2 size;
    uniform float4 radius;
    uniform float thickness;
    uniform float refract_index;
    uniform float refract_intensity;
    uniform float saturation;
    uniform float glass_height;
    uniform float4 foreground_color_premultiplied;
    uniform float is_dark_mode;
    uniform float has_outline;
    uniform float outline_thickness;
    uniform float density;
    
    half sdfRect(half2 p, half4 r) {
      r.xy = (p.x > 0.0) ? r.xy : r.zw;
      r.x  = (p.y > 0.0) ? r.x  : r.y;
      half2 q = abs(p) - size + r.x;
      return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;
    }
    
    half4 srcOver(half4 src, half4 dst) {
        half3 outRGB = (src.rgb + dst.rgb * (1.0 - src.a));
        float outA = src.a + (1.0 - src.a) * dst.a;
        return half4(outRGB, outA);
    }
    
    half4 main(in float2 fragCoord) {
      half2 p = fragCoord - center;
      half sd = sdfRect(p, radius);
      half2 uv = fragCoord;
    
      if (sd < 0.0) {
        half sdX = sdfRect(p + half2(1.0, 0.0), radius);
        half sdY = sdfRect(p + half2(0.0, 1.0), radius);
    
        half effectiveT = max(thickness, glass_height * min(size.x, size.y));
        half n_cos = max(effectiveT + sd, 0.0) / effectiveT;
        half n_cos2 = n_cos * n_cos;
        half n_sin = sqrt(1.0 - n_cos2);
        half3 normal = normalize(half3((sdX - sd) * n_cos, (sdY - sd) * n_cos, n_sin));
    
        half3 refract_vec = refract(half3(0.0, 0.0, -1.0), normal, 1.0 / refract_index);
        half h = sd < -effectiveT ? effectiveT : sqrt(sd * (-2.0 * effectiveT - sd));
        half refract_length = (h + 8.0 * thickness) / -refract_vec.z;
    
        uv += refract_vec.xy * refract_length * refract_intensity;
    
        half4 bg = img.eval(uv);
    
        half luminance = dot(bg.rgb, half3(0.2126, 0.7152, 0.0722));
        bg.rgb = mix(half3(luminance), bg.rgb, saturation);
    
        half4 result = srcOver(half4(foreground_color_premultiplied), bg);
    
        half edgeDist = -sd;
        half tlDir = max(dot(normal.xy, normalize(half2(-0.8, -1.0))), 0.0);
        half brDir = max(dot(normal.xy, normalize(half2(0.8, 1.0))), 0.0);
        
        half tlHighlight = 0.0;
        half brHighlight = 0.0;
        half rimLight = 0.0;
        
        if (is_dark_mode > 0.5) {
            half thinHlMask = smoothstep(2.0 * density, 0.0, edgeDist);
            tlHighlight = pow(tlDir, 8.0) * thinHlMask * 0.35;
        } else {
            tlHighlight = pow(tlDir, 8.0) * smoothstep(3.0 * density, 0.0, edgeDist) * 0.8;
            if (has_outline > 0.5) {
                half outlineMask = smoothstep(outline_thickness, 0.0, edgeDist);
                result.rgb = mix(result.rgb, half3(0.0), outlineMask * 0.15);
            }
        }
        
        result.rgb += half3(1.0) * (tlHighlight + brHighlight + rimLight);
        return result;
      }
    
      return half4(0.0);
    }
"""

fun Modifier.flareGlass(
    isDark: Boolean,
    radius: Float = 18f,
    thickness: Float = 2f,
    intensity: Float = 1.2f,
    index: Float = 1.52f,
    glassHeight: Float = 0.12f,
    hasOutline: Boolean = false,
    outlineThickness: Float = 1.0f
): Modifier = this.then(
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        Modifier.graphicsLayer {
            val shader = android.graphics.RuntimeShader(LIQUID_GLASS_AGSL)
            val dp = density
            
            val fgColor = if (isDark)
                android.graphics.Color.argb(160, 32, 34, 40)
            else
                android.graphics.Color.argb(135, 255, 255, 255)
                
            val a = android.graphics.Color.alpha(fgColor) / 255f
            val r = android.graphics.Color.red(fgColor) / 255f * a
            val g = android.graphics.Color.green(fgColor) / 255f * a
            val b = android.graphics.Color.blue(fgColor) / 255f * a
 
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("center", size.width / 2f, size.height / 2f)
            shader.setFloatUniform("size", size.width / 2f, size.height / 2f)
            val rPx = radius * dp
            shader.setFloatUniform("radius", rPx, rPx, rPx, rPx)
            shader.setFloatUniform("thickness", thickness * dp)
            shader.setFloatUniform("refract_intensity", intensity)
            shader.setFloatUniform("refract_index", index)
            shader.setFloatUniform("glass_height", glassHeight)
            shader.setFloatUniform("saturation", 1.45f)
            shader.setFloatUniform("foreground_color_premultiplied", r, g, b, a)
            shader.setFloatUniform("is_dark_mode", if (isDark) 1f else 0f)
            shader.setFloatUniform("has_outline", if (hasOutline) 1f else 0f)
            shader.setFloatUniform("outline_thickness", outlineThickness * dp)
            shader.setFloatUniform("density", dp)

            renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "img").asComposeRenderEffect()
        }
    } else {
        Modifier.background(
            if (isDark) Color(0xCC1A1C1E) else Color(0xCCFFFFFF),
            RoundedCornerShape(radius.dp)
        )
    }
)

@Composable
fun FlareInfoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FlareTheme.colors.accent
) {
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        FlareGlassButton(
            onClick = onClick,
            modifier = Modifier.size(18.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_i),
                contentDescription = null,
                tint = FlareTheme.colors.textPrimary,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun FlareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = FlareTheme.colors.accent,
    icon: Int? = null,
    enabled: Boolean = true
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0f, label = "buttonAlpha")
    
    if (alpha > 0.01f) {
        Row(
            modifier = modifier
                .alpha(alpha)
                .clip(RoundedCornerShape(16.dp))
                .background(FlareTheme.colors.bgItem)
                .border(1.dp, FlareTheme.colors.glassStroke, RoundedCornerShape(16.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                fontFamily = GeologicaMedium,
                fontSize = 15.sp,
                color = FlareTheme.colors.textPrimary
            )
        }
    }
}

fun Modifier.fadingEdge(
    showTop: Boolean,
    showBottom: Boolean,
    topFadeHeight: androidx.compose.ui.unit.Dp = 16.dp,
    bottomFadeHeight: androidx.compose.ui.unit.Dp = 16.dp
): Modifier = composed {
    val animatedTopHeight by animateDpAsState(
        targetValue = if (showTop) topFadeHeight else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "topFadeHeight"
    )
    val animatedBottomHeight by animateDpAsState(
        targetValue = if (showBottom) bottomFadeHeight else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "bottomFadeHeight"
    )

    val isDark = FlareTheme.colors.isDark
    val animatedTopLineAlpha by animateFloatAsState(
        targetValue = if (showTop) (if (isDark) 0.15f else 0.12f) else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "topLineAlpha"
    )
    val animatedBottomLineAlpha by animateFloatAsState(
        targetValue = if (showBottom) (if (isDark) 0.15f else 0.12f) else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "bottomLineAlpha"
    )

    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            
            val topFadeHeightPx = animatedTopHeight.toPx()
            val bottomFadeHeightPx = animatedBottomHeight.toPx()
            
            val numStops = 12
            
            if (topFadeHeightPx > 0.5f) {
                val topColors = List(numStops) { i ->
                    val progress = i.toFloat() / (numStops - 1)
                    val alpha = 0.28f + 0.72f * (progress * progress * (3f - 2f * progress))
                    Color.Black.copy(alpha = alpha)
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = topColors,
                        startY = 0f,
                        endY = topFadeHeightPx
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            
            if (bottomFadeHeightPx > 0.5f) {
                val bottomColors = List(numStops) { i ->
                    val progress = i.toFloat() / (numStops - 1)
                    val t = 1f - progress
                    val alpha = 0.28f + 0.72f * (t * t * (3f - 2f * t))
                    Color.Black.copy(alpha = alpha)
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = bottomColors,
                        startY = size.height - bottomFadeHeightPx,
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

            
            if (animatedTopLineAlpha > 0f) {
                val lineColor = if (isDark) Color.White else Color.Black
                drawLine(
                    color = lineColor.copy(alpha = animatedTopLineAlpha),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            if (animatedBottomLineAlpha > 0f) {
                val lineColor = if (isDark) Color.White else Color.Black
                drawLine(
                    color = lineColor.copy(alpha = animatedBottomLineAlpha),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
        }
}


@Composable
fun FlareGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = FlareTheme.colors.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "flareGlassButtonGlowAlpha"
    )
    
    val buttonBgBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isPressed) 0.22f else 0.08f),
                Color.White.copy(alpha = if (isPressed) 0.12f else 0.02f)
            )
        } else {
            listOf(
                Color.Black.copy(alpha = if (isPressed) 0.12f else 0.05f),
                Color.Black.copy(alpha = if (isPressed) 0.06f else 0.02f)
            )
        }
    )

    val buttonBorderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isPressed) 0.45f else 0.15f),
                if (isPressed) Color.White.copy(alpha = 0.15f) else Color.Transparent
            )
        } else {
            listOf(
                Color.Black.copy(alpha = if (isPressed) 0.22f else 0.10f),
                Color.Black.copy(alpha = if (isPressed) 0.06f else 0.02f)
            )
        }
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                6.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(
                                    (glowAlpha * 180).toInt(),
                                    255,
                                    255,
                                    255
                                )
                            )
                        }
                        canvas.nativeCanvas.drawCircle(
                            size.width / 2f,
                            size.height / 2f,
                            size.width / 2f - 0.5.dp.toPx(),
                            paint
                        )
                    }
                }
            }
            .background(
                brush = buttonBgBrush,
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                brush = buttonBorderBrush,
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
