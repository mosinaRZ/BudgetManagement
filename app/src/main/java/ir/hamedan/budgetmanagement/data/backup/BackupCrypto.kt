package ir.hamedan.budgetmanagement.data.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The file is not a backup this app can read (wrong type, truncated, or a newer format). */
class BackupFormatException(message: String) : Exception(message)

/** Decryption failed: the password is wrong or the file was modified/corrupted. */
class BackupPasswordException : Exception("Wrong password or damaged backup file")

/**
 * Password-based encryption of a backup file. Pure `javax.crypto`: it has no Android dependency,
 * so it is covered by plain JVM unit tests.
 *
 * Layout: `MAGIC(8) | iterations(4, big endian) | salt(16) | iv(12) | AES-256-GCM(ciphertext+tag)`.
 * The whole header is authenticated (it is the GCM additional data), so changing the iteration
 * count or salt makes decryption fail instead of silently weakening it.
 *
 * The backup is independent of the Android Keystore on purpose: it must still open after the
 * Keystore key is gone (that is the situation it exists for), on another phone, after a reinstall.
 */
object BackupCrypto {
    private val MAGIC = "CIDNABK1".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    const val DEFAULT_ITERATIONS = 600_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 5_000_000
    private const val HEADER_BYTES = 8 + 4 + SALT_BYTES + IV_BYTES
    const val MIN_PASSWORD_LENGTH = 8

    private val random = SecureRandom()

    fun encrypt(plain: ByteArray, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) { "Backup password is too short" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Unsupported iteration count" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).putInt(iterations).put(salt).put(iv).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    /**
     * @throws BackupFormatException not one of our files.
     * @throws BackupPasswordException wrong password, or the file was altered.
     */
    fun decrypt(blob: ByteArray, password: CharArray): ByteArray {
        if (blob.size < HEADER_BYTES + TAG_BITS / 8) throw BackupFormatException("Backup file is too small")
        if (!blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw BackupFormatException("Not a Cidna backup file")
        val buffer = ByteBuffer.wrap(blob, MAGIC.size, HEADER_BYTES - MAGIC.size)
        val iterations = buffer.int
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupFormatException("Unsupported backup parameters")
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val iv = ByteArray(IV_BYTES).also(buffer::get)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(blob, 0, HEADER_BYTES)
            cipher.doFinal(blob, HEADER_BYTES, blob.size - HEADER_BYTES)
        } catch (e: AEADBadTagException) {
            throw BackupPasswordException()
        } catch (e: GeneralSecurityException) {
            throw BackupPasswordException()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}