package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SystemTransactionTextTest {

    @Test fun persianTitleIsShownInEnglishAndKeepsTheGoalName() {
        assertThat(SystemTransactionText.localizeTitle("واریز به قلک: لپ‌تاپ", false))
            .isEqualTo("Deposit to saving goal: لپ‌تاپ")
    }

    @Test fun englishTitleIsShownInPersian() {
        assertThat(SystemTransactionText.localizeTitle("Auto Monthly Deposit to: Trip", true))
            .isEqualTo("واریز خودکار ماهانه به قلک: Trip")
        assertThat(SystemTransactionText.localizeTitle("Withdrawal from saving goal: Car", true))
            .isEqualTo("برداشت از قلک: Car")
    }

    @Test fun debtTitlesAreLocalized() {
        assertThat(SystemTransactionText.localizeTitle("پرداخت بدهی به: علی", false))
            .isEqualTo("Debt payment to: علی")
        assertThat(SystemTransactionText.localizeTitle("اصلاح/برداشت از پرداخت بدهی: علی", false))
            .isEqualTo("Correction/withdrawal of debt payment: علی")
    }

    @Test fun notesAreLocalizedBothWays() {
        assertThat(SystemTransactionText.localizeNote("واریز دستی به قلک", false)).isEqualTo("Manual saving goal deposit")
        assertThat(SystemTransactionText.localizeNote("Automatic monthly deposit", true)).isEqualTo("واریز خودکار ماهانه")
        assertThat(SystemTransactionText.localizeNote("ثبت اولیه علی", false)).isEqualTo("Initial entry علی")
    }

    @Test fun userTextIsNeverTouched() {
        assertThat(SystemTransactionText.localizeTitle("Lunch with friends", true)).isEqualTo("Lunch with friends")
        assertThat(SystemTransactionText.localizeNote("my own note", false)).isEqualTo("my own note")
        assertThat(SystemTransactionText.localizeNote(null, true)).isNull()
        assertThat(SystemTransactionText.localizeTitle("", true)).isEmpty()
    }
}