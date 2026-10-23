package ir.hamedan.budgetmanagement.data.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cidna key schedule, protocol v2 ("the password never leaves the device").
 *
 *   masterKey  = PBKDF2-HMAC-SHA256(NFKC(password), kdfSalt, 600 000, 32 bytes)
 *   authSecret = HKDF-Expand(masterKey, "cidna/v2/authentication")   -> sent to the server
 *   wrapKey    = HKDF-Expand(masterKey, "cidna/v2/password-wrap")    -> NEVER leaves the device
 *
 * The server only ever receives [authSecret]. Because HKDF outputs under different `info`
 * labels are independent, [authSecret] reveals nothing about [passwordWrapKey], so the
 * server cannot unwrap the data key. Recovery follows the same pattern with a random
 * 256-bit recovery key (see [recoveryProof] / [recoveryWrapKey]).
 *
 * Pure JVM on purpose (java.util.Base64, no android.* classes) so it is unit-testable and
 * can be mirrored byte-for-byte by other clients. Keep in sync with the Go twin
 * `backend/internal/infrastructure/auth/e2ee_envelope.go`.
 */
object CidnaKdf {
    const val VERSION = 2
    const val PBKDF2_ITERATIONS = 600_000
    const val SALT_BYTES = 16
    const val KEY_BYTES = 32
    const val NONCE_BYTES = 12

    private const val INFO_AUTH = "cidna/v2/authentication"
    private const val INFO_PASSWORD_WRAP = "cidna/v2/password-wrap"
    private const val INFO_RECOVERY_WRAP = "cidna/v2/recovery-wrap"
    private const val INFO_RECOVERY_PROOF = "cidna/v2/recovery-proof"
    private const val AAD_PASSWORD_ENVELOPE = "cidna/v2/password-envelope"
    private const val AAD_RECOVERY_ENVELOPE = "cidna/v2/recovery-envelope"

    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    /** NFKC-normalises so the same password typed on different keyboards derives the same key. */
    fun deriveMasterKey(password: CharArray, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): ByteArray {
        require(salt.size == SALT_BYTES) { "Invalid KDF salt." }
        require(iterations >= PBKDF2_ITERATIONS) { "KDF work factor is below the minimum." }
        val normalized = Normalizer.normalize(String(password), Normalizer.Form.NFKC).toCharArray()
        val spec = PBEKeySpec(normalized, salt, iterations, KEY_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            normalized.fill('\u0000')
        }
    }

    /** HKDF-Expand (RFC 5869) for a single 32-byte block with HMAC-SHA256. */
    private fun expand(prk: ByteArray, info: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info.toByteArray(StandardCharsets.UTF_8))
        mac.update(0x01.toByte())
        return mac.doFinal()
    }

    fun authSecret(masterKey: ByteArray): String =
        Base64.getEncoder().encodeToString(expand(masterKey, INFO_AUTH))

    fun passwordWrapKey(masterKey: ByteArray): ByteArray = expand(masterKey, INFO_PASSWORD_WRAP)

    fun recoveryWrapKey(recoveryBytes: ByteArray): ByteArray = expand(recoveryBytes, INFO_RECOVERY_WRAP)

    /** Sent to the server when recovering; the server stores only its SHA-256 ([recoveryKeyHash]). */
    fun recoveryProof(recoveryBytes: ByteArray): String =
        Base64.getEncoder().encodeToString(expand(recoveryBytes, INFO_RECOVERY_PROOF))

    fun recoveryKeyHash(recoveryBytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(expand(recoveryBytes, INFO_RECOVERY_PROOF))

    fun encodeRecoveryKey(recoveryBytes: ByteArray): String = Base64.getEncoder().encodeToString(recoveryBytes)

    /** Parses a recovery key typed by the user; returns null unless it is exactly 32 bytes. */
    fun decodeRecoveryKey(text: String): ByteArray? = runCatching {
        Base64.getDecoder().decode(text.filterNot(Char::isWhitespace)).takeIf { it.size == KEY_BYTES }
    }.getOrNull()

    fun encodeB64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    /** Lenient on padding/whitespace (the server stores values exactly as clients sent them). */
    fun decodeB64(text: String): ByteArray {
        val clean = text.filterNot { it.isWhitespace() || it == '=' }
        return Base64.getDecoder().decode(clean + "=".repeat((4 - clean.length % 4) % 4))
    }

    class Sealed(val ciphertext: ByteArray, val nonce: ByteArray)

    fun sealPasswordEnvelope(wrapKey: ByteArray, dataKey: ByteArray) = seal(wrapKey, dataKey, AAD_PASSWORD_ENVELOPE)
    fun sealRecoveryEnvelope(wrapKey: ByteArray, dataKey: ByteArray) = seal(wrapKey, dataKey, AAD_RECOVERY_ENVELOPE)
    fun openPasswordEnvelope(wrapKey: ByteArray, ciphertext: ByteArray, nonce: ByteArray) =
        open(wrapKey, ciphertext, nonce, AAD_PASSWORD_ENVELOPE)
    fun openRecoveryEnvelope(wrapKey: ByteArray, ciphertext: ByteArray, nonce: ByteArray) =
        open(wrapKey, ciphertext, nonce, AAD_RECOVERY_ENVELOPE)

    private fun seal(key: ByteArray, plaintext: ByteArray, aad: String): Sealed {
        val nonce = randomBytes(NONCE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad.toByteArray(StandardCharsets.UTF_8))
        return Sealed(cipher.doFinal(plaintext), nonce)
    }

    private fun open(key: ByteArray, ciphertext: ByteArray, nonce: ByteArray, aad: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad.toByteArray(StandardCharsets.UTF_8))
        return cipher.doFinal(ciphertext)
    }
}