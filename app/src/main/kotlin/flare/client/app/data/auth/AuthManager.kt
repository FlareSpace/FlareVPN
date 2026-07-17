package flare.client.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import flare.client.app.data.SettingsManager
import flare.client.app.data.api.FlareBackendApi
import kotlinx.coroutines.delay

class AuthManager(private val context: Context, private val settingsManager: SettingsManager) {

    suspend fun startAuthFlow(): String? {
        val initResponse = FlareBackendApi.initAuth() ?: return null
        
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(initResponse.bot_url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        return initResponse.uuid
    }
    
    fun clearAnonymousFlag() {
        settingsManager.isAnonymousSession = false
    }

    suspend fun pollForToken(uuid: String, isBind: Boolean = false): Int { 
        val maxRetries = 60 
        var retries = 0

        while (retries < maxRetries) {
            val tokenToPass = if (isBind) settingsManager.jwtToken else null
            val statusResponse = FlareBackendApi.checkAuthStatus(uuid, tokenToPass)
            if (statusResponse != null) {
                if (statusResponse.status == "completed" && statusResponse.token != null) {
                    settingsManager.jwtToken = statusResponse.token
                    settingsManager.isAnonymousSession = false
                    return 0
                }
                if (statusResponse.status == "conflict") {
                    return 2
                }
            }
            delay(3000)
            retries++
        }
        return 1
    }
    
    suspend fun createAnonymousAccount(): String? {
        val response = FlareBackendApi.createAnonymousAccount()
        if (response.isSuccess) {
            val data = response.getOrNull()
            if (data != null) {
                settingsManager.jwtToken = data.token
                settingsManager.isAnonymousSession = true
                return data.auth_key
            }
        }
        return null
    }

    suspend fun loginAnonymous(authKey: String): Boolean {
        val response = FlareBackendApi.loginAnonymous(authKey)
        if (response.isSuccess) {
            val data = response.getOrNull()
            if (data != null && data.status == "completed" && data.token != null) {
                settingsManager.jwtToken = data.token
                settingsManager.isAnonymousSession = true
                return true
            }
        }
        return false
    }
    
    fun isLoggedIn(): Boolean {
        return settingsManager.jwtToken != null
    }
    
    fun isAnonymousSession(): Boolean {
        return settingsManager.isAnonymousSession
    }
    
    fun getToken(): String? {
        return settingsManager.jwtToken
    }
    
    fun logout() {
        settingsManager.jwtToken = null
        settingsManager.isAnonymousSession = false
    }
}
