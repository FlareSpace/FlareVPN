package flare.client.app.ui.notification

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import flare.client.app.R
import flare.client.app.ui.components.GeologicaMedium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import flare.client.app.ui.theme.FlareTheme
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


@Composable
fun ComposeNotificationHost(
    modifier: Modifier = Modifier,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState? = null
) {
    val notifications = remember { mutableStateListOf<NotificationItemState>() }
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppNotificationManager.notifications.collect { data ->
            if (notifications.size >= 3) {
                
                notifications.firstOrNull { it.isVisible }?.let { oldest ->
                    oldest.isVisible = false
                }
            }
            
            val item = NotificationItemState(data = data)
            notifications.add(item)
            
            
            scope.launch {
                delay(3000)
                isExpanded = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        notifications.forEachIndexed { index, item ->
            val posFromTop = notifications.size - 1 - index
            
            
            key(item.id) {
                NotificationCard(
                    item = item,
                    posFromTop = posFromTop,
                    totalCount = notifications.size,
                    isExpanded = isExpanded,
                    accentColor = accentColor,
                    hazeState = hazeState,
                    onDismiss = {
                        item.isVisible = false
                    },
                    onClick = {
                        if (!isExpanded && notifications.size > 1) {
                            isExpanded = true
                        }
                    },
                    onRemoveRequest = {
                        notifications.remove(item)
                    }
                )
            }
        }
    }
}

class NotificationItemState(
    val id: Long = System.nanoTime(),
    val data: NotificationData,
    initialVisible: Boolean = true
) {
    var isVisible by mutableStateOf(initialVisible)
}

@Composable
fun NotificationCard(
    item: NotificationItemState,
    posFromTop: Int,
    totalCount: Int,
    isExpanded: Boolean,
    accentColor: Color,
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    onRemoveRequest: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    
    val swipeOffset = remember { Animatable(0f) }
    var isSwipingUp by remember { mutableStateOf(false) }

    
    var isEntering by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        
        delay(10)
        isEntering = false
    }

    val targetAlpha = if (!item.isVisible || isSwipingUp) 0f else 1f
    
    val targetScale = if (isExpanded) 1f else (1f - (posFromTop * 0.05f)).coerceAtLeast(0.8f)
    
    val baseTargetY = if (isExpanded) {
        80.dp * posFromTop
    } else {
        10.dp * posFromTop
    }

    
    val finalTargetY = if (isEntering) baseTargetY - 80.dp else baseTargetY

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "alpha",
        finishedListener = { if (it == 0f) onRemoveRequest() }
    )
    
    
    val swipeScaleEffect = if (swipeOffset.value < 0) {
        (1f - (swipeOffset.value.absoluteValue / 1000f)).coerceAtLeast(0.9f)
    } else 1f

    val scale by animateFloatAsState(
        targetValue = targetScale * swipeScaleEffect,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )
    
    val translateY by animateDpAsState(
        targetValue = finalTargetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translateY"
    )

    
    var progress by remember { mutableStateOf(1f) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = item.data.durationSec * 1000,
                easing = LinearEasing
            )
        ) { value, _ ->
            progress = value
        }
        if (!isSwipingUp) {
            onDismiss()
        }
    }

    val isDark = FlareTheme.colors.isDark
    val bgBase = FlareTheme.colors.bgNotificationBar

    Box(
        modifier = Modifier
            .offset { 
                IntOffset(
                    0, 
                    with(density) { (translateY.toPx() + swipeOffset.value).roundToInt() }
                ) 
            }
            .zIndex((totalCount - posFromTop).toFloat())
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha,
                compositingStrategy = CompositingStrategy.Offscreen
            )
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(Unit) {
                val dismissThreshold = 60.dp.toPx()
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        
                        if (dragAmount < 0 || swipeOffset.value < 0) {
                            scope.launch {
                                swipeOffset.snapTo(swipeOffset.value + dragAmount)
                            }
                        }
                    },
                    onDragEnd = {
                        if (swipeOffset.value < -dismissThreshold) { 
                            isSwipingUp = true
                            scope.launch {
                                swipeOffset.animateTo(
                                    targetValue = -1000f,
                                    animationSpec = tween(400, easing = FastOutLinearInEasing)
                                )
                                onRemoveRequest()
                            }
                        } else {
                            scope.launch {
                                swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    }
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .let { m ->
                if (hazeState != null) {
                    m.hazeEffect(state = hazeState) {
                        blurRadius = 24.dp
                    }
                } else m
            }
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(bgBase.copy(alpha = 0.7f), bgBase.copy(alpha = 0.55f))
                    } else {
                        listOf(bgBase.copy(alpha = 0.25f), bgBase.copy(alpha = 0.15f))
                    }
                )
            )
            .border(
                1.dp, 
                if (isDark) FlareTheme.colors.glassStroke else FlareTheme.colors.dividerColor, 
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconRes = item.data.iconRes ?: when (item.data.type) {
                NotificationType.SUCCESS -> R.drawable.ic_success
                NotificationType.ERROR -> R.drawable.ic_error
                NotificationType.WARNING -> R.drawable.ic_warning
            }
            
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = item.data.text,
                color = FlareTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontFamily = GeologicaMedium,
                modifier = Modifier.weight(1f)
            )
            
            if (item.data.actionText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { 
                            item.data.onAction?.invoke()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = item.data.actionText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = GeologicaMedium
                    )
                }
            }
        }
        
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(accentColor)
        )
    }
}


fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
