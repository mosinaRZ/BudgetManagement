package ir.hamedan.budgetmanagement.data.update

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppUpdateManagerTest {

    private lateinit var store: AppUpdateStore
    private var now = 1_000_000L
    private var served: AppUpdatePolicy? = null

    private val urls = mapOf("default" to "https://cidna.ir")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = AppUpdateStore(context)
        served = null
    }

    private fun policy(min: String?, latest: String? = "1.2.0") = AppUpdatePolicy(min, latest, urls)

    private fun manager(installed: String) = AppUpdateManager(
        api = AppPolicySource { served ?: throw IOException("offline") },
        store = store,
        installedVersionName = installed,
        clock = { now }
    )

    @Test
    fun oldBuild_isBlockedFromTheCachedPolicy_evenOffline() {
        store.save(policy(min = "1.1.2"))

        val blocked = manager("1.1.0").state.value.blocked

        assertThat(blocked).isNotNull()
        assertThat(blocked!!.installedVersion).isEqualTo("1.1.0")
        assertThat(blocked.latestVersion).isEqualTo("1.2.0")
        assertThat(blocked.updateUrls).containsKey("default")
    }

    @Test
    fun supportedBuild_isNotBlocked() {
        store.save(policy(min = "1.1.2"))

        assertThat(manager("1.1.2").isBlocked()).isFalse()
        assertThat(manager("1.2.0").isBlocked()).isFalse()
    }

    @Test
    fun neverReachedTheServer_isNotBlocked() {
        assertThat(manager("1.0.0").isBlocked()).isFalse()
    }

    @Test
    fun unparsableInstalledVersion_failsOpen() {
        store.save(policy(min = "1.1.2"))

        assertThat(manager("dev-build").isBlocked()).isFalse()
    }

    @Test
    fun refresh_blocksWhenTheServerRaisesTheMinimum_andUnblocksWhenItIsLowered() = runBlocking {
        val manager = manager("1.1.2")
        assertThat(manager.isBlocked()).isFalse()

        served = policy(min = "1.2.0")
        manager.refresh(force = true)
        assertThat(manager.isBlocked()).isTrue()

        served = policy(min = "1.1.2")
        manager.refresh(force = true)
        assertThat(manager.isBlocked()).isFalse()
    }

    @Test
    fun failedRefresh_keepsTheLastKnownDecision() = runBlocking {
        store.save(policy(min = "1.1.2"))
        val manager = manager("1.0.5")
        assertThat(manager.isBlocked()).isTrue()

        served = null // the request fails
        manager.refresh(force = true)

        assertThat(manager.isBlocked()).isTrue()
        assertThat(manager.state.value.checking).isFalse()
    }

    @Test
    fun refresh_isThrottled_butAlwaysAllowedWhenForced() = runBlocking {
        val manager = manager("1.2.0")
        served = policy(min = "1.1.2")
        manager.refresh()

        served = policy(min = "1.9.0")
        now += 60_000L // a minute later: still inside the throttle window
        manager.refresh()
        assertThat(manager.isBlocked()).isFalse()

        manager.refresh(force = true)
        assertThat(manager.isBlocked()).isTrue()
    }

    @Test
    fun serverRejection426_blocksAtOnce_andClearsWhenThePolicyAcceptsTheBuild() = runBlocking {
        val manager = manager("1.2.0")
        assertThat(manager.isBlocked()).isFalse()

        // The background refresh that follows a 426 fails here (nothing is served), so the block holds.
        manager.onServerRejectedVersion()
        assertThat(manager.isBlocked()).isTrue()

        // The policy now says 1.2.0 is fine, so the earlier rejection no longer applies. The
        // background refresh may be running, so retry until one refresh has seen the new policy.
        served = policy(min = "1.1.2")
        repeat(40) {
            if (!manager.isBlocked()) return@repeat
            manager.refresh(force = true)
            delay(50)
        }
        assertThat(manager.isBlocked()).isFalse()
    }

    @Test
    fun rejectionOfAnOldBuild_doesNotCarryOverToTheUpdatedBuild() {
        store.markRejected("1.1.0")

        assertThat(manager("1.1.0").isBlocked()).isTrue()
        assertThat(manager("1.2.0").isBlocked()).isFalse()
    }

    @Test
    fun awaitStartupCheck_returnsOnceTheFirstRefreshFinished_evenWhenItFailed() = runBlocking {
        val manager = manager("1.2.0")
        served = null

        manager.refresh()
        manager.awaitStartupCheck() // must not hang
    }
}