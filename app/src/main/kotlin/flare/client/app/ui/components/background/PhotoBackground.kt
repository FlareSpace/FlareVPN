package flare.client.app.ui.components.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import flare.client.app.ui.theme.FlareTheme
import java.io.File

@Composable
fun PhotoBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    
    
    photoSeed: String = "default_seed"
) {
    val context = LocalContext.current
    val fileToLoad = File(context.filesDir, "background_photo.jpg")
    val fileExists = fileToLoad.exists()
    val overlayColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f)

    Box(modifier = modifier.fillMaxSize()) {
        if (fileExists) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fileToLoad)
                    .memoryCacheKey("background_photo_$photoSeed")
                    .diskCacheKey("background_photo_$photoSeed")
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
            )
        } else {
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FlareTheme.colors.gradientBase)
            )
        }

        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )
    }
}
