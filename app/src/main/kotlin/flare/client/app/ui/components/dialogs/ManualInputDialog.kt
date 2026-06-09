package flare.client.app.ui.components.dialogs

import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.ui.MainActivity
import flare.client.app.ui.theme.FlareTheme


@Composable
fun ManualInputDialog(
    onDismissRequest: () -> Unit,
    title: String,
    hint: String,
    initialText: String,
    onCancel: () -> Unit,
    onAdd: (String) -> Unit,
    accentColor: Int,
    textValue: String,
    onTextValueChange: (String) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val geologicaMedium = FontFamily(Font(R.font.geologica_medium))
    val geologicaRegular = FontFamily(Font(R.font.geologica_regular))

    GlassDialog(
        onDismissRequest = onDismissRequest,
        maxWidthDp = 340,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = FlareTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontFamily = geologicaMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

        Text(
            text = I18n.strings.label_credentials,
            color = FlareTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = geologicaRegular,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(Color(accentColor).copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .border(1.dp, Color(accentColor), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (textValue.isEmpty()) {
                Text(
                    text = hint,
                    color = FlareTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = geologicaRegular
                )
            }
            BasicTextField(
                value = textValue,
                onValueChange = onTextValueChange,
                textStyle = TextStyle(
                    color = FlareTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontFamily = geologicaRegular
                ),
                cursorBrush = SolidColor(Color(accentColor)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
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
                    text = I18n.strings.btn_cancel,
                    color = FlareTheme.colors.textSecondary,
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
                        indication = androidx.compose.material3.ripple(bounded = true, color = Color(accentColor)),
                        onClick = { onAdd(textValue) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = I18n.strings.btn_add,
                    color = Color(accentColor),
                    fontSize = 14.sp,
                    fontFamily = geologicaMedium
                )
            }
        }
        }
    }
}