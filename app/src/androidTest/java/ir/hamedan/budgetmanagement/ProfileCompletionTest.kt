package ir.hamedan.budgetmanagement

import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.ui.screens.profile.ProfileCompletion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCompletionTest {

    private fun user(gender: String, birth: String? = "1990-01-01") = UserEntity(
        id = "u", firstName = "A", lastName = "B", gender = gender, birthDate = birth
    )

    @Test
    fun preferNotToSayCountsAsAnswered() {
        assertTrue(ProfileCompletion.of(user("prefer_not_to_say")).isComplete)
        assertTrue(ProfileCompletion.of(user("male")).isComplete)
    }

    @Test
    fun blankGenderOrMissingBirthDateIsStillMissing() {
        assertEquals(listOf(ir.hamedan.budgetmanagement.ui.screens.profile.MissingProfileField.GENDER),
            ProfileCompletion.of(user("")).missing)
        assertEquals(listOf(ir.hamedan.budgetmanagement.ui.screens.profile.MissingProfileField.BIRTH_DATE),
            ProfileCompletion.of(user("male", null)).missing)
    }
}