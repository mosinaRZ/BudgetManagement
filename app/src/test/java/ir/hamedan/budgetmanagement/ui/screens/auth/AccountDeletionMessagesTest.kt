package ir.hamedan.budgetmanagement.ui.screens.auth

import com.google.common.truth.Truth.assertThat
import ir.hamedan.budgetmanagement.data.network.ApiException
import org.junit.Test

class AccountDeletionMessagesTest {

    @Test
    fun wrongPasswordWhenDeletingDoesNotTalkAboutTheCurrentPassword() {
        val error = ApiException(422, "INVALID_PASSWORD", "password is incorrect")
        assertThat(authErrorMessage(error, false, AuthContext.DELETE_ACCOUNT)).isEqualTo("The password is incorrect.")
        assertThat(authErrorMessage(error, false, AuthContext.CHANGE_PASSWORD)).isEqualTo("The current password is incorrect.")
    }

    @Test
    fun lockoutAndExpiredSessionAreExplainedForDeletion() {
        assertThat(authErrorMessage(ApiException(429, "RATE_LIMITED", "x"), false, AuthContext.DELETE_ACCOUNT))
            .contains("Too many failed attempts")
        assertThat(authErrorMessage(ApiException(401, "UNAUTHORIZED", "x"), false, AuthContext.DELETE_ACCOUNT))
            .contains("session has expired")
    }

    @Test
    fun deletedAccountHasItsOwnMessage() {
        assertThat(ApiException(403, "ACCOUNT_DELETED", "x").userMessage(false)).isEqualTo("This account has been deleted.")
    }

    @Test
    fun fifteenDigitInternationalNumbersAreAccepted() {
        assertThat(AuthInputNormalizer.phone("+123456789012345")).isEqualTo("+123456789012345")
    }
}