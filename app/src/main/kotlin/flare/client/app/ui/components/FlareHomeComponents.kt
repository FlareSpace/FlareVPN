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
    Offset(0.1f, 0.1f),
    Offset(0.9f, 0.8f),
    Offset(0.8f, 0.1f),
    Offset(0.2f, 0.9f),
    Offset(0.5f, 0.5f),
    Offset(0.1f, 0.7f),
    Offset(0.8f, 0.4f)
)

private val meshRadiuses = listOf(900f, 950f, 850f, 900f, 1000f, 800f, 850f)

@Composable
fun FlareHomeBackground(
    backgroundType: Int = 1,
    isAnimationEnabled: Boolean = true,
    animationSpeed: Float = 1.0f,
    photoSeed: String = "default_seed",
    modifier: Modifier = Modifier
) {
    val themeColors = FlareTheme.colors
    val isDark = themeColors.isDark

    when (backgroundType) {
        0 -> {
            Box(modifier = modifier.fillMaxSize().background(themeColors.bgDark))
            return
        }
        2 -> {
            flare.client.app.ui.components.background.AuroraBackground(
                modifier = modifier
            )
            return
        }
        3 -> {
            flare.client.app.ui.components.background.PhotoBackground(
                modifier = modifier,
                isDark = isDark,
                photoSeed = photoSeed
            )
            return
        }
    }

    var time by remember { mutableStateOf(0f) }

    if (isAnimationEnabled) {
        LaunchedEffect(animationSpeed) {
            var lastTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { frameTime ->
                    val deltaSeconds = (frameTime - lastTime) / 1_000_000_000f
                    lastTime = frameTime
                    time += deltaSeconds * animationSpeed * 0.35f 
                }
            }
        }
    }

    val density = LocalDensity.current
    
    val extraColor1Start = if (isDark) Color(0x0CFF3D00) else Color(0x15FF3D00) 
    val extraColor1End = Color(0x00FF3D00)
    val extraColor2Start = if (isDark) Color(0x0C7C4DFF) else Color(0x157C4DFF) 
    val extraColor2End = Color(0x007C4DFF)

    val brushes = remember(themeColors, density, isDark) {
        val darkAlphaMult = if (isDark) 0.5f else 1.0f 
        
        val colorsList = listOf(
            themeColors.gradientBlueStart.let { it.copy(alpha = it.alpha * darkAlphaMult) } to themeColors.gradientBlueEnd,
            themeColors.gradientPurpleStart.let { it.copy(alpha = it.alpha * darkAlphaMult * 0.9f) } to themeColors.gradientPurpleEnd,
            themeColors.gradientMagentaStart.let { it.copy(alpha = it.alpha * darkAlphaMult) } to themeColors.gradientMagentaEnd,
            themeColors.gradientCyanStart.let { it.copy(alpha = it.alpha * darkAlphaMult * 1.5f) } to themeColors.gradientCyanEnd, 
            extraColor1Start to extraColor1End,
            extraColor2Start to extraColor2End,
            themeColors.gradientWhiteStart.let { it.copy(alpha = it.alpha * (if (isDark) 0.08f else 0.5f)) } to themeColors.gradientWhiteEnd
        )
        colorsList.mapIndexed { i, (start, end) ->
            val radiusPx = meshRadiuses[i] * density.density
            Brush.radialGradient(
                0.0f to start,
                0.4f to start.copy(alpha = start.alpha * 0.7f),
                0.8f to start.copy(alpha = start.alpha * 0.2f),
                1.0f to end,
                center = Offset.Zero,
                radius = radiusPx
            )
        }
    }

    val noiseBitmap = remember {
        createNoiseBitmap(opacity = if (isDark) 0.035f else 0.025f).asImageBitmap()
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
                    val phase = i * 2.1f
                    val speedX = 0.5f + (i * 0.08f)
                    val speedY = 0.4f + (i * 0.09f)

                    val timeScale = time * 0.8f

                    val offsetX = if (isAnimationEnabled) {
                        (sin(timeScale * speedX + phase) * 0.45f + cos(timeScale * 0.4f * speedX) * 0.2f)
                    } else 0f
                    
                    val offsetY = if (isAnimationEnabled) {
                        (cos(timeScale * speedY + phase) * 0.45f + sin(timeScale * 0.3f * speedY) * 0.2f)
                    } else 0f

                    val base = meshBasePositions[i]
                    val center = Offset(
                        (base.x + offsetX) * width,
                        (base.y + offsetY) * height
                    )

                    val radiusPx = meshRadiuses[i] * density.density

                    val scaleX = if (isAnimationEnabled) 1.2f + sin(timeScale * 0.5f + phase) * 0.4f else 1.2f
                    val scaleY = if (isAnimationEnabled) 1.2f + cos(timeScale * 0.6f + phase) * 0.4f else 1.2f
                    val rot = if (isAnimationEnabled) (timeScale * 12f * speedX + phase * 50f) % 360f else (phase * 50f) % 360f

                    withTransform({
                        translate(center.x, center.y)
                        rotate(rot)
                        scale(scaleX, scaleY)
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
fun FlareClipboardButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    onManualInputClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onImportFileClick: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White,
    isCustomColorEnabled: Boolean = false
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
                    if (isCustomColorEnabled) {
                        Brush.linearGradient(
                            colors = listOf(
                                FlareTheme.colors.accent,
                                FlareTheme.colors.accentEnd.copy(alpha = 0.85f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset.Infinite
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF5B8CFF),
                                Color(0xFFA066FF).copy(alpha = 0.85f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset.Infinite
                        )
                    }
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
