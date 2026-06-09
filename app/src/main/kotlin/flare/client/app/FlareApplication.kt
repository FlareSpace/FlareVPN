package flare.client.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import flare.client.app.data.SettingsManager
import okio.Path.Companion.toOkioPath

class FlareApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory())
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(context, 0.10)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("coil_image_cache").toOkioPath())
                        .maxSizeBytes(50L * 1024 * 1024) 
                        .build()
                }
                .crossfade(true)
                .build()
        }

        val settings = SettingsManager(this)
        val mode = when (settings.themeMode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        
        if (settings.isAppTriggerEnabled) {
            val intent = android.content.Intent(this, flare.client.app.service.AppMonitorService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
