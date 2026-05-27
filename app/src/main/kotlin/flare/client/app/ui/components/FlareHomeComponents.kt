package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import kotlin.math.cos
import kotlin.math.sin
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.ui.MainViewModel
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size




private fun createNoiseBitmap(width: Int = 128, height: Int = 128, opacity: Float = 0.015f): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    val random = java.util.Random()
    for (i in pixels.indices) {
        val noise = random.nextInt(256)
        val alpha = (random.nextFloat() * opacity * 255).toInt()
        pixels[i] = (alpha shl 24) or (noise shl 16) or (noise shl 8) or noise
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private val meshBasePositions = listOf(
    Offset(0.1f, 0.2f),
    Offset(0.8f, 0.8f),
    Offset(0.9f, 0.1f),
    Offset(0.2f, 0.9f),
    Offset(0.5f, 0.4f),
    Offset(0.1f, 0.8f),
    Offset(0.7f, 0.3f)
)

private val meshRadiuses = listOf(800f, 750f, 700f, 800f, 700f, 700f, 600f)

@Composable
fun FlareHomeBackground(
    isGradientEnabled: Boolean = true,
    isAnimationEnabled: Boolean = true,
    animationSpeed: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val themeColors = FlareTheme.colors
    val isDark = themeColors.isDark

    if (!isGradientEnabled) {
        Box(modifier = modifier.fillMaxSize().background(themeColors.bgDark))
        return
    }

    var time by remember { mutableStateOf(0f) }

    if (isAnimationEnabled) {
        LaunchedEffect(animationSpeed) {
            var lastTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { frameTime ->
                    val deltaSeconds = (frameTime - lastTime) / 1_000_000_000f
                    lastTime = frameTime
                    time += deltaSeconds * animationSpeed * 0.4f 
                }
            }
        }
    }

    val density = LocalDensity.current
    
    
    val extraColor1Start = if (isDark) Color(0x0AFF3D00) else Color(0x12FF3D00) 
    val extraColor1End = Color(0x00FF3D00)
    val extraColor2Start = if (isDark) Color(0x0A7C4DFF) else Color(0x127C4DFF) 
    val extraColor2End = Color(0x007C4DFF)

    val brushes = remember(themeColors, density, isDark) {
        val darkAlphaMult = if (isDark) 0.35f else 1.0f 
        
        val colorsList = listOf(
            themeColors.gradientBlueStart.let { it.copy(alpha = it.alpha * darkAlphaMult) } to themeColors.gradientBlueEnd,
            themeColors.gradientPurpleStart.let { it.copy(alpha = it.alpha * darkAlphaMult * 0.8f) } to themeColors.gradientPurpleEnd,
            themeColors.gradientMagentaStart.let { it.copy(alpha = it.alpha * darkAlphaMult) } to themeColors.gradientMagentaEnd,
            themeColors.gradientCyanStart.let { it.copy(alpha = it.alpha * darkAlphaMult * 1.5f) } to themeColors.gradientCyanEnd, 
            extraColor1Start to extraColor1End,
            extraColor2Start to extraColor2End,
            themeColors.gradientWhiteStart.let { it.copy(alpha = it.alpha * (if (isDark) 0.05f else 0.4f)) } to themeColors.gradientWhiteEnd
        )
        colorsList.mapIndexed { i, (start, end) ->
            val radiusPx = meshRadiuses[i] * density.density
            Brush.radialGradient(
                colors = listOf(start, end),
                center = Offset.Zero,
                radius = radiusPx
            )
        }
    }

    val noiseBitmap = remember {
        createNoiseBitmap(opacity = if (isDark) 0.03f else 0.02f).asImageBitmap()
    }
    val noiseBrush = remember(noiseBitmap) {
        ShaderBrush(
            ImageShader(
                image = noiseBitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.gradientBase)
            .graphicsLayer() 
            .drawBehind {
                val width = size.width
                val height = size.height
                if (width <= 0f || height <= 0f) return@drawBehind

                val blendMode = BlendMode.SrcOver

                brushes.forEachIndexed { i, brush ->
                    val phase = i * 1.5f
                    val speedX = 0.8f + (i * 0.12f)
                    val speedY = 0.6f + (i * 0.15f)

                    val offsetX = if (isAnimationEnabled) {
                        (sin(time * speedX + phase) * 0.35f + cos(time * 0.6f * speedX) * 0.15f)
                    } else 0f
                    
                    val offsetY = if (isAnimationEnabled) {
                        (cos(time * speedY + phase) * 0.35f + sin(time * 0.5f * speedY) * 0.15f)
                    } else 0f

                    val base = meshBasePositions[i]
                    val center = Offset(
                        (base.x + offsetX) * width,
                        (base.y + offsetY) * height
                    )

                    val radiusPx = meshRadiuses[i] * density.density

                    withTransform({
                        translate(center.x, center.y)
                    }) {
                        drawCircle(
                            brush = brush,
                            center = Offset.Zero,
                            radius = radiusPx,
                            blendMode = blendMode
                        )
                    }
                }

                drawRect(brush = noiseBrush)
            }
    )
}


@Composable
fun FlareConnectButton(
    connectionState: MainViewModel.ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }

    val animatedRotationX by animateFloatAsState(targetValue = rotationX, label = "rotX")
    val animatedRotationY by animateFloatAsState(targetValue = rotationY, label = "rotY")
    
    val connectingProgress by animateFloatAsState(
        targetValue = if (connectionState == MainViewModel.ConnectionState.CONNECTING) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "connectingProgress"
    )

    val connectedProgress by animateFloatAsState(
        targetValue = if (connectionState == MainViewModel.ConnectionState.CONNECTED) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "connectedProgress"
    )

    val overshootEasing = remember { CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = if (scale == 1f) {
            tween(durationMillis = 300, easing = overshootEasing)
        } else {
            tween(durationMillis = 100)
        },
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    val connectingRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connectingRotation1"
    )

    val connectingRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connectingRotation2"
    )

    val maxTilt = 12f

    val rimStartOff = FlareTheme.colors.btnConnectRimStart
    val rimEndOff = FlareTheme.colors.btnConnectRimEnd
    val bodyStartOff = FlareTheme.colors.btnConnectBodyStart
    val bodyCenterOff = FlareTheme.colors.btnConnectBodyCenter
    val bodyEndOff = FlareTheme.colors.btnConnectBodyEnd
    val iconTint = FlareTheme.colors.btnConnectIconTint

    val rimStartOn = Color(0xFF050616)
    val rimEndOn = Color(0xFF4A52A0)
    val bodyStartOn = Color(0xFF00FFFF)
    val bodyCenterOn = Color(0xFF0066FF)
    val bodyEndOn = Color(0xFF5500FF)

    Box(
        modifier = modifier
            .size(300.dp)
            .graphicsLayer {
                this.rotationX = animatedRotationX
                this.rotationY = animatedRotationY
                this.scaleX = animatedScale
                this.scaleY = animatedScale
                this.cameraDistance = 8f * 300f * density
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        scale = 0.92f
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        rotationY = ((offset.x - centerX) / centerX).coerceIn(-1.5f, 1.5f) * maxTilt
                        rotationX = -((offset.y - centerY) / centerY).coerceIn(-1.5f, 1.5f) * maxTilt
                        
                        tryAwaitRelease()
                        
                        scale = 1f
                        rotationX = 0f
                        rotationY = 0f
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawConnectButtonLayers(
                scope = this,
                connectingProgress = connectingProgress,
                connectedProgress = connectedProgress,
                shimmerPhase = shimmerPhase,
                connectingRotation1 = connectingRotation1,
                connectingRotation2 = connectingRotation2,
                rimStartOff = rimStartOff,
                rimEndOff = rimEndOff,
                bodyStartOff = bodyStartOff,
                bodyCenterOff = bodyCenterOff,
                bodyEndOff = bodyEndOff,
                rimStartOn = rimStartOn,
                rimEndOn = rimEndOn,
                bodyStartOn = bodyStartOn,
                bodyCenterOn = bodyCenterOn,
                bodyEndOn = bodyEndOn
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_power),
            contentDescription = null,
            tint = lerp(iconTint, Color.White, maxOf(connectingProgress, connectedProgress)),
            modifier = Modifier
                .size(56.dp)
                .offset(y = (-4).dp) 
        )
    }
}

