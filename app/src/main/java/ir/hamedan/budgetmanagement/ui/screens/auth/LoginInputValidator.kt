package ir.hamedan.budgetmanagement.ui.screens.auth

/** Validates login input before any authentication request is made. */
object LoginInputValidator {
    fun validate(identifier: String, password: String, isPersian: Boolean): String? {
        if (identifier.isBlank()) {
            return if (isPersian) "شماره موبایل یا ایمیل را وارد کنید" else "Enter your phone number or email"
        }
        if (password.isBlank()) {
            return if (isPersian) "گذرواژه را وارد کنید" else "Enter your password"
        }
        return null
    }
}