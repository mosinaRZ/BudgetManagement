package ir.hamedan.budgetmanagement.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.launch

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

    var identifier by remember { mutableStateOf("") }
    var challengeId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var recoveryKey by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun requestOtp() {
        val value = identifier.trim()

        if (value.isBlank()) {
            error = if (isPersian) {
                "شماره موبایل یا ایمیل را وارد کنید"
            } else {
                "Enter your phone number or email"
            }
            return
        }

        // A new OTP request invalidates the previous challenge on the client.
        challengeId = null
        code = ""
        error = null
        loading = true

        scope.launch {
            authRepository.requestOtp(
                destination = value,
                channel = if (value.contains("@")) "email" else "sms",
                purpose = "PASSWORD_RESET"
            )
                .onSuccess { response ->
                    challengeId = response.challengeId
                }
                .onFailure { throwable ->
                    error = throwable.message
                        ?: if (isPersian) {
                            "ارسال کد ناموفق بود"
                        } else {
                            "Failed to send code"
                        }
                }

            loading = false
        }
    }

    fun resetPassword() {
        val currentChallengeId = challengeId

        if (currentChallengeId.isNullOrBlank()) {
            error = if (isPersian) {
                "ابتدا کد بازیابی را دریافت کنید"
            } else {
                "Request a recovery code first"
            }
            return
        }

        if (code.length != 6) {
            error = if (isPersian) {
                "کد تأیید باید ۶ رقم باشد"
            } else {
                "Verification code must contain 6 digits"
            }
            return
        }

        if (recoveryKey.trim().length < 16) {
            error = if (isPersian) {
                "کلید بازیابی معتبر نیست"
            } else {
                "Recovery key is invalid"
            }
            return
        }

        if (newPassword.length !in 8..256) {
            error = if (isPersian) {
                "گذرواژه باید بین ۸ تا ۲۵۶ کاراکتر باشد"
            } else {
                "Password must be between 8 and 256 characters"
            }
            return
        }

        loading = true
        error = null

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
                            onResetSuccess()
                        }
                        .onFailure { throwable ->
                            error = throwable.message
                                ?: if (isPersian) {
                                    "بازیابی ناموفق بود"
                                } else {
                                    "Reset failed"
                                }
                        }
                }
                .onFailure { throwable ->
                    error = throwable.message
                        ?: if (isPersian) {
                            "تأیید اطلاعات بازیابی ناموفق بود"
                        } else {
                            "Recovery verification failed"
                        }
                }

            loading = false
        }
    }

    AuroraBackground()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isPersian) {
                "بازیابی گذرواژه"
            } else {
                "Reset password"
            }
        )

        OutlinedTextField(
            value = identifier,
            onValueChange = {
                identifier = it
                challengeId = null
                code = ""
                error = null
            },
            label = {
                Text(
                    if (isPersian) {
                        "شماره موبایل یا ایمیل"
                    } else {
                        "Phone or email"
                    }
                )
            },
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !loading
        )

        Button(
            onClick = ::requestOtp,
            enabled = !loading && identifier.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isPersian) {
                    "ارسال کد بازیابی"
                } else {
                    "Send recovery code"
                }
            )
        }

        if (challengeId != null) {
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.filter(Char::isDigit).take(6)
                    error = null
                },
                label = {
                    Text(
                        if (isPersian) {
                            "کد تأیید"
                        } else {
                            "Verification code"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !loading
            )

            OutlinedTextField(
                value = recoveryKey,
                onValueChange = {
                    recoveryKey = it
                    error = null
                },
                label = {
                    Text(
                        if (isPersian) {
                            "کلید بازیابی"
                        } else {
                            "Recovery key"
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !loading
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    error = null
                },
                label = {
                    Text(
                        if (isPersian) {
                            "گذرواژه جدید"
                        } else {
                            "New password"
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !loading
            )

            Button(
                onClick = ::resetPassword,
                enabled = !loading &&
                        code.length == 6 &&
                        recoveryKey.trim().length >= 16 &&
                        newPassword.length in 8..256,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        if (isPersian) {
                            "تغییر گذرواژه"
                        } else {
                            "Reset password"
                        }
                    )
                }
            }
        }

        error?.let { Text(it) }

        Button(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isPersian) {
                    "بازگشت"
                } else {
                    "Back"
                }
            )
        }
    }
}