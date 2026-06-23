package flare.client.app.singbox

import android.content.Context
import android.util.Log
import flare.client.app.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SingBoxTrafficMonitor {

    private const val TAG = "SingBoxTrafficMonitor"

    private var trafficJob: Job? = null
    @Volatile private var currentUpSpeed: Long = 0L
    @Volatile private var currentDownSpeed: Long = 0L
    @Volatile private var activeCall: okhttp3.Call? = null
    private val trafficScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startTrafficStream(context: Context, clashSecret: String) {
        val settings = SettingsManager(context)
        if (!settings.isStatusNotificationEnabled || !settings.isNotificationSpeedEnabled) {
            stopTrafficStream()
            return
        }
        if (trafficJob != null) return

        stopTrafficStream()
        trafficJob = trafficScope.launch {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("http://127.0.0.1:9092/traffic")
                .apply {
                    if (clashSecret.isNotEmpty()) {
                        header("Authorization", "Bearer $clashSecret")
                    }
                }
                .build()

            var attempt = 0
            while (isActive) {
                try {
                    val call = client.newCall(request)
                    activeCall = call
                    call.execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val body = response.body
                        val reader = body.charStream().buffered()
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            if (line.isNotBlank()) {
                                try {
                                    val obj = JSONObject(line)
                                    currentUpSpeed = obj.optLong("up", 0L)
                                    currentDownSpeed = obj.optLong("down", 0L)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing traffic JSON: ${e.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    Log.w(TAG, "Traffic stream error (attempt ${++attempt}): ${e.message}")
                    currentUpSpeed = 0L
                    currentDownSpeed = 0L
                    delay(2000)
                } finally {
                    activeCall = null
                }
            }
        }
    }

    fun stopTrafficStream() {
        trafficJob?.cancel()
        trafficJob = null
        try {
            activeCall?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling active traffic call: ${e.message}")
        }
        activeCall = null
        currentUpSpeed = 0L
        currentDownSpeed = 0L
    }

    fun getTraffic(callback: (Long, Long) -> Unit) {
        callback(currentUpSpeed, currentDownSpeed)
    }
}
