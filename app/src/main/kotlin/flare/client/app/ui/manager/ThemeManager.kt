package flare.client.app.ui.manager

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import flare.client.app.R
import flare.client.app.data.SettingsManager
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.notification.NotificationType
import flare.client.app.ui.i18n.I18n

class ThemeManager(
    private val activity: AppCompatActivity,
    private val settings: SettingsManager,
    private val onAccentChanged: (Int, Int) -> Unit
) {

    var gradientAnimator: ValueAnimator? = null
    var themeContentView: View? = null

    companion object {
        var lastUiMode: Int = -1
        var themeChangedJustNow: Boolean = false
        var lastThemeChangeTime: Long = 0

        const val COLOR_DEFAULT     = 0xFF5B8CFF.toInt()
        const val COLOR_DEFAULT_END = 0xFF5B8CFF.toInt()
        const val COLOR_GREEN       = 0xFF34C759.toInt()
        const val COLOR_GREEN_END   = 0xFF25A244.toInt()
        const val COLOR_PURPLE      = 0xFF9B59B6.toInt()
        const val COLOR_PURPLE_END  = 0xFFBF8FFF.toInt()
        const val COLOR_RED         = 0xFFFF453A.toInt()
        const val COLOR_RED_END     = 0xFFFF6B6B.toInt()
        const val COLOR_PINK        = 0xFFFF375F.toInt()
        const val COLOR_PINK_END    = 0xFFFF6FA1.toInt()
        const val COLOR_ORANGE      = 0xFFFF9F0A.toInt()
        const val COLOR_ORANGE_END  = 0xFFFFB340.toInt()
        const val COLOR_INDIGO      = 0xFF5E5CE6.toInt()
        const val COLOR_INDIGO_END  = 0xFF7D7AFF.toInt()
        const val COLOR_CYAN        = 0xFF64D2FF.toInt()
        const val COLOR_CYAN_END    = 0xFF5AC8FA.toInt()
        const val COLOR_AMBER       = 0xFFFFD60A.toInt()
        const val COLOR_AMBER_END   = 0xFFFFE082.toInt()
        const val COLOR_VIOLET      = 0xFFBF5AF2.toInt()
        const val COLOR_VIOLET_END  = 0xFFD6A0FF.toInt()
        const val COLOR_TEAL        = 0xFF30B0C7.toInt()
        const val COLOR_TEAL_END    = 0xFF62D2E4.toInt()
        
        const val COLOR_MATERIAL_YOU     = COLOR_DEFAULT
        const val COLOR_MATERIAL_YOU_END = COLOR_DEFAULT_END
    }


    fun applyBackgroundGradient() {
        
    }

    fun startGradientAnimation() {
        
    }

    fun stopGradientAnimation() {
        
    }

    fun updateThemeValueUI() {
    }

    fun applyTheme() {
        val mode = when (settings.themeMode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun updateSystemBars(isDark: Boolean) {
        val window = activity.window
        val decorView = window.decorView
        androidx.core.view.WindowCompat.getInsetsController(window, decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
    }

    fun getColorsForKey(key: String): Pair<Int, Int> = when (key) {
        "material_you" -> getMaterialYouColors()
        "green"  -> Pair(COLOR_GREEN,  COLOR_GREEN_END)
        "purple" -> Pair(COLOR_PURPLE, COLOR_PURPLE_END)
        "red"    -> Pair(COLOR_RED,    COLOR_RED_END)
        "pink"   -> Pair(COLOR_PINK,   COLOR_PINK_END)
        "orange" -> Pair(COLOR_ORANGE, COLOR_ORANGE_END)
        "indigo" -> Pair(COLOR_INDIGO, COLOR_INDIGO_END)
        "cyan"   -> Pair(COLOR_CYAN,   COLOR_CYAN_END)
        "amber"  -> Pair(COLOR_AMBER,  COLOR_AMBER_END)
        "violet" -> Pair(COLOR_VIOLET, COLOR_VIOLET_END)
        "teal"   -> Pair(COLOR_TEAL,   COLOR_TEAL_END)
        else     -> Pair(COLOR_DEFAULT, COLOR_DEFAULT_END)
    }

    private fun getMaterialYouColors(): Pair<Int, Int> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val primary   = activity.resources.getColor(android.R.color.system_accent1_500, activity.theme)
            val secondary = activity.resources.getColor(android.R.color.system_accent2_500, activity.theme)
            Pair(primary, secondary)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            try {
                val wm = android.app.WallpaperManager.getInstance(activity)
                val wc = wm.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
                val primary   = wc?.primaryColor?.toArgb()   ?: COLOR_DEFAULT
                val secondary = wc?.secondaryColor?.toArgb() ?: COLOR_DEFAULT_END
                Pair(primary, secondary)
            } catch (e: Exception) {
                Pair(COLOR_DEFAULT, COLOR_DEFAULT_END)
            }
        } else {
            Pair(COLOR_DEFAULT, COLOR_DEFAULT_END)
        }
    }

    fun animateAccentChange(fromAccent: Int, fromAccentEnd: Int, targetAccent: Int, targetAccentEnd: Int) {
        val evaluator = ArgbEvaluator()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                val blend    = evaluator.evaluate(f, fromAccent,    targetAccent)    as Int
                val blendEnd = evaluator.evaluate(f, fromAccentEnd, targetAccentEnd) as Int
                applyAccentColorsToUI(blend, blendEnd)
            }
            start()
        }
        applyAccentColorsToUI(targetAccent, targetAccentEnd)
    }

    fun applyAccentColorsToUI(accent: Int, accentEnd: Int) {
        onAccentChanged(accent, accentEnd)
    }
}

