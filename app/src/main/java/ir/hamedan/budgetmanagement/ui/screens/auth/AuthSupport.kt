package ir.hamedan.budgetmanagement.ui.screens.auth

import ir.hamedan.budgetmanagement.data.network.ApiException

/**
 * Pure helpers shared by the sign-in, registration, recovery and change-password flows.
 * Nothing in here touches Android or Compose so it can be unit tested on the JVM.
 */
object AuthInputNormalizer {

    /** Converts Persian (۰-۹) and Arabic-Indic (٠-٩) digits to ASCII digits. */
    fun toAsciiDigits(value: String): String = buildString(value.length) {
        for (c in value) {
            append(
                when (c) {
                    in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                    in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                    else -> c
                }
            )
        }
    }

    /** Keeps at most [length] ASCII digits; used for one-time codes typed on a Persian keyboard. */
    fun otpCode(input: String, length: Int = 6): String =
        toAsciiDigits(input).filter { it in '0'..'9' }.take(length)

    /**
     * Normalises a phone number to the "+<country><number>" form the backend requires.
     * Accepts Iranian local forms (09123456789, 9123456789, 989123456789, 0098...) and
     * numbers that already carry a "+". Returns null when the value cannot be a phone number.
     */
    fun phone(input: String): String? {
        val cleaned = toAsciiDigits(input).filter { it in '0'..'9' || it == '+' }
        if (cleaned.isEmpty()) return null
        if (cleaned.count { it == '+' } > 1 || cleaned.indexOf('+') > 0) return null

        var value = when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("00") -> "+" + cleaned.drop(2)
            cleaned.startsWith("09") && cleaned.length == 11 -> "+98" + cleaned.drop(1)
            cleaned.startsWith("9") && cleaned.length == 10 -> "+98$cleaned"
            cleaned.startsWith("98") && cleaned.length == 12 -> "+$cleaned"
            else -> return null
        }
        // "+98 0912..." (trunk zero kept after the country code)
        if (value.startsWith("+980") && value.length == 14) value = "+98" + value.drop(4)

        return value.takeIf { it.length in 8..15 && it.drop(1).all { ch -> ch in '0'..'9' } }
    }

    fun email(input: String): String? {
        val value = input.trim()
        return value.takeIf { EMAIL_REGEX.matches(it) && it.length <= 320 }
    }

    /** Phone or email, depending on whether the value contains "@". */
    fun identifier(input: String): String? {
        val value = input.trim()
        return if (value.contains('@')) email(value) else phone(value)
    }

    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
}

/** Password rules for NEW passwords (registration, recovery, change). Login accepts any stored password. */
object PasswordPolicy {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 64

    private val COMMON = setOf(
        "12345678", "123456789", "1234567890", "11111111", "00000000", "password", "password1",
        "qwertyui", "qwerty123", "iloveyou", "abcd1234", "12341234", "1q2w3e4r"
    )

    enum class Strength { EMPTY, WEAK, MEDIUM, GOOD, STRONG }

    /** Printable ASCII without spaces: avoids keyboard-layout and invisible-character surprises. */
    fun isAllowedChar(c: Char): Boolean = c.code in 0x21..0x7E

    fun characterClasses(password: String): Int =
        listOf(
            password.any { it in 'a'..'z' },
            password.any { it in 'A'..'Z' },
            password.any { it in '0'..'9' },
            password.any { isAllowedChar(it) && !it.isLetterOrDigit() }
        ).count { it }

    fun strength(password: String): Strength {
        if (password.isEmpty()) return Strength.EMPTY
        if (password.length < MIN_LENGTH) return Strength.WEAK
        val score = characterClasses(password) + if (password.length >= 12) 1 else 0
        return when {
            score <= 1 -> Strength.WEAK
            score == 2 -> Strength.MEDIUM
            score == 3 -> Strength.GOOD
            else -> Strength.STRONG
        }
    }

