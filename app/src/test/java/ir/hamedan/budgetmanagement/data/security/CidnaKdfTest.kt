package ir.hamedan.budgetmanagement.data.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Base64

class CidnaKdfTest {
    private val salt = ByteArray(16) { it.toByte() }
    private val recovery = ByteArray(32) { (it + 32).toByte() }

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    // Vectors produced independently with Python (hashlib/hmac); the Go twin asserts the same ones.
    @Test fun masterKeyAndDerivedSecretsMatchReferenceVectors() {
        val master = CidnaKdf.deriveMasterKey("correct horse battery staple".toCharArray(), salt)
        assertThat(hex(master)).isEqualTo("ef177144eec9420cbc1093d2a8b344a92bc506d0d4ec9c028dd19f8324d8c1e6")
        assertThat(CidnaKdf.authSecret(master)).isEqualTo("dDkCvX0jTBYzaVoxNjQPRh7k7YYnyd4jbM0Jg05nHOE=")
        assertThat(hex(CidnaKdf.passwordWrapKey(master)))
            .isEqualTo("a54c3fa5cac75136360d7f2bb0354b780251b27ae35065317b68f1f03feeb51b")
    }

    @Test fun recoveryDerivationMatchesReferenceVectors() {
        assertThat(CidnaKdf.encodeRecoveryKey(recovery)).isEqualTo("ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8=")
        assertThat(hex(CidnaKdf.recoveryWrapKey(recovery)))
            .isEqualTo("f9a2d833c5f93e2bc5b8dd984f528e5748ba105ccb39c1bd98190e875dc4f436")
        assertThat(CidnaKdf.recoveryProof(recovery)).isEqualTo("0j7trEP4Yn8Eq97HkhJY17I8KZJvwlA/UWMr6X7iNp0=")
        assertThat(Base64.getEncoder().encodeToString(CidnaKdf.recoveryKeyHash(recovery)))
            .isEqualTo("7F5vHTQGEXb8HfjmYR43WIsscYKILvfpCWqQjPg9IrE=")
    }

    @Test fun authSecretIsIndependentOfTheWrapKey() {
        val master = CidnaKdf.deriveMasterKey("pässwörd-123".toCharArray(), salt)
        assertThat(Base64.getDecoder().decode(CidnaKdf.authSecret(master)))
            .isNotEqualTo(CidnaKdf.passwordWrapKey(master))
    }

    @Test fun envelopeRoundTripAndPurposeBinding() {
        val dataKey = CidnaKdf.randomBytes(32)
        val key = CidnaKdf.randomBytes(32)
        val sealed = CidnaKdf.sealPasswordEnvelope(key, dataKey)
        assertThat(sealed.ciphertext.size).isEqualTo(48)
        assertThat(CidnaKdf.openPasswordEnvelope(key, sealed.ciphertext, sealed.nonce)).isEqualTo(dataKey)
        val swapped = runCatching { CidnaKdf.openRecoveryEnvelope(key, sealed.ciphertext, sealed.nonce) }
        assertThat(swapped.isFailure).isTrue()
    }

    @Test fun recoveryKeyParsingRejectsWrongLengths() {
        assertThat(CidnaKdf.decodeRecoveryKey(" ICEiIyQlJicoKSorLC0uLzAxMjM0\nNTY3ODk6Ozw9Pj8= ")).isEqualTo(recovery)
        assertThat(CidnaKdf.decodeRecoveryKey("short")).isNull()
    }
}