private fun drawConnectButtonLayers(
    scope: DrawScope,
    connectingProgress: Float,
    connectedProgress: Float,
    shimmerPhase: Float,
    connectingRotation1: Float,
    connectingRotation2: Float,
    rimStartOff: Color,
    rimEndOff: Color,
    bodyStartOff: Color,
    bodyCenterOff: Color,
    bodyEndOff: Color,
    rimStartOn: Color,
    rimEndOn: Color,
    bodyStartOn: Color,
    bodyCenterOn: Color,
    bodyEndOn: Color
) {
    val width = scope.size.width
    val height = scope.size.height
    val density = scope.density
    
    val totalActiveProgress = maxOf(connectingProgress, connectedProgress)
    val progress = totalActiveProgress

    if (connectedProgress > 0f) {
        val pulseScale1 = 1f + 0.08f * sin(shimmerPhase) * connectedProgress
        val radius1 = 140.dp.toPx(scope) * pulseScale1
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x805B8CFF), Color(0x305B8CFF), Color.Transparent),
                center = Offset(width / 2, height / 2),
                radius = radius1
            ),
            center = Offset(width / 2, height / 2),
            radius = radius1,
            alpha = connectedProgress
        )
    }

    if (connectedProgress > 0f) {
        val pulseScale2 = 1f + 0.05f * cos(shimmerPhase + 1f) * connectedProgress
        val radius2 = 150.dp.toPx(scope) * pulseScale2
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xDDBB00FF), Color(0x882200FF), Color.Transparent),
                center = Offset(width / 2, height / 2),
                radius = radius2
            ),
            center = Offset(width / 2, height / 2),
            radius = radius2,
            alpha = connectedProgress
        )
    }

    if (connectingProgress > 0f) {
        val fastPulseScale = 1f + 0.06f * sin(connectingRotation1 * Math.PI.toFloat() / 180f) * connectingProgress
        val radiusConnecting = 145.dp.toPx(scope) * fastPulseScale
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x6000FFFF), Color(0x205500FF), Color.Transparent),
                center = Offset(width / 2, height / 2),
                radius = radiusConnecting
            ),
            center = Offset(width / 2, height / 2),
            radius = radiusConnecting,
            alpha = connectingProgress
        )
    }

    val shadowColor = lerp(Color(0x66000000), Color(0x88000000), progress)
    drawOvalWithOffsets(scope, shadowColor, 55f, 65f, 55f, 50f)

    val rimStart = lerp(rimStartOff, rimStartOn, progress)
    val rimEnd = lerp(rimEndOff, rimEndOn, progress)
    
    val rimLeft = 53.dp.toPx(scope)
    val rimTop = 52.dp.toPx(scope)
    val rimRight = width - 53.dp.toPx(scope)
    val rimBottom = height - 60.dp.toPx(scope)
    
    val rimCenterX = (rimLeft + rimRight) / 2f
    val rimCenterY = (rimTop + rimBottom) / 2f
    val rimHalfW = (rimRight - rimLeft) / 2f
    val rimHalfH = (rimBottom - rimTop) / 2f
    
    val baseAngle = Math.PI.toFloat() / 4f
    val rimAngle = baseAngle - if (connectedProgress > 0f) shimmerPhase * connectedProgress else 0f
    val rimDx = rimHalfW * cos(rimAngle)
    val rimDy = rimHalfH * sin(rimAngle)
    
    drawOvalWithOffsets(
        scope, 
        Brush.linearGradient(
            colors = listOf(rimEnd, rimStart),
            start = Offset(rimCenterX + rimDx, rimCenterY + rimDy),
            end = Offset(rimCenterX - rimDx, rimCenterY - rimDy)
        ),
        53f, 52f, 53f, 60f
    )

    if (connectingProgress > 0f) {
        val rimW = rimRight - rimLeft
        val rimH = rimBottom - rimTop
        
        val ring1Brush = Brush.sweepGradient(
            0.0f to Color(0x0000FFFF),
            0.5f to Color(0x0000FFFF),
            0.8f to Color(0x4400FFFF),
            0.95f to Color(0xFF00FFFF),
            1.0f to Color(0x0000FFFF)
        )
        
        scope.rotate(degrees = connectingRotation1, pivot = Offset(width / 2, height / 2)) {
            scope.drawOval(
                brush = ring1Brush,
                topLeft = Offset(rimLeft, rimTop),
                size = androidx.compose.ui.geometry.Size(rimW, rimH),
                style = Stroke(width = 3.dp.toPx(scope)),
                alpha = connectingProgress
            )
        }
        
        val ring2Brush = Brush.sweepGradient(
            0.0f to Color(0x00FF00FF),
            0.5f to Color(0x00FF00FF),
            0.8f to Color(0x44FF00FF),
            0.95f to Color(0xFFFF00FF),
            1.0f to Color(0x00FF00FF)
        )
        
        val offset2 = 3.dp.toPx(scope)
        scope.rotate(degrees = connectingRotation2, pivot = Offset(width / 2, height / 2)) {
            scope.drawOval(
                brush = ring2Brush,
                topLeft = Offset(rimLeft + offset2, rimTop + offset2),
                size = androidx.compose.ui.geometry.Size(rimW - 2 * offset2, rimH - 2 * offset2),
                style = Stroke(width = 2.dp.toPx(scope)),
                alpha = connectingProgress
            )
        }
    }

    val bodyStartOnAnim: Color
    val bodyCenterOnAnim: Color
    val bodyEndOnAnim: Color

    if (progress > 0f) {
        val activePhase = if (connectedProgress > 0f) shimmerPhase else (connectingRotation1 * Math.PI.toFloat() / 180f)
        val t = (activePhase / (2f * Math.PI.toFloat())) % 1f
        val c1 = bodyStartOn
        val c2 = bodyCenterOn
        val c3 = bodyEndOn
        
        bodyStartOnAnim = when {
            t < 0.33f -> lerp(c1, c2, t / 0.33f)
            t < 0.66f -> lerp(c2, c3, (t - 0.33f) / 0.33f)
            else -> lerp(c3, c1, (t - 0.66f) / 0.34f)
        }
        
        bodyCenterOnAnim = when {
            t < 0.33f -> lerp(c2, c3, t / 0.33f)
            t < 0.66f -> lerp(c3, c1, (t - 0.33f) / 0.33f)
            else -> lerp(c1, c2, (t - 0.66f) / 0.34f)
        }
        
        bodyEndOnAnim = when {
            t < 0.33f -> lerp(c3, c1, t / 0.33f)
            t < 0.66f -> lerp(c1, c2, (t - 0.33f) / 0.33f)
            else -> lerp(c2, c3, (t - 0.66f) / 0.34f)
        }
    } else {
        bodyStartOnAnim = bodyStartOn
        bodyCenterOnAnim = bodyCenterOn
        bodyEndOnAnim = bodyEndOn
    }

    val bodyStart = lerp(bodyStartOff, bodyStartOnAnim, progress)
    val bodyCenter = lerp(bodyCenterOff, bodyCenterOnAnim, progress)
    val bodyEnd = lerp(bodyEndOff, bodyEndOnAnim, progress)

    val bodyLeft = 59.dp.toPx(scope)
    val bodyTop = 58.dp.toPx(scope)
    val bodyRight = width - 59.dp.toPx(scope)
    val bodyBottom = height - 66.dp.toPx(scope)

    val bodyCenterX = (bodyLeft + bodyRight) / 2f
    val bodyCenterY = (bodyTop + bodyBottom) / 2f
    val bodyHalfW = (bodyRight - bodyLeft) / 2f
    val bodyHalfH = (bodyBottom - bodyTop) / 2f

    val bodyAngle = baseAngle + when {
        connectedProgress > 0f -> shimmerPhase * connectedProgress
        connectingProgress > 0f -> connectingRotation1 * Math.PI.toFloat() / 180f
        else -> 0f
    }
    val bodyDx = bodyHalfW * cos(bodyAngle)
    val bodyDy = bodyHalfH * sin(bodyAngle)
    
    drawOvalWithOffsets(
        scope,
        Brush.linearGradient(
            colors = listOf(bodyEnd, bodyCenter, bodyStart),
            start = Offset(bodyCenterX + bodyDx, bodyCenterY + bodyDy),
            end = Offset(bodyCenterX - bodyDx, bodyCenterY - bodyDy)
        ),
        59f, 58f, 59f, 66f
    )

    val highlightColor = lerp(Color(0x44FFFFFF), Color(0xBBFFFFFF), progress)
    
    val bodyRectLeft = 59.dp.toPx(scope)
    val bodyRectTop = 58.dp.toPx(scope)
    val bodyRectWidth = width - 59.dp.toPx(scope) * 2
    val bodyRectHeight = height - (58 + 66).dp.toPx(scope)
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(highlightColor, Color.Transparent),
            center = Offset(bodyRectLeft + bodyRectWidth * 0.3f, bodyRectTop + bodyRectHeight * 0.2f),
            radius = 120.dp.toPx(scope)
        ),
        center = Offset(width / 2, height / 2 - 4.dp.toPx(scope)), 
        radius = (bodyRectWidth / 2).coerceAtLeast(0f)
    )

    val innerHiColorStart = Color(0x55000000)
    val innerHiColorCenter = Color(0x11000000)
    val innerHiColorEnd = lerp(Color(0x22FFFFFF), Color(0x33FFFFFF), progress)
    
    drawOvalWithOffsets(
        scope,
        Brush.linearGradient(
            colors = listOf(innerHiColorStart, innerHiColorCenter, innerHiColorEnd),
            start = Offset(88.dp.toPx(scope), 84.dp.toPx(scope)),
            end = Offset((width - 88.dp.toPx(scope)), (height - 92.dp.toPx(scope)))
        ),
        88f, 84f, 88f, 92f
    )

    val innerShadowColor = Color(0x44000000)
    
    drawOvalWithOffsets(
        scope,
        Brush.verticalGradient(listOf(innerShadowColor, Color.Transparent)),
        102f, 98f, 102f, 106f
    )

    val innerGlowAlpha = if (connectedProgress > 0f) {
        (connectedProgress - 0.5f).coerceAtLeast(0f) * 2f
    } else {
        connectingProgress * 0.7f
    }
    
    if (innerGlowAlpha > 0f) {
        val activePhase = if (connectedProgress > 0f) shimmerPhase * 3f else (connectingRotation1 * Math.PI.toFloat() / 180f * 2f)
        val cyanAlpha = innerGlowAlpha * (0.7f + 0.3f * sin(activePhase))
        val innerGlowRadius = 48.dp.toPx(scope) * (1f + 0.05f * sin(activePhase))
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x8800FFFF), Color.Transparent),
                radius = 50.dp.toPx(scope) * (1f + 0.05f * sin(activePhase))
            ),
            center = Offset(width / 2, height / 2 - 4.dp.toPx(scope)), 
            radius = innerGlowRadius,
            alpha = cyanAlpha
        )
    }
}