    /** Returns a localized reason the password cannot be used, or null when it is acceptable. */
    fun validate(password: String, isPersian: Boolean): String? = when {
        password.length < MIN_LENGTH ->
            if (isPersian) "گذرواژه باید حداقل $MIN_LENGTH کاراکتر باشد." else "Password must be at least $MIN_LENGTH characters."
        password.length > MAX_LENGTH ->
            if (isPersian) "گذرواژه نباید بیشتر از $MAX_LENGTH کاراکتر باشد." else "Password must be at most $MAX_LENGTH characters."
        !password.all(::isAllowedChar) ->
            if (isPersian) "فقط از حروف انگلیسی، اعداد و نمادها (بدون فاصله) استفاده کنید." else "Use English letters, digits and symbols only (no spaces)."
        characterClasses(password) < 2 || password.lowercase() in COMMON ->
            if (isPersian) "گذرواژه خیلی ساده است؛ حروف، عدد یا نماد را ترکیب کنید." else "Password is too simple; mix letters, digits or symbols."
        else -> null
    }
}

enum class AuthContext { LOGIN, REGISTER, OTP_REQUEST, RESET_PASSWORD, CHANGE_PASSWORD }

/**
 * Turns any failure into a message that makes sense on the screen where it happened.
 * The generic [ApiException.userMessage] describes a *session* problem for 401, which is
 * misleading when the user simply typed a wrong password or code.
 */
fun authErrorMessage(error: Throwable, isPersian: Boolean, context: AuthContext): String {
    val api = error as? ApiException
    if (api != null) {
        val code = api.code.orEmpty()
        return when {
            api.statusCode == 0 || code == "NETWORK_ERROR" -> api.userMessage(isPersian)
            code == "INVALID_PASSWORD" ->
                if (isPersian) "گذرواژه فعلی نادرست است." else "The current password is incorrect."
            code == "RATE_LIMITED" -> when (context) {
                AuthContext.LOGIN, AuthContext.CHANGE_PASSWORD ->
                    if (isPersian) "تلاش‌های ناموفق زیاد بود. چند دقیقه بعد دوباره تلاش کنید." else "Too many failed attempts. Try again in a few minutes."
                else -> api.userMessage(isPersian)
            }
            context == AuthContext.LOGIN && (api.statusCode == 401 || code == "UNAUTHORIZED") ->
                if (isPersian) "شماره موبایل/ایمیل یا گذرواژه نادرست است." else "Incorrect phone/email or password."
            context == AuthContext.REGISTER && (api.statusCode == 401 || code == "UNAUTHORIZED") ->
                if (isPersian) "کد تأیید نادرست یا منقضی شده است." else "The verification code is wrong or has expired."
            context == AuthContext.REGISTER && code == "CONFLICT" ->
                if (isPersian) "با این شماره یا ایمیل قبلاً حساب ساخته شده است." else "An account already exists for this phone number or email."
            context == AuthContext.RESET_PASSWORD && (api.statusCode == 401 || code == "UNAUTHORIZED") ->
                if (isPersian) "کد تأیید یا کلید بازیابی نادرست است." else "The verification code or recovery key is wrong."
            context == AuthContext.CHANGE_PASSWORD && api.statusCode == 401 ->
                if (isPersian) "نشست شما منقضی شده است. دوباره وارد شوید." else "Your session has expired. Please sign in again."
            code == "VALIDATION_ERROR" || code == "BAD_REQUEST" ->
                if (isPersian) "اطلاعات واردشده معتبر نیست؛ ورودی‌ها را بررسی کنید." else api.userMessage(false)
            else -> api.userMessage(isPersian)
        }
    }
    return if (isPersian) {
        "عملیات ناموفق بود. دوباره تلاش کنید."
    } else {
        error.message?.takeIf { it.isNotBlank() } ?: "The operation failed. Please try again."
    }
}