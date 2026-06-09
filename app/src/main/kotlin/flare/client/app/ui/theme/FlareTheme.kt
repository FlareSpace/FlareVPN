package flare.client.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import flare.client.app.ui.manager.ThemeManager
@Immutable
data class FlareColors(
    val bgDark: Color,
    val bgSurface: Color,
    val bgItem: Color,
    val bgNavBar: Color,
    val bgNotificationBar: Color,
    val accent: Color,
    val accentEnd: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val white: Color,
    val glassStroke: Color,
    val dividerColor: Color,
    val menuTextColor: Color,
    val navIconTint: Color,
    val connectedGreen: Color,
    val disconnectedRed: Color,
    val dialogGlassFill: Color,
    val dialogGlassStroke: Color,
    val bgProfileSelected: Color,
    val textProfileSelectedPrimary: Color,
    val textProfileSelectedSecondary: Color,
    val dividerProfileSelected: Color,
    
    
    val btnConnectRimStart: Color,
    val btnConnectRimEnd: Color,
    val btnConnectBodyStart: Color,
    val btnConnectBodyCenter: Color,
    val btnConnectBodyEnd: Color,
    val btnConnectIconTint: Color,
    val gradientBase: Color,
    val gradientBlueStart: Color,
    val gradientBlueEnd: Color,
    val gradientPurpleStart: Color,
    val gradientPurpleEnd: Color,
    val gradientMagentaStart: Color,
    val gradientMagentaEnd: Color,
    val gradientCyanStart: Color,
    val gradientCyanEnd: Color,
    val gradientWhiteStart: Color,
    val gradientWhiteEnd: Color,
    val glassInputBg: Color,
    val infoBg: Color,
    val infoStroke: Color,
    val trafficTextColor: Color,

    val headerGradientStart: Color,
    val headerGradientEnd: Color,

    val isDark: Boolean
)

val LocalFlareColors = staticCompositionLocalOf {
    FlareColors(
        bgDark = Color(0xFFF3F5F8),
        bgSurface = Color(0xFFF4F6F9),
        bgItem = Color(0xFFFFFFFF),
        bgNavBar = Color(0xCCF0F2F5),
        bgNotificationBar = Color(0xFFF4F6F9),
        accent = Color(0xFF5B8CFF),
        accentEnd = Color(0xFF5B8CFF),
        textPrimary = Color(0xFF1A1C1E),
        textSecondary = Color(0xFF707991),
        white = Color(0xFFFFFFFF),
        glassStroke = Color(0x1A000000),
        dividerColor = Color(0x18000000),
        menuTextColor = Color(0xFF1A1C1E),
        navIconTint = Color(0xFF4A5568),
        connectedGreen = Color(0xFF30D158),
        disconnectedRed = Color(0xFFFF453A),
        dialogGlassFill = Color(0x4DFFFFFF),
        dialogGlassStroke = Color(0x1A000000),
        bgProfileSelected = Color(0xFFD8DEE9),
        textProfileSelectedPrimary = Color(0xFF1A1C1E),
        textProfileSelectedSecondary = Color(0xFF707991),
        dividerProfileSelected = Color(0x18000000),
        
        btnConnectRimStart = Color(0xFFD1D5DB),
        btnConnectRimEnd = Color(0xFFF3F4F6),
        btnConnectBodyStart = Color(0xFFF9FAFB),
        btnConnectBodyCenter = Color(0xFFE5E7EB),
        btnConnectBodyEnd = Color(0xFFD1D5DB),
        btnConnectIconTint = Color(0xFF707991),
        gradientBase = Color(0xFFE8EBF0),
        gradientBlueStart = Color(0x355B8CFF),
        gradientBlueEnd = Color(0x005B8CFF),
        gradientPurpleStart = Color(0x35A066FF),
        gradientPurpleEnd = Color(0x00A066FF),
        gradientMagentaStart = Color(0x25FF4081),
        gradientMagentaEnd = Color(0x00FF4081),
        gradientCyanStart = Color(0x2500E5FF),
        gradientCyanEnd = Color(0x0000E5FF),
        gradientWhiteStart = Color(0x80FFFFFF),
        gradientWhiteEnd = Color(0x00FFFFFF),
        glassInputBg = Color(0x1A000000),
        infoBg = Color(0x1A000000),
        infoStroke = Color(0x26000000),
        trafficTextColor = Color(0xFFFFFFFF),

        headerGradientStart = Color(0x30FFFFFF),
        headerGradientEnd = Color(0x20FFFFFF),

        isDark = false
    )
}

@Composable
private fun animatedColor(targetValue: Color): Color {
    return androidx.compose.animation.animateColorAsState(
        targetValue = targetValue,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "ThemeColorTransition"
    ).value
}

