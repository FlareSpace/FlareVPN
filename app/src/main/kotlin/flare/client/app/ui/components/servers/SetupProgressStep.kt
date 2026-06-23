package flare.client.app.ui.components.servers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.components.FlareButton
import flare.client.app.ui.components.FlareCard
import flare.client.app.ui.i18n.I18n
import flare.client.app.ui.theme.FlareTheme

import flare.client.app.ui.components.GeologicaMedium
import flare.client.app.ui.components.GeologicaRegular

@Composable
fun SetupProgressStep(
    status: String,
    progress: Float,
    error: String?,
    accentColor: Color,
    onBackClick: () -> Unit
) {
    val isError = error != null
    val titleText = I18n.strings.servers_setup_progress_title

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titleText,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
        )

        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 20.dp,
            paddingVertical = 28.dp,
            borderColor = if (isError) FlareTheme.colors.disconnectedRed.copy(alpha = 0.3f) else accentColor.copy(alpha = 0.15f),
            borderWidth = 1.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isError) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        tint = FlareTheme.colors.disconnectedRed,
                        modifier = Modifier.size(64.dp).padding(bottom = 24.dp)
                    )
                }

                Text(
                    text = status,
                    fontFamily = GeologicaMedium,
                    fontSize = 17.sp,
                    color = if (isError) FlareTheme.colors.disconnectedRed else FlareTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                if (!isError) {
                    val progressColor by animateColorAsState(
                        targetValue = if (progress >= 100f) FlareTheme.colors.connectedGreen else accentColor,
                        label = "progressColor"
                    )

                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    
                    ThreeJumpingDots(
                        modifier = Modifier.padding(top = 24.dp),
                        dotSize = 10.dp,
                        dotColor = accentColor,
                        dotSpacing = 8.dp
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    FlareButton(
                        text = I18n.strings.btn_cancel,
                        onClick = onBackClick,
                        accentColor = accentColor,
                        icon = R.drawable.ic_arrow_left
                    )
                }
            }
        }
    }
}

@Composable
fun FlareProgressStep(
    status: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = I18n.strings.servers_tariff_title,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp).align(Alignment.Start)
        )

        FlareCard(
            cornerType = DisplayItem.CornerType.ALL,
            paddingHorizontal = 20.dp,
            paddingVertical = 36.dp,
            borderColor = accentColor.copy(alpha = 0.15f),
            borderWidth = 0.5.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = accentColor,
                        strokeWidth = 3.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = status,
                    fontFamily = GeologicaMedium,
                    fontSize = 17.sp,
                    color = FlareTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
