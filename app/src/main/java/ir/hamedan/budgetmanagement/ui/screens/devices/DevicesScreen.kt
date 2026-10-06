package ir.hamedan.budgetmanagement.ui.screens.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.network.DeviceApi
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthErrorBanner
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.DateUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DevicesScreen(onBack: () -> Unit) {
    val vm: DevicesViewModel = appViewModel()
    val devices by vm.devices.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val revokingId by vm.revokingId.collectAsState()
    val error by vm.error.collectAsState()
    val notice by vm.notice.collectAsState()
    val isPersian = isPersianLocale()

    // The primary device is the first one ever registered on the account.
    val thisDeviceIsPrimary = devices.any { it.id == vm.currentDeviceId && it.isPrimary }
    val primaryExists = devices.any { it.isPrimary }

    var pendingRevoke by remember { mutableStateOf<DeviceApi.Device?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeNotice()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 128.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = if (isPersian)
                        "دستگاه‌هایی که با حساب شما وارد شده‌اند. اگر دستگاهی را نمی‌شناسید، آن را حذف کنید و رمز عبور خود را تغییر دهید."
                    else
                        "Devices signed in to your account. If you don't recognise one, remove it and change your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                if (primaryExists) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            thisDeviceIsPrimary && isPersian ->
                                "این دستگاه، دستگاه اصلی حساب شماست و می‌تواند سایر دستگاه‌ها را حذف کند."
                            thisDeviceIsPrimary ->
                                "This is your account's primary device and it can remove all other devices."
                            isPersian ->
                                "دستگاه اصلی، اولین دستگاهی است که وارد حساب شده و فقط از خودش قابل حذف است."
                            else ->
                                "The primary device is the first one that signed in and can only be removed from itself."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                }
            }

            error?.let { message -> item { AuthErrorBanner(message = message) } }

            if (devices.isEmpty() && !isLoading && error == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isPersian) "دستگاهی پیدا نشد" else "No devices found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (devices.isEmpty() && isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            items(devices, key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    isCurrent = device.id == vm.currentDeviceId,
                    isPrimary = device.isPrimary,
                    isRevoking = revokingId == device.id,
                    removeEnabled = revokingId == null,
                    isPersian = isPersian,
                    onRemove = { pendingRevoke = device }
                )
            }
        }

        // Header: same back-button + title treatment as the other sub-screens.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isPersian) "بازگشت" else "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "دستگاه‌های حساب" else "Account Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPersian) "${devices.size.toString()} دستگاه متصل" else "${devices.size} connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            IconButton(
                onClick = vm::refresh,
                enabled = !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), CircleShape)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isPersian) "بازخوانی" else "Refresh",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
        )
    }

    pendingRevoke?.let { device ->
        AlertDialog(
            onDismissRequest = { pendingRevoke = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isPersian) "حذف این دستگاه؟" else "Remove this device?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isPersian)
                        "«${device.displayName(true)}» از حساب شما خارج می‌شود و برای ورود دوباره به رمز عبور نیاز دارد."
                    else
                        "\"${device.displayName(false)}\" will be signed out of your account and will need your password to sign in again.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.revoke(device.id); pendingRevoke = null },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(if (isPersian) "حذف دستگاه" else "Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevoke = null }) { Text(if (isPersian) "انصراف" else "Cancel") }
            }
        )
    }
}

