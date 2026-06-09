package flare.client.app.ui.components.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import flare.client.app.R
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.ui.i18n.I18n
import android.view.WindowManager

enum class OnboardingStep {
    WELCOME, PERMISSIONS, FRAGMENTATION, MUX, SPLIT_A, SPLIT_B, SUCCESS
}

fun OnboardingStep.index(): Int = ordinal

private class OnboardingStrings(val strings: flare.client.app.ui.i18n.FlareStrings) {
    val welcomeTitle = strings.onboarding_welcome_title
    val welcomeSubtitle = strings.onboarding_welcome_question
    val btnYes = strings.option_yes
    val btnNo = strings.option_no
    
    val permissionsTitle = strings.onboarding_permissions_title
    val permissionsSubtitle = strings.onboarding_permissions_subtitle
    
    val permNotificationTitle = strings.onboarding_notifications_title
    val permNotificationDesc = strings.onboarding_notifications_desc
    
    val permBatteryTitle = strings.onboarding_battery_title
    val permBatteryDesc = strings.onboarding_battery_desc
    
    val btnNext = strings.btn_next
    
    val fragmentationTitle = strings.onboarding_fragmentation_title
    val fragmentationQuestion = strings.onboarding_fragmentation_question
    val fragmentationDesc = strings.onboarding_fragmentation_desc
    
    val muxTitle = strings.onboarding_mux_title
    val muxQuestion = strings.onboarding_mux_question
    val muxDesc = strings.onboarding_mux_desc
    
    val splitTitle = strings.settings_label_split_tunneling
    val splitSubtitle = strings.onboarding_split_subtitle
    
    val splitWhiteTitle = strings.onboarding_split_white_title
    val splitWhiteDesc = strings.onboarding_split_white_desc
    
    val splitBlackTitle = strings.onboarding_split_black_title
    val splitBlackDesc = strings.onboarding_split_black_desc
    
    val splitWhiteHeader = strings.onboarding_split_white_header
    val splitBlackHeader = strings.onboarding_split_black_header
    
    val presetRuTitle = strings.onboarding_preset_ru_title
    val presetRuDesc = strings.onboarding_preset_ru_desc
    
    val presetSocialTitle = strings.onboarding_preset_social_title
    val presetSocialDesc = strings.onboarding_preset_social_desc
    
    val presetAiTitle = strings.onboarding_preset_ai_title
    val presetAiDesc = strings.onboarding_preset_ai_desc
    
    val successTitle = strings.onboarding_success_title
    val successDesc = strings.onboarding_success_desc
    val btnToMain = strings.onboarding_btn_go_main
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun OnboardingDialog(
    onDismissRequest: () -> Unit,
    isNotificationGranted: Boolean,
    isBatteryOptimized: Boolean,
    onNotificationClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onFragmentationSelected: (Boolean) -> Unit,
    onMuxSelected: (Boolean) -> Unit,
    onSplitPresetsApplied: (mode: String, ru: Boolean, social: Boolean, ai: Boolean) -> Unit,
    onFinish: () -> Unit,
    accentColor: Int,
    hazeState: HazeState? = null
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }

    
    var isFragEnabled by remember { mutableStateOf(false) }
    var isMuxEnabled by remember { mutableStateOf(false) }
    
    
    var splitMode by remember { mutableStateOf<String?>(null) }
    
    
    var isRuSelected by remember { mutableStateOf(false) }
    var isSocialSelected by remember { mutableStateOf(false) }
    var isAiSelected by remember { mutableStateOf(false) }

    val geologicaMedium = FontFamily(Font(R.font.geologica_medium))
    val geologicaRegular = FontFamily(Font(R.font.geologica_regular))
    val context = LocalContext.current

