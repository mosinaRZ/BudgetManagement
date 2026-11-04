package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.data.network.AdminApi
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthErrorBanner
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import kotlinx.coroutines.delay

private val PanelShape = RoundedCornerShape(24.dp)

private val AllEventTypes = listOf(
    "login_success", "login_failed", "account_locked", "registered", "password_changed", "password_reset",
    "account_deleted", "account_restored", "device_removed", "admin_user_edited", "admin_role_changed",
    "admin_user_deleted", "admin_user_restored", "admin_user_unlocked", "admin_sessions_revoked", "admin_access_denied"
)

@Composable
fun AdminAnalyticsScreen(
    pendingUserFilter: Pair<String, String>?,
    onPendingConsumed: () -> Unit,
    onForbidden: () -> Unit,
    contentPadding: PaddingValues
) {
    val vm: AdminAnalyticsViewModel = appViewModel()
    val state by vm.state.collectAsState()
    val isPersian = isPersianLocale()
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(pendingUserFilter) {
        if (pendingUserFilter != null) {
            vm.filterUser(pendingUserFilter.first, pendingUserFilter.second)
            onPendingConsumed()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.reload(silent = true)
        }
    }
    LaunchedEffect(state.forbidden) { if (state.forbidden) onForbidden() }

    val f = state.filters
    val data = state.data

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { d ->
                    FilterChip(
                        selected = f.days == d,
                        onClick = { vm.setDays(d) },
                        label = { Text(if (isPersian) "${fmtNumber(d.toLong(), true)} روز" else "${d}d") }
                    )
                }
                Box(Modifier.weight(1f))
                Box {
                    IconButton(
                        onClick = { showFilters = true },
                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) { Icon(Icons.Default.FilterList, if (isPersian) "فیلتر" else "Filters", tint = MaterialTheme.colorScheme.primary) }
                    if (f.activeCount > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).size(18.dp).background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(numberFormat(isPersian).format(f.activeCount), style = MaterialTheme.typography.labelSmall, color = Color.White) }
                    }
                }
            }
        }

        if (f.activeCount > 0) {
            item { ActiveFilters(f, isPersian, vm) }
        }

        state.error?.let { message -> item { AuthErrorBanner(message = message) } }

        if (data == null) {
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            item { RiskCard(data, isPersian) }
            item { KpiGrid(data, isPersian) }
            if (data.eventsTruncated) {
                item {
                    Text(
                        if (isPersian) "تعداد رویدادها بسیار زیاد است؛ نمودارها فقط جدیدترین ${fmtNumber(data.eventsConsidered.toLong(), true)} رویداد را نشان می‌دهند."
                        else "There are a lot of events; charts only cover the newest ${fmtNumber(data.eventsConsidered.toLong(), false)}.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            item { LoginsCard(data, isPersian) }
            item { RhythmCard(data, isPersian) }
            item { SecurityCard(data, isPersian) }
            if (data.suspicious.isNotEmpty()) item { SuspiciousCard(data, isPersian) }
            item { UsersCard(data, isPersian) }
            item { SyncCard(data, isPersian, onUserClick = { id, label -> vm.filterUser(id, label) }) }
        }

        item {
            SectionCard(
                title = if (isPersian) "رویدادهای امنیتی" else "Security feed",
                subtitle = if (isPersian) "${fmtNumber(state.eventsTotal, true)} رویداد در این بازه" else "${fmtNumber(state.eventsTotal, false)} events in this range",
                icon = Icons.Default.Shield,
                tint = MaterialTheme.colorScheme.error
            ) {
                if (state.events.isEmpty()) {
                    Text(
                        if (isPersian) "رویدادی با این فیلترها پیدا نشد." else "No events match these filters.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        items(state.events, key = { it.id }) { event ->
            EventRow(event, isPersian, onUserClick = { e -> vm.filterUser(e.userId, e.userLabel.ifBlank { e.userId.takeLast(6) }) })
        }
        if (state.canLoadMoreEvents) {
            item {
                OutlinedButton(
                    onClick = vm::loadMoreEvents,
                    enabled = !state.eventsLoadingMore,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (state.eventsLoadingMore) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (isPersian) "نمایش بیشتر" else "Show more")
                }
            }
        }
    }

    if (showFilters) {
        AnalyticsFiltersSheet(f, isPersian, vm, onDismiss = { showFilters = false })
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Surface(
        shape = PanelShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).background(tint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
            content()
        }
    }
}

@Composable
private fun RiskCard(data: AdminApi.Analytics, isPersian: Boolean) {
    val levelLabel = when (data.riskLevel) {
        "high" -> if (isPersian) "ریسک بالا" else "High risk"
        "medium" -> if (isPersian) "ریسک متوسط" else "Medium risk"
        else -> if (isPersian) "ریسک پایین" else "Low risk"
    }
    SectionCard(
        title = if (isPersian) "وضعیت امنیت" else "Security posture",
        subtitle = if (isPersian) "بر پایه رویدادهای ${fmtNumber(data.days.toLong(), true)} روز اخیر" else "Based on the last ${data.days} days",
        icon = Icons.Default.GppMaybe,
        tint = when (data.riskLevel) {
            "high" -> MaterialTheme.colorScheme.error
            "medium" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
    ) {
        RiskGauge(
            score = data.riskScore, level = data.riskLevel, levelLabel = levelLabel,
            caption = if (isPersian) "از ۱۰۰" else "out of 100", isPersian = isPersian
        )
        Text(
            if (isPersian) "امتیاز از روی رویدادهای خطرناک، قفل شدن حساب‌ها و نسبت ورودهای ناموفق حساب می‌شود؛ یک شاخص راهنماست نه حکم قطعی."
            else "The score combines dangerous events, account lockouts and the share of failed sign-ins. It is a guide, not a verdict.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun KpiGrid(data: AdminApi.Analytics, isPersian: Boolean) {
    val k = data.kpis
    val success = data.loginsByDay.map { it.success.toFloat() }
    val failed = data.loginsByDay.map { it.failed.toFloat() }
    val signups = data.signupsByDay.map { it.count.toFloat() }
    val syncRecs = data.syncByDay.map { it.records.toFloat() }
    val ok = Color(0xFF2FB872)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(Modifier.weight(1f), if (isPersian) "کل کاربران" else "Total users", fmtCompact(k.totalUsers, isPersian), Icons.Default.Group, MaterialTheme.colorScheme.primary, null,
                if (isPersian) "${fmtNumber(k.activeUsers, true)} فعال" else "${fmtNumber(k.activeUsers, false)} active")
            Kpi(Modifier.weight(1f), if (isPersian) "آنلاین اکنون" else "Online now", fmtNumber(k.onlineNow, isPersian), Icons.Default.Sensors, ok, null,
                if (isPersian) "${fmtNumber(data.onlineWindowSec / 60, true)} دقیقه اخیر" else "last ${data.onlineWindowSec / 60} min")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(Modifier.weight(1f), if (isPersian) "ورود موفق" else "Sign-ins", fmtCompact(k.loginsSuccess, isPersian), Icons.AutoMirrored.Filled.Login, MaterialTheme.colorScheme.primary, success,
                if (isPersian) "${fmtNumber(k.uniqueLoginUsers, true)} کاربر یکتا" else "${fmtNumber(k.uniqueLoginUsers, false)} unique users")
            Kpi(Modifier.weight(1f), if (isPersian) "ورود ناموفق" else "Failed sign-ins", fmtCompact(k.loginsFailed, isPersian), Icons.Default.Block, MaterialTheme.colorScheme.tertiary, failed, null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(Modifier.weight(1f), if (isPersian) "رویداد خطرناک" else "Dangerous events", fmtNumber(k.dangerEvents, isPersian), Icons.Default.Shield, MaterialTheme.colorScheme.error, null,
                if (isPersian) "${fmtNumber(k.lockedUsers, true)} حساب قفل" else "${fmtNumber(k.lockedUsers, false)} locked now")
            Kpi(Modifier.weight(1f), if (isPersian) "کاربر جدید" else "New users", fmtNumber(k.newUsers, isPersian), Icons.Default.PersonAdd, Color(0xFF4F9DB8), signups, null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Kpi(Modifier.weight(1f), if (isPersian) "رکورد سینک‌شده" else "Synced records", fmtCompact(k.syncRecords, isPersian), Icons.Default.CloudSync, Color(0xFF9B7EDB), syncRecs,
                fmtBytes(k.syncBytes, isPersian))
            Kpi(Modifier.weight(1f), if (isPersian) "حساب قفل‌شده" else "Locked accounts", fmtNumber(k.lockedUsers, isPersian), Icons.Default.Lock, MaterialTheme.colorScheme.tertiary, null,
                if (isPersian) "${fmtNumber(k.lockedEvents, true)} قفل در بازه" else "${fmtNumber(k.lockedEvents, false)} lockouts in range")
        }
    }
}

@Composable
private fun Kpi(modifier: Modifier, title: String, value: String, icon: ImageVector, tint: Color, spark: List<Float>?, caption: String?) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(28.dp).background(tint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (spark != null && spark.size >= 2) {
                Sparkline(spark, tint, Modifier.fillMaxWidth().height(28.dp))
            } else if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LoginsCard(data: AdminApi.Analytics, isPersian: Boolean) {
    val labels = data.loginsByDay.map { dayLabel(it.day, isPersian) }
    val total = data.kpis.loginsSuccess + data.kpis.loginsFailed
    val rate = if (total > 0) data.kpis.loginsSuccess * 100 / total else 100
    SectionCard(
        title = if (isPersian) "ورود کاربران" else "User sign-ins",
        subtitle = if (isPersian) "نرخ موفقیت ${numberFormat(true).format(rate)}٪" else "Success rate $rate%",
        icon = Icons.AutoMirrored.Filled.Login
    ) {
        LineAreaChart(
            labels = labels,
            series = listOf(
                LineSeries(if (isPersian) "موفق" else "Successful", data.loginsByDay.map { it.success.toFloat() }, MaterialTheme.colorScheme.primary),
                LineSeries(if (isPersian) "ناموفق" else "Failed", data.loginsByDay.map { it.failed.toFloat() }, MaterialTheme.colorScheme.error)
            ),
            isPersian = isPersian
        )
    }
}

@Composable
private fun RhythmCard(data: AdminApi.Analytics, isPersian: Boolean) {
    SectionCard(
        title = if (isPersian) "ساعت‌های شلوغ ورود" else "Peak sign-in hours",
        subtitle = if (isPersian) "روز هفته × ساعت (به وقت دستگاه شما)" else "Weekday × hour (your device time)",
        icon = Icons.Default.Sensors,
        tint = Color(0xFF4F9DB8)
    ) {
        HeatmapGrid(data.heatmap, isPersian, unitLabel = if (isPersian) "ورود" else "sign-ins")
    }
}

@Composable
private fun SecurityCard(data: AdminApi.Analytics, isPersian: Boolean) {
    val info = MaterialTheme.colorScheme.primary
    val warn = MaterialTheme.colorScheme.tertiary
    val danger = MaterialTheme.colorScheme.error
    SectionCard(
        title = if (isPersian) "اکشن‌ها و رویدادهای خطرناک" else "Dangerous actions & events",
        subtitle = if (isPersian) "تفکیک روزانه بر اساس شدت" else "Daily breakdown by severity",
        icon = Icons.Default.Shield,
        tint = danger
    ) {
        StackedBarChart(
            labels = data.severityByDay.map { dayLabel(it.day, isPersian) },
            stacks = data.severityByDay.map { listOf(it.info.toFloat(), it.warning.toFloat(), it.danger.toFloat()) },
            names = listOf(severityLabel("info", isPersian), severityLabel("warning", isPersian), severityLabel("danger", isPersian)),
            colors = listOf(info, warn, danger),
            isPersian = isPersian
        )
        if (data.eventTypes.isNotEmpty()) {
            SectionLabel(if (isPersian) "پرتکرارترین رویدادها" else "Most frequent events")
            HorizontalBars(
                items = data.eventTypes.take(6).map { eventLabel(it.key, isPersian) to it.count },
                colors = chartPalette(), isPersian = isPersian
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
}

@Composable
private fun SuspiciousCard(data: AdminApi.Analytics, isPersian: Boolean) {
    SectionCard(
        title = if (isPersian) "منابع مشکوک" else "Suspicious sources",
        subtitle = if (isPersian) "بیشترین ورودهای ناموفق به تفکیک آدرس" else "Most failed sign-ins by address",
        icon = Icons.Default.Public,
        tint = MaterialTheme.colorScheme.tertiary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.suspicious.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        s.ip, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), fontWeight = FontWeight.SemiBold
                    )
                    StatusPill(if (isPersian) "${fmtNumber(s.accounts, true)} حساب" else "${s.accounts} accounts", MaterialTheme.colorScheme.outline)
                    StatusPill(if (isPersian) "${fmtNumber(s.failed, true)} ناموفق" else "${s.failed} failed", MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun UsersCard(data: AdminApi.Analytics, isPersian: Boolean) {
    val palette = chartPalette()
    SectionCard(
        title = if (isPersian) "کاربران و دستگاه‌ها" else "Users & devices",
        subtitle = if (isPersian) "توزیع نقش، پلتفرم و نسخه برنامه" else "Role, platform and app-version mix",
        icon = Icons.Default.Group,
        tint = Color(0xFF9B7EDB)
    ) {
        SectionLabel(if (isPersian) "نقش‌ها" else "Roles")
        DonutWithLegend(
            slices = data.roles.mapIndexed { i, b -> DonutSlice(roleLabel(b.key, isPersian), b.count.toFloat(), palette[i % palette.size]) },
            centerTitle = fmtCompact(data.kpis.activeUsers, isPersian),
            centerSubtitle = if (isPersian) "کاربر فعال" else "active",
            isPersian = isPersian
        )
        if (data.platforms.isNotEmpty()) {
            SectionLabel(if (isPersian) "پلتفرم دستگاه‌ها" else "Device platforms")
            DonutWithLegend(
                slices = data.platforms.mapIndexed { i, b -> DonutSlice(platformLabel(b.key, isPersian), b.count.toFloat(), palette[(i + 2) % palette.size]) },
                centerTitle = fmtCompact(data.platforms.sumOf { it.count }, isPersian),
                centerSubtitle = if (isPersian) "دستگاه" else "devices",
                isPersian = isPersian
            )
        }
        if (data.appVersions.isNotEmpty()) {
            SectionLabel(if (isPersian) "نسخه‌های برنامه" else "App versions")
            HorizontalBars(items = data.appVersions.map { (if (it.key == "unknown") platformLabel("unknown", isPersian) else "v${it.key}") to it.count }, colors = palette, isPersian = isPersian)
        }
        SectionLabel(if (isPersian) "ثبت‌نام‌های روزانه" else "Daily sign-ups")
        LineAreaChart(
            labels = data.signupsByDay.map { dayLabel(it.day, isPersian) },
            series = listOf(LineSeries(if (isPersian) "ثبت‌نام" else "Sign-ups", data.signupsByDay.map { it.count.toFloat() }, Color(0xFF4F9DB8))),
            isPersian = isPersian, height = 150.dp
        )
    }
}

@Composable
private fun SyncCard(data: AdminApi.Analytics, isPersian: Boolean, onUserClick: (String, String) -> Unit) {
    val palette = chartPalette()
    SectionCard(
        title = if (isPersian) "داده‌های سینک‌شده" else "Synced data",
        subtitle = if (isPersian) "${fmtNumber(data.kpis.syncRecords, true)} رکورد · ${fmtBytes(data.kpis.syncBytes, true)} (رمزنگاری‌شده)" else "${fmtNumber(data.kpis.syncRecords, false)} records · ${fmtBytes(data.kpis.syncBytes, false)} (encrypted)",
        icon = Icons.Default.CloudSync,
        tint = Color(0xFF9B7EDB)
    ) {
        LineAreaChart(
            labels = data.syncByDay.map { dayLabel(it.day, isPersian) },
            series = listOf(LineSeries(if (isPersian) "رکورد" else "Records", data.syncByDay.map { it.records.toFloat() }, Color(0xFF9B7EDB))),
            isPersian = isPersian, height = 160.dp
        )
        if (data.syncByEntity.isNotEmpty()) {
            SectionLabel(if (isPersian) "نوع داده" else "By data type")
            HorizontalBars(items = data.syncByEntity.take(7).map { entityLabel(it.key, isPersian) to it.count }, colors = palette, isPersian = isPersian)
        }
        if (data.topSyncUsers.isNotEmpty()) {
            SectionLabel(if (isPersian) "بیشترین حجم سینک" else "Heaviest syncers")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.topSyncUsers.forEach { u ->
                    val label = u.label.ifBlank { u.userId.takeLast(6) }
                    Row(
                        Modifier.fillMaxWidth().clickable { onUserClick(u.userId, label) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${fmtCompact(u.records, isPersian)} · ${fmtBytes(u.bytes, isPersian)}",
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilters(f: AnalyticsFilters, isPersian: Boolean, vm: AdminAnalyticsViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (f.userId.isNotEmpty()) {
            FilterChip(selected = true, onClick = vm::clearUser, label = { Text(f.userLabel.ifBlank { f.userId.takeLast(6) }) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) })
        }
        if (f.severity.isNotEmpty()) {
            FilterChip(selected = true, onClick = { vm.setSeverity("") }, label = { Text(severityLabel(f.severity, isPersian)) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) })
        }
        if (f.platform.isNotEmpty()) {
            FilterChip(selected = true, onClick = { vm.setPlatform("") }, label = { Text(platformLabel(f.platform, isPersian)) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) })
        }
        if (f.types.isNotEmpty()) {
            FilterChip(
                selected = true, onClick = { vm.setTypes(emptySet()) },
                label = { Text(if (isPersian) "${fmtNumber(f.types.size.toLong(), true)} نوع رویداد" else "${f.types.size} event types") },
                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AnalyticsFiltersSheet(f: AnalyticsFilters, isPersian: Boolean, vm: AdminAnalyticsViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(if (isPersian) "فیلتر اطلاعات" else "Filter data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SectionLabel(if (isPersian) "شدت" else "Severity")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("", "info", "warning", "danger").forEach { s ->
                    FilterChip(selected = f.severity == s, onClick = { vm.setSeverity(s) }, label = { Text(severityLabel(s, isPersian)) })
                }
            }
            SectionLabel(if (isPersian) "پلتفرم دستگاه" else "Device platform")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("" to (if (isPersian) "همه" else "All"), "android" to "Android", "ios" to "iOS").forEach { (v, label) ->
                    FilterChip(selected = f.platform == v, onClick = { vm.setPlatform(v) }, label = { Text(label) })
                }
            }
            SectionLabel(if (isPersian) "نوع رویداد" else "Event type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AllEventTypes.forEach { type ->
                    val on = type in f.types
                    FilterChip(
                        selected = on,
                        onClick = { vm.setTypes(if (on) f.types - type else f.types + type) },
                        label = { Text(eventLabel(type, isPersian)) }
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.clearFilters() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text(if (isPersian) "بازنشانی" else "Reset")
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text(if (isPersian) "تمام" else "Done")
                }
            }
        }
    }
}