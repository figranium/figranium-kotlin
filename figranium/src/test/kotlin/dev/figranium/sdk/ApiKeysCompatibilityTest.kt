package dev.figranium.sdk

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeysCompatibilityTest {
    @Test fun scopedApiKeysUseWorkspaceSessionAndPreserveTaskAllowlist() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            val body = when (request.method) {
                "GET" -> """{"keys":[],"legacySecret":null,"permissions":["tasks:read","tasks:run","tasks:manage"]}"""
                "POST" -> """{"key":{"id":"key/1","name":"Runner","permissions":["tasks:run"],"taskIds":["task-1"],"createdAt":"2026-10-30T00:00:00Z"},"secret":"once"}"""
                "PATCH" -> """{"key":{"id":"key/1","name":"Production","permissions":["tasks:run"],"taskIds":["task-1"],"createdAt":"2026-10-30T00:00:00Z"}}"""
                else -> """{"ok":true}"""
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()

        val workspace = Figranium(
            baseUrl = "http://localhost:11345",
            authentication = FigraniumAuthentication.Session,
            httpClient = transport,
        )
        assertEquals(3, workspace.apiKeys.list().permissions.size)
        val created = workspace.apiKeys.create(ApiKeyCreateInput(
            permissions = listOf(ApiKeyPermission.TASKS_RUN),
            name = "Runner",
            taskIds = listOf("task-1"),
        ))
        assertEquals("once", created.secret)
        assertEquals(listOf("task-1"), created.key.taskIds)
        assertEquals("Production", workspace.apiKeys.rename("key/1", "Production").key.name)
        assertTrue(workspace.apiKeys.revoke("key/1").ok)
        assertEquals(listOf("GET", "POST", "PATCH", "DELETE"), requests.map { it.method })
        assertEquals(
            listOf("/api/api-keys", "/api/api-keys", "/api/api-keys/key%2F1", "/api/api-keys/key%2F1"),
            requests.map { it.url.encodedPath },
        )
        assertTrue(requests.drop(1).all { it.header("X-Requested-With") == "XMLHttpRequest" })
        val buffer = Buffer()
        requests[1].body!!.writeTo(buffer)
        val payload = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
        assertEquals("tasks:run", payload.getValue("permissions").toString().trim('[', '"', ']'))
        assertEquals("task-1", payload.getValue("taskIds").toString().trim('[', '"', ']'))

        val apiClient = Figranium(authentication = FigraniumAuthentication.ApiKey("secret"), httpClient = transport)
        val anonymous = Figranium(authentication = FigraniumAuthentication.None, httpClient = transport)
        var rejected = 0
        val operations: List<suspend () -> Any> = listOf(
            { apiClient.apiKeys.list() },
            { apiClient.apiKeys.create(ApiKeyCreateInput(listOf(ApiKeyPermission.TASKS_RUN))) },
            { apiClient.apiKeys.rename("key", "Blocked") },
            { apiClient.apiKeys.revoke("key") },
            { anonymous.apiKeys.list() },
        )
        for (operation in operations) {
            try { operation() } catch (error: FigraniumException) {
                if (error.code == "SESSION_REQUIRED") rejected++
            }
        }
        assertEquals(5, rejected)
        assertEquals(4, requests.size)
    }
}
