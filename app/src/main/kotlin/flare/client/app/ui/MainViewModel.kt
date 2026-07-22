package flare.client.app.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

class MainViewModel(application: Application) : AndroidViewModel(application) {

    data class NoticeState(
        val id: Int = 0,
        val needsToShow: Boolean = false,
        val titleRu: String = "",
        val titleEn: String = "",
        val textRu: String = "",
        val textEn: String = "",
        val actionTextRu: String = "",
        val actionTextEn: String = "",
        val actionUrl: String = ""
    )

    private val _noticeState = MutableStateFlow(NoticeState())
    val noticeState: StateFlow<NoticeState> = _noticeState.asStateFlow()

    private var updateCheckJob: kotlinx.coroutines.Job? = null
    private var noticeCheckJob: kotlinx.coroutines.Job? = null

    private val initMutex = Mutex()
    @Volatile
    private var isInitialized = false

    fun initializeAsync() {
        if (isInitialized) return
        viewModelScope.launch {
            ensureInitialized()
        }
    }

    private suspend fun ensureInitialized() {
        if (isInitialized) return
        initMutex.withLock {
            if (isInitialized) return@withLock
            startNoticeCheckJob()
            startUpdateCheckJob()
            isInitialized = true
        }
    }

    private fun startUpdateCheckJob() {
        val app = getApplication<Application>()
        updateCheckJob?.cancel()

        updateCheckJob = viewModelScope.launch(Dispatchers.IO) {
            val settings = SettingsManager(app)
            if (!settings.isUpdateCheckEnabled) return@launch

            val startupDelay = (15L + (Math.random() * 45).toLong()) * 1000L
            delay(startupDelay)

            if (isActive) {
                flare.client.app.util.VersionManager.checkUpdates(app)
                settings.lastUpdateCheckTime = System.currentTimeMillis()
            }

            while (isActive) {
                val intervalMs = when (settings.updateCheckFrequency) {
                    "daily" -> 24 * 3600 * 1000L
                    "weekly" -> 7 * 24 * 3600 * 1000L
                    "monthly" -> 30 * 24 * 3600 * 1000L
                    else -> 24 * 3600 * 1000L
                }
                delay(intervalMs)

                if (isActive) {
                    flare.client.app.util.VersionManager.checkUpdates(app)
                    settings.lastUpdateCheckTime = System.currentTimeMillis()
                }
            }
        }
    }

    private fun startNoticeCheckJob() {
        val app = getApplication<Application>()
        noticeCheckJob?.cancel()

        noticeCheckJob = viewModelScope.launch(Dispatchers.IO) {
            val settings = SettingsManager(app)
            while (isActive) {
                try {
                    checkNoticeFromServer(app, settings)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error checking notice from server", e)
                }
                delay(3600 * 1000L) 
            }
        }
    }

    private suspend fun checkNoticeFromServer(context: Context, settings: SettingsManager) {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val url = "https://raw.githubusercontent.com/FlareSpace/flareVPN/refs/heads/main/notice/notice.json"
        val requestBuilder = okhttp3.Request.Builder().url(url)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    val json = org.json.JSONObject(body)

                    val id = json.optInt("id", 0)
                    val show = json.optBoolean("show", false)
                    val minVersionCode = json.optInt("min_version_code", 0)
                    val maxVersionCode = json.optInt("max_version_code", Int.MAX_VALUE)

                    val currentVersionCode = flare.client.app.BuildConfig.VERSION_CODE

                    if (show && id > settings.lastReadNoticeId &&
                        currentVersionCode >= minVersionCode &&
                        currentVersionCode <= maxVersionCode) {

                        val titleObj = json.optJSONObject("title")
                        val titleRu = titleObj?.optString("ru") ?: (json.optString("title").takeIf { it.isNotBlank() } ?: "Объявление")
                        val titleEn = titleObj?.optString("en") ?: (json.optString("title").takeIf { it.isNotBlank() } ?: "Announcement")

                        val textObj = json.optJSONObject("text")
                        val textRu = textObj?.optString("ru") ?: json.optString("text")
                        val textEn = textObj?.optString("en") ?: json.optString("text")

                        val actionTextObj = json.optJSONObject("action_text")
                        val actionTextRu = actionTextObj?.optString("ru") ?: (json.optString("action_text").takeIf { it.isNotBlank() } ?: "Понятно")
                        val actionTextEn = actionTextObj?.optString("en") ?: (json.optString("action_text").takeIf { it.isNotBlank() } ?: "Got it")

                        val actionUrl = json.optString("action_url", "")

                        settings.noticeId = id
                        settings.noticeTitleRu = titleRu
                        settings.noticeTitleEn = titleEn
                        settings.noticeTextRu = textRu
                        settings.noticeTextEn = textEn
                        settings.noticeActionTextRu = actionTextRu
                        settings.noticeActionTextEn = actionTextEn
                        settings.noticeActionUrl = actionUrl
                        settings.needsToShowNotice = true

                        withContext(Dispatchers.Main) {
                            _noticeState.value = NoticeState(
                                id = id,
                                needsToShow = true,
                                titleRu = titleRu,
                                titleEn = titleEn,
                                textRu = textRu,
                                textEn = textEn,
                                actionTextRu = actionTextRu,
                                actionTextEn = actionTextEn,
                                actionUrl = actionUrl
                            )
                        }
                    } else if (!show || id <= settings.lastReadNoticeId) {
                        settings.needsToShowNotice = false
                        withContext(Dispatchers.Main) {
                            if (_noticeState.value.needsToShow) {
                                _noticeState.value = _noticeState.value.copy(needsToShow = false)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to check remote notice config: ${e.message}")
        }
    }

    fun dismissNotice() {
        val app = getApplication<Application>()
        val settings = SettingsManager(app)
        val currentNotice = _noticeState.value
        settings.lastReadNoticeId = currentNotice.id
        settings.needsToShowNotice = false
        _noticeState.value = _noticeState.value.copy(needsToShow = false)
    }
}
