package flare.client.app.test

import dev.chrisbanes.haze.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.testHaze(state: HazeState) = this.hazeEffect(state) {
    blurRadius = 15.dp
    mask = Brush.verticalGradient(
        colors = listOf(Color.Black, Color.Black, Color.Transparent)
    )
    progressive = HazeProgressive.verticalGradient(startY = 0f, endY = 100f)
}
