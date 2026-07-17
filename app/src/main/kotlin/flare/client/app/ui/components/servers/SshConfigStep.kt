package flare.client.app.ui.components.servers

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring


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
    
    val ipFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isUsernameFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    val isShifted = isUsernameFocused || isPasswordFocused
    val offsetProgress by animateDpAsState(
        targetValue = if (isShifted) (-80).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetProgress)
    ) {
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
                hint = I18n.strings.servers_ssh_profile_name_hint,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { ipFocusRequester.requestFocus() })
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardIpPortField(
                ipValue = ip,
                onIpChange = onIpChange,
                portValue = port,
                onPortChange = onPortChange,
                accentColor = accentColor,
                icon = R.drawable.ic_language_filled,
                ipFocusRequester = ipFocusRequester,
                ipKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                ipKeyboardActions = KeyboardActions(onNext = { usernameFocusRequester.requestFocus() }),
                portKeyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                portKeyboardActions = KeyboardActions(onNext = { usernameFocusRequester.requestFocus() })
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_username,
                value = user,
                onValueChange = onUserChange,
                accentColor = accentColor,
                isValid = user.isNotBlank(),
                icon = R.drawable.ic_suitcase,
                hint = "root",
                focusRequester = usernameFocusRequester,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                onFocusChanged = { isUsernameFocused = it }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = I18n.strings.servers_ssh_password,
                value = pass,
                onValueChange = onPassChange,
                accentColor = accentColor,
                isValid = pass.isNotBlank(),
                keyboardType = KeyboardType.Password,
                icon = R.drawable.ic_vpn_key,
                hint = "••••••••",
                focusRequester = passwordFocusRequester,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                onFocusChanged = { isPasswordFocused = it }
            )
        }
    }
}
