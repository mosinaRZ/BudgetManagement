package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client-side data-key management for encrypted sync payloads (end-to-end encryption).
 *
 * The 256-bit data key is generated on the device and only ever leaves it wrapped
 * (see [CidnaKdf]). The server receives an authentication secret that is cryptographically
 * independent from the wrapping key, never the password itself, so it can neither read
 * nor unwrap the data key.
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

    /** Derives the account master key; the caller must wipe it with [wipe] when done. */
    fun deriveMasterKey(password: CharArray, kdfSaltB64: String): ByteArray =
        CidnaKdf.deriveMasterKey(password, CidnaKdf.decodeB64(kdfSaltB64))

    fun authSecret(masterKey: ByteArray): String = CidnaKdf.authSecret(masterKey)

    fun createRegistrationMaterial(password: CharArray): RegistrationMaterial {
        val saltBytes = CidnaKdf.randomBytes(CidnaKdf.SALT_BYTES)
        val dataKey = CidnaKdf.randomBytes(CidnaKdf.KEY_BYTES)
        val masterKey = CidnaKdf.deriveMasterKey(password, saltBytes)
        val passwordWrapKey = CidnaKdf.passwordWrapKey(masterKey)
        val passwordEnvelope = CidnaKdf.sealPasswordEnvelope(passwordWrapKey, dataKey)

        val recoveryBytes = CidnaKdf.randomBytes(CidnaKdf.KEY_BYTES)
        val recoveryEnvelope = CidnaKdf.sealRecoveryEnvelope(CidnaKdf.recoveryWrapKey(recoveryBytes), dataKey)
        val material = RegistrationMaterial(
            kdfSalt = CidnaKdf.encodeB64(saltBytes),
            masterKey = masterKey,
            authSecret = CidnaKdf.authSecret(masterKey),
            passwordKeyEnvelope = CidnaKdf.encodeB64(passwordEnvelope.ciphertext),
            passwordKeyNonce = CidnaKdf.encodeB64(passwordEnvelope.nonce),
            recoveryKey = CidnaKdf.encodeRecoveryKey(recoveryBytes),
            recoveryKeyHash = CidnaKdf.encodeB64(CidnaKdf.recoveryKeyHash(recoveryBytes)),
            recoveryKeyEnvelope = CidnaKdf.encodeB64(recoveryEnvelope.ciphertext),
            recoveryKeyNonce = CidnaKdf.encodeB64(recoveryEnvelope.nonce)
        )
        storeDataKey(dataKey)
        wipe(passwordWrapKey, recoveryBytes, dataKey)
        return material
    }

    /** Unwraps and stores the account data key received at sign-in. */
    fun initializeFromLogin(masterKey: ByteArray, envelopeB64: String?, nonceB64: String?) {
        require(!envelopeB64.isNullOrBlank() && !nonceB64.isNullOrBlank()) { "Account key envelope is missing." }
        val wrapKey = CidnaKdf.passwordWrapKey(masterKey)
        try {
            val dataKey = CidnaKdf.openPasswordEnvelope(wrapKey, CidnaKdf.decodeB64(envelopeB64), CidnaKdf.decodeB64(nonceB64))
            require(dataKey.size == CidnaKdf.KEY_BYTES) { "Invalid sync data key length." }
            storeDataKey(dataKey)
            wipe(dataKey)
        } finally {
            wipe(wrapKey)
        }
    }

    /** Recovers the data key with the recovery key and re-wraps it under [newPassword]. */
    fun createPasswordResetMaterial(
        newPassword: CharArray,
        kdfSaltB64: String,
        recoveryKey: String,
        recoveryKeyEnvelopeB64: String,
        recoveryKeyNonceB64: String
    ): PasswordResetMaterial {
        val recoveryBytes = CidnaKdf.decodeRecoveryKey(recoveryKey) ?: throw InvalidRecoveryKeyException()
        val dataKey = try {
            CidnaKdf.openRecoveryEnvelope(
                CidnaKdf.recoveryWrapKey(recoveryBytes),
                CidnaKdf.decodeB64(recoveryKeyEnvelopeB64),
                CidnaKdf.decodeB64(recoveryKeyNonceB64)
            )
        } catch (e: javax.crypto.AEADBadTagException) {
            throw InvalidRecoveryKeyException()
        } finally {
            wipe(recoveryBytes)
        }
        require(dataKey.size == CidnaKdf.KEY_BYTES) { "Invalid recovered sync data key." }
        return wrapForPassword(dataKey, newPassword, kdfSaltB64)
    }

    /**
     * Re-wraps the data key this device already holds under [newPassword] for an in-session
     * password change. The account KDF salt stays unchanged (the server rejects another one).
     */
    fun createPasswordChangeMaterial(newPassword: CharArray, kdfSaltB64: String): PasswordResetMaterial {
        val dataKey = getDataKey() ?: error("Sync data key is not initialized.")
        require(dataKey.size == CidnaKdf.KEY_BYTES) { "Invalid sync data key length." }
        return wrapForPassword(dataKey, newPassword, kdfSaltB64)
    }

    private fun wrapForPassword(dataKey: ByteArray, password: CharArray, kdfSaltB64: String): PasswordResetMaterial {
        val masterKey = deriveMasterKey(password, kdfSaltB64)
        val wrapKey = CidnaKdf.passwordWrapKey(masterKey)
        val envelope = CidnaKdf.sealPasswordEnvelope(wrapKey, dataKey)
        wipe(wrapKey)
        return PasswordResetMaterial(
            dataKey = dataKey,
            masterKey = masterKey,
            authSecret = CidnaKdf.authSecret(masterKey),
            kdfSalt = CidnaKdf.encodeB64(CidnaKdf.decodeB64(kdfSaltB64)),
            passwordKeyEnvelope = CidnaKdf.encodeB64(envelope.ciphertext),
            passwordKeyNonce = CidnaKdf.encodeB64(envelope.nonce)
        )
    }

    fun storeRecoveredDataKey(dataKey: ByteArray) {
        require(dataKey.size == CidnaKdf.KEY_BYTES) { "Invalid sync data key length." }
        storeDataKey(dataKey.copyOf())
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
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ciphertext)
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

    class InvalidRecoveryKeyException : IllegalArgumentException("Recovery key is invalid.")

    data class EncryptedPayload(val ciphertext: ByteArray, val nonce: ByteArray)

    /** [masterKey]/[authSecret] are local-only derivations; the caller wipes [masterKey]. */
    class RegistrationMaterial(
        val kdfSalt: String,
        val masterKey: ByteArray,
        val authSecret: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String,
        val recoveryKey: String,
        val recoveryKeyHash: String,
        val recoveryKeyEnvelope: String,
        val recoveryKeyNonce: String
    )

    class PasswordResetMaterial(
        val dataKey: ByteArray,
        val masterKey: ByteArray,
        val authSecret: String,
        val kdfSalt: String,
        val passwordKeyEnvelope: String,
        val passwordKeyNonce: String
    )

    fun currentKeyVersion(): Int = CURRENT_KEY_VERSION

    companion object {
        private const val CURRENT_KEY_VERSION = 1
        private const val PREFS_NAME = "budget_sync_crypto"
        private const val KEY_DATA_KEY = "data_key_b64"

        fun wipe(vararg arrays: ByteArray) = arrays.forEach { it.fill(0) }
    }
}