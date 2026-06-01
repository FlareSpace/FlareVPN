package flare.client.app.ui

import flare.client.app.ui.i18n.I18n


import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import flare.client.app.R
import flare.client.app.ui.components.*
import flare.client.app.ui.theme.FlareTheme


private val GeologicaMedium = FontFamily(Font(R.font.geologica_medium, FontWeight.Medium))

@Composable
fun RoutingScreen(
    routingRules: List<RoutingRuleState>,
    onBack: () -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onModeClick: (String, String) -> Unit,
    onDownloadClick: (String) -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {

        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .hazeSource(state = hazeState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .padding(top = 80.dp, bottom = 160.dp)
                    .padding(horizontal = 20.dp)
            ) {
                routingRules.forEach { rule ->
                    FlareRoutingCard(
                        rule = rule,
                        onToggle = { enabled: Boolean -> onToggleRule(rule.id, enabled) },
                        onModeClick = { mode: String -> onModeClick(rule.id, mode) },
                        onDownloadClick = { onDownloadClick(rule.id) },
                        accentColor = accentColor,
                        hazeState = hazeState
                    )
                }
            }
        }

        
        FlareTopBar(
            title = I18n.strings.settings_routing_title,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBack
        )
    }
}
