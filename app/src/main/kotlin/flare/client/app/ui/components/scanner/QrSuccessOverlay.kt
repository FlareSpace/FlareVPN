package flare.client.app.ui.components.scanner

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun QrSuccessOverlay(
    isVisible: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (!isVisible) return

    val density = LocalDensity.current
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = DecelerateInterpolator())
            )
            onAnimationEnd()
        }
    }

    val progress = animatable.value
    val frameAlpha = if (progress < 0.25f) progress / 0.25f else 1f - ((progress - 0.25f) / 0.75f) * 0.08f
    val frameScale = 0.92f + 0.08f * progress
    val successColor = Color(0xFF30D158)

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (frameAlpha <= 0f) return@Canvas

        val frameSize = min(size.width, size.height) * 0.62f
        val left = (size.width - frameSize) / 2f
        val top = (size.height - frameSize) / 2f

        withTransform({
            scale(frameScale, frameScale, Offset(size.width / 2f, size.height / 2f))
        }) {
            drawQrFrame(
                left = left,
                top = top,
                right = left + frameSize,
                bottom = top + frameSize,
                alpha = frameAlpha,
                color = successColor,
                density = density.density
            )
        }
    }
}

private fun DrawScope.drawQrFrame(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    alpha: Float,
    color: Color,
    density: Float
) {
    val strokeWidth = 8f * density
    val glowWidth = 18f * density
    
    val spanH = right - left
    val spanV = bottom - top
    if (spanH <= 0 || spanV <= 0) return

    val corner = (min(spanH, spanV) * 0.16f).coerceIn(32f * density, 72f * density)
    val minGap = 22f * density
    val maxGap = 52f * density

    
    drawFrameParts(left, top, right, bottom, corner, minGap, maxGap, color.copy(alpha = 0.35f * alpha), glowWidth)
    
    drawFrameParts(left, top, right, bottom, corner, minGap, maxGap, color.copy(alpha = alpha), strokeWidth)
}

private fun DrawScope.drawFrameParts(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    corner: Float,
    minGap: Float,
    maxGap: Float,
    color: Color,
    strokeWidth: Float
) {
    val sw = strokeWidth / 2f
    val l = left + sw
    val t = top + sw
    val r = right - sw
    val b = bottom - sw

    
    
    drawLine(color, Offset(l, t), Offset(l + corner, t), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, Offset(l, t), Offset(l, t + corner), strokeWidth, cap = StrokeCap.Round)
    
    drawLine(color, Offset(r - corner, t), Offset(r, t), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, Offset(r, t), Offset(r, t + corner), strokeWidth, cap = StrokeCap.Round)
    
    drawLine(color, Offset(l, b - corner), Offset(l, b), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, Offset(l, b), Offset(l + corner, b), strokeWidth, cap = StrokeCap.Round)
    
    drawLine(color, Offset(r, b - corner), Offset(r, b), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, Offset(r - corner, b), Offset(r, b), strokeWidth, cap = StrokeCap.Round)

    
    drawHorizontalOpen(l + corner, r - corner, t, minGap, maxGap, color, strokeWidth)
    drawHorizontalOpen(l + corner, r - corner, b, minGap, maxGap, color, strokeWidth)
    
    drawVerticalOpen(l, t + corner, b - corner, minGap, maxGap, color, strokeWidth)
    drawVerticalOpen(r, t + corner, b - corner, minGap, maxGap, color, strokeWidth)
}

private fun DrawScope.drawHorizontalOpen(
    xStart: Float, xEnd: Float, y: Float,
    minGap: Float, maxGap: Float,
    color: Color, strokeWidth: Float
) {
    val len = xEnd - xStart
    if (len <= 2f) return
    val gap = (len * 0.26f).coerceIn(minGap, maxGap).coerceAtMost(len * 0.42f)
    val mid = (xStart + xEnd) / 2f
    drawLine(color, Offset(xStart, y), Offset(mid - gap / 2f, y), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, Offset(mid + gap / 2f, y), Offset(xEnd, y), strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawVerticalOpen(
    x: Float, yStart: Float, yEnd: Float,
    minGap: Float, maxGap: Float,
    color: Color, strokeWidth: Float
) {
    val len = yEnd - yStart
    if (len <= 2f) return
    val gap = (len * 0.26f).coerceIn(minGap, maxGap).coerceAtMost(len * 0.42f)
    val mid = (yStart + yEnd) / 2f
    drawLine(color, Offset(x, yStart), Offset(x, mid - gap / 2f), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, Offset(x, mid + gap / 2f), Offset(x, yEnd), strokeWidth, cap = StrokeCap.Round)
}

private class DecelerateInterpolator : androidx.compose.animation.core.Easing {
    override fun transform(fraction: Float): Float {
        return 1.0f - (1.0f - fraction) * (1.0f - fraction)
    }
}
