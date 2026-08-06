package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * بنر کوچک، غیرمودال یادآوری یک مجوزِ داده‌نشده. برخلاف OnboardingDialog، این یکی
 * روی محتوای برنامه شناور می‌مونه و کاربر رو مسدود نمی‌کنه؛ خودِ صفحه‌ای که این
 * کامپوننت رو صدا می‌زنه مسئول تصمیم «کِی نشونش بده» است (بر اساس
 * PermissionReminderPreferences.shouldRemindNow).
 */
@Composable
fun PermissionReminderBanner(
    visible: Boolean,
    isPersian: Boolean,
    permission: OnboardingPermission,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    onNeverAskAgain: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = permission.emoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) "یادت رفت: ${permission.titleFa}" else "Reminder: ${permission.titleEn}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isPersian) permission.reasonFa else permission.reasonEn,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = onLater) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isPersian) "بعداً" else "Later",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNeverAskAgain) {
                        Text(if (isPersian) "دیگه نشون نده" else "Don't remind me", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = onAllow, shape = RoundedCornerShape(12.dp)) {
                        Text(if (isPersian) "دادن دسترسی" else "Allow", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}