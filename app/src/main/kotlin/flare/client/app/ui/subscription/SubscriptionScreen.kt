package flare.client.app.ui.subscription

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import flare.client.app.R
import flare.client.app.data.api.Device
import flare.client.app.data.api.KeyInfo
import flare.client.app.data.auth.AuthManager
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.FlareButton
import flare.client.app.ui.components.FlareCard
import flare.client.app.ui.components.FlareGlassMenu
import flare.client.app.ui.components.FlareSubScreenTopBar
import flare.client.app.ui.components.GeologicaMedium
import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.notification.NotificationType
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.util.GlassUtils
import flare.client.app.util.QrUtils
import flare.client.app.ui.components.dialogs.GlassDialog
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    authManager: AuthManager,
    hazeState: HazeState,
    viewModel: SubscriptionViewModel,
    onImportSubscription: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoggedIn by remember { mutableStateOf(authManager.isLoggedIn()) }
    var isPolling by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    
    
    val state by viewModel.state.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }
    
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var showTopUpMenu by remember { mutableStateOf(false) }

    val paymentState by viewModel.paymentState.collectAsState()
    
    LaunchedEffect(actionError) {
        if (actionError != null) {
            AppNotificationManager.showNotification(
                type = NotificationType.ERROR,
                text = actionError!!,
                durationSec = 4
            )
            viewModel.clearActionError()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.refresh()
        }
    }

    val scrollState = rememberScrollState()
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    val pullRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .let { if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) it.hazeSource(state = hazeState) else it }
        ) {
            @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh(force = true) },
                modifier = Modifier.fillMaxSize(),
                state = pullRefreshState,
                indicator = {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                        isRefreshing = isRefreshing,
                        state = pullRefreshState,
                        color = FlareTheme.colors.accent,
                        containerColor = FlareTheme.colors.bgSurface
                    )
                }
            ) {
                if (isLoggedIn && state is SubscriptionState.Loading) {
                    SubscriptionSkeleton()
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .verticalScroll(scrollState)
                            .statusBarsPadding()
                            .padding(top = 80.dp, bottom = 160.dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    AnimatedContent(
                        targetState = isLoggedIn,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300)))
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                            }
                        },
                        label = "loginTransition"
                    ) { loggedIn ->
                        if (!loggedIn) {
                            AuthFlowSection(
                                authManager = authManager,
                                onAuthSuccess = { isLoggedIn = true }
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (val currentState = state) {
                            is SubscriptionState.Loading -> {
                                
                            }
                            is SubscriptionState.Error -> {
                                Text(
                                    text = currentState.message,
                                    color = Color(0xFFFF3B30),
                                    fontFamily = GeologicaMedium,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                KeyActionChip(
                                    icon = R.drawable.ic_refresh,
                                    label = I18n.strings.sub_refresh_btn,
                                    accentColor = FlareTheme.colors.accent,
                                    onClick = { viewModel.refresh() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LogoutButton(authManager) { isLoggedIn = false }
                            }
                            is SubscriptionState.NotLoggedIn -> {
                                if (!authManager.isLoggedIn()) {
                                    isLoggedIn = false
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = FlareTheme.colors.accent)
                                    }
                                }
                            }
                            is SubscriptionState.Success -> {
                                val info = currentState.info
                                val keys = info.keys ?: emptyList()
                                val activeKeysCount = keys.count { it.status == "active" }
                                val expiredKeysCount = keys.count { it.status != "active" }
                                val premiumDays = keys.filter { it.status == "active" }.sumOf { key ->
                                    try {
                                        val formatIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }
                                        val date = formatIn.parse(key.expires_at)
                                        if (date != null) {
                                            val diff = date.time - System.currentTimeMillis()
                                            val days = diff / (1000 * 60 * 60 * 24)
                                            if (days > 0) days.toInt() else 0
                                        } else 0
                                    } catch(e: Exception) { 0 }
                                }
                                val totalUsedTraffic = keys.sumOf { it.used_traffic ?: 0L }
                                val trafficGb = String.format(Locale.US, "%.1f GB", totalUsedTraffic / (1024.0 * 1024.0 * 1024.0))

                                Text(
                                    text = I18n.strings.sub_welcome,
                                    color = FlareTheme.colors.textPrimary,
                                    fontFamily = GeologicaMedium,
                                    fontSize = 28.sp,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Brush.verticalGradient(
                                            colors = listOf(
                                                FlareTheme.colors.accent.copy(alpha = 0.5f),
                                                FlareTheme.colors.accent
                                            )
                                        ))
                                        .padding(20.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text(
                                                    text = I18n.strings.sub_keys_header,
                                                    color = Color.Black.copy(alpha = 0.7f),
                                                    fontFamily = GeologicaMedium,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(I18n.strings.sub_active_count.format(activeKeysCount), fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f))
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black.copy(alpha=0.3f)))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(I18n.strings.sub_expired_count.format(expiredKeysCount), fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f))
                                                }
                                            }
                                            Text(
                                                text = "FlareVPN",
                                                color = Color.Black.copy(alpha = 0.7f),
                                                fontFamily = GeologicaMedium,
                                                fontSize = 12.sp
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        
                                        Text(
                                            text = I18n.strings.sub_plan_premium,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            fontFamily = GeologicaRegular,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = I18n.strings.sub_days_left.format(premiumDays),
                                            color = Color.Black,
                                            fontFamily = GeologicaMedium,
                                            fontSize = 36.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column {
                                                Text(
                                                    text = I18n.strings.sub_traffic_label,
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    fontFamily = GeologicaRegular,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = trafficGb,
                                                    color = Color.Black,
                                                    fontFamily = GeologicaMedium,
                                                    fontSize = 28.sp
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(modifier = Modifier.width(14.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(Color.Black))
                                                Box(modifier = Modifier.width(14.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).border(1.dp, Color.Black, RoundedCornerShape(2.dp)))
                                                Box(modifier = Modifier.width(18.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(I18n.strings.sub_sparks_label, color = Color.Black.copy(alpha = 0.7f), fontSize = 14.sp)
                                                Text("${info.balance}", color = Color.Black, fontSize = 14.sp, fontFamily = GeologicaMedium)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(I18n.strings.sub_total_keys_label, color = Color.Black.copy(alpha = 0.7f), fontSize = 14.sp)
                                                Text("${keys.size}", color = Color.Black, fontSize = 14.sp, fontFamily = GeologicaMedium)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            val interactionSource = remember { MutableInteractionSource() }
                                            val isPressed by interactionSource.collectIsPressedAsState()
                                            val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "btnScale")
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(56.dp)
                                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                                    .clip(RoundedCornerShape(28.dp))
                                                    .background(Color.Black)
                                                    .clickable(interactionSource = interactionSource, indication = null) {
                                                        if (authManager.isAnonymousSession()) {
                                                            showTopUpDialog = true
                                                        } else {
                                                            showTopUpMenu = true
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(I18n.strings.sub_connect_btn_text, color = Color.White, fontFamily = GeologicaMedium, fontSize = 14.sp)
                                                
                                                FlareGlassMenu(
                                                    expanded = showTopUpMenu,
                                                    onDismissRequest = { showTopUpMenu = false },
                                                    items = listOf(
                                                        GlassUtils.MenuItem(0, I18n.strings.sub_topup_telegram) {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("tg://resolve?domain=${flare.client.app.data.api.FlareBackendApi.BOT_USERNAME}&start=renew"))
                                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            context.startActivity(intent)
                                                        },
                                                        GlassUtils.MenuItem(1, I18n.strings.sub_topup_app) {
                                                            showTopUpDialog = true
                                                        }
                                                    ),
                                                    hazeState = hazeState,
                                                    alignment = Alignment.TopCenter,
                                                    offset = androidx.compose.ui.unit.IntOffset(0, 0)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                KeysSectionHeader(
                                    title = I18n.strings.sub_keys_section,
                                    onAddClick = { showAddKeyDialog = true }
                                )
                                val lineColor = if (FlareTheme.colors.isDark) Color.White.copy(alpha=0.15f) else Color.Black.copy(alpha=0.15f)
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(lineColor))
                                
                                if (keys.isEmpty()) {
                                    Text(
                                        text = I18n.strings.sub_no_active_keys,
                                        color = FlareTheme.colors.textPrimary,
                                        fontFamily = GeologicaMedium,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(vertical = 24.dp)
                                    )
                                } else {
                                    val keysScrollState = rememberScrollState()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 400.dp)
                                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                            .drawWithContent {
                                                val topAlpha = (keysScrollState.value / 50f).coerceIn(0f, 1f)
                                                val bottomAlpha = if (keysScrollState.maxValue > 0) {
                                                    ((keysScrollState.maxValue - keysScrollState.value) / 50f).coerceIn(0f, 1f)
                                                } else 0f
                                                
                                                val topFade = Color.Black.copy(alpha = 1f - (topAlpha * 0.6f))
                                                val bottomFade = Color.Black.copy(alpha = 1f - (bottomAlpha * 0.6f))
                                                
                                                drawContent()
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        0f to topFade,
                                                        0.02f to Color.Black,
                                                        0.98f to Color.Black,
                                                        1f to bottomFade
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                            .verticalScroll(keysScrollState),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Spacer(modifier = Modifier.height(0.dp))
                                        keys.forEachIndexed { index, keyInfo ->
                                            KeyCard(
                                                index = index,
                                                keyInfo = keyInfo,
                                                hazeState = hazeState,
                                                onCopyLink = { link ->
                                                    clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("Link", link))
                                                    AppNotificationManager.showNotification(
                                                        type = NotificationType.SUCCESS,
                                                        text = I18n.strings.sub_link_copied,
                                                        durationSec = 3
                                                    )
                                                },
                                                onRevokeKey = { viewModel.revokeKey(keyInfo.id) },
                                                onDeleteKey = { viewModel.deleteKey(keyInfo.id) },
                                                onRenameKey = { newName -> viewModel.renameKey(keyInfo.id, newName) },
                                                onRemoveDevice = { viewModel.removeDevice(it) }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(0.dp))
                                    }
                                }
                                
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(lineColor))
                                Spacer(modifier = Modifier.height(32.dp))
    
                                if (isActionLoading || isPolling) {
                                    CircularProgressIndicator(color = FlareTheme.colors.accent, modifier = Modifier.padding(16.dp))
                                } else {
                                    if (authManager.isAnonymousSession()) {
                                        BindTelegramButton(onClick = {
                                            isPolling = true
                                            coroutineScope.launch {
                                                val uuid = authManager.startAuthFlow()
                                                if (uuid != null) {
                                                    val res = authManager.pollForToken(uuid, isBind = true)
                                                    if (res == 0) {
                                                        viewModel.refresh()
                                                    } else if (res == 2) {
                                                        AppNotificationManager.showNotification(
                                                            type = NotificationType.ERROR,
                                                            text = I18n.strings.sub_bind_err_already_linked,
                                                            durationSec = 4
                                                        )
                                                    }
                                                }
                                                isPolling = false
                                            }
                                        })
                                    }
                                    LogoutButton(authManager) { isLoggedIn = false }
                                }
                            }
                        }
                    }
                } 
            } 
        }
        }
        }
        }
        }
        
        FlareSubScreenTopBar(
            title = I18n.strings.sub_manage_title,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBackClick
        )
        
        if (showAddKeyDialog) {
            val currentBalance = (state as? SubscriptionState.Success)?.info?.balance ?: 0
            flare.client.app.ui.components.dialogs.GlassDialog(
                onDismissRequest = { showAddKeyDialog = false },
                hazeState = hazeState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 20.dp, end = 20.dp)
                ) {
                    Text(
                        text = I18n.strings.sub_add_key_dialog_title,
                        color = FlareTheme.colors.textPrimary,
                        fontFamily = GeologicaMedium,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = I18n.strings.sub_add_key_dialog_desc.format(currentBalance),
                        color = FlareTheme.colors.textSecondary,
                        fontFamily = GeologicaRegular,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(FlareTheme.colors.dividerColor)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(bounded = true, color = FlareTheme.colors.textSecondary),
                                    onClick = { showAddKeyDialog = false }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = I18n.strings.btn_cancel,
                                color = FlareTheme.colors.textSecondary,
                                fontFamily = GeologicaMedium,
                                fontSize = 15.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(FlareTheme.colors.dividerColor)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(bounded = true, color = FlareTheme.colors.accent),
                                    onClick = {
                                        showAddKeyDialog = false
                                        if (currentBalance < 100) {
                                            AppNotificationManager.showNotification(
                                                type = NotificationType.ERROR,
                                                text = I18n.strings.sub_err_insufficient_sparks.format(currentBalance),
                                                durationSec = 4
                                            )
                                        } else {
                                            viewModel.createKey {
                                                
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = I18n.strings.btn_add,
                                color = if (currentBalance >= 100) FlareTheme.colors.accent else FlareTheme.colors.textSecondary,
                                fontFamily = GeologicaMedium,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        if (showTopUpDialog) {
            TopUpDialog(
                viewModel = viewModel,
                onDismiss = {
                    showTopUpDialog = false
                    viewModel.resetPaymentState()
                },
                hazeState = hazeState
            )
        }
    }

@Composable
fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontFamily = GeologicaMedium,
            color = FlareTheme.colors.textPrimary
        )
        if (action != null) {
            action()
        }
    }
}

@Composable
fun KeysSectionHeader(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontFamily = GeologicaMedium,
                color = FlareTheme.colors.textPrimary
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FlareTheme.colors.accent.copy(alpha = 0.15f))
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = FlareTheme.colors.accent,
                    fontFamily = GeologicaMedium,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun KeyCard(
    index: Int,
    keyInfo: KeyInfo,
    hazeState: HazeState?,
    onCopyLink: (String) -> Unit,
    onRevokeKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onRenameKey: (String) -> Unit,
    onRemoveDevice: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isDevicesExpanded by remember { mutableStateOf(false) }
    
    val isDark = FlareTheme.colors.isDark
    val borderColor = if (isDark) FlareTheme.colors.accent.copy(alpha = 0.3f) else FlareTheme.colors.accent.copy(alpha = 0.15f)
    val bgColor = if (isDark) Color.White.copy(alpha = 0.03f) else FlareTheme.colors.accent.copy(alpha = 0.03f)

    FlareCard(
        cornerType = DisplayItem.CornerType.ALL,
        paddingHorizontal = 20.dp,
        paddingVertical = 20.dp,
        cornerRadius = 24.dp,
        backgroundColor = bgColor,
        borderColor = borderColor,
        borderWidth = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = keyInfo.name ?: I18n.strings.sub_key_title.format(index + 1),
                        color = FlareTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontFamily = GeologicaMedium
                    )
                    Spacer(modifier = Modifier.width(1.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = I18n.strings.sub_key_rename,
                            tint = FlareTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        FlareGlassMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            items = listOf(
                                GlassUtils.MenuItem(0, I18n.strings.sub_key_rename) {
                                    showRenameDialog = true
                                },
                                GlassUtils.MenuItem(1, I18n.strings.sub_key_delete) {
                                    onDeleteKey()
                                }
                            ),
                            hazeState = hazeState,
                            alignment = Alignment.TopStart,
                            offset = androidx.compose.ui.unit.IntOffset(0, 0)
                        )
                    }
                }
                val isActive = keyInfo.status == "active"
                val statusColor = if (isActive) Color(0xFF34C759) else Color(0xFFFF3B30)
                val statusBg = statusColor.copy(alpha = 0.12f)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isActive) I18n.strings.sub_key_status_active else I18n.strings.sub_key_status_inactive,
                        color = statusColor,
                        fontFamily = GeologicaMedium,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            
            val isActive = keyInfo.status == "active"
            Text(
                I18n.strings.sub_key_expires.format(formatIsoDate(keyInfo.expires_at)),
                color = FlareTheme.colors.textSecondary,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp
            )
            val addedCount = keyInfo.devices?.size ?: 0
            val limit = keyInfo.ip_limit ?: 5
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = I18n.strings.sub_devices_limit.format(addedCount, limit),
                color = FlareTheme.colors.textSecondary,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp
            )

            
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FlareTheme.colors.dividerColor)
            )
            Spacer(modifier = Modifier.height(14.dp))

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (keyInfo.sub_link != null) {
                    val mainBtnBg = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black
                    KeyActionChip(
                        icon = R.drawable.ic_copy,
                        label = I18n.strings.sub_key_copy,
                        accentColor = mainBtnBg,
                        contentColor = Color.White,
                        onClick = { onCopyLink(keyInfo.sub_link) },
                        modifier = Modifier.weight(1f)
                    )
                }
                KeyActionChip(
                    icon = R.drawable.ic_refresh,
                    label = I18n.strings.sub_key_reissue,
                    accentColor = Color(0xFFFF3B30),
                    contentColor = Color.White,
                    onClick = onRevokeKey,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Column(modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(dampingRatio = 0.9f, stiffness = 1600f))) {
                AnimatedVisibility(
                    visible = isDevicesExpanded,
                    enter = expandVertically(animationSpec = tween(180)) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(140)) + fadeOut(animationSpec = tween(100))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val devices = keyInfo.devices ?: emptyList()
                        if (devices.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = I18n.strings.sub_no_devices_text,
                                    color = FlareTheme.colors.textSecondary,
                                    fontFamily = GeologicaMedium,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            devices.forEach { device ->
                                DeviceCard(
                                    device = device,
                                    keyIndex = index,
                                    onRemove = { onRemoveDevice(device.hwid) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                AnimatedContent(
                    targetState = isDevicesExpanded,
                    transitionSpec = {
                        if (targetState) {
                            fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(120))
                        } else {
                            fadeIn(animationSpec = tween(120, delayMillis = 100)) togetherWith fadeOut(animationSpec = tween(60))
                        }
                    },
                    label = "handleToButton"
                ) { expanded ->
                    if (expanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            if (dragAmount < -5f) {
                                                isDevicesExpanded = false
                                            }
                                        }
                                    )
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { isDevicesExpanded = false }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(FlareTheme.colors.textSecondary.copy(alpha = 0.3f))
                            )
                        }
                    } else {
                        val btnBg = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
                        val btnContent = if (FlareTheme.colors.isDark) Color.White else Color.Black
                        KeyActionChip(
                            icon = R.drawable.ic_arrow_down,
                            label = I18n.strings.sub_devices_section,
                            accentColor = btnBg,
                            contentColor = btnContent,
                            onClick = { isDevicesExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        val currentKeyName = keyInfo.name ?: I18n.strings.sub_key_title.format(index + 1)
        var newName by remember(showRenameDialog) { mutableStateOf(keyInfo.name ?: "") }
        flare.client.app.ui.components.dialogs.GlassDialog(
            onDismissRequest = { showRenameDialog = false },
            hazeState = hazeState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = I18n.strings.sub_rename_dialog_title,
                    color = FlareTheme.colors.textPrimary,
                    fontFamily = GeologicaMedium,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (FlareTheme.colors.isDark) Color.White.copy(alpha=0.05f) else Color.Black.copy(alpha=0.05f))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = FlareTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontFamily = GeologicaRegular
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(FlareTheme.colors.accent),
                        decorationBox = { innerTextField ->
                            if (newName.isEmpty()) {
                                Text(
                                    text = currentKeyName,
                                    color = FlareTheme.colors.textSecondary,
                                    fontSize = 16.sp,
                                    fontFamily = GeologicaRegular
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(FlareTheme.colors.dividerColor)
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = androidx.compose.material3.ripple(bounded = true, color = FlareTheme.colors.textSecondary),
                                onClick = { showRenameDialog = false }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = I18n.strings.btn_cancel,
                            color = FlareTheme.colors.textSecondary,
                            fontFamily = GeologicaMedium,
                            fontSize = 15.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(FlareTheme.colors.dividerColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = androidx.compose.material3.ripple(bounded = true, color = FlareTheme.colors.accent),
                                onClick = {
                                    showRenameDialog = false
                                    if (newName.isNotBlank()) {
                                        onRenameKey(newName)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = I18n.strings.sub_btn_save,
                            color = FlareTheme.colors.accent,
                            fontFamily = GeologicaMedium,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyActionChip(
    icon: Int,
    label: String,
    accentColor: Color,
    contentColor: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "chipScale"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontFamily = GeologicaMedium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun DeviceCard(device: Device, keyIndex: Int, onRemove: () -> Unit) {
    val osSearchString = listOfNotNull(
        device.os_version,
        device.name,
        device.user_agent,
        device.hwid
    ).joinToString(" ").lowercase()

    val isAndroid = osSearchString.contains("android")
    val isIos = osSearchString.contains("ios") || osSearchString.contains("iphone") || osSearchString.contains("ipad") || osSearchString.contains("mac")
    val isWindows = osSearchString.contains("windows")

    val iconRes = when {
        isAndroid -> R.drawable.ic_android
        isIos -> R.drawable.ic_apple
        isWindows -> R.drawable.ic_windows
        else -> R.drawable.ic_info_i
    }

    val iconBgColor = when {
        isAndroid -> Color(0xFF34C759)
        isIos -> Color(0xFF8E8E93)
        isWindows -> Color.White
        else -> Color(0xFFFF9500)
    }

    val innerCardBg = if (FlareTheme.colors.isDark) Color(0xFF2C2C2E).copy(alpha = 0.4f) else Color(0xFFF2F2F7).copy(alpha = 0.6f)
    val innerCardBorder = if (FlareTheme.colors.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(innerCardBg)
            .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            val tint = if (isWindows) Color.Unspecified else Color.White
            Icon(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                color = FlareTheme.colors.textPrimary,
                fontFamily = GeologicaMedium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = device.os_version ?: I18n.strings.label_unknown,
                color = FlareTheme.colors.textSecondary,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = I18n.strings.sub_device_hwid.format(device.hwid),
                color = FlareTheme.colors.textSecondary.copy(alpha = 0.5f),
                fontFamily = GeologicaRegular,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFF3B30).copy(alpha = 0.1f))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = I18n.strings.sub_device_delete_btn,
                tint = Color(0xFFFF3B30),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun LogoutButton(authManager: AuthManager, onLoggedOut: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = I18n.strings.sub_logout_btn,
            color = Color(0xFFFF3B30).copy(alpha = 0.75f),
            fontFamily = GeologicaMedium,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    authManager.logout()
                    onLoggedOut()
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun BindTelegramButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_telegram),
                contentDescription = null,
                tint = FlareTheme.colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = I18n.strings.sub_bind_telegram_btn,
                color = FlareTheme.colors.accent,
                fontFamily = GeologicaMedium,
                fontSize = 14.sp
            )
        }
    }
}

fun formatIsoDate(isoString: String): String {
    return try {
        val locale = if (I18n.strings == flare.client.app.ui.i18n.RuFlareStrings) Locale.forLanguageTag("ru") else Locale.ENGLISH
        val formatIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", locale)
        formatIn.timeZone = TimeZone.getTimeZone("UTC")
        val date = formatIn.parse(isoString)
        val formatOut = SimpleDateFormat("dd MMMM yyyy", locale)
        date?.let { formatOut.format(it) } ?: isoString
    } catch (e: Exception) {
        isoString
    }
}

@Composable
fun RenewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "renewButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF34C759),
                        Color(0xFF248A3D)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = if (small) 10.dp else 14.dp, horizontal = if (small) 16.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_telegram),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (small) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(if (small) 8.dp else 10.dp))
            Text(
                text = I18n.strings.sub_renew_btn,
                color = Color.White,
                fontFamily = GeologicaMedium,
                fontSize = if (small) 14.sp else 15.sp
            )
        }
    }
}

@Composable
fun SubscriptionSkeleton() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    val color = if (FlareTheme.colors.isDark) Color.White else Color.Black
    val skeletonBrush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = alpha))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 80.dp, bottom = 160.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(skeletonBrush)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
             Row(
                 verticalAlignment = Alignment.CenterVertically,
                 horizontalArrangement = Arrangement.spacedBy(8.dp)
             ) {
                 Box(modifier = Modifier.width(100.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).background(skeletonBrush))
                 Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(skeletonBrush))
             }
        }
        
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(skeletonBrush)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Box(modifier = Modifier.width(140.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).background(skeletonBrush))
        }
        
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
        )
    }
}

