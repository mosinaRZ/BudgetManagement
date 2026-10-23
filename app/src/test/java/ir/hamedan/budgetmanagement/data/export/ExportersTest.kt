package ir.hamedan.budgetmanagement.data.export

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
// Requires android.testOptions.unitTests.isIncludeAndroidResources = true in app/build.gradle.kts.
@Config(application = Application::class, sdk = [35])
class ExportersTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val stats = ExportStats(0.0, 1200.0, 450.0, 750.0, 750.0, "IRR", 1_700_000_000_000L, 1_690_000_000_000L, 1_710_000_000_000L)

    @Test fun `xlsx exporter creates readable workbook with empty transactions`() {
        val file = XlsxExporter.generate(context, emptyList(), stats, isPersian = false)
        assertTrue(file.isFile)
        assertTrue(file.length() > 500)
        ZipFile(file).use { zip ->
            assertNotNull("workbook metadata must exist", zip.getEntry("xl/workbook.xml"))
            assertNotNull("worksheet must exist", zip.getEntry("xl/worksheets/sheet1.xml"))
            val sharedStrings = zip.getInputStream(zip.getEntry("xl/sharedStrings.xml")).bufferedReader().use { it.readText() }
            assertTrue(sharedStrings.contains("Cidna Financial Statement"))
            assertTrue(sharedStrings.contains("Statement"))
        }
        file.delete()
    }

    @Test fun `xlsx exporter handles transaction values and category mapping`() {
        val transaction = TransactionEntity(id = "tx-1", title = "Salary", amount = 1200, categoryId = "income", type = "INCOME", timestamp = 1_700_000_000_000L, note = "monthly")
        val file = XlsxExporter.generate(context, listOf(transaction), stats, isPersian = false, categoryNames = mapOf("income" to "Salary"))
        assertTrue(file.isFile && file.length() > 500)
        ZipFile(file).use { zip ->
            val sharedStrings = zip.getInputStream(zip.getEntry("xl/sharedStrings.xml")).bufferedReader().use { it.readText() }
            assertTrue(sharedStrings.contains("Salary"))
            assertTrue(sharedStrings.contains("monthly"))
        }
        file.delete()
    }
}
