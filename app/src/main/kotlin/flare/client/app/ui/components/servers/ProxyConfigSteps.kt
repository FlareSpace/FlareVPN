package flare.client.app.ui.components.servers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.FlareCard
import flare.client.app.ui.components.FlareWizardInputField
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.SelectedProtocol
import flare.client.app.ui.theme.FlareTheme

import flare.client.app.ui.components.GeologicaMedium
import flare.client.app.ui.components.GeologicaRegular

@Composable
fun XrayConfigStep(
    selectedProtocol: SelectedProtocol,
    port: String,
    onPortChange: (String) -> Unit,
    sni: String,
    onSniChange: (String) -> Unit,
    obfsPassword: String,
    onObfsPasswordChange: (String) -> Unit,
    portHoppingEnabled: Boolean,
    onPortHoppingEnabledChange: (Boolean) -> Unit,
    portHoppingValue: String,
    onPortHoppingValueChange: (String) -> Unit,
    accentColor: Color
) {
    val isHy2 = selectedProtocol == SelectedProtocol.HYSTERIA2
    val portLabel = if (isHy2) I18n.strings.servers_hysteria2_port_label else I18n.strings.servers_xray_port_label
    val portHint = if (isHy2) I18n.strings.wizard_hysteria2_port_hint else I18n.strings.wizard_xray_port_hint
    val sniLabel = if (isHy2) I18n.strings.servers_hysteria2_sni_label else I18n.strings.servers_xray_sni_label
    val sniHint = if (isHy2) I18n.strings.wizard_hysteria2_sni_hint else I18n.strings.wizard_xray_sni_hint

    val titleText = if (isHy2) I18n.strings.servers_hysteria2_title else I18n.strings.servers_xray_title

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
                title = portLabel,
                value = port,
                onValueChange = onPortChange,
                accentColor = accentColor,
                isValid = port.isNotBlank(),
                hint = portHint,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                icon = R.drawable.ic_port
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlareWizardInputField(
                title = sniLabel,
                value = sni,
                onValueChange = onSniChange,
                accentColor = accentColor,
                isValid = sni.isNotBlank(),
                hint = sniHint,
                icon = R.drawable.ic_language
            )

            if (isHy2) {
                Spacer(modifier = Modifier.height(20.dp))
                
                FlareWizardInputField(
                    title = I18n.strings.servers_hysteria2_obfs_pass_label,
                    value = obfsPassword,
                    onValueChange = onObfsPasswordChange,
                    accentColor = accentColor,
                    isValid = obfsPassword.isNotBlank(),
                    hint = I18n.strings.wizard_hysteria2_obfs_pass_hint,
                    icon = R.drawable.ic_vpn_key
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = I18n.strings.servers_hysteria2_port_hopping_label,
                        fontFamily = GeologicaMedium,
                        fontSize = 14.sp,
                        color = FlareTheme.colors.textPrimary
                    )
                    androidx.compose.material3.Switch(
                        checked = portHoppingEnabled,
                        onCheckedChange = onPortHoppingEnabledChange,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = FlareTheme.colors.textSecondary,
                            uncheckedTrackColor = FlareTheme.colors.bgItem.copy(alpha = 0.5f)
                        )
                    )
                }
                
                AnimatedVisibility(
                    visible = portHoppingEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FlareWizardInputField(
                                    title = I18n.strings.wizard_hysteria2_port_hopping_hint,
                                    value = portHoppingValue,
                                    onValueChange = onPortHoppingValueChange,
                                    accentColor = accentColor,
                                    isValid = portHoppingValue.isNotBlank(),
                                    hint = "e.g. 20000-50000",
                                    icon = R.drawable.ic_port
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            androidx.compose.material3.Button(
                                onClick = { 
                                    val start = (20000..40000).random()
                                    val end = start + 10000
                                    onPortHoppingValueChange("$start-$end")
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.15f),
                                    contentColor = accentColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 24.dp).height(50.dp)
                            ) {
                                Text(
                                    text = I18n.strings.servers_hysteria2_port_hopping_auto,
                                    fontFamily = GeologicaMedium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShadowsocksConfigStep(
    port: String,
    onPortChange: (String) -> Unit,
    sni: String,
    onSniChange: (String) -> Unit,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_shadowsocks_title

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
                title = I18n.strings.servers_shadowsocks_port_label,
                value = port,
                onValueChange = onPortChange,
                accentColor = accentColor,
                isValid = port.isNotBlank(),
                hint = I18n.strings.wizard_shadowsocks_port_hint,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                icon = R.drawable.ic_port
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            FlareWizardInputField(
                title = I18n.strings.servers_shadowsocks_sni_label,
                value = sni,
                onValueChange = onSniChange,
                accentColor = accentColor,
                isValid = sni.isNotBlank(),
                hint = I18n.strings.wizard_shadowsocks_sni_hint,
                icon = R.drawable.ic_language
            )
        }
    }
}

@Composable
fun WireGuardConfigStep(
    port: String,
    onPortChange: (String) -> Unit,
    accentColor: Color
) {
    val titleText = I18n.strings.servers_wireguard_title

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
                title = I18n.strings.servers_wireguard_port_label,
                value = port,
                onValueChange = onPortChange,
                accentColor = accentColor,
                isValid = port.isNotBlank(),
                hint = I18n.strings.wizard_wireguard_port_hint,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                icon = R.drawable.ic_port
            )
        }
    }
}
