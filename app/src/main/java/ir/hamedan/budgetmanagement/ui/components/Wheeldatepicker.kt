package ir.hamedan.budgetmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import ir.hamedan.budgetmanagement.utils.AppHapticType
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.rememberAppHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Date picker with three snapping wheels (day / month / year), shown in a dialog.
 *
 * The calendar follows the app language: Jalali with Persian month names and digits for Persian,
 * Gregorian for English. The selectable range is [minDate]..[maxDate] inclusive.
 * [onConfirm] receives local start-of-day epoch millis, the convention used for stored dates.
 */
@Composable
fun WheelDatePickerDialog(
    isPersian: Boolean,
    title: String,
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate = LocalDate.now(),
    maxDate: LocalDate = LocalDate.now().plusYears(20)
) {
    val jalali = isPersian
    val minF = remember(minDate, jalali) { wdFieldsOf(minDate, jalali) }
    val maxF = remember(maxDate, jalali) { wdFieldsOf(maxDate, jalali) }

    fun clamp(v: WdYmd): WdYmd {
        val y = v.y.coerceIn(minF.y, maxF.y)
        val mLo = if (y == minF.y) minF.m else 1
        val mHi = if (y == maxF.y) maxF.m else 12
        val m = v.m.coerceIn(mLo, mHi)
        val dLo = if (y == minF.y && m == minF.m) minF.d else 1
        val dHi = minOf(wdDaysIn(y, m, jalali), if (y == maxF.y && m == maxF.m) maxF.d else 31)
        return WdYmd(y, m, v.d.coerceIn(dLo, dHi))
    }

    var picked by remember {
        val initial = wdLocalDate(initialMillis).let {
            when {
                it.isBefore(minDate) -> minDate
                it.isAfter(maxDate) -> maxDate
                else -> it
            }
        }
        mutableStateOf(clamp(wdFieldsOf(initial, jalali)))
    }

    fun num(n: Int): String = if (isPersian) TransactionShareFormatter.toPersianDigits(n.toString()) else n.toString()

    val monthLo = if (picked.y == minF.y) minF.m else 1
    val monthHi = if (picked.y == maxF.y) maxF.m else 12
    val dayLo = if (picked.y == minF.y && picked.m == minF.m) minF.d else 1
    val dayHi = minOf(
        wdDaysIn(picked.y, picked.m, jalali),
        if (picked.y == maxF.y && picked.m == maxF.m) maxF.d else 31
    )
    val monthNames = if (jalali) DateUtils.PERSIAN_MONTH_NAMES else DateUtils.ENGLISH_MONTH_NAMES

    val dayWheel = WdWheelSpec(
        label = if (isPersian) "روز" else "Day",
        items = (dayLo..dayHi).map(::num),
        selectedIndex = picked.d - dayLo,
        weight = 1f,
        onSelect = { picked = clamp(picked.copy(d = dayLo + it)) }
    )
    val monthWheel = WdWheelSpec(
        label = if (isPersian) "ماه" else "Month",
        items = (monthLo..monthHi).map { monthNames[it - 1] },
        selectedIndex = picked.m - monthLo,
        weight = 1.5f,
        onSelect = { picked = clamp(picked.copy(m = monthLo + it)) }
    )
    val yearWheel = WdWheelSpec(
        label = if (isPersian) "سال" else "Year",
        items = (minF.y..maxF.y).map(::num),
        selectedIndex = picked.y - minF.y,
        weight = 1.2f,
        onSelect = { picked = clamp(picked.copy(y = minF.y + it)) }
    )
    // Day first in Persian (۲۵ مهر ۱۳۷۵), month first in English (Oct 25, 1996).
    val wheels = if (isPersian) listOf(dayWheel, monthWheel, yearWheel) else listOf(monthWheel, dayWheel, yearWheel)

    val pickedDate = remember(picked, jalali) { wdDateOf(picked, jalali) }
    val pickedMillis = pickedDate?.let(::wdMillisOf)
    val itemHeight = 44.dp
    val direction = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = pickedMillis?.let { DateUtils.formatTimestamp(it, isPersian) }.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    wheels.forEach { wheel ->
                        Text(
                            text = wheel.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(wheel.weight)
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(itemHeight * 5)) {
                    val band = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(itemHeight)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), band)
                    )
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        wheels.forEach { wheel ->
                            WdWheel(
                                items = wheel.items,
                                selectedIndex = wheel.selectedIndex,
                                onSelected = wheel.onSelect,
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(wheel.weight)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }
                    Button(
                        onClick = { pickedMillis?.let(onConfirm) },
                        enabled = pickedMillis != null,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isPersian) "تأیید" else "Confirm", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/* ───────────────────────── internals ───────────────────────── */

private data class WdYmd(val y: Int, val m: Int, val d: Int)

private class WdWheelSpec(
    val label: String,
    val items: List<String>,
    val selectedIndex: Int,
    val weight: Float,
    val onSelect: (Int) -> Unit
)

private fun wdFieldsOf(date: LocalDate, jalali: Boolean): WdYmd =
    if (jalali) DateUtils.toJalali(date).let { WdYmd(it.first, it.second, it.third) }
    else WdYmd(date.year, date.monthValue, date.dayOfMonth)

private fun wdDaysIn(y: Int, m: Int, jalali: Boolean): Int =
    if (jalali) DateUtils.getDaysInJalaliMonth(y, m) else YearMonth.of(y, m).lengthOfMonth()

/**
 * Jalali -> Gregorian by inverting the app's own [DateUtils.toJalali], so the chosen date always
 * displays back as exactly the same Jalali date everywhere else in the app.
 */
private fun wdDateOf(f: WdYmd, jalali: Boolean): LocalDate? {
    if (!jalali) return runCatching { LocalDate.of(f.y, f.m, f.d) }.getOrNull()
    var day = f.d
    while (day >= 1) {
        val target = f.y * 10000 + f.m * 100 + day
        val monthOffset = if (f.m <= 7) (f.m - 1) * 31L else 186L + (f.m - 7) * 30L
        var candidate = LocalDate.of(f.y + 621, 3, 21).plusDays(monthOffset + day - 1)
        repeat(8) {
            val (jy, jm, jd) = DateUtils.toJalali(candidate)
            val key = jy * 10000 + jm * 100 + jd
            if (key == target) return candidate
            candidate = candidate.plusDays(if (key < target) 1L else -1L)
        }
        day-- // this day does not exist in that month (e.g. 30 Esfand); try the previous one
    }
    return null
}

private fun wdMillisOf(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun wdLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/**
 * One snapping wheel. The centred row is the selection; neighbours fade with distance.
 * Selection is reported once the wheel settles, and the wheel follows [selectedIndex]
 * when the parent changes it (for example when a shorter month clamps the day).
 */
@Composable
private fun WdWheel(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    itemHeight: Dp,
    modifier: Modifier = Modifier
) {
    val lastIndex = (items.size - 1).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val currentOnSelected by rememberUpdatedState(onSelected)

    // The list has two rows of padding above and below, so scroll position 0 centres item 0.
    val centerIndex by remember(items.size) {
        derivedStateOf {
            (listState.firstVisibleItemIndex + (listState.firstVisibleItemScrollOffset / itemPx).roundToInt())
                .coerceIn(0, lastIndex)
        }
    }

    LaunchedEffect(listState, items.size) {
        snapshotFlow { if (listState.isScrollInProgress) -1 else centerIndex }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .collect { currentOnSelected(it) }
    }
    LaunchedEffect(listState, items.size) {
        snapshotFlow { centerIndex }.drop(1).collect { haptics.perform(AppHapticType.Tick) }
    }
    LaunchedEffect(selectedIndex, items.size) {
        if (!listState.isScrollInProgress && selectedIndex in items.indices && selectedIndex != centerIndex) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = fling,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = itemHeight * 2)
    ) {
        itemsIndexed(items) { index, label ->
            val distance = abs(index - centerIndex)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { scope.launch { listState.animateScrollToItem(index) } },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = when (distance) {
                            0 -> 1f
                            1 -> 0.55f
                            2 -> 0.28f
                            else -> 0.12f
                        }
                    ),
                    maxLines = 1
                )
            }
        }
    }
}