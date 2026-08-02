package ir.hamedan.budgetmanagement.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * LocaleHelper controls app language + layout direction persistence.
 * Default language is Persian ("fa").
 *
 * Pref file: "settings"
 * Key: "Locale.Helper.Selected.Language"
 */
@RunWith(AndroidJUnit4::class)
class LocaleHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearLanguagePrefs()
    }

    @After
    fun tearDown() {
        clearLanguagePrefs()
        // restore default fa for other instrumented tests in the same process
        LocaleHelper.setLocale(context, "fa")
    }

    private fun clearLanguagePrefs() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun readRawLanguage(): String? {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("Locale.Helper.Selected.Language", null)
    }

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    @Test
    fun getLanguage_whenNeverSet_returnsFa() {
        clearLanguagePrefs()
        assertThat(LocaleHelper.getLanguage(context)).isEqualTo("fa")
    }

    // -------------------------------------------------------------------------
    // Persist
    // -------------------------------------------------------------------------

    @Test
    fun setLocale_en_persistsToSettingsPrefs() {
        LocaleHelper.setLocale(context, "en")

        assertThat(readRawLanguage()).isEqualTo("en")
        assertThat(LocaleHelper.getLanguage(context)).isEqualTo("en")
    }

    @Test
    fun setLocale_fa_persistsToSettingsPrefs() {
        LocaleHelper.setLocale(context, "en")
        LocaleHelper.setLocale(context, "fa")

        assertThat(readRawLanguage()).isEqualTo("fa")
        assertThat(LocaleHelper.getLanguage(context)).isEqualTo("fa")
    }

    @Test
    fun setLocale_doesNotWriteIntoCurrencyPrefs() {
        LocaleHelper.setLocale(context, "en")

        val currencyLeak = context
            .getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .getString("Locale.Helper.Selected.Language", null)

        assertThat(currencyLeak).isNull()
        assertThat(readRawLanguage()).isEqualTo("en")
    }

    // -------------------------------------------------------------------------
    // Locale / configuration
    // -------------------------------------------------------------------------

    @Test
    fun setLocale_updatesDefaultLocale() {
        LocaleHelper.setLocale(context, "en")
        assertThat(Locale.getDefault().language).isEqualTo("en")

        LocaleHelper.setLocale(context, "fa")
        assertThat(Locale.getDefault().language).isEqualTo("fa")
    }

    @Test
    fun setLocale_returnsContextWithMatchingLanguage() {
        val enCtx = LocaleHelper.setLocale(context, "en")
        assertThat(enCtx.resources.configuration.locales[0].language).isEqualTo("en")

        val faCtx = LocaleHelper.setLocale(context, "fa")
        assertThat(faCtx.resources.configuration.locales[0].language).isEqualTo("fa")
    }

    @Test
    fun setLocale_fa_setsRtlLayoutDirection() {
        val faCtx = LocaleHelper.setLocale(context, "fa")
        // LAYOUT_DIRECTION_RTL = 1
        assertThat(faCtx.resources.configuration.layoutDirection)
            .isEqualTo(android.util.LayoutDirection.RTL)
    }

    @Test
    fun setLocale_en_setsLtrLayoutDirection() {
        val enCtx = LocaleHelper.setLocale(context, "en")
        assertThat(enCtx.resources.configuration.layoutDirection)
            .isEqualTo(android.util.LayoutDirection.LTR)
    }

    // -------------------------------------------------------------------------
    // onAttach
    // -------------------------------------------------------------------------

    @Test
    fun onAttach_withoutPriorChoice_usesFaDefault() {
        clearLanguagePrefs()
        val attached = LocaleHelper.onAttach(context)

        assertThat(LocaleHelper.getLanguage(context)).isEqualTo("fa")
        assertThat(attached.resources.configuration.locales[0].language).isEqualTo("fa")
    }

    @Test
    fun onAttach_withPersistedEn_restoresEnglish() {
        LocaleHelper.setLocale(context, "en")

        val attached = LocaleHelper.onAttach(context)

        assertThat(LocaleHelper.getLanguage(context)).isEqualTo("en")
        assertThat(attached.resources.configuration.locales[0].language).isEqualTo("en")
    }
}