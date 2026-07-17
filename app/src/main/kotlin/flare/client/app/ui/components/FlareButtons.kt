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
fun FlareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = FlareTheme.colors.accent,
    textColor: Color = FlareTheme.colors.textPrimary,
    icon: Int? = null,
    enabled: Boolean = true
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0f, label = "buttonAlpha")
    
    if (alpha > 0.01f) {
        Row(
            modifier = modifier
                .alpha(alpha)
                .clip(RoundedCornerShape(16.dp))
                .background(FlareTheme.colors.bgItem)
                .border(1.dp, FlareTheme.colors.glassStroke, RoundedCornerShape(16.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                fontFamily = GeologicaMedium,
                fontSize = 15.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun FlareGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = FlareTheme.colors.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "flareGlassButtonGlowAlpha"
    )
    
    val buttonBgBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isPressed) 0.22f else 0.08f),
                Color.White.copy(alpha = if (isPressed) 0.12f else 0.02f)
            )
        } else {
            listOf(
                Color.Black.copy(alpha = if (isPressed) 0.12f else 0.05f),
                Color.Black.copy(alpha = if (isPressed) 0.06f else 0.02f)
            )
        }
    )

    val buttonBorderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isPressed) 0.45f else 0.15f),
                if (isPressed) Color.White.copy(alpha = 0.15f) else Color.Transparent
            )
        } else {
            listOf(
                Color.Black.copy(alpha = if (isPressed) 0.22f else 0.10f),
                Color.Black.copy(alpha = if (isPressed) 0.06f else 0.02f)
            )
        }
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                6.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(
                                    (glowAlpha * 180).toInt(),
                                    255,
                                    255,
                                    255
                                )
                            )
                        }
                        canvas.nativeCanvas.drawCircle(
                            size.width / 2f,
                            size.height / 2f,
                            size.width / 2f - 0.5.dp.toPx(),
                            paint
                        )
                    }
                }
            }
            .background(
                brush = buttonBgBrush,
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                brush = buttonBorderBrush,
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun FlareInfoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FlareTheme.colors.accent
) {
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        FlareGlassButton(
            onClick = onClick,
            modifier = Modifier.size(18.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_i),
                contentDescription = null,
                tint = FlareTheme.colors.textPrimary,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
