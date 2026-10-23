package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs [DatabaseKeyStore] against an isolated prefs file and Keystore alias, so the real database
 * key of the installed app is never touched.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseKeyStoreTest {

    private lateinit var context: Context
    private lateinit var store: DatabaseKeyStore

    private fun newStore() = DatabaseKeyStore(
        context = context,
        prefsName = PREFS,
        keystoreAlias = ALIAS,
        maxAttempts = 2,
        retryDelayMs = 0
    )

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = newStore()
        store.discardAll()
    }

    @After
    fun tearDown() {
        store.discardAll()
    }

    @Test
    fun firstRun_createsAndPersistsA32ByteKey() {
        val key = store.getPassphrase(null)

        assertThat(key).hasLength(32)
        val persisted = prefs().getString(DatabaseKeyStore.KEY_WRAPPED_PASSPHRASE, null)
        assertThat(persisted).isNotNull()
        // The plain key must never be stored.
        assertThat(persisted).isNotEqualTo(Base64.encodeToString(key, Base64.NO_WRAP))
    }

    @Test
    fun newInstance_simulatingProcessRestart_readsTheSameKey() {
        val first = store.getPassphrase(null)
        val afterRestart = newStore().getPassphrase(null)
        assertThat(afterRestart).isEqualTo(first)
    }

    @Test
    fun returnedArrays_areIndependentCopies() {
        val first = store.getPassphrase(null)
        val expected = first.copyOf()
        first.fill(0)
        assertThat(store.getPassphrase(null)).isEqualTo(expected)
    }

    @Test
    fun resetHook_runsOnFirstRunOnly() {
        var calls = 0
        store.getPassphrase { calls++ }
        store.getPassphrase { calls++ }
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun failingResetHook_commitsNothing() {
        assertThrows(IllegalStateException::class.java) {
            store.getPassphrase { throw IllegalStateException("cannot delete database") }
        }
        assertThat(prefs().contains(DatabaseKeyStore.KEY_WRAPPED_PASSPHRASE)).isFalse()

        // The next attempt starts clean and succeeds.
        assertThat(store.getPassphrase(null)).hasLength(32)
    }

    @Test
    fun corruptedBlob_withoutHook_throwsLost_andKeepsStoredValue() {
        store.getPassphrase(null)
        val corrupted = Base64.encodeToString(ByteArray(64) { it.toByte() }, Base64.NO_WRAP)
        prefs().edit().putString(DatabaseKeyStore.KEY_WRAPPED_PASSPHRASE, corrupted).commit()

        assertThrows(DatabaseKeyLostException::class.java) { store.getPassphrase(null) }
        assertThat(prefs().getString(DatabaseKeyStore.KEY_WRAPPED_PASSPHRASE, null)).isEqualTo(corrupted)
    }

    @Test
    fun corruptedBlob_withHook_replacesKeyAndRaisesNoticeOnce() {
        val original = store.getPassphrase(null)
        prefs().edit().putString(DatabaseKeyStore.KEY_WRAPPED_PASSPHRASE, "!!not-base64!!").commit()

        var resets = 0
        val replacement = store.getPassphrase { resets++ }

        assertThat(resets).isEqualTo(1)
        assertThat(replacement).hasLength(32)
        assertThat(replacement).isNotEqualTo(original)
        assertThat(store.consumeKeyResetNotice()).isTrue()
        assertThat(store.consumeKeyResetNotice()).isFalse()

        // The replacement is durable and is used from now on without another reset.
        assertThat(newStore().getPassphrase { resets++ }).isEqualTo(replacement)
        assertThat(resets).isEqualTo(1)
    }

    @Test
    fun deletedKeystoreKey_isReportedAsLost() {
        store.getPassphrase(null)
        KeystoreSecretBox(ALIAS).deleteKey()

        assertThrows(DatabaseKeyLostException::class.java) { store.getPassphrase(null) }
    }

    private companion object {
        const val PREFS = "test_db_key_store"
        const val ALIAS = "test_budget_db_key"
    }
}