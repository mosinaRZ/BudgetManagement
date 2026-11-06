package ir.hamedan.budgetmanagement.ui.screens.categories.sphere

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * ریاضیِ «کره‌ی آیکون». عمداً بدون وابستگی به Compose/Android نوشته شده تا روی JVM تست شود.
 *
 * دستگاه مختصات: x به راست، y به بالا، z به سمت بیننده (راست‌گرد).
 * هنگام رسم، y به مختصات صفحه (که رو به پایین است) برگردانده می‌شود.
 */

internal data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(k: Float) = Vec3(x * k, y * k, z * k)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val l = length()
        return if (l < 1e-6f) this else Vec3(x / l, y / l, z / l)
    }

    companion object {
        val Front = Vec3(0f, 0f, 1f)
    }
}

/** کواترنیون یکه برای چرخش؛ برخلاف ماتریس، با هزاران به‌روزرسانی پشت‌سرهم دچار رانش نمی‌شود. */
internal data class Quat(val w: Float, val x: Float, val y: Float, val z: Float) {

    operator fun times(o: Quat) = Quat(
        w * o.w - x * o.x - y * o.y - z * o.z,
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w
    )

    fun normalized(): Quat {
        val l = sqrt(w * w + x * x + y * y + z * z)
        return if (l < 1e-6f) Identity else Quat(w / l, x / l, y / l, z / l)
    }

    fun rotate(v: Vec3): Vec3 {
        val q = Vec3(x, y, z)
        val t = q.cross(v) * 2f
        return v + t * w + q.cross(t)
    }

    /** ماتریس چرخش ۳×۳ به‌صورت سطری در [out] (۹ عدد). */
    fun toMatrix(out: FloatArray) {
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z
        out[0] = 1f - 2f * (yy + zz); out[1] = 2f * (xy - wz); out[2] = 2f * (xz + wy)
        out[3] = 2f * (xy + wz); out[4] = 1f - 2f * (xx + zz); out[5] = 2f * (yz - wx)
        out[6] = 2f * (xz - wy); out[7] = 2f * (yz + wx); out[8] = 1f - 2f * (xx + yy)
    }

    /** همین چرخش، اما فقط [t] برابر (۰..۱) زاویه‌اش؛ برای انیمیشن نرمِ «بیا جلو». */
    fun scaledBy(t: Float): Quat {
        val q = if (w < 0f) Quat(-w, -x, -y, -z) else this
        val half = acos(q.w.coerceIn(-1f, 1f))
        if (half < 1e-5f) return Identity
        val s = sin(half)
        return fromAxisAngle(Vec3(q.x / s, q.y / s, q.z / s), 2f * half * t)
    }

    companion object {
        val Identity = Quat(1f, 0f, 0f, 0f)

        fun fromAxisAngle(axis: Vec3, angle: Float): Quat {
            val n = axis.normalized()
            if (n.length() < 1e-6f) return Identity
            val s = sin(angle / 2f)
            return Quat(cos(angle / 2f), n.x * s, n.y * s, n.z * s)
        }

        /** کوتاه‌ترین چرخشی که بردار یکه‌ی [a] را روی بردار یکه‌ی [b] می‌برد. */
        fun fromTo(a: Vec3, b: Vec3): Quat {
            val d = a.dot(b)
            if (d > 0.999999f) return Identity
            if (d < -0.999999f) {
                var axis = Vec3(1f, 0f, 0f).cross(a)
                if (axis.length() < 1e-4f) axis = Vec3(0f, 1f, 0f).cross(a)
                return fromAxisAngle(axis, PI.toFloat())
            }
            val c = a.cross(b)
            return Quat(1f + d, c.x, c.y, c.z).normalized()
        }
    }
}

internal object SphereLayout {

    private const val GOLDEN_ANGLE = 2.3999632f // π(3 − √5)

    /**
     * [count] نقطه‌ی یکنواخت روی کره‌ی یکه (مارپیچ فیبوناچی)؛ بدون خوشه‌شدن در قطب‌ها
     * و با فاصله‌ی تقریباً برابر بین همسایه‌ها.
     */
    fun fibonacci(count: Int): List<Vec3> = List(count) { i ->
        val y = 1f - 2f * (i + 0.5f) / count
        val r = sqrt((1f - y * y).coerceAtLeast(0f))
        val phi = i * GOLDEN_ANGLE
        Vec3(cos(phi) * r, y, sin(phi) * r)
    }
}

