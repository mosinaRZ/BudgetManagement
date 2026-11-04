package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale

/**
 * The administrator's own app: reached straight after sign-in when the account's role is admin and
 * completely separate from the regular budget screens (no tabs, sync UI or budget data here).
 */
@Composable
fun AdminRootScreen(
    selfUserId: String,
    onSignOut: () -> Unit
) {
    val isPersian = isPersianLocale()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pendingUserFilter by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showSignOut by remember { mutableStateOf(false) }
    var accessLost by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Space for the floating header above and the capsule navigation below.
    val padding = PaddingValues(top = 92.dp, bottom = 100.dp)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AuroraBackground()

        when (tab) {
            0 -> AdminUsersScreen(
                snackbarHostState = snackbarHostState,
                selfUserId = selfUserId,
                onForbidden = { accessLost = true },
                onShowUserAnalytics = { id, label ->
                    pendingUserFilter = id to label
                    tab = 1
                },
                contentPadding = padding
            )
            else -> AdminAnalyticsScreen(
                pendingUserFilter = pendingUserFilter,
                onPendingConsumed = { pendingUserFilter = null },
                onForbidden = { accessLost = true },
                contentPadding = padding
            )
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer12()
            Column(Modifier.weight(1f)) {
                Text(
                    if (isPersian) "پنل مدیریت سیدنا" else "Cidna Admin",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
                Text(
                    if (tab == 0) (if (isPersian) "مدیریت کاربران" else "User management") else (if (isPersian) "آنالیز و امنیت" else "Analytics & security"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(
                onClick = { showSignOut = true },
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, if (isPersian) "خروج" else "Sign out", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            isPersian = isPersian,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
        )

        // Floating capsule navigation, same language as the main app's bottom bar.
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            shadowElevation = 8.dp
        ) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NavTab(Icons.Default.People, if (isPersian) "کاربران" else "Users", tab == 0) { tab = 0 }
                NavTab(Icons.Default.Insights, if (isPersian) "آنالیز" else "Analytics", tab == 1) { tab = 1 }
            }
        }
    }

    if (showSignOut) {
        ConfirmDialog(
            title = if (isPersian) "خروج از پنل مدیریت؟" else "Sign out of the admin panel?",
            message = if (isPersian) "آیا مطمئنی؟ برای ورود دوباره باید رمز عبور را وارد کنی." else "Are you sure? You'll need your password to sign in again.",
            confirm = if (isPersian) "خروج" else "Sign out",
            destructive = false,
            isPersian = isPersian,
            onConfirm = { showSignOut = false; onSignOut() },
            onDismiss = { showSignOut = false }
        )
    }

    if (accessLost) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (isPersian) "دسترسی ادمین فعال نیست" else "Admin access is no longer active", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isPersian) "نقش این حساب تغییر کرده یا حساب غیرفعال شده است. برای ادامه از حساب خارج شو و دوباره وارد شو."
                    else "This account's role changed or it was disabled. Sign out and back in to continue."
                )
            },
            confirmButton = {
                TextButton(onClick = { accessLost = false; onSignOut() }) { Text(if (isPersian) "خروج" else "Sign out", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
private fun Spacer12() { Box(Modifier.width(12.dp)) }

@Composable
private fun NavTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
        label = "tabBg"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        label = "tabFg"
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = content)
    }
}
