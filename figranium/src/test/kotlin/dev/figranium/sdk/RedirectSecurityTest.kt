package dev.figranium.sdk

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RedirectSecurityTest {
    @Test fun injectedRedirectFollowingClientIsOverriddenForRequestsAndStreams() = runBlocking {
        val requests = mutableListOf<Request>()
        val injected = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                requests += request
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(302).message("Found")
                    .header("Location", "https://attacker.example/stolen")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }.build()
        val client = Figranium(
            baseUrl = "https://api.example",
            authentication = FigraniumAuthentication.ApiKey("test-secret", "x-api-key"),
            httpClient = injected,
        )
        assertFalse(client.safeHttpClient.followRedirects)
        assertFalse(client.safeHttpClient.followSslRedirects)

        try {
            client.health.check()
            fail("HTTP redirect must be rejected")
        } catch (error: FigraniumException) {
            assertEquals("REDIRECT_NOT_ALLOWED", error.code)
            assertEquals(302, error.status)
        }
        try {
            client.executions.stream("run-1").toList()
            fail("SSE redirect must be rejected")
        } catch (error: FigraniumException) {
            assertEquals("REDIRECT_NOT_ALLOWED", error.code)
            assertEquals(302, error.status)
        }
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.header("x-api-key") == "test-secret" })
    }
}
