package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ir.hamedan.budgetmanagement.data.draft.Draft
import ir.hamedan.budgetmanagement.data.draft.DraftStore
import ir.hamedan.budgetmanagement.data.draft.DraftType
import ir.hamedan.budgetmanagement.ui.screens.draft.DraftSummary
import ir.hamedan.budgetmanagement.ui.screens.draft.describeDraft
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// ═════════════════════════════════════════════════════════════════════════════
// سیستم پیش‌نویس (Draft)
//
// هر جریان ثبت با [rememberDraftSession] به این سیستم وصل می‌شود و سه کار را خودکار می‌گیرد:
//  ۱) ذخیره‌ی خودکار و بی‌صدای فرم هنگام ورودی (با کمی تأخیر)، و ذخیره‌ی فوری وقتی برنامه به
//     پس‌زمینه می‌رود یا صفحه بسته می‌شود؛ پس بستن ناگهانی برنامه هم چیزی را از بین نمی‌برد.
//  ۲) هنگام ورود دوباره: شیتِ «ادامه یا حذف»، یا بازیابی مستقیم اگر از فهرست پیش‌نویس‌ها آمده باشد.
//  ۳) هنگام خروج دستی از صفحه (وقتی چیزی وارد شده): شیتِ «ذخیره به‌عنوان پیش‌نویس / حذف / ادامه».
// بعد از ثبتِ موفق، پیش‌نویس همان لحظه حذف می‌شود ([DraftSession.markCompleted]).
// ═════════════════════════════════════════════════════════════════════════════

private const val AUTOSAVE_DEBOUNCE_MS = 450L

// مقدارهای نگهبانِ جریان وضعیت؛ JSON همیشه با «{» شروع می‌شود و هرگز با این‌ها برخورد نمی‌کند.
private const val STATE_PAUSED = ""
private const val STATE_EMPTY = "-"

/**
 * وضعیت پیش‌نویسِ یک صفحه‌ی ثبت. با [rememberDraftSession] ساخته می‌شود.
 *
 * @param snapshotProvider حالت فعلی فرم؛ `null` یعنی هنوز چیز معناداری وارد نشده
 * @param restoreAction حالتِ پیش‌نویس را در فرم می‌ریزد
 */
