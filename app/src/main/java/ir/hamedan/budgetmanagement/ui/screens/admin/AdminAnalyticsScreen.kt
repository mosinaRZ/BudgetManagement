package ir.hamedan.budgetmanagement.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

private val AnalyticsRanges = listOf(7, 30, 90)

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
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsRanges.forEach { d ->
                    FilterChip(
                        selected = f.days == d,
                        onClick = { vm.setDays(d) },
                        label = { Text(if (isPersian) "${fmtNumber(d, true)} روز" else "${d}d") }
                    )
                }
                Box(Modifier.weight(1f))
                AdminFilterButton(activeCount = f.activeCount, isPersian = isPersian, onClick = { showFilters = true })
            }
        }

        if (f.activeCount > 0) {
            item { ActiveFilters(f, isPersian, vm) }
        }

        state.error?.let { message -> item { AuthErrorBanner(message = message) } }

        if (data == null) {
            if (state.isLoading) item { AdminLoading(vertical = 64.dp) }
        } else {
            item { RiskCard(data, isPersian) }
            item { KpiGrid(data, isPersian) }
            if (data.eventsTruncated) {
                item {
                    Text(
                        if (isPersian) "تعداد رویدادها بسیار زیاد است؛ نمودارها فقط جدیدترین ${fmtNumber(data.eventsConsidered, true)} رویداد را نشان می‌دهند."
                        else "There are a lot of events; charts only cover the newest ${fmtNumber(data.eventsConsidered, false)}.",
                        style = MaterialTheme.typography.adminCaption,
                        color = MaterialTheme.colorScheme.tertiary
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
                        style = MaterialTheme.typography.adminCaption,
                        color = adminMuted()
                    )
                }
            }
        }
        items(state.events, key = { it.id }) { event ->
            AdminEventRow(event, isPersian, onUserClick = { e -> vm.filterUser(e.userId, e.userLabel.ifBlank { e.userId.takeLast(6) }) })
        }
        if (state.canLoadMoreEvents) {
            item {
                OutlinedButton(
                    onClick = vm::loadMoreEvents,
                    enabled = !state.eventsLoadingMore,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = AdminShapes.Button
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

// ------------------------------------------------------------------ cards

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    AdminCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminIconBadge(icon, tint)
            Column {
                Text(title, style = MaterialTheme.typography.adminTitleSmall, fontWeight = FontWeight.Bold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = adminMuted())
            }
        }
        content()
    }
}

@Composable
private fun RiskCard(data: AdminApi.Analytics, isPersian: Boolean) {
    SectionCard(
        title = if (isPersian) "وضعیت امنیت" else "Security posture",
        subtitle = if (isPersian) "بر پایه رویدادهای ${fmtNumber(data.days, true)} روز اخیر" else "Based on the last ${data.days} days",
        icon = Icons.Default.GppMaybe,
        tint = riskColor(data.riskLevel)
    ) {
        RiskGauge(
            score = data.riskScore,
            level = data.riskLevel,
            levelLabel = riskLabel(data.riskLevel, isPersian),
            caption = if (isPersian) "از ${fmtNumber(100, true)}" else "out of 100",
            isPersian = isPersian
        )
        Text(
            if (isPersian) "امتیاز از روی رویدادهای خطرناک، قفل شدن حساب‌ها و نسبت ورودهای ناموفق حساب می‌شود؛ یک شاخص راهنماست نه حکم قطعی."
            else "The score combines dangerous events, account lockouts and the share of failed sign-ins. It is a guide, not a verdict.",
            style = MaterialTheme.typography.labelSmall,
            color = adminMuted()
        )
    }
}

private data class KpiSpec(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color,
    val spark: List<Float>? = null,
    val caption: String? = null
)

