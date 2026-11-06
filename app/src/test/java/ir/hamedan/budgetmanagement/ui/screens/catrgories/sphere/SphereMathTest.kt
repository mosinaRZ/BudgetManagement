package ir.hamedan.budgetmanagement.ui.screens.categories.sphere

import com.google.common.truth.Truth.assertThat
import kotlin.math.PI
import org.junit.Test

class SphereMathTest {

    private val eps = 1e-4f

    private fun assertVec(actual: Vec3, expected: Vec3, tolerance: Float = eps) {
        assertThat(actual.x).isWithin(tolerance).of(expected.x)
        assertThat(actual.y).isWithin(tolerance).of(expected.y)
        assertThat(actual.z).isWithin(tolerance).of(expected.z)
    }

    // ---------------------------------------------------------------- Fibonacci

    @Test
    fun `fibonacci points are unit length, balanced and well separated`() {
        val points = SphereLayout.fibonacci(120)
        assertThat(points).hasSize(120)
        points.forEach { assertThat(it.length()).isWithin(1e-4f).of(1f) }
        assertThat(points.map { it.y }.average()).isWithin(1e-3).of(0.0)

        var minDistance = Float.MAX_VALUE
        for (i in points.indices) for (j in i + 1 until points.size) {
            val d = Vec3(
                points[i].x - points[j].x,
                points[i].y - points[j].y,
                points[i].z - points[j].z
            ).length()
            if (d < minDistance) minDistance = d
        }
        // فاصله‌ی ایده‌آل ≈ ۰٫۳۲؛ هیچ دو آیکونی نباید روی هم بیفتند.
        assertThat(minDistance).isGreaterThan(0.22f)
    }

    // ---------------------------------------------------------------- Quaternion

    @Test
    fun `dragging right moves the front of the sphere to the right`() {
        // مثل IconSphereState.dragBy: محور = (dy, dx, 0)
        val q = Quat.fromAxisAngle(Vec3(0f, 30f, 0f), 0.3f)
        val front = q.rotate(Vec3.Front)
        assertThat(front.x).isGreaterThan(0.2f)
        assertThat(front.y).isWithin(eps).of(0f)
    }

    @Test
    fun `dragging down moves the front of the sphere down (negative y)`() {
        val q = Quat.fromAxisAngle(Vec3(30f, 0f, 0f), 0.3f)
        val front = q.rotate(Vec3.Front)
        assertThat(front.y).isLessThan(-0.2f)
        assertThat(front.x).isWithin(eps).of(0f)
    }

    @Test
    fun `diagonal drag moves the front diagonally`() {
        val q = Quat.fromAxisAngle(Vec3(20f, 20f, 0f), 0.3f)
        val front = q.rotate(Vec3.Front)
        assertThat(front.x).isGreaterThan(0.1f)
        assertThat(front.y).isLessThan(-0.1f)
        assertThat(front.x).isWithin(eps).of(-front.y)
    }

    @Test
    fun `fromTo maps the source vector onto the target`() {
        val a = Vec3(0.6f, 0.0f, 0.8f)
        assertVec(Quat.fromTo(a, Vec3.Front).rotate(a), Vec3.Front)

        val b = Vec3(-0.3f, 0.9f, -0.3f).normalized()
        assertVec(Quat.fromTo(b, Vec3.Front).rotate(b), Vec3.Front)
    }

    @Test
    fun `fromTo handles identical and opposite vectors`() {
        assertThat(Quat.fromTo(Vec3.Front, Vec3.Front)).isEqualTo(Quat.Identity)
        val back = Vec3(0f, 0f, -1f)
        assertVec(Quat.fromTo(back, Vec3.Front).rotate(back), Vec3.Front)
    }

    @Test
    fun `scaledBy half applied twice equals the full rotation`() {
        val a = Vec3(0.6f, 0f, 0.8f)
        val full = Quat.fromTo(a, Vec3.Front)
        val half = full.scaledBy(0.5f)
        assertVec((half * half).rotate(a), Vec3.Front)
        assertVec(full.scaledBy(1f).rotate(a), Vec3.Front)
        assertVec(full.scaledBy(0f).rotate(a), a)
    }

    @Test
    fun `matrix and rotate agree`() {
        val q = Quat.fromAxisAngle(Vec3(1f, 2f, 3f), 0.9f)
        val v = Vec3(0.3f, -0.5f, 0.8f)
        val m = FloatArray(9)
        q.toMatrix(m)
        val viaMatrix = Vec3(
            m[0] * v.x + m[1] * v.y + m[2] * v.z,
            m[3] * v.x + m[4] * v.y + m[5] * v.z,
            m[6] * v.x + m[7] * v.y + m[8] * v.z
        )
        assertVec(viaMatrix, q.rotate(v))
    }

    @Test
    fun `thousands of incremental rotations stay normalised`() {
        var q = Quat.Identity
        val step = Quat.fromAxisAngle(Vec3(0.5f, 1f, 0.28f), 0.013f)
        repeat(20_000) { q = (step * q).normalized() }
        val len = kotlin.math.sqrt(q.w * q.w + q.x * q.x + q.y * q.y + q.z * q.z)
        assertThat(len).isWithin(1e-4f).of(1f)
        assertThat(q.rotate(Vec3(1f, 0f, 0f)).length()).isWithin(1e-3f).of(1f)
    }

