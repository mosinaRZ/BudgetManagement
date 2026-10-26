package ir.hamedan.budgetmanagement.data.share

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionShareCardRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun slotsThatHaveAPicture_resolveToARealResource() {
        // share_bg_01..05, 07..09 and 11 are part of the project.
        listOf(0, 1, 2, 3, 4, 6, 7, 8, 10).forEach { index ->
            assertWithMessage("slot $index").that(TransactionShareCardRenderer.backgroundResId(context, index))
                .isNotEqualTo(0)
            assertWithMessage("slot $index").that(TransactionShareCardRenderer.hasArtwork(context, index)).isTrue()
        }
    }

    @Test
    fun slotIndexesWrapAround() {
        assertThat(TransactionShareCardRenderer.backgroundResId(context, 12))
            .isEqualTo(TransactionShareCardRenderer.backgroundResId(context, 0))
        assertThat(TransactionShareCardRenderer.backgroundResId(context, -12))
            .isEqualTo(TransactionShareCardRenderer.backgroundResId(context, 0))
    }

    @Test
    fun cardsOpenWithARealPictureWheneverOneExists() {
        for (seed in -300..300) {
            val index = TransactionShareCardRenderer.defaultBackgroundIndex(context, seed)
            assertThat(index).isIn(0 until TransactionShareCardRenderer.BACKGROUND_COUNT)
            assertWithMessage("seed $seed -> slot $index")
                .that(TransactionShareCardRenderer.hasArtwork(context, index)).isTrue()
        }
    }

    @Test
    fun differentSeedsSpreadOverSeveralPictures() {
        val used = (0 until 200).map { TransactionShareCardRenderer.defaultBackgroundIndex(context, it) }.toSet()
        assertThat(used.size).isAtLeast(5)
    }
}