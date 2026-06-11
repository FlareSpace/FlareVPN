package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import android.widget.ImageView
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import flare.client.app.util.GlassUtils
import flare.client.app.data.model.PingState
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.theme.FlareTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect


@Composable
fun ProfileCard(
    name: String,
    description: String? = null,
    isSelected: Boolean = false,
    pingState: PingState = PingState.None,
    pingStyle: String = "time",
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    chainNumber: Int? = null,
    onClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onQrCodeClick: () -> Unit = {},
    onEditJsonClick: () -> Unit,
    onEditSimpleClick: () -> Unit = {},
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val selectionBgColor = FlareTheme.colors.bgProfileSelected
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {

        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 0.dp,
            paddingVertical = 0.dp,
            onClick = onClick,
            onLongClick = { offset ->
                touchOffset = offset
                menuExpanded = true
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(selectionBgColor)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(accentColor)
                            .align(Alignment.CenterStart)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chainNumber != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(24.dp)
                                .border(
                                    width = 1.dp,
                                    color = accentColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chainNumber.toString(),
                                fontFamily = GeologicaMedium,
                                fontSize = 11.sp,
                                color = accentColor
                            )
                        }
                    }

                    val startPadding = if (chainNumber != null) 10.dp else 16.dp
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = startPadding, end = 12.dp)
                            .alpha(if (isSelected) 1.0f else 0.7f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name,
                            fontFamily = GeologicaRegular,
                            fontSize = 14.sp,
                            color = if (isSelected) FlareTheme.colors.textProfileSelectedPrimary else FlareTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!description.isNullOrEmpty()) {
                            Text(
                                text = description,
                                fontFamily = GeologicaRegular,
                                fontSize = 11.sp,
                                color = if (isSelected) FlareTheme.colors.textProfileSelectedSecondary else FlareTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (pingState is PingState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = accentColor
                            )
                        } else if (pingState is PingState.Result) {
                            val latency = pingState.latency
                            val isError = pingState.isError
                            
                            val showIcon = pingStyle == "icon" || pingStyle == "both"
                            val showText = pingStyle == "time" || pingStyle == "both"

                            val (iconRes, textColor) = when {
                                isError || latency > 5000 -> R.drawable.ic_error to Color.Red
                                latency <= 300 -> R.drawable.ic_success to Color(0xFF4CAF50)
                                latency <= 800 -> R.drawable.ic_warning to Color(0xFFFFC107)
                                else -> R.drawable.ic_error to Color.Red
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (showIcon) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                }
                                if (showText) {
                                    val errText = if (isError) {
                                        val rawMsg = pingState.errorMessage ?: ""
                                        val isRussian = I18n.strings.label_error == "Ошибка"
                                        if (isRussian) {
                                            when (rawMsg) {
                                                "Timeout" -> "Таймаут"
                                                "DNS Fail" -> "DNS сбой"
                                                "Config Err" -> "Ошибка конф."
                                                "Core err" -> "Core ERR"
                                                "TLS Failed" -> "TLS Fail"
                                                "Unreachable" -> "Недоступен"
                                                "Refused" -> "Отказ"
                                                "Failed" -> "Ошибка"
                                                "" -> "Ошибка"
                                                else -> rawMsg
                                            }
                                        } else {
                                            when (rawMsg) {
                                                "Core err" -> "Core ERR"
                                                "TLS Failed" -> "TLS Fail"
                                                "" -> "Error"
                                                else -> rawMsg
                                            }
                                        }
                                    } else {
                                        "$latency ms"
                                    }
                                    Text(
                                        text = errText,
                                        fontFamily = GeologicaRegular,
                                        fontSize = 12.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }

                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(20.dp)
                            )
                        }
                    }

                    
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(
                                if (isSelected) FlareTheme.colors.dividerProfileSelected
                                else if (FlareTheme.colors.isDark) FlareTheme.colors.bgSurface.copy(alpha = 0.3f)
                                else Color.Black.copy(alpha = 0.1f)
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onEditJsonClick,
                        modifier = Modifier.size(32.dp) 
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = "Edit JSON",
                            tint = if (isSelected) accentColor else FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        FlareGlassMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            items = listOf(
                flare.client.app.util.GlassUtils.MenuItem(1, I18n.strings.menu_qr_code) {
                    menuExpanded = false
                    onQrCodeClick()
                },
                flare.client.app.util.GlassUtils.MenuItem(2, I18n.strings.menu_link) {
                    menuExpanded = false
                    onShareClick()
                }
            ),
            hazeState = hazeState,
            touchOffset = touchOffset
        )
        
        
        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                thickness = 0.5.dp,
                color = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)
            )
        }
    }
}