@Stable
class DraftSession internal constructor(
    val type: DraftType,
    val enabled: Boolean,
    private val snapshotProvider: () -> JsonObject?,
    private val restoreAction: (JsonObject) -> Unit
) {
    /** پیش‌نویسی که منتظر تصمیم کاربر است (ادامه یا حذف)؛ تا آن زمان ذخیره‌ی خودکار متوقف است. */
    var pendingDraft by mutableStateOf<Draft?>(null)
        internal set

    /** شیتِ «ذخیره به‌عنوان پیش‌نویس؟» باز است. */
    var exitPromptVisible by mutableStateOf(false)
        internal set

    internal var tracking by mutableStateOf(false)
    internal var finished by mutableStateOf(false)
    internal var decided = false
    private var pendingClose: (() -> Unit)? = null

    /** وقتی true است باید دکمه‌ی «بازگشت» سیستم از [requestClose] رد شود. */
    val shouldInterceptBack: Boolean
        get() = enabled && tracking && !finished && snapshotProvider() != null

    internal fun currentSnapshot(): JsonObject? = snapshotProvider()

    /** رشته‌ی قابل‌مقایسه‌ی وضعیت برای [snapshotFlow]؛ فقط وقتی تغییر کند ذخیره‌ی خودکار کار می‌کند. */
    internal fun observedState(): String = when {
        !enabled || !tracking || finished -> STATE_PAUSED
        else -> snapshotProvider()?.toString() ?: STATE_EMPTY
    }

    /** ذخیره‌ی فوری حالت فعلی (هنگام رفتن به پس‌زمینه یا بسته‌شدن صفحه). */
    internal fun flush() {
        if (!enabled || !tracking || finished) return
        snapshotProvider()?.let { DraftStore.save(type, it) }
    }

    internal fun restoreFrom(draft: Draft) {
        try {
            restoreAction(draft.payload)
            tracking = true
        } catch (e: Exception) {
            // پیش‌نویسِ خراب نباید صفحه را بیندازد؛ کنار گذاشته می‌شود و فرم تازه شروع می‌شود.
            DraftStore.discard(type)
            tracking = true
        }
    }

    /** خروج از صفحه؛ اگر چیزی وارد شده باشد اول از کاربر می‌پرسد. */
    fun requestClose(onClose: () -> Unit) {
        if (shouldInterceptBack) {
            pendingClose = onClose
            exitPromptVisible = true
        } else {
            onClose()
        }
    }

    /** «ادامه‌ی پیش‌نویس» در شیت ورود. */
    fun resume() {
        val draft = pendingDraft ?: return
        pendingDraft = null
        restoreFrom(draft)
    }

    /** «حذف و شروع از اول» در شیت ورود. */
    fun startFresh() {
        DraftStore.discard(type)
        pendingDraft = null
        tracking = true
    }

    /** «ذخیره و خروج». */
    fun saveAndClose() {
        flush()
        runPendingClose()
    }

    /** «حذف و خروج». */
    fun discardAndClose() {
        finished = true
        DraftStore.discard(type)
        runPendingClose()
    }

    /** «ادامه‌ی ویرایش» (یا بستن شیت). */
    fun keepEditing() {
        pendingClose = null
        exitPromptVisible = false
    }

    /** ثبت موفق بود: پیش‌نویس دیگر معنا ندارد و ذخیره‌ی خودکار برای همیشه متوقف می‌شود. */
    fun markCompleted() {
        finished = true
        pendingDraft = null
        exitPromptVisible = false
        pendingClose = null
        DraftStore.discard(type)
    }

    private fun runPendingClose() {
        val close = pendingClose
        pendingClose = null
        exitPromptVisible = false
        close?.invoke()
    }
}

/**
 * اتصال یک صفحه‌ی ثبت به سیستم پیش‌نویس.
 *
 * @param enabled false در حالت ویرایش (پیش‌نویس فقط برای ثبتِ جدید است)
 * @param snapshot حالت فعلی فرم به‌صورت JSON، یا `null` اگر فرم خالی است
 * @param restore پیش‌نویس را در فرم می‌ریزد
 */
@Composable
fun rememberDraftSession(
    type: DraftType,
    enabled: Boolean = true,
    snapshot: () -> JsonObject?,
    restore: (JsonObject) -> Unit
): DraftSession {
    val context = LocalContext.current
    remember(context) { DraftStore.init(context) }

    val snapshotState = rememberUpdatedState(snapshot)
    val restoreState = rememberUpdatedState(restore)
    val session = remember(type, enabled) {
        DraftSession(
            type = type,
            enabled = enabled,
            snapshotProvider = { snapshotState.value.invoke() },
            restoreAction = { restoreState.value.invoke(it) }
        )
    }
    val ready by DraftStore.ready.collectAsState()

    // ورود به صفحه: پیش‌نویسِ ذخیره‌شده را بازیابی می‌کنیم یا از کاربر می‌پرسیم.
    LaunchedEffect(session, ready) {
        if (!session.enabled || !ready || session.decided) return@LaunchedEffect
        session.decided = true
        val requested = DraftStore.consumeResumeRequest(type)
        val stored = DraftStore.get(type)
        // فرمی که از چرخش صفحه یا بازگشت از صفحه‌ی دیگر محتوا دارد، «تازه» نیست و پرسشی لازم ندارد.
        val formIsFresh = session.currentSnapshot() == null
        when {
            stored != null && formIsFresh && requested -> session.restoreFrom(stored)
            stored != null && formIsFresh -> session.pendingDraft = stored
            else -> session.tracking = true
        }
    }

    // ذخیره‌ی خودکار با تأخیر؛ خالی‌شدن کامل فرم، پیش‌نویس را هم پاک می‌کند.
    LaunchedEffect(session) {
        if (!session.enabled) return@LaunchedEffect
        snapshotFlow { session.observedState() }.collectLatest { state ->
            when (state) {
                STATE_PAUSED -> Unit
                STATE_EMPTY -> DraftStore.discard(session.type)
                else -> {
                    delay(AUTOSAVE_DEBOUNCE_MS)
                    DraftStore.save(session.type, Json.parseToJsonElement(state).jsonObject)
                }
            }
        }
    }

    // رفتن به پس‌زمینه یا ترک صفحه: بدون تأخیر ذخیره می‌کنیم.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, session) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) session.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            session.flush()
        }
    }

    return session
}

