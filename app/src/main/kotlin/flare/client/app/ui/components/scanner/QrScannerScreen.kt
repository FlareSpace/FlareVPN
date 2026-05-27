package flare.client.app.ui.components.scanner

import android.Manifest
import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import flare.client.app.data.SettingsManager
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.theme.FlareTheme
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onGalleryClicked: () -> Unit,
    accentColor: Color
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    LaunchedEffect(context) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    var isSuccessVisible by remember { mutableStateOf(false) }
    var detectedValue by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val provider = cameraProvider
        if (provider != null) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                cameraExecutor,
                                BarcodeAnalyzer { value ->
                                    if (!isSuccessVisible) {
                                        detectedValue = value
                                        isSuccessVisible = true
                                    }
                                }
                            )
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = { }
            )
        }

        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 52.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = I18n.strings.qr_camera_hint,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 80.dp)
            )

            Button(
                onClick = onGalleryClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = I18n.strings.btn_select_from_gallery,
                    fontSize = 15.sp
                )
            }
        }

        
        QrSuccessOverlay(
            isVisible = isSuccessVisible,
            onAnimationEnd = {
                detectedValue?.let { onBarcodeDetected(it) }
            }
        )
    }
}
