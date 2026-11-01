package ir.hamedan.budgetmanagement.ui.screens.profile

import ir.hamedan.budgetmanagement.data.local.models.UserEntity

/** Which profile detail is still missing, in the order the user is guided through them. */
enum class MissingProfileField { FIRST_NAME, LAST_NAME, GENDER, BIRTH_DATE }

/**
 * Profile completion shown on the home screen.
 *
 * The account itself counts as the first completed step: the user already signed up with a
 * verified phone number, so the bar never starts from zero (1 base step + 4 profile fields = 20%
 * each).
 */
data class ProfileCompletion(
    val percent: Int,
    val missing: List<MissingProfileField>
) {
    val isComplete: Boolean get() = missing.isEmpty()

    companion object {
        private const val PROFILE_FIELDS = 4
        private const val BASE_STEPS = 1 // account created with a phone number

        fun of(user: UserEntity): ProfileCompletion {
            val missing = buildList {
                if (user.firstName.isBlank()) add(MissingProfileField.FIRST_NAME)
                if (user.lastName.isBlank()) add(MissingProfileField.LAST_NAME)
                if (user.gender != "male" && user.gender != "female") add(MissingProfileField.GENDER)
                if (user.birthDate.isNullOrBlank()) add(MissingProfileField.BIRTH_DATE)
            }
            val done = BASE_STEPS + (PROFILE_FIELDS - missing.size)
            return ProfileCompletion(
                percent = done * 100 / (BASE_STEPS + PROFILE_FIELDS),
                missing = missing
            )
        }
    }
}