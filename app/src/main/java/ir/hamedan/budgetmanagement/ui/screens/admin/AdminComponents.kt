package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.hamedan.budgetmanagement.data.network.AdminApi

/*
 * Building blocks shared by every admin screen. Dialogs and sheets copy the look of the main
 * app's own (28dp surfaceVariant dialogs with an icon badge and 48dp buttons; surface-coloured
 * bottom sheets with 28dp top corners), so confirmations feel the same everywhere.
 */

// ------------------------------------------------------------------ cards and badges

/** The frosted card used for every group of content (users, KPIs, charts). Clickable when [onClick] is given. */
@Composable
internal fun AdminCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = AdminShapes.Card,
    border: BorderStroke = adminCardBorder(),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), verticalArrangement = verticalArrangement, content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = adminCardColor(), contentColor = MaterialTheme.colorScheme.onSurface, border = border, content = body)
    } else {
        Surface(modifier = modifier, shape = shape, color = adminCardColor(), contentColor = MaterialTheme.colorScheme.onSurface, border = border, content = body)
    }
}

@Composable
internal fun AdminIconBadge(icon: ImageVector, tint: Color, size: Dp = 36.dp, iconSize: Dp = 20.dp) {
    Box(
        Modifier.size(size).background(tint.copy(alpha = AdminAlpha.Tint), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
internal fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = AdminAlpha.Tint))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Deleted / locked / online / offline, in that priority. One definition for the list and the detail sheet. */
@Composable
internal fun UserStatusPill(user: AdminApi.AdminUser, isPersian: Boolean) {
    when {
        user.isDeleted -> StatusPill(if (isPersian) "حذف شده" else "Deleted", MaterialTheme.colorScheme.error)
        user.isLocked -> StatusPill(if (isPersian) "قفل" else "Locked", MaterialTheme.colorScheme.tertiary)
        user.online -> StatusPill(if (isPersian) "آنلاین" else "Online", adminSuccess())
        else -> StatusPill(if (isPersian) "آفلاین" else "Offline", MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun UserAvatar(user: AdminApi.AdminUser, size: Dp = 48.dp) {
    val accent = roleColor(user.role)
    val initials = user.fullName
        .split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.12f))))
                .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (initials.isNotEmpty()) {
                Text(initials, style = MaterialTheme.typography.adminTitleSmall, fontWeight = FontWeight.Bold, color = accent)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = accent, modifier = Modifier.size(size / 2))
            }
        }
        if (!user.isDeleted) {
            val dot = if (user.online) adminSuccess() else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
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

// ------------------------------------------------------------------ text, states

@Composable
internal fun AdminSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = adminMuted())
}

@Composable
internal fun AdminLoading(modifier: Modifier = Modifier, vertical: Dp = 56.dp) {
    Box(modifier.fillMaxWidth().padding(vertical = vertical), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun AdminEmptyState(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = AdminAlpha.Muted), modifier = Modifier.size(48.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = adminMuted(), textAlign = TextAlign.Center)
        action?.invoke()
    }
}

// ------------------------------------------------------------------ filters

/** Filter icon with the active-filter count badge. Same control on the users and analytics screens. */
@Composable
internal fun AdminFilterButton(
    activeCount: Int,
    isPersian: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(modifier) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .background(adminCardColor(), AdminShapes.Field)
                .border(adminCardBorder(), AdminShapes.Field)
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = if (isPersian) "فیلتر" else "Filters",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (activeCount > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).size(18.dp).background(MaterialTheme.colorScheme.error, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    fmtNumber(activeCount, isPersian),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

/** Single-choice chips ("" is a valid value, used for "All"). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminChipGroup(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    enabled: Boolean = true
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) AdminSectionLabel(title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(selected = selected == value, enabled = enabled, onClick = { onSelect(value) }, label = { Text(label) })
            }
        }
    }
}

/** Multi-choice chips; [onToggle] receives the tapped value. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminMultiChipGroup(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) AdminSectionLabel(title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(selected = value in selected, onClick = { onToggle(value) }, label = { Text(label) })
            }
        }
    }
}

// ------------------------------------------------------------------ dialogs

/**
 * Dialog in the main app's style: 28dp surfaceVariant card, optional icon badge, centred title,
 * scrollable [content] and a row of [actions] (use [DialogCancelButton] / [DialogConfirmButton]).
 */
@Composable
internal fun AdminDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    dismissible: Boolean = true,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), AdminShapes.Dialog)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), AdminShapes.Dialog)
                    .clip(AdminShapes.Dialog)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (icon != null) AdminIconBadge(icon = icon, tint = iconTint, size = 56.dp, iconSize = 28.dp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

@Composable
internal fun RowScope.DialogCancelButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(48.dp),
        shape = AdminShapes.Button
    ) {
        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun RowScope.DialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    icon: ImageVector? = Icons.Default.Check
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f).height(48.dp),
        shape = AdminShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AdminConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    destructive: Boolean,
    isPersian: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = if (destructive) Icons.Default.Warning else Icons.Default.Info
) {
    AdminDialog(
        onDismiss = onDismiss,
        title = title,
        icon = icon,
        iconTint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        actions = {
            DialogCancelButton(if (isPersian) "انصراف" else "Cancel", onDismiss)
            DialogConfirmButton(confirm, onConfirm, destructive = destructive, icon = null)
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------------------------------------------------------ bottom sheets

/** Bottom sheet in the main app's style: surface colour, 28dp top corners, fully expanded, scrollable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AdminShapes.Sheet,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

/** Reset + Done footer shared by both filter sheets. */
@Composable
internal fun AdminSheetActions(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onSecondary, modifier = Modifier.weight(1f).height(48.dp), shape = AdminShapes.Button) {
            Text(secondary)
        }
        Button(onClick = onPrimary, modifier = Modifier.weight(1f).height(48.dp), shape = AdminShapes.Button) {
            Text(primary, fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------------ security event

/** A single security event, used in the user sheet and the analytics feed. */
@Composable
internal fun AdminEventRow(
    event: AdminApi.AdminEvent,
    isPersian: Boolean,
    compact: Boolean = false,
    onUserClick: ((AdminApi.AdminEvent) -> Unit)? = null
) {
    val color = severityColor(event.severity)
    val clickable = onUserClick != null && event.userId.isNotEmpty()
    AdminCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (clickable) ({ onUserClick?.invoke(event) }) else null,
        shape = AdminShapes.Item,
        border = adminCardBorder(color, if (event.severity == "danger") 0.35f else 0.12f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminIconBadge(eventIcon(event.type), color, size = 38.dp)
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
                    Text(
                        who,
                        style = MaterialTheme.typography.adminCaption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = AdminAlpha.Strong),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val meta = listOf(detailLabel(event.detail, isPersian), event.deviceName, event.ip)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Content),
                        color = adminMuted(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(relativeTime(event.createdAt, isPersian), style = MaterialTheme.typography.labelSmall, color = adminMuted())
                if (!compact) StatusPill(severityLabel(event.severity, isPersian), color)
            }
        }
    }
}