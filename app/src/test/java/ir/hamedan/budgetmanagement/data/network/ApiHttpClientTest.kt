package ir.hamedan.budgetmanagement.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiHttpClientTest {

    @Test
    fun `request sends json headers bearer token and parses success body`() {
        ServerSocket(0).use { server ->
            server.soTimeout = 5_000
            val capturedRequest = AtomicReference("")
            val workerFailure = AtomicReference<Throwable?>(null)

            val worker = thread(name = "api-http-test-server") {
                try {
                    server.accept().use { socket ->
                        socket.soTimeout = 5_000
                        val input = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8)
                        val headers = mutableListOf<String>()

                        while (true) {
                            val line = input.readLine() ?: break
                            headers += line
                            if (line.isEmpty()) break
                        }

                        val contentLength = headers
                            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                            ?.substringAfter(':')
                            ?.trim()
                            ?.toIntOrNull()
                            ?: 0

                        val requestBody = CharArray(contentLength)
                        if (contentLength > 0) {
                            var offset = 0
                            while (offset < contentLength) {
                                val count = input.read(requestBody, offset, contentLength - offset)
                                if (count < 0) break
                                offset += count
                            }
                        }

                        capturedRequest.set(
                            headers.joinToString("\n") + "\n\n" + String(requestBody)
                        )

                        val responseBody = """{"ok":true}"""
                        val responseBytes = responseBody.toByteArray(StandardCharsets.UTF_8)
                        val responseHeaders = (
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: application/json; charset=utf-8\r\n" +
                                        "Content-Length: ${responseBytes.size}\r\n" +
                                        "Connection: close\r\n\r\n"
                                ).toByteArray(StandardCharsets.US_ASCII)

                        socket.getOutputStream().apply {
                            write(responseHeaders)
                            write(responseBytes)
                            flush()
                        }
                    }
                } catch (t: Throwable) {
                    workerFailure.set(t)
                }
            }

            val response = ApiHttpClient("http://127.0.0.1:${server.localPort}/")
                .request(
                    method = "POST",
                    path = "/test",
                    body = org.json.JSONObject().put("value", 7),
                    bearerToken = "token-123"
                )

            worker.join(5_000)

            assertTrue("Test server thread did not finish", !worker.isAlive)
            workerFailure.get()?.let { throw AssertionError("Test server failed", it) }

            assertEquals(200, response.statusCode)
            assertTrue(response.json().getBoolean("ok"))

            val request = capturedRequest.get()
            assertTrue(request.startsWith("POST /test HTTP/1.1"))
            assertTrue(request.contains("Authorization: Bearer token-123"))
            assertTrue(request.contains("Content-Type: application/json"))
            assertTrue(request.contains(""""value":7"""))
        }
    }

    @Test
    fun `response maps unreadable body to ApiException`() {
        val error = try {
            ApiHttpClient.ApiResponse(502, "<html>bad gateway</html>").json()
            null
        } catch (e: ApiException) {
            e
        }

        assertNotNull(error)
        assertEquals(502, error!!.statusCode)
        assertEquals("BAD_RESPONSE", error.code)
    }

    @Test
    fun `error helpers safely handle invalid and absent error payload`() {
        val response = ApiHttpClient.ApiResponse(500, "not-json")

        assertNull(response.errorCode())
        assertEquals("Try later", response.errorMessage("Try later"))
    }
}
