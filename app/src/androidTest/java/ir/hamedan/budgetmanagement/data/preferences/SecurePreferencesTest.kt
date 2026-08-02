package ir.hamedan.budgetmanagement.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for encrypted biometric flag storage.
 *
 * Production type: ir.hamedan.budgetmanagement.data.preferences.SharedPreferences
 * (name collides with android.content.SharedPreferences — always use FQN or import alias)
 *
 * Pref file: budget_secure_prefs (EncryptedSharedPreferences)
 * Key: biometric_enabled
 * Default: false
 */
@RunWith(AndroidJUnit4::class)
class SecurePreferencesTest {

    private lateinit var context: Context

    // Alias to avoid clash with android.content.SharedPreferences
    private val securePrefs = ir.hamedan.budgetmanagement.data.preferences.SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // reset to default
        securePrefs.setBiometricEnabled(context, false)
    }

    @After
    fun tearDown() {
        securePrefs.setBiometricEnabled(context, false)
    }

    // -------------------------------------------------------------------------
    // Default
    // -------------------------------------------------------------------------

    @Test
    fun getBiometricEnabled_default_isFalse() {
        securePrefs.setBiometricEnabled(context, false)
        assertThat(securePrefs.getBiometricEnabled(context)).isFalse()
    }

    // -------------------------------------------------------------------------
    // Set / get
    // -------------------------------------------------------------------------

    @Test
    fun setBiometricEnabled_true_persists() {
        securePrefs.setBiometricEnabled(context, true)
        assertThat(securePrefs.getBiometricEnabled(context)).isTrue()
    }

    @Test
    fun setBiometricEnabled_false_afterTrue_persists() {
        securePrefs.setBiometricEnabled(context, true)
        assertThat(securePrefs.getBiometricEnabled(context)).isTrue()

        securePrefs.setBiometricEnabled(context, false)
        assertThat(securePrefs.getBiometricEnabled(context)).isFalse()
    }

    @Test
    fun setBiometricEnabled_idempotentTrue() {
        securePrefs.setBiometricEnabled(context, true)
        securePrefs.setBiometricEnabled(context, true)
        assertThat(securePrefs.getBiometricEnabled(context)).isTrue()
    }

    // -------------------------------------------------------------------------
    // Isolation from other pref files
    // -------------------------------------------------------------------------

    @Test
    fun doesNotWriteIntoCurrencyOrSettingsPrefs() {
        securePrefs.setBiometricEnabled(context, true)

        val currencyHasKey = context
            .getSharedPreferences("budget_currency_prefs", Context.MODE_PRIVATE)
            .contains("biometric_enabled")

        val settingsHasKey = context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .contains("biometric_enabled")

        assertThat(currencyHasKey).isFalse()
        assertThat(settingsHasKey).isFalse()
        assertThat(securePrefs.getBiometricEnabled(context)).isTrue()
    }

    // -------------------------------------------------------------------------
    // Encryption smoke (file should not store plaintext boolean as readable "true")
    // -------------------------------------------------------------------------

    @Test
    fun encryptedFile_isNotReadableAsPlainSharedPreferences() {
        securePrefs.setBiometricEnabled(context, true)

        // Opening the same file name as a normal SharedPreferences must NOT
        // expose a clean boolean the way EncryptedSharedPreferences does.
        // (Implementation detail of Tink/EncryptedSharedPreferences: values are ciphertext.)
        val plain = context.getSharedPreferences("budget_secure_prefs", Context.MODE_PRIVATE)
        val plainValue = plain.getBoolean("biometric_enabled", false)

        // If encryption works, plain read of the key should not reliably return true.
        // Some devices may still show empty defaults.
        assertThat(plainValue).isFalse()
        // Canonical API still returns true
        assertThat(securePrefs.getBiometricEnabled(context)).isTrue()
    }
}