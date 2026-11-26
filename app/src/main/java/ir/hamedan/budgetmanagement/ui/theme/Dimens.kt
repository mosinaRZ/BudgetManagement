package ir.hamedan.budgetmanagement.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * یک «منبع حقیقت» برای تمام فاصله‌ها، گوشه‌ها و ابعاد ثابت برنامه.
 *
 * قبلاً هر صفحه عددهای خودش را داشت (۱۲/۱۶/۲۰/۲۴ برای پدینگ کناری، ۷۵/۱۲۰/۱۴۰/۱۷۰ برای
 * فاصله از بالا، ۱۴/۱۶/۱۸/۲۰/۲۲/۲۴/۲۸ برای گوشه‌ها). از این به بعد همه‌چیز از همین‌جا می‌آید.
 */

/** مقیاس فاصله‌ها (۴-پوینتی با چند گام کمکی). */
object AppSpacing {
    val Xxs = 2.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp
}

/** مقیاس شعاع گوشه‌ها: هر نوع المان فقط یکی از این‌هاست. */
object AppRadius {
    /** چیپ، ایندیکیتور و المان‌های بسیار کوچک. */
    val Xs = 8.dp

    /** دکمه‌های داخل سگمنت، ورودی‌های کوچک. */
    val Sm = 12.dp

    /** آیتم لیست، فیلدها، نوار فیلتر و سوییچر تب. */
    val Md = 16.dp

    /** کارت‌های محتوا، تاپ‌بار شناور و کارت‌های خالی/خلاصه. */
    val Lg = 24.dp

    /** دیالوگ‌ها و شیت‌ها. */
    val Xl = 28.dp
}

object AppShapes {
    val Chip = RoundedCornerShape(AppRadius.Xs)
    val Control = RoundedCornerShape(AppRadius.Sm)
    val Item = RoundedCornerShape(AppRadius.Md)
    val Card = RoundedCornerShape(AppRadius.Lg)
    val TopBar = RoundedCornerShape(AppRadius.Lg)
    val Dialog = RoundedCornerShape(AppRadius.Xl)
    val SheetTop = RoundedCornerShape(topStart = AppRadius.Xl, topEnd = AppRadius.Xl)
}

object AppDimens {
    /** پدینگ افقی استاندارد همه‌ی صفحات (هم‌اندازه با حاشیه‌ی داک پایین). */
    val ScreenHorizontal = AppSpacing.Xxl

    /** فاصله‌ی استاندارد بین کارت‌ها/آیتم‌های لیست. */
    val ItemSpacing = AppSpacing.Md

    /** فاصله‌ی بین بخش‌های بزرگ داخل یک صفحه. */
    val SectionSpacing = AppSpacing.Lg

    /** فاصله‌ی محتوا از لبه‌ی پایین تاپ‌بار/فیلتر. */
    val ContentGap = AppSpacing.Lg

    // ── تاپ‌بار ──
    val TopBarHeight = 56.dp
    val TopBarVerticalPadding = AppSpacing.Sm
    val TopBarSlotGap = AppSpacing.Md

    /** کل ارتفاعی که تاپ‌بار شناور اشغال می‌کند (بدون status bar). */
    val TopBarBlockHeight = TopBarHeight + TopBarVerticalPadding * 2

    /** نوار فیلتر زمانی: ۴۶ ارتفاع + ۴+۴ پدینگ عمودی. */
    val FilterBarBlockHeight = 54.dp

    // ── باتم‌بار (داک) ──
    val DockHeight = 64.dp
    val DockBottomMargin = AppSpacing.Sm

    /** کل ارتفاعی که داک بالای نوار ناوبری سیستم اشغال می‌کند. */
    val DockBlockHeight = DockHeight + DockBottomMargin

    // ── FAB ──
    val FabSize = 56.dp
    val FabMargin = AppSpacing.Xxl

    /** ارتفاع کلی FAB + حاشیه‌اش (بدون nav bar). */
    val FabBlockHeight = FabSize + FabMargin

    // ── اسنک‌بار ──
    val SnackbarHorizontal = AppSpacing.Xxl
    val SnackbarGap = AppSpacing.Md
}

/** ارتفاع status bar فعلیِ دستگاه (ناچ/پانچ‌هول‌ها را هم در نظر می‌گیرد). */
@Composable
fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** ارتفاع نوار ناوبری سیستم (۰ در حالت دکمه‌های کلاسیک غیر edge-to-edge). */
@Composable
fun navigationBarBottom(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * فاصله‌ی بالایِ محتوا تا زیر تاپ‌بارِ شناور.
 *
 * به‌جای عددهای ثابت (۷۵/۱۲۰/۱۴۰/۱۷۰) که در دستگاه‌های با ناچ روی تاپ‌بار می‌افتادند،
 * ارتفاع واقعی status bar + تاپ‌بار (+ نوار فیلتر در صورت وجود) + فاصله‌ی استاندارد محاسبه می‌شود.
 */
@Composable
fun topBarClearance(
    extra: Dp = 0.dp,
    gap: Dp = AppDimens.ContentGap
): Dp = statusBarTop() + AppDimens.TopBarBlockHeight + extra + gap

/** مثل [topBarClearance] ولی برای صفحه‌هایی که زیر تاپ‌بار نوار فیلتر زمانی هم دارند. */
@Composable
fun topBarWithFilterClearance(gap: Dp = AppDimens.ContentGap): Dp =
    topBarClearance(extra = AppDimens.FilterBarBlockHeight, gap = gap)

/**
 * فاصله‌ی پایینِ محتوا تا بالای داک ناوبری (صفحه‌های تب‌دار).
 * شامل nav bar سیستم + داک + حاشیه‌ی استاندارد.
 */
@Composable
fun dockClearance(gap: Dp = AppDimens.ContentGap): Dp =
    navigationBarBottom() + AppDimens.DockBlockHeight + gap

/** فاصله‌ی پایینِ محتوا تا بالای FAB در زیرصفحه‌ها. */
@Composable
fun fabClearance(gap: Dp = AppDimens.ContentGap): Dp =
    navigationBarBottom() + AppDimens.FabBlockHeight + gap

/** فاصله‌ی پایین برای صفحه‌هایی که نه داک دارند نه FAB. */
@Composable
fun plainBottomClearance(gap: Dp = AppDimens.ContentGap): Dp = navigationBarBottom() + gap