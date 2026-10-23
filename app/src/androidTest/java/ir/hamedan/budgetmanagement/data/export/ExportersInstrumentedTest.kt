
package ir.hamedan.budgetmanagement.data.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportersInstrumentedTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val stats = ExportStats(
        0.0,
        1200.0,
        450.0,
        750.0,
        750.0,
        "IRR",
        1_700_000_000_000L,
        1_690_000_000_000L,
        1_710_000_000_000L
    )

    @Test
    fun pdfExporter() {
        val empty = PdfExporter.generate(
            context,
            emptyList(),
            stats,
            isPersian = false
        )

        var populated: File? = null

        try {
            assertTrue(empty.isFile && empty.length() > 500)

            val header = empty.inputStream().bufferedReader().use {
                it.readLine()?.take(4)
            }
            assertEquals("%PDF", header)

            populated = PdfExporter.generate(
                context,
                listOf(
                    TransactionEntity(
                        id = "tx-2",
                        title = "Rent",
                        amount = 450,
                        categoryId = "home",
                        type = "EXPENSE",
                        timestamp = 1_700_000_000_000L,
                        note = "monthly rent"
                    )
                ),
                stats,
                isPersian = true,
                categoryNames = mapOf("home" to "خانه")
            )

            assertTrue(populated.isFile && populated.length() > 500)

            val populatedHeader = populated.inputStream().bufferedReader().use {
                it.readLine()?.take(4)
            }
            assertEquals("%PDF", populatedHeader)
        } finally {
            empty.delete()
            populated?.delete()
        }
    }
}
