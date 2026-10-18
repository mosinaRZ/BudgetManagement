package ir.hamedan.budgetmanagement.utils

import ir.hamedan.budgetmanagement.data.network.ApiException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMessagesTest {

    private val persianLetters = Regex("[\\u0600-\\u06FF]")

    @Test
    fun rawExceptionTextIsNeverShown() {
        val error = IllegalStateException("java.lang.NullPointerException at Foo.bar(Foo.kt:42)")
        assertFalse(ErrorMessages.of(error, isPersian = false).contains("NullPointer"))
        assertFalse(ErrorMessages.of(error, isPersian = true).contains("NullPointer"))
    }

    @Test
    fun persianAndEnglishMessagesUseTheirOwnLanguage() {
        val errors = listOf(
            SocketTimeoutException("timeout"),
            UnknownHostException("host"),
            IOException("disk"),
            RuntimeException("boom")
        )
        errors.forEach { e ->
            assertTrue(persianLetters.containsMatchIn(ErrorMessages.of(e, isPersian = true)))
            assertFalse(persianLetters.containsMatchIn(ErrorMessages.of(e, isPersian = false)))
        }
    }

    @Test
    fun networkFailuresShareOneMessage() {
        assertEquals(
            ErrorMessages.of(SocketTimeoutException(), false),
            ErrorMessages.of(UnknownHostException(), false)
        )
    }

    @Test
    fun apiValidationErrorDoesNotLeakServerText() {
        val e = ApiException(400, "VALIDATION_ERROR", "validation failed")
        assertFalse(ErrorMessages.of(e, false).contains("validation failed"))
        assertFalse(persianLetters.containsMatchIn(ErrorMessages.of(e, false)))
        assertTrue(persianLetters.containsMatchIn(ErrorMessages.of(e, true)))
    }

    @Test
    fun nullAndUnknownCodesFallBackToGenericMessage() {
        assertEquals(ErrorMessages.generic(false), ErrorMessages.of(null, false))
        val unknown = ApiException(418, "SOMETHING_NEW", "teapot")
        assertNotEquals("teapot", ErrorMessages.of(unknown, false))
    }
}