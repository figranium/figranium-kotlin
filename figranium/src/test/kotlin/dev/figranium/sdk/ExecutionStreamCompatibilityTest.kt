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
import org.junit.Test

class ExecutionStreamCompatibilityTest {
    @Test fun executionStreamIncludesEncodedRunId() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("event: update\ndata: {\"status\":\"done\"}\n\n".toResponseBody("text/event-stream".toMediaType()))
                .build()
        }.build()
        val client = Figranium(
            baseUrl = "http://localhost:11345",
            authentication = FigraniumAuthentication.Session,
            httpClient = transport,
        )

        val events = client.executions.stream("run/1? & x").toList()
        assertEquals("/api/executions/stream", requests.single().url.encodedPath)
        assertEquals("run/1? & x", requests.single().url.queryParameter("runId"))
        assertEquals("update", events.single().event)
        assertEquals("{\"status\":\"done\"}", events.single().raw)
    }
}
