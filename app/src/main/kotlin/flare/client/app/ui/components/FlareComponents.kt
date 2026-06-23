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
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
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



class DynamicAndroidFont(
    val isBold: Boolean,
    override val weight: FontWeight,
    override val style: FontStyle = FontStyle.Normal,
    val fontKey: String = activeFontKey
) : AndroidFont(
    loadingStrategy = FontLoadingStrategy.Blocking,
    typefaceLoader = DynamicTypefaceLoader,
    variationSettings = FontVariation.Settings()
) {
    companion object {
        @Volatile
        var activeFontKey: String = "geologica"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DynamicAndroidFont) return false
        return isBold == other.isBold && weight == other.weight && style == other.style && fontKey == other.fontKey
    }

    override fun hashCode(): Int {
        var result = isBold.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + fontKey.hashCode()
        return result
    }

    override fun toString(): String {
        return "DynamicAndroidFont(isBold=$isBold, weight=$weight, style=$style, fontKey=$fontKey)"
    }
}

object DynamicTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        val dynamicFont = font as? DynamicAndroidFont ?: return null
        return getFontTypeface(context, dynamicFont.fontKey, dynamicFont.isBold)
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        val dynamicFont = font as? DynamicAndroidFont ?: return null
        return getFontTypeface(context, dynamicFont.fontKey, dynamicFont.isBold)
    }

    private fun getFontTypeface(context: Context, fontKey: String, isBold: Boolean): Typeface? {
        return when (fontKey) {
            "system" -> {
                Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            }
            "google_sans" -> {
                ResourcesCompat.getFont(context, R.font.google_sans_flex)
            }
            "inter" -> {
                ResourcesCompat.getFont(context, if (isBold) R.font.inter_medium else R.font.inter_regular)
            }
            else -> {
                ResourcesCompat.getFont(context, if (isBold) R.font.geologica_medium else R.font.geologica_regular)
            }
        }
    }
}

private val regularFontsCache = mutableMapOf<String, FontFamily>()
val GeologicaRegular: FontFamily
    get() {
        val key = DynamicAndroidFont.activeFontKey
        return regularFontsCache.getOrPut(key) {
            FontFamily(DynamicAndroidFont(isBold = false, FontWeight.Normal, fontKey = key))
        }
    }

private val mediumFontsCache = mutableMapOf<String, FontFamily>()
val GeologicaMedium: FontFamily
    get() {
        val key = DynamicAndroidFont.activeFontKey
        return mediumFontsCache.getOrPut(key) {
            FontFamily(DynamicAndroidFont(isBold = true, FontWeight.Medium, fontKey = key))
        }
    }

val InterRegular = FontFamily(Font(R.font.inter_regular, FontWeight.Normal))
val InterMedium = FontFamily(Font(R.font.inter_medium, FontWeight.Medium))

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(80f),
            FontVariation.grade(0)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexRegular = FontFamily(
    Font(
        R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(80f),
            FontVariation.grade(0)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexMedium = FontFamily(
    Font(
        R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(80f),
            FontVariation.grade(0)
        )
    )
)