@Composable
fun FlareTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    accentColor: Color = Color(ThemeManager.COLOR_DEFAULT),
    accentEndColor: Color = Color(ThemeManager.COLOR_DEFAULT_END),
    content: @Composable () -> Unit
) {
    val targetColors = if (isDark) {
        FlareColors(
            bgDark = Color(0xFF000000),
            bgSurface = Color(0xFF0F0F11),
            bgItem = Color(0xFF1C1C1E),
            bgNavBar = Color(0x951C1C1E),
            bgNotificationBar = Color(0xFF1C1C1E),
            accent = accentColor,
            accentEnd = accentEndColor,
            textPrimary = Color(0xFFE8ECF4),
            textSecondary = Color(0xFF8E9099),
            white = Color(0xFFFFFFFF),
            glassStroke = Color(0x26FFFFFF),
            dividerColor = Color(0x0DFFFFFF),
            menuTextColor = Color(0xFFFFFFFF),
            navIconTint = Color(0xFFFFFFFF),
            connectedGreen = Color(0xFF30D158),
            disconnectedRed = Color(0xFFFF453A),
            dialogGlassFill = Color(0xFF1A1A1E).copy(alpha = 0.08f),
            dialogGlassStroke = Color(0x20FFFFFF),
            bgProfileSelected = Color(0xFF2C2C2E),
            textProfileSelectedPrimary = Color(0xFFFFFFFF),
            textProfileSelectedSecondary = Color(0xFFB0BDD1),
            dividerProfileSelected = Color(0x26FFFFFF),
            
            btnConnectRimStart = Color(0xFF0A0C1C),
            btnConnectRimEnd = Color(0xFF3F4678),
            btnConnectBodyStart = Color(0xFF333866),
            btnConnectBodyCenter = Color(0xFF1B1E38),
            btnConnectBodyEnd = Color(0xFF0B0D19),
            btnConnectIconTint = Color(0xFFA0A5D6),
            gradientBase = Color(0xFF000000),
            gradientBlueStart = Color(0x401565C0),
            gradientBlueEnd = Color(0x001565C0),
            gradientPurpleStart = Color(0x406A1B9A),
            gradientPurpleEnd = Color(0x006A1B9A),
            gradientMagentaStart = Color(0x30D81B60),
            gradientMagentaEnd = Color(0x00D81B60),
            gradientCyanStart = Color(0x2500ACC1),
            gradientCyanEnd = Color(0x0000ACC1),
            gradientWhiteStart = Color(0x15FFFFFF),
            gradientWhiteEnd = Color(0x00FFFFFF),
            glassInputBg = Color(0x1AFFFFFF),
            infoBg = Color(0x26FFFFFF),
            infoStroke = Color(0x33FFFFFF),
            trafficTextColor = Color(0xFFE8ECF4),

            headerGradientStart = Color(0xB01C1C1E),
            headerGradientEnd = Color(0x8A000000),

            isDark = true
        )
    } else {
        FlareColors(
            bgDark = Color(0xFFF6F8FC),
            bgSurface = Color(0xFFF9FAFC),
            bgItem = Color(0xFFFFFFFF),
            bgNavBar = Color(0xDDF1F4FA),
            bgNotificationBar = Color(0xFFF9FAFC),
            accent = accentColor,
            accentEnd = accentEndColor,
            textPrimary = Color(0xFF1A1C1E),
            textSecondary = Color(0xFF707991),
            white = Color(0xFFFFFFFF),
            glassStroke = Color(0x0C000000),
            dividerColor = Color(0x0A000000),
            menuTextColor = Color(0xFF1A1C1E),
            navIconTint = Color(0xFF4A5568),
            connectedGreen = Color(0xFF30D158),
            disconnectedRed = Color(0xFFFF453A),
            dialogGlassFill = Color(0xFFFFFFFF).copy(alpha = 0.08f),
            dialogGlassStroke = Color(0x1A000000),
            bgProfileSelected = Color(0xFFE2EAFD),
            textProfileSelectedPrimary = Color(0xFF1A1C1E),
            textProfileSelectedSecondary = Color(0xFF707991),
            dividerProfileSelected = Color(0x0A000000),
            
            btnConnectRimStart = Color(0xFFD1D5DB),
            btnConnectRimEnd = Color(0xFFF3F4F6),
            btnConnectBodyStart = Color(0xFFF9FAFB),
            btnConnectBodyCenter = Color(0xFFE5E7EB),
            btnConnectBodyEnd = Color(0xFFD1D5DB),
            btnConnectIconTint = Color(0xFF707991),
            gradientBase = Color(0xFFF3F5FA),
            gradientBlueStart = Color(0x245B8CFF),
            gradientBlueEnd = Color(0x005B8CFF),
            gradientPurpleStart = Color(0x20A066FF),
            gradientPurpleEnd = Color(0x00A066FF),
            gradientMagentaStart = Color(0x1CEB4B93),
            gradientMagentaEnd = Color(0x00EB4B93),
            gradientCyanStart = Color(0x1C00E5FF),
            gradientCyanEnd = Color(0x0000E5FF),
            gradientWhiteStart = Color(0x70FFFFFF),
            gradientWhiteEnd = Color(0x00FFFFFF),
            glassInputBg = Color(0x0F000000),
            infoBg = Color(0x1A000000),
            infoStroke = Color(0x26000000),
            trafficTextColor = Color(0xFFFFFFFF),

            headerGradientStart = Color(0x30FFFFFF),
            headerGradientEnd = Color(0x20FFFFFF),

            isDark = false
        )
    }
    
    val colors = FlareColors(
        bgDark = animatedColor(targetColors.bgDark),
        bgSurface = animatedColor(targetColors.bgSurface),
        bgItem = animatedColor(targetColors.bgItem),
        bgNavBar = animatedColor(targetColors.bgNavBar),
        bgNotificationBar = animatedColor(targetColors.bgNotificationBar),
        accent = animatedColor(targetColors.accent),
        accentEnd = animatedColor(targetColors.accentEnd),
        textPrimary = animatedColor(targetColors.textPrimary),
        textSecondary = animatedColor(targetColors.textSecondary),
        white = animatedColor(targetColors.white),
        glassStroke = animatedColor(targetColors.glassStroke),
        dividerColor = animatedColor(targetColors.dividerColor),
        menuTextColor = animatedColor(targetColors.menuTextColor),
        navIconTint = animatedColor(targetColors.navIconTint),
        connectedGreen = animatedColor(targetColors.connectedGreen),
        disconnectedRed = animatedColor(targetColors.disconnectedRed),
        dialogGlassFill = animatedColor(targetColors.dialogGlassFill),
        dialogGlassStroke = animatedColor(targetColors.dialogGlassStroke),
        bgProfileSelected = animatedColor(targetColors.bgProfileSelected),
        textProfileSelectedPrimary = animatedColor(targetColors.textProfileSelectedPrimary),
        textProfileSelectedSecondary = animatedColor(targetColors.textProfileSelectedSecondary),
        dividerProfileSelected = animatedColor(targetColors.dividerProfileSelected),
        btnConnectRimStart = animatedColor(targetColors.btnConnectRimStart),
        btnConnectRimEnd = animatedColor(targetColors.btnConnectRimEnd),
        btnConnectBodyStart = animatedColor(targetColors.btnConnectBodyStart),
        btnConnectBodyCenter = animatedColor(targetColors.btnConnectBodyCenter),
        btnConnectBodyEnd = animatedColor(targetColors.btnConnectBodyEnd),
        btnConnectIconTint = animatedColor(targetColors.btnConnectIconTint),
        gradientBase = animatedColor(targetColors.gradientBase),
        gradientBlueStart = animatedColor(targetColors.gradientBlueStart),
        gradientBlueEnd = animatedColor(targetColors.gradientBlueEnd),
        gradientPurpleStart = animatedColor(targetColors.gradientPurpleStart),
        gradientPurpleEnd = animatedColor(targetColors.gradientPurpleEnd),
        gradientMagentaStart = animatedColor(targetColors.gradientMagentaStart),
        gradientMagentaEnd = animatedColor(targetColors.gradientMagentaEnd),
        gradientCyanStart = animatedColor(targetColors.gradientCyanStart),
        gradientCyanEnd = animatedColor(targetColors.gradientCyanEnd),
        gradientWhiteStart = animatedColor(targetColors.gradientWhiteStart),
        gradientWhiteEnd = animatedColor(targetColors.gradientWhiteEnd),
        glassInputBg = animatedColor(targetColors.glassInputBg),
        infoBg = animatedColor(targetColors.infoBg),
        infoStroke = animatedColor(targetColors.infoStroke),
        trafficTextColor = animatedColor(targetColors.trafficTextColor),
        headerGradientStart = animatedColor(targetColors.headerGradientStart),
        headerGradientEnd = animatedColor(targetColors.headerGradientEnd),
        isDark = isDark
    )

    val textSelectionColors = remember(colors.accent) {
        TextSelectionColors(
            handleColor = colors.accent,
            backgroundColor = colors.accent.copy(alpha = 0.4f)
        )
    }

    CompositionLocalProvider(
        LocalFlareColors provides colors,
        LocalTextSelectionColors provides textSelectionColors,
        content = content
    )
}

object FlareTheme {
    val colors: FlareColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFlareColors.current
}
