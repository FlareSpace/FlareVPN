package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import dev.chrisbanes.haze.materials.CupertinoMaterials
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
    
    val lightTint = Color(0xFFEFF1F4).copy(alpha = 0.3f)
    val darkTint = Color(0xFF24262A).copy(alpha = 0.55f)
    val hazeStyle = HazeStyle(
        blurRadius  = 45.dp,
        tints       = listOf(HazeTint(color = if (isDark) darkTint else lightTint)),
        noiseFactor = 0.01f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) androidx.compose.ui.graphics.Color.Transparent else flare.client.app.ui.theme.FlareTheme.colors.bgItem.copy(alpha = 0.95f)).hazeEffect(state = hazeState, style = hazeStyle) {
                alpha = scrollProgress
            }
            .drawBehind {
                if (scrollProgress > 0f) {
                    val strokeWidth = 0.5.dp.toPx()
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
                modifier = Modifier.basicMarquee(),
                fontFamily = GeologicaMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = FlareTheme.colors.textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
fun FlareSubScreenTopBar(
    title: String,
    hazeState: HazeState,
    scrollState: ScrollState? = null,
    lazyListState: LazyListState? = null,
    onBack: (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val isDark = FlareTheme.colors.isDark
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("flare_settings", android.content.Context.MODE_PRIVATE) }
    var appearanceType by remember { mutableStateOf(prefs.getInt("appearance_type", 1)) }
    
    androidx.compose.runtime.DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "appearance_type") {
                appearanceType = sharedPreferences.getInt("appearance_type", 1)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Crossfade(targetState = appearanceType, label = "appearanceCrossfade") { type ->
        when (type) {
            0 -> {
                FlareTopBar(
                    title = title,
                    hazeState = hazeState,
                    scrollState = scrollState,
                    lazyListState = lazyListState,
                    onBack = onBack,
                    subtitle = subtitle,
                    actions = actions
                )
            }
            1 -> {
                val blurEnabled = flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled

            val scrollOffset = when {
                scrollState != null -> scrollState.value
                lazyListState != null -> {
                    if (lazyListState.firstVisibleItemIndex > 0) 500 else lazyListState.firstVisibleItemScrollOffset
                }
                else -> 0
            }
            
            val density = LocalDensity.current
            val maxScrollPx = with(density) { 48.dp.toPx() }
            val scrollProgress = (scrollOffset / maxScrollPx).coerceIn(0f, 1f)

            val collapsedHeight = if (subtitle != null) 56.dp else 48.dp
            val expandedHeight = if (subtitle != null) 80.dp else 64.dp
            val currentHeight = expandedHeight - (expandedHeight - collapsedHeight) * scrollProgress

            val expandedFontSize = if (subtitle != null) 22f else 24f
            val collapsedFontSize = 18f
            val currentFontSize = (expandedFontSize - (expandedFontSize - collapsedFontSize) * scrollProgress).sp

            val maxPadding = if (onBack != null || actions != null) {
                (20 + 52 * scrollProgress).dp
            } else {
                20.dp
            }
            val startPadding = maxPadding
            val endPadding = maxPadding

            val hazeTintColor = if (isDark) {
                Color.Transparent
            } else {
                Color.White.copy(alpha = 0.15f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (blurEnabled) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurRadius = 25.dp
                                progressive = HazeProgressive.verticalGradient(
                                    startIntensity = 1f,
                                    endIntensity = 0f,
                                    preferPerformance = true
                                )
                                tints = listOf(HazeTint(color = hazeTintColor))
                                noiseFactor = 0f
                            }
                        } else {
                            val baseColor = if (isDark) Color.Black else Color.White
                            Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        baseColor.copy(alpha = 0.5f),
                                        baseColor.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }
                    )
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 12.dp)
                    .height(currentHeight)
            ) {
                if (onBack != null) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "backBtnScale"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = onBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(
                                    width = if (isDark) 0.5.dp else 1.dp,
                                    brush = if (isDark) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.65f),
                                                Color(0x09000000)
                                            )
                                        )
                                    },
                                    shape = CircleShape
                                )
                                .flareGlass(
                                    isDark = isDark,
                                    radius = 24f,
                                    intensity = 1.6f,
                                    index = 1.5f,
                                    glassHeight = 0.5f,
                                    thickness = 5f,
                                    hasOutline = false
                                )
                                .then(
                                    if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) {
                                        Modifier
                                            .background(Color.Transparent)
                                            .hazeEffect(state = hazeState) {
                                                blurRadius = 10.dp
                                                tints = listOf(HazeTint(color = if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)))
                                                noiseFactor = 0f
                                            }
                                    } else {
                                        Modifier.background(
                                            color = if (isDark) Color(0xA0202228) else Color(0x87FFFFFF),
                                            shape = CircleShape
                                        )
                                    }
                                )
                        )

                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = null,
                            tint = FlareTheme.colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(currentHeight)
                        .fillMaxWidth()
                        .padding(start = startPadding, end = endPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (subtitle == null) {
                        Text(
                            text = title,
                            modifier = Modifier.basicMarquee(),
                            fontFamily = GeologicaMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = currentFontSize,
                            color = FlareTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.basicMarquee(),
                                fontFamily = GeologicaMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = currentFontSize,
                                color = FlareTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            if (scrollProgress < 0.95f) {
                                Spacer(modifier = Modifier.height((4 * (1f - scrollProgress)).dp))
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            alpha = 1f - scrollProgress
                                            scaleX = 1f - 0.1f * scrollProgress
                                            scaleY = 1f - 0.1f * scrollProgress
                                        }
                                ) {
                                    subtitle()
                                }
                            }
                        }
                    }
                }

                if (actions != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = actions
                    )
                }
            }
            }
            else -> {
                FlareTopOled(
                    title = title,
                    hazeState = hazeState,
                    scrollState = scrollState,
                    lazyListState = lazyListState,
                    onBack = onBack,
                    subtitle = subtitle,
                    actions = actions
                )
            }
        }
    }
}

