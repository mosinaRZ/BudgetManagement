package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.network.AdminApi
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.showError
import ir.hamedan.budgetmanagement.ui.components.showInfo
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthErrorBanner
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class RoleChange(val user: AdminApi.AdminUser, val first: String, val last: String, val role: String)

@Composable
fun AdminUsersScreen(
    snackbarHostState: SnackbarHostState,
    selfUserId: String,
    onForbidden: () -> Unit,
    onShowUserAnalytics: (userId: String, label: String) -> Unit,
    contentPadding: PaddingValues
) {
    val vm: AdminUsersViewModel = appViewModel()
    val state by vm.state.collectAsState()
    val detail by vm.detail.collectAsState()
    val isPersian = isPersianLocale()
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AdminApi.AdminUser?>(null) }
    var confirmDelete by remember { mutableStateOf<AdminApi.AdminUser?>(null) }
    var confirmRevoke by remember { mutableStateOf<AdminApi.AdminUser?>(null) }
    var confirmRole by remember { mutableStateOf<RoleChange?>(null) }

    val undoLabel = if (isPersian) "بازگردانی" else "Undo"
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            // One coroutine per message: a new snackbar replaces the old one instead of waiting behind it.
            scope.launch {
                when (event) {
                    is UsersEvent.Info -> snackbarHostState.showInfo(event.message)
                    is UsersEvent.Failure -> snackbarHostState.showError(event.message)
                    is UsersEvent.Undoable -> {
                        val result = snackbarHostState.showInfo(event.message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) vm.undo(event.undoToken) else vm.forgetUndo(event.undoToken)
                    }
                }
            }
        }
    }
    // Presence changes while the admin watches, so the list quietly refreshes.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            vm.reload(silent = true)
        }
    }
    LaunchedEffect(state.forbidden) { if (state.forbidden) onForbidden() }

    val q = state.query
    val activeFilters = listOf(q.role, q.status, q.sort.takeIf { it != "newest" }.orEmpty()).count { it.isNotEmpty() }
    val hasAnyQuery = q.search.isNotEmpty() || q.role.isNotEmpty() || q.status.isNotEmpty() || q.presence.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchField(
                    value = searchText,
                    isPersian = isPersian,
                    onValueChange = { searchText = it.take(48); vm.onSearchChanged(searchText) },
                    modifier = Modifier.weight(1f)
                )
                AdminFilterButton(activeCount = activeFilters, isPersian = isPersian, size = 56.dp, onClick = { showFilters = true })
            }
        }

        item {
            PresenceBar(
                selected = q.presence,
                total = state.total,
                isPersian = isPersian,
                onSelect = vm::setPresence
            )
        }

        state.error?.let { message -> item { AuthErrorBanner(message = message) } }

        if (state.isLoading && state.users.isEmpty()) {
            item { AdminLoading() }
        }

        if (!state.isLoading && state.users.isEmpty() && state.error == null) {
            item {
                AdminEmptyState(
                    icon = Icons.Default.PeopleAlt,
                    text = if (isPersian) "کاربری با این فیلترها پیدا نشد" else "No users match these filters",
                    action = {
                        if (hasAnyQuery) {
                            TextButton(onClick = { searchText = ""; vm.clearFilters() }) {
                                Text(if (isPersian) "پاک کردن فیلترها" else "Clear filters")
                            }
                        }
                    }
                )
            }
        }

        items(state.users, key = { it.id }) { user ->
            UserCard(
                user = user,
                isSelf = user.id == selfUserId,
                busy = user.id in state.busyIds,
                isPersian = isPersian,
                onClick = { vm.openDetail(user.id) }
            )
            if (user.id == state.users.last().id) {
                LaunchedEffect(state.users.size) { vm.loadMore() }
            }
        }

        if (state.isLoadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            role = q.role, status = q.status, sort = q.sort, isPersian = isPersian,
            onRole = vm::setRole, onStatus = vm::setStatus, onSort = vm::setSort,
            onReset = { searchText = ""; vm.clearFilters() },
            onDismiss = { showFilters = false }
        )
    }

    detail.userId?.let { id ->
        UserDetailSheet(
            state = detail,
            fallback = state.users.firstOrNull { it.id == id },
            isSelf = id == selfUserId,
            busy = id in state.busyIds,
            isPersian = isPersian,
            onDismiss = vm::closeDetail,
            onEdit = { editing = it },
            onUnlock = vm::unlockUser,
            onRevoke = { confirmRevoke = it },
            onDelete = { confirmDelete = it },
            onRestore = vm::restoreUser,
            onShowAnalytics = { user ->
                vm.closeDetail()
                onShowUserAnalytics(user.id, user.displayLabel())
            }
        )
    }

    editing?.let { user ->
        EditUserDialog(
            user = user,
            isSelf = user.id == selfUserId,
            isPersian = isPersian,
            onDismiss = { editing = null },
            onSave = { first, last, role ->
                editing = null
                if (role != user.role) confirmRole = RoleChange(user, first, last, role)
                else vm.saveEdit(user, first, last, role)
            }
        )
    }

    confirmRole?.let { change ->
        val user = change.user
        val name = user.displayLabel()
        AdminConfirmDialog(
            title = if (isPersian) "تغییر نقش؟" else "Change role?",
            message = if (isPersian)
                "نقش «$name» از ${roleLabel(user.role, true)} به ${roleLabel(change.role, true)} تغییر می‌کند و همه نشست‌های او باطل می‌شود."
            else
                "\"$name\" changes from ${roleLabel(user.role, false)} to ${roleLabel(change.role, false)} and all their sessions are signed out.",
            confirm = if (isPersian) "بله، تغییر بده" else "Yes, change",
            destructive = change.role == "admin" || user.role == "admin",
            isPersian = isPersian,
            icon = Icons.Default.AdminPanelSettings,
            onConfirm = { confirmRole = null; vm.saveEdit(user, change.first, change.last, change.role) },
            onDismiss = { confirmRole = null }
        )
    }

    confirmDelete?.let { user ->
        val name = user.displayLabel()
        AdminConfirmDialog(
            title = if (isPersian) "حذف کاربر؟" else "Delete user?",
            message = if (isPersian)
                "آیا مطمئنی؟ «$name» فوراً از حساب خارج می‌شود. حذف نرم است و تا بعد از آن می‌توانی از طریق «بازگردانی» برش گردانی."
            else
                "Are you sure? \"$name\" is signed out immediately. This is a soft delete — you can bring them back with Undo.",
            confirm = if (isPersian) "بله، حذف کن" else "Yes, delete",
            destructive = true,
            isPersian = isPersian,
            icon = Icons.Default.DeleteOutline,
            onConfirm = { confirmDelete = null; vm.deleteUser(user) },
            onDismiss = { confirmDelete = null }
        )
    }

    confirmRevoke?.let { user ->
        val name = user.displayLabel()
        AdminConfirmDialog(
            title = if (isPersian) "ابطال همه نشست‌ها؟" else "Revoke all sessions?",
            message = if (isPersian)
                "آیا مطمئنی؟ «$name» از همه دستگاه‌ها خارج می‌شود و باید دوباره وارد شود."
            else
                "Are you sure? \"$name\" is signed out of every device and must sign in again.",
            confirm = if (isPersian) "بله، باطل کن" else "Yes, revoke",
            destructive = true,
            isPersian = isPersian,
            icon = Icons.AutoMirrored.Filled.Logout,
            onConfirm = { confirmRevoke = null; vm.revokeSessions(user) },
            onDismiss = { confirmRevoke = null }
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    isPersian: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        shape = AdminShapes.Field,
        placeholder = {
            Text(
                if (isPersian) "جستجوی نام، شماره یا شناسه" else "Search name, phone or ID",
                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = if (isPersian) "پاک کردن" else "Clear")
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            unfocusedContainerColor = adminCardColor(),
            focusedContainerColor = adminCardColor()
        )
    )
}

