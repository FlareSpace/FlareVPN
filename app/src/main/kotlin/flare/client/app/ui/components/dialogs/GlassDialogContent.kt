package flare.client.app.ui.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import android.view.ContextThemeWrapper
import flare.client.app.R
import flare.client.app.ui.theme.FlareTheme


@Composable
fun GlassDialogContent(
    title: String,
    text: String,
    cancelText: String?,
    actionText: String,
    onCancel: (() -> Unit)?,
    onAction: () -> Unit,
    onClose: (() -> Unit)?,
    accentColor: Int,
    accentEndColor: Int
) {
    val geologicaMedium = FontFamily(Font(R.font.geologica_medium))
    val geologicaRegular = FontFamily(Font(R.font.geologica_regular))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = FlareTheme.colors.textPrimary,
                fontSize = 20.sp,
                fontFamily = geologicaMedium,
                modifier = Modifier.weight(1f)
            )

            if (onClose != null) {
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    colorFilter = ColorFilter.tint(FlareTheme.colors.textSecondary),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp),
                            onClick = onClose
                        )
                )
            }
        }

        Text(
            text = text,
            color = FlareTheme.colors.textSecondary,
            fontSize = 15.sp,
            fontFamily = geologicaRegular,
            lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onCancel != null && cancelText != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(bounded = true, color = FlareTheme.colors.textSecondary),
                            onClick = onCancel
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cancelText,
                        color = FlareTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = geologicaMedium
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        color = androidx.compose.ui.graphics.Color(accentColor),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = androidx.compose.ui.graphics.Color(0x14FFFFFF) 
                        ),
                        onClick = onAction
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionText,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp,
                    fontFamily = geologicaMedium
                )
            }
        }
    }
}