@Composable
private fun DeviceCard(
    device: DeviceApi.Device,
    isCurrent: Boolean,
    isPrimary: Boolean,
    isRevoking: Boolean,
    removeEnabled: Boolean,
    isPersian: Boolean,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val borderColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape)
            .border(BorderStroke(if (isCurrent) 1.5.dp else 1.dp, borderColor), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = platformIcon(device.platform),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = device.displayName(isPersian),
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCurrent || isPrimary) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isCurrent) {
                            DeviceBadge(
                                icon = Icons.Default.VerifiedUser,
                                text = if (isPersian) "این دستگاه" else "This device"
                            )
                        }
                        if (isPrimary) {
                            DeviceBadge(
                                icon = Icons.Default.Star,
                                text = if (isPersian) "دستگاه اصلی" else "Primary device"
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val model = device.model.takeIf { it.isNotBlank() && !it.equals(device.name, ignoreCase = true) }
            model?.let {
                DetailRow(Icons.Default.Smartphone, if (isPersian) "مدل" else "Model", it, ltr = true)
            }
            if (device.platform.isNotBlank() || device.osVersion.isNotBlank()) {
                val os = listOf(platformLabel(device.platform), device.osVersion)
                    .filter { it.isNotBlank() && !(it == platformLabel(device.platform) && device.osVersion.startsWith(it, ignoreCase = true)) }
                    .joinToString(" · ")
                DetailRow(Icons.Default.Memory, if (isPersian) "سیستم‌عامل" else "System", os.ifBlank { platformLabel(device.platform) }, ltr = true)
            }
            if (device.appVersion.isNotBlank()) {
                DetailRow(Icons.Default.Apps, if (isPersian) "نسخه برنامه" else "App version", device.appVersion, ltr = true)
            }
            if (device.lastIp.isNotBlank()) {
                DetailRow(Icons.Default.Public, if (isPersian) "آدرس IP (مخفی‌شده)" else "IP address (masked)", device.lastIp, ltr = true)
            }
            DetailRow(
                icon = Icons.Default.Schedule,
                label = if (isPersian) "آخرین فعالیت" else "Last activity",
                value = if (isCurrent) (if (isPersian) "هم‌اکنون فعال" else "Active now") else relativeTime(device.lastSeenAt, isPersian)
            )
            if (!isCurrent && device.lastSeenAt > 0) {
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = if (isPersian) "تاریخ دقیق" else "Exact time",
                    value = dateTime(device.lastSeenAt, isPersian),
                    ltr = !isPersian
                )
            }
            if (device.createdAt > 0) {
                DetailRow(
                    icon = Icons.Default.Event,
                    label = if (isPersian) "اولین ورود" else "First sign-in",
                    value = dateTime(device.createdAt, isPersian),
                    ltr = !isPersian
                )
            }
        }

        if (!isCurrent && isPrimary) {
            // Only the primary device itself may remove it, so no remove button on other devices.
            Text(
                text = if (isPersian) "این دستگاه فقط از خودش قابل حذف است."
                else "This device can only be removed from itself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else if (!isCurrent) {
            OutlinedButton(
                onClick = onRemove,
                enabled = removeEnabled,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (removeEnabled) 0.5f else 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                if (isRevoking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                } else {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPersian) "حذف دستگاه" else "Remove device", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DeviceBadge(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, ltr: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(
                // Versions, models and addresses are Latin text: keep their glyph order even in RTL.
                textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content
            ),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun DeviceApi.Device.displayName(isPersian: Boolean): String =
    name.ifBlank { model }.ifBlank { if (isPersian) "دستگاه ناشناس" else "Unknown device" }

private fun platformIcon(platform: String): ImageVector = when (platform.lowercase()) {
    "ios" -> Icons.Default.PhoneIphone
    "android" -> Icons.Default.PhoneAndroid
    else -> Icons.Default.Devices
}

private fun platformLabel(platform: String): String = when (platform.lowercase()) {
    "android" -> "Android"
    "ios" -> "iOS"
    "web" -> "Web"
    "desktop" -> "Desktop"
    else -> ""
}

private fun relativeTime(millis: Long, isPersian: Boolean, now: Long = System.currentTimeMillis()): String {
    if (millis <= 0) return if (isPersian) "نامشخص" else "Unknown"
    val minutes = ((now - millis).coerceAtLeast(0)) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> if (isPersian) "چند لحظه پیش" else "Just now"
        minutes < 60 -> if (isPersian) "$minutes دقیقه پیش" else "$minutes min ago"
        hours < 24 -> if (isPersian) "$hours ساعت پیش" else "$hours h ago"
        days < 30 -> if (isPersian) "$days روز پیش" else "$days d ago"
        else -> DateUtils.formatTimestamp(millis, isPersian)
    }
}

private fun dateTime(millis: Long, isPersian: Boolean): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    return "${DateUtils.formatTimestamp(millis, isPersian)} - $time"
}