@Composable
fun FlareTopOled(
    title: String,
    hazeState: HazeState,
    scrollState: ScrollState? = null,
    lazyListState: LazyListState? = null,
    onBack: (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val isDark = FlareTheme.colors.isDark
    
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
        Color.White.copy(alpha = 0.15f * scrollProgress)
    } else {
        Color.Black.copy(alpha = 0.12f * scrollProgress)
    }
    
    val bgColor = if (isDark) Color.Black else Color.White
    
    val hazeStyle = HazeStyle(
        blurRadius = 45.dp,
        tints = listOf(HazeTint(color = bgColor.copy(alpha = 0.5f))),
        noiseFactor = 0.01f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) Color.Transparent else bgColor.copy(alpha = 0.95f))
            .hazeEffect(state = hazeState, style = hazeStyle) {
                alpha = 1f
            }
            .drawBehind {
                if (scrollProgress > 0f) {
                    val strokeWidth = 0.5.dp.toPx()
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
                modifier = Modifier.basicMarquee(),
                fontFamily = GeologicaMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = FlareTheme.colors.textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

@Composable
fun FlareGlassTooltip(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    text: String,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    var shouldRenderPopup by remember { mutableStateOf(false) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(expanded) {
        if (expanded) {
            shouldRenderPopup = true
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            if (shouldRenderPopup) {
                animProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                shouldRenderPopup = false
            }
        }
    }

    if (shouldRenderPopup) {
        androidx.compose.ui.window.Popup(
            onDismissRequest = onDismissRequest,
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

            val scale = 0.3f + 0.7f * animProgress.value
            val alpha = animProgress.value.coerceIn(0f, 1f)

            Box(
                modifier = modifier
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
                                it.background(if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) androidx.compose.ui.graphics.Color.Transparent else flare.client.app.ui.theme.FlareTheme.colors.bgItem.copy(alpha = 0.95f)).hazeEffect(state = hazeState) { blurRadius = 3.dp }
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
}
