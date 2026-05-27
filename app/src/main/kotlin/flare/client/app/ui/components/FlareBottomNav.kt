package flare.client.app.ui.components

import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .width(currentWidthDp)
                .height(64.dp)
                .bottomNavSoftShadow(isDarkTheme),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
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
                                blurRadius = 2.5.dp
                            }
                        } else {
                            it.background(
                                color = if (isDarkTheme) Color(0xA0202228) else Color(0x87FFFFFF),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                            )
                        }
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.08f)
                                )
                            }
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                    )
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
                    isShrunk = isShrunk
                )
            }
        }

        
        if (isShrunk || arrowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(64.dp)
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
                        id = if (isShrunkToHome) R.drawable.ic_nav_home else R.drawable.ic_arrow_right
                    ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(64.dp)
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
    isShrunk: Boolean
) {
    val density = LocalDensity.current
    val dp = density.density

    var containerWidthPx  by remember { mutableStateOf(0f) }
    var containerHeightPx by remember { mutableStateOf(0f) }

    
    val pillHeight by derivedStateOf {
        if (containerHeightPx > 0f) containerHeightPx - 14f * dp
        else 50f * dp
    }

    val leftFrac  = remember { Animatable(selectedIndex / 3f) }
    val rightFrac = remember { Animatable((selectedIndex + 1) / 3f) }
    val scope = rememberCoroutineScope()

    
    var lastIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(selectedIndex) {
        val newL = selectedIndex / 3f
        val newR = (selectedIndex + 1) / 3f
        val animate = lastIndex >= 0 && lastIndex != selectedIndex
        if (animate) {
            val spec = tween<Float>(
                durationMillis = (340 * 1.2f).toInt(),
                easing = Easing { AnticipateOvershootInterpolator(0.6f, 1.2f).getInterpolation(it) }
            )
            launch { leftFrac.animateTo(newL, spec) }
            launch { rightFrac.animateTo(newR, spec) }
        } else {
            leftFrac.snapTo(newL)
            rightFrac.snapTo(newR)
        }
        lastIndex = selectedIndex
    }

    val glowAlpha = remember { Animatable(0f) }
    val expansion = remember { Animatable(0f) }

    
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

                    
                    val pad      = 8f * dp
                    val curLeft  = leftFrac.value  * cw + pad
                    val curRight = rightFrac.value * cw - pad
                    val onPill   = startX >= curLeft && startX <= curRight

                    val dragStartLeftFrac  = leftFrac.value
                    val dragStartRightFrac = rightFrac.value
                    val dragThreshold      = 10f * dp
                    var dragIntercepted    = false

                    
                    if (onPill) {
                        scope.launch {
                            launch { glowAlpha.animateTo(1f, tween(120)) }
                            launch {
                                expansion.animateTo(5f, tween(200,
                                    easing = Easing { OvershootInterpolator(1.4f).getInterpolation(it) }))
                            }
                        }
                    }

                    do {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val delta = change.position.x - startX

                        if (!dragIntercepted && abs(delta) > dragThreshold && onPill) {
                            dragIntercepted = true
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
                        scope.launch {
                            launch { glowAlpha.animateTo(0f, tween(200)) }
                            launch {
                                expansion.animateTo(0f, tween(300,
                                    easing = Easing { OvershootInterpolator(1.4f).getInterpolation(it) }))
                            }
                            val spec = tween<Float>(
                                durationMillis = (340 * 1.2f).toInt(),
                                easing = Easing { AnticipateOvershootInterpolator(0.6f, 1.2f).getInterpolation(it) }
                            )
                            launch { leftFrac.animateTo(newTab / 3f, spec) }
                            launch { rightFrac.animateTo((newTab + 1) / 3f, spec) }
                        }
                        latestOnTabSelected.value(newTab)
                    } else {
                        
                        scope.launch {
                            launch { glowAlpha.animateTo(0f, tween(100)) }
                            launch { expansion.animateTo(0f, tween(150)) }
                        }
                        val now = System.currentTimeMillis()
                        if (touchedTab == 1 && latestSelectedIndex.value == 1 && now - lastTapMs < 350L) {
                            
                            lastTapMs = 0L
                            latestOnDoubleTap.value()
                        } else {
                            lastTapMs = now
                            latestOnTabSelected.value(touchedTab)
                        }
                    }
                }
            }

    ) {
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || pillHeight <= 0f) return@Canvas

            
            val cy    = size.height - 32f * dp
            val halfH = (pillHeight / 2f) + (expansion.value * dp)
            val radius = halfH

            val pad = 8f * dp
            val cw = size.width
            val curLeft = leftFrac.value * cw + pad
            val curRight = rightFrac.value * cw - pad
            val rect = Rect(curLeft, cy - halfH, curRight, cy + halfH)

            drawIntoCanvas { canvas ->
                val nc          = canvas.nativeCanvas
                val glow        = glowAlpha.value
                val effGlow     = 0.05f + glow * 0.95f
                val ambMargin   = 22f * dp

                val aEndR = (accentEnd.red   * 255).toInt()
                val aEndG = (accentEnd.green * 255).toInt()
                val aEndB = (accentEnd.blue  * 255).toInt()
                val aStR  = (accentStart.red   * 255).toInt()
                val aStG  = (accentStart.green * 255).toInt()
                val aStB  = (accentStart.blue  * 255).toInt()

                
                val ambPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    shader = android.graphics.RadialGradient(
                        rect.center.x, rect.center.y,
                        ((rect.width + ambMargin * 2) * 0.6f).coerceAtLeast(1f),
                        intArrayOf(
                            android.graphics.Color.argb((50 * effGlow).toInt(), aEndR, aEndG, aEndB),
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
                                android.graphics.Color.argb((180 * glow).toInt(), aStR, aStG, aStB),
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
                            android.graphics.Color.argb(255, aStR, aStG, aStB),
                            android.graphics.Color.argb(255, aEndR, aEndG, aEndB)
                        ),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )
                }
                nc.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, radius, pillPaint)

                
                val innerGlow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1.0f * dp
                    shader = android.graphics.LinearGradient(
                        0f, rect.top, 0f, rect.bottom,
                        intArrayOf(
                            android.graphics.Color.argb(100, 255, 255, 255),
                            android.graphics.Color.argb(0,   255, 255, 255)
                        ),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )
                }
                val innerB = 1.0f * dp
                nc.drawRoundRect(
                    rect.left + innerB, rect.top + innerB,
                    rect.right - innerB, rect.bottom - innerB,
                    radius - innerB, radius - innerB, innerGlow
                )

                
                val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style       = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1.5f * dp
                    shader = if (isNightMode) {
                        android.graphics.LinearGradient(
                            0f, rect.top, 0f, rect.bottom,
                            intArrayOf(
                                android.graphics.Color.argb(200, 255, 255, 255),
                                android.graphics.Color.argb(40,  255, 255, 255),
                                android.graphics.Color.argb(80,  aEndR, aEndG, aEndB)
                            ),
                            floatArrayOf(0f, 0.45f, 1f),
                            android.graphics.Shader.TileMode.CLAMP
                        )
                    } else {
                        android.graphics.LinearGradient(
                            0f, rect.top, 0f, rect.bottom,
                            intArrayOf(
                                android.graphics.Color.argb(180, aStR, aStG, aStB),
                                android.graphics.Color.argb(60,  aEndR, aEndG, aEndB),
                                android.graphics.Color.argb(100, aEndR, aEndG, aEndB)
                            ),
                            floatArrayOf(0f, 0.45f, 1f),
                            android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                }
                val b = 0.65f * dp
                nc.drawRoundRect(
                    rect.left + b, rect.top + b, rect.right - b, rect.bottom - b,
                    radius - b, radius - b, borderPaint
                )
            }
        }

        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
        ) {
            NavTabIcon(R.drawable.ic_nav_settings, selectedIndex == 0, Modifier.weight(1f))
            NavTabIcon(R.drawable.ic_nav_home,     selectedIndex == 1, Modifier.weight(1f))
            NavTabIcon(R.drawable.ic_nav_servers,  selectedIndex == 2, Modifier.weight(1f))
        }
    }
}



@Composable
private fun NavTabIcon(
    iconRes: Int,
    isSelected: Boolean,
    modifier: Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.45f,
        animationSpec = tween(220), label = "iconAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.88f,
        animationSpec = tween(220), label = "iconScale"
    )

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = FlareTheme.colors.navIconTint,
            modifier = Modifier
                .size(64.dp)
                .padding(20.dp)
                .graphicsLayer {
                    this.alpha  = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                }
        )
    }
}
