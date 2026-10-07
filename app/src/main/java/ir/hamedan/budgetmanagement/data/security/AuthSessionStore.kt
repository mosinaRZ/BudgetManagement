package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Secure at-rest storage for authentication/session material. */
class AuthSessionStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _authenticated = MutableStateFlow(readAuthenticated())
    val authenticated: StateFlow<Boolean> = _authenticated.asStateFlow()

    /** Why the last session ended. */
    enum class SessionEnd { NONE, EXPIRED, DEVICE_REMOVED }

    /**
     * Set when the session was ended by the server side (expired/revoked refresh token, or this
     * device was removed from the account) rather than by the user signing out. In-memory only:
     * it exists so the login screen can explain why the user was sent back to it.
     */
    @Volatile
    private var sessionEnd = SessionEnd.NONE

    /** Returns why the last session ended (once), then resets it. */
    fun consumeEnd(): SessionEnd {
        val value = sessionEnd
        sessionEnd = SessionEnd.NONE
        return value
    }

    /** Returns true once if the last session end was unexpected, then resets the flag. */
    fun consumeUnexpectedEnd(): Boolean = consumeEnd() != SessionEnd.NONE

    fun save(
        accessToken: String,
        refreshToken: String,
        userId: String,
        deviceId: String,
        kdfSalt: String,
        passwordKeyEnvelope: String,
        passwordKeyNonce: String,
        recoveryKeyEnvelope: String,
        recoveryKeyNonce: String,
        role: String? = null
    ) {
        sessionEnd = SessionEnd.NONE
        check(prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_KDF_SALT, kdfSalt)
            .putString(KEY_PASSWORD_KEY_ENVELOPE, passwordKeyEnvelope)
            .putString(KEY_PASSWORD_KEY_NONCE, passwordKeyNonce)
            .putString(KEY_RECOVERY_KEY_ENVELOPE, recoveryKeyEnvelope)
            .putString(KEY_RECOVERY_KEY_NONCE, recoveryKeyNonce)
            .putString(KEY_ROLE, role?.takeIf { it.isNotBlank() })
            .commit()) { "Unable to persist authentication session." }
        _authenticated.value = true
    }

    fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun userId(): String? = prefs.getString(KEY_USER_ID, null)
    fun deviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)
    fun kdfSalt(): String? = prefs.getString(KEY_KDF_SALT, null)
    fun passwordKeyEnvelope(): String? = prefs.getString(KEY_PASSWORD_KEY_ENVELOPE, null)
    fun passwordKeyNonce(): String? = prefs.getString(KEY_PASSWORD_KEY_NONCE, null)
    fun recoveryKeyEnvelope(): String? = prefs.getString(KEY_RECOVERY_KEY_ENVELOPE, null)
    fun recoveryKeyNonce(): String? = prefs.getString(KEY_RECOVERY_KEY_NONCE, null)
    fun role(): String? = prefs.getString(KEY_ROLE, null)

    fun isAuthenticated(): Boolean = readAuthenticated()

    /** Clears only credentials. Local encrypted financial data is retained for re-authentication. */
    fun clearSession(unexpected: Boolean = false, deviceRemoved: Boolean = false) {
        // A more specific reason is never downgraded by a later generic "unexpected" clear
        // (several layers may report the same 401).
        sessionEnd = when {
            deviceRemoved -> SessionEnd.DEVICE_REMOVED
            unexpected -> if (sessionEnd == SessionEnd.DEVICE_REMOVED) SessionEnd.DEVICE_REMOVED else SessionEnd.EXPIRED
            else -> SessionEnd.NONE
        }
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_USER_ID)
            .remove(KEY_KDF_SALT)
            .remove(KEY_PASSWORD_KEY_ENVELOPE)
            .remove(KEY_PASSWORD_KEY_NONCE)
            .remove(KEY_RECOVERY_KEY_ENVELOPE)
            .remove(KEY_RECOVERY_KEY_NONCE)
            .remove(KEY_ROLE)
            .commit()
        _authenticated.value = false
    }

    fun clear() = clearSession()

    private fun readAuthenticated(): Boolean =
        !prefs.getString(KEY_ACCESS, null).isNullOrBlank() &&
                !prefs.getString(KEY_REFRESH, null).isNullOrBlank() &&
                !prefs.getString(KEY_USER_ID, null).isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "budget_auth_session"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_KDF_SALT = "kdf_salt"
        private const val KEY_PASSWORD_KEY_ENVELOPE = "password_key_envelope"
        private const val KEY_PASSWORD_KEY_NONCE = "password_key_nonce"
        private const val KEY_RECOVERY_KEY_ENVELOPE = "recovery_key_envelope"
        private const val KEY_RECOVERY_KEY_NONCE = "recovery_key_nonce"
        private const val KEY_ROLE = "role"
    }
}