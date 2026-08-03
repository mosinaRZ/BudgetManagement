package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseKeyProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun getPassphrase_returnsNonEmptyKey() {
        val key = DatabaseKeyProvider.getPassphrase(context)
        assertThat(key).isNotEmpty()
        assertThat(key.size).isAtLeast(16)
    }

    @Test
    fun getPassphrase_returnsSameKeyOnMultipleCalls() {
        val key1 = DatabaseKeyProvider.getPassphrase(context)
        val key2 = DatabaseKeyProvider.getPassphrase(context)
        assertThat(key1).isEqualTo(key2)
    }

    @Test
    fun getPassphrase_isConsistentAcrossCalls() {
        val keys = List(5) { DatabaseKeyProvider.getPassphrase(context) }
        keys.forEach { assertThat(it).isEqualTo(keys.first()) }
    }
}