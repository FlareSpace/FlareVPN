package flare.client.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import kotlin.math.cos
import kotlin.math.sin
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewStub
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.annotation.IdRes
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import flare.client.app.R
import flare.client.app.ui.manager.*
import flare.client.app.data.SettingsManager
import androidx.activity.compose.setContent

import flare.client.app.data.model.DisplayItem
import flare.client.app.data.model.ProfileSummary
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import flare.client.app.util.GlassUtils
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper

import androidx.compose.ui.graphics.graphicsLayer
import flare.client.app.ui.components.*
import flare.client.app.ui.components.dialogs.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.notification.NotificationType
import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.navigation.compose.rememberNavController
import flare.client.app.ui.navigation.Destination
import flare.client.app.ui.FlareApp
import flare.client.app.util.ProfileExportHelper
import flare.client.app.ui.theme.FlareTheme


enum class WizardStep {
    CARDS, SSH_CONFIG, PROTOCOL, XRAY_CONFIG, PROGRESS, SUCCESS, FLARE_TARIFFS, FLARE_PROGRESS, FLARE_SUCCESS
}

class MainActivity : AppCompatActivity() {

    
    
    override fun attachBaseContext(newBase: Context) {
        val lang = newBase
            .getSharedPreferences("flare_settings", Context.MODE_PRIVATE)
            .getString("app_language", "auto") ?: "auto"
        val wrapped = if (lang == "en" || lang == "ru") {
            val locale = java.util.Locale(lang)
            java.util.Locale.setDefault(locale)
            val cfg = android.content.res.Configuration(newBase.resources.configuration)
            cfg.setLocale(locale)
            newBase.createConfigurationContext(cfg)
        } else {
            newBase
        }
        super.attachBaseContext(wrapped)
    }


    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val wizardViewModel: WizardViewModel by viewModels()

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var themeManager: ThemeManager

    private lateinit var settings: SettingsManager
    
    private var hasPendingTriggerVpnPermissionRequest = false
    private var isInitializingSettings = false
    private var mainUiInitialized = true

    private var runtimeAccentColor by mutableStateOf(ThemeManager.COLOR_DEFAULT)
    private var runtimeAccentEndColor by mutableStateOf(ThemeManager.COLOR_DEFAULT_END)
    private var isClipboardLoading by mutableStateOf(false)

    
    private var showManualInputDialogState by mutableStateOf(false)
    private var showAppSelectionDialogState by mutableStateOf(false)
    private var appSelectionDialogApps by mutableStateOf<List<AppListItem>>(emptyList())
    private var showUsageDialogState by mutableStateOf(false)
    private var showEditSubscriptionDialogState by mutableStateOf(false)
    private var editSubscriptionTarget by mutableStateOf<flare.client.app.data.model.SubscriptionEntity?>(null)
    private var showProfileQrDialogState by mutableStateOf(false)
    private var profileQrBitmap by mutableStateOf<Bitmap?>(null)
    private var lastQrIsSubscription by mutableStateOf(false)

    private var showOnboardingDialogState by mutableStateOf(false)
    private var isNotificationPermissionGranted by mutableStateOf(false)
    private var isBatteryOptimizationIgnored by mutableStateOf(false)

