package flare.client.app.ui.components.servers

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.FlareCard
import flare.client.app.ui.components.FlareWizardInputField
import flare.client.app.ui.components.FlareWizardIpPortField
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.theme.FlareTheme




@Composable
fun SshConfigStep(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    ip: String,
    onIpChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    user: String,
    onUserChange: (String) -> Unit,
    pass: String,
    onPassChange: (String) -> Unit,
    onSshKeyClick: () -> Unit,
    hazeState: HazeState,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_ssh_title

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titleText,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 16.dp,
            paddingVertical = 20.dp,
            borderColor = accentColor.copy(alpha = 0.15f),
            borderWidth = 1.dp
        ) {
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_profile_name,
                value = profileName,
                onValueChange = onProfileNameChange,
                accentColor = accentColor,
                isValid = profileName.isNotBlank(),
                icon = R.drawable.ic_cloud,
                hint = I18n.strings.servers_ssh_profile_name_hint
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardIpPortField(
                ipValue = ip,
                onIpChange = onIpChange,
                portValue = port,
                onPortChange = onPortChange,
                accentColor = accentColor,
                icon = R.drawable.ic_language_filled
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_username,
                value = user,
                onValueChange = onUserChange,
                accentColor = accentColor,
                isValid = user.isNotBlank(),
                icon = R.drawable.ic_suitcase,
                hint = "root"
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_password,
                value = pass,
                onValueChange = onPassChange,
                accentColor = accentColor,
                isValid = pass.isNotBlank(),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                icon = R.drawable.ic_vpn_key,
                hint = "••••••••"
            )
        }
    }
}