// ═════════════════════════════════════════════════════════════════════════════
// رابط کاربری
// ═════════════════════════════════════════════════════════════════════════════

/** رنگ تأکیدِ متنی که در هر دو تم از نسبت کنتراست ۴٫۵:۱ روی سطح کمتر نمی‌شود. */
@Composable
private fun draftAccentText(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFF1F6B53) else MaterialTheme.colorScheme.primary

private fun relativeTime(then: Long, isPersian: Boolean): String {
    val minutes = ((System.currentTimeMillis() - then).coerceAtLeast(0L)) / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    val nf = NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    return when {
        minutes < 1L -> if (isPersian) "همین الان" else "Just now"
        minutes < 60L -> if (isPersian) "${nf.format(minutes)} دقیقه پیش" else "${nf.format(minutes)} min ago"
        hours < 24L -> if (isPersian) "${nf.format(hours)} ساعت پیش" else "${nf.format(hours)} h ago"
        else -> if (isPersian) "${nf.format(days)} روز پیش" else "${nf.format(days)} d ago"
    }
}

/** حبابِ گرادیانیِ ایموجی، هم‌خانواده‌ی حباب‌های شیت‌های دیگر برنامه. */
@Composable
private fun DraftBadge(emoji: String, size: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.22f), colors.secondary.copy(alpha = 0.34f))))
            .border(1.dp, colors.primary.copy(alpha = 0.30f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, style = if (size >= 60) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun DraftInfoCard(summary: DraftSummary, updatedAt: Long, isPersian: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DraftBadge(emoji = summary.emoji, size = 46)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summary.detail.isNotBlank()) {
                Text(
                    text = summary.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = relativeTime(updatedAt, isPersian),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

/**
 * شیتِ ورود به صفحه‌ی ثبت وقتی پیش‌نویسِ ناتمام وجود دارد: «ادامه» یا «حذف و شروع از اول».
 * بستن شیت (کشیدن/بازگشت) یعنی «بعداً»؛ از صفحه خارج می‌شویم و پیش‌نویس دست‌نخورده می‌ماند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftResumeSheet(
    session: DraftSession,
    isPersian: Boolean,
    currencyUnit: String,
    onExit: () -> Unit
) {
    val draft = session.pendingDraft ?: return
    val colors = MaterialTheme.colorScheme
    val summary = remember(draft, isPersian, currencyUnit) { describeDraft(draft, isPersian, currencyUnit) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onExit,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DraftBadge(emoji = "📝", size = 68)
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isPersian) "یه پیش‌نویس ناتمام داری" else "You have an unfinished draft",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colors.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isPersian) "از همون‌جایی که ماندی ادامه بده و ثبتش کن، یا حذفش کن و از اول شروع کن."
                else "Pick up where you left off and finish it, or delete it and start over.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(18.dp))
            DraftInfoCard(summary = summary, updatedAt = draft.updatedAt, isPersian = isPersian)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { session.resume() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isPersian) "ادامه‌ی پیش‌نویس" else "Continue draft",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { session.startFresh() },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (isPersian) "حذف و شروع از اول" else "Delete and start over",
                    color = colors.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** شیتِ خروج دستی از صفحه‌ی ثبت وقتی چیزی وارد شده است. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftExitSheet(session: DraftSession, isPersian: Boolean) {
    if (!session.exitPromptVisible) return
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { session.keepEditing() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DraftBadge(emoji = "💾", size = 68)
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isPersian) "به‌عنوان پیش‌نویس ذخیره شود؟" else "Save as a draft?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colors.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isPersian) "چیزی که وارد کردی هنوز ثبت نشده. اگه ذخیره‌اش کنی، هر وقت خواستی می‌تونی ادامه بدی."
                else "What you entered isn't recorded yet. Keep it as a draft and continue whenever you like.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { session.saveAndClose() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isPersian) "ذخیره و خروج" else "Save and exit",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { session.discardAndClose() },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(
                        text = if (isPersian) "حذف و خروج" else "Discard and exit",
                        color = colors.error,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(
                    onClick = { session.keepEditing() },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(
                        text = if (isPersian) "ادامه‌ی ویرایش" else "Keep editing",
                        color = draftAccentText(),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * فهرست پیش‌نویس‌های ناتمام برای هاب «افزودن و مدیریت». هر ردیف با یک لمس به همان جریان برمی‌گردد
 * (و پیش‌نویس را بدون پرسش بازیابی می‌کند)، و با دکمه‌ی حذف، بعد از تأیید پاک می‌شود.
 */
@Composable
fun DraftsSection(
    drafts: Map<DraftType, Draft>,
    isPersian: Boolean,
    currencyUnit: String,
    onResume: (DraftType) -> Unit,
    onDelete: (DraftType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (drafts.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val accentText = draftAccentText()
    var confirmDelete by remember { mutableStateOf<DraftType?>(null) }
    val ordered = remember(drafts) { DraftType.values().mapNotNull { drafts[it] } }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPersian) "پیش‌نویس‌های ناتمام" else "Unfinished drafts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = NumberFormat.getIntegerInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
                    .format(ordered.size.toLong()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentText,
                modifier = Modifier
                    .background(accentText.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            )
        }

        ordered.forEach { draft ->
            key(draft.type) {
                val summary = remember(draft, isPersian, currencyUnit) { describeDraft(draft, isPersian, currencyUnit) }
                val shape = RoundedCornerShape(24.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.primary.copy(alpha = 0.10f), colors.surfaceVariant.copy(alpha = 0.42f))
                            ),
                            shape
                        )
                        .border(1.dp, colors.primary.copy(alpha = 0.22f), shape)
                        .clickable { onResume(draft.type) }
                        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DraftBadge(emoji = summary.emoji, size = 46)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (summary.detail.isNotBlank()) {
                            Text(
                                text = summary.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = relativeTime(draft.updatedAt, isPersian),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (isPersian) "ادامه" else "Continue",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentText,
                        modifier = Modifier
                            .background(accentText.copy(alpha = 0.14f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    IconButton(onClick = { confirmDelete = draft.type }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = if (isPersian) "حذف پیش‌نویس" else "Delete draft",
                            tint = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    confirmDelete?.let { type ->
        val target = drafts[type]
        if (target == null) {
            confirmDelete = null
        } else {
            val targetSummary = remember(target, isPersian, currencyUnit) { describeDraft(target, isPersian, currencyUnit) }
            AppConfirmDialog(
                title = if (isPersian) "این پیش‌نویس حذف شود؟" else "Delete this draft?",
                message = if (isPersian) "اطلاعاتی که وارد کرده بودی برای همیشه پاک می‌شود و قابل بازگشت نیست."
                else "Everything you entered will be permanently removed and can't be restored.",
                confirmText = if (isPersian) "حذف" else "Delete",
                cancelText = if (isPersian) "انصراف" else "Cancel",
                destructive = true,
                icon = Icons.Rounded.DeleteOutline,
                confirmIcon = Icons.Rounded.DeleteOutline,
                onConfirm = {
                    onDelete(type)
                    confirmDelete = null
                },
                onDismiss = { confirmDelete = null }
            ) {
                DraftInfoCard(summary = targetSummary, updatedAt = target.updatedAt, isPersian = isPersian)
            }
        }
    }
}