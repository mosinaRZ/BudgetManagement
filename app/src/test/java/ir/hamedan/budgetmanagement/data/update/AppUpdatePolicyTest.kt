package ir.hamedan.budgetmanagement.data.update

import android.app.Application
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppUpdatePolicyTest {

    @Test
    fun fromJson_readsVersionsAndKeepsOnlyHttpsLinks() {
        val policy = AppUpdatePolicy.fromJson(
            JSONObject(
                """
                {"minVersion":"1.1.2","latestVersion":"1.2.0",
                 "updateUrls":{"play":"https://play.google.com/x","myket":"http://insecure.example","bazaar":""}}
                """.trimIndent()
            )
        )

        assertThat(policy.minVersion).isEqualTo("1.1.2")
        assertThat(policy.latestVersion).isEqualTo("1.2.0")
        assertThat(policy.updateUrls).containsExactly("play", "https://play.google.com/x")
    }

    @Test
    fun fromJson_treatsMissingOrBlankVersionsAsUnknown() {
        val policy = AppUpdatePolicy.fromJson(JSONObject("""{"minVersion":"  ","latestVersion":null}"""))

        assertThat(policy.minVersion).isNull()
        assertThat(policy.latestVersion).isNull()
        assertThat(policy.updateUrls).isEmpty()
    }

    @Test
    fun toJson_roundTrips() {
        val original = AppUpdatePolicy("1.1.2", null, mapOf("default" to "https://cidna.ir"))

        val copy = AppUpdatePolicy.fromJson(original.toJson())

        assertThat(copy).isEqualTo(original)
    }
}