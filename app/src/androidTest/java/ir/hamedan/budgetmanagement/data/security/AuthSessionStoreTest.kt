package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Test

class AuthSessionStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("budget_auth_session", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun save_marksSessionAuthenticatedAndClearRemovesCredentials() {
        val store = AuthSessionStore(context)
        assertThat(store.isAuthenticated()).isFalse()

        store.save(
            accessToken = "access",
            refreshToken = "refresh",
            userId = "user-1",
            deviceId = "device-1",
            kdfSalt = "salt",
            passwordKeyEnvelope = "envelope",
            passwordKeyNonce = "nonce",
            recoveryKeyEnvelope = "recovery-envelope",
            recoveryKeyNonce = "recovery-nonce",
            role = "USER"
        )

        assertThat(store.isAuthenticated()).isTrue()
        assertThat(store.accessToken()).isEqualTo("access")
        assertThat(store.role()).isEqualTo("USER")
        assertThat(store.authenticated.value).isTrue()

        store.clearSession()

        assertThat(store.isAuthenticated()).isFalse()
        assertThat(store.refreshToken()).isNull()
        assertThat(store.userId()).isNull()
        assertThat(store.deviceId()).isEqualTo("device-1")
    }
}