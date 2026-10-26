package ir.hamedan.budgetmanagement.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebLinksTest {

    @Test
    fun persianPagesLiveAtTheRootOfTheWebsite() {
        assertThat(WebLinks.url(WebLinks.Page.PRIVACY, isPersian = true)).isEqualTo("https://cidna.ir/privacy/")
        assertThat(WebLinks.url(WebLinks.Page.SECURITY, isPersian = true)).isEqualTo("https://cidna.ir/security/")
        assertThat(WebLinks.url(WebLinks.Page.TERMS, isPersian = true)).isEqualTo("https://cidna.ir/terms/")
        assertThat(WebLinks.url(WebLinks.Page.LICENSES, isPersian = true)).isEqualTo("https://cidna.ir/licenses/")
        assertThat(WebLinks.url(WebLinks.Page.ACCOUNT_DELETION, isPersian = true))
            .isEqualTo("https://cidna.ir/account-deletion/")
    }

    @Test
    fun englishPagesLiveUnderEn() {
        assertThat(WebLinks.url(WebLinks.Page.PRIVACY, isPersian = false)).isEqualTo("https://cidna.ir/en/privacy/")
        assertThat(WebLinks.url(WebLinks.Page.TERMS, isPersian = false)).isEqualTo("https://cidna.ir/en/terms/")
    }
}