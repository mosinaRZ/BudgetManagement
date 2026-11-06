package ir.hamedan.budgetmanagement.data.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppVersionTest {

    @Test
    fun parse_readsFullAndShortVersions() {
        assertThat(AppVersion.parse("1.2.0")).isEqualTo(AppVersion(1, 2, 0))
        assertThat(AppVersion.parse("1.2")).isEqualTo(AppVersion(1, 2, 0))
        assertThat(AppVersion.parse(" v1.10.4 ")).isEqualTo(AppVersion(1, 10, 4))
    }

    @Test
    fun parse_ignoresBuildSuffix() {
        assertThat(AppVersion.parse("1.2.0-debug")).isEqualTo(AppVersion(1, 2, 0))
        assertThat(AppVersion.parse("1.1.2+7")).isEqualTo(AppVersion(1, 1, 2))
    }

    @Test
    fun parse_rejectsUnusableText() {
        assertThat(AppVersion.parse(null)).isNull()
        assertThat(AppVersion.parse("")).isNull()
        assertThat(AppVersion.parse("soon")).isNull()
        assertThat(AppVersion.parse("1.2.3.4")).isNull()
        assertThat(AppVersion.parse("1..2")).isNull()
    }

    @Test
    fun compare_isNumericNotTextual() {
        assertThat(AppVersion(1, 1, 2)).isLessThan(AppVersion(1, 2, 0))
        assertThat(AppVersion(1, 10, 0)).isGreaterThan(AppVersion(1, 9, 9))
        assertThat(AppVersion.parse("1.2")).isEqualTo(AppVersion.parse("1.2.0"))
        assertThat(AppVersion(2, 0, 0)).isGreaterThan(AppVersion(1, 99, 99))
    }
}
