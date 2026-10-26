package dev.figranium.sdk

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

    @Test fun cookieStateSupportsOmittedFreshAndNamed() {
        val base = Task(name = "Search", url = "https://example.com", mode = "agent")
        val omitted = json.parseToJsonElement(json.encodeToString(Task.serializer(), base)).jsonObject
        assertFalse(omitted.containsKey("cookieStateId"))

        val fresh = json.parseToJsonElement(
            json.encodeToString(Task.serializer(), base.copy(cookieStateId = JsonNull))
        ).jsonObject
        assertTrue(fresh.containsKey("cookieStateId"))
        assertEquals(JsonNull, fresh["cookieStateId"])

        val named = json.parseToJsonElement(
            json.encodeToString(Task.serializer(), base.copy(cookieStateId = JsonPrimitive("cookie-1")))
        ).jsonObject
        assertEquals(JsonPrimitive("cookie-1"), named["cookieStateId"])
    }

    @Test fun deleteTaskVersionUsesSessionRouteAndEncodedIds() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"success":true,"versionId":"version/1"}""".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val client = Figranium(
            baseUrl = "http://localhost:11345",
            authentication = FigraniumAuthentication.Session,
            httpClient = transport,
        )
        val result = client.tasks.deleteVersion("task/1", "version/1")
        assertEquals(JsonPrimitive(true), result["success"])
        assertEquals(JsonPrimitive("version/1"), result["versionId"])
        assertEquals("DELETE", requests.single().method)
        assertEquals("/api/tasks/task%2F1/versions/version%2F1", requests.single().url.encodedPath)
        assertEquals("XMLHttpRequest", requests.single().header("X-Requested-With"))
    }

    @Test fun baserowOutputIncludesDatabaseAndDedicated() {
        val output = TaskOutput(credentialId = "credential-1", tableId = "table-1", databaseId = "database-1", dedicated = true)
        val encoded = json.parseToJsonElement(json.encodeToString(TaskOutput.serializer(), output)).jsonObject
        assertEquals(JsonPrimitive("database-1"), encoded["databaseId"])
        assertEquals(JsonPrimitive(true), encoded["dedicated"])
    }
}
