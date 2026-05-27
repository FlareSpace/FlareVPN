package flare.client.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import flare.client.app.R
import flare.client.app.util.GlassUtils
import flare.client.app.ui.theme.FlareTheme



class TransformOriginHolder {
    var pivotX: Float = 0.5f
    var pivotY: Float = 0.5f
}

@Composable
fun FlareGlassMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<GlassUtils.MenuItem>,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    touchOffset: androidx.compose.ui.geometry.Offset? = null,
    offset: IntOffset = IntOffset(0, 0),
    alignment: Alignment = Alignment.TopStart
) {
    if (expanded) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val originHolder = remember { TransformOriginHolder() }
        
        val positionProvider = remember(touchOffset, offset, alignment, density) {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize
                ): androidx.compose.ui.unit.IntOffset {
                    val margin = with(density) { 16.dp.roundToPx() }
                    
                    if (touchOffset != null) {
                        val clickX = anchorBounds.left + touchOffset.x.toInt()
                        val clickY = anchorBounds.top + touchOffset.y.toInt()
                        
                        val isLeftShifted = clickX + popupContentSize.width > windowSize.width - margin
                        val isUpShifted = clickY + popupContentSize.height > windowSize.height - margin
                        
                        var x = if (isLeftShifted) clickX - popupContentSize.width else clickX
                        var y = if (isUpShifted) clickY - popupContentSize.height else clickY
                        
                        x = x.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                        y = y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
                        
                        originHolder.pivotX = if (isLeftShifted) 1f else 0f
                        originHolder.pivotY = if (isUpShifted) 1f else 0f
                        
                        return androidx.compose.ui.unit.IntOffset(x, y)
                    } else {
                        val anchorWidth = anchorBounds.width
                        val anchorHeight = anchorBounds.height
                        
                        val isLeftShifted = alignment == Alignment.TopEnd || alignment == Alignment.CenterEnd || alignment == Alignment.BottomEnd
                        val isUpShifted = alignment == Alignment.BottomStart || alignment == Alignment.BottomCenter || alignment == Alignment.BottomEnd
                        
                        var x = if (isLeftShifted) {
                            anchorBounds.right - popupContentSize.width
                        } else {
                            anchorBounds.left
                        }
                        
                        var y = if (isUpShifted) {
                            anchorBounds.top - popupContentSize.height
                        } else {
                            anchorBounds.bottom
                        }
                        
                        x += offset.x
                        y += offset.y
                        
                        x = x.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                        y = y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
                        
                        originHolder.pivotX = if (isLeftShifted) 1f else 0f
                        originHolder.pivotY = if (isUpShifted) 1f else 0f
                        
                        return androidx.compose.ui.unit.IntOffset(x, y)
                    }
                }
            }
        }

        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = positionProvider,
            properties = PopupProperties(
                focusable = true,
                clippingEnabled = false 
            )
        ) {
            val isDark = FlareTheme.colors.isDark
            
            val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
            
            androidx.compose.runtime.LaunchedEffect(Unit) {
                launch {
                    alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }

            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = modifier
                    .width(160.dp)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        transformOrigin = TransformOrigin(originHolder.pivotX, originHolder.pivotY)
                    }
                    .clip(shape)
            ) {
                
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .flareGlass(
                            isDark = isDark,
                            radius = 16f,
                            intensity = 1.6f,
                            index = 1.5f,
                            glassHeight = 0.1f,
                            hasOutline = true,
                            outlineThickness = 2.0f
                        )
                        .let {
                            if (hazeState != null) {
                                it.hazeEffect(state = hazeState) {
                                    blurRadius = 3.dp
                                }
                            } else {
                                it.background(
                                    if (isDark) Color(0x661A1C1E) else Color(0x99FFFFFF)
                                )
                            }
                        }
                )

                
                FlareGlassMenuContent(
                    items = items,
                    onItemClick = {
                        it.onClick()
                        onDismissRequest()
                    }
                )
            }
        }
    }
}


@Composable
fun FlareGlassMenuContent(
    items: List<GlassUtils.MenuItem>,
    onItemClick: (GlassUtils.MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = FlareTheme.colors.isDark

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .padding(vertical = 4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        items.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(
                            bounded = true,
                            color = if (isDark) Color.White.copy(alpha = 0.1f)
                                    else Color.Black.copy(alpha = 0.1f)
                        ),
                        onClick = { onItemClick(item) }
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = item.title.toString(),
                    color = FlareTheme.colors.menuTextColor,
                    fontSize = 14.sp,
                    fontFamily = GeologicaMedium,
                    maxLines = 1
                )
            }

            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(start = 16.dp, end = 16.dp)
                        .background(
                            if (isDark) Color(0x14FFFFFF) else Color(0x1E000000)
                        )
                )
            }
        }
    }
}


@Composable
fun FlareGlassTooltipContent(
    text: CharSequence,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.toString(),
        modifier = modifier
            .widthIn(max = 280.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        color = FlareTheme.colors.textPrimary,
        fontSize = 14.sp,
        fontFamily = GeologicaRegular,
        lineHeight = 17.sp
    )
}
