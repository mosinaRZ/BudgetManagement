package ir.hamedan.budgetmanagement.platform.biometric

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper

object BiometricPromptManager {
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String? = null,
        onSuccess: () -> Unit
    ) {
        // تشخیص زبان برنامه (فارسی یا انگلیسی)
        val isPersian = LocaleHelper.getLanguage(activity) == "fa"

        // تعیین متون پیش‌فرض دوزبانه در صورت ورودی نداشتن
        val promptTitle = title ?: if (isPersian) "احراز هویت" else "Authentication"
        val negativeButtonText = if (isPersian) "انصراف" else "Cancel"

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}