package flare.client.app.ui.components.servers

import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.components.GeologicaMedium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.ui.components.FlareGlassMenu
import flare.client.app.ui.theme.FlareTheme




@Composable
fun FlareWizardSelectField(
    title: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    optionTitles: List<String>,
    onOptionSelected: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = GeologicaRegular,
            fontSize = 13.sp,
            color = FlareTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        val borderColor = if (expanded) accentColor else FlareTheme.colors.glassStroke.copy(alpha = 0.5f)
        val bgColor = if (expanded) accentColor.copy(alpha = 0.05f) else FlareTheme.colors.bgItem.copy(alpha = 0.5f)

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable { onExpandedChange(true) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = if (expanded) accentColor else FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 12.dp).size(20.dp)
                    )
                }

                Text(
                    text = value,
                    fontFamily = GeologicaMedium,
                    fontSize = 15.sp,
                    color = FlareTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = null,
                    tint = FlareTheme.colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            FlareGlassMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                items = options.mapIndexed { index, option ->
                    flare.client.app.util.GlassUtils.MenuItem(index, optionTitles[index]) {
                        onOptionSelected(option)
                    }
                },
                hazeState = hazeState,
                alignment = Alignment.CenterEnd
            )
        }
    }
}
