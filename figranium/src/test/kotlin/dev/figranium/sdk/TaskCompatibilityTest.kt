package dev.figranium.sdk

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

    @Test fun baserowOutputIncludesDatabaseAndDedicated() {
        val output = TaskOutput(credentialId = "credential-1", tableId = "table-1", databaseId = "database-1", dedicated = true)
        val encoded = json.parseToJsonElement(json.encodeToString(TaskOutput.serializer(), output)).jsonObject
        assertEquals(JsonPrimitive("database-1"), encoded["databaseId"])
        assertEquals(JsonPrimitive(true), encoded["dedicated"])
    }
}
