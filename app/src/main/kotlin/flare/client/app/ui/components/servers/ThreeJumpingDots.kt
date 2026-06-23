package flare.client.app.ui.components.servers

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ThreeJumpingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotSpacing: Dp = 6.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jumpingDots")
    
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 0
                -12f at 300
                0f at 600
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 200
                -12f at 500
                0f at 800
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 400
                -12f at 700
                0f at 1000
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer { translationY = dot1Offset }
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer { translationY = dot2Offset }
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer { translationY = dot3Offset }
                .background(dotColor, CircleShape)
        )
    }
}
