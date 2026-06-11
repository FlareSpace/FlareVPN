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
    FREE, PLUS, PREMIUM
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
    var composeSetupStatus by mutableStateOf("")
    var composeSetupProgress by mutableStateOf(0f)
    var composeSetupError by mutableStateOf<String?>(null)
    var composeSelectedServerType by mutableStateOf<ServerType?>(null)
    var composeSelectedTariff by mutableStateOf<TariffType?>(null)
    var composeSelectedProtocol by mutableStateOf(SelectedProtocol.XRAY)
    var composeFreeSubscriptionSuccess by mutableStateOf(true)

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
                    composeWizardStep = WizardStep.FLARE_PROGRESS
                    addFreeSubscription {
                        composeWizardStep = WizardStep.FLARE_SUCCESS
                    }
                }
            }
            WizardStep.FLARE_PROGRESS -> {
                
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
        composeSetupStatus = ""
        composeSetupProgress = 0f
        composeSetupError = null
        composeFreeSubscriptionSuccess = true
    }

    fun addFreeSubscription(onComplete: () -> Unit) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val allSubs = profileRepository.getAllSubscriptions().first()
            val oldSub = allSubs.find { it.name == "✨ FlareVPN Free" }
            if (oldSub != null) {
                profileRepository.deleteSubscription(oldSub)
            }

            val profiles = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                        
                    val requestBuilder = okhttp3.Request.Builder()
                        .url(flare.client.app.BuildConfig.FREE_SERVERS_URL)
                    
                    val response = client.newCall(requestBuilder.build()).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        response.close()
                        
                        val cleaned = body.trim()
                        val lines = if (cleaned.startsWith("vless://") || cleaned.startsWith("vmess://") || cleaned.startsWith("ss://")) {
                            cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        } else {
                            try {
                                val flat = cleaned.replace("\r", "").replace("\n", "")
                                val clean = flat.replace("-", "+").replace("_", "/")
                                val padded = when (clean.length % 4) { 2 -> "$clean=="; 3 -> "$clean="; else -> clean }
                                val decoded = String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT)).trim()
                                decoded.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            } catch (e: Exception) {
                                cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            }
                        }
                        
                        val parsed = lines.mapIndexedNotNull { _, line ->
                            try {
                                flare.client.app.data.parser.ClipboardParser.buildProfileFromUri(
                                    getApplication(), line, subscriptionId = null
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                        
                        flare.client.app.util.ProfileRenamer.renameProfiles(parsed)
                    } else {
                        response.close()
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (profiles.isNotEmpty()) {
                val sub = SubscriptionEntity(
                    name = "✨ FlareVPN Free",
                    url = "",
                    total = Long.MAX_VALUE
                )
                profileRepository.insertSubscriptionWithProfiles(sub, profiles)
                composeFreeSubscriptionSuccess = true
            } else {
                composeFreeSubscriptionSuccess = false
            }

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
                mport = if (composeXrayPortHoppingEnabled) composeXrayPortHoppingValue.trim() else null
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