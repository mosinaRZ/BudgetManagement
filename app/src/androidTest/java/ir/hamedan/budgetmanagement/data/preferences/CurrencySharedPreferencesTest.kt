package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Critical: wrong currency unit multiplies/divides displayed and entered amounts by 10.
 *
 * Contract used across the app:
 * - Stored amounts in DB are in Toman-scale
 * - "IRT" (default) → show/enter as Toman (no ×10)
 * - "IRR" → show as Rial (×10) and when user enters Rial, save amount/10
 */
@RunWith(AndroidJUnit4::class)
class CurrencySharedPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearPrefs()
        // Reset in-memory flow to known default before each test
        CurrencySharedPreferences.setCurrency(context, "IRT")
    }

    @After
    fun tearDown() {
        clearPrefs()
        CurrencySharedPreferences.setCurrency(context, "IRT")
    }

    private fun clearPrefs() {
        context.getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    @Test
    fun getCurrency_whenNeverSet_returnsIRT() {
        clearPrefs()
        // After clear, disk has no key; getCurrency must fall back to IRT
        // Note: in-memory flow may still hold previous value until init/set
        val fromDisk = context
            .getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .getString("app_currency", "IRT")

        Truth.assertThat(fromDisk).isEqualTo("IRT")
        Truth.assertThat(CurrencySharedPreferences.getCurrency(context)).isEqualTo("IRT")
    }

    // -------------------------------------------------------------------------
    // Persist
    // -------------------------------------------------------------------------

    @Test
    fun setCurrency_IRR_persistsToDisk() {
        CurrencySharedPreferences.setCurrency(context, "IRR")

        val raw = context
            .getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .getString("app_currency", null)

        Truth.assertThat(raw).isEqualTo("IRR")
        Truth.assertThat(CurrencySharedPreferences.getCurrency(context)).isEqualTo("IRR")
    }

    @Test
    fun setCurrency_IRT_persistsToDisk() {
        CurrencySharedPreferences.setCurrency(context, "IRR")
        CurrencySharedPreferences.setCurrency(context, "IRT")

        Truth.assertThat(CurrencySharedPreferences.getCurrency(context)).isEqualTo("IRT")
    }

    @Test
    fun setCurrency_survivesReReadWithoutInit() {
        CurrencySharedPreferences.setCurrency(context, "IRR")

        // Simulate cold read path used by getCurrency (disk only)
        val again = context
            .getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .getString("app_currency", "IRT")

        Truth.assertThat(again).isEqualTo("IRR")
    }

    // -------------------------------------------------------------------------
    // init() syncs disk → flow
    // -------------------------------------------------------------------------

    @Test
    fun init_loadsDiskValueIntoFlow() = runTest {
        // Write disk directly (as if previous app session)
        context.getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_currency", "IRR")
            .commit()

        // Force flow away from disk value
        CurrencySharedPreferences.setCurrency(context, "IRT")
        // Overwrite disk again to IRR without going through setCurrency flow path:
        context.getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_currency", "IRR")
            .commit()

        CurrencySharedPreferences.init(context)

        Truth.assertThat(CurrencySharedPreferences.currencyFlow.first()).isEqualTo("IRR")
        Truth.assertThat(CurrencySharedPreferences.getCurrency(context)).isEqualTo("IRR")
    }

    // -------------------------------------------------------------------------
    // Flow emissions (UI listens to this)
    // -------------------------------------------------------------------------

    @Test
    fun setCurrency_updatesCurrencyFlowImmediately() = runTest {
        CurrencySharedPreferences.setCurrency(context, "IRT")
        Truth.assertThat(CurrencySharedPreferences.currencyFlow.first()).isEqualTo("IRT")

        CurrencySharedPreferences.setCurrency(context, "IRR")
        Truth.assertThat(CurrencySharedPreferences.currencyFlow.first()).isEqualTo("IRR")

        CurrencySharedPreferences.setCurrency(context, "IRT")
        Truth.assertThat(CurrencySharedPreferences.currencyFlow.first()).isEqualTo("IRT")
    }

    // -------------------------------------------------------------------------
    // Pref file isolation
    // -------------------------------------------------------------------------

    @Test
    fun usesDedicatedPrefFile_notDefaultSettings() {
        CurrencySharedPreferences.setCurrency(context, "IRR")

        val currencyFile = context
            .getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .getString("app_currency", null)

        val settingsFile = context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("app_currency", null)

        Truth.assertThat(currencyFile).isEqualTo("IRR")
        Truth.assertThat(settingsFile).isNull() // must not leak into LocaleHelper prefs
    }
}