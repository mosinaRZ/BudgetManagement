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
 *
 * Gender is optional (see README): "prefer not to say" is a legitimate answer, so it counts as
 * done. Only a blank/unknown value is treated as missing. Previously only male/female counted,
 * which left everyone who chose "prefer not to say" (also the stored default) with a CTA that
 * could never be satisfied.
 */
data class ProfileCompletion(
    val percent: Int,
    val missing: List<MissingProfileField>
) {
    val isComplete: Boolean get() = missing.isEmpty()

    companion object {
        private const val PROFILE_FIELDS = 4
        private const val BASE_STEPS = 1 // account created with a phone number

        private val VALID_GENDERS = setOf("male", "female", "prefer_not_to_say")

        /** Single source of truth for "gender step is done", shared with the profile screen ring. */
        fun isGenderAnswered(gender: String?): Boolean = gender in VALID_GENDERS

        fun of(user: UserEntity): ProfileCompletion {
            val missing = buildList {
                if (user.firstName.isBlank()) add(MissingProfileField.FIRST_NAME)
                if (user.lastName.isBlank()) add(MissingProfileField.LAST_NAME)
                if (!isGenderAnswered(user.gender)) add(MissingProfileField.GENDER)
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