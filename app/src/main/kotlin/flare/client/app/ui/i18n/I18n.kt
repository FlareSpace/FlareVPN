package flare.client.app.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object I18n {
    var strings: FlareStrings by mutableStateOf(RuFlareStrings)

    fun updateLocale(locale: String) {
        val lang = if (locale.lowercase() == "auto") {
            java.util.Locale.getDefault().language
        } else {
            locale.lowercase()
        }
        
        strings = when (lang) {
            "ru" -> RuFlareStrings
            "en" -> EnFlareStrings
            else -> EnFlareStrings
        }
    }

    fun isMyServers(name: String?): Boolean {
        if (name == null) return false
        return name == RuFlareStrings.sub_my_servers || name == EnFlareStrings.sub_my_servers
    }
}
