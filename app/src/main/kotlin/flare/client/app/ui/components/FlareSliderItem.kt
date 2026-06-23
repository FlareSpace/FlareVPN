package flare.client.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.ui.theme.FlareTheme

@Composable
fun FlareSliderItem(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isTop: Boolean = false,
    isBottom: Boolean = false,
    isMiddle: Boolean = false
) {
    val colors = FlareTheme.colors
    val backgroundColor = colors.bgItem.copy(alpha = 0.85f)
    val inactiveTrackColor = colors.textSecondary.copy(alpha = 0.2f)

    val density = LocalDensity.current
    val baseThumbWidthPx = with(density) { 35.dp.toPx() }
    val baseThumbHeightPx = with(density) { 22.dp.toPx() }
    val trackHeightPx = with(density) { 12.dp.toPx() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val currentFraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "sliderGlow"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumbScale"
    )

    fun updateValueFromOffset(xOffset: Float) {
        val width = canvasSize.width.toFloat()
        val thumbWidthPx = baseThumbWidthPx * thumbScale
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
        modifier = modifier
            .fillMaxWidth()
            .background(
                backgroundColor,
                shape = when {
                    isTop && isBottom -> RoundedCornerShape(20.dp)
                    isTop -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    isBottom -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    isMiddle -> androidx.compose.ui.graphics.RectangleShape
                    else -> RoundedCornerShape(20.dp)
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = GeologicaRegular,
                fontSize = 16.sp,
                color = colors.textPrimary
            )
            Text(
                text = valueText,
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

            val thumbWidthPx = baseThumbWidthPx * thumbScale
            val thumbHeightPx = baseThumbHeightPx * thumbScale

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
