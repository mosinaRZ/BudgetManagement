package ir.hamedan.budgetmanagement.ui.tour

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import ir.hamedan.budgetmanagement.data.preferences.TourPreferences

/**
 * وضعیت زنده‌ی تور معرفی برنامه.
 *
 * این شیء عمداً بیرون از Composition نگه داشته می‌شود: تور از «خانه» به «افزودن» (که مسیر جدایی در
 * ناوبری اصلی است) می‌رود و remember هیچ صفحه‌ای از این جابه‌جایی جان سالم به در نمی‌برد.
 *
 * هیچ‌چیز از پیشرفت تور روی دیسک ذخیره نمی‌شود؛ فقط «تمام‌شدن» ثبت می‌شود ([TourPreferences]).
 * پس اگر برنامه وسط تور بسته شود، دفعه‌ی بعد تور دوباره از اول شروع می‌شود.
 */
object TourController {

    var isRunning by mutableStateOf(false)
        private set

    var stepIndex by mutableIntStateOf(0)
        private set

    /** کادر هر هدفِ علامت‌گذاری‌شده بر حسب مختصات پنجره؛ هدفی که از صفحه رفته باشد حذف می‌شود. */
    val targets: SnapshotStateMap<TourTarget, Rect> = mutableStateMapOf()

    private var ownerUserId: String? = null

    val steps: List<TourStep> get() = TourSteps.all

    val currentStep: TourStep
        get() = steps[stepIndex.coerceIn(0, steps.lastIndex)]

    val currentScreen: TourScreen
        get() = currentStep.screen

    /** تا وقتی تور در جریان است صفحه‌ها به‌جای داده‌ی واقعی، داده‌ی نمونه نشان می‌دهند (بدون دست‌زدن به دیتابیس). */
    val sampleActive: Boolean get() = isRunning

    val isLastStep: Boolean get() = stepIndex >= steps.lastIndex

    fun start(userId: String?) {
        if (isRunning) return
        ownerUserId = userId
        targets.clear()
        stepIndex = 0
        isRunning = true
    }

    fun next(context: Context) {
        if (!isRunning) return
        if (isLastStep) finish(context) else stepIndex += 1
    }

    fun back() {
        if (isRunning && stepIndex > 0) stepIndex -= 1
    }

    /** پایان یا «رد کردن»: تور برای این کاربر دیگر هرگز نمایش داده نمی‌شود. */
    fun finish(context: Context) {
        if (!isRunning) return
        TourPreferences.markCompleted(context, ownerUserId)
        reset()
    }

    /**
     * قطع تور بدون ثبت پایان (مثلاً وقتی قفل برنامه یا خروج از حساب کاربر را از صفحه‌ی اصلی بیرون می‌برد).
     * تور هنوز «تمام‌نشده» است و دفعه‌ی بعد از ابتدا شروع می‌شود.
     */
    fun abort() {
        if (isRunning) reset()
    }

    private fun reset() {
        isRunning = false
        stepIndex = 0
        ownerUserId = null
        targets.clear()
    }
}

/**
 * این کامپوننت را برای اسپات‌لایت‌شدن در تور علامت‌گذاری می‌کند.
 * وقتی تور در جریان نیست هیچ کاری نمی‌کند و هزینه‌ای ندارد.
 */
fun Modifier.tourTarget(target: TourTarget): Modifier = composed {
    DisposableEffect(target) {
        onDispose { TourController.targets.remove(target) }
    }
    onGloballyPositioned { coordinates ->
        if (TourController.isRunning && coordinates.isAttached) {
            val bounds = coordinates.boundsInWindow()
            if (TourController.targets[target] != bounds) TourController.targets[target] = bounds
        }
    }
}

/** کوتاه‌نویسی برای صفحه‌ها: آیا همین مرحله‌ی تور الان فعال است؟ */
@Composable
fun isTourStep(id: String): Boolean = TourController.isRunning && TourController.currentStep.id == id
