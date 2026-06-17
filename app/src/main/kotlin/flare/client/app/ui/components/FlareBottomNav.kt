package flare.client.app.ui.components

import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.derivedStateOf
import dev.chrisbanes.haze.hazeEffect
import flare.client.app.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import android.os.Build
import flare.client.app.ui.theme.FlareTheme
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Offset


tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Modifier.bottomNavSoftShadow(
    isDark: Boolean,
    cornersRadius: androidx.compose.ui.unit.Dp = 28.dp
): Modifier {
    if (isDark) return this
    return this.drawBehind {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            
            
            val paintAmbient = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    14.dp.toPx(),
                    0f,
                    4.dp.toPx(),
                    android.graphics.Color.argb(32, 0, 0, 0)
                )
            }
            nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornersRadius.toPx(),
                cornersRadius.toPx(),
                paintAmbient
            )
            
            
            val paintSpot = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    8.dp.toPx(),
                    0f,
                    6.dp.toPx(),
                    android.graphics.Color.argb(22, 0, 0, 0)
                )
            }
            nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornersRadius.toPx(),
                cornersRadius.toPx(),
                paintSpot
            )
        }
    }
}







@Composable
fun FlareBottomNav(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    isShrunk: Boolean = false,
    isShrunkToHome: Boolean = false,
    onArrowClick: () -> Unit = {},
    onDoubleTapPill: () -> Unit = {},
    accentColorStart: Color = Color(0xFF50C8FF),
    accentColorEnd: Color = Color(0xFF0064FF),
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val isDarkTheme = FlareTheme.colors.isDark
    
    
    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        
        kotlinx.coroutines.delay(150)
        isReady = true
    }

    
    val navTranslationY by animateDpAsState(
        targetValue = if (isVisible && isReady) 0.dp else 200.dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = if (isVisible)
                Easing { android.view.animation.DecelerateInterpolator().getInterpolation(it) }
            else
                Easing { android.view.animation.AccelerateInterpolator().getInterpolation(it) }
        ), label = "navTransY"
    )

    
    val containerWidthFraction by animateFloatAsState(
        targetValue = if (isShrunk) 0f else 1f,
        animationSpec = tween(
            durationMillis = 450,
            easing = Easing { AnticipateOvershootInterpolator(0.8f).getInterpolation(it) }
        ), label = "widthFrac"
    )

    
    val tabsAlpha by animateFloatAsState(
        targetValue = if (isShrunk) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (isShrunk) 200 else 300,
            delayMillis = if (isShrunk) 0 else 200
        ), label = "tabsAlpha"
    )

    
    val arrowAlpha by animateFloatAsState(
        targetValue = if (isShrunk) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isShrunk) 400 else 200,
            delayMillis = if (isShrunk) 150 else 0
        ), label = "arrowAlpha"
    )
    val arrowScale by animateFloatAsState(
        targetValue = if (isShrunk) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = if (isShrunk) 400 else 200,
            delayMillis = if (isShrunk) 150 else 0,
            easing = if (isShrunk)
                Easing { OvershootInterpolator(1.4f).getInterpolation(it) }
            else
                Easing { android.view.animation.AccelerateInterpolator().getInterpolation(it) }
        ), label = "arrowScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp) 
            .offset(y = navTranslationY),
        contentAlignment = Alignment.BottomCenter
    ) {
        val dpValue = density.density
        val minWpx = 64f * dpValue
        
        
        val fullWidthDp = maxWidth - 40.dp
        val fullWpx = fullWidthDp.value * dpValue
        
        val currentWpx = minWpx + (fullWpx - minWpx) * containerWidthFraction
        val currentWidthDp = with(density) { currentWpx.toDp() }

        val pillPressed = remember { mutableStateOf(false) }
        val panelScale by animateFloatAsState(
            targetValue = if (pillPressed.value) 1.04f else 1.0f,
            animationSpec = tween(
                durationMillis = 200,
                easing = Easing { OvershootInterpolator(1.2f).getInterpolation(it) }
            ),
            label = "panelScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .width(currentWidthDp)
                .height(60.dp)
                .graphicsLayer {
                    scaleX = panelScale
                    scaleY = panelScale
                }
                .bottomNavSoftShadow(isDarkTheme),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = if (isDarkTheme) 0.5.dp else 1.dp,
                        brush = if (isDarkTheme) {
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                    )
                    .flareGlass(
                        isDark = isDarkTheme,
                        radius = 28f,
                        intensity = 1.6f,
                        index = 1.5f,
                        glassHeight = 0.5f,
                        thickness = 5f,
                        hasOutline = false
                      )
                    .let {
                        if (hazeState != null) {
                            it.hazeEffect(state = hazeState) {
                                blurRadius = 3.dp
                            }
                        } else {
                            it.background(
                                color = if (isDarkTheme) Color(0xA0202228) else Color(0x87FFFFFF),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                            )
                        }
                    }
            )

            
            
            
            Box(
                modifier = Modifier
                    .requiredWidth(fullWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = tabsAlpha },
                contentAlignment = Alignment.BottomCenter
            ) {
                LiquidPillCanvas(
                    selectedIndex = selectedIndex,
                    onTabSelected = onTabSelected,
                    onDoubleTapPill = onDoubleTapPill,
                    accentStart = accentColorStart,
                    accentEnd = accentColorEnd,
                    isNightMode = isDarkTheme,
                    isShrunk = isShrunk,
                    onPillPressed = { pillPressed.value = it }
                )
            }
        }

        
        if (isShrunk || arrowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(60.dp)
                    .graphicsLayer { 
                        alpha = arrowAlpha
                        scaleX = arrowScale
                        scaleY = arrowScale 
                    }
                    .pointerInput(isShrunk, isShrunkToHome) {
                        if (!isShrunk) return@pointerInput
                        detectTapGestures(
                            onTap = {
                                if (!isShrunkToHome) {
                                    onArrowClick()
                                }
                            },
                            onDoubleTap = {
                                if (isShrunkToHome) {
                                    onDoubleTapPill()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isShrunkToHome || selectedIndex == 1) R.drawable.ic_nav_spark else R.drawable.ic_arrow_right
                    ),
                    contentDescription = null,
                    tint = FlareTheme.colors.navIconTint,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(16.dp)
                )
            }
        }
    }
}