    // ---------------------------------------------------------------- Depth cues

    @Test
    fun `nearer icons are larger and more opaque than farther ones`() {
        assertThat(SphereDepth.perspective(1f)).isGreaterThan(SphereDepth.perspective(0f))
        assertThat(SphereDepth.perspective(0f)).isGreaterThan(SphereDepth.perspective(-1f))
        assertThat(SphereDepth.perspective(0f)).isWithin(1e-6f).of(1f)

        assertThat(SphereDepth.alpha(1f)).isWithin(1e-6f).of(1f)
        assertThat(SphereDepth.alpha(-1f)).isWithin(1e-6f).of(SphereDepth.MIN_ALPHA)
        var previous = -1f
        for (i in 0..20) {
            val a = SphereDepth.alpha(-1f + i * 0.1f)
            assertThat(a).isAtLeast(previous)
            previous = a
        }
    }

    // ---------------------------------------------------------------- Projector

    private val axes = listOf(Vec3.Front, Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f))

    @Test
    fun `projector places the front point at the centre and sorts back to front`() {
        val projector = SphereProjector(axes)
        projector.project(Quat.Identity, centerX = 100f, centerY = 100f, radius = 50f)

        assertThat(projector.screenX[0]).isWithin(1e-3f).of(100f)
        assertThat(projector.screenY[0]).isWithin(1e-3f).of(100f)
        assertThat(projector.depth[0]).isWithin(1e-4f).of(1f)
        // نقطه‌ی (۱،۰،۰) روی لبه‌ی کره، در عمق صفر و با مقیاس ۱
        assertThat(projector.screenX[1]).isWithin(1e-3f).of(150f)
        assertThat(projector.scale[1]).isWithin(1e-4f).of(1f)
        // عقب‌ترین اول، جلوترین آخر
        assertThat(projector.order.toList()).containsExactly(2, 1, 0).inOrder()
    }

    @Test
    fun `projector keeps order sorted after rotation`() {
        val points = SphereLayout.fibonacci(60)
        val projector = SphereProjector(points)
        var q = Quat.Identity
        val step = Quat.fromAxisAngle(Vec3(0.5f, 1f, 0.28f), 0.2f)
        repeat(25) {
            q = (step * q).normalized()
            projector.project(q, 0f, 0f, 100f)
            for (k in 1 until projector.count) {
                assertThat(projector.depth[projector.order[k - 1]])
                    .isAtMost(projector.depth[projector.order[k]])
            }
        }
    }

    @Test
    fun `hit test returns the front icon under the finger and ignores empty space`() {
        val projector = SphereProjector(axes)
        projector.project(Quat.Identity, 100f, 100f, 50f)

        assertThat(projector.hitTest(100f, 100f, baseSize = 20f, slop = 4f)).isEqualTo(0)
        assertThat(projector.hitTest(152f, 100f, baseSize = 20f, slop = 4f)).isEqualTo(1)
        assertThat(projector.hitTest(10f, 10f, baseSize = 20f, slop = 4f)).isEqualTo(-1)
    }

    @Test
    fun `icons on the back half cannot be selected`() {
        val projector = SphereProjector(listOf(Vec3(0f, 0f, -1f)))
        projector.project(Quat.Identity, 100f, 100f, 50f)
        assertThat(projector.hitTest(100f, 100f, baseSize = 20f, slop = 4f)).isEqualTo(-1)
    }

    @Test
    fun `rotating by half a turn swaps front and back`() {
        val projector = SphereProjector(listOf(Vec3.Front))
        projector.project(Quat.fromAxisAngle(Vec3(0f, 1f, 0f), PI.toFloat()), 0f, 0f, 100f)
        assertThat(projector.depth[0]).isWithin(1e-4f).of(-1f)
    }

    // ---------------------------------------------------------------- Interaction state

    @Test
    fun `state drag follows the finger direction`() {
        val state = IconSphereState(Quat.Identity)
        state.beginDrag()
        state.dragBy(dx = 40f, dy = 0f, radiusPx = 200f)
        assertThat(state.rotation.rotate(Vec3.Front).x).isGreaterThan(0.1f)

        val down = IconSphereState(Quat.Identity)
        down.beginDrag()
        down.dragBy(dx = 0f, dy = 40f, radiusPx = 200f)
        assertThat(down.rotation.rotate(Vec3.Front).y).isLessThan(-0.1f)
    }

    @Test
    fun `fast release keeps spinning and then friction stops it`() {
        val state = IconSphereState(Quat.Identity)
        state.beginDrag()
        state.endDrag(velocityX = 1500f, velocityY = -700f, radiusPx = 200f)
        assertThat(state.spinSpeed).isGreaterThan(0f)
        assertThat(state.spinSpeed).isAtMost(12f)

        var guard = 0
        while (state.spinSpeed > 0f && guard++ < 10_000) state.stepInertia(1f / 60f)
        assertThat(state.spinSpeed).isEqualTo(0f)
        assertThat(guard).isLessThan(10_000)
    }

    @Test
    fun `slow release does not start a spin`() {
        val state = IconSphereState(Quat.Identity)
        state.beginDrag()
        state.endDrag(velocityX = 5f, velocityY = 5f, radiusPx = 200f)
        assertThat(state.spinSpeed).isEqualTo(0f)
    }
}