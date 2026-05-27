package flare.client.app.singbox

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object GeoFileManager {

    private const val TAG = "GeoFileManager"

    private const val GEOIP_MAIN_FILE   = "geoip-ru.srs"
    private const val GEOSITE_MAIN_FILE = "geosite-ru.srs"
    
    private const val GEOSITE_ADS_FILE = "geosite-ads.srs"
    private const val GEOIP_CN_FILE    = "geoip-cn.srs"
    private const val GEOSITE_CN_FILE  = "geosite-cn.srs"

    private val client = okhttp3.OkHttpClient()

    
    fun ensureGeoFiles(context: Context) {
        val filesDir = context.filesDir
        copyAssetIfNeeded(context, "geoip-ru.srs", File(filesDir, GEOIP_MAIN_FILE))
        copyAssetIfNeeded(context, "geosite-ru.srs", File(filesDir, GEOSITE_MAIN_FILE))
    }

    fun geoFilesExist(context: Context): Boolean {
        val dir = context.filesDir
        return File(dir, GEOIP_MAIN_FILE).exists() && File(dir, GEOSITE_MAIN_FILE).exists()
    }

    fun isFileDownloaded(context: Context, fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists() && file.length() > 0
    }

    fun downloadFile(
        context: Context,
        url: String,
        destName: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val request = okhttp3.Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onError(e.message ?: "Download failed")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        onError("Unexpected code $resp")
                        return
                    }

                    val body = resp.body
                    val contentLength = body.contentLength()
                    val destFile = File(context.filesDir, destName)
                    
                    try {
                        body.byteStream().use { input ->
                            FileOutputStream(destFile).use { output ->
                                val buffer = ByteArray(8192)
                                var totalRead = 0L
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    totalRead += read
                                    if (contentLength > 0) {
                                        val progress = ((totalRead * 100) / contentLength).toInt()
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                        onSuccess()
                    } catch (e: IOException) {
                        onError(e.message ?: "Save failed")
                    }
                }
            }
        })
    }

    private fun copyAssetIfNeeded(context: Context, assetName: String, dest: File) {
        if (dest.exists() && dest.length() > 0) {
            return
        }
        
        Log.i(TAG, "Copying $assetName from assets to ${dest.absolutePath}")
        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "$assetName copied successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy $assetName from assets", e)
        }
    }
}


