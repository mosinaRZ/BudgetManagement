package ir.hamedan.budgetmanagement.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.security.RememberedLoginStore
import ir.hamedan.budgetmanagement.data.preferences.SharedPreferences
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.NotificationHelper
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    localUnlockOnly: Boolean = false
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as BudgetApp
    val authRepository = remember { app.container.authRepository }
    val rememberedLoginStore = remember { app.container.rememberedLoginStore }

    var username by remember { mutableStateOf(rememberedLoginStore.identifier().orEmpty()) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var biometricPromptShown by remember(localUnlockOnly) { mutableStateOf(false) }
    var biometricCanceled by remember(localUnlockOnly) { mutableStateOf(false) }

    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val biometricLoginEnabled = remember(context) { SharedPreferences.getBiometricEnabled(context) }
    val biometricReady = biometricLoginEnabled && rememberedLoginStore.hasBiometricCredential() && rememberedLoginStore.canUseStrongBiometric()
    val passwordUnlockReady = rememberedLoginStore.hasPasswordVerifier()

    fun finishLocalUnlock() {
        isLoggingIn = false
        password = ""
        onLoginSuccess()
    }

    fun startBiometricUnlock() {
        if (isLoggingIn || biometricPromptShown) return
        val activity = context as? FragmentActivity
        val cipher = rememberedLoginStore.prepareDecryptionCipher()
        if (activity == null || cipher == null) {
            errorMessage = if (isPersian) "ورود بیومتریک آماده نیست؛ لطفاً با رمز عبور ادامه دهید." else "Biometric login is unavailable; continue with your password."
            return
        }

        biometricPromptShown = true
        isLoggingIn = true
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Local unlock only. No access/refresh token is touched and no network request is made.
                finishLocalUnlock()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isLoggingIn = false
                biometricPromptShown = false
                errorMessage = null
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                errorMessage = if (isPersian) "اثر انگشت شناسایی نشد؛ دوباره تلاش کنید." else "Biometric not recognized; try again."
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (isPersian) "تأیید هویت برای ورود" else "Verify to continue")
                .setSubtitle(if (isPersian) "برای ورود به برنامه اثر انگشت خود را تأیید کنید" else "Confirm your biometric to unlock the app")
                .setNegativeButtonText("\u200B")
                .build(),
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    fun submitLocalPassword() {
        if (isLoggingIn) return
        if (!passwordUnlockReady) {
            errorMessage = if (isPersian) "رمز ورود محلی هنوز آماده نیست؛ یک‌بار با اتصال به سرور وارد شوید." else "Local password verification is not initialized; sign in once with the server."
            return
        }
        isLoggingIn = true
        errorMessage = null
        if (rememberedLoginStore.verifyPassword(password)) {
            finishLocalUnlock()
        } else {
            isLoggingIn = false
            errorMessage = if (isPersian) "رمز عبور نادرست است." else "Incorrect password."
        }
    }

    fun submitServerLogin() {
        if (isLoggingIn) return
        val validationError = LoginInputValidator.validate(username, password, isPersian)
        if (validationError != null) {
            errorMessage = validationError
            return
        }
        isLoggingIn = true
        errorMessage = null
        scope.launch {
            authRepository.login(username.trim(), password)
                .onSuccess {
                    runCatching {
                        app.seedDefaultCategoriesIfNeeded()
                        rememberedLoginStore.saveIdentifier(username.trim())
                        rememberedLoginStore.savePasswordVerifier(password)
                        NotificationHelper.sendWelcomeIfNeeded(context)

                        val activity = context as? FragmentActivity
                        val biometricCipher = rememberedLoginStore.prepareEncryptionCipher()
                        if (SharedPreferences.getBiometricEnabled(context) && activity != null && biometricCipher != null && !rememberedLoginStore.hasBiometricCredential()) {
                            val passwordToRemember = password
                            val prompt = BiometricPrompt(
                                activity,
                                ContextCompat.getMainExecutor(context),
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        result.cryptoObject?.cipher?.let { rememberedLoginStore.savePassword(it, passwordToRemember) }
                                        isLoggingIn = false
                                        password = ""
                                        onLoginSuccess()
                                    }

                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        isLoggingIn = false
                                        password = ""
                                        onLoginSuccess()
                                    }
                                }
                            )
                            prompt.authenticate(
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle(if (isPersian) "فعال‌سازی ورود با اثر انگشت" else "Enable biometric login")
                                    .setSubtitle(if (isPersian) "برای ورودهای بعدی، اثر انگشت خود را تأیید کنید" else "Confirm your biometric for future logins")
                                    .setNegativeButtonText(if (isPersian) "بعداً" else "Later")
                                    .build(),
                                BiometricPrompt.CryptoObject(biometricCipher)
                            )
                        } else {
                            isLoggingIn = false
                            password = ""
                            onLoginSuccess()
                        }
                    }.onFailure { error ->
                        isLoggingIn = false
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: if (isPersian) "آماده‌سازی اولیه برنامه انجام نشد" else "Initial app setup failed"
                    }
                }
                .onFailure { error ->
                    isLoggingIn = false
                    errorMessage = (error as? ir.hamedan.budgetmanagement.data.network.ApiException)?.userMessage(isPersian)
                        ?: error.message?.takeIf { it.isNotBlank() }
                                ?: if (isPersian) "ورود ناموفق بود" else "Login failed"
                }
        }
    }

    LaunchedEffect(localUnlockOnly, biometricReady) {
        if (localUnlockOnly && biometricReady) {
            startBiometricUnlock()
        }
    }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        val lastBackPressTime = LocalLoginBackPress.current
        if (currentTime - lastBackPressTime < 2500) {
            (context as? Activity)?.finish()
        } else {
            LocalLoginBackPress.current = currentTime
            Toast.makeText(context, if (isPersian) "برای خروج، دوباره دکمه بازگشت را بزنید" else "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (localUnlockOnly) (if (isPersian) "خوش آمدید" else "Welcome back") else (if (isPersian) "خوش آمدید" else "Welcome Back"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (localUnlockOnly) (if (isPersian) "برای ادامه، هویت خود را تأیید کنید" else "Verify your identity to continue") else (if (isPersian) "لطفاً مشخصات خود را وارد کنید" else "Please enter your credentials"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    readOnly = false,
                    label = { Text(if (isPersian) "شماره موبایل یا ایمیل" else "Phone or email") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next, hintLocales = LocaleList(Locale("en"))),
                    keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                if (true) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text(if (isPersian) "گذرواژه" else "Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); if (localUnlockOnly) submitLocalPassword() else submitServerLogin() }),
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) { Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester)
                    )
                }

                LoadingButton(
                    text = when {
                        !localUnlockOnly -> if (isPersian) "ورود به حساب" else "Sign In"
                        password.isBlank() && biometricReady -> if (isPersian) "ورود با اثر انگشت" else "Sign in with fingerprint"
                        else -> if (isPersian) "ورود با رمز عبور" else "Sign in with password"
                    },
                    isLoading = isLoggingIn,
                    onClick = when {
                        !localUnlockOnly -> ::submitServerLogin
                        password.isBlank() && biometricReady -> ::startBiometricUnlock
                        else -> ::submitLocalPassword
                    }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = onForgotPassword,
                        enabled = !isLoggingIn
                    ) {
                        Text(
                            text = if (isPersian) "رمز عبور را فراموش کرده‌اید؟" else "Forgot your password?",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isPersian) "حساب کاربری ندارید؟" else "Don't have an account?",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(6.dp))
                            TextButton(
                                onClick = onRegister,
                                enabled = !isLoggingIn,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    if (isPersian) "ساخت حساب" else "Create account",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(errorMessage != null) {
                    errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}

private object LocalLoginBackPress { var current: Long = 0L }

@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(16.dp)

    // انیمیشن ترنزیشن بی‌نهایت برای جابه‌جایی افکت نوری لودینگ
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")

    // ۱. انیمیشن موقعیت افقی نور درخشش (از چپ به راست)
    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    // ۲. انیمیشن پالس ملایم متن در حالت لودینگ
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )

    // تعریف گریدینت درخشش خطی بر اساس رنگ‌های تم نانو بنانا
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary, // #002114 یا #408A71
            Color(0xFFB0E4CC).copy(alpha = 0.8f), // رنگ درخشش روشن اسپلش
            MaterialTheme.colorScheme.primary
        ),
        start = Offset(shimmerTranslateX - 300f, 0f),
        end = Offset(shimmerTranslateX, 0f)
    )

    Button(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = buttonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        ),
        enabled = !isLoading,
        contentPadding = PaddingValues(0.dp) // حذف پدینگ برای اعمال یکدست گریدینت
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isLoading) Modifier.background(shimmerBrush)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLoading) (if (LocaleHelper.getLanguage(LocalContext.current) == "fa") "در حال ورود..." else "Signing In...") else text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (isLoading) textAlpha else 1f)
            )
        }
    }
}