@Composable
fun TopUpDialog(
    viewModel: SubscriptionViewModel,
    onDismiss: () -> Unit,
    hazeState: HazeState?
) {
    val paymentState by viewModel.paymentState.collectAsState()
    var step by remember { mutableStateOf(1) } 
    var selectedCoin by remember { mutableStateOf<String?>(null) }
    var selectedQty by remember { mutableStateOf<Int?>(null) }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    
    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Loading || paymentState is PaymentState.Ready || paymentState is PaymentState.Completed) {
            step = 3
        } else if (paymentState is PaymentState.Error) {
            step = 1
        }
    }

    GlassDialog(
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        maxWidthDp = 340
    ) {
        AnimatedContent(
            targetState = if (step < 3) 1 else 2,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
            },
            label = "topup_outer_transition"
        ) { outerStep ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (outerStep == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = I18n.strings.sub_topup_title,
                            color = FlareTheme.colors.textPrimary,
                            fontFamily = GeologicaMedium,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(bounded = true),
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = null,
                                tint = FlareTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = I18n.strings.sub_topup_desc,
                        color = FlareTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = GeologicaRegular,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                                )
                            }
                        },
                        label = "topup_inner_transition"
                    ) { innerStep ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (innerStep == 1) {
                                Text(
                                    text = I18n.strings.sub_topup_select_coin,
                                    color = FlareTheme.colors.textPrimary,
                                    fontFamily = GeologicaMedium,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )
                                CoinCard(
                                    name = "USDT (TRC20)",
                                    description = I18n.strings.sub_coin_usdt_desc,
                                    iconRes = R.drawable.ic_chain,
                                    selected = selectedCoin == "usdt_trc20"
                                ) {
                                    selectedCoin = "usdt_trc20"
                                    selectedQty = null
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                CoinCard(
                                    name = "Litecoin",
                                    description = I18n.strings.sub_coin_ltc_desc,
                                    iconRes = R.drawable.ic_chain,
                                    selected = selectedCoin == "ltc"
                                ) {
                                    selectedCoin = "ltc"
                                    selectedQty = null
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                CoinCard(
                                    name = "Bitcoin",
                                    description = I18n.strings.sub_coin_btc_desc,
                                    iconRes = R.drawable.ic_chain,
                                    selected = selectedCoin == "btc"
                                ) {
                                    selectedCoin = "btc"
                                    selectedQty = null
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                DialogPrimaryButton(
                                    onClick = { if (selectedCoin != null) step = 2 },
                                    text = I18n.strings.sub_btn_next,
                                    enabled = selectedCoin != null
                                )
                            } else {
                                Text(
                                    text = I18n.strings.sub_topup_select_sparks,
                                    color = FlareTheme.colors.textPrimary,
                                    fontFamily = GeologicaMedium,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )
                                
                                val packages = when (selectedCoin) {
                                    "usdt_trc20" -> listOf(
                                        Triple(300, "300 Sparks", I18n.strings.sub_sparks_lite_desc),
                                        Triple(600, "600 Sparks", I18n.strings.sub_sparks_medium_desc),
                                        Triple(900, "900 Sparks", I18n.strings.sub_sparks_max_desc)
                                    )
                                    "btc" -> listOf(
                                        Triple(200, "200 Sparks", I18n.strings.sub_sparks_lite_desc),
                                        Triple(400, "400 Sparks", I18n.strings.sub_sparks_medium_desc),
                                        Triple(850, "850 Sparks", I18n.strings.sub_sparks_max_desc)
                                    )
                                    else -> listOf(
                                        Triple(100, "100 Sparks", I18n.strings.sub_sparks_lite_desc),
                                        Triple(500, "500 Sparks", I18n.strings.sub_sparks_medium_desc),
                                        Triple(1000, "1000 Sparks", I18n.strings.sub_sparks_max_desc)
                                    )
                                }

                                packages.forEachIndexed { index, pkg ->
                                    val (sparksCount, title, desc) = pkg
                                    SparksCard(
                                        name = title,
                                        description = desc,
                                        qty = sparksCount,
                                        selected = selectedQty == sparksCount
                                    ) {
                                        selectedQty = sparksCount
                                    }
                                    if (index < packages.size - 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DialogSecondaryButton(
                                        onClick = { step = 1 },
                                        text = I18n.strings.sub_btn_back,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    DialogPrimaryButton(
                                        onClick = { 
                                            if (selectedCoin != null && selectedQty != null) {
                                                viewModel.createTopupPayment(selectedCoin!!, selectedQty!!)
                                            }
                                        },
                                        text = I18n.strings.sub_btn_pay,
                                        enabled = selectedQty != null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (val state = paymentState) {
                        is PaymentState.Loading -> {
                            CircularProgressIndicator(color = FlareTheme.colors.accent)
                        }
                        is PaymentState.Ready -> {
                            Text(
                                text = I18n.strings.sub_payment_title,
                                color = FlareTheme.colors.textPrimary,
                                fontFamily = GeologicaMedium,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = I18n.strings.sub_payment_desc,
                                color = FlareTheme.colors.textSecondary,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            val qrBitmap = remember(state.addressIn) { QrUtils.generateQrCodeBitmap(state.addressIn) }
                            if (qrBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = I18n.strings.sub_payment_amount.format(state.coinAmount, selectedCoin?.uppercase() ?: ""),
                                color = FlareTheme.colors.textPrimary,
                                fontFamily = GeologicaMedium,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Text(
                                text = state.addressIn,
                                color = FlareTheme.colors.textPrimary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontFamily = GeologicaRegular,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.addressIn))
                                        AppNotificationManager.showNotification(NotificationType.SUCCESS, I18n.strings.sub_payment_address_copied, durationSec = 3)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = I18n.strings.sub_payment_waiting,
                                color = FlareTheme.colors.textSecondary.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        is PaymentState.Completed -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                    .border(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = I18n.strings.sub_payment_completed,
                                color = Color(0xFF22C55E),
                                fontFamily = GeologicaMedium,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                            DialogPrimaryButton(
                                onClick = onDismiss,
                                text = I18n.strings.btn_finish
                            )
                        }
                        is PaymentState.Error -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_error),
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = I18n.strings.sub_payment_error.format(state.message),
                                color = Color(0xFFEF4444),
                                fontFamily = GeologicaMedium,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                            DialogPrimaryButton(
                                onClick = { step = 1 },
                                text = I18n.strings.sub_btn_retry
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun CoinCard(
    name: String,
    description: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = FlareTheme.colors.accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) accentColor.copy(alpha = 0.08f)
                else FlareTheme.colors.bgItem.copy(alpha = 0.5f)
            )
            .border(
                width = 1.5.dp,
                color = if (selected) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = name,
                    color = FlareTheme.colors.textPrimary,
                    fontFamily = GeologicaMedium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = FlareTheme.colors.textSecondary,
                    fontFamily = GeologicaRegular,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            
            if (selected) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SparksCard(
    name: String,
    description: String,
    qty: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = FlareTheme.colors.accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) accentColor.copy(alpha = 0.08f)
                else FlareTheme.colors.bgItem.copy(alpha = 0.5f)
            )
            .border(
                width = 1.5.dp,
                color = if (selected) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_spark_filled),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = name,
                    color = FlareTheme.colors.textPrimary,
                    fontFamily = GeologicaMedium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = FlareTheme.colors.textSecondary,
                    fontFamily = GeologicaRegular,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            
            if (selected) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DialogPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val accentColor = FlareTheme.colors.accent
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                color = if (enabled) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
            fontSize = 15.sp,
            fontFamily = GeologicaMedium
        )
    }
}

@Composable
fun DialogSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                color = FlareTheme.colors.bgItem.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = FlareTheme.colors.glassStroke.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = FlareTheme.colors.textPrimary,
            fontSize = 15.sp,
            fontFamily = GeologicaMedium
        )
    }
}
