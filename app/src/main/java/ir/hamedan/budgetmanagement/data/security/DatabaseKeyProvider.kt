package ir.hamedan.budgetmanagement.data.security

import android.content.Context

/** Base type of every failure to obtain the database passphrase. */
sealed class DatabaseKeyException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * The passphrase is gone for good (Keystore key deleted/invalidated, corrupted or tampered
 * storage). Any existing database encrypted with it can no longer be opened.
 */
class DatabaseKeyLostException(message: String, cause: Throwable? = null) :
    DatabaseKeyException(message, cause)

/**
 * The passphrase could not be obtained *right now* (Keystore malfunction, disk write failure).
 * Nothing is considered lost: retrying later, e.g. on the next launch, is expected to work, and no
 * data is ever deleted because of this error.
 */
class DatabaseKeyUnavailableException(message: String, cause: Throwable? = null) :
    DatabaseKeyException(message, cause)

/**
 * Supplies the SQLCipher passphrase of the app database.
 *
 * The 256-bit random passphrase is wrapped with an AES-GCM key that lives in the Android Keystore;
 * only the wrapped blob is persisted (see [DatabaseKeyStore] for the durability and recovery rules).
 */
object DatabaseKeyProvider {

    private const val PREFS_NAME = "db_key_store_v2"
    private const val KEYSTORE_ALIAS = "budget_db_key_v2"

    @Volatile
    private var store: DatabaseKeyStore? = null

    private fun store(context: Context): DatabaseKeyStore =
        store ?: synchronized(this) {
            store ?: DatabaseKeyStore(
                context = context,
                prefsName = PREFS_NAME,
                keystoreAlias = KEYSTORE_ALIAS
            ).also { store = it }
        }

    /**
     * Returns the database passphrase, creating and durably persisting it on first use.
     *
     * Each call returns a new array the caller owns (SQLCipher may zero it after opening).
     *
     * @param onKeyReset called right before a *new* key is committed (first run, or after a definitive
     * loss of the old one). Use it to delete a database that the new key cannot open. If it throws,
     * no key is committed. When `null` and the key was lost, [DatabaseKeyLostException] is thrown
     * instead of silently replacing the key.
     * @throws DatabaseKeyLostException the key is permanently lost and no [onKeyReset] was supplied.
     * @throws DatabaseKeyUnavailableException the Keystore/disk failed; safe to retry later.
     */
    fun getPassphrase(context: Context, onKeyReset: (() -> Unit)? = null): ByteArray =
        store(context).getPassphrase(onKeyReset)

    /**
     * Returns `true` once after a lost key was replaced (which also means the local database was
     * reset), so the UI can inform the user that data is being restored from the server.
     */
    fun consumeKeyResetNotice(context: Context): Boolean =
        store(context).consumeKeyResetNotice()
}