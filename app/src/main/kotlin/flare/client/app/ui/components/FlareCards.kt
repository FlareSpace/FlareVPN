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
                                    Text(
                                        text = if (isError) I18n.strings.label_error else "$latency ms",
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
                color = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
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
    isExpanded: Boolean = false,
    isRefreshing: Boolean = false,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.ALL,
    onUpdateClick: () -> Unit,
    onSpeedTestClick: () -> Unit,
    onEditJsonClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val isVirtual = name == I18n.strings.sub_single_profiles
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)
    
    FlareCard(
        cornerType = cornerType,
        paddingHorizontal = 0.dp,
        paddingVertical = 0.dp,
        cornerRadius = 15.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 16.dp),
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
                                        .fillMaxWidth(trafficProgress)
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
                                color = FlareTheme.colors.trafficTextColor
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.widthIn(min = 116.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                                clip = false
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (FlareTheme.colors.isDark) {
                                        listOf(
                                            Color(0xFFFFFFFF).copy(alpha = 0.08f),
                                            Color(0xFFFFFFFF).copy(alpha = 0.03f)
                                        )
                                    } else {
                                        listOf(
                                            Color(0xFFFFFFFF).copy(alpha = 0.65f),
                                            Color(0xFFFFFFFF).copy(alpha = 0.35f)
                                        )
                                    }
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = if (FlareTheme.colors.isDark) {
                                        listOf(
                                            Color.White.copy(alpha = 0.18f),
                                            Color.White.copy(alpha = 0.03f)
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.55f),
                                            Color.Black.copy(alpha = 0.05f)
                                        )
                                    }
                                ),
                                shape = CircleShape
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    onClick = onUpdateClick,
                                    enabled = !isRefreshing
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = accentColor
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_refresh),
                                    contentDescription = I18n.strings.label_update,
                                    tint = FlareTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(16.dp)
                                .background(
                                    if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.12f)
                                    else Color.Black.copy(alpha = 0.08f)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSpeedTestClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_speedometer),
                                contentDescription = I18n.strings.label_speed_test,
                                tint = FlareTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(16.dp)
                                .background(
                                    if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.12f)
                                    else Color.Black.copy(alpha = 0.08f)
                                )
                        )

                        Box {
                            var menuExpanded by remember { mutableStateOf(false) }
                            
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = { menuExpanded = true }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = null,
                                    tint = FlareTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
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
                    if (expire > 0) {
                        val expireMillis = if (expire > 1000000000000L) expire else expire * 1000L
                        val date = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(expireMillis))
                        Text(
                            text = I18n.strings.label_expires.format(date),
                            fontFamily = GeologicaMedium,
                            fontSize = 9.sp,
                            color = FlareTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (!description.isNullOrEmpty()) {
                Text(
                    text = description,
                    fontFamily = GeologicaRegular,
                    fontSize = 11.sp,
                    color = FlareTheme.colors.textSecondary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                        .alpha(0.8f)
                )
            }
        }
        
        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            HorizontalDivider(
                color = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                thickness = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
