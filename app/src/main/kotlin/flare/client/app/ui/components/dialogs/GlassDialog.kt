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
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        SideEffect {
            dialogWindow?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.35f)
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }

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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} 
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .let {
                        if (hazeState != null) {
                            val isDark = FlareTheme.colors.isDark
                            val baseStyle = HazeMaterials.ultraThin()
                            val lightTint = baseStyle.tints.firstOrNull()?.color
                                ?: Color.White.copy(alpha = 0.30f)
                            val darkTint = Color(0xFF1A1A1A).copy(alpha = 0.30f)
                            val ultraThinStyle = HazeStyle(
                                blurRadius  = baseStyle.blurRadius,
                                tints       = listOf(HazeTint(color = if (isDark) darkTint else lightTint)),
                                noiseFactor = baseStyle.noiseFactor
                            )
                            it.hazeEffect(
                                state = hazeState,
                                style = ultraThinStyle
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
