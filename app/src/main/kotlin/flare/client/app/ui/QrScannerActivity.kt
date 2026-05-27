package flare.client.app.ui

import flare.client.app.ui.i18n.I18n

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import flare.client.app.data.SettingsManager
import flare.client.app.ui.components.scanner.QrScannerScreen
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_DEFAULT
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_GREEN
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_PURPLE
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_RED
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_PINK
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_ORANGE
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_INDIGO
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_CYAN
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_AMBER
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_VIOLET
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_TEAL

class QrScannerActivity : ComponentActivity() {

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setupContent()
            } else {
                Toast.makeText(this, I18n.strings.error_camera_permission_denied, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            decodeQrFromImage(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (granted) {
            setupContent()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupContent() {
        setContent {
            FlareTheme {
                QrScannerScreen(
                    onBarcodeDetected = { deliverResult(it) },
                    onGalleryClicked = { galleryLauncher.launch("image/*") },
                    accentColor = Color(resolveAccentColor())
                )
            }
        }
    }

    private fun deliverResult(content: String) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_QR_CONTENT, content)
        )
        finish()
    }

    private fun decodeQrFromImage(uri: android.net.Uri) {
        runCatching {
            val image = InputImage.fromFilePath(this, uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.firstOrNull()?.rawValue
                    if (!value.isNullOrBlank()) {
                        deliverResult(value)
                    } else {
                        Toast.makeText(this, I18n.strings.error_qr_not_found_in_image, Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, I18n.strings.error_qr_not_found_in_image, Toast.LENGTH_SHORT).show()
                }
        }.onFailure {
            Toast.makeText(this, I18n.strings.error_qr_not_found_in_image, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveAccentColor(): Int {
        val settings = SettingsManager(this)
        return when (settings.accentColorKey) {
            "material_you" -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    resources.getColor(android.R.color.system_accent1_500, theme)
                } else {
                    COLOR_DEFAULT
                }
            }
            "green" -> COLOR_GREEN
            "purple" -> COLOR_PURPLE
            "red" -> COLOR_RED
            "pink" -> COLOR_PINK
            "orange" -> COLOR_ORANGE
            "indigo" -> COLOR_INDIGO
            "cyan" -> COLOR_CYAN
            "amber" -> COLOR_AMBER
            "violet" -> COLOR_VIOLET
            "teal" -> COLOR_TEAL
            else -> COLOR_DEFAULT
        }
    }

    companion object {
        const val EXTRA_QR_CONTENT = "extra_qr_content"
    }
}
