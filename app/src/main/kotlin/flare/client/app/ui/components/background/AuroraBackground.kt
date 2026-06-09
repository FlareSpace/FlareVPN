package flare.client.app.ui.components.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import flare.client.app.ui.theme.FlareTheme

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier
) {
    val colors = FlareTheme.colors
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp }
    val screenHeight = with(density) { configuration.screenHeightDp.dp }
    
    val infiniteTransition = rememberInfiniteTransition()

    val x1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = screenWidth.value + 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val y1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = screenHeight.value / 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(23000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val x2 by infiniteTransition.animateFloat(
        initialValue = screenWidth.value + 50f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val y2 by infiniteTransition.animateFloat(
        initialValue = screenHeight.value + 50f,
        targetValue = screenHeight.value / 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val x3 by infiniteTransition.animateFloat(
        initialValue = screenWidth.value / 2f,
        targetValue = screenWidth.value / 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(17000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val y3 by infiniteTransition.animateFloat(
        initialValue = screenHeight.value / 2f,
        targetValue = screenHeight.value - 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier.fillMaxSize().background(colors.gradientBase)) {
        Box(
            modifier = Modifier
                .offset(x = x1.dp, y = y1.dp)
                .size(300.dp)
                .background(colors.accent.copy(alpha = 0.6f), CircleShape)
                .blur(120.dp)
        )
        Box(
            modifier = Modifier
                .offset(x = x2.dp, y = y2.dp)
                .size(350.dp)
                .background(colors.accentEnd.copy(alpha = 0.5f), CircleShape)
                .blur(140.dp)
        )
        Box(
            modifier = Modifier
                .offset(x = x3.dp, y = y3.dp)
                .size(250.dp)
                .background(colors.textPrimary.copy(alpha = 0.2f), CircleShape)
                .blur(100.dp)
        )
    }
}