@Composable
fun SubscriptionCard(
    name: String,
    description: String? = null,
    trafficInfo: String? = null,
    trafficProgress: Float = 0f,
    expire: Long = 0L,
    updateInterval: Long = 0L,
    isExpanded: Boolean = false,
    isRefreshing: Boolean = false,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.ALL,
    onUpdateClick: () -> Unit,
    onSpeedTestClick: () -> Unit,
    onEditJsonClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
    isPinned: Boolean = false,
    showQrAndLink: Boolean = true,
    onPinClick: () -> Unit = {},
    onQrClick: () -> Unit = {},
    onShareLinkClick: () -> Unit = {},
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    val isVirtual = name == I18n.strings.sub_single_profiles
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)
    
    Box(modifier = Modifier.fillMaxWidth()) {
        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 0.dp,
            paddingVertical = 0.dp,
            cornerRadius = 15.dp,
            onClick = onClick,
            onLongClick = { offset ->
                touchOffset = offset
                contextMenuExpanded = true
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = FlareTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, end = 8.dp)
                    ) {
                        Text(
                            text = name,
                            fontFamily = GeologicaMedium,
                            fontSize = 15.sp,
                            color = FlareTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (!trafficInfo.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            color = FlareTheme.colors.dividerColor,
                                            shape = CircleShape
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(trafficProgress.coerceIn(0f, 1f))
                                            .background(
                                                color = accentColor,
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Text(
                                    text = trafficInfo,
                                    modifier = Modifier.align(Alignment.Center),
                                    fontFamily = GeologicaMedium,
                                    fontSize = 10.sp,
                                    color = FlareTheme.colors.textPrimary
                                )
                                if (trafficProgress > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(ProgressClipShape(trafficProgress))
                                    ) {
                                        Text(
                                            text = trafficInfo,
                                            modifier = Modifier.align(Alignment.Center),
                                            fontFamily = GeologicaMedium,
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (expire > 0 || updateInterval > 0) {
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                if (expire > 0) {
                                    val expireMillis = if (expire > 1000000000000L) expire else expire * 1000L
                                    val date = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(expireMillis))
                                    Text(
                                        text = I18n.strings.label_expires.format(date),
                                        fontFamily = GeologicaMedium,
                                        fontSize = 9.sp,
                                        color = FlareTheme.colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (updateInterval > 0) {
                                    val formattedInterval = formatUpdateInterval(updateInterval)
                                    Text(
                                        text = I18n.strings.label_update_interval.format(formattedInterval),
                                        fontFamily = GeologicaMedium,
                                        fontSize = 9.sp,
                                        color = FlareTheme.colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.widthIn(min = 116.dp)
                    ) {
                        FlareGlassContainer(
                            shape = CircleShape,
                            radius = 17.dp,
                            hazeState = hazeState,
                            modifier = Modifier.offset(x = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 5.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val buttonTint = FlareTheme.colors.textPrimary

                                FlareGlassButton(
                                    onClick = onUpdateClick,
                                    enabled = !isRefreshing
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 1.5.dp,
                                            color = accentColor
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_refresh),
                                            contentDescription = I18n.strings.label_update,
                                            tint = buttonTint,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(5.dp))

                                FlareGlassButton(
                                    onClick = onSpeedTestClick
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_speedometer),
                                        contentDescription = I18n.strings.label_speed_test,
                                        tint = buttonTint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(5.dp))

                                Box {
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    
                                    FlareGlassButton(
                                        onClick = { menuExpanded = true }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_more_vert),
                                            contentDescription = null,
                                            tint = buttonTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    val editLabel = I18n.strings.menu_edit_subscription
                                    val deleteLabel = I18n.strings.menu_delete_subscription
                                    
                                    val items = if (isVirtual) {
                                        listOf(
                                            flare.client.app.util.GlassUtils.MenuItem(1, deleteLabel) { 
                                                menuExpanded = false
                                                onDeleteClick() 
                                            }
                                        )
                                    } else {
                                        listOf(
                                            flare.client.app.util.GlassUtils.MenuItem(1, editLabel) { 
                                                menuExpanded = false
                                                onEditJsonClick() 
                                            },
                                            flare.client.app.util.GlassUtils.MenuItem(2, deleteLabel) { 
                                                menuExpanded = false
                                                onDeleteClick() 
                                            }
                                        )
                                    }
                                    
                                    FlareGlassMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        items = items,
                                        hazeState = hazeState,
                                        alignment = Alignment.TopEnd
                                    )
                                }
                            }
                        }
                    }
                }

                if (!description.isNullOrEmpty()) {
                    HorizontalDivider(
                        color = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                        thickness = 0.5.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp, top = 6.dp)
                            .fillMaxWidth()
                            .background(
                                color = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = description,
                            fontFamily = GeologicaRegular,
                            fontSize = 11.sp,
                            color = FlareTheme.colors.textSecondary,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                }
            }
            
            if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
                HorizontalDivider(
                    color = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                    thickness = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (isPinned) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = FlareTheme.colors.textSecondary.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 6.dp, start = 6.dp)
                    .size(11.dp)
            )
        }

        val menuItems = remember(isPinned, showQrAndLink) {
            val list = mutableListOf<flare.client.app.util.GlassUtils.MenuItem>()
            if (showQrAndLink) {
                list.add(flare.client.app.util.GlassUtils.MenuItem(1, I18n.strings.menu_qr_code) {
                    contextMenuExpanded = false
                    onQrClick()
                })
                list.add(flare.client.app.util.GlassUtils.MenuItem(2, I18n.strings.menu_link) {
                    contextMenuExpanded = false
                    onShareLinkClick()
                })
            }
            val pinLabel = if (isPinned) I18n.strings.menu_unpin_subscription else I18n.strings.menu_pin_subscription
            list.add(flare.client.app.util.GlassUtils.MenuItem(3, pinLabel) {
                contextMenuExpanded = false
                onPinClick()
            })
            list
        }

        FlareGlassMenu(
            expanded = contextMenuExpanded,
            onDismissRequest = { contextMenuExpanded = false },
            items = menuItems,
            hazeState = hazeState,
            touchOffset = touchOffset
        )
    }
}

@Composable
fun AppSelectionItem(
    name: String,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    isChecked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter ?: painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
        }

        Text(
            text = name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            fontFamily = GeologicaRegular,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isChecked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = FlareTheme.colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GlassMenuItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = FlareTheme.colors.menuTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatUpdateInterval(seconds: Long): String {
    if (seconds <= 0) return ""
    val mins = seconds / 60
    val hours = seconds / 3600
    val days = seconds / 86400
    
    val isRussian = I18n.strings.label_update == "Обновить"
    
    return when {
        days > 0 -> {
            val remainHours = hours % 24
            if (remainHours > 0) {
                if (isRussian) "${days} д. ${remainHours} ч." else "${days} d ${remainHours} h"
            } else {
                if (isRussian) "${days} д." else "${days} d"
            }
        }
        hours > 0 -> {
            val remainMins = mins % 60
            if (remainMins > 0) {
                if (isRussian) "${hours} ч. ${remainMins} мин." else "${hours} h ${remainMins} m"
            } else {
                if (isRussian) "${hours} ч." else "${hours} h"
            }
        }
        else -> {
            if (isRussian) "${mins} мин." else "${mins} m"
        }
    }
}

private class ProgressClipShape(private val progress: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val clipWidth = size.width * progress.coerceIn(0f, 1f)
        val radius = size.height / 2f
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 0f,
                    top = 0f,
                    right = clipWidth,
                    bottom = size.height
                ),
                topLeft = androidx.compose.ui.geometry.CornerRadius(radius),
                topRight = androidx.compose.ui.geometry.CornerRadius(radius),
                bottomRight = androidx.compose.ui.geometry.CornerRadius(radius),
                bottomLeft = androidx.compose.ui.geometry.CornerRadius(radius)
            )
        )
    }
}
