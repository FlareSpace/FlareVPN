package flare.client.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.theme.FlareTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToReveal(
    modifier: Modifier = Modifier,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    isChained: Boolean = false,
    onDeleteClick: () -> Unit,
    onChainClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    val shape = when (cornerType) {
        DisplayItem.CornerType.ALL -> RoundedCornerShape(12.dp)
        DisplayItem.CornerType.TOP -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        DisplayItem.CornerType.BOTTOM -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        DisplayItem.CornerType.NONE -> androidx.compose.ui.graphics.RectangleShape
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val revealWidthPx = widthPx * 0.35f
        val density = LocalDensity.current
        val actionButtonWidthDp = with(density) { revealWidthPx.toDp() }

        val minOffset = -revealWidthPx
        val maxOffset = if (onChainClick != null) revealWidthPx else 0f

        val draggableState = rememberDraggableState { delta ->
            coroutineScope.launch {
                val newOffset = (dragOffset.value + delta).coerceIn(minOffset, maxOffset)
                dragOffset.snapTo(newOffset)
            }
        }

        val isRevealed by remember { derivedStateOf { dragOffset.value != 0f } }

        fun snapToOffset(targetOffset: Float) {
            coroutineScope.launch {
                dragOffset.animateTo(
                    targetOffset,
                    spring(stiffness = Spring.StiffnessMedium)
                )
            }
        }

        
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
        ) {
            
            val redColor = FlareTheme.colors.disconnectedRed
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .width(actionButtonWidthDp)
                    .graphicsLayer {
                        val progress = if (revealWidthPx > 0f) kotlin.math.abs(dragOffset.value.coerceAtMost(0f)) / revealWidthPx else 0f
                        alpha = progress
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                redColor.copy(alpha = 0.85f),
                                redColor
                            )
                        )
                    )
                    .clickable {
                        onDeleteClick()
                        snapToOffset(0f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            val progress = if (revealWidthPx > 0f) kotlin.math.abs(dragOffset.value.coerceAtMost(0f)) / revealWidthPx else 0f
                            scaleX = 0.5f + 0.5f * progress
                            scaleY = 0.5f + 0.5f * progress
                            alpha = progress
                        }
                )
            }

            
            if (onChainClick != null) {
                val chainColor = if (isChained) FlareTheme.colors.disconnectedRed else FlareTheme.colors.connectedGreen
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .width(actionButtonWidthDp)
                        .graphicsLayer {
                            val progress = if (revealWidthPx > 0f) kotlin.math.abs(dragOffset.value.coerceAtLeast(0f)) / revealWidthPx else 0f
                            alpha = progress
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    chainColor,
                                    chainColor.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .clickable {
                            onChainClick()
                            snapToOffset(0f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chain),
                        contentDescription = "Chain",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                val progress = if (revealWidthPx > 0f) kotlin.math.abs(dragOffset.value.coerceAtLeast(0f)) / revealWidthPx else 0f
                                scaleX = 0.5f + 0.5f * progress
                                scaleY = 0.5f + 0.5f * progress
                                alpha = progress
                            }
                    )
                }
            }
        }

        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val targetOffset = if (velocity < -500f) {
                            if (dragOffset.value > 0f) 0f else -revealWidthPx
                        } else if (velocity > 500f) {
                            if (dragOffset.value < 0f) 0f else if (onChainClick != null) revealWidthPx else 0f
                        } else {
                            if (dragOffset.value < -revealWidthPx * 0.45f) {
                                -revealWidthPx
                            } else if (dragOffset.value > revealWidthPx * 0.45f && onChainClick != null) {
                                revealWidthPx
                            } else {
                                0f
                            }
                        }
                        snapToOffset(targetOffset)
                    }
                )
        ) {
            content()

            
            if (isRevealed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { snapToOffset(0f) }
                            )
                        }
                )
            }
        }
    }
}
