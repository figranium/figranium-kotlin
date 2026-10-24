package dev.figranium.sdk

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CookieStatesCompatibilityTest {
    @Test fun sessionMutationsAddCsrfHeaderAndPreserveEncodedIds() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val client = Figranium(
            baseUrl = "http://localhost:11345",
            authentication = FigraniumAuthentication.Session,
            httpClient = transport,
        )
        val state = buildJsonObject {
            put("cookies", kotlinx.serialization.json.JsonArray(emptyList()))
            put("origins", kotlinx.serialization.json.JsonArray(emptyList()))
        }
        client.cookieStates.create("login", state)
        client.cookieStates.delete("state/1")
        assertEquals(2, requests.size)
        assertEquals("POST", requests[0].method)
        assertEquals("/api/cookie-states", requests[0].url.encodedPath)
        assertEquals("XMLHttpRequest", requests[0].header("X-Requested-With"))
        assertEquals("DELETE", requests[1].method)
        assertEquals("/api/cookie-states/state%2F1", requests[1].url.encodedPath)
        assertEquals("XMLHttpRequest", requests[1].header("X-Requested-With"))
    }

    @Test fun apiKeysDoNotGainSessionCsrfHeader() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val client = Figranium(
            authentication = FigraniumAuthentication.ApiKey("test"),
            httpClient = transport,
        )
        client.cookieStates.delete("state-1")
        assertEquals("Bearer test", requests.single().header("authorization"))
        assertNull(requests.single().header("X-Requested-With"))
    }

    @Test fun explicitlySuppliedOriginIsPreserved() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val client = Figranium(
            authentication = FigraniumAuthentication.Session,
            headers = mapOf("Origin" to "https://figranium.example"),
            httpClient = transport,
        )
        client.cookieStates.delete("state-1")
        assertEquals("https://figranium.example", requests.single().header("Origin"))
        assertNull(requests.single().header("X-Requested-With"))
    }
}
