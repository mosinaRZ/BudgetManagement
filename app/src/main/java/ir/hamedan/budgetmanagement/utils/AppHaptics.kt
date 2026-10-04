package ir.hamedan.budgetmanagement.utils

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * انواع ویبره‌ی برنامه. هر نوع «ریتم» ثابتی دارد و در همه‌ی دستگاه‌ها یکسان اجرا می‌شود.
 *
 * - Tick:      ضربه‌ی خیلی سبک (تغییر کوچک، اسکرول روی مقدار)
 * - Click:     ضربه‌ی معمولی (انتخاب، سوییچ)
 * - LongPress: ضربه‌ی محکم‌تر (فشار طولانی، مخفی/نمایان‌کردن مبالغ، تأیید)
 * - Success:   دو ضربه‌ی کوتاه پشت‌سرهم (تکمیل عملیات)
 */
enum class AppHapticType { Tick, Click, LongPress, Success }

/**
 * چرا ویبره‌ی قبلی روی بعضی گوشی‌ها حس نمی‌شد؟
 * `LocalHapticFeedback` فقط `View.performHapticFeedback` را صدا می‌زند و آن به تنظیم سیستمیِ
 * «لرزش لمسی / Touch feedback» وابسته است (در بعضی گوشی‌ها پیش‌فرض خاموش است) و
 * شدت/مدتش را هم سازنده‌ی گوشی تعیین می‌کند (در بعضی دستگاه‌ها تقریباً نامحسوس است).
 *
 * این کلاس اول مستقیماً با Vibrator و یک VibrationEffect مشخص (مدت + شدت) ویبره می‌دهد
 * و فقط اگر دستگاه ویبراتور نداشت/خطا داد، به performHapticFeedback برمی‌گردد.
 *
 * نیازمند: <uses-permission android:name="android.permission.VIBRATE" /> در AndroidManifest.xml
 */
class AppHaptics internal constructor(
    context: Context,
    private val view: View
) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    fun perform(type: AppHapticType) {
        if (!vibrateDirectly(type)) vibrateViaView(type)
    }

    private fun vibrateDirectly(type: AppHapticType): Boolean {
        val v = vibrator ?: return false
        if (!v.hasVibrator()) return false

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = buildEffect(type, v.hasAmplitudeControl())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
                } else {
                    v.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(legacyDurationMs(type))
            }
            true
        }.getOrDefault(false) // مثلاً نبودن مجوز VIBRATE → SecurityException
    }

    private fun buildEffect(type: AppHapticType, hasAmplitudeControl: Boolean): VibrationEffect {
        // اگر موتور ویبره کنترل شدت ندارد، فقط «مدت» ریتم را تعیین می‌کند.
        fun amp(value: Int) = if (hasAmplitudeControl) value else VibrationEffect.DEFAULT_AMPLITUDE
        return when (type) {
            AppHapticType.Tick -> VibrationEffect.createOneShot(20L, amp(110))
            AppHapticType.Click -> VibrationEffect.createOneShot(30L, amp(170))
            AppHapticType.LongPress -> VibrationEffect.createOneShot(45L, amp(230))
            AppHapticType.Success -> {
                val timings = longArrayOf(0L, 30L, 60L, 40L)
                if (hasAmplitudeControl) {
                    VibrationEffect.createWaveform(timings, intArrayOf(0, 170, 0, 230), -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
            }
        }
    }

    private fun legacyDurationMs(type: AppHapticType): Long = when (type) {
        AppHapticType.Tick -> 20L
        AppHapticType.Click -> 30L
        AppHapticType.LongPress -> 45L
        AppHapticType.Success -> 70L
    }

    @Suppress("DEPRECATION")
    private fun vibrateViaView(type: AppHapticType) {
        val constant = when (type) {
            AppHapticType.Tick -> HapticFeedbackConstants.CLOCK_TICK
            AppHapticType.Click -> HapticFeedbackConstants.VIRTUAL_KEY
            AppHapticType.LongPress -> HapticFeedbackConstants.LONG_PRESS
            AppHapticType.Success ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(
            constant,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) { AppHaptics(context.applicationContext, view) }
}