@Composable
private fun LiquidPillCanvas(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onDoubleTapPill: () -> Unit,
    accentStart: Color,
    accentEnd: Color,
    isNightMode: Boolean,
    isShrunk: Boolean,
    onPillPressed: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val dp = density.density

    var containerWidthPx  by remember { mutableStateOf(0f) }
    var containerHeightPx by remember { mutableStateOf(0f) }

    val pillHeight by derivedStateOf {
        if (containerHeightPx > 0f) containerHeightPx - 10f * dp
        else 46f * dp
    }

    val leftFrac  = remember { Animatable(selectedIndex / 3f) }
    val rightFrac = remember { Animatable((selectedIndex + 1) / 3f) }
    val scope = rememberCoroutineScope()

    var isTapped by remember { mutableStateOf(false) }
    var isTapTransition by remember { mutableStateOf(false) }
    val tapTransitionFrac = remember { Animatable(0f) }

    var lastIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(selectedIndex) {
        val newL = selectedIndex / 3f
        val newR = (selectedIndex + 1) / 3f
        val animate = lastIndex >= 0 && lastIndex != selectedIndex
        if (animate) {
            if (isTapped) {
                isTapped = false
                isTapTransition = true
                
                leftFrac.snapTo(newL)
                rightFrac.snapTo(newR)
                
                launch {
                    tapTransitionFrac.snapTo(0f)
                    tapTransitionFrac.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                    isTapTransition = false
                }
            } else {
                isTapTransition = false
                val spec = tween<Float>(
                    durationMillis = (340 * 1.2f).toInt(),
                    easing = Easing { AnticipateOvershootInterpolator(0.6f, 1.2f).getInterpolation(it) }
                )
                launch { leftFrac.animateTo(newL, spec) }
                launch { rightFrac.animateTo(newR, spec) }
            }
        } else {
            isTapTransition = false
            leftFrac.snapTo(newL)
            rightFrac.snapTo(newR)
        }
        lastIndex = selectedIndex
    }

    val glowAlpha = remember { Animatable(0f) }
    val expansion = remember { Animatable(0f) }
    val dragGlowAlpha = remember { Animatable(0f) }

    val latestOnTabSelected  = rememberUpdatedState(onTabSelected)
    val latestOnDoubleTap    = rememberUpdatedState(onDoubleTapPill)
    val latestSelectedIndex  = rememberUpdatedState(selectedIndex)
    val latestIsShrunk       = rememberUpdatedState(isShrunk)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                containerWidthPx  = it.size.width.toFloat()
                containerHeightPx = it.size.height.toFloat()
            }
            .pointerInput(isShrunk) {
                if (isShrunk) return@pointerInput
                var lastTapMs = 0L
                awaitEachGesture {
                    val down   = awaitFirstDown(requireUnconsumed = false)
                    val cw = containerWidthPx
                      if (cw <= 0f) return@awaitEachGesture

                    val startX = down.position.x

                    val touchedTab = when {
                        startX < cw / 3f       -> 0
                        startX < 2f * cw / 3f  -> 1
                        else                   -> 2
                    }

                    val pad      = 4f * dp
                    val curLeft  = leftFrac.value  * cw + pad
                    val curRight = rightFrac.value * cw - pad
                    val onPill   = startX >= curLeft && startX <= curRight

                    val dragStartLeftFrac  = leftFrac.value
                    val dragStartRightFrac = rightFrac.value
                    val dragThreshold      = 10f * dp
                    var dragIntercepted    = false

                    if (onPill) {
                        onPillPressed(true)
                    }

                    do {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val delta = change.position.x - startX

                        if (!dragIntercepted && abs(delta) > dragThreshold && onPill) {
                            dragIntercepted = true
                            scope.launch {
                                dragGlowAlpha.animateTo(1f, tween(150))
                            }
                        }

                        if (dragIntercepted) {
                            val stretchFrac = ((abs(delta) * 0.08f).coerceAtMost(18f * dp)) / cw
                            val deltaFrac   = delta / cw
                            val minWidth = 0.08f
                            scope.launch {
                                if (delta > 0) {
                                    val targetRight = (dragStartRightFrac + deltaFrac + stretchFrac).coerceIn(0f, 1f)
                                    val targetLeft = (dragStartLeftFrac + deltaFrac).coerceIn(0f, (targetRight - minWidth).coerceAtLeast(0f))
                                    leftFrac.snapTo(targetLeft)
                                    rightFrac.snapTo(targetRight)
                                } else {
                                    val targetLeft = (dragStartLeftFrac + deltaFrac - stretchFrac).coerceIn(0f, 1f)
                                    val targetRight = (dragStartRightFrac + deltaFrac).coerceIn((targetLeft + minWidth).coerceAtMost(1f), 1f)
                                    leftFrac.snapTo(targetLeft)
                                    rightFrac.snapTo(targetRight)
                                }
                            }
                            change.consume()
                        }
                    } while (true)

                    if (dragIntercepted) {
                        val centerFrac = (leftFrac.value + rightFrac.value) / 2f
                        val newTab = when {
                            centerFrac < 1f / 3f -> 0
                            centerFrac < 2f / 3f -> 1
                            else                 -> 2
                        }
                        onPillPressed(false)
                        scope.launch {
                            launch { dragGlowAlpha.animateTo(0f, tween(250)) }
                            val spec = tween<Float>(
                                durationMillis = (340 * 1.2f).toInt(),
                                easing = Easing { AnticipateOvershootInterpolator(0.6f, 1.2f).getInterpolation(it) }
                            )
                            launch { leftFrac.animateTo(newTab / 3f, spec) }
                            launch { rightFrac.animateTo((newTab + 1) / 3f, spec) }
                        }
                        latestOnTabSelected.value(newTab)
                    } else {
                        onPillPressed(false)
                        scope.launch {
                            dragGlowAlpha.animateTo(0f, tween(150))
                        }
                        val now = System.currentTimeMillis()
                        if (touchedTab == 1 && latestSelectedIndex.value == 1 && now - lastTapMs < 350L) {
                            lastTapMs = 0L
                            latestOnDoubleTap.value()
                        } else {
                            lastTapMs = now
                            if (touchedTab != latestSelectedIndex.value) {
                                isTapped = true
                            }
                            latestOnTabSelected.value(touchedTab)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || pillHeight <= 0f) return@Canvas

            val cy = size.height / 2f
            val cw = size.width
            val pad = 4f * dp

            fun drawPillAt(lFrac: Float, rFrac: Float, scaleVal: Float, alphaMult: Float) {
                if (alphaMult <= 0.01f) return
                val halfH = (pillHeight / 2f + expansion.value * dp) * scaleVal
                val radius = halfH
                val curLeft = lFrac * cw + pad
                val curRight = rFrac * cw - pad
                val centerX = (curLeft + curRight) / 2f
                val rawWidth = curRight - curLeft
                val halfW = rawWidth * 0.47f * scaleVal
                val rect = Rect(centerX - halfW, cy - halfH, centerX + halfW, cy + halfH)

                drawIntoCanvas { canvas ->
                    val nc = canvas.nativeCanvas
                    val glow = glowAlpha.value
                    val effGlow = 0.05f + glow * 0.95f
                    val ambMargin = 22f * dp

                    val aEndR = (accentEnd.red * 255).toInt()
                    val aEndG = (accentEnd.green * 255).toInt()
                    val aEndB = (accentEnd.blue * 255).toInt()
                    val aStR = (accentStart.red * 255).toInt()
                    val aStG = (accentStart.green * 255).toInt()
                    val aStB = (accentStart.blue * 255).toInt()

                    val ambPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        shader = android.graphics.RadialGradient(
                            rect.center.x, rect.center.y,
                            ((rect.width + ambMargin * 2) * 0.6f).coerceAtLeast(1f),
                            intArrayOf(
                                android.graphics.Color.argb((50 * effGlow * alphaMult).toInt(), aEndR, aEndG, aEndB),
                                android.graphics.Color.TRANSPARENT
                            ),
                            null, android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                    nc.drawRoundRect(
                        rect.left - ambMargin, rect.top - ambMargin,
                        rect.right + ambMargin, rect.bottom + ambMargin,
                        radius + ambMargin, radius + ambMargin, ambPaint
                    )

                    if (glow > 0.01f) {
                        val coreM = 10f * dp
                        val corePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            shader = android.graphics.RadialGradient(
                                rect.center.x, rect.center.y,
                                ((rect.width + coreM * 2) * 0.5f).coerceAtLeast(1f),
                                intArrayOf(
                                    android.graphics.Color.argb((180 * glow * alphaMult).toInt(), aStR, aStG, aStB),
                                    android.graphics.Color.argb(0, aEndR, aEndG, aEndB)
                                ),
                                null, android.graphics.Shader.TileMode.CLAMP
                            )
                        }
                        nc.drawRoundRect(
                            rect.left - coreM, rect.top - coreM,
                            rect.right + coreM, rect.bottom + coreM,
                            radius + coreM, radius + coreM, corePaint
                        )
                    }

                    val pillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        if (isNightMode) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                blendMode = android.graphics.BlendMode.SCREEN
                            else
                                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
                        }
                        shader = android.graphics.LinearGradient(
                            0f, rect.top, 0f, rect.bottom,
                            intArrayOf(
                                android.graphics.Color.argb((45 * alphaMult).toInt(), aStR, aStG, aStB),
                                android.graphics.Color.argb((45 * alphaMult).toInt(), aEndR, aEndG, aEndB)
                            ),
                            null, android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                    nc.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, radius, pillPaint)
                }
            }

            
            val glowAlphaVal = dragGlowAlpha.value
            if (glowAlphaVal > 0.01f) {
                val curLeft = leftFrac.value * cw + pad
                val curRight = rightFrac.value * cw - pad
                val centerX = (curLeft + curRight) / 2f
                val rawWidth = curRight - curLeft
                val halfW = rawWidth * 0.47f
                val halfH = (pillHeight / 2f) + (expansion.value * dp)
                val rect = Rect(centerX - halfW, cy - halfH, centerX + halfW, cy + halfH)
                drawIntoCanvas { canvas ->
                    val panelPath = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = Rect(0f, 0f, size.width, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx())
                            )
                        )
                    }
                    clipPath(panelPath) {
                        if (rect.width > 0.1f) {
                            val glareWidth = rect.width * 1.5f
                            val glareHeight = 3.dp.toPx()
                            val glareColor = Color.White.copy(alpha = 0.9f * glowAlphaVal)
                            val glareAccentTop = accentStart.copy(alpha = 0.6f * glowAlphaVal)
                            val glareAccentBottom = accentEnd.copy(alpha = 0.6f * glowAlphaVal)

                            drawOval(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, glareAccentTop, Color.Transparent),
                                    startX = centerX - glareWidth / 2,
                                    endX = centerX + glareWidth / 2
                                ),
                                topLeft = Offset(centerX - glareWidth / 2, -glareHeight),
                                size = androidx.compose.ui.geometry.Size(glareWidth, glareHeight * 2)
                            )
                            
                            drawOval(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, glareColor, Color.Transparent),
                                    startX = centerX - glareWidth / 4,
                                    endX = centerX + glareWidth / 4
                                ),
                                topLeft = Offset(centerX - glareWidth / 4, -glareHeight / 2),
                                size = androidx.compose.ui.geometry.Size(glareWidth / 2, glareHeight)
                            )

                            drawOval(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, glareAccentBottom, Color.Transparent),
                                    startX = centerX - glareWidth / 2,
                                    endX = centerX + glareWidth / 2
                                ),
                                topLeft = Offset(centerX - glareWidth / 2, size.height - glareHeight),
                                size = androidx.compose.ui.geometry.Size(glareWidth, glareHeight * 2)
                            )
                            
                            drawOval(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, glareColor, Color.Transparent),
                                    startX = centerX - glareWidth / 4,
                                    endX = centerX + glareWidth / 4
                                ),
                                topLeft = Offset(centerX - glareWidth / 4, size.height - glareHeight / 2),
                                size = androidx.compose.ui.geometry.Size(glareWidth / 2, glareHeight)
                            )
                        }
                    }
                }
            }

            if (isTapTransition) {
                val frac = tapTransitionFrac.value
                val oldL = lastIndex.coerceAtLeast(0) / 3f
                val oldR = (lastIndex.coerceAtLeast(0) + 1) / 3f
                val newL = selectedIndex / 3f
                val newR = (selectedIndex + 1) / 3f

                
                drawPillAt(oldL, oldR, scaleVal = 1f, alphaMult = 1f - frac)
                
                drawPillAt(newL, newR, scaleVal = 1f, alphaMult = frac)
            } else {
                drawPillAt(leftFrac.value, rightFrac.value, scaleVal = 1f, alphaMult = 1f)
            }
        }

        val centerFrac = (leftFrac.value + rightFrac.value) / 2f
        val influence0 = if (isTapTransition) {
            when (0) {
                selectedIndex -> tapTransitionFrac.value
                lastIndex -> 1f - tapTransitionFrac.value
                else -> 0f
            }
        } else {
            (1f - abs(centerFrac - 1f / 6f) / (1f / 3f)).coerceIn(0f, 1f)
        }
        val influence1 = if (isTapTransition) {
            when (1) {
                selectedIndex -> tapTransitionFrac.value
                lastIndex -> 1f - tapTransitionFrac.value
                else -> 0f
            }
        } else {
            (1f - abs(centerFrac - 0.5f) / (1f / 3f)).coerceIn(0f, 1f)
        }
        val influence2 = if (isTapTransition) {
            when (2) {
                selectedIndex -> tapTransitionFrac.value
                lastIndex -> 1f - tapTransitionFrac.value
                else -> 0f
            }
        } else {
            (1f - abs(centerFrac - 5f / 6f) / (1f / 3f)).coerceIn(0f, 1f)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
        ) {
            NavTabIcon(
                outlineRes = R.drawable.ic_nav_settings,
                filledRes = R.drawable.ic_nav_settings_filled,
                influence = influence0,
                accentColor = accentStart,
                modifier = Modifier.weight(1f)
            )
            NavTabIcon(
                outlineRes = R.drawable.ic_nav_spark,
                filledRes = R.drawable.ic_nav_spark_filled,
                influence = influence1,
                accentColor = accentStart,
                modifier = Modifier.weight(1f)
            )
            NavTabIcon(
                outlineRes = R.drawable.ic_nav_servers,
                filledRes = R.drawable.ic_nav_servers_filled,
                influence = influence2,
                accentColor = accentStart,
                modifier = Modifier.weight(1f)
            )
        }
    }
}



@Composable
private fun NavTabIcon(
    outlineRes: Int,
    filledRes: Int,
    influence: Float,
    accentColor: Color,
    modifier: Modifier
) {
    val scale = 0.88f + 0.12f * influence
    val filledAlpha = influence
    val outlineAlpha = 0.45f * (1f - influence)

    val iconColor = androidx.compose.ui.graphics.lerp(
        start = FlareTheme.colors.navIconTint,
        stop = accentColor,
        fraction = influence
    )

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .padding(16.dp)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                }
        ) {
            if (outlineAlpha > 0.01f) {
                Icon(
                    painter = painterResource(id = outlineRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = outlineAlpha }
                )
            }
            if (filledAlpha > 0.01f) {
                Icon(
                    painter = painterResource(id = filledRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = filledAlpha }
                )
            }
        }
    }
}
