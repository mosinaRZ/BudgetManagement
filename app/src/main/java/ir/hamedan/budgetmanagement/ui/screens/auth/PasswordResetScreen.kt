package ir.hamedan.budgetmanagement.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.Modifier
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
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RESET_OTP_RESEND_SECONDS = 60L

@Composable
fun PasswordResetScreen(
    onResetSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetApp
    val authRepository = app.container.authRepository
    val scope = rememberCoroutineScope()
    val isPersian = isPersianLocale()
    val focusManager = LocalFocusManager.current

    var identifier by remember { mutableStateOf("") }
    var challengeId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var recoveryKey by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var sentAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val cooldown = (RESET_OTP_RESEND_SECONDS - (now - sentAt) / 1000).coerceAtLeast(0)
    val normalizedIdentifier = AuthInputNormalizer.identifier(identifier)

    fun requestOtp() {
        val destination = normalizedIdentifier
        if (destination == null) {
            error = if (isPersian) "شماره موبایل یا ایمیل معتبر نیست" else "Enter a valid phone number or email"
            return
        }
        // A new OTP request invalidates the previous challenge on the client.
        challengeId = null
        code = ""
        error = null
        loading = true
        scope.launch {
            authRepository.requestOtp(
                destination = destination,
                channel = if (destination.contains("@")) "email" else "sms",
                purpose = "PASSWORD_RESET"
            )
                .onSuccess { response -> challengeId = response.challengeId; sentAt = System.currentTimeMillis(); now = sentAt }
                .onFailure { error = authErrorMessage(it, isPersian, AuthContext.OTP_REQUEST) }
            loading = false
        }
    }

    fun resetPassword() {
        if (loading) return
        val currentChallengeId = challengeId
        if (currentChallengeId.isNullOrBlank()) {
            error = if (isPersian) "ابتدا کد بازیابی را دریافت کنید" else "Request a recovery code first"
            return
        }
        if (code.length != 6) {
            error = if (isPersian) "کد تأیید باید ۶ رقم باشد" else "Verification code must contain 6 digits"
            return
        }
        if (recoveryKey.trim().length < 16) {
            error = if (isPersian) "کلید بازیابی معتبر نیست" else "Recovery key is invalid"
            return
        }
        PasswordPolicy.validate(newPassword, isPersian)?.let { problem ->
            error = problem
            return
        }

        loading = true
        error = null
        focusManager.clearFocus()

        scope.launch {
            authRepository.prepareRecovery(
                challengeId = currentChallengeId,
                otpCode = code,
                recoveryKey = recoveryKey.trim()
            )
                .onSuccess { preparation ->
                    authRepository.resetPassword(
                        recoverySessionToken = preparation.recoverySessionToken,
                        newPassword = newPassword,
                        recoveryKey = recoveryKey.trim(),
                        recoveryKeyEnvelope = preparation.recoveryKeyEnvelope,
                        recoveryKeyNonce = preparation.recoveryKeyNonce,
                        kdfSalt = preparation.kdfSalt,
                        userId = preparation.userId
                    )
                        .onSuccess {
                            (context.applicationContext as BudgetApp).container.rememberedLoginStore.clearBiometricCredential()
                            onResetSuccess()
                        }
                        .onFailure { error = authErrorMessage(it, isPersian, AuthContext.RESET_PASSWORD) }
                }
                .onFailure { error = authErrorMessage(it, isPersian, AuthContext.RESET_PASSWORD) }

            loading = false
        }
    }

    AuthScreenContainer {
        AuthHeader(
            icon = Icons.Default.LockReset,
            title = if (isPersian) "بازیابی گذرواژه" else "Reset password",
            subtitle = if (isPersian) "با کد تأیید و کلید بازیابی، گذرواژه جدید بسازید" else "Use your verification code and recovery key to set a new password"
        )

        AuthCard {
            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    challengeId = null
                    code = ""
                    error = null
                },
                enabled = !loading,
                label = { Text(if (isPersian) "شماره موبایل یا ایمیل" else "Phone or email") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = credentialTextStyle(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = ::requestOtp,
                enabled = !loading && identifier.isNotBlank() && cooldown == 0L,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = when {
                        cooldown > 0 -> if (isPersian) "ارسال مجدد تا $cooldown ثانیه دیگر" else "Resend in ${cooldown}s"
                        challengeId != null -> if (isPersian) "ارسال مجدد کد" else "Resend code"
                        else -> if (isPersian) "ارسال کد بازیابی" else "Send recovery code"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            AnimatedVisibility(visible = challengeId != null) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = AuthInputNormalizer.otpCode(it); error = null },
                        enabled = !loading,
                        label = { Text(if (isPersian) "کد تأیید" else "Verification code") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = credentialTextStyle(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = recoveryKey,
                        onValueChange = { recoveryKey = it.filterNot(Char::isWhitespace); error = null },
                        enabled = !loading,
                        label = { Text(if (isPersian) "کلید بازیابی" else "Recovery key") },
                        leadingIcon = { Icon(Icons.Default.Key, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = credentialTextStyle(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it.take(PasswordPolicy.MAX_LENGTH + 1); error = null },
                        enabled = !loading,
                        label = { Text(if (isPersian) "گذرواژه جدید" else "New password") },
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); resetPassword() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    PasswordStrengthMeter(password = newPassword, isPersian = isPersian)
                }
            }

            AnimatedVisibility(visible = error != null) {
                error?.let { AuthErrorBanner(message = it) }
            }

            if (challengeId != null) {
                LoadingButton(
                    text = if (isPersian) "تغییر گذرواژه" else "Reset password",
                    isLoading = loading,
                    loadingText = if (isPersian) "در حال بازیابی..." else "Resetting...",
                    onClick = ::resetPassword
                )
            }
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
}