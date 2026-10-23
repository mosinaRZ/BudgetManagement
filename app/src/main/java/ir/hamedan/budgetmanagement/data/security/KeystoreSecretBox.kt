package ir.hamedan.budgetmanagement.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Minimal AES-256-GCM "secret box" whose key lives in the Android Keystore and never leaves it.
 *
 * It does not use the deprecated `androidx.security:security-crypto`: only the platform
 * Keystore and `javax.crypto` are used, so there is no third-party layer that can break or be
 * discontinued.
 *
 * Blob layout: `[version:1][ivLength:1][iv][ciphertext+tag]`. Callers bind a blob to its purpose with
 * [aad] (additional authenticated data), so a blob cannot be swapped between different uses.
 *
 * Failures are split in two on purpose:
 *  - [PermanentFailure]: the blob or the Keystore key is definitively unusable (key gone,
 *    invalidated, malformed blob, authentication-tag mismatch). Retrying cannot help.
 *  - any other exception: possibly transient Keystore trouble (a common OEM quirk right after boot
 *    or while the system is under pressure). Callers must NOT treat those as data loss.
 */
internal class KeystoreSecretBox(private val alias: String) {

    /** The blob/key combination can never be decrypted again. */
    class PermanentFailure(message: String, cause: Throwable? = null) :
        GeneralSecurityException(message, cause)

    /** Encrypts [plaintext], creating the Keystore key on first use. */
    fun encrypt(plaintext: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Nothing is encrypted with this key yet (we are about to create a new blob), so it is
            // safe to drop the dead key and start over with a fresh one.
            deleteKey()
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        cipher.updateAAD(aad)
        val iv = cipher.iv
        check(iv.size in MIN_IV_BYTES..MAX_IV_BYTES) { "Unexpected GCM IV length: ${iv.size}" }
        val ciphertext = cipher.doFinal(plaintext)

        val out = ByteArray(HEADER_BYTES + iv.size + ciphertext.size)
        out[0] = FORMAT_VERSION
        out[1] = iv.size.toByte()
        System.arraycopy(iv, 0, out, HEADER_BYTES, iv.size)
        System.arraycopy(ciphertext, 0, out, HEADER_BYTES + iv.size, ciphertext.size)
        return out
    }

    /**
     * Decrypts a blob produced by [encrypt].
     *
     * @throws PermanentFailure when the blob is malformed, the key is missing/invalidated or the
     * authentication tag does not match.
     */
    fun decrypt(blob: ByteArray, aad: ByteArray): ByteArray {
        if (blob.size < HEADER_BYTES || blob[0] != FORMAT_VERSION) {
            throw PermanentFailure("Unsupported or truncated blob")
        }
        val ivLength = blob[1].toInt() and 0xFF
        val cipherOffset = HEADER_BYTES + ivLength
        if (ivLength < MIN_IV_BYTES || blob.size < cipherOffset + TAG_BYTES) {
            throw PermanentFailure("Malformed blob")
        }

        val key = loadKey() ?: throw PermanentFailure("Keystore key '$alias' is missing")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, HEADER_BYTES, ivLength))
            cipher.updateAAD(aad)
            cipher.doFinal(blob, cipherOffset, blob.size - cipherOffset)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw PermanentFailure("Keystore key '$alias' was invalidated", e)
        } catch (e: BadPaddingException) {
            // AEADBadTagException extends BadPaddingException: wrong key, wrong AAD or corrupt data.
            throw PermanentFailure("Authentication failed for key '$alias'", e)
        }
    }

    /** Removes the Keystore key (no-op if it does not exist). */
    fun deleteKey() {
        val keyStore = openKeyStore()
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun loadKey(): SecretKey? = openKeyStore().getKey(alias, null) as? SecretKey

    private fun getOrCreateKey(): SecretKey {
        loadKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun openKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val TAG_BITS = 128
        const val TAG_BYTES = TAG_BITS / 8
        const val FORMAT_VERSION: Byte = 1
        const val HEADER_BYTES = 2
        const val MIN_IV_BYTES = 12
        const val MAX_IV_BYTES = 255
    }
}