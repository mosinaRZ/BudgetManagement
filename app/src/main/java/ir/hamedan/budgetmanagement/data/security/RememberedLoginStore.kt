package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * Local-only login material.
 *
 * Access/refresh tokens are deliberately not handled here. The password is never stored
 * in any form that can be reversed: local password re-authentication uses a salted PBKDF2
 * verifier, and the biometric credential encrypts a random, meaningless unlock token under
 * a hardware-backed key that requires a fresh strong-biometric authentication per use.
 */
class RememberedLoginStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveIdentifier(identifier: String) {
        identifier.trim().takeIf { it.isNotBlank() }?.let {
            prefs.edit().putString(KEY_IDENTIFIER, it).apply()
        }
    }

    fun identifier(): String? = prefs.getString(KEY_IDENTIFIER, null)?.takeIf { it.isNotBlank() }

    fun savePasswordVerifier(password: String) {
        if (password.isBlank()) return
        runCatching {
            val salt = ByteArray(PBKDF2_SALT_BYTES).also(SecureRandom()::nextBytes)
            val verifier = derive(password, salt, PBKDF2_ITERATIONS)
            prefs.edit()
                .putString(KEY_PASSWORD_VERIFIER, Base64.encodeToString(verifier, Base64.NO_WRAP))
                .putString(KEY_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putInt(KEY_PASSWORD_ITERATIONS, PBKDF2_ITERATIONS)
                .apply()
        }
    }

    fun hasPasswordVerifier(): Boolean =
        !prefs.getString(KEY_PASSWORD_VERIFIER, null).isNullOrBlank() &&
                !prefs.getString(KEY_PASSWORD_SALT, null).isNullOrBlank()

    fun verifyPassword(password: String): Boolean = runCatching {
        if (!hasPasswordVerifier()) return false
        val salt = Base64.decode(prefs.getString(KEY_PASSWORD_SALT, null), Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString(KEY_PASSWORD_VERIFIER, null), Base64.NO_WRAP)
        // Verifiers written by older builds carry no iteration count and used 120 000.
        val iterations = prefs.getInt(KEY_PASSWORD_ITERATIONS, LEGACY_PBKDF2_ITERATIONS)
        val actual = derive(password, salt, iterations)
        java.security.MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)

    fun canUseStrongBiometric(): Boolean =
        BiometricManager.from(appContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    fun hasBiometricCredential(): Boolean =
        prefs.contains(KEY_PASSWORD_CIPHERTEXT) && prefs.contains(KEY_PASSWORD_IV) && keyExists()

    fun prepareEncryptionCipher(): Cipher? {
        if (!canUseStrongBiometric()) return null
        return runCatching {
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        }.getOrNull()
    }

    fun prepareDecryptionCipher(): Cipher? {
        if (!hasBiometricCredential()) return null
        return runCatching {
            val iv = Base64.decode(prefs.getString(KEY_PASSWORD_IV, null), Base64.NO_WRAP)
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
        }.getOrElse {
            clearBiometricCredential()
            null
        }
    }

    /**
     * Enrols the biometric credential with a fresh random unlock token. Nothing derived from
     * the account password is stored: the token only proves that the hardware key was used
     * after a successful strong-biometric authentication.
     */
    fun enrollBiometric(cipher: Cipher) {
        runCatching {
            val token = ByteArray(UNLOCK_TOKEN_BYTES).also(SecureRandom()::nextBytes)
            val ciphertext = cipher.doFinal(token)
            token.fill(0)
            prefs.edit()
                .putString(KEY_PASSWORD_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_PASSWORD_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        }.onFailure { clearBiometricCredential() }
    }

    /**
     * Cryptographically confirms an authenticated biometric prompt: the unlock token can only
     * be decrypted if the Keystore key was actually released by the biometric hardware.
     */
    fun confirmBiometric(cipher: Cipher): Boolean = runCatching {
        val ciphertext = Base64.decode(prefs.getString(KEY_PASSWORD_CIPHERTEXT, null), Base64.NO_WRAP)
        val token = cipher.doFinal(ciphertext)
        val valid = token.size == UNLOCK_TOKEN_BYTES
        token.fill(0)
        valid
    }.getOrElse {
        clearBiometricCredential()
        false
    }

    fun clearBiometricCredential() {
        prefs.edit().remove(KEY_PASSWORD_CIPHERTEXT).remove(KEY_PASSWORD_IV).apply()
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.let {
                if (it.containsAlias(KEY_ALIAS)) it.deleteEntry(KEY_ALIAS)
            }
        }
    }

    fun clearLocalReauthentication() {
        prefs.edit()
            .remove(KEY_PASSWORD_VERIFIER)
            .remove(KEY_PASSWORD_SALT)
            .remove(KEY_PASSWORD_ITERATIONS)
            .remove(KEY_IDENTIFIER)
            .apply()
        clearBiometricCredential()
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, PBKDF2_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun keyExists(): Boolean = runCatching {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.containsAlias(KEY_ALIAS)
    }.getOrDefault(false)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(0)
        }

        generator.init(builder.build())
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "budget_remembered_login"
        private const val KEY_IDENTIFIER = "last_login_identifier"
        private const val KEY_PASSWORD_VERIFIER = "local_password_verifier"
        private const val KEY_PASSWORD_SALT = "local_password_salt"
        private const val KEY_PASSWORD_ITERATIONS = "local_password_iterations"
        // Storage keys keep their historical names so existing installs migrate cleanly; the
        // value is an unlock token, never a password.
        private const val KEY_PASSWORD_CIPHERTEXT = "biometric_password_ciphertext"
        private const val KEY_PASSWORD_IV = "biometric_password_iv"
        private const val KEY_ALIAS = "budget_biometric_login_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 210_000
        private const val LEGACY_PBKDF2_ITERATIONS = 120_000
        private const val UNLOCK_TOKEN_BYTES = 32
        private const val PBKDF2_KEY_BITS = 256
        private const val PBKDF2_SALT_BYTES = 16
    }
}