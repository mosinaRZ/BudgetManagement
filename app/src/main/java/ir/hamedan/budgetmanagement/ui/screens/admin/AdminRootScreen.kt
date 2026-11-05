package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarDefaults
import ir.hamedan.budgetmanagement.ui.components.AppSnackbarHost
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale

/** Header row: 16dp + 44dp button + 16dp, below the status bar. */
private val HeaderHeight = 76.dp

/** Floating capsule navigation: 16dp gap + 58dp capsule + breathing room, above the navigation bar. */
private val NavSpace = 90.dp

private const val TAB_USERS = 0
private const val TAB_ANALYTICS = 1

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
    var tab by rememberSaveable { mutableIntStateOf(TAB_USERS) }
    var pendingUserFilter by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showSignOut by remember { mutableStateOf(false) }
    var accessLost by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // The screens scroll underneath the floating header and navigation capsule and pad themselves
    // by these amounts. System-bar heights are included so tall status bars never cover content.
    val contentPadding = PaddingValues(
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HeaderHeight,
        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + NavSpace
    )

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            AuroraBackground()

            when (tab) {
                TAB_USERS -> AdminUsersScreen(
                    snackbarHostState = snackbarHostState,
                    selfUserId = selfUserId,
                    onForbidden = { accessLost = true },
                    onShowUserAnalytics = { id, label ->
                        pendingUserFilter = id to label
                        tab = TAB_ANALYTICS
                    },
                    contentPadding = contentPadding
                )
                else -> AdminAnalyticsScreen(
                    pendingUserFilter = pendingUserFilter,
                    onPendingConsumed = { pendingUserFilter = null },
                    onForbidden = { accessLost = true },
                    contentPadding = contentPadding
                )
            }

            AdminHeader(
                title = if (isPersian) "پنل مدیریت سیدنا" else "Cidna Admin",
                subtitle = if (tab == TAB_USERS) {
                    if (isPersian) "مدیریت کاربران" else "User management"
                } else {
                    if (isPersian) "آنالیز و امنیت" else "Analytics & security"
                },
                signOutDescription = if (isPersian) "خروج" else "Sign out",
                onSignOut = { showSignOut = true },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            AppSnackbarHost(
                hostState = snackbarHostState,
                isPersian = isPersian,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = AppSnackbarDefaults.AboveBottomBar)
            )

            AdminNavBar(
                selectedTab = tab,
                isPersian = isPersian,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
            )
        }
    }

    if (showSignOut) {
        AdminConfirmDialog(
            title = if (isPersian) "خروج از پنل مدیریت؟" else "Sign out of the admin panel?",
            message = if (isPersian) "آیا مطمئنی؟ برای ورود دوباره باید رمز عبور را وارد کنی."
            else "Are you sure? You'll need your password to sign in again.",
            confirm = if (isPersian) "خروج" else "Sign out",
            destructive = false,
            isPersian = isPersian,
            icon = Icons.AutoMirrored.Filled.Logout,
            onConfirm = { showSignOut = false; onSignOut() },
            onDismiss = { showSignOut = false }
        )
    }

    if (accessLost) {
        AdminDialog(
            onDismiss = {},
            dismissible = false,
            title = if (isPersian) "دسترسی ادمین فعال نیست" else "Admin access is no longer active",
            icon = Icons.Default.Lock,
            iconTint = MaterialTheme.colorScheme.error,
            actions = {
                DialogConfirmButton(
                    text = if (isPersian) "خروج" else "Sign out",
                    onClick = { accessLost = false; onSignOut() },
                    icon = Icons.AutoMirrored.Filled.Logout
                )
            }
        ) {
            Text(
                text = if (isPersian) "نقش این حساب تغییر کرده یا حساب غیرفعال شده است. برای ادامه از حساب خارج شو و دوباره وارد شو."
                else "This account's role changed or it was disabled. Sign out and back in to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Same title treatment as the main app's sub-screens (round 44dp controls, bold title, muted subtitle). */
@Composable
private fun AdminHeader(
    title: String,
    subtitle: String,
    signOutDescription: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdminIconBadge(Icons.Default.AdminPanelSettings, MaterialTheme.colorScheme.primary, size = 44.dp, iconSize = 24.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(text = subtitle, style = MaterialTheme.typography.adminCaption, color = adminMuted())
        }
        IconButton(
            onClick = onSignOut,
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = signOutDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Floating capsule navigation, same language as the main app's bottom bar. */
@Composable
private fun AdminNavBar(
    selectedTab: Int,
    isPersian: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = adminCardBorder(alpha = 0.12f),
        shadowElevation = 8.dp
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NavTab(Icons.Default.People, if (isPersian) "کاربران" else "Users", selectedTab == TAB_USERS) { onSelect(TAB_USERS) }
            NavTab(Icons.Default.Insights, if (isPersian) "آنالیز" else "Analytics", selectedTab == TAB_ANALYTICS) { onSelect(TAB_ANALYTICS) }
        }
    }
}

@Composable
private fun NavTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        label = "tabBg"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else adminMuted(),
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
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = content)
    }
}