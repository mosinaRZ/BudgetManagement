package ir.hamedan.budgetmanagement.data.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
    private val password = "correct horse battery".toCharArray()
    private val plain = "داده‌ی محرمانه / secret data".toByteArray()

    // A low iteration count keeps the tests fast; the format accepts anything in its valid range.
    private fun seal(data: ByteArray = plain, pw: CharArray = password) = BackupCrypto.encrypt(data, pw, 100_000)

    @Test
    fun roundTrip() {
        assertThat(BackupCrypto.decrypt(seal(), password)).isEqualTo(plain)
    }

    @Test
    fun sameInputGivesDifferentFiles() {
        assertThat(seal()).isNotEqualTo(seal())
    }

    @Test
    fun wrongPasswordIsRejected() {
        assertThrows(BackupPasswordException::class.java) {
            BackupCrypto.decrypt(seal(), "another password".toCharArray())
        }
    }

    @Test
    fun anyModifiedByteIsRejected() {
        val blob = seal()
        for (index in listOf(10, 13, 20, 40, blob.size - 1)) {
            val tampered = blob.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertThrows("byte $index", Exception::class.java) { BackupCrypto.decrypt(tampered, password) }
        }
    }

    @Test
    fun foreignAndTruncatedFilesAreRecognisedAsNotBackups() {
        assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt(ByteArray(200), password) }
        assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt(seal().copyOf(10), password) }
    }

    @Test
    fun shortPasswordIsRefusedWhenCreatingABackup() {
        assertThrows(IllegalArgumentException::class.java) { seal(pw = "short".toCharArray()) }
    }
}