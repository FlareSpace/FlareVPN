package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.theme.FlareTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import dev.chrisbanes.haze.HazeProgressive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlareCard(
    modifier: Modifier = Modifier,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    paddingHorizontal: androidx.compose.ui.unit.Dp = 16.dp,
    paddingVertical: androidx.compose.ui.unit.Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: ((androidx.compose.ui.geometry.Offset) -> Unit)? = null,
    showRipple: Boolean = false,
    borderColor: Color = Color.Transparent,
    borderWidth: androidx.compose.ui.unit.Dp = 0.dp,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val defaultBgColor = FlareTheme.colors.bgItem.copy(alpha = 0.85f)
    val resolvedBgColor = if (backgroundColor != Color.Unspecified) backgroundColor else defaultBgColor
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val shape = when (cornerType) {
        DisplayItem.CornerType.ALL -> RoundedCornerShape(cornerRadius)
        DisplayItem.CornerType.TOP -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        DisplayItem.CornerType.BOTTOM -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
        DisplayItem.CornerType.NONE -> androidx.compose.ui.graphics.RectangleShape
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(resolvedBgColor)
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape)
                else Modifier
            )
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick?.invoke() },
                            onLongPress = { offset -> 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onLongClick?.invoke(offset) 
                            }
                        )
                    }
                } else Modifier
            )
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        content = content
    )
}

@Composable
fun FlareGlassContainer(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    radius: androidx.compose.ui.unit.Dp = 12.dp,
    accentColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = FlareTheme.colors.isDark
    
    Box(
        modifier = modifier
            
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(
                            12.dp.toPx(),
                            0f,
                            4.dp.toPx(),
                            android.graphics.Color.argb(if (isDark) 75 else 20, 0, 0, 0)
                        )
                    }
                    val radiusPx = radius.toPx()
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        radiusPx,
                        radiusPx,
                        paint
                    )
                }
            }
            
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    } else if (accentColor != Color.Unspecified) {
                        listOf(
                            accentColor.copy(alpha = 0.15f),
                            accentColor.copy(alpha = 0.05f)
                        )
                    } else {
                        listOf(
                            Color(0xFFFFFFFF).copy(alpha = 0.85f),
                            Color(0xFFF2F2F7).copy(alpha = 0.60f)
                        )
                    }
                ),
                shape = shape
            )
            
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    } else if (accentColor != Color.Unspecified) {
                        listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.12f)
                        )
                    } else {
                        listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.04f)
                        )
                    }
                ),
                shape = shape
            )
            .clip(shape)
    ) {
        
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.18f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 15f
                    )
                )
        )
        
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun GlassIconContainer(
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = FlareTheme.colors.isDark
    val borderAlphaStart = if (isDark) 0.35f else 0.45f
    val borderAlphaEnd = if (isDark) 0.05f else 0.08f

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        iconBgColor,
                        iconBgColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlphaStart),
                        Color.White.copy(alpha = borderAlphaEnd),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.15f else 0.22f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 40f
                    )
                )
        )
        content()
    }
}

@Composable
fun FlareRoutingCard(
    rule: flare.client.app.ui.viewmodel.RoutingRuleState,
    onToggle: (Boolean) -> Unit,
    onModeClick: (String) -> Unit,
    onDownloadClick: () -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState? = null
) {
    FlareCard(
        modifier = Modifier.padding(bottom = 12.dp),
        cornerType = DisplayItem.CornerType.ALL,
        paddingVertical = 16.dp
    ) {
        var menuExpanded by remember { mutableStateOf(false) }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title(),
                    fontFamily = GeologicaMedium,
                    fontSize = 17.sp,
                    color = FlareTheme.colors.textPrimary
                )
                val description = rule.description?.invoke() ?: ""
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        fontFamily = GeologicaRegular,
                        fontSize = 12.sp,
                        color = FlareTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            FlareGlassSwitch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                accentColor = accentColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rule.isDownloading) {
                CircularProgressIndicator(
                    progress = { rule.progress / 100f },
                    modifier = Modifier.size(16.dp).padding(start = 4.dp),
                    color = accentColor,
                    strokeWidth = 2.dp,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                FlareAnimatedPercentText(
                    progress = rule.progress,
                    modifier = Modifier.padding(start = 6.dp)
                )
            } else {
                val isDownloaded = flare.client.app.singbox.GeoFileManager.isFileDownloaded(
                    androidx.compose.ui.platform.LocalContext.current,
                    rule.fileNames.first()
                )
                
                val iconRes = if (isDownloaded) R.drawable.ic_refresh else R.drawable.ic_download
                
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = FlareTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val updateText = if (isDownloaded) {
                    if (rule.lastUpdate == 0L) {
                        if (rule.isBuiltin) I18n.strings.routing_badge_builtin
                        else I18n.strings.routing_update_never
                    } else {
                        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(rule.lastUpdate))
                    }
                } else {
                    I18n.strings.routing_action_download
                }

                Text(
                    text = updateText,
                    fontFamily = GeologicaRegular,
                    fontSize = 12.sp,
                    color = FlareTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { menuExpanded = true }
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modeText = when (rule.mode) {
                        "proxy" -> I18n.strings.routing_mode_proxy
                        "block" -> I18n.strings.routing_mode_block
                        "direct" -> I18n.strings.routing_mode_direct
                        else -> I18n.strings.routing_mode_direct
                    }
                    Text(
                        text = modeText,
                        fontFamily = GeologicaMedium,
                        fontSize = 13.sp,
                        color = accentColor
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(13.dp)
                            .padding(start = 3.dp)
                            .graphicsLayer(rotationZ = 90f)
                    )
                }

                val modes = listOf(
                    "proxy" to I18n.strings.routing_mode_proxy,
                    "direct" to I18n.strings.routing_mode_direct,
                    "block" to I18n.strings.routing_mode_block
                )
                
                FlareGlassMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = modes.mapIndexed { i, mode ->
                        flare.client.app.util.GlassUtils.MenuItem(i, mode.second) {
                            onModeClick(mode.first)
                            menuExpanded = false
                        }
                    },
                    hazeState = hazeState,
                    alignment = Alignment.TopEnd
                )
            }
        }
    }
}
