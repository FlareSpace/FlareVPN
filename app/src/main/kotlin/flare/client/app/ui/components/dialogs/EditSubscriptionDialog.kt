package flare.client.app.ui.components.dialogs

import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.ui.theme.FlareTheme


@Composable
fun EditSubscriptionDialog(
    onDismissRequest: () -> Unit,
    nameValue: String,
    onNameChange: (String) -> Unit,
    urlValue: String,
    onUrlChange: (String) -> Unit,
    supportWeb: String?,
    supportTg: String?,
    onSupportWebClick: () -> Unit,
    onSupportTgClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    accentColor: Int,
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
                text = I18n.strings.edit_sub_title,
                color = FlareTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontFamily = geologicaMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

        Text(
            text = I18n.strings.edit_sub_name_hint,
            color = FlareTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = geologicaRegular,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(accentColor).copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .border(1.dp, Color(accentColor), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (nameValue.isEmpty()) {
                Text(
                    text = I18n.strings.edit_sub_name_hint,
                    color = FlareTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = geologicaRegular
                )
            }
            BasicTextField(
                value = nameValue,
                onValueChange = onNameChange,
                textStyle = TextStyle(
                    color = FlareTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontFamily = geologicaRegular
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color(accentColor)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = I18n.strings.edit_sub_url_hint,
            color = FlareTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = geologicaRegular,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(accentColor).copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .border(1.dp, Color(accentColor), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (urlValue.isEmpty()) {
                Text(
                    text = I18n.strings.edit_sub_url_hint,
                    color = FlareTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = geologicaRegular
                )
            }
            BasicTextField(
                value = urlValue,
                onValueChange = onUrlChange,
                textStyle = TextStyle(
                    color = FlareTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontFamily = geologicaRegular
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                cursorBrush = SolidColor(Color(accentColor)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!supportWeb.isNullOrEmpty() || !supportTg.isNullOrEmpty()) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = I18n.strings.label_support,
                    color = FlareTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = geologicaRegular,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier.widthIn(min = 160.dp)
                ) {
                    if (!supportWeb.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(Color(accentColor).copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .border(1.dp, Color(accentColor), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(bounded = true, color = FlareTheme.colors.textSecondary),
                                    onClick = onSupportWebClick
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_chrome),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                colorFilter = ColorFilter.tint(FlareTheme.colors.textPrimary)
                            )
                            Text(
                                text = supportWeb,
                                color = FlareTheme.colors.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = geologicaRegular,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    if (!supportTg.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(accentColor).copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .border(1.dp, Color(accentColor), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(bounded = true, color = FlareTheme.colors.textSecondary),
                                    onClick = onSupportTgClick
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_telegram),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                colorFilter = ColorFilter.tint(FlareTheme.colors.textPrimary)
                            )
                            Text(
                                text = supportTg,
                                color = FlareTheme.colors.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = geologicaRegular,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }

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
                        onClick = onSave
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = I18n.strings.btn_save,
                    color = Color(accentColor),
                    fontSize = 14.sp,
                    fontFamily = geologicaMedium
                )
            }
        }
        }
    }
}