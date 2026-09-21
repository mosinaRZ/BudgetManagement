package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client-side data-key management for encrypted sync payloads.
 *
 * The backend stores ciphertext and never needs the plaintext data key.
 * The password-key envelope format is deliberately kept local to the
 * Android client because the Go service treats the envelope as opaque bytes.
 */
class SyncKeyManager(private val context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun createRegistrationMaterial(password: CharArray): RegistrationMaterial {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val dataKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrappingKey = deriveWrappingKey(password, salt)
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(128, nonce))
        val envelope = cipher.doFinal(dataKey)
        storeDataKey(dataKey)
        return RegistrationMaterial(
            kdfSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
            passwordKeyEnvelope = Base64.encodeToString(envelope, Base64.NO_WRAP),
            passwordKeyNonce = Base64.encodeToString(nonce, Base64.NO_WRAP)
        )
    }

    fun initializeFromLogin(password: CharArray, kdfSaltB64: String, envelopeB64: String?, nonceB64: String?) {
        if (!envelopeB64.isNullOrBlank() && !nonceB64.isNullOrBlank()) {
            val salt = Base64.decode(kdfSaltB64, Base64.DEFAULT)
            val wrappingKey = deriveWrappingKey(password, salt)
            val dataKey = decrypt(
                wrappingKey,
                Base64.decode(envelopeB64, Base64.DEFAULT),
                Base64.decode(nonceB64, Base64.DEFAULT)
            )
            require(dataKey.size == 32) { "Invalid sync data key length." }
            storeDataKey(dataKey)
            return
        }

        // Existing accounts created before client-side envelopes were introduced
        // can still use the same device. Cross-device recovery for such accounts
        // must be completed through the registration/password-reset flow.
        if (getDataKey() == null) {
            val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }
            storeDataKey(generated)
        }
    }

    fun encrypt(plaintext: ByteArray): EncryptedPayload {
        val key = getDataKey() ?: error("Sync data key is not initialized.")
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return EncryptedPayload(cipher.doFinal(plaintext), nonce)
    }

    fun decrypt(ciphertext: ByteArray, nonce: ByteArray): ByteArray {
        val key = getDataKey() ?: error("Sync data key is not initialized.")
        return decrypt(SecretKeySpec(key, "AES"), ciphertext, nonce)
    }

    fun clear() {
        prefs.edit().remove(KEY_DATA_KEY).apply()
    }

    private fun storeDataKey(key: ByteArray) {
        prefs.edit()
            .putString(KEY_DATA_KEY, Base64.encodeToString(key, Base64.NO_WRAP))
            .apply()
    }

    private fun getDataKey(): ByteArray? = prefs.getString(KEY_DATA_KEY, null)?.let {
        Base64.decode(it, Base64.NO_WRAP)
    }

    private fun deriveWrappingKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, 256)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .encoded,
                "AES"
            )
        } finally {
            spec.clearPassword()
        }
    }

    private fun decrypt(key: SecretKey, ciphertext: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        return cipher.doFinal(ciphertext)
    }

    data class EncryptedPayload(val ciphertext: ByteArray, val nonce: ByteArray)

    data class RegistrationMaterial(
        val kdfSalt: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String
    )

    fun currentKeyVersion(): Int = CURRENT_KEY_VERSION

    companion object {
        private const val CURRENT_KEY_VERSION = 1
        private const val PREFS_NAME = "budget_sync_crypto"
        private const val KEY_DATA_KEY = "data_key_b64"
        private const val PBKDF2_ITERATIONS = 120_000
    }
}