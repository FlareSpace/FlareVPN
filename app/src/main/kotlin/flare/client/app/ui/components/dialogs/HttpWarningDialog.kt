package flare.client.app.ui.components.dialogs

import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.ui.theme.FlareTheme

@Composable
fun HttpWarningDialog(
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val geologicaMedium = flare.client.app.ui.components.GeologicaMedium
    val geologicaRegular = flare.client.app.ui.components.GeologicaRegular

    GlassDialog(
        onDismissRequest = onDismissRequest,
        maxWidthDp = 340,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = I18n.strings.label_warning,
                color = FlareTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontFamily = geologicaMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = I18n.strings.http_warning_message,
                color = FlareTheme.colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = geologicaRegular,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(bounded = true, color = Color(0xFF34C759)),
                            onClick = onCancel
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = I18n.strings.btn_cancel,
                        color = Color(0xFF34C759),
                        fontSize = 14.sp,
                        fontFamily = geologicaMedium
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(bounded = true, color = Color(0xFFFF3B30)),
                            onClick = onConfirm
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = I18n.strings.btn_add,
                        color = Color(0xFFFF3B30),
                        fontSize = 14.sp,
                        fontFamily = geologicaMedium
                    )
                }
            }
        }
    }
}
