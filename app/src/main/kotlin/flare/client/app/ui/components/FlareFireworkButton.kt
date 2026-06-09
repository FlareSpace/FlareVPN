package flare.client.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import flare.client.app.ui.MainViewModel
import flare.client.app.ui.theme.FlareTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class RayProperty(
    val baseAngle: Float,
    val seed1: Float,
    val seed2: Float,
    val seed3: Float,
    val type: Int,
    val baseLengthFactor: Float,
    val targetThicknessDp: Float,
    val activeColor: Color,
    val hasSpark: Boolean,
    val offAlpha: Float
)

@Composable
fun FlareFireworkButton(
    connectionState: MainViewModel.ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 300.dp,
    backgroundType: Int = 1
) {
    var scale by remember { mutableStateOf(1f) }

    
    val totalActiveProgress by animateFloatAsState(
        targetValue = if (connectionState == MainViewModel.ConnectionState.CONNECTING ||
                          connectionState == MainViewModel.ConnectionState.CONNECTED) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "totalActiveProgress"
    )

    
    val connectedProgress by animateFloatAsState(
        targetValue = if (connectionState == MainViewModel.ConnectionState.CONNECTED) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "connectedProgress"
    )

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    var time by remember { mutableFloatStateOf(0f) }
    
    
    
    val isAnimatingState = remember {
        derivedStateOf {
            totalActiveProgress > 0f || connectedProgress > 0f
        }
    }

    LaunchedEffect(isAnimatingState.value) {
        if (isAnimatingState.value) {
            var lastTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { frameTime ->
                    val delta = (frameTime - lastTime) / 1_000_000_000f
                    lastTime = frameTime
                    time += delta
                }
            }
        }
    }

    val isDark = FlareTheme.colors.isDark
    val useDarkShadow = isDark && backgroundType == 0

    
    val offColor = if (isDark) Color(0xFF4A4A4A) else Color(0xFF8E9EB2)
    val offDotColor = if (isDark) Color(0xFF5A5A5A) else Color(0xFFA8B7CE)

    
    val totalRays = 42

    val rayProperties = remember {
        List(totalRays) { i ->
            val baseAngle = (i.toFloat() / totalRays) * 2f * PI.toFloat()
            val seed1 = ((i * 97 + 31) % 100) / 100f
            val seed2 = ((i * 43 + 17) % 100) / 100f
            val seed3 = ((i * 71 + 53) % 100) / 100f
            
            val type = when {
                i % 6 == 0 -> 0 
                i % 3 == 0 -> 1 
                else -> 2       
            }
            
            val baseLengthFactor = when (type) {
                0 -> 0.75f + seed1 * 0.15f
                1 -> 0.45f + seed1 * 0.15f
                else -> 0.2f + seed1 * 0.15f
            }
            
            val targetThicknessDp = when (type) {
                0 -> 3.5f + seed2 * 1.0f
                1 -> 2.0f + seed2 * 1.0f
                else -> 1.2f + seed2 * 0.8f
            }
            
            val activeColor = when (type) {
                0 -> if (seed3 > 0.4f) Color.White else Color(0xFFFF1493) 
                1 -> if (seed3 > 0.6f) Color(0xFFE040FB) else if (seed3 > 0.3f) Color(0xFFFF007F) else Color.White
                else -> if (seed3 > 0.5f) Color(0xFFFF69B4) else Color(0xFFD500F9)
            }
            
            val hasSpark = type == 0 || (type == 1 && seed1 > 0.4f)
            val offAlpha = 0.4f + seed2 * 0.3f

            RayProperty(
                baseAngle = baseAngle,
                seed1 = seed1,
                seed2 = seed2,
                seed3 = seed3,
                type = type,
                baseLengthFactor = baseLengthFactor,
                targetThicknessDp = targetThicknessDp,
                activeColor = activeColor,
                hasSpark = hasSpark,
                offAlpha = offAlpha
            )
        }
    }

    
    val rayColorLists = remember {
        List(totalRays) {
            java.util.ArrayList<Color>(3).apply {
                add(Color.Transparent)
                add(Color.Transparent)
                add(Color.Transparent)
            }
        }
    }

    val sparkColorLists = remember {
        List(totalRays) {
            java.util.ArrayList<Color>(2).apply {
                add(Color.Transparent)
                add(Color.Transparent)
            }
        }
    }

    
    val vignetteBrush = remember(useDarkShadow) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val maxRadius = size.width / 2.3f
                val center = Offset(size.width / 2, size.height / 2)
                return RadialGradientShader(
                    colors = listOf(
                        Color(0xFF1A0033).copy(alpha = 0.85f),
                        Color(0xFF0D001A).copy(alpha = 0.95f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.05f
                )
            }
        }
    }

    val ambientGlowBrush = remember(useDarkShadow) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val maxRadius = size.width / 2.3f
                val center = Offset(size.width / 2, size.height / 2)
                return RadialGradientShader(
                    colors = listOf(
                        Color(0xFFFF007F).copy(alpha = 0.4f),
                        Color(0xFF7B1FA2).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * (if (useDarkShadow) 0.85f else 0.75f)
                )
            }
        }
    }

    val hotPinkCoreBrush = remember {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val maxRadius = size.width / 2.3f
                val center = Offset(size.width / 2, size.height / 2)
                return RadialGradientShader(
                    colors = listOf(
                        Color(0xFFFF1493).copy(alpha = 0.9f),
                        Color(0xFFD500F9).copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.5f
                )
            }
        }
    }

    val blindingWhiteBrush = remember {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val maxRadius = size.width / 2.3f
                val center = Offset(size.width / 2, size.height / 2)
                return RadialGradientShader(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.8f),
                        Color(0xFFFFB6C1).copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.25f
                )
            }
        }
    }

    Box(
        modifier = modifier
            .requiredSize(buttonSize)
            .graphicsLayer {
                this.scaleX = animatedScale
                this.scaleY = animatedScale
            },
        contentAlignment = Alignment.Center
    ) {
        
        Box(
            modifier = Modifier
                .requiredSize(buttonSize * 0.5f)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            scale = 0.92f
                            tryAwaitRelease()
                            scale = 1f
                            onClick()
                        }
                    )
                }
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dpToPx = 1.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2.3f

            
            if (useDarkShadow && totalActiveProgress > 0f) {
                val vignetteRadius = maxRadius * 1.05f
                drawCircle(
                    brush = vignetteBrush,
                    radius = vignetteRadius,
                    center = center,
                    alpha = totalActiveProgress
                )
            }

            
            if (totalActiveProgress > 0f) {
                
                val ambientGlowRadius = maxRadius * (if (useDarkShadow) 0.85f else 0.75f)
                drawCircle(
                    brush = ambientGlowBrush,
                    radius = ambientGlowRadius,
                    center = center,
                    alpha = totalActiveProgress
                )
                
                
                drawCircle(
                    brush = hotPinkCoreBrush,
                    radius = maxRadius * 0.5f,
                    center = center,
                    alpha = totalActiveProgress
                )
                
                
                drawCircle(
                    brush = blindingWhiteBrush,
                    radius = maxRadius * 0.25f,
                    center = center,
                    alpha = totalActiveProgress
                )
            }
            
            
            if (totalActiveProgress < 1f) {
                val iconAlpha = 1f - totalActiveProgress
                val iconColor = offColor.copy(alpha = iconAlpha)
                
                val iconRadius = maxRadius * 0.22f
                val strokeWidth = 3f * dpToPx
                
                
                drawArc(
                    color = iconColor,
                    startAngle = -60f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(center.x - iconRadius, center.y - iconRadius),
                    size = Size(iconRadius * 2, iconRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                
                drawLine(
                    color = iconColor,
                    start = Offset(center.x, center.y - iconRadius * 0.2f),
                    end = Offset(center.x, center.y - iconRadius * 1.2f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            
            for (i in 0 until totalRays) {
                val prop = rayProperties[i]

                val pulsePhase = sin(time * (2.5f + prop.seed1) + i.toFloat() * 0.5f)
                val targetLength = maxRadius * prop.baseLengthFactor * (0.95f + 0.05f * pulsePhase)
                
                
                val angleOffset = (prop.seed1 - 0.5f) * 0.12f 
                val rotation = time * 0.025f

                
                val ringRadius = maxRadius * 0.45f
                val staticInner = ringRadius
                val staticLength = ringRadius + (2.5f * dpToPx) 

                val currentAngle = lerp(prop.baseAngle, prop.baseAngle + angleOffset, connectedProgress) + rotation
                val currentLength = lerp(staticLength, targetLength, connectedProgress)
                
                
                val targetInner = maxRadius * (0.02f + 0.05f * prop.seed2)
                val currentInner = lerp(staticInner, targetInner, connectedProgress)
                
                val startX = center.x + cos(currentAngle) * currentInner
                val startY = center.y + sin(currentAngle) * currentInner
                val endX = center.x + cos(currentAngle) * currentLength
                val endY = center.y + sin(currentAngle) * currentLength

                val rayColor = lerp(offColor, prop.activeColor, totalActiveProgress)
                
                val onAlpha = 0.85f + 0.15f * sin(time * 4f + i * 2f)
                val currentAlpha = lerp(prop.offAlpha, onAlpha, totalActiveProgress)
                
                val thickness = lerp(1.5f * dpToPx, prop.targetThicknessDp * dpToPx, connectedProgress)

                
                if ((prop.activeColor == Color.White || prop.activeColor == Color(0xFFFF1493)) && totalActiveProgress > 0f) {
                    val haloColor = if (prop.activeColor == Color.White) Color(0xFFFF007F) else Color(0xFF9C27B0)
                    drawLine(
                        color = haloColor.copy(alpha = currentAlpha * 0.5f * totalActiveProgress),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = thickness * 2.2f,
                        cap = StrokeCap.Round
                    )
                }

                
                val colorList = rayColorLists[i]
                colorList[0] = rayColor.copy(alpha = currentAlpha)
                colorList[1] = rayColor.copy(alpha = currentAlpha * 0.9f)
                colorList[2] = rayColor.copy(alpha = currentAlpha * 0.05f) 

                val lineBrush = Brush.linearGradient(
                    colors = colorList,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                )

                drawLine(
                    brush = lineBrush,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round
                )
                
                
                if (prop.hasSpark && connectedProgress > 0.1f) {
                    
                    val gap = (3f + prop.seed3 * 4f) * dpToPx * connectedProgress
                    val sparkDist = currentLength + gap
                    
                    val sparkX = center.x + cos(currentAngle) * sparkDist
                    val sparkY = center.y + sin(currentAngle) * sparkDist
                    
                    val sparkRadius = thickness * 0.7f * lerp(0.5f, 1f, totalActiveProgress)
                    val sparkAlpha = currentAlpha * (0.7f + 0.3f * sin(time * 5f + i)) * connectedProgress
                    
                    if (sparkAlpha > 0f) {
                        val sparkColor = lerp(offDotColor, prop.activeColor, totalActiveProgress)
                        
                        
                        if (totalActiveProgress > 0f) {
                            val glowColor = if (prop.activeColor == Color.White) Color(0xFFFF007F) else prop.activeColor
                            val sparkColorList = sparkColorLists[i]
                            sparkColorList[0] = glowColor.copy(alpha = sparkAlpha * 0.65f)
                            sparkColorList[1] = Color.Transparent

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = sparkColorList,
                                    center = Offset(sparkX, sparkY),
                                    radius = sparkRadius * 3.5f
                                ),
                                radius = sparkRadius * 3.5f,
                                center = Offset(sparkX, sparkY)
                            )
                        }
                        
                        
                        drawCircle(
                            color = sparkColor.copy(alpha = sparkAlpha),
                            radius = sparkRadius,
                            center = Offset(sparkX, sparkY)
                        )
                    }
                }
            }
        }
    }
}

