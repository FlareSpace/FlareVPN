package flare.client.app.ui.components.dialogs

import flare.client.app.ui.i18n.I18n

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.ui.theme.FlareTheme


@Composable
fun ProfileQRDialog(
    onDismissRequest: () -> Unit,
    qrBitmap: Bitmap?,
    onClose: () -> Unit,
    title: String = I18n.strings.profile_qr_dialog_title,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val geologicaMedium = FontFamily(Font(R.font.geologica_medium))

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_close),
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

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                color = FlareTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontFamily = geologicaMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 6.dp, end = 6.dp, top = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = I18n.strings.profile_qr_image_description,
                    modifier = Modifier.size(240.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(modifier = Modifier.size(240.dp))
            }
        }
        }
    }
}