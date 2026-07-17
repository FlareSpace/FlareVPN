package flare.client.app.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import flare.client.app.ui.theme.FlareTheme
import kotlinx.coroutines.CancellationException

@Composable
fun PredictiveBackWrapper(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var swipeY by remember { mutableFloatStateOf(0f) }
    var isSwiping by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    PredictiveBackHandler(enabled = enabled) { progress ->
        try {
            isSwiping = true
            progress.collect { backEvent ->
                swipeProgress = backEvent.progress
                swipeY = backEvent.touchY
            }
            onBack()
        } catch (e: CancellationException) {
            isSwiping = false
            swipeProgress = 0f
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isSwiping) swipeProgress else 0f,
        label = "predictive_progress"
    )
    val animatedTouchY by animateFloatAsState(
        targetValue = swipeY,
        label = "predictive_touch_y"
    )

    val isDark = FlareTheme.colors.isDark

    val scale = 1f - (0.1f * animatedProgress)
    val translationX = animatedProgress * (screenWidthPx * 0.1f) 
    val pivotY = if (screenHeightPx > 0) animatedTouchY / screenHeightPx else 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.translationX = translationX
                this.transformOrigin = TransformOrigin(1f, pivotY) 
                this.shape = RoundedCornerShape((32f * animatedProgress).dp)
                this.clip = animatedProgress > 0f
            }
    ) {
        content()
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        (if (isDark) Color.White else Color.Black)
                            .copy(alpha = 0.15f * animatedProgress)
                    )
            )
        }
    }
}