private fun drawOvalWithOffsets(
    scope: DrawScope,
    brush: Brush,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    val width = scope.size.width
    val height = scope.size.height
    val l = left.dp.toPx(scope)
    val t = top.dp.toPx(scope)
    val r = width - right.dp.toPx(scope)
    val b = height - bottom.dp.toPx(scope)
    
    scope.drawOval(
        brush = brush,
        topLeft = Offset(l, t),
        size = androidx.compose.ui.geometry.Size(r - l, b - t)
    )
}

private fun drawOvalWithOffsets(
    scope: DrawScope,
    color: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    val width = scope.size.width
    val height = scope.size.height
    val l = left.dp.toPx(scope)
    val t = top.dp.toPx(scope)
    val r = width - right.dp.toPx(scope)
    val b = height - bottom.dp.toPx(scope)
    
    scope.drawOval(
        color = color,
        topLeft = Offset(l, t),
        size = androidx.compose.ui.geometry.Size(r - l, b - t)
    )
}




@Composable
fun FlareClipboardButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    onManualInputClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onImportFileClick: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf<Offset?>(null) }

    val isDark = FlareTheme.colors.isDark
    val borderAlphaStart = if (isDark) 0.35f else 0.45f
    val borderAlphaEnd = if (isDark) 0.05f else 0.08f

    Box {
        Box(
            modifier = modifier
                .height(48.dp)
                .widthIn(min = 120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5B8CFF),
                            Color(0xFFA066FF).copy(alpha = 0.85f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset.Infinite
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
                    shape = RoundedCornerShape(24.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { offset ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            touchOffset = offset
                            menuExpanded = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
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

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = I18n.strings.btn_clipboard,
                    fontFamily = GeologicaMedium,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
            }
        }
        
        FlareGlassMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            items = listOf(
                flare.client.app.util.GlassUtils.MenuItem(1, I18n.strings.menu_manual_input) {
                    menuExpanded = false
                    onManualInputClick()
                },
                flare.client.app.util.GlassUtils.MenuItem(2, I18n.strings.menu_qr_code) {
                    menuExpanded = false
                    onQrScanClick()
                },
                flare.client.app.util.GlassUtils.MenuItem(3, I18n.strings.menu_file) {
                    menuExpanded = false
                    onImportFileClick()
                }
            ),
            hazeState = hazeState,
            touchOffset = touchOffset
        )
    }
}


private fun androidx.compose.ui.unit.Dp.toPx(scope: DrawScope): Float = with(scope) { this@toPx.toPx() }
