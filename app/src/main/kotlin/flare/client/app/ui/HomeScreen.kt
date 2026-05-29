package flare.client.app.ui

import flare.client.app.ui.i18n.I18n

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import flare.client.app.ui.MainViewModel
import flare.client.app.ui.components.RollingTimer
import androidx.compose.foundation.isSystemInDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import flare.client.app.ui.theme.FlareTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.res.painterResource


@Composable
fun HomeScreen(
    connectionState: MainViewModel.ConnectionState,
    timerText: String,
    profiles: List<DisplayItem>,
    chainedProfileIds: List<Long> = emptyList(),
    onProfileChainToggle: (ProfileSummary) -> Unit = {},
    isClipboardLoading: Boolean,
    isAnySubscriptionExpanded: Boolean,
    accentColor: Int,
    pingStyle: String,
    isGradientEnabled: Boolean,
    isAnimationEnabled: Boolean,
    animationSpeed: Float,
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
    val isConnected = connectionState == MainViewModel.ConnectionState.CONNECTED
    val isConnecting = connectionState == MainViewModel.ConnectionState.CONNECTING || connectionState == MainViewModel.ConnectionState.DISCONNECTING

    
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
    val buttonSize = if (isLandscape) 170.dp else 300.dp
    val buttonOffsetY = if (isLandscape) 10.dp else 50.dp
    val addProfilesBottomPadding = if (isLandscape) 24.dp else 130.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            val guidelineHeight = if (isLandscape) screenHeight * 0.35f else screenHeight * 0.38f

            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(guidelineHeight),
                contentAlignment = Alignment.BottomCenter
            ) {
                FlareConnectButton(
                    connectionState = connectionState,
                    buttonSize = buttonSize,
                    onClick = {
                        if (connectionState != MainViewModel.ConnectionState.CONNECTING &&
                            connectionState != MainViewModel.ConnectionState.DISCONNECTING
                        ) {
                            onConnectClick()
                        }
                    },
                    modifier = Modifier.offset(y = buttonOffsetY)
                )
            }

            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = guidelineHeight)
                    .offset(y = 13.dp)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .hazeSource(state = hazeState), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                val containerAlpha by animateFloatAsState(
                    targetValue = if (isConnected || isConnecting) 1f else 0f,
                    animationSpec = tween(400),
                    label = "containerAlpha"
                )

                val timerContentAlpha by animateFloatAsState(
                    targetValue = if (isConnected) 1f else 0f,
                    animationSpec = tween(300),
                    label = "timerContentAlpha"
                )

                val loadingContentAlpha by animateFloatAsState(
                    targetValue = if (isConnecting) 1f else 0f,
                    animationSpec = tween(300),
                    label = "loadingContentAlpha"
                )

                if (containerAlpha > 0f) {
                    val timerColor = if (FlareTheme.colors.isDark) Color(0xFFE2E5EC) else Color(0xFF1A1C1E)
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .alpha(containerAlpha),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loadingContentAlpha > 0f) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .alpha(loadingContentAlpha),
                                color = timerColor,
                                strokeWidth = 2.dp
                            )
                        }
                        if (timerContentAlpha > 0f) {
                            RollingTimer(
                                time = timerText,
                                color = timerColor,
                                fontSize = 17.sp,
                                modifier = Modifier
                                    .alpha(timerContentAlpha)
                            )
                        }
                    }
                }

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
                            accentColor = Color(accentColor)
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
                    .padding(end = 24.dp, bottom = if (isLandscape) 24.dp else 104.dp)
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
                            .hazeEffect(state = hazeState) {
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
