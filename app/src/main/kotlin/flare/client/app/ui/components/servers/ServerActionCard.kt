package flare.client.app.ui.components.servers

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.ui.theme.FlareTheme




@Composable
fun ServerActionCard(
    title: String,
    description: String,
    icon: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.15f) else FlareTheme.colors.bgItem.copy(alpha = 0.85f),
        animationSpec = tween(250),
        label = "actionCardBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "actionCardBorder"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = tween(200),
        label = "actionCardScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBgColor)
            .border(0.5.dp, animatedBorderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                fontFamily = GeologicaMedium,
                fontSize = 16.sp,
                color = FlareTheme.colors.textPrimary
            )
            Text(
                text = description,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp,
                color = FlareTheme.colors.textSecondary
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