    val strings = remember(I18n.strings) { OnboardingStrings(I18n.strings) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        SideEffect {
            dialogWindow?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.35f)
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .widthIn(max = 380.dp)
                    .wrapContentHeight()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    
                    if (currentStep != OnboardingStep.SUCCESS) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val steps = OnboardingStep.values().filter { it != OnboardingStep.SUCCESS }
                            steps.forEach { step ->
                                val active = step == currentStep
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .height(6.dp)
                                        .width(if (active) 16.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) Color(accentColor)
                                            else FlareTheme.colors.textSecondary.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }

                    
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState.index() > initialState.index()) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "onboarding_step_transition"
                    ) { step ->
                        when (step) {
                            OnboardingStep.WELCOME -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(Color(accentColor).copy(alpha = 0.15f))
                                            .border(1.5.dp, Color(accentColor).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_cloud),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(Color(accentColor)),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = strings.welcomeTitle,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 22.sp,
                                        fontFamily = geologicaMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = strings.welcomeSubtitle,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 15.sp,
                                        fontFamily = geologicaRegular,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .border(
                                                    1.dp,
                                                    FlareTheme.colors.textSecondary.copy(alpha = 0.3f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true),
                                                    onClick = {
                                                        onFinish()
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.btnNo,
                                                color = FlareTheme.colors.textPrimary,
                                                fontSize = 14.sp,
                                                fontFamily = geologicaMedium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .background(
                                                    color = Color(accentColor),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true),
                                                    onClick = {
                                                        currentStep = OnboardingStep.PERMISSIONS
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.btnYes,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontFamily = geologicaMedium
                                            )
                                        }
                                    }
                                }
                            }

                            OnboardingStep.PERMISSIONS -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = strings.permissionsTitle,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 20.sp,
                                        fontFamily = geologicaMedium,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Text(
                                        text = strings.permissionsSubtitle,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 14.sp,
                                        fontFamily = geologicaRegular,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    
                                    PermissionCard(
                                        title = strings.permNotificationTitle,
                                        description = strings.permNotificationDesc,
                                        isGranted = isNotificationGranted,
                                        onClick = onNotificationClick,
                                        accentColor = accentColor,
                                        geologicaMedium = geologicaMedium,
                                        geologicaRegular = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    
                                    PermissionCard(
                                        title = strings.permBatteryTitle,
                                        description = strings.permBatteryDesc,
                                        isGranted = isBatteryOptimized,
                                        onClick = onBatteryClick,
                                        accentColor = accentColor,
                                        geologicaMedium = geologicaMedium,
                                        geologicaRegular = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    
                                    AnimatedVisibility(
                                        visible = isNotificationGranted,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .background(
                                                    color = Color(accentColor),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true),
                                                    onClick = {
                                                        currentStep = OnboardingStep.FRAGMENTATION
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.btnNext,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontFamily = geologicaMedium
                                            )
                                        }
                                    }
                                }
                            }

                            OnboardingStep.FRAGMENTATION -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = strings.fragmentationTitle,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 20.sp,
                                        fontFamily = geologicaMedium
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = strings.fragmentationQuestion,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 16.sp,
                                        fontFamily = geologicaMedium
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = strings.fragmentationDesc,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 14.sp,
                                        fontFamily = geologicaRegular,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        OptionButton(
                                            text = strings.btnYes,
                                            isSelected = isFragEnabled,
                                            onClick = { isFragEnabled = true },
                                            accentColor = accentColor,
                                            geologicaMedium = geologicaMedium,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        OptionButton(
                                            text = strings.btnNo,
                                            isSelected = !isFragEnabled,
                                            onClick = { isFragEnabled = false },
                                            accentColor = accentColor,
                                            geologicaMedium = geologicaMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(
                                                color = Color(accentColor),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = {
                                                    onFragmentationSelected(isFragEnabled)
                                                    currentStep = OnboardingStep.MUX
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.btnNext,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontFamily = geologicaMedium
                                        )
                                    }
                                }
                            }

                            OnboardingStep.MUX -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = strings.muxTitle,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 20.sp,
                                        fontFamily = geologicaMedium
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = strings.muxQuestion,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 16.sp,
                                        fontFamily = geologicaMedium
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = strings.muxDesc,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 14.sp,
                                        fontFamily = geologicaRegular,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        OptionButton(
                                            text = strings.btnYes,
                                            isSelected = isMuxEnabled,
                                            onClick = { isMuxEnabled = true },
                                            accentColor = accentColor,
                                            geologicaMedium = geologicaMedium,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        OptionButton(
                                            text = strings.btnNo,
                                            isSelected = !isMuxEnabled,
                                            onClick = { isMuxEnabled = false },
                                            accentColor = accentColor,
                                            geologicaMedium = geologicaMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(
                                                color = Color(accentColor),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = {
                                                    onMuxSelected(isMuxEnabled)
                                                    currentStep = OnboardingStep.SPLIT_A
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.btnNext,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontFamily = geologicaMedium
                                        )
                                    }
                                }
                            }

                            OnboardingStep.SPLIT_A -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = strings.splitTitle,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 20.sp,
                                        fontFamily = geologicaMedium
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = strings.splitSubtitle,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 14.sp,
                                        fontFamily = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    
                                    SplitModeCard(
                                        title = strings.splitWhiteTitle,
                                        description = strings.splitWhiteDesc,
                                        isSelected = splitMode == "whitelist",
                                        onClick = { splitMode = "whitelist" },
                                        accentColor = accentColor,
                                        geologicaMedium = geologicaMedium,
                                        geologicaRegular = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    
                                    SplitModeCard(
                                        title = strings.splitBlackTitle,
                                        description = strings.splitBlackDesc,
                                        isSelected = splitMode == "blacklist",
                                        onClick = { splitMode = "blacklist" },
                                        accentColor = accentColor,
                                        geologicaMedium = geologicaMedium,
                                        geologicaRegular = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(
                                                color = Color(accentColor),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = {
                                                    if (splitMode != null) {
                                                        currentStep = OnboardingStep.SPLIT_B
                                                    } else {
                                                        
                                                        currentStep = OnboardingStep.SUCCESS
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.btnNext,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontFamily = geologicaMedium
                                        )
                                    }
                                }
                            }

                            OnboardingStep.SPLIT_B -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val isWhitelist = splitMode == "whitelist"
                                    Text(
                                        text = if (isWhitelist) strings.splitWhiteHeader
                                               else strings.splitBlackHeader,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 18.sp,
                                        fontFamily = geologicaMedium,
                                        lineHeight = 24.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    
                                    PresetCard(
                                        title = strings.presetRuTitle,
                                        description = strings.presetRuDesc,
                                        isSelected = isRuSelected,
                                        onClick = { isRuSelected = !isRuSelected },
                                        accentColor = accentColor,
                                        geologicaMedium = geologicaMedium,
                                        geologicaRegular = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    
                                    PresetCard(
                                        title = strings.presetSocialTitle,
                                        description = strings.presetSocialDesc,
                                        isSelected = isSocialSelected,
                                        onClick = { isSocialSelected = !isSocialSelected },
                                        accentColor = accentColor,
                                        geologicaMedium = geologicaMedium,
                                        geologicaRegular = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    
                                    PresetCard(
                                        title = strings.presetAiTitle,
                                        description = strings.presetAiDesc,
                                        isSelected = isAiSelected,
                                        onClick = { isAiSelected = !isAiSelected },
                                        accentColor = accentColor,
                                        geologicaMedium = geologicaMedium,
                                        geologicaRegular = geologicaRegular
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(
                                                color = Color(accentColor),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = {
                                                    onSplitPresetsApplied(splitMode ?: "whitelist", isRuSelected, isSocialSelected, isAiSelected)
                                                    currentStep = OnboardingStep.SUCCESS
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.btnNext,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontFamily = geologicaMedium
                                        )
                                    }
                                }
                            }

                            OnboardingStep.SUCCESS -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                            .border(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(Color(0xFF22C55E)),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = strings.successTitle,
                                        color = FlareTheme.colors.textPrimary,
                                        fontSize = 20.sp,
                                        fontFamily = geologicaMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = strings.successDesc,
                                        color = FlareTheme.colors.textSecondary,
                                        fontSize = 14.sp,
                                        fontFamily = geologicaRegular,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(
                                                color = Color(accentColor),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true),
                                                onClick = {
                                                    onFinish()
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.btnToMain,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontFamily = geologicaMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    accentColor: Int,
    geologicaMedium: FontFamily,
    geologicaRegular: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isGranted) Color(accentColor)
                else FlareTheme.colors.glassInputBg
            )
            .border(
                width = 1.dp,
                color = if (isGranted) Color.Transparent else FlareTheme.colors.dialogGlassStroke,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = !isGranted,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isGranted) Color.White else FlareTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = geologicaMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = if (isGranted) Color.White.copy(alpha = 0.7f) else FlareTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = geologicaRegular,
                    lineHeight = 18.sp
                )
            }

            if (isGranted) {
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Int,
    geologicaMedium: FontFamily,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(accentColor)
                else FlareTheme.colors.glassInputBg
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else FlareTheme.colors.dialogGlassStroke,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else FlareTheme.colors.textPrimary,
            fontSize = 14.sp,
            fontFamily = geologicaMedium
        )
    }
}

@Composable
fun SplitModeCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Int,
    geologicaMedium: FontFamily,
    geologicaRegular: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(accentColor)
                else FlareTheme.colors.glassInputBg
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else FlareTheme.colors.dialogGlassStroke,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = if (isSelected) Color.White else FlareTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontFamily = geologicaMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = if (isSelected) Color.White.copy(alpha = 0.7f) else FlareTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = geologicaRegular,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PresetCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Int,
    geologicaMedium: FontFamily,
    geologicaRegular: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(accentColor)
                else FlareTheme.colors.glassInputBg
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else FlareTheme.colors.dialogGlassStroke,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else FlareTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = geologicaMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = if (isSelected) Color.White.copy(alpha = 0.7f) else FlareTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = geologicaRegular,
                    lineHeight = 18.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
