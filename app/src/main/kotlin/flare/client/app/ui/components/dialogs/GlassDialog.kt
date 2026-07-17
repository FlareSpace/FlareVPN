package flare.client.app.ui.components.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import flare.client.app.ui.theme.FlareTheme

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.SideEffect
import android.view.WindowManager

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    maxWidthDp: Int = 340,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val context = androidx.compose.ui.platform.LocalContext.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        SideEffect {
            dialogWindow?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.60f)
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = window.attributes
                    params.blurBehindRadius = (15 * context.resources.displayMetrics.density).toInt()
                    window.attributes = params
                }
            }
        }

        val animProgress = remember { Animatable(0f) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        val scale = 0.85f + 0.15f * animProgress.value
        val alpha = animProgress.value.coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .widthIn(max = maxWidthDp.dp)
                    .wrapContentHeight()
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} 
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .let {
                        if (hazeState != null) {
                            val isDark = FlareTheme.colors.isDark
                            val baseStyle = HazeMaterials.thin()
                            val baseAlpha = baseStyle.tints.firstOrNull()?.color?.alpha ?: 0.30f
                            val lightTint = baseStyle.tints.firstOrNull()?.color
                                ?: Color.White.copy(alpha = 0.30f)
                            val darkTint = Color(0xFF1A1A1A).copy(alpha = baseAlpha)
                            val dialogStyle = HazeStyle(
                                blurRadius  = baseStyle.blurRadius,
                                tints       = listOf(HazeTint(color = if (isDark) darkTint else lightTint)),
                                noiseFactor = 0f
                            )
                            it.background(if (flare.client.app.ui.theme.FlareTheme.effects.isBlurEnabled) androidx.compose.ui.graphics.Color.Transparent else flare.client.app.ui.theme.FlareTheme.colors.bgItem.copy(alpha = 0.95f)).hazeEffect(
                                state = hazeState,
                                style = dialogStyle
                            )
                        } else {
                            it.background(FlareTheme.colors.dialogGlassFill)
                        }
                    }
                    .border(
                        width = 0.5.dp,
                        color = FlareTheme.colors.dialogGlassStroke,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val scrollState = rememberScrollState()
                Box(
                    modifier = if (isLandscape) {
                        Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(scrollState)
                    } else Modifier
                ) {
                    content()
                }
            }
        }
    }
}
