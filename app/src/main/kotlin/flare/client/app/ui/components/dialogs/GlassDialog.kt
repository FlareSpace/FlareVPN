package flare.client.app.ui.components.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import dev.chrisbanes.haze.hazeEffect
import flare.client.app.ui.theme.FlareTheme

import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.SideEffect
import android.view.WindowManager

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    maxWidthDp: Int = 340,
    blurRadius: Float = 1f,
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
        val context = androidx.compose.ui.platform.LocalContext.current
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        SideEffect {
            dialogWindow?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.60f)
                
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = window.attributes
                    params.blurBehindRadius = (blurRadius * context.resources.displayMetrics.density).toInt()
                    window.attributes = params
                }
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
                            it.hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    blurRadius = blurRadius.dp,
                                    tints = emptyList()
                                )
                            )
                        } else {
                            it
                        }
                    }
                    .background(FlareTheme.colors.dialogGlassFill)
                    .then(
                        if (!FlareTheme.colors.isDark) {
                            Modifier.border(
                                width = 1.dp,
                                color = FlareTheme.colors.dialogGlassStroke,
                                shape = RoundedCornerShape(24.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                content()
            }
        }
    }
}
