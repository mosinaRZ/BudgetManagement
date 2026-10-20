package ir.hamedan.budgetmanagement.ui.screens.guest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthErrorBanner
import ir.hamedan.budgetmanagement.ui.screens.auth.PasswordStrengthMeter
import ir.hamedan.budgetmanagement.ui.screens.auth.credentialTextStyle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.screens.auth.PasswordPolicy
import kotlinx.coroutines.launch

/**
 * Set or change the guest passcode. Same layout and behaviour as the account's change-password
 * dialog (visibility toggles, strength meter, confirmation field, error banner, progress on save).
 * Guests have no account password, so "change password" here means this local passcode. The first
 * time, there is no current passcode to ask for.
 */
@Composable
fun GuestPasscodeDialog(
    isPersian: Boolean,
    hasPasscode: Boolean,
    onDismiss: () -> Unit,
    onSubmit: suspend (currentPasscode: String, newPasscode: String) -> Result<Unit>,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isCurrentVisible by remember { mutableStateOf(false) }
    var isNewVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val englishKeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        hintLocales = LocaleList(Locale("en"))
    )

    fun submit() {
        if (isSubmitting) return
        val problem = when {
            hasPasscode && current.isEmpty() -> if (isPersian) "گذرواژه فعلی را وارد کنید" else "Enter your current passcode"
            new.isEmpty() || confirm.isEmpty() -> if (isPersian) "لطفاً تمام فیلدها را پر کنید" else "Please fill all fields"
            new != confirm -> if (isPersian) "گذرواژه جدید و تکرار آن مطابقت ندارند" else "The passcodes do not match"
            hasPasscode && new == current -> if (isPersian) "گذرواژه جدید باید با فعلی متفاوت باشد" else "The new passcode must differ from the current one"
            else -> PasswordPolicy.validate(new, isPersian)
        }
        if (problem != null) {
            errorMessage = problem
            return
        }
        isSubmitting = true
        errorMessage = null
        scope.launch {
            onSubmit(current, new)
                .onSuccess { onSuccess() }
                .onFailure {
                    errorMessage = if (isPersian) "گذرواژه فعلی نادرست است." else "The current passcode is incorrect."
                }
            isSubmitting = false
        }
    }

    AlertDialog(
        // The save cannot be cancelled half-way, so the dialog stays until it finishes.
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (hasPasscode) {
                    if (isPersian) "تغییر گذرواژه مهمان" else "Change guest passcode"
                } else {
                    if (isPersian) "تعیین گذرواژه مهمان" else "Set guest passcode"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isPersian)
                        "این گذرواژه فقط روی همین دستگاه ذخیره می‌شود و به هیچ حسابی وصل نیست."
                    else
                        "This passcode is stored only on this device and is not linked to any account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (hasPasscode) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it.take(256); errorMessage = null },
                        enabled = !isSubmitting,
                        label = { Text(if (isPersian) "گذرواژه فعلی" else "Current passcode") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = credentialTextStyle(),
                        keyboardOptions = englishKeyboardOptions,
                        visualTransformation = if (isCurrentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isCurrentVisible = !isCurrentVisible }) {
                                Icon(
                                    imageVector = if (isCurrentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPersian) "نمایش یا پنهان کردن گذرواژه" else "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it.take(PasswordPolicy.MAX_LENGTH + 1); errorMessage = null },
                    enabled = !isSubmitting,
                    label = { Text(if (isPersian) "گذرواژه جدید" else "New passcode") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = englishKeyboardOptions,
                    visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isNewVisible = !isNewVisible }) {
                            Icon(
                                imageVector = if (isNewVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPersian) "نمایش یا پنهان کردن گذرواژه" else "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                PasswordStrengthMeter(password = new, isPersian = isPersian)

                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.take(PasswordPolicy.MAX_LENGTH + 1); errorMessage = null },
                    enabled = !isSubmitting,
                    label = { Text(if (isPersian) "تکرار گذرواژه جدید" else "Confirm new passcode") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    isError = confirm.isNotEmpty() && confirm != new,
                    keyboardOptions = englishKeyboardOptions.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isPersian) "حداقل ${PasswordPolicy.MIN_LENGTH} کاراکتر؛ فقط حروف انگلیسی، عدد و نماد (بدون فاصله)."
                    else "At least ${PasswordPolicy.MIN_LENGTH} characters; English letters, digits and symbols only (no spaces).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { AuthErrorBanner(message = it) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isPersian) "ذخیره گذرواژه" else "Save passcode")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    )
}

/**
 * Deletes the guest workspace on this device. When a passcode exists it must be entered, so a
 * person holding the unlocked phone cannot wipe the workspace without the passcode.
 */
@Composable
fun GuestDeleteWorkspaceDialog(
    isPersian: Boolean,
    hasPasscode: Boolean,
    onDismiss: () -> Unit,
    onSubmit: suspend (passcode: String) -> Result<Unit>,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var passcode by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (isSubmitting) return
        if (hasPasscode && passcode.isEmpty()) {
            errorMessage = if (isPersian) "گذرواژه مهمان را وارد کنید" else "Enter the guest passcode"
            return
        }
        isSubmitting = true
        errorMessage = null
        scope.launch {
            onSubmit(passcode)
                .onSuccess { onSuccess() }
                .onFailure {
                    errorMessage = if (isPersian) "گذرواژه نادرست است یا حذف انجام نشد." else "Incorrect passcode, or deletion failed."
                }
            isSubmitting = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = {
            Text(
                text = if (isPersian) "حذف داده‌های مهمان" else "Delete guest data",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isPersian)
                        "همه تراکنش‌ها، دسته‌بندی‌ها و تنظیمات مهمان از این دستگاه حذف می‌شوند. این کار برگشت‌پذیر نیست."
                    else
                        "All transactions, categories and settings of the guest workspace will be removed from this device. This cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasPasscode) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it; errorMessage = null },
                        label = { Text(if (isPersian) "گذرواژه مهمان" else "Guest passcode") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, hintLocales = LocaleList(Locale("en"))),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = ::submit,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isPersian) "حذف" else "Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    )
}