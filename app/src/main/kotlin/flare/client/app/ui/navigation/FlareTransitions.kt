package flare.client.app.ui.navigation

import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

internal const val ROOT_TAB_EXIT_DURATION = 300
internal const val ROOT_TAB_ENTER_DURATION = 300
internal const val SETTINGS_EXIT_DURATION = 300
internal const val SETTINGS_ENTER_DURATION = 300
internal const val ROOT_TAB_BLUR = 0f
internal const val SETTINGS_BLUR = 25f
internal const val MORPH_DURATION = 450

internal fun rootTabIndexForRoute(route: String?): Int? = when {
    route == Destination.Settings.route || route?.startsWith("settings/") == true -> 0
    route == Destination.Home.route -> 1
    route == Destination.Servers.route -> 2
    else -> null
}

internal fun isSettingsDetailRoute(route: String?): Boolean = when (route) {
    Destination.AdvancedSettings.route,
    Destination.PingSettings.route,
    Destination.RoutingSettings.route,
    Destination.BasicSettings.route,
    Destination.SubscriptionsSettings.route,
    Destination.VpnSubscription.route,
    Destination.ThemeSettings.route,
    Destination.LanguageSettings.route,
    Destination.Journal.route -> true
    else -> false
}

internal fun isSettingsRoute(route: String?): Boolean =
    route == Destination.Settings.route || isSettingsDetailRoute(route)

internal fun isEditorRoute(route: String?): Boolean =
    route?.startsWith("editor/") == true

internal fun NavBackStackEntry.route(): String? = destination.route

internal fun rootTabEnterTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
    isLandscape: Boolean
): EnterTransition {
    val fromIndex = rootTabIndexForRoute(initial.route()) ?: return EnterTransition.None
    val toIndex = rootTabIndexForRoute(target.route()) ?: return EnterTransition.None
    if (fromIndex == toIndex) return EnterTransition.None

    val direction = if (toIndex > fromIndex) 1 else -1
    return if (isLandscape) {
        slideInVertically(
            animationSpec = tween(
                durationMillis = ROOT_TAB_ENTER_DURATION,
                easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
            ),
            initialOffsetY = { fullHeight -> fullHeight * direction }
        ) + fadeIn(animationSpec = tween(durationMillis = ROOT_TAB_ENTER_DURATION))
    } else {
        slideInHorizontally(
            animationSpec = tween(
                durationMillis = ROOT_TAB_ENTER_DURATION,
                easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
            ),
            initialOffsetX = { fullWidth -> fullWidth * direction }
        ) + fadeIn(animationSpec = tween(durationMillis = ROOT_TAB_ENTER_DURATION))
    }
}

internal fun rootTabExitTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
    isLandscape: Boolean
): ExitTransition {
    val fromIndex = rootTabIndexForRoute(initial.route()) ?: return ExitTransition.None
    val toIndex = rootTabIndexForRoute(target.route()) ?: return ExitTransition.None
    if (fromIndex == toIndex) return ExitTransition.None

    val direction = if (toIndex > fromIndex) -1 else 1
    return if (isLandscape) {
        slideOutVertically(
            animationSpec = tween(
                durationMillis = ROOT_TAB_EXIT_DURATION,
                easing = { DecelerateInterpolator(1.5f).getInterpolation(it) }
            ),
            targetOffsetY = { fullHeight -> fullHeight * direction }
        ) + fadeOut(animationSpec = tween(durationMillis = ROOT_TAB_EXIT_DURATION))
    } else {
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = ROOT_TAB_EXIT_DURATION,
                easing = { DecelerateInterpolator(1.5f).getInterpolation(it) }
            ),
            targetOffsetX = { fullWidth -> fullWidth * direction }
        ) + fadeOut(animationSpec = tween(durationMillis = ROOT_TAB_EXIT_DURATION))
    }
}

internal fun settingsForwardEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = SETTINGS_ENTER_DURATION,
            easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
        )
    ) + fadeIn(animationSpec = tween(durationMillis = SETTINGS_ENTER_DURATION))

internal fun settingsForwardExitTransition(): ExitTransition = ExitTransition.None

internal fun settingsBackEnterTransition(): EnterTransition = EnterTransition.None

internal fun settingsBackExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = SETTINGS_EXIT_DURATION,
            easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
        )
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(
            durationMillis = SETTINGS_EXIT_DURATION,
            easing = { DecelerateInterpolator(2.0f).getInterpolation(it) }
        )
    ) + fadeOut(animationSpec = tween(durationMillis = SETTINGS_EXIT_DURATION))
