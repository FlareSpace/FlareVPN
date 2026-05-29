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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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

@Composable
fun FlareSideNav(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onDoubleTapPill: () -> Unit = {},
    accentColorStart: Color = Color(0xFF50C8FF),
    accentColorEnd: Color = Color(0xFF0064FF),
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val density = LocalDensity.current
    val isDarkTheme = FlareTheme.colors.isDark
    
    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        isReady = true
    }

    val navTranslationX by animateDpAsState(
        targetValue = if (isVisible && isReady) 0.dp else (-200).dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = if (isVisible)
                Easing { android.view.animation.DecelerateInterpolator().getInterpolation(it) }
            else
                Easing { android.view.animation.AccelerateInterpolator().getInterpolation(it) }
        ), label = "navTransX"
    )

    BoxWithConstraints(
        modifier = modifier
            .width(100.dp)
            .fillMaxHeight()
            .offset(x = navTranslationX),
        contentAlignment = Alignment.CenterStart
    ) {
        val dpValue = density.density
        val fullHeightDp = 280.dp
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .width(64.dp)
                .height(fullHeightDp)
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
                    .requiredHeight(fullHeightDp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                VerticalLiquidPillCanvas(
                    selectedIndex = selectedIndex,
                    onTabSelected = onTabSelected,
                    onDoubleTapPill = onDoubleTapPill,
                    accentStart = accentColorStart,
                    accentEnd = accentColorEnd,
                    isNightMode = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun VerticalLiquidPillCanvas(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onDoubleTapPill: () -> Unit,
    accentStart: Color,
    accentEnd: Color,
    isNightMode: Boolean
) {
    val density = LocalDensity.current
    val dp = density.density

    var containerWidthPx  by remember { mutableStateOf(0f) }
    var containerHeightPx by remember { mutableStateOf(0f) }

    val pillWidth by derivedStateOf {
        if (containerWidthPx > 0f) containerWidthPx - 14f * dp
        else 50f * dp
    }

    val topFrac = remember { Animatable(selectedIndex / 3f) }
    val bottomFrac = remember { Animatable((selectedIndex + 1) / 3f) }
    val scope = rememberCoroutineScope()

    var lastIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(selectedIndex) {
        val newT = selectedIndex / 3f
        val newB = (selectedIndex + 1) / 3f
        val animate = lastIndex >= 0 && lastIndex != selectedIndex
        if (animate) {
            val spec = tween<Float>(
                durationMillis = (340 * 1.2f).toInt(),
                easing = Easing { AnticipateOvershootInterpolator(0.6f, 1.2f).getInterpolation(it) }
            )
            launch { topFrac.animateTo(newT, spec) }
            launch { bottomFrac.animateTo(newB, spec) }
        } else {
            topFrac.snapTo(newT)
            bottomFrac.snapTo(newB)
        }
        lastIndex = selectedIndex
    }

    val glowAlpha = remember { Animatable(0f) }
    val expansion = remember { Animatable(0f) }

    val latestOnTabSelected  = rememberUpdatedState(onTabSelected)
    val latestOnDoubleTap    = rememberUpdatedState(onDoubleTapPill)
    val latestSelectedIndex  = rememberUpdatedState(selectedIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                containerWidthPx  = it.size.width.toFloat()
                containerHeightPx = it.size.height.toFloat()
            }
            .pointerInput(Unit) {
                var lastTapMs = 0L
                awaitEachGesture {
                    val down   = awaitFirstDown(requireUnconsumed = false)
                    val ch = containerHeightPx
                    if (ch <= 0f) return@awaitEachGesture

                    val startY = down.position.y

                    val touchedTab = when {
                        startY < ch / 3f       -> 0
                        startY < 2f * ch / 3f  -> 1
                        else                   -> 2
                    }

                    val pad      = 8f * dp
                    val curTop   = topFrac.value  * ch + pad
                    val curBottom = bottomFrac.value * ch - pad
                    val onPill   = startY >= curTop && startY <= curBottom

                    val dragStartTopFrac  = topFrac.value
                    val dragStartBottomFrac = bottomFrac.value
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

                        val delta = change.position.y - startY

                        if (!dragIntercepted && abs(delta) > dragThreshold && onPill) {
                            dragIntercepted = true
                        }

                        if (dragIntercepted) {
                            val stretchFrac = ((abs(delta) * 0.08f).coerceAtMost(18f * dp)) / ch
                            val deltaFrac   = delta / ch
                            val minHeight = 0.08f
                            scope.launch {
                                if (delta > 0) {
                                    val targetBottom = (dragStartBottomFrac + deltaFrac + stretchFrac).coerceIn(0f, 1f)
                                    val targetTop = (dragStartTopFrac + deltaFrac).coerceIn(0f, (targetBottom - minHeight).coerceAtLeast(0f))
                                    topFrac.snapTo(targetTop)
                                    bottomFrac.snapTo(targetBottom)
                                } else {
                                    val targetTop = (dragStartTopFrac + deltaFrac - stretchFrac).coerceIn(0f, 1f)
                                    val targetBottom = (dragStartBottomFrac + deltaFrac).coerceIn((targetTop + minHeight).coerceAtMost(1f), 1f)
                                    topFrac.snapTo(targetTop)
                                    bottomFrac.snapTo(targetBottom)
                                }
                            }
                            change.consume()
                        }
                    } while (true)

                    if (dragIntercepted) {
                        val centerFrac = (topFrac.value + bottomFrac.value) / 2f
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
                            launch { topFrac.animateTo(newTab / 3f, spec) }
                            launch { bottomFrac.animateTo((newTab + 1) / 3f, spec) }
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
            if (size.height <= 0f || pillWidth <= 0f) return@Canvas

            val cx = size.width / 2f
            val halfW = (pillWidth / 2f) + (expansion.value * dp)
            val radius = halfW

            val pad = 8f * dp
            val ch = size.height
            val curTop = topFrac.value * ch + pad
            val curBottom = bottomFrac.value * ch - pad
            val rect = Rect(cx - halfW, curTop, cx + halfW, curBottom)

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
                        ((rect.height + ambMargin * 2) * 0.6f).coerceAtLeast(1f),
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
                            ((rect.height + coreM * 2) * 0.5f).coerceAtLeast(1f),
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

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(64.dp)
        ) {
            val cellWeightModifier = Modifier.weight(1f)
            NavTabIcon(R.drawable.ic_nav_settings, selectedIndex == 0, cellWeightModifier)
            NavTabIcon(R.drawable.ic_nav_home,     selectedIndex == 1, cellWeightModifier)
            NavTabIcon(R.drawable.ic_nav_servers,  selectedIndex == 2, cellWeightModifier)
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
        modifier = modifier.fillMaxWidth(),
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
