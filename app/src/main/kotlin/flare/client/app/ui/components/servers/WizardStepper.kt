package flare.client.app.ui.components.servers

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import flare.client.app.ui.theme.FlareTheme




@Composable
fun WizardStepper(
    activeIndex: Int,
    steps: List<String>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, stepTitle ->
            val isCompleted = index < activeIndex
            val isActive = index == activeIndex
            
            val circleColor by animateColorAsState(
                targetValue = when {
                    isActive -> accentColor
                    isCompleted -> accentColor.copy(alpha = 0.5f)
                    else -> FlareTheme.colors.textSecondary.copy(alpha = 0.15f)
                },
                animationSpec = tween(300),
                label = "circleColor"
            )

            val circleSize by animateDpAsState(
                targetValue = if (isActive) 24.dp else 20.dp,
                animationSpec = tween(300),
                label = "circleSize"
            )

            val textColor = when {
                isActive -> FlareTheme.colors.textPrimary
                isCompleted -> FlareTheme.colors.textSecondary
                else -> FlareTheme.colors.textSecondary.copy(alpha = 0.4f)
            }

            val textWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(circleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                fontFamily = GeologicaMedium,
                                fontSize = if (isActive) 11.sp else 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.White else FlareTheme.colors.textSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stepTitle,
                    fontFamily = GeologicaRegular,
                    fontSize = 10.sp,
                    fontWeight = textWeight,
                    color = textColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            if (index < steps.size - 1) {
                val lineColor by animateColorAsState(
                    targetValue = if (isCompleted) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.15f),
                    animationSpec = tween(300),
                    label = "lineColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 11.dp)
                        .height(2.dp)
                        .background(lineColor)
                )
            }
        }
    }
}
