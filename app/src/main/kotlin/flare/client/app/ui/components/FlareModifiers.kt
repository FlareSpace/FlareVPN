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




private const val LIQUID_GLASS_AGSL = """
    uniform shader img;
    uniform float2 resolution;
    uniform float2 center;
    uniform float2 size;
    uniform float4 radius;
    uniform float thickness;
    uniform float refract_index;
    uniform float refract_intensity;
    uniform float saturation;
    uniform float glass_height;
    uniform float4 foreground_color_premultiplied;
    uniform float is_dark_mode;
    uniform float has_outline;
    uniform float outline_thickness;
    uniform float density;
    uniform float is_liquid_glass_enabled;
    
    half sdfRect(half2 p, half4 r) {
      r.xy = (p.x > 0.0) ? r.xy : r.zw;
      r.x  = (p.y > 0.0) ? r.x  : r.y;
      half2 q = abs(p) - size + r.x;
      return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;
    }
    
    half4 srcOver(half4 src, half4 dst) {
        half3 outRGB = (src.rgb + dst.rgb * (1.0 - src.a));
        float outA = src.a + (1.0 - src.a) * dst.a;
        return half4(outRGB, outA);
    }
    
    half4 main(in float2 fragCoord) {
      half2 p = fragCoord - center;
      half sd = sdfRect(p, radius);
      half2 uv = fragCoord;
    
      if (sd < 0.0) {
        if (is_liquid_glass_enabled < 0.5) {
            half4 bg = img.eval(uv);
            half4 result = srcOver(half4(foreground_color_premultiplied), bg);
            if (is_dark_mode <= 0.5 && has_outline > 0.5) {
                half edgeDist = -sd;
                half outlineMask = smoothstep(outline_thickness, 0.0, edgeDist);
                result.rgb = mix(result.rgb, half3(0.0), outlineMask * 0.15);
            }
            return result;
        }

        half sdX = sdfRect(p + half2(1.0, 0.0), radius);
        half sdY = sdfRect(p + half2(0.0, 1.0), radius);
    
        half effectiveT = max(thickness, glass_height * min(size.x, size.y));
        half n_cos = max(effectiveT + sd, 0.0) / effectiveT;
        half n_cos2 = n_cos * n_cos;
        half n_sin = sqrt(1.0 - n_cos2);
        half3 normal = normalize(half3((sdX - sd) * n_cos, (sdY - sd) * n_cos, n_sin));
    
        half3 refract_vec = refract(half3(0.0, 0.0, -1.0), normal, 1.0 / refract_index);
        half h = sd < -effectiveT ? effectiveT : sqrt(sd * (-2.0 * effectiveT - sd));
        half refract_length = (h + 8.0 * thickness) / -refract_vec.z;
    
        uv += refract_vec.xy * refract_length * refract_intensity;
    
        half4 bg = img.eval(uv);
    
        half luminance = dot(bg.rgb, half3(0.2126, 0.7152, 0.0722));
        bg.rgb = mix(half3(luminance), bg.rgb, saturation);
    
        half4 result = srcOver(half4(foreground_color_premultiplied), bg);
    
        half edgeDist = -sd;
        half tlDir = max(dot(normal.xy, normalize(half2(-0.8, -1.0))), 0.0);
        half brDir = max(dot(normal.xy, normalize(half2(0.8, 1.0))), 0.0);
        
        half tlHighlight = 0.0;
        half brHighlight = 0.0;
        half rimLight = 0.0;
        
        if (is_dark_mode > 0.5) {
            half thinHlMask = smoothstep(2.0 * density, 0.0, edgeDist);
            tlHighlight = pow(tlDir, 8.0) * thinHlMask * 0.35;
        } else {
            tlHighlight = pow(tlDir, 8.0) * smoothstep(3.0 * density, 0.0, edgeDist) * 0.8;
            if (has_outline > 0.5) {
                half outlineMask = smoothstep(outline_thickness, 0.0, edgeDist);
                result.rgb = mix(result.rgb, half3(0.0), outlineMask * 0.15);
            }
        }
        
        result.rgb += half3(1.0) * (tlHighlight + brHighlight + rimLight);
        return result;
      }
    
      return half4(0.0);
    }
"""

