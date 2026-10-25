package dev.figranium.sdk

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSummaryCompatibilityTest {
    @Test fun summariesSelectSessionOrApiKeyRoute() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            val body = if (request.url.encodedPath == "/api/tasks") {
                """[{"id":"task-1","name":"Session task","description":"A task"}]"""
            } else {
                """{"tasks":[{"id":"task-2","name":"API task"}]}"""
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
        assertEquals("task-1", workspace.tasks.listSummaries().single().id)
        assertEquals("A task", workspace.tasks.listSummaries().single().description)
        assertEquals("API task", apiKey.tasks.listSummaries().single().name)
        assertEquals(
            listOf("/api/tasks", "/api/tasks", "/api/tasks/list"),
            requests.map { it.url.encodedPath },
        )
    }
}
