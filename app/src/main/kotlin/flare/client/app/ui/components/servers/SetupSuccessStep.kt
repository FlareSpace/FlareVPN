package flare.client.app.ui.components.servers

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.FlareButton
import flare.client.app.ui.components.FlareCard
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.theme.FlareTheme




@Composable
fun SetupSuccessStep(
    onGoHomeClick: () -> Unit,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_setup_success_title

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titleText,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
        )

        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 20.dp,
            paddingVertical = 28.dp,
            borderColor = FlareTheme.colors.connectedGreen.copy(alpha = 0.3f),
            borderWidth = 1.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = FlareTheme.colors.connectedGreen,
                    modifier = Modifier.size(64.dp).padding(bottom = 24.dp)
                )
                
                Text(
                    text = I18n.strings.servers_setup_success,
                    fontFamily = GeologicaMedium,
                    fontSize = 22.sp,
                    color = FlareTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = I18n.strings.servers_setup_success_desc,
                    fontFamily = GeologicaRegular,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                FlareButton(
                    text = I18n.strings.onboarding_btn_go_main,
                    onClick = onGoHomeClick,
                    accentColor = accentColor,
                    icon = R.drawable.ic_arrow_left
                )
            }
        }
    }
}
