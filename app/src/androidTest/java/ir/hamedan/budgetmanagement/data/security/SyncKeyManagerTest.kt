package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class SyncKeyManagerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("budget_sync_crypto", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun registrationMaterial_roundTripsEncryptedPayload() {
        val manager = SyncKeyManager(context)
        val material = manager.createRegistrationMaterial("StrongPassword123!".toCharArray())

        manager.initializeFromLogin(
            password = "StrongPassword123!".toCharArray(),
            kdfSaltB64 = material.kdfSalt,
            envelopeB64 = material.passwordKeyEnvelope,
            nonceB64 = material.passwordKeyNonce
        )

        val plaintext = "sensitive-financial-data".toByteArray()
        val encrypted = manager.encrypt(plaintext)
        assertThat(manager.decrypt(encrypted.ciphertext, encrypted.nonce))
            .isEqualTo(plaintext)
        assertThat(material.recoveryKey).isNotEmpty()
        assertThat(material.recoveryKeyHash).isNotEmpty()
    }
}