@Composable
private fun KpiGrid(data: AdminApi.Analytics, isPersian: Boolean) {
    val k = data.kpis
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val success = adminSuccess()
    val info = adminInfo()
    val violet = adminViolet()
    fun n(value: Long) = fmtNumber(value, isPersian)
    fun word(fa: String, en: String) = if (isPersian) fa else en

    val specs = listOf(
        KpiSpec(
            word("کل کاربران", "Total users"), fmtCompact(k.totalUsers, isPersian), Icons.Default.Group, primary,
            caption = "${n(k.activeUsers)} ${word("فعال", "active")}"
        ),
        KpiSpec(
            word("آنلاین اکنون", "Online now"), n(k.onlineNow), Icons.Default.Sensors, success,
            caption = word("${fmtNumber(data.onlineWindowSec / 60, true)} دقیقه اخیر", "last ${data.onlineWindowSec / 60} min")
        ),
        KpiSpec(
            word("ورود موفق", "Sign-ins"), fmtCompact(k.loginsSuccess, isPersian), Icons.AutoMirrored.Filled.Login, primary,
            spark = data.loginsByDay.map { it.success.toFloat() }
        ),
        KpiSpec(
            word("ورود ناموفق", "Failed sign-ins"), fmtCompact(k.loginsFailed, isPersian), Icons.Default.Block, tertiary,
            spark = data.loginsByDay.map { it.failed.toFloat() }
        ),
        KpiSpec(
            word("رویداد خطرناک", "Dangerous events"), n(k.dangerEvents), Icons.Default.Shield, error,
            caption = word("${n(k.lockedUsers)} حساب قفل", "${n(k.lockedUsers)} locked now")
        ),
        KpiSpec(
            word("کاربر جدید", "New users"), n(k.newUsers), Icons.Default.PersonAdd, info,
            spark = data.signupsByDay.map { it.count.toFloat() }
        ),
        KpiSpec(
            word("رکورد سینک‌شده", "Synced records"), fmtCompact(k.syncRecords, isPersian), Icons.Default.CloudSync, violet,
            spark = data.syncByDay.map { it.records.toFloat() }
        ),
        KpiSpec(
            word("حساب قفل‌شده", "Locked accounts"), n(k.lockedUsers), Icons.Default.Lock, tertiary,
            caption = word("${n(k.lockedEvents)} قفل در بازه", "${n(k.lockedEvents)} lockouts in range")
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        specs.chunked(2).forEach { pair ->
            // Equal height inside a row, whether a tile shows a sparkline or a caption.
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { Kpi(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun Kpi(spec: KpiSpec, modifier: Modifier = Modifier) {
    AdminCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminIconBadge(spec.icon, spec.tint, size = 28.dp, iconSize = 16.dp)
            Text(spec.title, style = MaterialTheme.typography.adminCaption, color = adminMuted(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(spec.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (spec.spark != null && spec.spark.size >= 2) {
            Sparkline(spec.spark, spec.tint, Modifier.fillMaxWidth().height(28.dp))
        } else if (spec.caption != null) {
            Text(spec.caption, style = MaterialTheme.typography.labelSmall, color = adminMuted(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LoginsCard(data: AdminApi.Analytics, isPersian: Boolean) {
    val total = data.kpis.loginsSuccess + data.kpis.loginsFailed
    val rate = if (total > 0) (data.kpis.loginsSuccess * 100 / total).toInt() else 100
    SectionCard(
        title = if (isPersian) "ورود کاربران" else "User sign-ins",
        subtitle = if (isPersian) "نرخ موفقیت ${fmtPercent(rate, true)}" else "Success rate ${fmtPercent(rate, false)}",
        icon = Icons.AutoMirrored.Filled.Login
    ) {
        LineAreaChart(
            labels = data.loginsByDay.map { dayLabel(it.day, isPersian) },
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
        tint = adminInfo()
    ) {
        HeatmapGrid(data.heatmap, isPersian, unitLabel = if (isPersian) "ورود" else "sign-ins")
    }
}

@Composable
private fun SecurityCard(data: AdminApi.Analytics, isPersian: Boolean) {
    SectionCard(
        title = if (isPersian) "اکشن‌ها و رویدادهای خطرناک" else "Dangerous actions & events",
        subtitle = if (isPersian) "تفکیک روزانه بر اساس شدت" else "Daily breakdown by severity",
        icon = Icons.Default.Shield,
        tint = MaterialTheme.colorScheme.error
    ) {
        StackedBarChart(
            labels = data.severityByDay.map { dayLabel(it.day, isPersian) },
            stacks = data.severityByDay.map { listOf(it.info.toFloat(), it.warning.toFloat(), it.danger.toFloat()) },
            names = listOf("info", "warning", "danger").map { severityLabel(it, isPersian) },
            colors = listOf("info", "warning", "danger").map { severityColor(it) },
            isPersian = isPersian
        )
        if (data.eventTypes.isNotEmpty()) {
            AdminSectionLabel(if (isPersian) "پرتکرارترین رویدادها" else "Most frequent events")
            HorizontalBars(
                items = data.eventTypes.take(6).map { eventLabel(it.key, isPersian) to it.count },
                colors = chartPalette(),
                isPersian = isPersian
            )
        }
    }
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
                        s.ip,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusPill(
                        if (isPersian) "${fmtNumber(s.accounts, true)} حساب" else "${s.accounts} accounts",
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPill(
                        if (isPersian) "${fmtNumber(s.failed, true)} ناموفق" else "${s.failed} failed",
                        MaterialTheme.colorScheme.error
                    )
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
        tint = adminViolet()
    ) {
        AdminSectionLabel(if (isPersian) "نقش‌ها" else "Roles")
        DonutWithLegend(
            slices = data.roles.mapIndexed { i, b -> DonutSlice(roleLabel(b.key, isPersian), b.count.toFloat(), palette[i % palette.size]) },
            centerTitle = fmtCompact(data.kpis.activeUsers, isPersian),
            centerSubtitle = if (isPersian) "کاربر فعال" else "active",
            isPersian = isPersian
        )
        if (data.platforms.isNotEmpty()) {
            AdminSectionLabel(if (isPersian) "پلتفرم دستگاه‌ها" else "Device platforms")
            DonutWithLegend(
                slices = data.platforms.mapIndexed { i, b -> DonutSlice(platformLabel(b.key, isPersian), b.count.toFloat(), palette[(i + 2) % palette.size]) },
                centerTitle = fmtCompact(data.platforms.sumOf { it.count }, isPersian),
                centerSubtitle = if (isPersian) "دستگاه" else "devices",
                isPersian = isPersian
            )
        }
        if (data.appVersions.isNotEmpty()) {
            AdminSectionLabel(if (isPersian) "نسخه‌های برنامه" else "App versions")
            HorizontalBars(
                items = data.appVersions.map { (if (it.key == "unknown") platformLabel("unknown", isPersian) else "v${it.key}") to it.count },
                colors = palette,
                isPersian = isPersian
            )
        }
        AdminSectionLabel(if (isPersian) "ثبت‌نام‌های روزانه" else "Daily sign-ups")
        LineAreaChart(
            labels = data.signupsByDay.map { dayLabel(it.day, isPersian) },
            series = listOf(LineSeries(if (isPersian) "ثبت‌نام" else "Sign-ups", data.signupsByDay.map { it.count.toFloat() }, adminInfo())),
            isPersian = isPersian,
            height = 150.dp
        )
    }
}

@Composable
private fun SyncCard(data: AdminApi.Analytics, isPersian: Boolean, onUserClick: (String, String) -> Unit) {
    SectionCard(
        title = if (isPersian) "داده‌های سینک‌شده" else "Synced data",
        subtitle = "${fmtNumber(data.kpis.syncRecords, isPersian)} ${if (isPersian) "رکورد" else "records"} · " +
                "${fmtBytes(data.kpis.syncBytes, isPersian)} ${if (isPersian) "(رمزنگاری‌شده)" else "(encrypted)"}",
        icon = Icons.Default.CloudSync,
        tint = adminViolet()
    ) {
        LineAreaChart(
            labels = data.syncByDay.map { dayLabel(it.day, isPersian) },
            series = listOf(LineSeries(if (isPersian) "رکورد" else "Records", data.syncByDay.map { it.records.toFloat() }, adminViolet())),
            isPersian = isPersian,
            height = 160.dp
        )
        if (data.syncByEntity.isNotEmpty()) {
            AdminSectionLabel(if (isPersian) "نوع داده" else "By data type")
            HorizontalBars(
                items = data.syncByEntity.take(7).map { entityLabel(it.key, isPersian) to it.count },
                colors = chartPalette(),
                isPersian = isPersian
            )
        }
        if (data.topSyncUsers.isNotEmpty()) {
            AdminSectionLabel(if (isPersian) "بیشترین حجم سینک" else "Heaviest syncers")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.topSyncUsers.forEach { u ->
                    val label = u.label.ifBlank { u.userId.takeLast(6) }
                    Row(
                        Modifier.fillMaxWidth().clickable { onUserClick(u.userId, label) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${fmtCompact(u.records, isPersian)} · ${fmtBytes(u.bytes, isPersian)}",
                            style = MaterialTheme.typography.adminCaption,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = AdminAlpha.Strong)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ filters

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilters(f: AnalyticsFilters, isPersian: Boolean, vm: AdminAnalyticsViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (f.userId.isNotEmpty()) RemovableChip(f.userLabel.ifBlank { f.userId.takeLast(6) }, vm::clearUser)
        if (f.severity.isNotEmpty()) RemovableChip(severityLabel(f.severity, isPersian)) { vm.setSeverity("") }
        if (f.platform.isNotEmpty()) RemovableChip(platformLabel(f.platform, isPersian)) { vm.setPlatform("") }
        if (f.types.isNotEmpty()) {
            RemovableChip(
                if (isPersian) "${fmtNumber(f.types.size, true)} نوع رویداد" else "${f.types.size} event types"
            ) { vm.setTypes(emptySet()) }
        }
    }
}

@Composable
private fun AnalyticsFiltersSheet(f: AnalyticsFilters, isPersian: Boolean, vm: AdminAnalyticsViewModel, onDismiss: () -> Unit) {
    AdminBottomSheet(onDismiss = onDismiss, title = if (isPersian) "فیلتر اطلاعات" else "Filter data") {
        AdminChipGroup(
            title = if (isPersian) "شدت" else "Severity",
            options = listOf("", "info", "warning", "danger").map { it to severityLabel(it, isPersian) },
            selected = f.severity,
            onSelect = vm::setSeverity
        )
        AdminChipGroup(
            title = if (isPersian) "پلتفرم دستگاه" else "Device platform",
            options = listOf("" to (if (isPersian) "همه" else "All"), "android" to "Android", "ios" to "iOS"),
            selected = f.platform,
            onSelect = vm::setPlatform
        )
        AdminMultiChipGroup(
            title = if (isPersian) "نوع رویداد" else "Event type",
            options = AllEventTypes.map { it to eventLabel(it, isPersian) },
            selected = f.types,
            onToggle = { type -> vm.setTypes(if (type in f.types) f.types - type else f.types + type) }
        )
        AdminSheetActions(
            secondary = if (isPersian) "بازنشانی" else "Reset",
            onSecondary = vm::clearFilters,
            primary = if (isPersian) "تمام" else "Done",
            onPrimary = onDismiss
        )
    }
}