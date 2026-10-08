package ir.hamedan.budgetmanagement.ui.screens.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.repository.AuthRepository
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val OTP_RESEND_SECONDS = 60L
private const val REGISTRATION_STEPS = 4

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetApp
    val authRepository = app.container.authRepository
    val scope = rememberCoroutineScope()
    val isPersian = isPersianLocale()
    val focusManager = LocalFocusManager.current

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var phoneCode by remember { mutableStateOf("") }
    var phoneChallenge by remember { mutableStateOf<String?>(null) }
    var phoneSentAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var recoveryKey by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    // One ticker drives the resend countdown.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    fun cooldown(sentAt: Long): Long = (OTP_RESEND_SECONDS - (now - sentAt) / 1000).coerceAtLeast(0)

    val normalizedPhone = AuthInputNormalizer.phone(phone)
    val phoneCooldown = cooldown(phoneSentAt)
    val passwordValid = password.isNotEmpty() &&
            PasswordPolicy.validate(password, isPersian) == null &&
            password == confirmPassword

    // Progress counts the four things the user has to get right, in order.
    val completedSteps = listOf(
        normalizedPhone != null,
        phoneChallenge != null,
        phoneCode.length == 6,
        passwordValid
    ).count { it }

    fun requestSmsCode(destination: String) {
        loading = true
        error = null
        scope.launch {
            authRepository.requestOtp(destination, "sms", "REGISTER")
                .onSuccess {
                    phoneChallenge = it.challengeId
                    phoneSentAt = System.currentTimeMillis()
                    now = phoneSentAt
                }
                .onFailure { error = authErrorMessage(it, isPersian, AuthContext.OTP_REQUEST) }
            loading = false
        }
    }

    fun submit() {
        if (loading) return
        val validPhone = normalizedPhone
        val challenge = phoneChallenge
        when {
            validPhone == null -> error = if (isPersian) "شماره موبایل معتبر نیست" else "Enter a valid phone number"
            challenge == null || phoneCode.length != 6 ->
                error = if (isPersian) "کد پیامکی ۶ رقمی را وارد کنید" else "Enter the 6-digit SMS code"
            else -> {
                val passwordProblem = PasswordPolicy.validate(password, isPersian)
                if (passwordProblem != null) {
                    error = passwordProblem
                    return
                }
                if (password != confirmPassword) {
                    error = if (isPersian) "گذرواژه و تکرار آن یکسان نیستند" else "The passwords do not match"
                    return
                }
                loading = true
                error = null
                focusManager.clearFocus()
                scope.launch {
                    authRepository.register(
                        AuthRepository.RegistrationInput(
                            phoneNumber = validPhone,
                            email = null,
                            password = password,
                            otpChallengeId = challenge,
                            otpCode = phoneCode
                        )
                    )
                        .onSuccess { result -> recoveryKey = result.recoveryKey }
                        .onFailure { error = authErrorMessage(it, isPersian, AuthContext.REGISTER) }
                    loading = false
                }
            }
        }
    }

    AuthScreenContainer {
        AuthHeader(
            icon = Icons.Default.PersonAdd,
            title = if (isPersian) "ساخت حساب کاربری" else "Create account",
            subtitle = if (isPersian) "شماره موبایل خود را تأیید کنید و گذرواژه تعیین کنید"
            else "Verify your phone number and choose a password"
        )

        RegistrationProgress(completed = completedSteps, total = REGISTRATION_STEPS, isPersian = isPersian)
        Spacer(Modifier.height(16.dp))

        // ---- Step 1: phone verification
        AuthCard {
            AuthSectionTitle(if (isPersian) "۱. تأیید شماره موبایل" else "1. Verify your phone")
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    // A different number invalidates the code that was sent to the old one.
                    phoneChallenge = null
                    phoneCode = ""
                    error = null
                },
                enabled = !loading,
                label = { Text(if (isPersian) "شماره موبایل" else "Phone number") },
                placeholder = {
                    Text(
                        if (isPersian) "مثلا: 09123456789" else "For example: 09123456789",
                        style = credentialTextStyle(),
                        color = Color.Gray
                    )
                },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = credentialTextStyle(),
                isError = phone.isNotBlank() && normalizedPhone == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { normalizedPhone?.let(::requestSmsCode) },
                enabled = !loading && normalizedPhone != null && phoneCooldown == 0L,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Sms, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        phoneCooldown > 0 -> if (isPersian) "ارسال مجدد تا $phoneCooldown ثانیه دیگر" else "Resend in ${phoneCooldown}s"
                        phoneChallenge != null -> if (isPersian) "ارسال مجدد کد پیامکی" else "Resend SMS code"
                        else -> if (isPersian) "ارسال کد پیامکی" else "Send SMS code"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (phoneChallenge != null) {
                OutlinedTextField(
                    value = phoneCode,
                    onValueChange = { phoneCode = AuthInputNormalizer.otpCode(it); error = null },
                    enabled = !loading,
                    label = { Text(if (isPersian) "کد پیامکی" else "SMS code") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Step 2: password
        AuthCard {
            AuthSectionTitle(if (isPersian) "۲. تعیین گذرواژه" else "2. Choose a password")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(PasswordPolicy.MAX_LENGTH + 1); error = null },
                enabled = !loading,
                label = { Text(if (isPersian) "گذرواژه" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPersian) "نمایش یا پنهان کردن گذرواژه" else "Toggle password visibility"
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = credentialTextStyle(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next, hintLocales = LocaleList(Locale("en"))),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )
            PasswordStrengthMeter(password = password, isPersian = isPersian)
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it.take(PasswordPolicy.MAX_LENGTH + 1); error = null },
                enabled = !loading,
                label = { Text(if (isPersian) "تکرار گذرواژه" else "Confirm password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = credentialTextStyle(),
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); submit() }),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (isPersian) "حداقل ${PasswordPolicy.MIN_LENGTH} کاراکتر؛ فقط حروف انگلیسی، عدد و نماد (بدون فاصله)."
                else "At least ${PasswordPolicy.MIN_LENGTH} characters; English letters, digits and symbols only (no spaces).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            error?.let { AuthErrorBanner(message = it) }

            LoadingButton(
                text = if (isPersian) "ساخت حساب" else "Create account",
                isLoading = loading,
                loadingText = if (isPersian) "در حال ساخت حساب..." else "Creating account...",
                onClick = ::submit
            )
        }

        TextButton(onClick = onBack, enabled = !loading, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isPersian) "بازگشت به ورود" else "Back to sign in",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    recoveryKey?.let { key ->
        RecoveryKeyDialog(recoveryKey = key, isPersian = isPersian, onConfirmed = onRegistered)
    }
}

