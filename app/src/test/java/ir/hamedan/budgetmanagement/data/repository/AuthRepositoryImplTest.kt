package ir.hamedan.budgetmanagement.data.repository

import io.mockk.*
import ir.hamedan.budgetmanagement.data.guest.GuestModeManager
import ir.hamedan.budgetmanagement.data.local.SyncLocalDataSource
import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.network.AccountApi
import ir.hamedan.budgetmanagement.data.network.AuthApi
import ir.hamedan.budgetmanagement.data.security.*
import ir.hamedan.budgetmanagement.data.sync.SyncEngine
import ir.hamedan.budgetmanagement.worker.SyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryImplTest {
    private val authApi = mockk<AuthApi>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val syncStateRepository = mockk<SyncStateRepository>(relaxed = true)
    private val session = mockk<AuthSessionStore>(relaxed = true)
    private val keys = mockk<SyncKeyManager>(relaxed = true)
    private val syncEngine = mockk<SyncEngine>(relaxed = true)
    private val local = mockk<SyncLocalDataSource>(relaxed = true)
    private val identity = mockk<DeviceIdentityStore>(relaxed = true)
    private val remembered = mockk<RememberedLoginStore>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val accountApi = mockk<AccountApi>(relaxed = true)
    private val deviceInfo = mockk<DeviceInfoProvider>(relaxed = true)
    private val guest = mockk<GuestModeManager>(relaxed = true)

    private val subject = AuthRepositoryImpl(authApi, userDao, syncStateRepository, session, keys, syncEngine,
        local, identity, remembered, scheduler, accountApi, deviceInfo, guest)

    @Test fun `blank login input is rejected before any API or credential access`() = runTest {
        val blankIdentifier = subject.login("  ", "valid-password", false, false)
        val blankPassword = subject.login("user@example.com", "  ", false, false)
        assertTrue(blankIdentifier.isFailure)
        assertTrue(blankPassword.isFailure)
        assertTrue(blankIdentifier.exceptionOrNull() is IllegalArgumentException)
        verify(exactly = 0) { authApi.loginParams(any()) }
        verify(exactly = 0) { authApi.login(any(), any(), any(), any(), any()) }
    }

    @Test fun `otp destination is trimmed and api failure is returned as Result failure`() = runTest {
        every { authApi.requestOtp("user@example.com", "email", "register") } returns AuthApi.OtpResponse("challenge", 123L)
        val result = subject.requestOtp("  user@example.com  ", "email", "register")
        assertEquals("challenge", result.getOrThrow().challengeId)
        verify(exactly = 1) { authApi.requestOtp("user@example.com", "email", "register") }

        every { authApi.requestOtp("broken", "email", "register") } throws IllegalStateException("offline")
        assertTrue(subject.requestOtp("broken", "email", "register").isFailure)
    }

    @Test fun `registration rejects blank phone before creating key material`() = runTest {
        val input = AuthRepository.RegistrationInput(phoneNumber = " ", password = "long-password", otpChallengeId = "c", otpCode = "123456")
        assertTrue(subject.register(input).isFailure)
        verify(exactly = 0) { keys.createRegistrationMaterial(any()) }
        verify(exactly = 0) { authApi.register(any()) }
    }
}