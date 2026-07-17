package flare.client.app.ui.components.scanner

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min

@Composable
fun QrSuccessOverlay(
    isVisible: Boolean,
    qrResult: QrDetectResult?,
    accentColor: Color,
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

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (frameAlpha <= 0f) return@Canvas

        val left: Float
        val top: Float
        val right: Float
        val bottom: Float

        val boundingBox = qrResult?.boundingBox
        if (boundingBox != null) {
            val rotation = qrResult.rotationDegrees
            val imgW = qrResult.imageWidth
            val imgH = qrResult.imageHeight

            val rotatedW = if (rotation == 90 || rotation == 270) imgH.toFloat() else imgW.toFloat()
            val rotatedH = if (rotation == 90 || rotation == 270) imgW.toFloat() else imgH.toFloat()

            val viewW = size.width
            val viewH = size.height

            val scaleX = viewW / rotatedW
            val scaleY = viewH / rotatedH
            val scale = maxOf(scaleX, scaleY)

            val offsetX = (viewW - rotatedW * scale) / 2f
            val offsetY = (viewH - rotatedH * scale) / 2f

            left = boundingBox.left * scale + offsetX
            top = boundingBox.top * scale + offsetY
            right = boundingBox.right * scale + offsetX
            bottom = boundingBox.bottom * scale + offsetY
        } else {
            val frameSize = min(size.width, size.height) * 0.62f
            left = (size.width - frameSize) / 2f
            top = (size.height - frameSize) / 2f
            right = left + frameSize
            bottom = top + frameSize
        }

        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f

        withTransform({
            scale(frameScale, frameScale, Offset(centerX, centerY))
        }) {
            drawQrFrame(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                alpha = frameAlpha,
                color = accentColor,
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
    val padding = 12f * density
    val l = left - padding
    val t = top - padding
    val r = right + padding
    val b = bottom + padding

    val sizeMin = min(r - l, b - t)
    if (sizeMin <= 0) return

    val cornerRadius = (sizeMin * 0.12f).coerceIn(16f * density, 32f * density)
    val lineLength = (sizeMin * 0.15f).coerceIn(16f * density, 48f * density)

    val strokeWidth = 6f * density
    val glowWidth = 16f * density

    
    drawCornerBrackets(l, t, r, b, cornerRadius, lineLength, color.copy(alpha = 0.3f * alpha), glowWidth)

    
    drawCornerBrackets(l, t, r, b, cornerRadius, lineLength, color.copy(alpha = alpha), strokeWidth)
}

private fun DrawScope.drawCornerBrackets(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    r: Float,
    lineLength: Float,
    color: Color,
    strokeWidth: Float
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    
    val pathTl = Path().apply {
        moveTo(left + r + lineLength, top)
        lineTo(left + r, top)
        arcTo(
            rect = Rect(left, top, left + 2 * r, top + 2 * r),
            startAngleDegrees = 270f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false
        )
        lineTo(left, top + r + lineLength)
    }
    drawPath(pathTl, color, style = stroke)

    
    val pathTr = Path().apply {
        moveTo(right - r - lineLength, top)
        lineTo(right - r, top)
        arcTo(
            rect = Rect(right - 2 * r, top, right, top + 2 * r),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        lineTo(right, top + r + lineLength)
    }
    drawPath(pathTr, color, style = stroke)

    
    val pathBl = Path().apply {
        moveTo(left + r + lineLength, bottom)
        lineTo(left + r, bottom)
        arcTo(
            rect = Rect(left, bottom - 2 * r, left + 2 * r, bottom),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        lineTo(left, bottom - r - lineLength)
    }
    drawPath(pathBl, color, style = stroke)

    
    val pathBr = Path().apply {
        moveTo(right - r - lineLength, bottom)
        lineTo(right - r, bottom)
        arcTo(
            rect = Rect(right - 2 * r, bottom - 2 * r, right, bottom),
            startAngleDegrees = 90f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false
        )
        lineTo(right, bottom - r - lineLength)
    }
    drawPath(pathBr, color, style = stroke)
}

private class DecelerateInterpolator : androidx.compose.animation.core.Easing {
    override fun transform(fraction: Float): Float {
        return 1.0f - (1.0f - fraction) * (1.0f - fraction)
    }
}

