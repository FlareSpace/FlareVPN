package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.ui.theme.FlareTheme


import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun ProfileJsonEditor(
    initialName: String,
    initialContent: String,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit,
    accentColor: Color = FlareTheme.colors.accent,
    hazeState: HazeState,
    initialScheme: String = ""
) {

    var name by remember { mutableStateOf(initialName) }
    var content by remember { mutableStateOf(initialContent) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .hazeSource(state = hazeState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .padding(top = 80.dp, bottom = 80.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = I18n.strings.label_profile_name,
                    fontFamily = GeologicaRegular,
                    fontSize = 13.sp,
                    color = FlareTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                JsonEditorTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                Text(
                    text = I18n.strings.label_json_data,
                    fontFamily = GeologicaRegular,
                    fontSize = 13.sp,
                    color = FlareTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                JsonEditorTextField(
                    value = content,
                    onValueChange = { content = it },
                    singleLine = false,
                    minLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        FlareTopBar(
            title = I18n.strings.label_config_editor,
            hazeState = hazeState,
            scrollState = scrollState,
            onBack = onBack,
            subtitle = if (initialScheme.isNotBlank()) {
                {
                    JsonEditorProtocolChip(
                        scheme = initialScheme,
                        accentColor = accentColor
                    )
                }
            } else null,
            actions = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSave(name, content) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun JsonEditorProtocolChip(scheme: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = scheme.uppercase(),
            fontFamily = GeologicaMedium,
            fontSize = 11.sp,
            color = accentColor,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun JsonEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = FlareTheme.colors.accent
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) {
            accentColor.copy(alpha = 0.08f).compositeOver(FlareTheme.colors.bgItem)
        } else {
            FlareTheme.colors.bgItem
        },
        animationSpec = tween(220),
        label = "jsonFieldBg"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(12.dp),
        textStyle = TextStyle(
            fontFamily = if (singleLine) GeologicaMedium else GeologicaRegular,
            fontSize = if (singleLine) 15.sp else 14.sp,
            color = FlareTheme.colors.textPrimary
        ),
        cursorBrush = SolidColor(accentColor),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        )
    )
}

