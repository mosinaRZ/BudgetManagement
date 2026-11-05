package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ir.hamedan.budgetmanagement.data.network.AdminApi

/** Name and role editor. Role chips are locked for the admin's own account. */
@Composable
internal fun EditUserDialog(
    user: AdminApi.AdminUser,
    isSelf: Boolean,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSave: (first: String, last: String, role: String) -> Unit
) {
    var first by remember(user.id) { mutableStateOf(user.firstName) }
    var last by remember(user.id) { mutableStateOf(user.lastName) }
    var role by remember(user.id) { mutableStateOf(user.role) }
    val changed = first.trim() != user.firstName || last.trim() != user.lastName || role != user.role

    AdminDialog(
        onDismiss = onDismiss,
        title = if (isPersian) "ویرایش کاربر" else "Edit user",
        icon = Icons.Default.Edit,
        actions = {
            DialogCancelButton(if (isPersian) "انصراف" else "Cancel", onDismiss)
            DialogConfirmButton(
                text = if (isPersian) "ذخیره" else "Save",
                enabled = changed,
                onClick = { onSave(first.trim(), last.trim(), role) }
            )
        }
    ) {
        OutlinedTextField(
            value = first,
            onValueChange = { first = it.take(80) },
            singleLine = true,
            label = { Text(if (isPersian) "نام" else "First name") },
            shape = AdminShapes.Field,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = last,
            onValueChange = { last = it.take(80) },
            singleLine = true,
            label = { Text(if (isPersian) "نام خانوادگی" else "Last name") },
            shape = AdminShapes.Field,
            modifier = Modifier.fillMaxWidth()
        )
        AdminChipGroup(
            title = if (isPersian) "نقش" else "Role",
            options = AllRoles.map { it to roleLabel(it, isPersian) },
            selected = role,
            onSelect = { role = it },
            enabled = !isSelf,
            modifier = Modifier.fillMaxWidth()
        )
        if (isSelf) {
            Text(
                if (isPersian) "نقش حساب خودتان را نمی‌توانید تغییر دهید." else "You can't change your own role.",
                style = MaterialTheme.typography.labelSmall,
                color = adminMuted(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}