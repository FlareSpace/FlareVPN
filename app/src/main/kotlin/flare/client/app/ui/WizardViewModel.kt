package flare.client.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.model.SubscriptionEntity
import flare.client.app.data.repository.ProfileRepository
import flare.client.app.ui.i18n.I18n
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class ServerType {
    FLARE, CUSTOM
}

enum class TariffType {
    FREE, PREMIUM
}

enum class SelectedProtocol {
    XRAY, HYSTERIA2, SHADOWSOCKS, WIREGUARD
}

class WizardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = flare.client.app.data.db.AppDatabase.getInstance(application)
    private val profileRepository = ProfileRepository(db.profileDao(), db.subscriptionDao())
    
    private var setupJob: kotlinx.coroutines.Job? = null
    
    var composeWizardStep by mutableStateOf(WizardStep.CARDS)
    var composeSshProfileName by mutableStateOf("")
    var composeSshIp by mutableStateOf("")
    var composeSshPort by mutableStateOf("22")
    var composeSshUser by mutableStateOf("")
    var composeSshPassword by mutableStateOf("")
    var composeXrayPort by mutableStateOf("")
    var composeXraySni by mutableStateOf("")
    var composeXrayObfsPassword by mutableStateOf("")
    var composeXrayFingerprint by mutableStateOf("chrome")
    var composeXrayPortHoppingEnabled by mutableStateOf(false)
    var composeXrayPortHoppingValue by mutableStateOf("")
    var composeXrayTransport by mutableStateOf("tcp")
    var composeXrayHost by mutableStateOf("")
    var composeXrayPath by mutableStateOf("")
    var composeXrayServiceName by mutableStateOf("")
    var composeXrayXhttpMode by mutableStateOf("auto")
    var composeIsXrayTransportExpanded by mutableStateOf(false)
    var composeIsXrayXhttpModeExpanded by mutableStateOf(false)
    var composeSetupStatus by mutableStateOf("")
    var composeSetupProgress by mutableStateOf(0f)
    var composeSetupError by mutableStateOf<String?>(null)
    var composeSelectedServerType by mutableStateOf<ServerType?>(null)
    var composeSelectedTariff by mutableStateOf<TariffType?>(null)
    var composeSelectedProtocol by mutableStateOf(SelectedProtocol.XRAY)
    var composeFreeSubscriptionSuccess by mutableStateOf(true)
    var composeFreeSubscriptionError by mutableStateOf<String?>(null)
    var composeAuthError by mutableStateOf<String?>(null)
    var composeIsAuthPolling by mutableStateOf(false)

    val isSshConfigValid: Boolean
        get() {
            val portInt = composeSshPort.toIntOrNull()
            val isPortValid = portInt != null && portInt in 1..65535
            val isHostValid = composeSshIp.isNotBlank() && composeSshIp.length >= 3 && !composeSshIp.contains(" ")
            
            return composeSshProfileName.isNotBlank() &&
                   isHostValid &&
                   isPortValid &&
                   composeSshUser.isNotBlank() &&
                   composeSshPassword.isNotBlank()
        }

    val isXrayConfigValid: Boolean
        get() {
            val portInt = composeXrayPort.toIntOrNull()
            val isPortValid = composeXrayPort.isBlank() || (portInt != null && portInt in 1..65535)
            val isSniValid = composeXraySni.isBlank() || (composeXraySni.length >= 3 && !composeXraySni.contains(" "))
            
            return isPortValid && isSniValid
        }

    fun nextStep() {
        when (composeWizardStep) {
            WizardStep.CARDS -> {
                if (composeSelectedServerType == ServerType.CUSTOM) {
                    composeWizardStep = WizardStep.SSH_CONFIG
                } else if (composeSelectedServerType == ServerType.FLARE) {
                    composeWizardStep = WizardStep.FLARE_TARIFFS
                }
            }
            WizardStep.SSH_CONFIG -> {
                if (isSshConfigValid) {
                    composeWizardStep = WizardStep.PROTOCOL
                }
            }
            WizardStep.PROTOCOL -> {
                composeWizardStep = WizardStep.XRAY_CONFIG
            }
            WizardStep.XRAY_CONFIG -> {
                if (isXrayConfigValid) {
                    startSetup()
                }
            }
            WizardStep.PROGRESS -> {
                if (composeSetupProgress >= 100f) {
                    composeWizardStep = WizardStep.SUCCESS
                }
            }
            WizardStep.FLARE_TARIFFS -> {
                if (composeSelectedTariff == TariffType.FREE) {
                    val authManager = flare.client.app.data.auth.AuthManager(getApplication(), flare.client.app.data.SettingsManager(getApplication()))
                    if (!authManager.isLoggedIn()) {
                        composeWizardStep = WizardStep.FLARE_FREE_AUTH_PROMPT
                    } else {
                        composeWizardStep = WizardStep.FLARE_PROGRESS
                        composeSetupStatus = flare.client.app.ui.i18n.I18n.strings.wizard_setup_free_status
                        addFreeSubscription(auth = true) {
                            composeWizardStep = WizardStep.FLARE_SUCCESS
                        }
                    }
                } else if (composeSelectedTariff == TariffType.PREMIUM) {
                    val authManager = flare.client.app.data.auth.AuthManager(getApplication(), flare.client.app.data.SettingsManager(getApplication()))
                    if (!authManager.isLoggedIn()) {
                        composeWizardStep = WizardStep.FLARE_AUTH
                        
                    } else {
                        composeWizardStep = WizardStep.FLARE_BUY
                    }
                }
            }
            WizardStep.FLARE_PROGRESS -> {
                
            }
            WizardStep.FLARE_AUTH, WizardStep.FLARE_BUY, WizardStep.FLARE_FREE_AUTH_PROMPT -> {
                
            }
            WizardStep.SUCCESS, WizardStep.FLARE_SUCCESS -> {
                reset()
            }
        }
    }

    fun previousStep() {
        if (composeWizardStep == WizardStep.PROGRESS) {
            setupJob?.cancel()
            setupJob = null
            composeSetupError = null
        }
        composeWizardStep = when (composeWizardStep) {
            WizardStep.SSH_CONFIG -> WizardStep.CARDS
            WizardStep.PROTOCOL -> WizardStep.SSH_CONFIG
            WizardStep.XRAY_CONFIG -> WizardStep.PROTOCOL
            WizardStep.PROGRESS -> WizardStep.XRAY_CONFIG
            WizardStep.FLARE_TARIFFS -> WizardStep.CARDS
            WizardStep.FLARE_SUCCESS -> WizardStep.FLARE_TARIFFS
            WizardStep.FLARE_PROGRESS -> WizardStep.FLARE_TARIFFS
            WizardStep.FLARE_AUTH -> WizardStep.FLARE_TARIFFS
            WizardStep.FLARE_BUY -> WizardStep.FLARE_TARIFFS
            WizardStep.FLARE_FREE_AUTH_PROMPT -> WizardStep.FLARE_TARIFFS
            else -> composeWizardStep
        }
    }

    fun reset() {
        setupJob?.cancel()
        setupJob = null
        composeWizardStep = WizardStep.CARDS
        composeSelectedServerType = null
        composeSelectedTariff = null
        composeSelectedProtocol = SelectedProtocol.XRAY
        composeSshProfileName = ""
        composeSshIp = ""
        composeSshPort = "22"
        composeSshUser = ""
        composeSshPassword = ""
        composeXrayPort = ""
        composeXraySni = ""
        composeXrayObfsPassword = ""
        composeXrayFingerprint = "chrome"
        composeXrayPortHoppingEnabled = false
        composeXrayPortHoppingValue = ""
        composeXrayTransport = "tcp"
        composeXrayHost = ""
        composeXrayPath = ""
        composeXrayServiceName = ""
        composeXrayXhttpMode = "auto"
        composeIsXrayTransportExpanded = false
        composeIsXrayXhttpModeExpanded = false
        composeSetupStatus = ""
        composeSetupProgress = 0f
        composeSetupError = null
        composeFreeSubscriptionSuccess = true
        composeFreeSubscriptionError = null
        composeAuthError = null
        composeIsAuthPolling = false
    }

    fun startAuthAndPoll(onAuthSuccess: (() -> Unit)? = null) {
        val authManager = flare.client.app.data.auth.AuthManager(getApplication(), flare.client.app.data.SettingsManager(getApplication()))
        composeIsAuthPolling = true
        composeAuthError = null
        viewModelScope.launch {
            val uuid = authManager.startAuthFlow()
            if (uuid != null) {
                val success = authManager.pollForToken(uuid) == 0
                composeIsAuthPolling = false
                if (success) {
                    if (onAuthSuccess != null) {
                        onAuthSuccess()
                    } else {
                        composeWizardStep = WizardStep.FLARE_BUY
                    }
                } else {
                    composeAuthError = flare.client.app.ui.i18n.I18n.strings.wizard_setup_auth_error_timeout
                }
            } else {
                composeIsAuthPolling = false
                composeAuthError = flare.client.app.ui.i18n.I18n.strings.wizard_setup_auth_error_network
            }
        }
    }

    fun selectFreeWithoutAuth() {
        composeWizardStep = WizardStep.FLARE_PROGRESS
        composeSetupStatus = flare.client.app.ui.i18n.I18n.strings.wizard_setup_free_status
        addFreeSubscription(auth = false) {
            composeWizardStep = WizardStep.FLARE_SUCCESS
        }
    }

    fun selectFreeWithAuth() {
        composeWizardStep = WizardStep.FLARE_AUTH
        
    }

    fun onFreeAuthSuccess() {
        composeWizardStep = WizardStep.FLARE_PROGRESS
        composeSetupStatus = flare.client.app.ui.i18n.I18n.strings.wizard_setup_free_status
        addFreeSubscription(auth = true) {
            composeWizardStep = WizardStep.FLARE_SUCCESS
        }
    }

    fun onPremiumAuthSuccess() {
        composeWizardStep = WizardStep.FLARE_BUY
    }

    fun openTelegramBuy() {
        val tariffName = "premium"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("tg://resolve?domain=${flare.client.app.data.api.FlareBackendApi.BOT_USERNAME}&start=buy_$tariffName"))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun completeBuy() {
        reset()
    }

    fun addFreeSubscription(auth: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            composeFreeSubscriptionError = null
            
            val authManager = flare.client.app.data.auth.AuthManager(getApplication(), flare.client.app.data.SettingsManager(getApplication()))
            val token = authManager.getToken()
            
            var subLink: String? = null
            var errorMsg: String? = null

            val result = if (auth && token != null) {
                flare.client.app.data.api.FlareBackendApi.createFreeKeyAuthorized(token)
            } else {
                val settings = flare.client.app.data.SettingsManager(getApplication())
                val hwid = settings.getHardwareId()
                flare.client.app.data.api.FlareBackendApi.createFreeKeyAnonymous(hwid)
            }
            
            result.onSuccess { response ->
                subLink = response.sub_link
            }.onFailure { e ->
                errorMsg = e.message
            }

            if (subLink != null) {
                try {
                    val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("proxy_link", subLink)
                    clipboard.setPrimaryClip(clip)
                } catch (e: Exception) {
                    
                }
                
                try {
                    val app = getApplication<Application>()
                    val settings = flare.client.app.data.SettingsManager(app)
                    val hwidString = settings.getHardwareId()
                    val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                    val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
                    
                    val parseResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        flare.client.app.data.parser.ClipboardParser.parse(app, subLink, hwidString, model, osVersion, settings.subUserAgent, settings.subUpdateTimeout)
                    }
                    
                    when (parseResult) {
                        is flare.client.app.data.parser.ClipboardParser.ParseResult.Subscription -> {
                            profileRepository.insertSubscriptionWithProfiles(parseResult.subscription, parseResult.profiles)
                            composeFreeSubscriptionSuccess = true
                        }
                        is flare.client.app.data.parser.ClipboardParser.ParseResult.MultipleProfiles -> {
                            parseResult.profiles.forEach { profile ->
                                profileRepository.insertProfile(profile)
                            }
                            composeFreeSubscriptionSuccess = true
                        }
                        is flare.client.app.data.parser.ClipboardParser.ParseResult.SingleProfile -> {
                            profileRepository.insertProfile(parseResult.profile)
                            composeFreeSubscriptionSuccess = true
                        }
                        is flare.client.app.data.parser.ClipboardParser.ParseResult.Error -> {
                            errorMsg = parseResult.message
                            composeFreeSubscriptionSuccess = false
                        }
                    }
                } catch (e: Exception) {
                    errorMsg = flare.client.app.ui.i18n.I18n.strings.wizard_setup_free_parse_error
                    composeFreeSubscriptionSuccess = false
                }
            } else {
                composeFreeSubscriptionSuccess = false
                if (errorMsg?.contains("HTTP 400") == true) {
                    errorMsg = flare.client.app.ui.i18n.I18n.strings.wizard_setup_free_limit_exceeded
                } else if (errorMsg?.contains("HTTP 403") == true) {
                    errorMsg = flare.client.app.ui.i18n.I18n.strings.wizard_setup_free_telegram_required
                }
            }
            
            composeFreeSubscriptionError = errorMsg

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 2000) {
                delay(2000 - elapsed)
            }

            onComplete()
        }
    }

    private fun startSetup() {
        setupJob?.cancel()
        composeSetupError = null
        setupJob = viewModelScope.launch {
            val strings = I18n.strings
            composeWizardStep = WizardStep.PROGRESS
            composeSetupProgress = 0f
            composeSetupStatus = strings.ssh_status_connecting
            
            val creator: flare.client.app.util.VpnServerCreator = if (composeSelectedProtocol == SelectedProtocol.HYSTERIA2) {
                flare.client.app.util.HysteriaServerCreator(getApplication())
            } else if (composeSelectedProtocol == SelectedProtocol.SHADOWSOCKS) {
                flare.client.app.util.ShadowsocksServerCreator(getApplication())
            } else if (composeSelectedProtocol == SelectedProtocol.WIREGUARD) {
                flare.client.app.util.WireGuardServerCreator(getApplication())
            } else {
                flare.client.app.util.XrayServerCreator(getApplication())
            }
            
            val progressJob = launch {
                creator.progress.collect { progress ->
                    composeSetupProgress = progress.toFloat()
                }
            }
            
            val statusJob = launch {
                creator.status.collect { status ->
                    composeSetupStatus = status
                }
            }

            val defaultPort = when (composeSelectedProtocol) {
                SelectedProtocol.WIREGUARD -> "51820"
                SelectedProtocol.SHADOWSOCKS -> "8388"
                else -> "443"
            }
            val finalPort = composeXrayPort.ifBlank { defaultPort }
            val finalSni = composeXraySni.ifBlank { "google.com" }
            val snis = finalSni.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val primarySni = snis.firstOrNull() ?: "google.com"
            val sshPortInt = composeSshPort.toIntOrNull() ?: 22

            val config = flare.client.app.util.VpnServerConfig(
                host = composeSshIp,
                sshPort = sshPortInt,
                user = composeSshUser,
                pass = composeSshPassword,
                vpnPort = finalPort.toIntOrNull() ?: 443,
                sni = primarySni,
                obfsPassword = composeXrayObfsPassword,
                fingerprint = composeXrayFingerprint,
                mport = if (composeXrayPortHoppingEnabled) composeXrayPortHoppingValue.trim() else null,
                transport = composeXrayTransport,
                transportHost = composeXrayHost,
                transportPath = composeXrayPath,
                serviceName = composeXrayServiceName,
                xhttpMode = composeXrayXhttpMode
            )

            val vlessUri = creator.setup(config)
            
            progressJob.cancel()
            statusJob.cancel()

            if (!isActive) return@launch

            if (vlessUri != null) {
                val subName = strings.sub_my_servers
                val allSubs = profileRepository.getAllSubscriptions().first()
                var sub = allSubs.find { I18n.isMyServers(it.name) }
                if (sub == null) {
                    val newSub = SubscriptionEntity(
                        name = subName,
                        url = "",
                        total = Long.MAX_VALUE
                    )
                    val id = profileRepository.insertSubscription(newSub)
                    sub = newSub.copy(id = id)
                }

                var parseError: String? = null
                val parsedProfile = try {
                    flare.client.app.data.parser.ClipboardParser.buildProfileFromUri(
                        getApplication(), vlessUri, subscriptionId = sub.id
                    )
                } catch (e: Exception) {
                    android.util.Log.e("WizardViewModel", "Failed to parse generated URI: $vlessUri", e)
                    parseError = e.message ?: e.toString()
                    null
                }
                
                if (parsedProfile != null) {
                    val finalProfile = parsedProfile.copy(
                        name = composeSshProfileName,
                        serverDescription = when (composeSelectedProtocol) {
                            SelectedProtocol.HYSTERIA2 -> "Custom Hysteria 2 Server"
                            SelectedProtocol.SHADOWSOCKS -> "Custom Shadowsocks Server"
                            SelectedProtocol.WIREGUARD -> "Custom WireGuard Server"
                            else -> "Custom Xray Server"
                        }
                    )
                    profileRepository.insertProfile(finalProfile)
                    delay(500)
                    composeWizardStep = WizardStep.SUCCESS
                } else {
                    composeSetupError = "Failed to parse connection link: $parseError"
                }
            } else {
                composeSetupError = creator.status.value
            }
        }
    }
}