/** Thin progress bar with a count, so the user always sees how close the account is to being created. */
@Composable
private fun RegistrationProgress(completed: Int, total: Int, isPersian: Boolean) {
    val done = completed == total
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPersian) "پیشرفت ثبت‌نام" else "Registration progress",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            if (done) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (isPersian) "$completed از $total" else "$completed of $total",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(completed.toFloat() / total)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/**
 * Shown exactly once after registration. The key cannot be shown again, so the user must
 * explicitly confirm that it was stored before the dialog can be closed.
 */
@Composable
private fun RecoveryKeyDialog(recoveryKey: String, isPersian: Boolean, onConfirmed: () -> Unit) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (isPersian) "کلید بازیابی را ذخیره کنید" else "Save your recovery key",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isPersian)
                        "اگر گذرواژه‌تان را فراموش کنید، فقط با این کلید می‌توانید حساب و اطلاعات رمزنگاری‌شده را بازیابی کنید. این کلید دوباره نمایش داده نمی‌شود؛ آن را در جایی امن و خارج از برنامه نگه دارید."
                    else
                        "If you forget your password, this key is the only way to recover your account and encrypted data. It will not be shown again; keep it somewhere safe outside the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                SelectionContainer {
                    Text(
                        text = recoveryKey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                        ),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = { copyRecoveryKey(context, recoveryKey); copied = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (copied) (if (isPersian) "کپی شد" else "Copied") else (if (isPersian) "کپی کلید" else "Copy key"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saved, onCheckedChange = { saved = it })
                    Text(
                        text = if (isPersian) "کلید را در جای امن ذخیره کرده‌ام" else "I have stored the key safely",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmed, enabled = saved, shape = RoundedCornerShape(12.dp)) {
                Text(if (isPersian) "ادامه" else "Continue")
            }
        }
    )
}

private fun copyRecoveryKey(context: Context, key: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText("recovery-key", key)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Keeps the key out of the clipboard preview overlay.
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    }
    clipboard.setPrimaryClip(clip)
}