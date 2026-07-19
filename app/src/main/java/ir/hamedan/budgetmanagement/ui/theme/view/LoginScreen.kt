package ir.hamedan.budgetmanagement.ui.theme.view

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.hamedan.budgetmanagement.data.SharedPreferences
import ir.hamedan.budgetmanagement.item.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()
    val scope = rememberCoroutineScope()

    // فیلدهای متنی ورودی
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 🚀 مشکل اول: متغیر وضعیت لودینگ که جا افتاده بود اضافه شد
    var isLoggingIn by remember { mutableStateOf(false) }

    val validUsername = "Cenna"
    val validPassword = "Sina2020@#$"

    val passwordFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val showBiometricPrompt = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            // 🚀 فعال کردن لودینگ دکمه به محض باز شدن سنسور
            isLoggingIn = true
            errorMessage = null

            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        // در صورت موفقیت، لودینگ فعال می‌ماند تا صفحه تغییر کند
                        onLoginSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // 🚀 در صورت لغو توسط کاربر یا خطا، لودینگ دکمه خاموش می‌شود
                        isLoggingIn = false
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // 🚀 اگر اثر انگشت اشتباه بود، لودینگ خاموش می‌شود تا کاربر دوباره تلاش کند
                        isLoggingIn = false
                        errorMessage = if (isPersian) "اثر انگشت شناسایی نشد" else "Biometric not recognized"
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (isPersian) "ورود به برنامه" else "App Login")
                .setNegativeButtonText(if (isPersian) "ورود با رمز عبور" else "Use Password")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }
    LaunchedEffect(Unit) {
        if (SharedPreferences.getBiometricEnabled(context)) {
            showBiometricPrompt()
        }
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2500) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            val message = if (isPersian) "برای خروج، دوباره دکمه بازگشت را بزنید" else "Press back again to exit"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isPersian) "خوش آمدید" else "Welcome Back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isPersian) "لطفاً مشخصات خود را وارد کنید" else "Please enter your credentials",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text(if (isPersian) "نام کاربری" else "Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next, // 👈 دکمه «بعدی» روی کیبورد
                        hintLocales = LocaleList(Locale("en"))
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = {
                            passwordFocusRequester.requestFocus() // 👈 انتقال فوکوس به فیلد رمز عبور
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(if (isPersian) "گذرواژه" else "Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done, // 👈 دکمه «تأیید/ورود» روی کیبورد
                        hintLocales = LocaleList(Locale("en"))
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            focusManager.clearFocus() // بستن کیبورد
                            if (!isLoggingIn && username.isNotBlank() && password.isNotBlank()) {
                                // 🚀 اجرای منطق دکمه ورود با زدن Enter/Done کیبورد
                                isLoggingIn = true
                                errorMessage = null

                                scope.launch {
                                    delay(1500)
                                    if (username.trim() == validUsername && password == validPassword) {
                                        isLoggingIn = false
                                        onLoginSuccess()
                                    } else {
                                        isLoggingIn = false
                                        errorMessage = if (isPersian)
                                            "نام کاربری یا رمز عبور اشتباه است"
                                        else
                                            "Invalid username or password"
                                    }
                                }
                            }
                        }
                    ),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester) // 👈 دریافت فوکوس
                )
                // 🚀 مشکل دوم: پیاده‌سازی منطق دکمه ورود و بررسی صحت اطلاعات هاردکد شده
                LoadingButton(
                    text = if (isPersian) "ورود به حساب" else "Sign In",
                    isLoading = isLoggingIn,
                    onClick = {
                        isLoggingIn = true
                        errorMessage = null

                        scope.launch {
                            delay(1500) // مکث کوتاه برای نمایش انیمیشن درخشش شیمر دکمه

                            if (username.trim() == validUsername && password == validPassword) {
                                isLoggingIn = false
                                onLoginSuccess()
                            } else {
                                isLoggingIn = false
                                errorMessage = if (isPersian)
                                    "نام کاربری یا رمز عبور اشتباه است"
                                else
                                    "Invalid username or password"
                            }
                        }
                    }
                )

                if (SharedPreferences.getBiometricEnabled(context)) {
                    // بررسی وجود سخت‌افزار حسگر اثر انگشت/بیومتریک
                    val hasBiometricHardware = remember(context) {
                        val biometricManager = BiometricManager.from(context)
                        val result = biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        // اگر سخت‌افزار وجود داشته باشد (چه اثر انگشت ثبت شده باشد چه نشده باشد) true برمی‌گرداند
                        result != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                                result != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                    }

// نمایش دکمه فقط در صورت وجود سخت‌افزار حسگر
                    if (hasBiometricHardware) {
                        TextButton(
                            onClick = { showBiometricPrompt() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = if (isPersian) "استفاده از اثر انگشت" else "Use Biometric")
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

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