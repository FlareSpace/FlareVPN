package flare.client.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrUtils {
    fun generateQrCodeBitmap(content: String, sizePx: Int = 900): Bitmap? {
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 0)
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val width = matrix.width
            val height = matrix.height

            
            var minX = width
            var maxX = -1
            var minY = height
            var maxY = -1
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (matrix.get(x, y)) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            
            val activeLeft = if (maxX >= minX) minX else 0
            val activeTop = if (maxY >= minY) minY else 0
            val qrWidth = if (maxX >= minX) (maxX - minX + 1) else width
            val qrHeight = if (maxY >= minY) (maxY - minY + 1) else height
            val qrSize = maxOf(qrWidth, qrHeight)

            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            
            val bgPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            val bgRadius = sizePx * 0.08f
            canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), bgRadius, bgRadius, bgPaint)

            
            val padding = sizePx * 0.08f
            val availableSize = sizePx - 2 * padding
            val moduleSize = availableSize / qrSize

            val contentLeft = padding
            val contentTop = padding

            val darkPaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            val whitePaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            
            fun isFinderPattern(col: Int, row: Int): Boolean {
                if (col in 0..6 && row in 0..6) return true
                if (col in (qrSize - 7) until qrSize && row in 0..6) return true
                if (col in 0..6 && row in (qrSize - 7) until qrSize) return true
                return false
            }

            
            fun drawFinderPattern(left: Float, top: Float) {
                
                val outerRect = RectF(left, top, left + 7 * moduleSize, top + 7 * moduleSize)
                val outerRadius = 1.75f * moduleSize
                canvas.drawRoundRect(outerRect, outerRadius, outerRadius, darkPaint)

                
                val innerRect = RectF(
                    left + moduleSize,
                    top + moduleSize,
                    left + 6 * moduleSize,
                    top + 6 * moduleSize
                )
                val innerRadius = 0.75f * moduleSize
                canvas.drawRoundRect(innerRect, innerRadius, innerRadius, whitePaint)

                
                val centerRect = RectF(
                    left + 2 * moduleSize,
                    top + 2 * moduleSize,
                    left + 5 * moduleSize,
                    top + 5 * moduleSize
                )
                val centerRadius = 0.5f * moduleSize
                canvas.drawRoundRect(centerRect, centerRadius, centerRadius, darkPaint)
            }

            
            for (col in 0 until qrSize) {
                for (row in 0 until qrSize) {
                    val matrixX = activeLeft + col
                    val matrixY = activeTop + row
                    if (matrixX < width && matrixY < height) {
                        if (!isFinderPattern(col, row) && matrix.get(matrixX, matrixY)) {
                            val left = contentLeft + col * moduleSize
                            val top = contentTop + row * moduleSize
                            val rect = RectF(left, top, left + moduleSize, top + moduleSize)
                            val radius = moduleSize * 0.35f
                            canvas.drawRoundRect(rect, radius, radius, darkPaint)
                        }
                    }
                }
            }

            
            drawFinderPattern(contentLeft, contentTop)
            drawFinderPattern(contentLeft + (qrSize - 7) * moduleSize, contentTop)
            drawFinderPattern(contentLeft, contentTop + (qrSize - 7) * moduleSize)

            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