fun Modifier.flareGlass(
    isDark: Boolean,
    radius: Float = 18f,
    thickness: Float = 2f,
    intensity: Float = 1.2f,
    index: Float = 1.52f,
    glassHeight: Float = 0.12f,
    hasOutline: Boolean = false,
    outlineThickness: Float = 1.0f
): Modifier = composed {
    val isLiquidGlassEnabled = FlareTheme.effects.isLiquidGlassEnabled
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        this.graphicsLayer {
            val shader = android.graphics.RuntimeShader(LIQUID_GLASS_AGSL)
            val dp = density
            
            val fgColor = if (isDark)
                android.graphics.Color.argb(160, 32, 34, 40)
            else
                android.graphics.Color.argb(135, 255, 255, 255)
                
            val a = android.graphics.Color.alpha(fgColor) / 255f
            val r = android.graphics.Color.red(fgColor) / 255f * a
            val g = android.graphics.Color.green(fgColor) / 255f * a
            val b = android.graphics.Color.blue(fgColor) / 255f * a
 
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("center", size.width / 2f, size.height / 2f)
            shader.setFloatUniform("size", size.width / 2f, size.height / 2f)
            val rPx = radius * dp
            shader.setFloatUniform("radius", rPx, rPx, rPx, rPx)
            shader.setFloatUniform("thickness", thickness * dp)
            shader.setFloatUniform("refract_intensity", intensity)
            shader.setFloatUniform("refract_index", index)
            shader.setFloatUniform("glass_height", glassHeight)
            shader.setFloatUniform("saturation", 1.45f)
            shader.setFloatUniform("foreground_color_premultiplied", r, g, b, a)
            shader.setFloatUniform("is_dark_mode", if (isDark) 1f else 0f)
            shader.setFloatUniform("has_outline", if (hasOutline) 1f else 0f)
            shader.setFloatUniform("outline_thickness", outlineThickness * dp)
            shader.setFloatUniform("density", dp)
            shader.setFloatUniform("is_liquid_glass_enabled", if (isLiquidGlassEnabled) 1f else 0f)

            renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "img").asComposeRenderEffect()
        }
    } else {
        this.background(
            if (isDark) Color(0xCC1A1C1E) else Color(0xCCFFFFFF),
            RoundedCornerShape(radius.dp)
        )
        .clip(RoundedCornerShape(radius.dp))
    }
}

fun Modifier.fadingEdge(
    showTop: Boolean,
    showBottom: Boolean,
    topFadeHeight: androidx.compose.ui.unit.Dp = 16.dp,
    bottomFadeHeight: androidx.compose.ui.unit.Dp = 16.dp
): Modifier = composed {
    val animatedTopHeight by animateDpAsState(
        targetValue = if (showTop) topFadeHeight else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "topFadeHeight"
    )
    val animatedBottomHeight by animateDpAsState(
        targetValue = if (showBottom) bottomFadeHeight else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "bottomFadeHeight"
    )

    val isDark = FlareTheme.colors.isDark
    val animatedTopLineAlpha by animateFloatAsState(
        targetValue = if (showTop) (if (isDark) 0.15f else 0.12f) else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "topLineAlpha"
    )
    val animatedBottomLineAlpha by animateFloatAsState(
        targetValue = if (showBottom) (if (isDark) 0.15f else 0.12f) else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "bottomLineAlpha"
    )

    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            
            val topFadeHeightPx = animatedTopHeight.toPx()
            val bottomFadeHeightPx = animatedBottomHeight.toPx()
            
            val numStops = 12
            
            if (topFadeHeightPx > 0.5f) {
                val topColors = List(numStops) { i ->
                    val progress = i.toFloat() / (numStops - 1)
                    val alpha = 0.28f + 0.72f * (progress * progress * (3f - 2f * progress))
                    Color.Black.copy(alpha = alpha)
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = topColors,
                        startY = 0f,
                        endY = topFadeHeightPx
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            
            if (bottomFadeHeightPx > 0.5f) {
                val bottomColors = List(numStops) { i ->
                    val progress = i.toFloat() / (numStops - 1)
                    val t = 1f - progress
                    val alpha = 0.28f + 0.72f * (t * t * (3f - 2f * t))
                    Color.Black.copy(alpha = alpha)
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = bottomColors,
                        startY = size.height - bottomFadeHeightPx,
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

            
            if (animatedTopLineAlpha > 0f) {
                val lineColor = if (isDark) Color.White else Color.Black
                drawLine(
                    color = lineColor.copy(alpha = animatedTopLineAlpha),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            if (animatedBottomLineAlpha > 0f) {
                val lineColor = if (isDark) Color.White else Color.Black
                drawLine(
                    color = lineColor.copy(alpha = animatedBottomLineAlpha),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
        }
}