/** پرسپکتیو، شفافیت و اندازه بر پایه‌ی عمق؛ آیکون‌های جلو درشت و پررنگ، آیکون‌های پشت ریز و کم‌رنگ. */
internal object SphereDepth {
    /** فاصله‌ی دوربین از مرکز کره، بر حسب شعاع. کمتر = پرسپکتیو تندتر. */
    const val CAMERA_DISTANCE = 3.4f
    const val MIN_ALPHA = 0.10f

    /** مقیاس پرسپکتیو برای عمق [z] در بازه‌ی −۱ (پشت) تا ۱ (جلو). */
    fun perspective(z: Float): Float = CAMERA_DISTANCE / (CAMERA_DISTANCE - z)

    fun alpha(z: Float): Float {
        val t = ((z + 1f) / 2f).coerceIn(0f, 1f)
        return MIN_ALPHA + (1f - MIN_ALPHA) * t.pow(1.7f)
    }

    /** بیشترین فاصله‌ی یک آیکون از مرکز، بر حسب شعاع (برای محاسبه‌ی اندازه‌ی امن کره در صفحه). */
    const val MAX_EXTENT = 1.05f
}

/**
 * نگاشت نقاط کره به صفحه برای یک چرخش مشخص. آرایه‌ها بین فریم‌ها دوباره استفاده می‌شوند تا رسم
 * در هر فریم هیچ شیء تازه‌ای نسازد. رسم و لمس هر دو از همین نتیجه استفاده می‌کنند؛ پس آنچه
 * کاربر می‌بیند دقیقاً همان چیزی است که لمس می‌کند.
 */
internal class SphereProjector(points: List<Vec3>) {
    val count = points.size
    private val px = FloatArray(count) { points[it].x }
    private val py = FloatArray(count) { points[it].y }
    private val pz = FloatArray(count) { points[it].z }
    private val m = FloatArray(9)

    val screenX = FloatArray(count)
    val screenY = FloatArray(count)
    val depth = FloatArray(count)
    val scale = FloatArray(count)

    /** اندیس‌ها از عقب به جلو مرتب‌اند (برای رسم به روش نقاش). */
    val order = IntArray(count) { it }

    fun project(rotation: Quat, centerX: Float, centerY: Float, radius: Float) {
        rotation.toMatrix(m)
        for (i in 0 until count) {
            val x = m[0] * px[i] + m[1] * py[i] + m[2] * pz[i]
            val y = m[3] * px[i] + m[4] * py[i] + m[5] * pz[i]
            val z = m[6] * px[i] + m[7] * py[i] + m[8] * pz[i]
            val f = SphereDepth.perspective(z)
            screenX[i] = centerX + x * radius * f
            screenY[i] = centerY - y * radius * f
            depth[i] = z
            scale[i] = f
        }
        sortByDepth()
    }

    /** مرتب‌سازی درجی؛ بین دو فریم ترتیب تقریباً ثابت است، پس تقریباً O(n) است. */
    private fun sortByDepth() {
        for (i in 1 until count) {
            val key = order[i]
            val keyDepth = depth[key]
            var j = i - 1
            while (j >= 0 && depth[order[j]] > keyDepth) {
                order[j + 1] = order[j]
                j--
            }
            order[j + 1] = key
        }
    }

    /**
     * نزدیک‌ترین آیکونِ رو به بیننده که لمسِ ([x], [y]) داخل دایره‌ی لمسش افتاده؛ وگرنه ‎−۱.
     * [baseSize] اندازه‌ی آیکون در عمق صفر (مقیاس ۱) و [slop] حاشیه‌ی اضافه‌ی لمس است (هر دو پیکسل).
     * آیکون‌های نیمه‌ی پشتی قابل‌انتخاب نیستند (کم‌رنگ‌اند و پشت بقیه قرار دارند).
     */
    fun hitTest(x: Float, y: Float, baseSize: Float, slop: Float, minDepth: Float = -0.05f): Int {
        var best = -1
        var bestDepth = Float.NEGATIVE_INFINITY
        for (i in 0 until count) {
            if (depth[i] < minDepth) continue
            val reach = baseSize * scale[i] / 2f + slop
            if (hypot(x - screenX[i], y - screenY[i]) <= reach && depth[i] > bestDepth) {
                best = i
                bestDepth = depth[i]
            }
        }
        return best
    }
}