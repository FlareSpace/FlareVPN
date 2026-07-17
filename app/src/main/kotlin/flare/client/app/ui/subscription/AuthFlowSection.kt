package flare.client.app.ui.subscription

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flare.client.app.R
import flare.client.app.data.auth.AuthManager
import flare.client.app.ui.components.FlareCard
import flare.client.app.data.model.DisplayItem
import flare.client.app.ui.theme.FlareTheme
import flare.client.app.ui.components.GeologicaMedium
import flare.client.app.ui.components.GeologicaRegular
import flare.client.app.ui.i18n.I18n
import kotlinx.coroutines.launch

enum class AuthStep {
    CHOICE,
    TELEGRAM_POLLING,
    CREATE_ANON,
    LOGIN_ANON
}

@Composable
fun AuthFlowSection(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit
) {
    var step by remember { mutableStateOf(AuthStep.CHOICE) }
    var authError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    FlareCard(
        cornerType = DisplayItem.CornerType.ALL,
        paddingHorizontal = 24.dp,
        paddingVertical = 24.dp,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(220)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(220)))
                    }
                },
                label = "authStepTransition"
            ) { currentStep ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentStep) {
                AuthStep.CHOICE -> {
                    Text(
                        text = I18n.strings.auth_login_title,
                        fontSize = 22.sp,
                        fontFamily = GeologicaMedium,
                        color = FlareTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = I18n.strings.auth_choice_desc,
                        fontSize = 15.sp,
                        fontFamily = GeologicaRegular,
                        color = FlareTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    
                    AuthButton(
                        text = I18n.strings.sub_auth_login_btn,
                        icon = R.drawable.ic_telegram,
                        iconTint = Color(0xFF2CA5E0),
                        onClick = {
                            step = AuthStep.TELEGRAM_POLLING
                            authError = null
                            coroutineScope.launch {
                                val uuid = authManager.startAuthFlow()
                                if (uuid != null) {
                                    val success = authManager.pollForToken(uuid) == 0
                                    if (success) {
                                        onAuthSuccess()
                                    } else {
                                        authError = I18n.strings.sub_auth_timeout
                                        step = AuthStep.CHOICE
                                    }
                                } else {
                                    authError = I18n.strings.sub_auth_error
                                    step = AuthStep.CHOICE
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    
                    AuthButton(
                        text = I18n.strings.auth_create_anon_btn,
                        icon = R.drawable.ic_key_create,
                        iconTint = FlareTheme.colors.textPrimary,
                        onClick = {
                            step = AuthStep.CREATE_ANON
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    
                    AuthButton(
                        text = I18n.strings.auth_login_key_btn,
                        icon = R.drawable.ic_key_login,
                        iconTint = FlareTheme.colors.textPrimary,
                        onClick = {
                            step = AuthStep.LOGIN_ANON
                        }
                    )
                }

                AuthStep.TELEGRAM_POLLING -> {
                    Text(
                        text = I18n.strings.sub_auth_title,
                        fontSize = 22.sp,
                        fontFamily = GeologicaMedium,
                        color = FlareTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = FlareTheme.colors.accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = I18n.strings.sub_auth_waiting,
                        color = FlareTheme.colors.textPrimary,
                        fontFamily = GeologicaRegular,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = I18n.strings.btn_cancel,
                        color = Color(0xFFFF3B30).copy(alpha = 0.8f),
                        fontFamily = GeologicaMedium,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { step = AuthStep.CHOICE }.padding(8.dp)
                    )
                }

                AuthStep.CREATE_ANON -> {
                    var generatedKey by remember { mutableStateOf<String?>(null) }
                    var isKeyVisible by remember { mutableStateOf(false) }
                    var isCreating by remember { mutableStateOf(false) }
                    val clipboardManager = LocalClipboardManager.current

                    LaunchedEffect(Unit) {
                        isCreating = true
                        val key = authManager.createAnonymousAccount()
                        generatedKey = key
                        isCreating = false
                        if (key != null) {
                            clipboardManager.setText(AnnotatedString(key))
                        } else {
                            authError = I18n.strings.auth_create_anon_err
                        }
                    }

                    Text(
                        text = I18n.strings.auth_anon_key_title,
                        fontSize = 22.sp,
                        fontFamily = GeologicaMedium,
                        color = FlareTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isCreating) {
                        CircularProgressIndicator(color = FlareTheme.colors.accent)
                    } else if (generatedKey != null) {
                        Text(
                            text = I18n.strings.auth_anon_key_desc,
                            fontSize = 14.sp,
                            fontFamily = GeologicaRegular,
                            color = Color(0xFFFF9500),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FlareTheme.colors.bgSurface)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isKeyVisible) generatedKey!! else maskKey(generatedKey!!),
                                fontFamily = GeologicaMedium,
                                fontSize = 16.sp,
                                color = FlareTheme.colors.accent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Icon(
                                painter = painterResource(if (isKeyVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                contentDescription = "Toggle Key Visibility",
                                tint = FlareTheme.colors.textSecondary,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(22.dp)
                                    .clickable { isKeyVisible = !isKeyVisible }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = I18n.strings.auth_copied_to_clipboard,
                            fontSize = 12.sp,
                            color = FlareTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        AuthButton(
                            text = I18n.strings.auth_continue_btn,
                            onClick = { onAuthSuccess() }
                        )
                    } else {
                        Text(
                            text = I18n.strings.auth_generate_key_err,
                            color = Color(0xFFFF3B30),
                            modifier = Modifier.padding(16.dp)
                        )
                        AuthButton(
                            text = I18n.strings.auth_back_btn,
                            onClick = { step = AuthStep.CHOICE }
                        )
                    }
                }

                AuthStep.LOGIN_ANON -> {
                    var inputKey by remember { mutableStateOf("") }
                    var isLoggingIn by remember { mutableStateOf(false) }

                    Text(
                        text = I18n.strings.auth_login_key_btn,
                        fontSize = 22.sp,
                        fontFamily = GeologicaMedium,
                        color = FlareTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = FlareTheme.colors.bgSurface,
                            unfocusedContainerColor = FlareTheme.colors.bgSurface,
                            focusedTextColor = FlareTheme.colors.textPrimary,
                            unfocusedTextColor = FlareTheme.colors.textPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(I18n.strings.auth_key_placeholder) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isLoggingIn) {
                        CircularProgressIndicator(color = FlareTheme.colors.accent)
                    } else {
                        AuthButton(
                            text = I18n.strings.auth_login_btn,
                            onClick = {
                                isLoggingIn = true
                                authError = null
                                coroutineScope.launch {
                                    val success = authManager.loginAnonymous(inputKey.trim())
                                    isLoggingIn = false
                                    if (success) {
                                        onAuthSuccess()
                                    } else {
                                        authError = I18n.strings.auth_invalid_key_err
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = I18n.strings.btn_cancel,
                            color = FlareTheme.colors.textSecondary,
                            fontFamily = GeologicaMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { step = AuthStep.CHOICE }.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

            if (authError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = authError!!,
                    color = Color(0xFFFF3B30),
                    fontFamily = GeologicaMedium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AuthButton(
    text: String,
    icon: Int? = null,
    iconTint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlareTheme.colors.bgItem)
            .border(1.dp, FlareTheme.colors.glassStroke, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontFamily = GeologicaMedium,
            fontSize = 15.sp,
            color = FlareTheme.colors.textPrimary
        )
    }
}

private fun maskKey(key: String): String {
    val parts = key.split("-")
    return if (parts.size >= 4) {
        "${parts[0]}-${parts[1]}-••••-••••"
    } else {
        val half = key.length / 2
        key.take(half) + "•".repeat(key.length - half)
    }
}
