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




@Composable
fun FlareSettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    value: String? = null,
    showArrow: Boolean = true,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true,
    onClick: (android.view.View) -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    var anchorView: android.view.View? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        AndroidView(
            factory = { context ->
                android.view.View(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0f },
            update = { anchorView = it }
        )

        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            onClick = { anchorView?.let { onClick(it) } },
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = FlareTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            
                            FlareGlassTooltip(
                                expanded = showTooltip && hazeState != null,
                                onDismissRequest = { showTooltip = false },
                                text = description,
                                hazeState = hazeState
                            )
                        }
                    }
                }

                if (value != null) {
                    Text(
                        text = value,
                        fontFamily = GeologicaRegular,
                        fontSize = 14.sp,
                        color = FlareTheme.colors.textSecondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                if (showArrow) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = FlareTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

            }
        }


        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}

@Composable
fun FlareSettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    accentColor: Color = FlareTheme.colors.accent,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = FlareTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = accentColor
                                )
                            }
                            
                            FlareGlassTooltip(
                                expanded = showTooltip && hazeState != null,
                                onDismissRequest = { showTooltip = false },
                                text = description,
                                hazeState = hazeState
                            )
                        }
                    }
                }

                FlareGlassSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    accentColor = accentColor
                )
            }
        }

        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}

@Composable
fun FlareSettingsValueItem(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
    menuItems: List<flare.client.app.util.GlassUtils.MenuItem>? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    accentColor: Color = FlareTheme.colors.accent,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true
) {
    var showTooltip by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            onClick = {
                if (menuItems != null) {
                    menuExpanded = true
                } else {
                    onClick?.invoke()
                }
            },
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = FlareTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = accentColor
                                )
                            }
                            
                            FlareGlassTooltip(
                                expanded = showTooltip && hazeState != null,
                                onDismissRequest = { showTooltip = false },
                                text = description,
                                hazeState = hazeState
                            )
                        }
                    }
                }

                Text(
                    text = value,
                    fontFamily = GeologicaMedium,
                    fontSize = 16.sp,
                    color = accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = FlareTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )

            }
        }
        
        if (menuItems != null) {
            FlareGlassMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = menuItems,
                hazeState = hazeState,
                alignment = Alignment.TopEnd
            )
        }

        if (cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}

@Composable
fun FlareSettingsInputItem(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    icon: Int = 0,
    description: String? = null,
    hint: String = "",
    suffix: String = "",
    cornerType: DisplayItem.CornerType = DisplayItem.CornerType.NONE,
    accentColor: Color = FlareTheme.colors.accent,
    isValid: Boolean = false,
    showBorder: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    iconBgColor: Color = Color.Unspecified,
    useGlassTooltipButton: Boolean = true,
    action: @Composable (RowScope.() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val baseCardColor = FlareTheme.colors.bgItem.copy(alpha = 0.85f)
    val cardBgColor by animateColorAsState(
        targetValue = if (isFocused) {
            accentColor.copy(alpha = 0.08f).compositeOver(baseCardColor)
        } else {
            baseCardColor
        },
        animationSpec = tween(220),
        label = "inputHighlightBg"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else FlareTheme.colors.textPrimary,
        animationSpec = tween(220),
        label = "inputTitleColor"
    )

    val currentBorderColor = if (isFocused || isValid) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
    val currentBorderWidth = if (showBorder) 1.5.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        FlareCard(
            cornerType = cornerType,
            paddingHorizontal = 16.dp,
            paddingVertical = 0.dp,
            modifier = Modifier.fillMaxSize(),
            borderColor = if (showBorder) currentBorderColor else Color.Transparent,
            borderWidth = currentBorderWidth,
            backgroundColor = cardBgColor,
            cornerRadius = 20.dp,
            onClick = { focusRequester.requestFocus() }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) {
                        GlassIconContainer(iconBgColor = iconBgColor) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = GeologicaRegular,
                        fontSize = 16.sp,
                        color = titleColor,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = if (icon != 0) { if (iconBgColor != Color.Unspecified) 12.dp else 16.dp } else 0.dp)
                    )

                    if (description != null) {
                        Box {
                            if (useGlassTooltipButton) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FlareGlassButton(
                                        onClick = { showTooltip = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_info_i),
                                            contentDescription = null,
                                            tint = FlareTheme.colors.textPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            } else {
                                FlareInfoIconButton(
                                    onClick = { showTooltip = true },
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = accentColor
                                )
                            }
                            
                            FlareGlassTooltip(
                                expanded = showTooltip && hazeState != null,
                                onDismissRequest = { showTooltip = false },
                                text = description,
                                hazeState = hazeState
                            )
                        }
                    }

                    if (isValid) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = FlareTheme.colors.connectedGreen,
                            modifier = Modifier.padding(start = 8.dp).size(16.dp)
                        )
                    }

                    if (action != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        action()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                        .widthIn(min = 80.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = GeologicaMedium,
                            fontSize = 16.sp,
                            color = accentColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        ),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .fillMaxWidth(),
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = hint,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 16.sp,
                                        color = FlareTheme.colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                if (suffix.isNotEmpty()) {
                    Text(
                        text = suffix,
                        fontFamily = GeologicaMedium,
                        fontSize = 16.sp,
                        color = accentColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        if (!showBorder && cornerType != DisplayItem.CornerType.BOTTOM && cornerType != DisplayItem.CornerType.ALL) {
            FlareDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasIcon = icon != 0,
                dividerOffset = if (icon != 0) {
                    if (iconBgColor != Color.Unspecified) 60.dp else 56.dp
                } else 16.dp
            )
        }
    }
}
