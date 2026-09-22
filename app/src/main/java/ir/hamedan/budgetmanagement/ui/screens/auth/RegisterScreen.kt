package ir.hamedan.budgetmanagement.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.repository.AuthRepository
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetApp
    val authApi = app.container.authApi
    val authRepository = app.container.authRepository
    val scope = rememberCoroutineScope()
    val isPersian = isPersianLocale()

    var phone by remember { mutableStateOf("+98") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneCode by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var phoneChallenge by remember { mutableStateOf<String?>(null) }
    var emailChallenge by remember { mutableStateOf<String?>(null) }
    var recoveryKey by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun requestOtp(destination: String, channel: String, onSuccess: (String) -> Unit) {
        if (destination.isBlank()) return
        loading = true
        error = null
        scope.launch {
            runCatching { authApi.requestOtp(destination, channel, "REGISTER") }
                .onSuccess { onSuccess(it.challengeId) }
                .onFailure { error = it.message ?: if (isPersian) "ارسال کد ناموفق بود" else "Failed to send code" }
            loading = false
        }
    }

    AuroraBackground()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (isPersian) "ساخت حساب کاربری" else "Create account")
        OutlinedTextField(phone, { phone = it }, label = { Text(if (isPersian) "شماره موبایل" else "Phone number") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { requestOtp(phone.trim(), "sms") { phoneChallenge = it } }, enabled = !loading && phone.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (isPersian) "ارسال کد پیامکی" else "Send SMS code") }
        if (phoneChallenge != null) {
            OutlinedTextField(phoneCode, { phoneCode = it.take(6) }, label = { Text(if (isPersian) "کد پیامکی" else "SMS code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
        }

        OutlinedTextField(email, { email = it }, label = { Text(if (isPersian) "ایمیل (اختیاری)" else "Email (optional)") }, leadingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (email.isNotBlank()) {
            Button(onClick = { requestOtp(email.trim(), "email") { emailChallenge = it } }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (isPersian) "ارسال کد ایمیل" else "Send email code") }
            if (emailChallenge != null) OutlinedTextField(emailCode, { emailCode = it.take(6) }, label = { Text(if (isPersian) "کد ایمیل" else "Email code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
        }

        OutlinedTextField(password, { password = it }, label = { Text(if (isPersian) "گذرواژه" else "Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), singleLine = true)
        error?.let { Text(it) }

        Button(
            onClick = {
                loading = true
                error = null

                scope.launch {
                    authRepository.register(
                        AuthRepository.RegistrationInput(
                            phoneNumber = phone.trim(),
                            email = email.trim().takeIf { it.isNotBlank() },
                            password = password,
                            otpChallengeId = phoneChallenge.orEmpty(),
                            otpCode = phoneCode,
                            emailOtpChallengeId = emailChallenge,
                            emailOtpCode = emailCode.takeIf { it.isNotBlank() }
                        )
                    )
                        .onSuccess { result ->
                            recoveryKey = result.recoveryKey
                        }
                        .onFailure {
                            error = it.message
                                ?: if (isPersian) {
                                    "ثبت‌نام ناموفق بود"
                                } else {
                                    "Registration failed"
                                }
                        }

                    loading = false
                }
            },
            enabled = !loading && phoneChallenge != null && phoneCode.length == 6 && password.length >= 8 && (email.isBlank() || (emailChallenge != null && emailCode.length == 6)),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator() else Text(if (isPersian) "ساخت حساب" else "Create account")
        }
        TextButton(onClick = onBack, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (isPersian) "بازگشت" else "Back") }
        Spacer(Modifier.height(8.dp))
    }

    recoveryKey?.let { key ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = onRegistered) { Text(if (isPersian) "ذخیره کردم" else "I saved it") } },
            title = { Text(if (isPersian) "کلید بازیابی را ذخیره کنید" else "Save your recovery key") },
            text = { Text(if (isPersian) "این کلید برای بازیابی حساب لازم است. آن را در یک محل امن خارج از برنامه نگه دارید:\n\n$key" else "This key is required for account recovery. Store it securely outside the app:\n\n$key") }
        )
    }
}