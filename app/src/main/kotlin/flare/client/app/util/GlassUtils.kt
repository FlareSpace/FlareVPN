package flare.client.app.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_DEFAULT
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_GREEN
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_PURPLE
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_RED
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_PINK
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_ORANGE
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_INDIGO
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_CYAN
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_AMBER
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_VIOLET
import flare.client.app.ui.manager.ThemeManager.Companion.COLOR_TEAL

object GlassUtils {

    data class MenuItem(val id: Int, val title: CharSequence, val onClick: () -> Unit)

    fun isNightMode(context: Context): Boolean {
        val settings = flare.client.app.data.SettingsManager(context)
        return when (settings.themeMode) {
            1 -> false
            2 -> true
            else -> (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }




    private fun getAccentColor(context: Context): Int? {
        val settings = flare.client.app.data.SettingsManager(context)
        if (!settings.isCustomColorEnabled) return null
        
        return when (settings.accentColorKey) {
            "material_you" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        context.getColor(android.R.color.system_accent1_500)
                    } catch (e: Exception) {
                        COLOR_DEFAULT
                    }
                } else {
                    COLOR_DEFAULT
                }
            }
            "green"  -> COLOR_GREEN
            "purple" -> COLOR_PURPLE
            "red"    -> COLOR_RED
            "pink"   -> COLOR_PINK
            "orange" -> COLOR_ORANGE
            "indigo" -> COLOR_INDIGO
            "cyan"   -> COLOR_CYAN
            "amber"  -> COLOR_AMBER
            "violet" -> COLOR_VIOLET
            "teal"   -> COLOR_TEAL
            else     -> COLOR_DEFAULT
        }
    }
}