/** All / online / offline chips with the result count at the end. */
@Composable
private fun PresenceBar(selected: String, total: Long, isPersian: Boolean, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "" to (if (isPersian) "همه" else "All"),
            "online" to (if (isPersian) "آنلاین" else "Online"),
            "offline" to (if (isPersian) "آفلاین" else "Offline")
        ).forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
        Spacer(Modifier.weight(1f))
        Text(
            "${fmtNumber(total, isPersian)} ${if (isPersian) "کاربر" else "users"}",
            style = MaterialTheme.typography.adminCaption,
            color = adminMuted()
        )
    }
}

@Composable
private fun UserCard(user: AdminApi.AdminUser, isSelf: Boolean, busy: Boolean, isPersian: Boolean, onClick: () -> Unit) {
    AdminCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().alpha(if (user.isDeleted) 0.6f else 1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            UserAvatar(user)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    user.fullName.ifBlank { if (isPersian) "بدون نام" else "No name" },
                    style = MaterialTheme.typography.adminTitleSmall, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    user.phoneNumber.ifBlank { "—" },
                    style = MaterialTheme.typography.adminCaption.copy(textDirection = TextDirection.Ltr),
                    color = adminMuted(),
                    maxLines = 1
                )
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusPill(roleLabel(user.role, isPersian), roleColor(user.role))
            UserStatusPill(user, isPersian)
            if (isSelf) StatusPill(if (isPersian) "شما" else "You", MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            MiniStat(Icons.Default.Devices, fmtNumber(user.deviceCount, isPersian))
            MiniStat(Icons.Default.Sync, fmtBytes(user.syncBytes, isPersian))
            Text(
                if (user.online) (if (isPersian) "اکنون فعال" else "Active now") else relativeTime(user.lastSeenAt, isPersian),
                style = MaterialTheme.typography.labelSmall,
                color = adminMuted()
            )
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.adminCaption, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
    }
}

