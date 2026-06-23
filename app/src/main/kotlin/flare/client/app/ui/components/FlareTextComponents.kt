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
fun RollingTimer(
    time: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val density = LocalDensity.current
        val baseWidth = with(density) { fontSize.toDp() * 0.65f }
        val colonWidth = with(density) { fontSize.toDp() * 0.35f }

        time.indices.forEach { i ->
            val char = time[i]
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn() togetherWith
                            slideOutVertically { -it } + fadeOut())
                        .using(SizeTransform(clip = false))
                },
                contentAlignment = Alignment.Center,
                label = "timer_digit_$i"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    fontFamily = GeologicaMedium,
                    fontSize = fontSize,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(if (digit == ':') colonWidth else baseWidth)
                )
            }
        }
    }
}

@Composable
fun FlareAnimatedPercentText(
    progress: Int,
    modifier: Modifier = Modifier,
    color: Color = FlareTheme.colors.textSecondary,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = progress,
            transitionSpec = {
                if (targetState > initialState) {
                    (androidx.compose.animation.slideInVertically { height -> height } + androidx.compose.animation.fadeIn())
                        .togetherWith(androidx.compose.animation.slideOutVertically { height -> -height } + androidx.compose.animation.fadeOut())
                } else {
                    (androidx.compose.animation.slideInVertically { height -> -height } + androidx.compose.animation.fadeIn())
                        .togetherWith(androidx.compose.animation.slideOutVertically { height -> height } + androidx.compose.animation.fadeOut())
                }.using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "percentAnimation"
        ) { targetProgress ->
            Text(
                text = "$targetProgress",
                fontFamily = GeologicaMedium,
                fontSize = fontSize,
                color = color
            )
        }
        Text(
            text = "%",
            fontFamily = GeologicaRegular,
            fontSize = fontSize,
            color = color
        )
    }
}

@Composable
fun FlareSectionHeader(text: String) {
    Text(
        text = text,
        fontFamily = GeologicaMedium,
        fontSize = 14.sp,
        color = FlareTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
