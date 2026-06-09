package flare.client.app.ui.components.dialogs

import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import flare.client.app.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Popup
import flare.client.app.ui.components.*
import flare.client.app.ui.theme.FlareTheme


@Composable
fun AppSelectionDialog(
    onDismissRequest: () -> Unit,
    tabIndex: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sitesText: String,
    onSitesTextChange: (String) -> Unit,
    modeText: String,
    onModeSelected: (String) -> Unit,
    isTriggerEnabled: Boolean,
    onTriggerChange: (Boolean) -> Unit,
    onTriggerHintClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    accentColor: Int,
    accentEndColor: Int = 0,
    selectedAppsCount: Int = 0,
    selectedSitesCount: Int = 0,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    appsContent: @Composable () -> Unit
) {
    val geologicaMedium = FontFamily(Font(R.font.geologica_medium))
    val geologicaRegular = FontFamily(Font(R.font.geologica_regular))

    var isSearchFocused by remember { mutableStateOf(false) }
    var isSitesFocused by remember { mutableStateOf(false) }

    GlassDialog(
        onDismissRequest = onDismissRequest,
        maxWidthDp = 300,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
        Text(
            text = I18n.strings.dialog_apps_title,
            color = FlareTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontFamily = geologicaMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        
        
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(40.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color(0x14FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .border(1.dp, Color(0x1A000000), androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
        ) {
            val dpPx = LocalDensity.current.density
            val pillLeft by animateFloatAsState(
                targetValue = if (tabIndex == 0) 4f * dpPx else 104f * dpPx,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            )
            val pillRight by animateFloatAsState(
                targetValue = if (tabIndex == 0) 96f * dpPx else 196f * dpPx,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            )

            ComposeLiquidPill(
                leftBound = pillLeft,
                rightBound = pillRight,
                pillHeight = 32f * dpPx,
                accentColor = Color(accentColor),
                accentEndColor = Color(if (accentEndColor != 0) accentEndColor else accentColor),
                isDark = FlareTheme.colors.isDark,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(0) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_apps),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorResource(if (tabIndex == 0) R.color.white else R.color.text_secondary)),
                            modifier = Modifier.size(24.dp)
                        )
                        if (selectedAppsCount > 0) {
                            Text(
                                text = " ($selectedAppsCount)",
                                color = colorResource(if (tabIndex == 0) R.color.white else R.color.text_secondary),
                                fontSize = 12.sp,
                                fontFamily = geologicaMedium,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(1) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_website),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (tabIndex == 1) FlareTheme.colors.white else FlareTheme.colors.textSecondary),
                            modifier = Modifier.size(24.dp)
                        )
                        if (selectedSitesCount > 0) {
                            Text(
                                text = " ($selectedSitesCount)",
                                color = if (tabIndex == 1) FlareTheme.colors.white else FlareTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontFamily = geologicaMedium,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(bottom = 8.dp)
        ) {
            AnimatedContent(
                targetState = tabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                }
            ) { targetTabIndex ->
                if (targetTabIndex == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val searchBgColor by animateColorAsState(
                            targetValue = if (isSearchFocused) {
                                Color(accentColor).copy(alpha = 0.08f).compositeOver(FlareTheme.colors.glassInputBg)
                            } else {
                                FlareTheme.colors.glassInputBg
                            },
                            animationSpec = tween(220),
                            label = "searchBg"
                        )
                        val searchBorderColor by animateColorAsState(
                            targetValue = if (isSearchFocused) Color(accentColor) else FlareTheme.colors.dividerColor,
                            animationSpec = tween(220),
                            label = "searchBorder"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(searchBgColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .border(1.dp, searchBorderColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = I18n.strings.search_apps_hint,
                                    color = FlareTheme.colors.textSecondary,
                                    fontSize = 14.sp,
                                    fontFamily = geologicaRegular
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(
                                    color = FlareTheme.colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = geologicaRegular
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Color(accentColor)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isSearchFocused = it.isFocused }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        appsContent()
                    }
                } else {
                    val sitesBgColor by animateColorAsState(
                        targetValue = if (isSitesFocused) {
                            Color(accentColor).copy(alpha = 0.08f).compositeOver(FlareTheme.colors.glassInputBg)
                        } else {
                            FlareTheme.colors.glassInputBg
                        },
                        animationSpec = tween(220),
                        label = "sitesBg"
                    )
                    val sitesBorderColor by animateColorAsState(
                        targetValue = if (isSitesFocused) Color(accentColor) else FlareTheme.colors.dividerColor,
                        animationSpec = tween(220),
                        label = "sitesBorder"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(sitesBgColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .border(1.dp, sitesBorderColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        if (sitesText.isEmpty()) {
                            Text(
                                text = I18n.strings.sites_hint,
                                color = FlareTheme.colors.textSecondary,
                                fontSize = 14.sp,
                                fontFamily = geologicaRegular
                            )
                        }
                        BasicTextField(
                            value = sitesText,
                            onValueChange = onSitesTextChange,
                            textStyle = TextStyle(
                                color = FlareTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                fontFamily = geologicaRegular
                            ),
                            cursorBrush = SolidColor(Color(accentColor)),
                            modifier = Modifier
                                .fillMaxSize()
                                .onFocusChanged { isSitesFocused = it.isFocused }
                        )
                    }
                }
            }
        }

        
        Box {
            var menuExpanded by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(FlareTheme.colors.glassInputBg, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .border(1.dp, FlareTheme.colors.dividerColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { menuExpanded = true }
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = I18n.strings.label_mode,
                    color = FlareTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = geologicaMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(modifier = Modifier.padding(start = 4.dp)) {
                    var tooltipExpanded by remember { mutableStateOf(false) }

                    FlareInfoIconButton(
                        onClick = { tooltipExpanded = true },
                        color = Color(accentColor)
                    )

                    val tooltipText = if (modeText == I18n.strings.split_mode_whitelist) {
                        I18n.strings.split_mode_whitelist_tooltip
                    } else {
                        I18n.strings.split_mode_blacklist_tooltip
                    }

                    if (tooltipExpanded) {
                        FlareGlassTooltip(
                            text = tooltipText,
                            onDismiss = { tooltipExpanded = false },
                            hazeState = hazeState
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = modeText,
                    color = Color(accentColor),
                    fontSize = 16.sp,
                    fontFamily = geologicaMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Image(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(accentColor)),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            FlareGlassMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = listOf(
                    flare.client.app.util.GlassUtils.MenuItem(0, I18n.strings.split_mode_whitelist) {
                        menuExpanded = false
                        onModeSelected("whitelist")
                    },
                    flare.client.app.util.GlassUtils.MenuItem(1, I18n.strings.split_mode_blacklist) {
                        menuExpanded = false
                        onModeSelected("blacklist")
                    }
                ),
                hazeState = hazeState,
                alignment = Alignment.TopEnd
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(FlareTheme.colors.glassInputBg, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .border(1.dp, FlareTheme.colors.dividerColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = I18n.strings.trigger_label,
                color = FlareTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontFamily = geologicaMedium
            )

            Box {
                var tooltipExpanded by remember { mutableStateOf(false) }

                FlareInfoIconButton(
                    onClick = { tooltipExpanded = true },
                    modifier = Modifier.padding(start = 4.dp),
                    color = Color(accentColor)
                )

                if (tooltipExpanded) {
                    FlareGlassTooltip(
                        text = I18n.strings.trigger_hint,
                        onDismiss = { tooltipExpanded = false },
                        hazeState = hazeState
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            FlareGlassSwitch(
                checked = isTriggerEnabled,
                onCheckedChange = onTriggerChange,
                accentColor = Color(accentColor)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = FlareTheme.colors.textSecondary),
                        onClick = onCancel
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = I18n.strings.btn_cancel,
                    color = FlareTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = geologicaMedium
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color(accentColor)),
                        onClick = onSave
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = I18n.strings.btn_save,
                    color = Color(accentColor),
                    fontSize = 14.sp,
                    fontFamily = geologicaMedium
                )
            }
        }
        }
    }
}

@Composable
private fun ComposeLiquidPill(
    leftBound: Float,
    rightBound: Float,
    pillHeight: Float,
    accentColor: Color,
    accentEndColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    showGlow: Boolean = false
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (leftBound >= rightBound || pillHeight <= 0f) return@Canvas

        val cy = height / 2f
        val halfH = pillHeight / 2f
        val radius = halfH

        val rect = androidx.compose.ui.geometry.Rect(
            left = leftBound,
            top = cy - halfH,
            right = rightBound,
            bottom = cy + halfH
        )

        drawContext.canvas.save()

        if (showGlow) {
            
            val ambientMargin = 22.dp.toPx()
            val glowRect = androidx.compose.ui.geometry.Rect(
                left = rect.left - ambientMargin,
                top = rect.top - ambientMargin,
                right = rect.right + ambientMargin,
                bottom = rect.bottom + ambientMargin
            )
            val outerRadius = radius + ambientMargin
            
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(accentEndColor.copy(alpha = 0.15f), Color.Transparent),
                    center = rect.center,
                    radius = glowRect.width * 0.6f
                ),
                topLeft = androidx.compose.ui.geometry.Offset(glowRect.left, glowRect.top),
                size = androidx.compose.ui.geometry.Size(glowRect.width, glowRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius, outerRadius)
            )
            
            
            val coreMargin = 10.dp.toPx()
            val coreGlowRect = androidx.compose.ui.geometry.Rect(
                left = rect.left - coreMargin,
                top = rect.top - coreMargin,
                right = rect.right + coreMargin,
                bottom = rect.bottom + coreMargin
            )
            val coreRadius = radius + coreMargin
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
                    center = rect.center,
                    radius = coreGlowRect.width * 0.5f
                ),
                topLeft = androidx.compose.ui.geometry.Offset(coreGlowRect.left, coreGlowRect.top),
                size = androidx.compose.ui.geometry.Size(coreGlowRect.width, coreGlowRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(coreRadius, coreRadius)
            )
        }

        
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(accentColor, accentEndColor),
                start = androidx.compose.ui.geometry.Offset(0f, rect.top),
                end = androidx.compose.ui.geometry.Offset(0f, rect.bottom)
            ),
            topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
            size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )

        
        val innerB = 1.dp.toPx()
        val innerGlowRect = androidx.compose.ui.geometry.Rect(
            left = rect.left + innerB,
            top = rect.top + innerB,
            right = rect.right - innerB,
            bottom = rect.bottom - innerB
        )
        val innerRadius = radius - innerB
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0f)),
                start = androidx.compose.ui.geometry.Offset(0f, rect.top),
                end = androidx.compose.ui.geometry.Offset(0f, rect.bottom)
            ),
            topLeft = androidx.compose.ui.geometry.Offset(innerGlowRect.left, innerGlowRect.top),
            size = androidx.compose.ui.geometry.Size(innerGlowRect.width, innerGlowRect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius, innerRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = innerB)
        )

        
        val b = 0.65f.dp.toPx()
        val borderRect = androidx.compose.ui.geometry.Rect(
            left = rect.left + b,
            top = rect.top + b,
            right = rect.right - b,
            bottom = rect.bottom - b
        )
        val borderRadius = radius - b
        val borderBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.16f),
                    accentEndColor.copy(alpha = 0.32f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, rect.top),
                end = androidx.compose.ui.geometry.Offset(0f, rect.bottom)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.7f),
                    accentEndColor.copy(alpha = 0.24f),
                    accentEndColor.copy(alpha = 0.4f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, rect.top),
                end = androidx.compose.ui.geometry.Offset(0f, rect.bottom)
            )
        }
        drawRoundRect(
            brush = borderBrush,
            topLeft = androidx.compose.ui.geometry.Offset(borderRect.left, borderRect.top),
            size = androidx.compose.ui.geometry.Size(borderRect.width, borderRect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(borderRadius, borderRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = b)
        )
        
        drawContext.canvas.restore()
    }
}


