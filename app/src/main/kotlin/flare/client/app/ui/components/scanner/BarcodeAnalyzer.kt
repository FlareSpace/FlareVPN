package flare.client.app.ui.components.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

data class QrDetectResult(
    val value: String,
    val boundingBox: android.graphics.Rect?,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int
)

class BarcodeAnalyzer(
    private val onBarcodeDetected: (QrDetectResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onBarcodeDetected(
                                QrDetectResult(
                                    value = value,
                                    boundingBox = barcode.boundingBox,
                                    imageWidth = imageProxy.width,
                                    imageHeight = imageProxy.height,
                                    rotationDegrees = rotationDegrees
                                )
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

