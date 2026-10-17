package ir.hamedan.budgetmanagement.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import ir.hamedan.budgetmanagement.R
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max

/** نتیجه‌ی رندر: bitmap + ارتفاع آن بر حسب dp (برای تنظیم اندازه‌ی Image در Glance). */
internal class RenderedText(val bitmap: Bitmap, val heightDp: Float)

/**
 * Glance فقط از RemoteViews استفاده می‌کند و نمی‌تواند فونت سفارشی (وزیرمتن / اینتر) را
 * روی Text بنشاند، و فیت خودکار متن (مثل AutoFitAmountText در HomeScreen) هم ندارد.
 * راه‌حل استاندارد: مبلغ را با StaticLayout روی Bitmap رسم کنیم.
 *  - همان فونت اپ
 *  - اندازه‌گیری دقیق عرض متن و کوچک‌کردن پله‌ای فونت (×0.92) مثل HomeScreen
 *  - اگر در حداقل فونت هم جا نشد: متن فشرده (میلیون/میلیارد)
 *  - جهت متن (RTL/LTR) فقط به زبان اپ وابسته است، نه به زبان سیستم
 */
internal object WidgetTextRenderer {

    private val typefaces = ConcurrentHashMap<Int, Typeface>()

    private fun typeface(context: Context, isPersian: Boolean): Typeface {
        val resId = if (isPersian) R.font.vazirmatn_bold else R.font.inter_18pt_bold
        return typefaces.getOrPut(resId) {
            ResourcesCompat.getFont(context.applicationContext, resId) ?: Typeface.DEFAULT_BOLD
        }
    }

    /**
     * @param amountText      مبلغ کامل (مثلاً «-۱٬۲۵۰٬۰۰۰»)
     * @param compactText     مبلغ فشرده برای وقتی که جا نمی‌شود (مثلاً «-۱٫۲۵ میلیون»)، یا null
     * @param unitText        واحد پول؛ کوچک‌تر و کم‌رنگ‌تر از عدد رسم می‌شود
     * @param unitScale       نسبت اندازه‌ی واحد به عدد (مثلاً 0.5)
     * @param alignment       ALIGN_NORMAL = ابتدای خط بر اساس زبان متن، ALIGN_CENTER = وسط‌چین
     */
    fun renderAmount(
        context: Context,
        amountText: String,
        compactText: String?,
        unitText: String,
        isPersian: Boolean,
        amountColor: Int,
        unitColor: Int,
        maxWidthDp: Float,
        maxSp: Float,
        minSp: Float,
        unitScale: Float,
        alignment: Layout.Alignment
    ): RenderedText {
        val metrics = context.resources.displayMetrics
        val widthPx = max(1, (maxWidthDp * metrics.density).toInt())
        val face = typeface(context, isPersian)

        fun paintFor(sizeSp: Float) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            color = amountColor
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
        }

        fun textFor(amount: String): CharSequence {
            val sb = SpannableStringBuilder(amount)
            if (unitText.isNotEmpty()) {
                sb.append('\u00A0')                       // فاصله‌ی غیرشکن
                val start = sb.length
                sb.append(unitText)
                sb.setSpan(RelativeSizeSpan(unitScale), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(ForegroundColorSpan(unitColor), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return sb
        }

        // ---- فیت خودکار: دقیقاً منطق AutoFitAmountText در HomeScreen ----
        var sizeSp = maxSp
        var current = amountText
        var usedCompact = false
        while (true) {
            val desired = Layout.getDesiredWidth(textFor(current), paintFor(sizeSp))
            if (ceil(desired).toInt() <= widthPx) break
            if (sizeSp > minSp + 0.1f) {
                sizeSp = max(minSp, sizeSp * 0.92f)
            } else if (!usedCompact && compactText != null) {
                usedCompact = true
                current = compactText
                sizeSp = maxSp
            } else break
        }

        val text = textFor(current)
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paintFor(sizeSp), widthPx)
            .setAlignment(alignment)
            .setIncludePad(false)
            .setMaxLines(1)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR) // همان رفتار پیش‌فرض Compose
            .build()

        val bitmap = Bitmap.createBitmap(widthPx, max(1, layout.height), Bitmap.Config.ARGB_8888)
        layout.draw(Canvas(bitmap))
        return RenderedText(bitmap, layout.height / metrics.density)
    }
}