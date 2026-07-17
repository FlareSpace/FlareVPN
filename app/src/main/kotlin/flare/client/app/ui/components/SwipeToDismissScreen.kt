package flare.client.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.dp
import flare.client.app.ui.theme.FlareTheme
import kotlinx.coroutines.launch
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlin.math.abs

@Composable
fun SwipeToDismissScreen(
    onDismissRight: (() -> Unit)? = null,
    onDismissLeft: (() -> Unit)? = null,
    onSwipeDismissStart: () -> Unit = {},
    backgroundContentRight: (@Composable () -> Unit)? = null,
    backgroundContentLeft: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    val baseBackgroundColor = FlareTheme.colors.bgDark

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val dismissThreshold = screenWidthPx * 0.35f
        val velocityThreshold = 500f

        val velocityTrackerRef = remember { mutableStateOf(VelocityTracker()) }

        val isDraggingRight by remember { derivedStateOf { dragOffsetX.value > 0.5f } }
        val isDraggingLeft by remember { derivedStateOf { dragOffsetX.value < -0.5f } }

        var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
        var predictiveBackTouchY by remember { mutableFloatStateOf(0f) }
        var isPredictiveBackActive by remember { mutableStateOf(false) }
        
        PredictiveBackHandler(enabled = onDismissRight != null) { progress ->
            try {
                isPredictiveBackActive = true
                progress.collect { backEvent ->
                    predictiveBackProgress = backEvent.progress
                    predictiveBackTouchY = backEvent.touchY
                }
                onDismissRight?.invoke()
            } catch (e: CancellationException) {
                isPredictiveBackActive = false
                predictiveBackProgress = 0f
            }
        }
        
        val animatedPredictiveProgress by animateFloatAsState(
            targetValue = if (isPredictiveBackActive) predictiveBackProgress else 0f,
            label = "predictive_progress"
        )
        val animatedPredictiveTouchY by animateFloatAsState(
            targetValue = predictiveBackTouchY,
            label = "predictive_touch_y"
        )

        val showRightBackground = isDraggingRight || animatedPredictiveProgress > 0f

        if (backgroundContentRight != null && showRightBackground) {
            Box(modifier = Modifier.fillMaxSize()) { backgroundContentRight() }
        }
        
        if (backgroundContentLeft != null && isDraggingLeft) {
            Box(modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = screenWidthPx + dragOffsetX.value
                }
            ) { backgroundContentLeft() }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onDismissRight, onDismissLeft) {
                    var currentDragOffset = 0f

                    detectHorizontalDragGestures(
                        onDragStart = { _ ->
                            velocityTrackerRef.value = VelocityTracker()
                            currentDragOffset = dragOffsetX.value
                        },
                        onDragEnd = {
                            val velocity = velocityTrackerRef.value.calculateVelocity().x
                            coroutineScope.launch {
                                val shouldDismissRight = onDismissRight != null && 
                                    dragOffsetX.value >= 0f && 
                                    (dragOffsetX.value > dismissThreshold || velocity > velocityThreshold)
                                
                                val shouldDismissLeft = onDismissLeft != null && 
                                    dragOffsetX.value <= 0f && 
                                    (dragOffsetX.value < -dismissThreshold || velocity < -velocityThreshold)
                                
                                if (shouldDismissRight && onDismissRight != null) {
                                    onSwipeDismissStart()
                                    dragOffsetX.animateTo(
                                        targetValue = screenWidthPx,
                                        animationSpec = tween(durationMillis = 220)
                                    )
                                    onDismissRight()
                                } else if (shouldDismissLeft && onDismissLeft != null) {
                                    onSwipeDismissStart()
                                    dragOffsetX.animateTo(
                                        targetValue = -screenWidthPx,
                                        animationSpec = tween(durationMillis = 220)
                                    )
                                    onDismissLeft()
                                } else {
                                    dragOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            velocityTrackerRef.value.addPointerInputChange(change)
                            val canDragRight = onDismissRight != null
                            val canDragLeft = onDismissLeft != null
                            
                            val newOffset = currentDragOffset + dragAmount
                            val coercedOffset = when {
                                canDragRight && canDragLeft -> newOffset
                                canDragRight -> newOffset.coerceAtLeast(0f)
                                canDragLeft -> newOffset.coerceAtMost(0f)
                                else -> 0f
                            }
                            
                            val hasMoved = coercedOffset != currentDragOffset
                            currentDragOffset = coercedOffset
                            
                            if (hasMoved) {
                                change.consume()
                                coroutineScope.launch {
                                    dragOffsetX.snapTo(coercedOffset)
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    if (animatedPredictiveProgress > 0f) {
                        val scale = 1f - (0.1f * animatedPredictiveProgress)
                        scaleX = scale
                        scaleY = scale
                        translationX = animatedPredictiveProgress * (screenWidthPx * 0.1f)
                        val pivotY = if (constraints.maxHeight > 0) animatedPredictiveTouchY / constraints.maxHeight.toFloat() else 0.5f
                        transformOrigin = TransformOrigin(1f, pivotY)
                        if (animatedPredictiveProgress > 0.005f) {
                            clip = true
                            shape = RoundedCornerShape((32f * animatedPredictiveProgress).dp)
                        } else {
                            clip = false
                        }
                    } else {
                        val progress = (abs(dragOffsetX.value) / screenWidthPx).coerceIn(0f, 1f)
                        translationX = dragOffsetX.value
                        if (dragOffsetX.value > 0f) {
                            val scale = 1f - progress * 0.15f
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin.Center
                            if (progress > 0.005f) {
                                clip = true
                                shape = RoundedCornerShape(size = (progress * 28f).dp)
                            } else {
                                clip = false
                            }
                        } else {
                            scaleX = 1f
                            scaleY = 1f
                            clip = false
                            transformOrigin = TransformOrigin.Center
                        }
                    }
                }
        ) {
            content()
            if (animatedPredictiveProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            (if (FlareTheme.colors.isDark) Color.White else Color.Black)
                                .copy(alpha = 0.15f * animatedPredictiveProgress)
                        )
                )
            }
        }
    }
}
