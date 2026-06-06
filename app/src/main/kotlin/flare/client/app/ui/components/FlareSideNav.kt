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
        
        val pillPressed = remember { mutableStateOf(false) }
        val panelScale by animateFloatAsState(
            targetValue = if (pillPressed.value) 1.05f else 1.0f,
            animationSpec = tween(
                durationMillis = 200,
                easing = Easing { OvershootInterpolator(1.2f).getInterpolation(it) }
            ), label = "panelScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .width(64.dp)
                .height(fullHeightDp)
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
                                    Color.White.copy(alpha = 0.09f)
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
                    isNightMode = isDarkTheme,
                    onPillPressed = { pillPressed.value = it }
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
    isNightMode: Boolean,
    onPillPressed: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val dp = density.density

    var containerWidthPx  by remember { mutableStateOf(0f) }
    var containerHeightPx by remember { mutableStateOf(0f) }

    val pillWidth by derivedStateOf {
        if (containerWidthPx > 0f) containerWidthPx - 10f * dp
        else 54f * dp
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
                        onPillPressed(true)
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
                        onPillPressed(false)
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
                        onPillPressed(false)
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
            val centerY = (curTop + curBottom) / 2f
            val rawHeight = curBottom - curTop
            val halfH = rawHeight * 0.47f
            val rect = Rect(cx - halfW, centerY - halfH, cx + halfW, centerY + halfH)

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
                            android.graphics.Color.argb(45, aStR, aStG, aStB),
                            android.graphics.Color.argb(45, aEndR, aEndG, aEndB)
                        ),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )
                }
                nc.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius, radius, pillPaint)
            }
        }

        val centerFrac = (topFrac.value + bottomFrac.value) / 2f
        val influence0 = (1f - abs(centerFrac - 1f / 6f) / (1f / 3f)).coerceIn(0f, 1f)
        val influence1 = (1f - abs(centerFrac - 0.5f) / (1f / 3f)).coerceIn(0f, 1f)
        val influence2 = (1f - abs(centerFrac - 5f / 6f) / (1f / 3f)).coerceIn(0f, 1f)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(64.dp)
        ) {
            val cellWeightModifier = Modifier.weight(1f)
            NavTabIcon(
                outlineRes = R.drawable.ic_nav_settings,
                filledRes = R.drawable.ic_nav_settings_filled,
                influence = influence0,
                accentColor = accentStart,
                modifier = cellWeightModifier
            )
            NavTabIcon(
                outlineRes = R.drawable.ic_nav_spark,
                filledRes = R.drawable.ic_nav_spark_filled,
                influence = influence1,
                accentColor = accentStart,
                modifier = cellWeightModifier
            )
            NavTabIcon(
                outlineRes = R.drawable.ic_nav_servers,
                filledRes = R.drawable.ic_nav_servers_filled,
                influence = influence2,
                accentColor = accentStart,
                modifier = cellWeightModifier
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
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .padding(20.dp)
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
