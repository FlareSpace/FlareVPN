package flare.client.app.ui.components.servers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.ui.components.FlareButton
import flare.client.app.ui.components.GeologicaMedium
import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.theme.FlareTheme

@Composable
fun FreeAuthPromptStep(
    accentColor: Color,
    onWithoutAuthClick: () -> Unit,
    onWithAuthClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = I18n.strings.wizard_setup_free_auth_prompt_title,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
        )

        flare.client.app.ui.components.FlareCard(
            cornerType = flare.client.app.data.model.DisplayItem.CornerType.ALL,
            paddingHorizontal = 20.dp,
            paddingVertical = 28.dp,
            borderColor = accentColor.copy(alpha = 0.3f),
            borderWidth = 0.5.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = I18n.strings.wizard_setup_free_auth_prompt_subtitle,
                    fontFamily = GeologicaMedium,
                    fontSize = 22.sp,
                    color = FlareTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = I18n.strings.wizard_setup_free_auth_prompt_desc,
                    fontFamily = GeologicaRegular,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                FlareButton(
                    text = I18n.strings.wizard_setup_free_auth_prompt_with,
                    onClick = onWithAuthClick,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlareButton(
                    text = I18n.strings.wizard_setup_free_auth_prompt_without,
                    onClick = onWithoutAuthClick,
                    accentColor = FlareTheme.colors.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
