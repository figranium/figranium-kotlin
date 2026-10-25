package dev.figranium.sdk

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class ExecutionListCompatibilityTest {
    @Test fun routesByAuthenticationAndDecodesNumericStatus() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            val body = if (request.url.encodedPath.endsWith("/result")) {
                """{"result":{"rows":[1]}}"""
            } else {
                """{"executions":[{"id":"run-1","timestamp":1,"status":200}]}"""
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val workspace = Figranium(authentication = FigraniumAuthentication.Session, httpClient = transport)
        val apiKey = Figranium(authentication = FigraniumAuthentication.ApiKey("secret"), httpClient = transport)
        assertEquals("200", workspace.executions.list().single().status)
        assertEquals("200", apiKey.executions.list().single().status)
        workspace.executions.list(apiKeyRoute = true)
        val full = apiKey.executions.getFullResult("run/1")

        assertEquals(
            listOf("/api/executions", "/api/executions/list", "/api/executions/list", "/api/executions/run%2F1/result"),
            requests.map { it.url.encodedPath },
        )
        assertEquals(Json.parseToJsonElement("""{"rows":[1]}"""), full)
        assertEquals("Bearer secret", requests.last().header("authorization"))
        val legacy = Json.decodeFromString(Execution.serializer(), """{"id":"run-2","timestamp":1,"status":"stopped"}""")
        assertEquals("stopped", legacy.status)
    }
}