    private fun setupPermissions() {
        permissionHandler = PermissionHandler(
            activity = this,
            onVpnResult = { isGranted ->
                if (isGranted) viewModel.startVpnFromUi()
                else showToast(I18n.strings.vpn_error_permission_denied)
            },
            onNotificationResult = { isGranted ->
                if (isGranted) {
                    showTestNotification()
                } else {
                    showToast(I18n.strings.onboarding_notifications_error)
                    settingsViewModel.composeIsStatusNotificationEnabled = false
                    settings.isStatusNotificationEnabled = false
                }
            },
            onOnboardingNotificationResult = { isGranted ->
                isNotificationPermissionGranted = isGranted
                if (isGranted) {
                    showToast(I18n.strings.onboarding_toast_notification_granted)
                } else {
                    showToast(I18n.strings.onboarding_toast_notification_denied)
                }
            },
            onBatteryResult = {
                isBatteryOptimizationIgnored = checkBatteryOptimizationIgnored()
                if (isBatteryOptimizationIgnored) {
                    showToast(I18n.strings.onboarding_toast_battery_unrestricted)
                }
            },
            onUsageResult = {
                
            },
            onImportFileResult = { uri ->
                if (uri == null) return@PermissionHandler
                if (!isSupportedImportFile(uri)) {
                    showToast(I18n.strings.error_import_file_type)
                    return@PermissionHandler
                }
                lifecycleScope.launch {
                    val content = withContext(Dispatchers.IO) { readTextFromUri(uri) }
                    if (content.isNullOrBlank()) {
                        showToast(I18n.strings.error_import_file_read)
                    } else {
                        viewModel.importFromClipboard(content)
                    }
                }
            },
            onQrScanResult = { qrContent ->
                if (qrContent.isNullOrBlank()) {
                    showToast(I18n.strings.error_qr_scan_empty)
                } else {
                    viewModel.importFromClipboard(qrContent)
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        
        
        
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("FlareVPN_CRASH", "Uncaught exception on thread ${thread.name}", throwable)
            
            previousHandler?.uncaughtException(thread, throwable)
                ?: android.os.Process.killProcess(android.os.Process.myPid())
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        val currentUiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        if (ThemeManager.lastUiMode != -1 && ThemeManager.lastUiMode != currentUiMode) {
            ThemeManager.themeChangedJustNow = true
            ThemeManager.lastThemeChangeTime = System.currentTimeMillis()
        }
        ThemeManager.lastUiMode = currentUiMode
        settings = SettingsManager(this)
        showOnboardingDialogState = !settings.isOnboardingCompleted
        isNotificationPermissionGranted = checkNotificationPermission()
        isBatteryOptimizationIgnored = checkBatteryOptimizationIgnored()

        

        setupPermissions()
        themeManager = ThemeManager(this, settings) { accent, accentEnd ->
            runtimeAccentColor = accent
            runtimeAccentEndColor = accentEnd
            settingsViewModel.composeAccentColor = androidx.compose.ui.graphics.Color(accent)
        }

        setContent {
            var screenAlpha by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                androidx.compose.animation.core.animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                ) { value, _ ->
                    screenAlpha = value
                }
            }

            val appHazeState = rememberHazeState()
            val dialogHazeState = rememberHazeState()
            val noticeState by viewModel.noticeState.collectAsState()
            val isDark = when (settingsViewModel.composeThemeMode) {
                1 -> false
                2 -> true
                else -> settingsViewModel.composeSystemIsDark
            }

            FlareTheme(
                isDark = isDark,
                accentColor = androidx.compose.ui.graphics.Color(runtimeAccentColor),
                accentEndColor = androidx.compose.ui.graphics.Color(runtimeAccentEndColor)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = screenAlpha }
                        .hazeSource(state = dialogHazeState)
                ) {
                    FlareApp(
                    isDark = isDark,
                    mainViewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    wizardViewModel = wizardViewModel,
                    accentColor = runtimeAccentColor,
                    accentEndColor = runtimeAccentEndColor,
                    isClipboardLoading = isClipboardLoading,
                    onManualInputClick = { showManualInputDialogState = true },
                    onQrScanClick = { permissionHandler.launchQrScanner(Intent(this@MainActivity, QrScannerActivity::class.java)) },
                    onImportFileClick = { permissionHandler.launchImportFile(arrayOf("text/plain", "application/json", "text/json")) },
                    onShareProfile = { summary ->
                        lifecycleScope.launch {
                            val profile = withContext(Dispatchers.IO) {
                                viewModel.getProfileById(summary.id)
                            }
                            if (profile != null) {
                                val link = flare.client.app.util.ProfileExportHelper.exportLink(profile)
                                if (link != null) {
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("proxy_link", link)
                                    clipboard.setPrimaryClip(clip)
                                    showToast(I18n.strings.success_link_copied)

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, link)
                                    }
                                    startActivity(Intent.createChooser(shareIntent, I18n.strings.btn_share_link))
                                } else {
                                    showToast(I18n.strings.error_link_generation)
                                }
                            } else {
                                showToast(I18n.strings.error_link_generation)
                            }
                        }
                    },
                    onQrProfile = { summary -> 
                        lifecycleScope.launch {
                            val profile = withContext(Dispatchers.IO) {
                                viewModel.getProfileById(summary.id)
                            }
                            if (profile != null) {
                                val link = flare.client.app.util.ProfileExportHelper.exportLink(profile)
                                if (link != null) {
                                    lastQrIsSubscription = false
                                    profileQrBitmap = generateQrCodeBitmap(link)
                                    showProfileQrDialogState = true
                                } else {
                                    showToast(I18n.strings.error_link_generation)
                                }
                            } else {
                                showToast(I18n.strings.error_link_generation)
                            }
                        }
                    },
                    onShareSubscription = { subscription ->
                        val link = subscription.url
                        if (link.isNotBlank()) {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("proxy_link", link)
                            clipboard.setPrimaryClip(clip)
                            showToast(I18n.strings.success_link_copied)

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, link)
                            }
                            startActivity(Intent.createChooser(shareIntent, I18n.strings.btn_share_link))
                        } else {
                            showToast(I18n.strings.error_link_generation)
                        }
                    },
                    onQrSubscription = { subscription ->
                        val link = subscription.url
                        if (link.isNotBlank()) {
                            profileQrBitmap = generateQrCodeBitmap(link)
                            if (profileQrBitmap != null) {
                                lastQrIsSubscription = true
                                showProfileQrDialogState = true
                            } else {
                                showToast(I18n.strings.error_profile_qr_generation)
                            }
                        } else {
                            showToast(I18n.strings.error_link_generation)
                        }
                    },
                    onLanguageSelected = { langCode ->
                        if (settings.appLanguage != langCode) {
                            settings.appLanguage = langCode
                            settingsViewModel.composeAppLanguage = langCode
                            
                            
                            I18n.updateLocale(langCode)
                            settingsViewModel.composeSplitTunnelingDesc = getSplitTunnelingDesc()
                        }
                    },
                    onLogLevelClick = { level ->
                        settings.coreLogLevel = level
                        settingsViewModel.composeCoreLogLevel = level
                    },
                    onUpdateFrequencyClick = { freq ->
                        settings.updateCheckFrequency = freq
                        settingsViewModel.composeUpdateFrequency = freq
                    },
                    onBestProfileOnlyConnectedClick = { enabled ->
                        settings.isBestProfileOnlyIfConnected = enabled
                        settingsViewModel.composeIsBestProfileOnlyConnected = enabled
                    },
                    onUserAgentClick = { agent ->
                        settings.subUserAgent = agent
                        settingsViewModel.composeSubUserAgent = agent
                    },
                    onRoutingModeClick = { ruleId, mode ->
                        viewModel.setRoutingRuleMode(ruleId, mode)
                    },
                    onPacketTypeClick = { packetType ->
                        settings.packetType = packetType
                        settingsViewModel.composePacketType = packetType
                        showSettingsNotification()
                    },
                    onMuxProtocolClick = { proto ->
                        settings.muxProtocol = proto
                        settingsViewModel.composeMuxProtocol = proto
                        showSettingsNotification()
                    },
                    onMuxPaddingClick = { enabled ->
                        settings.muxPadding = enabled
                        settingsViewModel.composeMuxPadding = enabled
                        showSettingsNotification()
                    },
                    onTunStackClick = { stack ->
                        settings.tunStack = stack
                        settingsViewModel.composeTunStack = stack
                        showSettingsNotification()
                    },

