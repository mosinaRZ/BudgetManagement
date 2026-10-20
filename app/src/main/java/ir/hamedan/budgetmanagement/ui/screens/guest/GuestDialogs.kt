package ir.hamedan.budgetmanagement.ui.screens.guest

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
 * Set or change the guest passcode. Guests have no account password, so "change password" in
 * guest mode means this local passcode. The first time, there is no current passcode to ask for.
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
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val englishKeyboard = KeyboardOptions(keyboardType = KeyboardType.Password, hintLocales = LocaleList(Locale("en")))

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
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(
                text = if (hasPasscode) {
                    if (isPersian) "تغییر گذرواژه مهمان" else "Change guest passcode"
                } else {
                    if (isPersian) "تعیین گذرواژه مهمان" else "Set guest passcode"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isPersian)
                        "این گذرواژه فقط روی همین دستگاه است و به هیچ حسابی وصل نیست."
                    else
                        "This passcode exists only on this device and is not linked to any account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                if (hasPasscode) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it; errorMessage = null },
                        label = { Text(if (isPersian) "گذرواژه فعلی" else "Current passcode") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = englishKeyboard,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it; errorMessage = null },
                    label = { Text(if (isPersian) "گذرواژه جدید" else "New passcode") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = englishKeyboard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; errorMessage = null },
                    label = { Text(if (isPersian) "تکرار گذرواژه جدید" else "Repeat new passcode") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = englishKeyboard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isPersian) "ذخیره" else "Save", fontWeight = FontWeight.Bold)
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