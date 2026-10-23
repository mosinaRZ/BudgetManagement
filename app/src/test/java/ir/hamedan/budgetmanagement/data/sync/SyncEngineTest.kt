package ir.hamedan.budgetmanagement.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import ir.hamedan.budgetmanagement.data.local.AppDatabase
import ir.hamedan.budgetmanagement.data.network.ApiException
import ir.hamedan.budgetmanagement.data.network.SyncApi
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.data.security.SyncKeyManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncEngineTest {
    private lateinit var database: AppDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { database.close() }

    @Test fun `sync exits before touching database when no authenticated session`() = runTest {
        val api = mockk<SyncApi>()
        val session = mockk<AuthSessionStore>()
        val keys = mockk<SyncKeyManager>()
        every { session.accessToken() } returns null

        val result = SyncEngine(database, api, session, keys).sync()

        assertSame(SyncEngine.SyncResult.NotAuthenticated, result)
        verify(exactly = 0) { api.sync(any(), any(), any(), any()) }
        verify(exactly = 0) { keys.encrypt(any()) }
    }

    @Test fun `unauthorized sync clears session and requires reauthentication`() = runTest {
        val api = mockk<SyncApi>()
        val session = mockk<AuthSessionStore>(relaxed = true)
        val keys = mockk<SyncKeyManager>(relaxed = true)
        every { session.accessToken() } returns "access-token"
        every { session.deviceId() } returns "device-1"
        every { api.sync(any(), any(), any(), any()) } throws ApiException(401, "UNAUTHORIZED", "expired")

        val result = SyncEngine(database, api, session, keys).sync()

        assertSame(SyncEngine.SyncResult.ReauthenticationRequired, result)
        verify(exactly = 1) { session.clearSession(unexpected = true) }
        verify(exactly = 1) { api.sync(any(), "device-1", 0L, emptyList()) }
    }
}