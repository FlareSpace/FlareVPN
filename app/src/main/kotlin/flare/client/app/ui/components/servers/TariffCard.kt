package flare.client.app.ui.components.servers

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.ui.theme.FlareTheme




@Composable
fun TariffCard(
    title: String,
    description: String,
    price: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.15f) else FlareTheme.colors.bgItem
    val borderColor = if (isSelected) accentColor else FlareTheme.colors.glassStroke

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                fontFamily = GeologicaMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else FlareTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontFamily = GeologicaRegular,
                fontSize = 13.sp,
                color = FlareTheme.colors.textSecondary
            )
        }
        
        Text(
            text = price,
            fontFamily = GeologicaMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) accentColor else FlareTheme.colors.textPrimary,
            textAlign = TextAlign.End
        )
    }
}