                    onPingStyleClick = { style ->
                        settings.pingStyle = style
                        settingsViewModel.composePingStyle = style
                    },
                    onThemeClick = { index -> handleThemeModeChange(index, window.decorView) },
                    onEditSubscriptionClick = { sub -> 
                        editSubscriptionTarget = sub
                        showEditSubscriptionDialogState = true 
                    },
                    onChangeAppsClick = { showAppSelectionDialog() },
                    onViewJournalClick = {  },
                    onClipboardClick = {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (text.isNullOrBlank()) {
                            showToast(I18n.strings.error_clipboard_empty)
                        } else {
                            viewModel.importFromClipboard(text)
                        }
                    },
                    showBottomNav = true,
                    onSelectedRootTabChanged = { index ->
                        settingsViewModel.composeBottomNavTabIndex = index
                    },
                    onDataManagementClick = {},
                    settings = settings,
                    onRestartRequired = { showSettingsNotification() },
                    appHazeState = appHazeState
                )
                }

                if (showManualInputDialogState) {
                    var textValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                    ManualInputDialog(
                        onDismissRequest = { showManualInputDialogState = false },
                        title = I18n.strings.manual_input_title,
                        hint = I18n.strings.manual_input_hint,
                        initialText = "",
                        textValue = textValue,
                        onTextValueChange = { textValue = it },
                        onCancel = { showManualInputDialogState = false },
                        onAdd = { 
                            if (it.trim().isNotEmpty()) {
                                viewModel.importFromClipboard(it.trim())
                                showManualInputDialogState = false
                            }
                        },
                        accentColor = runtimeAccentColor,
                        hazeState = dialogHazeState
                    )
                }

                if (showProfileQrDialogState) {
                    ProfileQRDialog(
                        onDismissRequest = { showProfileQrDialogState = false },
                        qrBitmap = profileQrBitmap,
                        onClose = { showProfileQrDialogState = false },
                        title = if (lastQrIsSubscription) I18n.strings.subscription_qr_dialog_title else I18n.strings.profile_qr_dialog_title,
                        hazeState = dialogHazeState
                    )
                }

                if (showEditSubscriptionDialogState && editSubscriptionTarget != null) {
                    val sub = editSubscriptionTarget!!
                    val initialName = if (I18n.isMyServers(sub.name)) {
                        I18n.strings.sub_my_servers
                    } else {
                        sub.name
                    }
                    var nameValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialName) }
                    var urlValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(sub.url) }

                    EditSubscriptionDialog(
                        onDismissRequest = { showEditSubscriptionDialogState = false },
                        nameValue = nameValue,
                        onNameChange = { nameValue = it },
                        urlValue = urlValue,
                        onUrlChange = { urlValue = it },
                        supportWeb = if (sub.webPageUrl.isNotBlank()) formatSupportUrl(sub.webPageUrl) else null,
                        supportTg = if (sub.supportUrl.isNotBlank()) formatSupportUrl(sub.supportUrl) else null,
                        onSupportWebClick = {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(sub.webPageUrl)))
                            } catch (e: Exception) { }
                        },
                        onSupportTgClick = {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(sub.supportUrl)))
                            } catch (e: Exception) { }
                        },
                        onCancel = { showEditSubscriptionDialogState = false },
                        onSave = {
                            viewModel.updateSubscription(sub.id, nameValue.trim(), urlValue.trim())
                            showEditSubscriptionDialogState = false
                        },
                        accentColor = runtimeAccentColor,
                        hazeState = dialogHazeState
                    )
                }

                if (showAppSelectionDialogState) {
                    var tabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
                    var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                    
                    val initialSites = settings.splitTunnelingSites.joinToString("\n")
                    var sitesText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialSites) }
                    
                    var modeApps by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(settings.splitTunnelingModeApps) }
                    var modeSites by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(settings.splitTunnelingModeSites) }
                    
                    val selectedPackages = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateListOf(*settings.splitTunnelingApps.toTypedArray()) }
                    var triggerEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(settings.isAppTriggerEnabled) }

                    val filteredApps = androidx.compose.runtime.remember(searchQuery) {
                        if (searchQuery.isEmpty()) appSelectionDialogApps else appSelectionDialogApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    }

                    val selectedAppsCount = selectedPackages.size
                    val selectedSitesCount = remember(sitesText) {
                        sitesText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.size
                    }

                    AppSelectionDialog(
                        onDismissRequest = { showAppSelectionDialogState = false },
                        tabIndex = tabIndex,
                        onTabSelected = { tabIndex = it },
                        selectedAppsCount = selectedAppsCount,
                        selectedSitesCount = selectedSitesCount,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        sitesText = sitesText,
                        onSitesTextChange = { sitesText = it },
                        modeText = if ((if (tabIndex == 0) modeApps else modeSites) == "whitelist") I18n.strings.split_mode_whitelist else I18n.strings.split_mode_blacklist,
                        onModeSelected = { newMode ->
                            if (tabIndex == 0) modeApps = newMode else modeSites = newMode
                        },
                        isTriggerEnabled = triggerEnabled,
                        onTriggerChange = { isChecked ->
                            if (isChecked && !isUsageAccessGranted()) {
                                triggerEnabled = false
                                showUsageDialogState = true
                            } else {
                                triggerEnabled = isChecked
                            }
                        },
                        onTriggerHintClick = {
                            showToast(I18n.strings.trigger_hint)
                        },
                        onCancel = { showAppSelectionDialogState = false },
                        onSave = {
                            val finalSites = sitesText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                            
                            val hasChanged = settings.splitTunnelingApps != selectedPackages.toSet() ||
                                            settings.splitTunnelingSites != finalSites ||
                                            settings.splitTunnelingModeApps != modeApps ||
                                            settings.splitTunnelingModeSites != modeSites ||
                                            settings.isAppTriggerEnabled != triggerEnabled

                            settings.splitTunnelingApps = selectedPackages.toSet()
                            settings.splitTunnelingSites = finalSites
                            settings.splitTunnelingModeApps = modeApps
                            settings.splitTunnelingModeSites = modeSites
                            
                            val triggerChanged = settings.isAppTriggerEnabled != triggerEnabled
                            settings.isAppTriggerEnabled = triggerEnabled
                            
                            if (triggerChanged) {
                                updateAppMonitorService()
                            }
                            
                            settingsViewModel.composeSplitTunnelingDesc = getSplitTunnelingDesc()
                            if (hasChanged) showSettingsNotification()
                            showAppSelectionDialogState = false
                        },
                        accentColor = runtimeAccentColor,
                        accentEndColor = runtimeAccentEndColor,
                        hazeState = dialogHazeState,
                        appsContent = {
                            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredApps.size) { i ->
                                    val item = filteredApps[i]
                                    val isSelected = selectedPackages.contains(item.packageName)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) selectedPackages.remove(item.packageName)
                                                else selectedPackages.add(item.packageName)
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            bitmap = item.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(42.dp)
                                        )
                                        Text(
                                            text = item.name,
                                            color = FlareTheme.colors.textPrimary,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                                        )
                                        if (isSelected) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_check),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color(runtimeAccentColor)),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                if (showUsageDialogState) {
                    flare.client.app.ui.components.dialogs.GlassDialog(
                        onDismissRequest = { showUsageDialogState = false },
                        maxWidthDp = 340,
                        hazeState = dialogHazeState
                    ) {
                        GlassDialogContent(
                            title = I18n.strings.onboarding_usage_title,
                            text = I18n.strings.permission_usage_stats_needed,
                            cancelText = I18n.strings.btn_cancel,
                            actionText = I18n.strings.btn_grant,
                            onCancel = { showUsageDialogState = false },
                            onAction = {
                                showUsageDialogState = false
                                try {
                                    startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", packageName, null)
                                    })
                                } catch (e: Exception) {
                                    startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            },
                            onClose = { showUsageDialogState = false },
                            accentColor = runtimeAccentColor,
                            accentEndColor = runtimeAccentEndColor
                        )
                    }
                }

                if (showOnboardingDialogState) {
                    OnboardingDialog(
                        onDismissRequest = { showOnboardingDialogState = false },
                        isNotificationGranted = isNotificationPermissionGranted,
                        isBatteryOptimized = isBatteryOptimizationIgnored,
                        onNotificationClick = {
                            permissionHandler.launchOnboardingNotificationPermission()
                        },
                        onBatteryClick = {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = android.net.Uri.parse("package:$packageName")
                                }
                                permissionHandler.launchBatteryPermission(intent)
                            } catch (e: Exception) {
                                try {
                                    startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (ex: Exception) {
                                    showToast(I18n.strings.error_open_settings)
                                }
                            }
                        },
                        onFragmentationSelected = { enabled ->
                            settings.isFragmentationEnabled = enabled
                            settingsViewModel.composeIsFragmentationEnabled = enabled
                        },
                        onMuxSelected = { enabled ->
                            settings.isMuxEnabled = enabled
                            settingsViewModel.composeIsMuxEnabled = enabled
                        },
                        onSplitPresetsApplied = { mode, ru, social, ai ->
                            applyOnboardingSplitPresets(mode, ru, social, ai)
                        },
                        onFinish = {
                            settings.isOnboardingCompleted = true
                            showOnboardingDialogState = false
                        },
                        accentColor = runtimeAccentColor,
                        hazeState = dialogHazeState
                    )
                }

                if (noticeState.needsToShow) {
                    val sysLang = java.util.Locale.getDefault().language.lowercase()
                    val currentLang = settingsViewModel.composeAppLanguage
                    val isRu = if (currentLang == "auto") sysLang == "ru" else currentLang == "ru"

                    val title = if (isRu) noticeState.titleRu else noticeState.titleEn
                    val text = if (isRu) noticeState.textRu else noticeState.textEn
                    val actionText = if (isRu) noticeState.actionTextRu else noticeState.actionTextEn

                    GlassDialog(
                        onDismissRequest = { viewModel.dismissNotice() },
                        maxWidthDp = 340,
                        hazeState = dialogHazeState
                    ) {
                        GlassDialogContent(
                            title = title,
                            text = text,
                            cancelText = null,
                            actionText = actionText.ifBlank { if (isRu) "Понятно" else "Got it" },
                            onCancel = null,
                            onAction = {
                                viewModel.dismissNotice()
                                if (noticeState.actionUrl.isNotBlank()) {
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(noticeState.actionUrl)))
                                    } catch (e: Exception) {}
                                }
                            },
                            onClose = { viewModel.dismissNotice() },
                            accentColor = runtimeAccentColor,
                            accentEndColor = runtimeAccentEndColor
                        )
                    }
                }
            }
        }

        handleIntentAction(intent)
        
        

        initializeMainUI()
    }


    private fun initializeMainUI() {
        I18n.updateLocale(settings.appLanguage)
        settingsViewModel.syncAll(settings)
        settingsViewModel.composeSystemIsDark = (applicationContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        settingsViewModel.composeSplitTunnelingDesc = getSplitTunnelingDesc()

        val isDark = when (settings.themeMode) {
            1 -> false
            2 -> true
            else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        themeManager.updateSystemBars(isDark)

        if (settings.isCustomColorEnabled) {
            val (accent, accentEnd) = themeManager.getColorsForKey(settings.accentColorKey)
            themeManager.applyAccentColorsToUI(accent, accentEnd)
        } else {
            themeManager.applyAccentColorsToUI(ThemeManager.COLOR_DEFAULT, ThemeManager.COLOR_DEFAULT_END)
        }

        viewModel.initializeAsync()
        observeViewModel()
        requestPendingTriggerVpnPermissionIfNeeded()
    }

    private fun getSplitTunnelingDesc(): String {
        val appsCount = settings.splitTunnelingApps.size
        val sitesCount = settings.splitTunnelingSites.size

        if (appsCount == 0 && sitesCount == 0) {
            return I18n.strings.split_tunneling_desc_default
        }

        return buildString {
            append(I18n.strings.label_selected)
            append(" ")
            if (appsCount > 0) {
                append("$appsCount ${I18n.strings.plural_apps(appsCount)}")
            }
            if (appsCount > 0 && sitesCount > 0) {
                append(I18n.strings.label_and)
            }
            if (sitesCount > 0) {
                append("$sitesCount ${I18n.strings.plural_sites(sitesCount)}")
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentAction(intent)
        requestPendingTriggerVpnPermissionIfNeeded()
    }

    private fun handleIntentAction(intent: Intent?) {
        if (intent?.getBooleanExtra(
                flare.client.app.service.AppMonitorService.EXTRA_REQUEST_VPN_PERMISSION,
                false
            ) == true
        ) {
            hasPendingTriggerVpnPermissionRequest = true
        }

        val uri = intent?.data ?: return
        if (uri.scheme?.equals("flarevpn", ignoreCase = true) == true) {
            val host = uri.host?.lowercase()
            when (host) {
                "open" -> {
                    viewModel.startVpnFromUi()
                }
                "close" -> {
                    viewModel.stopVpnFromUi()
                }
                "ping" -> {
                    viewModel.pingCurrentSubscription()
                }
                "best" -> {
                    viewModel.selectBestProfileFromUi()
                }
                "add" -> {
                    var targetUrl = uri.getQueryParameter("url")
                    if (targetUrl.isNullOrBlank()) {
                        val uriString = uri.toString()
                        val prefix = "flarevpn://add/"
                        if (uriString.startsWith(prefix, ignoreCase = true)) {
                            targetUrl = Uri.decode(uriString.substring(prefix.length))
                        }
                    }
                    if (!targetUrl.isNullOrBlank()) {
                        viewModel.importFromClipboard(targetUrl)
                    }
                }
            }
        }
    }

    private fun requestPendingTriggerVpnPermissionIfNeeded() {
        if (!hasPendingTriggerVpnPermissionRequest || !mainUiInitialized) return
        if (viewModel.connectionState.value != MainViewModel.ConnectionState.DISCONNECTED) {
            hasPendingTriggerVpnPermissionRequest = false
            return
        }

        hasPendingTriggerVpnPermissionRequest = false
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            permissionHandler.launchVpnPermission(vpnIntent)
        } else {
            viewModel.startVpnFromUi()
        }
    }

    private fun handleOnboardingBack() {
    }


    private fun isUsageAccessGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun checkNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun applyOnboardingSplitPresets(mode: String, ru: Boolean, social: Boolean, ai: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val installedPackages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                .map { it.packageName }
                .toSet()

            val selectedApps = mutableSetOf<String>()
            val selectedSites = mutableSetOf<String>()

            if (ru) {
                
                val ruAppPackages = listOf(
                    "ru.gosuslugi.dom", "ru.gosuslugi.goskey", "ru.mail.gosuslugi",
                    "ru.yandex.searchplugin", "ru.yandex.yandexmaps", "ru.yandex.taxi",
                    "ru.yandex.weather", "ru.yandex.disk", "ru.sberbankmobile",
                    "ru.tinkoff.activities", "ru.alfabank.mobile.android", "ru.vtb24.mobilebanking.android"
                )
                selectedApps.addAll(ruAppPackages.filter { installedPackages.contains(it) })

                
                selectedSites.addAll(listOf(
                    "gosuslugi.ru", "yandex.ru", "ya.ru", "sberbank.ru",
                    "tinkoff.ru", "t-bank.ru", "alfabank.ru", "vtb.ru"
                ))
            }

            if (social) {
                
                val socialPackages = listOf(
                    "org.telegram.messenger", "com.whatsapp", "com.instagram.android",
                    "com.facebook.katana", "com.twitter.android"
                )
                selectedApps.addAll(socialPackages.filter { installedPackages.contains(it) })

                
                selectedSites.addAll(listOf(
                    "t.me", "telegram.org", "whatsapp.com", "whatsapp.net",
                    "instagram.com", "facebook.com", "twitter.com", "x.com"
                ))
            }

            if (ai) {
                
                val aiPackages = listOf(
                    "com.openai.chatgpt", "com.google.android.apps.bard"
                )
                selectedApps.addAll(aiPackages.filter { installedPackages.contains(it) })

                
                selectedSites.addAll(listOf(
                    "openai.com", "chatgpt.com", "gemini.google.com", "claude.ai", "anthropic.com"
                ))
            }

            withContext(Dispatchers.Main) {
                settings.isSplitTunnelingEnabled = true
                settings.splitTunnelingModeApps = mode
                settings.splitTunnelingModeSites = mode
                settings.splitTunnelingApps = selectedApps
                settings.splitTunnelingSites = selectedSites

                
                settingsViewModel.composeIsSplitTunnelingEnabled = true
                settingsViewModel.composeSplitTunnelingDesc = getSplitTunnelingDesc()
                
                showSettingsNotification()
                showToast(I18n.strings.split_presets_applied)
            }
        }
    }

    private fun updateAppMonitorService() {
        val intent = Intent(this, flare.client.app.service.AppMonitorService::class.java)
        if (settings.isAppTriggerEnabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }

    data class AppListItem(
            val packageName: String,
            val name: String,
            val icon: androidx.compose.ui.graphics.ImageBitmap,
            var isSelected: Boolean
    )






    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val systemIsDark = (applicationContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        settingsViewModel.composeSystemIsDark = systemIsDark
        val newUiMode = newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        if (ThemeManager.lastUiMode != -1 && ThemeManager.lastUiMode != newUiMode) {
            ThemeManager.lastUiMode = newUiMode
            if (settings.themeMode == 0) {
                val isDark = newUiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                themeManager.updateSystemBars(isDark)
                AppNotificationManager.showNotification(
                    NotificationType.SUCCESS,
                    I18n.strings.notif_theme_changed_auto,
                    3
                )
            }
        }
    }




    private fun handleThemeModeChange(newMode: Int, view: android.view.View) {
        val currentIsNight = when (settings.themeMode) {
            1 -> false
            2 -> true
            else -> (applicationContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val targetIsNight = when (newMode) {
            1 -> false
            2 -> true
            else -> (applicationContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        
        val isSettingSame = newMode == settings.themeMode
        val isEffectivelySame = currentIsNight == targetIsNight

        if (!isSettingSame || !isEffectivelySame) {
            settings.themeMode = newMode
            settingsViewModel.composeThemeMode = newMode
            themeManager.applyTheme()
            
            if (!isEffectivelySame) {
                themeManager.updateSystemBars(targetIsNight)
                AppNotificationManager.showNotification(
                    NotificationType.SUCCESS,
                    I18n.strings.notif_theme_changed,
                    3
                )
            }
        }
    }

    private fun applyBackgroundGradient() {
    }

    private fun startGradientAnimation() {
    }

    private fun stopGradientAnimation() {
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.composeSystemIsDark = (applicationContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (settings.isBackgroundGradientEnabled && settings.isGradientAnimationEnabled) {
            startGradientAnimation()
        }
        if (!settings.isOnboardingCompleted) {
            isNotificationPermissionGranted = checkNotificationPermission()
            isBatteryOptimizationIgnored = checkBatteryOptimizationIgnored()
        }
    }

    override fun onPause() {
        super.onPause()
        stopGradientAnimation()
    }

    private fun restorePendingNavScreen() {
        val screen = settings.pendingNavScreen
        if (screen.isEmpty()) return
        settings.pendingNavScreen = ""
        
    }

    private fun showSettingsNotification() {
        val isThemeRecentlyChanged = System.currentTimeMillis() - ThemeManager.lastThemeChangeTime < 2000
        if (isInitializingSettings || ThemeManager.themeChangedJustNow || isThemeRecentlyChanged) return
        flare.client.app.ui.notification.AppNotificationManager.showNotification(
                flare.client.app.ui.notification.NotificationType.WARNING,
                I18n.strings.settings_restart_tunnel_hint,
                3
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.importEvent.collect { event ->
                when (event) {
                    is MainViewModel.ImportEvent.Loading -> {
                        isClipboardLoading = true
                    }
                    is MainViewModel.ImportEvent.Success -> {
                        isClipboardLoading = false
                        flare.client.app.ui.notification.AppNotificationManager.showNotification(
                            flare.client.app.ui.notification.NotificationType.SUCCESS,
                            event.message,
                            3
                        )
                    }
                    is MainViewModel.ImportEvent.Error -> {
                        isClipboardLoading = false
                        flare.client.app.ui.notification.AppNotificationManager.showNotification(
                            flare.client.app.ui.notification.NotificationType.ERROR,
                            event.message,
                            3
                        )
                    }
                    is MainViewModel.ImportEvent.NeedPermission -> {
                        isClipboardLoading = false
                        permissionHandler.launchVpnPermission(event.intent)
                    }
                }
            }
        }

        lifecycleScope.launch {
            androidx.compose.runtime.snapshotFlow { settingsViewModel.composeIsCustomColorEnabled }.collect { enabled ->
                if (mainUiInitialized) {
                    if (enabled) {
                        val (accent, accentEnd) = themeManager.getColorsForKey(settingsViewModel.composeAccentColorKey)
                        themeManager.animateAccentChange(runtimeAccentColor, runtimeAccentEndColor, accent, accentEnd)
                    } else {
                        themeManager.animateAccentChange(runtimeAccentColor, runtimeAccentEndColor, ThemeManager.COLOR_DEFAULT, ThemeManager.COLOR_DEFAULT_END)
                    }
                }
            }
        }

        lifecycleScope.launch {
            androidx.compose.runtime.snapshotFlow { settingsViewModel.composeAccentColorKey }.collect { key ->
                if (mainUiInitialized && settingsViewModel.composeIsCustomColorEnabled) {
                    val (accent, accentEnd) = themeManager.getColorsForKey(key)
                    themeManager.animateAccentChange(runtimeAccentColor, runtimeAccentEndColor, accent, accentEnd)
                }
            }
        }
    }


    private fun updateConnectButton(state: MainViewModel.ConnectionState) {
        
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showTestNotification() {
        flare.client.app.ui.notification.AppNotificationManager.showNotification(
            flare.client.app.ui.notification.NotificationType.SUCCESS,
            I18n.strings.notif_notifications_enabled,
            3
        )
    }

    private enum class OnboardingStep {
        WELCOME, PERMISSIONS, FRAGMENTATION, MUX, SUCCESS
    }


    private fun showAppTriggerSettings() {
        showAppSelectionDialog()
    }

    private fun onViewJournalClick(anchor: View) {
         
    }

    private fun showAppSelectionDialog() {
        if (settingsViewModel.composeIsChangeAppsLoading) return
        settingsViewModel.composeIsChangeAppsLoading = true
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            val apps = packages.mapNotNull { appInfo ->
                if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 ||
                    appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                ) {
                    val name = appInfo.loadLabel(pm).toString()
                    val packageName = appInfo.packageName
                    val iconDrawable = appInfo.loadIcon(pm)
                    val iconBitmap = iconDrawable.toBitmap().asImageBitmap()
                    AppListItem(packageName, name, iconBitmap, settings.splitTunnelingApps.contains(packageName))
                } else null
            }.sortedBy { it.name }

            withContext(Dispatchers.Main) {
                settingsViewModel.composeIsChangeAppsLoading = false
                appSelectionDialogApps = apps
                showAppSelectionDialogState = true
            }
        }
    }


    private fun generateQrCodeBitmap(content: String, sizePx: Int = 900): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun formatSupportUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("tg://")) return url
        return if (url.startsWith("@")) "tg://resolve?domain=${url.substring(1)}" else "https://$url"
    }



    private fun isSupportedImportFile(uri: Uri): Boolean {
        val name = getFileName(uri) ?: return false
        return name.endsWith(".json", ignoreCase = true) || name.endsWith(".txt", ignoreCase = true)
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = it.getString(index)
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    private fun readTextFromUri(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            java.io.BufferedReader(java.io.InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    }
}