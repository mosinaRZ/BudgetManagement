package ir.hamedan.budgetmanagement.data.network

import io.mockk.*
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import org.junit.Assert.*
import org.junit.Test

class AuthenticatedApiClientTest {
    private val client = mockk<ApiHttpClient>()
    private val authApi = mockk<AuthApi>()
    private val session = mockk<AuthSessionStore>(relaxed = true)

    @Test fun `missing access token rejects request without network call`() {
        every { session.accessToken() } returns null
        val subject = AuthenticatedApiClient(client, authApi, session)
        val error = try { subject.request("GET", "/private"); null } catch (e: ApiException) { e }
        assertEquals(401, error?.statusCode)
        assertEquals("UNAUTHORIZED", error?.code)
        verify(exactly = 0) { client.request(any(), any(), any(), any()) }
    }

    @Test fun `successful request does not refresh credentials`() {
        every { session.accessToken() } returns "access"
        every { client.request("GET", "/private", null, "access") } returns ApiHttpClient.ApiResponse(200, "{}")
        assertEquals(200, AuthenticatedApiClient(client, authApi, session).request("GET", "/private").statusCode)
        verify(exactly = 0) { authApi.refresh(any()) }
    }

    @Test fun `401 refreshes once and retries using new access token`() {
        every { session.accessToken() } returns "old-access"
        every { session.refreshToken() } returns "old-refresh"
        every { session.userId() } returns "user"
        every { session.deviceId() } returns "device"
        every { session.kdfSalt() } returns "salt"
        every { session.passwordKeyEnvelope() } returns "pke"
        every { session.passwordKeyNonce() } returns "pkn"
        every { session.recoveryKeyEnvelope() } returns "rke"
        every { session.recoveryKeyNonce() } returns "rkn"
        every { session.role() } returns "regular"
        every { client.request("GET", "/private", null, "old-access") } returns ApiHttpClient.ApiResponse(401, "{}")
        every { authApi.refresh("old-refresh") } returns AuthApi.RefreshResponse("new-access", "new-refresh", "pro")
        every { client.request("GET", "/private", null, "new-access") } returns ApiHttpClient.ApiResponse(200, "{}")

        val result = AuthenticatedApiClient(client, authApi, session).request("GET", "/private")
        assertEquals(200, result.statusCode)
        verify(exactly = 1) { authApi.refresh("old-refresh") }
        verify { session.save("new-access", "new-refresh", "user", "device", "salt", "pke", "pkn", "rke", "rkn", "pro") }
    }

    @Test fun `transient refresh failure preserves session`() {
        every { session.accessToken() } returns "access"
        every { session.refreshToken() } returns "refresh"
        every { client.request(any(), any(), any(), any()) } returns ApiHttpClient.ApiResponse(401, "{}")
        every { authApi.refresh("refresh") } throws ApiException(503, "SERVICE_UNAVAILABLE", "down")
        val error = try { AuthenticatedApiClient(client, authApi, session).request("GET", "/private"); null } catch (e: ApiException) { e }
        assertEquals(503, error?.statusCode)
        verify(exactly = 0) { session.clearSession(any(), any()) }
    }
}