package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

private val CardShape = RoundedCornerShape(24.dp)

private data class RoleChange(val user: AdminApi.AdminUser, val first: String, val last: String, val role: String)

@Composable
private fun cardColor() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

@Composable
private fun cardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

@Composable
internal fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
internal fun UserAvatar(user: AdminApi.AdminUser, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val initials = (user.fullName.ifBlank { user.phoneNumber.ifBlank { "?" } })
        .split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    val accent = roleColor(user.role)
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.12f))))
                .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
        }
        if (!user.isDeleted) {
            val dot = if (user.online) Color(0xFF3DDC84) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            Box(
                Modifier
                    .size(size / 3.4f)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
                    .background(dot, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it.take(48); vm.onSearchChanged(searchText) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = {
                        Text(
                            if (isPersian) "جستجوی نام، شماره یا شناسه" else "Search name, phone or ID",
                            style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = ""; vm.onSearchChanged("") }) {
                                Icon(Icons.Default.Close, if (isPersian) "پاک کردن" else "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        unfocusedContainerColor = cardColor(),
                        focusedContainerColor = cardColor()
                    )
                )
                Box {
                    IconButton(
                        onClick = { showFilters = true },
                        modifier = Modifier.size(52.dp).background(cardColor(), RoundedCornerShape(16.dp))
                    ) { Icon(Icons.Default.FilterList, if (isPersian) "فیلتر" else "Filters", tint = MaterialTheme.colorScheme.primary) }
                    if (activeFilters > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).size(18.dp).background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(numberFormat(isPersian).format(activeFilters), style = MaterialTheme.typography.labelSmall, color = Color.White) }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "" to (if (isPersian) "همه" else "All"),
                    "online" to (if (isPersian) "آنلاین" else "Online"),
                    "offline" to (if (isPersian) "آفلاین" else "Offline")
                ).forEach { (value, label) ->
                    FilterChip(selected = q.presence == value, onClick = { vm.setPresence(value) }, label = { Text(label) })
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (isPersian) "${fmtNumber(state.total, true)} کاربر" else "${fmtNumber(state.total, false)} users",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        state.error?.let { message -> item { AuthErrorBanner(message = message) } }

        if (state.isLoading && state.users.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (!state.isLoading && state.users.isEmpty() && state.error == null) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.PeopleAlt, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    Text(
                        if (isPersian) "کاربری با این فیلترها پیدا نشد" else "No users match these filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (q.search.isNotEmpty() || q.role.isNotEmpty() || q.status.isNotEmpty() || q.presence.isNotEmpty()) {
                        TextButton(onClick = { searchText = ""; vm.clearFilters() }) { Text(if (isPersian) "پاک کردن فیلترها" else "Clear filters") }
                    }
                }
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
        val listed = state.users.firstOrNull { it.id == id }
        UserDetailSheet(
            state = detail,
            fallback = listed,
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
                onShowUserAnalytics(user.id, user.fullName.ifBlank { user.phoneNumber })
            }
        )
    }

    editing?.let { user ->
        EditUserDialog(
            user = user, isSelf = user.id == selfUserId, isPersian = isPersian,
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
        val role = change.role
        ConfirmDialog(
            title = if (isPersian) "تغییر نقش؟" else "Change role?",
            message = if (isPersian)
                "نقش «${user.fullName.ifBlank { user.phoneNumber }}» از ${roleLabel(user.role, true)} به ${roleLabel(role, true)} تغییر می‌کند و همه نشست‌های او باطل می‌شود."
            else
                "\"${user.fullName.ifBlank { user.phoneNumber }}\" changes from ${roleLabel(user.role, false)} to ${roleLabel(role, false)} and all their sessions are signed out.",
            confirm = if (isPersian) "بله، تغییر بده" else "Yes, change",
            destructive = role == "admin" || user.role == "admin",
            isPersian = isPersian,
            onConfirm = { confirmRole = null; vm.saveEdit(user, change.first, change.last, role) },
            onDismiss = { confirmRole = null }
        )
    }

    confirmDelete?.let { user ->
        ConfirmDialog(
            title = if (isPersian) "حذف کاربر؟" else "Delete user?",
            message = if (isPersian)
                "آیا مطمئنی؟ «${user.fullName.ifBlank { user.phoneNumber }}» فوراً از حساب خارج می‌شود. حذف نرم است و تا بعد از آن می‌توانی از طریق «بازگردانی» برش گردانی."
            else
                "Are you sure? \"${user.fullName.ifBlank { user.phoneNumber }}\" is signed out immediately. This is a soft delete — you can bring them back with Undo.",
            confirm = if (isPersian) "بله، حذف کن" else "Yes, delete",
            destructive = true,
            isPersian = isPersian,
            onConfirm = { confirmDelete = null; vm.deleteUser(user) },
            onDismiss = { confirmDelete = null }
        )
    }

    confirmRevoke?.let { user ->
        ConfirmDialog(
            title = if (isPersian) "ابطال همه نشست‌ها؟" else "Revoke all sessions?",
            message = if (isPersian)
                "آیا مطمئنی؟ «${user.fullName.ifBlank { user.phoneNumber }}» از همه دستگاه‌ها خارج می‌شود و باید دوباره وارد شود."
            else
                "Are you sure? \"${user.fullName.ifBlank { user.phoneNumber }}\" is signed out of every device and must sign in again.",
            confirm = if (isPersian) "بله، باطل کن" else "Yes, revoke",
            destructive = true,
            isPersian = isPersian,
            onConfirm = { confirmRevoke = null; vm.revokeSessions(user) },
            onDismiss = { confirmRevoke = null }
        )
    }
}

@Composable
private fun UserCard(user: AdminApi.AdminUser, isSelf: Boolean, busy: Boolean, isPersian: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = cardColor(),
        border = cardBorder(),
        modifier = Modifier.fillMaxWidth().alpha(if (user.isDeleted) 0.6f else 1f)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                UserAvatar(user)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        user.fullName.ifBlank { if (isPersian) "بدون نام" else "No name" },
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        user.phoneNumber.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(roleLabel(user.role, isPersian), roleColor(user.role))
                when {
                    user.isDeleted -> StatusPill(if (isPersian) "حذف شده" else "Deleted", MaterialTheme.colorScheme.error)
                    user.isLocked -> StatusPill(if (isPersian) "قفل" else "Locked", MaterialTheme.colorScheme.tertiary)
                    user.online -> StatusPill(if (isPersian) "آنلاین" else "Online", Color(0xFF2FB872))
                    else -> StatusPill(if (isPersian) "آفلاین" else "Offline", MaterialTheme.colorScheme.outline)
                }
                if (isSelf) StatusPill(if (isPersian) "شما" else "You", MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MiniStat(Icons.Default.Devices, numberFormat(isPersian).format(user.deviceCount))
                MiniStat(Icons.Default.Sync, fmtBytes(user.syncBytes, isPersian))
                Text(
                    if (user.online) (if (isPersian) "اکنون فعال" else "Active now") else relativeTime(user.lastSeenAt, isPersian),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    role: String, status: String, sort: String, isPersian: Boolean,
    onRole: (String) -> Unit, onStatus: (String) -> Unit, onSort: (String) -> Unit,
    onReset: () -> Unit, onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(if (isPersian) "فیلتر و مرتب‌سازی" else "Filter & sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FilterGroup(
                title = if (isPersian) "نقش" else "Role",
                options = listOf("" to (if (isPersian) "همه" else "All")) + listOf("user", "pro", "premium", "admin").map { it to roleLabel(it, isPersian) },
                selected = role, onSelect = onRole
            )
            FilterGroup(
                title = if (isPersian) "وضعیت حساب" else "Account status",
                options = listOf(
                    "" to (if (isPersian) "همه" else "All"),
                    "active" to (if (isPersian) "فعال" else "Active"),
                    "locked" to (if (isPersian) "قفل‌شده" else "Locked"),
                    "deleted" to (if (isPersian) "حذف‌شده" else "Deleted")
                ),
                selected = status, onSelect = onStatus
            )
            FilterGroup(
                title = if (isPersian) "مرتب‌سازی" else "Sort by",
                options = listOf(
                    "newest" to (if (isPersian) "جدیدترین" else "Newest"),
                    "oldest" to (if (isPersian) "قدیمی‌ترین" else "Oldest"),
                    "name" to (if (isPersian) "نام" else "Name")
                ),
                selected = sort, onSelect = onSort
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text(if (isPersian) "بازنشانی" else "Reset")
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text(if (isPersian) "تمام" else "Done")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDetailSheet(
    state: DetailUiState,
    fallback: AdminApi.AdminUser?,
    isSelf: Boolean,
    busy: Boolean,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onEdit: (AdminApi.AdminUser) -> Unit,
    onUnlock: (AdminApi.AdminUser) -> Unit,
    onRevoke: (AdminApi.AdminUser) -> Unit,
    onDelete: (AdminApi.AdminUser) -> Unit,
    onRestore: (AdminApi.AdminUser) -> Unit,
    onShowAnalytics: (AdminApi.AdminUser) -> Unit
) {
    val user = state.detail?.user ?: fallback
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        if (user == null) {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                if (state.error != null) AuthErrorBanner(message = state.error, modifier = Modifier.padding(24.dp))
                else CircularProgressIndicator()
            }
            return@ModalBottomSheet
        }
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                UserAvatar(user, 60.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        user.fullName.ifBlank { if (isPersian) "بدون نام" else "No name" },
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(roleLabel(user.role, isPersian), roleColor(user.role))
                        when {
                            user.isDeleted -> StatusPill(if (isPersian) "حذف شده" else "Deleted", MaterialTheme.colorScheme.error)
                            user.isLocked -> StatusPill(if (isPersian) "قفل" else "Locked", MaterialTheme.colorScheme.tertiary)
                            user.online -> StatusPill(if (isPersian) "آنلاین" else "Online", Color(0xFF2FB872))
                            else -> StatusPill(if (isPersian) "آفلاین" else "Offline", MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }

            Surface(shape = CardShape, color = cardColor(), border = cardBorder()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(if (isPersian) "شماره" else "Phone", user.phoneNumber.ifBlank { "—" }, ltr = true)
                    InfoRow(if (isPersian) "شناسه" else "ID", user.id, ltr = true)
                    InfoRow(if (isPersian) "جنسیت" else "Gender", genderLabel(user.gender, isPersian))
                    InfoRow(if (isPersian) "تاریخ تولد" else "Birth date", user.birthDate.ifBlank { "—" })
                    InfoRow(if (isPersian) "عضویت" else "Joined", dateTime(user.createdAt, isPersian))
                    InfoRow(if (isPersian) "آخرین فعالیت" else "Last seen", if (user.online) (if (isPersian) "اکنون" else "Now") else relativeTime(user.lastSeenAt, isPersian))
                    InfoRow(if (isPersian) "نسخه برنامه" else "App version", user.appVersion.ifBlank { "—" }, ltr = true)
                    InfoRow(if (isPersian) "داده‌های سینک‌شده" else "Synced data", "${fmtNumber(user.syncRecords, isPersian)} · ${fmtBytes(user.syncBytes, isPersian)}")
                    InfoRow(if (isPersian) "تلاش ناموفق ورود" else "Failed sign-ins", numberFormat(isPersian).format(user.failedLoginAttempts))
                    if (user.isDeleted) InfoRow(if (isPersian) "زمان حذف" else "Deleted at", dateTime(user.deletedAt, isPersian))
                    if (user.isLocked) InfoRow(if (isPersian) "قفل تا" else "Locked until", dateTime(user.lockedUntil, isPersian))
                }
            }

            val devices = state.detail?.devices.orEmpty()
            if (devices.isNotEmpty()) {
                SectionTitle(if (isPersian) "دستگاه‌ها" else "Devices")
                devices.forEach { d ->
                    Surface(shape = RoundedCornerShape(18.dp), color = cardColor(), border = cardBorder()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(platformIcon(d.platform), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.displayName.ifBlank { platformLabel(d.platform, isPersian) }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOf(d.osVersion, d.appVersion.takeIf { it.isNotBlank() }?.let { "v$it" }, d.lastIp).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            StatusPill(
                                if (d.online) (if (isPersian) "آنلاین" else "Online") else relativeTime(d.lastSeenAt, isPersian),
                                if (d.online) Color(0xFF2FB872) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            val events = state.detail?.events.orEmpty()
            if (events.isNotEmpty()) {
                SectionTitle(if (isPersian) "رویدادهای اخیر" else "Recent activity")
                events.take(8).forEach { e -> EventRow(e, isPersian, compact = true) }
            }
            state.error?.let { AuthErrorBanner(message = it) }

            SectionTitle(if (isPersian) "اقدامات" else "Actions")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                if (!user.isDeleted) {
                    ActionButton(Icons.Default.Edit, if (isPersian) "ویرایش" else "Edit", enabled = !busy) { onEdit(user) }
                }
                ActionButton(Icons.Default.Sync, if (isPersian) "نمایش در آنالیز" else "Show in analytics") { onShowAnalytics(user) }
                if (user.isLocked && !user.isDeleted) {
                    ActionButton(Icons.Default.LockOpen, if (isPersian) "باز کردن قفل حساب" else "Unlock account", enabled = !busy) { onUnlock(user) }
                }
                if (!isSelf && !user.isDeleted) {
                    ActionButton(Icons.AutoMirrored.Filled.Logout, if (isPersian) "ابطال همه نشست‌ها" else "Revoke all sessions", enabled = !busy) { onRevoke(user) }
                }
                if (user.isDeleted) {
                    ActionButton(Icons.Default.Restore, if (isPersian) "بازیابی کاربر" else "Restore user", enabled = !busy) { onRestore(user) }
                } else if (!isSelf) {
                    ActionButton(Icons.Default.DeleteOutline, if (isPersian) "حذف کاربر" else "Delete user", enabled = !busy, destructive = true) { onDelete(user) }
                }
                if (isSelf) {
                    Text(
                        if (isPersian) "این حساب خودِ شماست؛ برای جلوگیری از قفل شدن پنل، حذف و تغییر نقش آن در دسترس نیست."
                        else "This is your own account; deleting it or changing its role is disabled so you can't lock yourself out.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

private fun genderLabel(gender: String, isPersian: Boolean): String = when (gender.lowercase()) {
    "male" -> if (isPersian) "مرد" else "Male"
    "female" -> if (isPersian) "زن" else "Female"
    "" -> "—"
    else -> gender
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
}

@Composable
private fun InfoRow(label: String, value: String, ltr: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = if (enabled) 0.45f else 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
    }
}

/** A single security event, used in the user sheet and the analytics feed. */
@Composable
internal fun EventRow(
    event: AdminApi.AdminEvent,
    isPersian: Boolean,
    compact: Boolean = false,
    onUserClick: ((AdminApi.AdminEvent) -> Unit)? = null
) {
    val color = severityColor(event.severity)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardColor(),
        border = BorderStroke(1.dp, color.copy(alpha = if (event.severity == "danger") 0.35f else 0.12f)),
        modifier = Modifier.fillMaxWidth().then(if (onUserClick != null && event.userId.isNotEmpty()) Modifier.clickable { onUserClick(event) } else Modifier)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(eventIcon(event.type), null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(eventLabel(event.type, isPersian), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (!compact && (event.userLabel.isNotBlank() || event.actorLabel.isNotBlank())) {
                    val who = buildString {
                        if (event.userLabel.isNotBlank()) append(event.userLabel)
                        if (event.actorLabel.isNotBlank()) {
                            if (isNotEmpty()) append(if (isPersian) "  ·  توسط " else "  ·  by ")
                            append(event.actorLabel)
                        }
                    }
                    Text(who, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val meta = listOf(
                    detailLabel(event.detail, isPersian),
                    event.deviceName,
                    event.ip
                ).filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Content),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(relativeTime(event.createdAt, isPersian), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                if (!compact) StatusPill(severityLabel(event.severity, isPersian), color)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditUserDialog(
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
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (isPersian) "ویرایش کاربر" else "Edit user", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = first, onValueChange = { first = it.take(80) }, singleLine = true,
                    label = { Text(if (isPersian) "نام" else "First name") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = last, onValueChange = { last = it.take(80) }, singleLine = true,
                    label = { Text(if (isPersian) "نام خانوادگی" else "Last name") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                )
                Text(if (isPersian) "نقش" else "Role", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("user", "pro", "premium", "admin").forEach { r ->
                        FilterChip(selected = role == r, enabled = !isSelf, onClick = { role = r }, label = { Text(roleLabel(r, isPersian)) })
                    }
                }
                if (isSelf) {
                    Text(
                        if (isPersian) "نقش حساب خودتان را نمی‌توانید تغییر دهید." else "You can't change your own role.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(first, last, role) }, enabled = changed) { Text(if (isPersian) "ذخیره" else "Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") } }
    )
}

@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    destructive: Boolean,
    isPersian: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) { Text(confirm, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") } }
    )
}
