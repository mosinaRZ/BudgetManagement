package ir.hamedan.budgetmanagement.data.guest

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Local-only state of the guest workspace: whether it exists on this device, the optional guest
 * passcode, and the optional fingerprint unlock. Nothing in this class talks to the network.
 *
 * The guest session flag is in memory only. When the app starts again, a guest without any unlock
 * secret goes straight into the workspace; a guest with one is asked for it before entering.
 */
class GuestModeStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _session = MutableStateFlow(false)
    val session: StateFlow<Boolean> = _session.asStateFlow()

    val isSessionActive: Boolean get() = _session.value

    fun beginSession() {
        _session.value = true
    }

    fun endSession() {
        _session.value = false
    }

    // ---- Workspace -------------------------------------------------------------------------

    /** True when a guest workspace was created on this device and not deleted since. */
    fun hasWorkspace(): Boolean = prefs.getBoolean(KEY_WORKSPACE, false)

    /** True when the guest set a passcode or a fingerprint, i.e. entering the workspace must be verified. */
    fun hasUnlockSecret(): Boolean = hasPasscode() || isBiometricEnabled()

    fun setWorkspace(created: Boolean) {
        check(prefs.edit().putBoolean(KEY_WORKSPACE, created).commit()) { "Unable to save guest state." }
    }

    // ---- Choice made while creating an account ------------------------------------------------

    /**
     * Remembers what happens to the guest data once the account with [phone] is signed in: moved
     * into it, or deleted. Applied at that sign-in, after the credentials were verified.
     */
    fun savePendingRegistrationChoice(phone: String, discardGuestData: Boolean) {
        check(
            prefs.edit()
                .putString(KEY_PENDING_PHONE, phone)
                .putBoolean(KEY_PENDING_DISCARD, discardGuestData)
                .commit()
        ) { "Unable to save guest state." }
    }

    /** The choice made for [identifier] while its account was created, or null when there is none. */
    fun pendingRegistrationDiscard(identifier: String): Boolean? {
        val phone = prefs.getString(KEY_PENDING_PHONE, null) ?: return null
        return if (phone == identifier) prefs.getBoolean(KEY_PENDING_DISCARD, false) else null
    }

    fun clearPendingRegistrationChoice() {
        prefs.edit().remove(KEY_PENDING_PHONE).remove(KEY_PENDING_DISCARD).commit()
    }

    // ---- Passcode (optional, used instead of a password) -------------------------------------

    fun hasPasscode(): Boolean =
        !prefs.getString(KEY_PASSCODE_VERIFIER, null).isNullOrBlank() &&
                !prefs.getString(KEY_PASSCODE_SALT, null).isNullOrBlank()

    /** Stores a salted PBKDF2 verifier. The passcode itself is never stored. */
    fun savePasscode(passcode: String) {
        require(passcode.isNotEmpty()) { "Passcode must not be empty." }
        val salt = ByteArray(PBKDF2_SALT_BYTES).also(SecureRandom()::nextBytes)
        val verifier = derive(passcode, salt)
        check(
            prefs.edit()
                .putString(KEY_PASSCODE_VERIFIER, Base64.encodeToString(verifier, Base64.NO_WRAP))
                .putString(KEY_PASSCODE_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .commit()
        ) { "Unable to save guest passcode." }
    }

    fun verifyPasscode(passcode: String): Boolean = runCatching {
        if (!hasPasscode()) return@runCatching false
        val salt = Base64.decode(prefs.getString(KEY_PASSCODE_SALT, null), Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString(KEY_PASSCODE_VERIFIER, null), Base64.NO_WRAP)
        java.security.MessageDigest.isEqual(expected, derive(passcode, salt))
    }.getOrDefault(false)

    fun clearPasscode() {
        prefs.edit().remove(KEY_PASSCODE_VERIFIER).remove(KEY_PASSCODE_SALT).commit()
    }

    // ---- Fingerprint (optional) --------------------------------------------------------------

    /** Whether fingerprint unlock is switched on. Use [isBiometricReady] before showing the prompt. */
    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun canUseStrongBiometric(): Boolean =
        BiometricManager.from(appContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    /** Cipher to hand to the enrollment prompt. Null when strong biometrics are unavailable. */
    fun prepareEnrollCipher(): Cipher? {
        if (!canUseStrongBiometric()) return null
        return runCatching {
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        }.getOrNull()
    }

    /** Call with the cipher returned by the successful enrollment prompt. */
    fun completeBiometricEnrollment(cipher: Cipher): Boolean = runCatching {
        val ciphertext = cipher.doFinal(BIOMETRIC_MARKER.toByteArray(StandardCharsets.UTF_8))
        check(
            prefs.edit()
                .putString(KEY_BIOMETRIC_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_BIOMETRIC_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                .commit()
        )
        true
    }.getOrDefault(false)

    /**
     * Cipher to hand to the unlock prompt. Returns null when fingerprint unlock is not usable; if the
     * key was invalidated (fingerprints changed on the device), fingerprint unlock is switched off
     * so the guest falls back to the passcode.
     */
    fun prepareUnlockCipher(): Cipher? {
        if (!isBiometricEnabled()) return null
        return runCatching {
            val iv = Base64.decode(prefs.getString(KEY_BIOMETRIC_IV, null), Base64.NO_WRAP)
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, existingKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
        }.getOrElse {
            disableBiometric()
            null
        }
    }

    /** Call with the cipher from the successful unlock prompt. */
    fun completeBiometricUnlock(cipher: Cipher): Boolean = runCatching {
        val ciphertext = Base64.decode(prefs.getString(KEY_BIOMETRIC_CIPHERTEXT, null), Base64.NO_WRAP)
        String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8) == BIOMETRIC_MARKER
    }.getOrDefault(false)

    fun disableBiometric() {
        prefs.edit()
            .remove(KEY_BIOMETRIC_CIPHERTEXT)
            .remove(KEY_BIOMETRIC_IV)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .commit()
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    /** Removes every local secret of the guest workspace (passcode, fingerprint, workspace flag). */
    fun clearAllSecrets() {
        clearPasscode()
        disableBiometric()
        clearPendingRegistrationChoice()
        setWorkspace(false)
    }

    // ---- Internals ---------------------------------------------------------------------------

    private fun derive(passcode: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun existingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: error("Guest biometric key is missing.")
    }

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
        /** Fixed id of the guest's profile row. It never matches a server account id (UUID). */
        const val GUEST_USER_ID = "local-guest"

        private const val PREFS_NAME = "budget_guest_mode"
        private const val KEY_WORKSPACE = "workspace_created"
        private const val KEY_PENDING_PHONE = "pending_registration_phone"
        private const val KEY_PENDING_DISCARD = "pending_registration_discard"
        private const val KEY_PASSCODE_VERIFIER = "passcode_verifier"
        private const val KEY_PASSCODE_SALT = "passcode_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BIOMETRIC_CIPHERTEXT = "biometric_ciphertext"
        private const val KEY_BIOMETRIC_IV = "biometric_iv"
        private const val KEY_ALIAS = "budget_guest_biometric_key"
        private const val BIOMETRIC_MARKER = "budget-guest-biometric-v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_BITS = 256
        private const val PBKDF2_SALT_BYTES = 16
    }
}