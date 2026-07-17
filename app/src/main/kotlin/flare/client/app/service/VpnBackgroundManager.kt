package flare.client.app.service

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import flare.client.app.data.SettingsManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.model.PingState
import flare.client.app.data.repository.ProfileRepository
import flare.client.app.singbox.SingBoxManager
import flare.client.app.util.PingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext


class VpnBackgroundManager(
    private val context: Context,
    private val serviceScope: CoroutineScope,
    private val onProfileSelected: suspend (newProfileId: Long) -> Unit,
    private val onRestartVpn: () -> Unit
) {

    companion object {
        private const val TAG = "VpnBackgroundManager"
    }

    private val db by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppDatabase.getInstance(context)
    }
    private val repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProfileRepository(db.profileDao(), db.subscriptionDao())
    }

    private var healthCheckJob: Job? = null
    private var recoveryJob: Job? = null
    private var bestProfileJob: Job? = null

    
    
    

    fun startMonitoring() {
        startHealthCheckJob()
        startBestProfileJob()
    }

    fun stopMonitoring() {
        healthCheckJob?.cancel()
        recoveryJob?.cancel()
        bestProfileJob?.cancel()
        healthCheckJob = null
        recoveryJob = null
        bestProfileJob = null
        Log.i(TAG, "All background jobs cancelled")
    }

    fun startRecovery() {
        val settings = SettingsManager(context)
        if (!settings.isAdaptiveTunnelEnabled) return
        if (recoveryJob?.isActive == true) return

        recoveryJob = serviceScope.launch {
            recoveryLoop()
        }
    }

    fun cancelRecovery() {
        recoveryJob?.cancel()
        recoveryJob = null
        Log.i(TAG, "Recovery cancelled (manual stop)")
    }

    
    
    

    private fun startHealthCheckJob() {
        val settings = SettingsManager(context)
        healthCheckJob?.cancel()
        if (!settings.isAdaptiveTunnelEnabled) return

        healthCheckJob = serviceScope.launch {
            healthCheckLoop(settings)
        }
    }

    private suspend fun healthCheckLoop(settings: SettingsManager) {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        while (currentCoroutineContext().isActive) {
            if (SingBoxManager.isRunning) {
                val url = settings.pingTestUrl
                try {
                    val proxyTag = java.net.URLEncoder.encode(SingBoxManager.primaryProxyTag, "UTF-8")
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val checkUrl = "http://127.0.0.1:9092/proxies/$proxyTag/delay?url=$encodedUrl&timeout=10000"
                    val secret = SingBoxManager.clashSecret
                    val request = okhttp3.Request.Builder()
                        .url(checkUrl)
                        .apply {
                            if (secret.isNotEmpty()) {
                                header("Authorization", "Bearer $secret")
                            }
                        }
                        .build()

                    var isWorking = false
                    withContext(Dispatchers.IO) {
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body.string()
                                val delay = org.json.JSONObject(body).optInt("delay", -1)
                                if (delay > 0) isWorking = true
                            }
                        }
                    }

                    if (!isWorking) {
                        Log.w(TAG, "Health check failed: proxy returned timeout or error")
                        startRecovery()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Health check failed: could not reach Clash API", e)
                    startRecovery()
                }
            }
            delay(20_000L)
        }
    }

    
    
    

    private suspend fun recoveryLoop() {
        Log.i(TAG, "Starting adaptive tunnel recovery...")
        val settings = SettingsManager(context)

        val selectedId = withContext(Dispatchers.IO) {
            repository.getSelectedProfile()?.id
        } ?: run {
            Log.w(TAG, "Recovery: no selected profile, aborting")
            return
        }

        
        onRestartVpn()

        
        val connectDeadline = SystemClock.elapsedRealtime() + 12_000L
        while (SystemClock.elapsedRealtime() < connectDeadline) {
            if (SingBoxManager.isRunning) break
            delay(500)
        }

        if (SingBoxManager.isRunning) {
            delay(2_000)
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val isWorking = withContext(Dispatchers.IO) {
                try {
                    val proxyTag = java.net.URLEncoder.encode(SingBoxManager.primaryProxyTag, "UTF-8")
                    val encodedUrl = java.net.URLEncoder.encode(settings.pingTestUrl, "UTF-8")
                    val checkUrl = "http://127.0.0.1:9092/proxies/$proxyTag/delay?url=$encodedUrl&timeout=10000"
                    val secret = SingBoxManager.clashSecret
                    val request = okhttp3.Request.Builder()
                        .url(checkUrl)
                        .apply { if (secret.isNotEmpty()) header("Authorization", "Bearer $secret") }
                        .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val delay = org.json.JSONObject(response.body.string()).optInt("delay", -1)
                            delay > 0
                        } else false
                    }
                } catch (e: Exception) { false }
            }

            if (isWorking) {
                Log.i(TAG, "Recovery successful with current profile.")
                return
            }
        }

        
        Log.i(TAG, "Current profile failed. Finding best alternative...")
        val allProfiles = withContext(Dispatchers.IO) { repository.getAllProfiles().first() }
        val currentProfile = allProfiles.find { it.id == selectedId } ?: return
        val subId = currentProfile.subscriptionId ?: return
        val candidates = allProfiles.filter { it.subscriptionId == subId }
        if (candidates.size <= 1) {
            Log.w(TAG, "No alternative profiles in subscription.")
            return
        }

        val bestId = selectBestProfile(selectedId)

        if (bestId != null && bestId != selectedId) {
            Log.i(TAG, "Switching to best profile: $bestId during recovery.")
            onProfileSelected(bestId)
            delay(500)
            onRestartVpn()
        } else {
            Log.w(TAG, "No working alternative found in current subscription.")
        }
    }

    
    
    

    private fun startBestProfileJob() {
        val settings = SettingsManager(context)
        bestProfileJob?.cancel()
        if (!settings.isBestProfileEnabled) return

        bestProfileJob = serviceScope.launch {
            bestProfileLoop(settings)
        }
    }

    private suspend fun bestProfileLoop(settings: SettingsManager) {
        val rawInterval = settings.bestProfileInterval.toLongOrNull() ?: 1800L
        val intervalMs = maxOf(rawInterval, 60L) * 1000L

        while (currentCoroutineContext().isActive) {
            val lastRun = settings.lastBestProfileRunTime
            val now = System.currentTimeMillis()
            val waitMs = (intervalMs - (now - lastRun)).coerceAtLeast(0L)
            if (waitMs > 0) delay(waitMs)

            if (!currentCoroutineContext().isActive || !settings.isBestProfileEnabled) return

            val shouldRun = if (settings.isBestProfileOnlyIfConnected) SingBoxManager.isRunning else true

            if (shouldRun) {
                settings.lastBestProfileRunTime = System.currentTimeMillis()

                val currentSelectedId = withContext(Dispatchers.IO) {
                    repository.getSelectedProfile()?.id
                }
                if (currentSelectedId != null) {
                    val bestId = selectBestProfile(currentSelectedId)
                    if (bestId != null && bestId != currentSelectedId) {
                        Log.i(TAG, "Best profile job: switching from $currentSelectedId to $bestId")
                        onProfileSelected(bestId)
                        if (SingBoxManager.isRunning) {
                            delay(300)
                            onRestartVpn()
                        }
                    }
                }
            } else {
                settings.lastBestProfileRunTime = System.currentTimeMillis()
            }
        }
    }

    
    private suspend fun selectBestProfile(currentSelectedId: Long): Long? {
        val settings = SettingsManager(context)
        val allProfiles = withContext(Dispatchers.IO) { repository.getAllProfiles().first() }
        val selectedProfile = allProfiles.find { it.id == currentSelectedId } ?: return null
        val subId = selectedProfile.subscriptionId

        val profiles = allProfiles.filter { it.subscriptionId == subId }
        if (profiles.size <= 1) return null

        val localPings = java.util.concurrent.ConcurrentHashMap<Long, PingState>()
        val loadingPings = profiles.associate { it.id to PingState.Loading }
        PingHelper.updatePingStates(loadingPings)

        val fullProfiles = withContext(Dispatchers.IO) {
            repository.getProfilesByIds(profiles.map { it.id })
        }
        val isProxy = settings.pingType.startsWith("via")

        if (!isProxy) {
            val method = if (settings.pingType == "TCP") "TCP" else "ICMP"
            supervisorScope {
                fullProfiles.forEach { profile ->
                    launch(Dispatchers.IO) {
                        try {
                            val (latency, error) = PingHelper.pingDirect(profile, method, settings.pingTimeout)
                            val state = PingState.Result(latency, latency < 0, error)
                            localPings[profile.id] = state
                            PingHelper.updatePingState(profile.id, state)
                        } catch (e: Exception) {
                            val state = PingState.Result(-1, true, e.message ?: "unknown error")
                            localPings[profile.id] = state
                            PingHelper.updatePingState(profile.id, state)
                        }
                    }
                }
            }
        } else {
            if (SingBoxManager.isRunning) {
                supervisorScope {
                    PingHelper.pingProxyBatch(
                        context = context,
                        profiles = fullProfiles,
                        testUrl = settings.pingTestUrl,
                        timeoutSec = settings.pingTimeout
                    ) { id, latency, error ->
                        val state = PingState.Result(latency, latency < 0, error)
                        localPings[id] = state
                        PingHelper.updatePingState(id, state)
                    }
                }
            } else {
                supervisorScope {
                    fullProfiles.forEach { profile ->
                        launch(Dispatchers.IO) {
                            try {
                                val (latency, error) = PingHelper.pingDirect(profile, "TCP", settings.pingTimeout)
                                val state = PingState.Result(latency, latency < 0, error)
                                localPings[profile.id] = state
                                PingHelper.updatePingState(profile.id, state)
                            } catch (e: Exception) {
                                val state = PingState.Result(-1, true, e.message ?: "unknown error")
                                localPings[profile.id] = state
                                PingHelper.updatePingState(profile.id, state)
                            }
                        }
                    }
                }
            }
        }

        
        val deadline = SystemClock.elapsedRealtime() + 15_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (profiles.all { localPings[it.id] is PingState.Result }) break
            delay(500)
        }

        val best = profiles
            .mapNotNull { p ->
                val state = localPings[p.id]
                if (state is PingState.Result && !state.isError && state.latency >= 0) p to state.latency
                else null
            }
            .minByOrNull { it.second }
            ?.first

        return if (best != null && best.id != currentSelectedId) best.id else null
    }
}
