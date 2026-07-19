package flare.client.app.ui.navigation.graph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.hazeSource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import flare.client.app.ui.notification.AppNotificationManager
import flare.client.app.ui.notification.NotificationType
import flare.client.app.ui.components.ProfileJsonEditor
import flare.client.app.ui.components.ProfileSimpleEditor
import flare.client.app.ui.SettingsViewModel
import flare.client.app.ui.components.FlareHomeBackground
import flare.client.app.ui.components.SwipeToDismissScreen
import flare.client.app.ui.navigation.Destination
import flare.client.app.ui.navigation.HomeBackgroundContent
import flare.client.app.ui.viewmodel.ProfilesViewModel
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.theme.FlareTheme

internal fun NavGraphBuilder.flareEditorGraph(
    navController: NavHostController,
    currentRoute: () -> String?,
    settingsViewModel: SettingsViewModel,
    profilesViewModel: ProfilesViewModel,
    vpnViewModel: flare.client.app.ui.viewmodel.VpnViewModel,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    accentColor: () -> Int,
    isClipboardLoading: () -> Boolean,
    isAnySubscriptionExpanded: () -> Boolean,
    appHazeState: dev.chrisbanes.haze.HazeState
) {
    composable(Destination.JsonEditor.route) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
        
        val profile by profilesViewModel.editingProfile.collectAsState()
        
        LaunchedEffect(id) {
            if (profile == null || profile?.id != id) {
                profilesViewModel.fetchProfileForEditing(id)
            }
        }

        DisposableEffect(id) {
            onDispose {
                profilesViewModel.setEditingProfile(null)
            }
        }

        SwipeToDismissScreen(
            onDismissRight = {
                navController.popBackStack()
            },
            onSwipeDismissStart = { settingsViewModel.startSwipeDismiss() },
            backgroundContentRight = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) }
        ) {
            val localHazeState = remember { dev.chrisbanes.haze.HazeState() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (FlareTheme.effects.isBlurEnabled) it.hazeSource(state = appHazeState) else it }
            ) {
                FlareHomeBackground(
                    backgroundType = settingsViewModel.composeBackgroundType,
                    isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && (currentRoute() == Destination.JsonEditor.route),
                    animationSpeed = settingsViewModel.composeGradientSpeed,
                    photoSeed = settingsViewModel.composePhotoSeed,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                        .let { if (FlareTheme.effects.isBlurEnabled) it.hazeSource(state = localHazeState) else it }
                )
                profile?.let { p ->
                    val profileScheme = p.protocol?.takeIf { it.isNotBlank() }
                        ?: try {
                            val outbounds = org.json.JSONObject(p.configJson).optJSONArray("outbounds")
                            outbounds?.optJSONObject(0)?.optString("type")
                        } catch (_: Exception) { null }?.takeIf { it.isNotBlank() }
                        ?: runCatching {
                            java.net.URI(p.uri).scheme ?: ""
                        }.getOrDefault("")
                    ProfileJsonEditor(
                        initialName = p.name,
                        initialContent = p.configJson,
                        accentColor = Color(accentColor()),
                        initialScheme = profileScheme,
                        onSave = { name: String, json: String ->
                            profilesViewModel.updateProfile(p.id, name, json)
                            AppNotificationManager.showNotification(
                                NotificationType.SUCCESS,
                                I18n.strings.notif_profile_changed,
                                3
                            )
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                        hazeState = localHazeState
                    )
                }
            }
        }
    }

    composable(Destination.SimpleEditor.route) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
        val profile by profilesViewModel.editingProfile.collectAsState()

        LaunchedEffect(id) {
            if (profile == null || profile?.id != id) {
                profilesViewModel.fetchProfileForEditing(id)
            }
        }

        DisposableEffect(id) {
            onDispose {
                profilesViewModel.setEditingProfile(null)
            }
        }

        SwipeToDismissScreen(
            onDismissRight = {
                navController.popBackStack()
            },
            onSwipeDismissStart = { settingsViewModel.startSwipeDismiss() },
            backgroundContentRight = { HomeBackgroundContent(vpnViewModel, profilesViewModel, settingsViewModel, homeListState, isClipboardLoading, isAnySubscriptionExpanded, accentColor, appHazeState) }
        ) {
            val localHazeState = remember { dev.chrisbanes.haze.HazeState() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (FlareTheme.effects.isBlurEnabled) it.hazeSource(state = appHazeState) else it }
            ) {
                FlareHomeBackground(
                    backgroundType = settingsViewModel.composeBackgroundType,
                    isAnimationEnabled = settingsViewModel.composeIsAnimationEnabled && (currentRoute() == Destination.SimpleEditor.route),
                    animationSpeed = settingsViewModel.composeGradientSpeed,
                    photoSeed = settingsViewModel.composePhotoSeed,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                        .let { if (FlareTheme.effects.isBlurEnabled) it.hazeSource(state = localHazeState) else it }
                )
                profile?.let { p ->
                    ProfileSimpleEditor(
                        profile = p,
                        onSave = { updatedProfile ->
                            profilesViewModel.updateProfileFull(updatedProfile)
                            AppNotificationManager.showNotification(
                                NotificationType.SUCCESS,
                                I18n.strings.notif_profile_changed,
                                3
                            )
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                        accentColor = Color(accentColor()),
                        hazeState = localHazeState
                    )
                }
            }
        }
    }
}
