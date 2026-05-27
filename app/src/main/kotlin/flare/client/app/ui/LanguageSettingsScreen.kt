package flare.client.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.*
import flare.client.app.ui.theme.FlareTheme


import flare.client.app.ui.i18n.I18n

private val GeologicaMedium = FontFamily(Font(R.font.geologica_medium, FontWeight.Medium))
private val GeologicaRegular = FontFamily(Font(R.font.geologica_regular, FontWeight.Normal))

@Composable
fun LanguageSettingsScreen(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    accentColor: Color,
    onBack: () -> Unit,
    hazeState: HazeState
) {
    val isDark = isSystemInDarkTheme()
    val strings = I18n.strings

    val options = listOf(
        strings.language_auto,
        "English",
        "Русский"
    )

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
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(top = 80.dp, bottom = 160.dp)
                    .padding(horizontal = 20.dp)
            ) {
                FlareSectionHeader(text = strings.settings_label_language)

                val currentLanguageDisplay = when (currentLanguage) {
                    "en" -> "English"
                    "ru" -> "Русский"
                    else -> strings.language_auto
                }
                
                FlareSettingsValueItem(
                    title = strings.settings_label_language,
                    value = currentLanguageDisplay,
                    menuItems = options.mapIndexed { i, opt ->
                        flare.client.app.util.GlassUtils.MenuItem(i, opt) {
                            onLanguageSelected(opt)
                        }
                    },
                    hazeState = hazeState,
                    accentColor = accentColor,
                    cornerType = DisplayItem.CornerType.ALL
                )
            }
        }

        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .hazeEffect(state = hazeState) {
                    blurRadius = 24.dp
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FlareTheme.colors.headerGradientStart, FlareTheme.colors.headerGradientEnd)
                    )
                )
                .statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp)
                .padding(top = 2.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null,
                    tint = FlareTheme.colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = strings.settings_language_title,
                fontFamily = GeologicaMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = FlareTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
