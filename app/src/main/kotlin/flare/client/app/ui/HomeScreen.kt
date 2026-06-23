package flare.client.app.ui

import flare.client.app.ui.i18n.I18n

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.data.model.ProfileSummary
import flare.client.app.data.model.SubscriptionEntity
import flare.client.app.ui.components.*
import flare.client.app.ui.viewmodel.VpnViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import flare.client.app.ui.theme.FlareTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp


@Composable
fun HomeScreen(
    connectionState: flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState,
    profiles: List<DisplayItem>,
    chainedProfileIds: List<Long> = emptyList(),
    onProfileChainToggle: (ProfileSummary) -> Unit = {},
    isClipboardLoading: Boolean,
    isAnySubscriptionExpanded: Boolean,
    accentColor: Int,
    pingStyle: String,
    isGradientEnabled: Boolean,
    backgroundType: Int = 1,
    isAnimationEnabled: Boolean,
    animationSpeed: Float,
    isCustomColorEnabled: Boolean = false,
    isChangeLaunchButtonColorEnabled: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    onConnectClick: () -> Unit,
    onProfileClick: (ProfileSummary) -> Unit,
    onProfileDelete: (ProfileSummary) -> Unit,
    onShareProfile: (ProfileSummary) -> Unit,
    onQrProfile: (ProfileSummary) -> Unit,
    onEditProfileJson: (ProfileSummary) -> Unit,
    onEditProfileSimple: (ProfileSummary) -> Unit,
    onSubscriptionToggle: (SubscriptionEntity) -> Unit,
    onSubscriptionDelete: (Long) -> Unit,
    onSubscriptionSpeedTest: (Long) -> Unit,
    onSubscriptionUpdate: (SubscriptionEntity) -> Unit,
    onEditSubscriptionJson: (SubscriptionEntity) -> Unit,
    onSubscriptionPinToggle: (SubscriptionEntity) -> Unit,
    onSubscriptionShare: (SubscriptionEntity) -> Unit,
    onSubscriptionQr: (SubscriptionEntity) -> Unit,
    onClipboardClick: () -> Unit,
    onManualInputClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onBack: () -> Unit,
    onScroll: (Int) -> Unit,
    hazeState: HazeState
) {
    BackHandler(enabled = isAnySubscriptionExpanded) {
        onBack()
    }
    val isConnected = connectionState == flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTED
    val isConnecting = connectionState == flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING || connectionState == flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTING

    
    val coroutineScope = rememberCoroutineScope()
    var isScrollingDown by remember { mutableStateOf(true) }
    val animatedTopPadding by animateDpAsState(
        targetValue = if (isAnySubscriptionExpanded) 4.dp else 11.dp,
        label = "listTopPadding"
    )

    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    val shouldShowButton = (isScrollingDown && canScrollForward && firstVisibleItemIndex > 1) ||
                           (!isScrollingDown && canScrollBackward)

    var lastIndex by remember { mutableStateOf(0) }
    var lastOffset by remember { mutableStateOf(0) }
    
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val dy = if (index != lastIndex) (index - lastIndex) * 100 else offset - lastOffset
                onScroll(dy)
                if (dy > 0) {
                    isScrollingDown = true
                } else if (dy < 0) {
                    isScrollingDown = false
                }
                lastIndex = index
                lastOffset = offset
            }
    }

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val buttonSize = if (isLandscape) 220.dp else 290.dp
    val buttonOffsetY = if (isLandscape) 10.dp else 40.dp
    val addProfilesBottomPadding = if (isLandscape) 24.dp else 96.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1.1f)
                            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .let { if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) it.hazeSource(state = hazeState) else it },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isAnySubscriptionExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 20.dp, top = 0.dp, bottom = 2.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = onBack
                                            )
                                    ) {
                                        Text(
                                            text = I18n.strings.collapse_all,
                                            color = Color(accentColor),
                                            fontSize = 12.sp,
                                            fontFamily = flare.client.app.ui.components.GeologicaMedium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            painter = painterResource(R.drawable.ic_arrow_up),
                                            contentDescription = null,
                                            tint = Color(accentColor),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = animatedTopPadding,
                                        bottom = 0.dp
                                    )
                            ) {
                                if (profiles.isEmpty()) {
                                    Text(
                                        text = I18n.strings.empty_profiles_hint,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 16.sp,
                                        fontFamily = flare.client.app.ui.components.GeologicaMedium,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = 32.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                } else {
                                    ProfileList(
                                        items = profiles,
                                        accentColor = Color(accentColor),
                                        pingStyle = pingStyle,
                                        listState = listState,
                                        chainedProfileIds = chainedProfileIds,
                                        onProfileChainToggle = onProfileChainToggle,
                                        onProfileClick = onProfileClick,
                                        onProfileDelete = onProfileDelete,
                                        onShareProfile = onShareProfile,
                                        onQrProfile = onQrProfile,
                                        onEditProfileJson = onEditProfileJson,
                                        onEditProfileSimple = onEditProfileSimple,
                                        onSubscriptionToggle = onSubscriptionToggle,
                                        onSubscriptionDelete = onSubscriptionDelete,
                                        onSubscriptionSpeedTest = onSubscriptionSpeedTest,
                                        onSubscriptionUpdate = onSubscriptionUpdate,
                                        onEditSubscriptionJson = onEditSubscriptionJson,
                                        onSubscriptionPinToggle = onSubscriptionPinToggle,
                                        onSubscriptionQr = onSubscriptionQr,
                                        onSubscriptionShare = onSubscriptionShare,
                                        hazeState = hazeState
                                    )
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = shouldShowButton,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 24.dp, bottom = 24.dp)
                        ) {
                            val isDarkTheme = FlareTheme.colors.isDark
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .bottomNavSoftShadow(isDarkTheme, cornersRadius = 20.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        coroutineScope.launch {
                                            if (isScrollingDown) {
                                                if (profiles.isNotEmpty()) {
                                                    listState.animateScrollToItem(profiles.lastIndex)
                                                }
                                            } else {
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(1.dp)
                                        .flareGlass(
                                            isDark = isDarkTheme,
                                            radius = 20f,
                                            intensity = 1.6f,
                                            index = 1.5f,
                                            glassHeight = 0.5f,
                                            thickness = 5f,
                                            hasOutline = false
                                        )
                                        .background(if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) androidx.compose.ui.graphics.Color.Transparent else flare.client.app.ui.theme.FlareTheme.colors.bgItem.copy(alpha = 0.95f)).hazeEffect(state = hazeState) {
                                            blurRadius = 2.5.dp
                                        }
                                        .background(
                                            color = if (isDarkTheme) Color(0xA0202228) else Color(0xA0FFFFFF),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = FlareTheme.colors.glassStroke,
                                            shape = CircleShape
                                        )
                                )

                                Icon(
                                    painter = painterResource(
                                        id = if (isScrollingDown) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up
                                    ),
                                    contentDescription = null,
                                    tint = FlareTheme.colors.navIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.9f)
                            .padding(start = 8.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        
                        StatusIndicatorRow(
                            connectionState = connectionState,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        
                        FlareFireworkButton(
                            connectionState = connectionState,
                            buttonSize = buttonSize,
                            onClick = {
                                if (connectionState != flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING &&
                                    connectionState != flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTING
                                ) {
                                    onConnectClick()
                                }
                            },
                            backgroundType = backgroundType,
                            isCustomColorEnabled = isCustomColorEnabled,
                            isChangeLaunchButtonColorEnabled = isChangeLaunchButtonColorEnabled
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        
                        if (!isAnySubscriptionExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = I18n.strings.label_add_profiles,
                                    color = FlareTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (profiles.isEmpty()) {
                                    Text(
                                        text = I18n.strings.hint_add_first_profile,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 13.sp,
                                        fontFamily = flare.client.app.ui.components.GeologicaRegular,
                                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }

                                FlareClipboardButton(
                                    isLoading = isClipboardLoading,
                                    onClick = onClipboardClick,
                                    onManualInputClick = onManualInputClick,
                                    onQrScanClick = onQrScanClick,
                                    onImportFileClick = onImportFileClick,
                                    hazeState = hazeState,
                                    accentColor = Color(accentColor),
                                    isCustomColorEnabled = isCustomColorEnabled
                                )
                            }
                        }
                    }
                }
            } else {
                val guidelineHeight = if (isLandscape) screenHeight * 0.35f else screenHeight * 0.38f

                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(guidelineHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    StatusIndicatorRow(
                        connectionState = connectionState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 8.dp)
                    )

                    FlareFireworkButton(
                        connectionState = connectionState,
                        buttonSize = buttonSize,
                        onClick = {
                            if (connectionState != flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING &&
                                connectionState != flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTING
                            ) {
                                onConnectClick()
                            }
                        },
                        backgroundType = backgroundType,
                        isCustomColorEnabled = isCustomColorEnabled,
                        isChangeLaunchButtonColorEnabled = isChangeLaunchButtonColorEnabled,
                        modifier = Modifier.offset(y = buttonOffsetY)
                    )
                }

                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = guidelineHeight)
                        .offset(y = 1.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .let { if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) it.hazeSource(state = hazeState) else it }, 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = isAnySubscriptionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 20.dp, top = 0.dp, bottom = 2.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onBack
                                    )
                            ) {
                                Text(
                                    text = I18n.strings.collapse_all,
                                    color = Color(accentColor),
                                    fontSize = 12.sp,
                                    fontFamily = flare.client.app.ui.components.GeologicaMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_up),
                                    contentDescription = null,
                                    tint = Color(accentColor),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp, 
                                end = 16.dp, 
                                top = animatedTopPadding, 
                                bottom = 0.dp
                            )
                    ) {
                        if (profiles.isEmpty()) {
                            Text(
                                text = I18n.strings.empty_profiles_hint,
                                color = FlareTheme.colors.textSecondary,
                                fontSize = 16.sp,
                                fontFamily = flare.client.app.ui.components.GeologicaMedium,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            ProfileList(
                                items = profiles,
                                accentColor = Color(accentColor),
                                pingStyle = pingStyle,
                                listState = listState,
                                chainedProfileIds = chainedProfileIds,
                                onProfileChainToggle = onProfileChainToggle,
                                onProfileClick = onProfileClick,
                                onProfileDelete = onProfileDelete,
                                onShareProfile = onShareProfile,
                                onQrProfile = onQrProfile,
                                onEditProfileJson = onEditProfileJson,
                                onEditProfileSimple = onEditProfileSimple,
                                onSubscriptionToggle = onSubscriptionToggle,
                                onSubscriptionDelete = onSubscriptionDelete,
                                onSubscriptionSpeedTest = onSubscriptionSpeedTest,
                                onSubscriptionUpdate = onSubscriptionUpdate,
                                onEditSubscriptionJson = onEditSubscriptionJson,
                                onSubscriptionPinToggle = onSubscriptionPinToggle,
                                onSubscriptionQr = onSubscriptionQr,
                                onSubscriptionShare = onSubscriptionShare,
                                hazeState = hazeState
                            )
                        }
                    }

                    if (!isAnySubscriptionExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = addProfilesBottomPadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = I18n.strings.label_add_profiles,
                                color = FlareTheme.colors.textSecondary,
                                fontSize = 13.sp,
                                fontFamily = flare.client.app.ui.components.GeologicaRegular,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (profiles.isEmpty()) {
                                Text(
                                    text = I18n.strings.hint_add_first_profile,
                                    color = FlareTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontFamily = flare.client.app.ui.components.GeologicaRegular,
                                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            FlareClipboardButton(
                                isLoading = isClipboardLoading,
                                onClick = onClipboardClick,
                                onManualInputClick = onManualInputClick,
                                onQrScanClick = onQrScanClick,
                                onImportFileClick = onImportFileClick,
                                hazeState = hazeState,
                                accentColor = Color(accentColor),
                                isCustomColorEnabled = isCustomColorEnabled
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = shouldShowButton,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = if (isLandscape) 24.dp else 96.dp)
                ) {
                    val isDarkTheme = FlareTheme.colors.isDark
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bottomNavSoftShadow(isDarkTheme, cornersRadius = 20.dp)
                            .clip(CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    if (isScrollingDown) {
                                        if (profiles.isNotEmpty()) {
                                            listState.animateScrollToItem(profiles.lastIndex)
                                        }
                                    } else {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(1.dp)
                                .flareGlass(
                                    isDark = isDarkTheme,
                                    radius = 20f,
                                    intensity = 1.6f,
                                    index = 1.5f,
                                    glassHeight = 0.5f,
                                    thickness = 5f,
                                    hasOutline = false
                                )
                                .background(if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) androidx.compose.ui.graphics.Color.Transparent else flare.client.app.ui.theme.FlareTheme.colors.bgItem.copy(alpha = 0.95f)).hazeEffect(state = hazeState) {
                                    blurRadius = 2.5.dp
                                }
                                .background(
                                    color = if (isDarkTheme) Color(0xA0202228) else Color(0xA0FFFFFF),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = FlareTheme.colors.glassStroke,
                                    shape = CircleShape
                                )
                        )

                        Icon(
                            painter = painterResource(
                                id = if (isScrollingDown) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up
                            ),
                            contentDescription = null,
                            tint = FlareTheme.colors.navIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorRow(
    connectionState: flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState,
    modifier: Modifier = Modifier
) {
    val isDark = FlareTheme.colors.isDark
    
    val baseColor = when (connectionState) {
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTED -> Color(0xFF34C759)
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTED -> Color(0xFF8E8E93)
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING -> if (isDark) Color(0xFF4A4A4A) else Color(0xFFBBBBBB)
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTING -> if (isDark) Color(0xFF4A4A4A) else Color(0xFFBBBBBB)
    }

    val animatedBaseColor by animateColorAsState(
        targetValue = baseColor,
        animationSpec = tween(durationMillis = 500),
        label = "indicatorBaseColor"
    )

    
    val isPulsing = connectionState == flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING ||
            connectionState == flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseProgress"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val overlayActiveProgress by animateFloatAsState(
        targetValue = if (isPulsing) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "overlayActive"
    )

    val statusText = when (connectionState) {
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTED -> I18n.strings.status_connected
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTED -> I18n.strings.status_disconnected
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING -> I18n.strings.status_connecting
        flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTING -> I18n.strings.status_disconnecting
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        
        Box(
            modifier = Modifier
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val dotRadius = 4.dp.toPx() 
                val glowRadius = 8.dp.toPx() 

                
                val auraColor = when (connectionState) {
                    flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTED -> Color(0xFF34C759)
                    flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING -> Color(0xFF34C759)
                    flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTING -> Color(0xFFFF3B30)
                    else -> Color.Transparent
                }

                if (connectionState != flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.DISCONNECTED) {
                    val currentPulse = if (isPulsing) pulseProgress else 1f
                    drawCircle(
                        color = auraColor,
                        radius = glowRadius * (0.85f + 0.15f * currentPulse),
                        center = center,
                        alpha = 0.25f * (if (isPulsing) currentPulse else 0.7f)
                    )
                }

                
                drawCircle(
                    color = animatedBaseColor,
                    radius = dotRadius,
                    center = center
                )

                
                if (overlayActiveProgress > 0f) {
                    val overlayColor = if (connectionState == flare.client.app.ui.viewmodel.VpnViewModel.ConnectionState.CONNECTING) {
                        Color(0xFF34C759)
                    } else {
                        Color(0xFFFF3B30)
                    }
                    drawCircle(
                        color = overlayColor,
                        radius = dotRadius,
                        center = center,
                        alpha = pulseProgress * overlayActiveProgress
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        
        AnimatedContent(
            targetState = statusText,
            transitionSpec = {
                (fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { -it / 2 }) togetherWith
                (fadeOut(animationSpec = tween(350)) + slideOutVertically(animationSpec = tween(350)) { it / 2 })
            },
            label = "statusTextTransition"
        ) { text ->
            Text(
                text = text,
                fontFamily = flare.client.app.ui.components.GeologicaMedium,
                fontSize = 15.sp,
                color = FlareTheme.colors.textPrimary
            )
        }
    }
}
