package ir.hamedan.budgetmanagement.data.security

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.SecureRandom

/**
 * Durable storage for the SQLCipher passphrase.
 *
 * Guarantees:
 *  1. **Durability before use.** A new passphrase is wrapped by an Android Keystore key, the wrapped
 *     blob is verified by a decrypt round-trip and written with a synchronous `commit()`; the
 *     passphrase is only returned (and therefore only ever used to create the database) after that
 *     write succeeded. A process kill right after the first launch can no longer leave a database
 *     behind whose key was never saved.
 *  2. **Never destroy data on a transient fault.** Keystore hiccups are retried, and if they persist a
 *     [DatabaseKeyUnavailableException] is thrown. Data is only considered lost on definitive
 *     evidence (key gone/invalidated, authentication failure, malformed blob).
 *  3. **Explicit, transactional recovery.** When the key is definitively lost, the existing database is
 *     unreadable. The caller decides what to do via `onKeyReset`; it runs *before* the replacement key
 *     is committed, so a crash at any point leaves a state that the next launch resolves again.
 *  4. **No dependency on the deprecated security-crypto**: only the platform Keystore is used.
 *
 * Use [DatabaseKeyProvider] in app code; this class takes its names as parameters so tests can run
 * against an isolated store without touching the real database key.
 */
internal class DatabaseKeyStore(
    context: Context,
    private val prefsName: String,
    private val keystoreAlias: String,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
) {
    private val appContext = context.applicationContext
    private val lock = Any()
    private val box = KeystoreSecretBox(keystoreAlias)

    // Binds the blob to this store, so it cannot be replayed from another entry.
    private val aad = "$prefsName/$KEY_WRAPPED_PASSPHRASE".toByteArray(Charsets.UTF_8)

    private val prefs get() = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /**
     * Returns a fresh copy of the passphrase on every call (SQLCipher may zero the array it is given).
     *
     * @param onKeyReset invoked when no usable key exists and a new one is about to be committed, i.e.
     * on first run and after a definitive key loss. It must delete any database that was encrypted
     * with an earlier key. If it throws, nothing is committed. When `null` and the key was lost,
     * [DatabaseKeyLostException] is thrown instead of silently replacing it.
     */
    fun getPassphrase(onKeyReset: (() -> Unit)?): ByteArray =
        synchronized(lock) { getPassphraseLocked(onKeyReset) }

    private fun getPassphraseLocked(onKeyReset: (() -> Unit)?): ByteArray {
        val stored = prefs.getString(KEY_WRAPPED_PASSPHRASE, null)
        if (stored != null) {
            try {
                return unwrap(stored)
            } catch (e: DatabaseKeyLostException) {
                if (onKeyReset == null) throw e
                Log.w(TAG, "Stored database key is unrecoverable; replacing it", e)
                return installNewKey(onKeyReset, keyWasLost = true)
            }
        }

        // Nothing stored: first run, or the key was never persisted (e.g. the process died first).
        return installNewKey(onKeyReset, keyWasLost = false)
    }

    /** `true` exactly once after a lost key was replaced, so the UI can tell the user. */
    fun consumeKeyResetNotice(): Boolean = synchronized(lock) {
        val current = prefs
        val pending = current.getBoolean(KEY_RESET_NOTICE, false)
        if (pending) current.edit().remove(KEY_RESET_NOTICE).commit()
        pending
    }

    /** Removes every trace of this store (blob, Keystore key, notice). */
    fun discardAll() {
        synchronized(lock) {
            prefs.edit().clear().commit()
            runCatching { box.deleteKey() }
        }
    }

    // ---------------------------------------------------------------------------------------------

    private fun installNewKey(onKeyReset: (() -> Unit)?, keyWasLost: Boolean): ByteArray {
        val fresh = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }

        // 1) Get rid of anything the previous key protected. Throwing here aborts before any commit.
        onKeyReset?.invoke()

        // 2) Drop the old, possibly broken material so the replacement starts from a clean slate.
        prefs.edit().remove(KEY_WRAPPED_PASSPHRASE).commit()
        runCatching { box.deleteKey() }

        // 3) Wrap, verify and commit the new key.
        persist(fresh)
        if (keyWasLost) prefs.edit().putBoolean(KEY_RESET_NOTICE, true).commit()
        return fresh
    }

    private fun persist(passphrase: ByteArray) {
        val blob = try {
            val wrapped = withRetry("wrap") { box.encrypt(passphrase, aad) }
            // Some devices hand out Keystore keys that encrypt but cannot decrypt. Find out now,
            // while nothing depends on this key yet.
            val roundTrip = withRetry("verify") { box.decrypt(wrapped, aad) }
            if (!roundTrip.contentEquals(passphrase)) error("Keystore round-trip mismatch")
            wrapped
        } catch (e: Exception) {
            throw DatabaseKeyUnavailableException("Cannot protect the database key with the Android Keystore", e)
        }

        val committed = prefs.edit()
            .putString(KEY_WRAPPED_PASSPHRASE, Base64.encodeToString(blob, Base64.NO_WRAP))
            .commit() // synchronous on purpose; apply() may lose the write if the process dies.
        if (!committed) throw DatabaseKeyUnavailableException("Failed to write the database key to disk")
    }

    private fun unwrap(stored: String): ByteArray {
        val blob = try {
            Base64.decode(stored, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw DatabaseKeyLostException("Stored database key is corrupt", e)
        }

        val bytes = try {
            withRetry("unwrap") { box.decrypt(blob, aad) }
        } catch (e: Exception) {
            if (e.isPermanentKeyFailure()) {
                throw DatabaseKeyLostException("Database key can no longer be decrypted", e)
            }
            throw DatabaseKeyUnavailableException("Android Keystore is not available right now", e)
        }
        if (bytes.size != PASSPHRASE_BYTES) {
            bytes.fill(0)
            throw DatabaseKeyLostException("Stored database key has an unexpected size")
        }
        return bytes
    }

    private fun Throwable.isPermanentKeyFailure(): Boolean =
        generateSequence(this) { it.cause }.any { it is KeystoreSecretBox.PermanentFailure }

    /** Retries Keystore operations: some OEM implementations fail sporadically and then recover. */
    private fun <T> withRetry(operation: String, block: () -> T): T {
        var last: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                last = e
                Log.w(TAG, "Keystore $operation failed (attempt $attempt/$maxAttempts)", e)
                if (attempt < maxAttempts && retryDelayMs > 0) {
                    try {
                        Thread.sleep(retryDelayMs * attempt)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }
        throw last ?: IllegalStateException("Keystore $operation failed")
    }

    internal companion object {
        private const val TAG = "DatabaseKeyStore"
        internal const val KEY_WRAPPED_PASSPHRASE = "wrapped_passphrase"
        private const val KEY_RESET_NOTICE = "key_reset_notice"
        private const val PASSPHRASE_BYTES = 32
        private const val DEFAULT_MAX_ATTEMPTS = 3
        private const val DEFAULT_RETRY_DELAY_MS = 150L
    }
}