@Composable
private fun FiltersSheet(
    role: String, status: String, sort: String, isPersian: Boolean,
    onRole: (String) -> Unit, onStatus: (String) -> Unit, onSort: (String) -> Unit,
    onReset: () -> Unit, onDismiss: () -> Unit
) {
    val all = if (isPersian) "همه" else "All"
    AdminBottomSheet(onDismiss = onDismiss, title = if (isPersian) "فیلتر و مرتب‌سازی" else "Filter & sort") {
        AdminChipGroup(
            title = if (isPersian) "نقش" else "Role",
            options = listOf("" to all) + AllRoles.map { it to roleLabel(it, isPersian) },
            selected = role,
            onSelect = onRole
        )
        AdminChipGroup(
            title = if (isPersian) "وضعیت حساب" else "Account status",
            options = listOf(
                "" to all,
                "active" to (if (isPersian) "فعال" else "Active"),
                "locked" to (if (isPersian) "قفل‌شده" else "Locked"),
                "deleted" to (if (isPersian) "حذف‌شده" else "Deleted")
            ),
            selected = status,
            onSelect = onStatus
        )
        AdminChipGroup(
            title = if (isPersian) "مرتب‌سازی" else "Sort by",
            options = listOf(
                "newest" to (if (isPersian) "جدیدترین" else "Newest"),
                "oldest" to (if (isPersian) "قدیمی‌ترین" else "Oldest"),
                "name" to (if (isPersian) "نام" else "Name")
            ),
            selected = sort,
            onSelect = onSort
        )
        AdminSheetActions(
            secondary = if (isPersian) "بازنشانی" else "Reset",
            onSecondary = onReset,
            primary = if (isPersian) "تمام" else "Done",
            onPrimary = onDismiss
        )
    }
}