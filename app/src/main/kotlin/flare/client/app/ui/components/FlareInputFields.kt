package flare.client.app.ui.components

import flare.client.app.ui.i18n.I18n

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.theme.FlareTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import dev.chrisbanes.haze.HazeProgressive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius




@Composable
fun FlareWizardInputField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    isValid: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    accentColor: Color = FlareTheme.colors.accent,
    icon: Int? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = keyboardType,
        imeAction = ImeAction.Next
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        val borderColor = if (isFocused || isValid) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
        val bgColor = if (isFocused || isValid) accentColor.copy(alpha = 0.05f) else FlareTheme.colors.bgItem.copy(alpha = 0.5f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (isFocused || isValid) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 12.dp).size(20.dp)
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        fontFamily = GeologicaRegular,
                        fontSize = 15.sp,
                        color = FlareTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                }
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = GeologicaMedium,
                        fontSize = 15.sp,
                        color = FlareTheme.colors.textPrimary
                    ),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { 
                            isFocused = it.isFocused 
                            onFocusChanged(it.isFocused)
                        }
                )
            }
        }
    }
}

@Composable
fun FlareWizardIpPortField(
    ipValue: String,
    onIpChange: (String) -> Unit,
    portValue: String,
    onPortChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = FlareTheme.colors.accent,
    icon: Int? = null,
    ipFocusRequester: FocusRequester = remember { FocusRequester() },
    ipKeyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    ipKeyboardActions: KeyboardActions = KeyboardActions.Default,
    portFocusRequester: FocusRequester = remember { FocusRequester() },
    portKeyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    ),
    portKeyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }
    val isValid = ipValue.isNotBlank() && portValue.isNotBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = I18n.strings.servers_ssh_ip,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        val borderColor = if (isFocused || isValid) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
        val bgColor = if (isFocused || isValid) accentColor.copy(alpha = 0.05f) else FlareTheme.colors.bgItem.copy(alpha = 0.5f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (isFocused || isValid) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp).size(20.dp)
                )
            }

            BasicTextField(
                value = ipValue,
                onValueChange = onIpChange,
                textStyle = TextStyle(
                    fontFamily = GeologicaMedium,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textPrimary
                ),
                cursorBrush = SolidColor(accentColor),
                singleLine = true,
                keyboardOptions = ipKeyboardOptions,
                keyboardActions = ipKeyboardActions,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (icon != null) 12.dp else 16.dp,
                        end = 16.dp
                    )
                    .focusRequester(ipFocusRequester)
                    .onFocusChanged { if (it.isFocused) isFocused = true else if (!it.hasFocus) isFocused = false }
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(FlareTheme.colors.dividerColor)
            )

            BasicTextField(
                value = portValue,
                onValueChange = onPortChange,
                textStyle = TextStyle(
                    fontFamily = GeologicaMedium,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(accentColor),
                singleLine = true,
                keyboardOptions = portKeyboardOptions,
                keyboardActions = portKeyboardActions,
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 8.dp)
                    .focusRequester(portFocusRequester)
                    .onFocusChanged { if (it.isFocused) isFocused = true else if (!it.hasFocus) isFocused = false }
            )
        }
    }
}
