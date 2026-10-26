package dev.figranium.sdk

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer

@PublishedApi internal fun pathId(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
private fun jsonObject(vararg values: Pair<String, JsonElement?>) = buildJsonObject { values.forEach { (key, value) -> if (value != null) put(key, value) } }
private fun string(value: String?) = value?.let(::JsonPrimitive)
@PublishedApi internal suspend inline fun <reified T> Figranium.get(path: String, query: Map<String, String?> = emptyMap(), options: RequestOptions = RequestOptions()): T = request("GET", path, serializer(), query = query, options = options)
@PublishedApi internal suspend inline fun <reified T> Figranium.send(method: String, path: String, body: JsonElement? = null, query: Map<String, String?> = emptyMap(), options: RequestOptions = RequestOptions()): T = request(method, path, serializer(), body, query, options)


/** Named browser storage states are managed using an authenticated workspace session. */
class CookieStatesResource internal constructor(private val client: Figranium) {
    suspend fun list(options: RequestOptions = RequestOptions()): JsonObject =
        client.get("/api/cookie-states", options = options)

    suspend fun get(id: String, options: RequestOptions = RequestOptions()): JsonObject =
        client.get("/api/cookie-states/${pathId(id)}", options = options)

    suspend fun create(name: String, state: JsonObject, options: RequestOptions = RequestOptions()): JsonObject =
        client.send("POST", "/api/cookie-states", jsonObject("name" to string(name), "state" to state), options = options)

    suspend fun rename(id: String, name: String, options: RequestOptions = RequestOptions()): JsonObject =
        client.send("PATCH", "/api/cookie-states/${pathId(id)}", jsonObject("name" to string(name)), options = options)

    suspend fun update(id: String, state: JsonObject, options: RequestOptions = RequestOptions()): JsonObject =
        client.send("PATCH", "/api/cookie-states/${pathId(id)}", jsonObject("state" to state), options = options)

    suspend fun delete(id: String, options: RequestOptions = RequestOptions()): JsonObject =
        client.send("DELETE", "/api/cookie-states/${pathId(id)}", options = options)
}

class TasksResource internal constructor(@PublishedApi internal val client: Figranium) {
    suspend fun list(options: RequestOptions = RequestOptions()): List<Task> = client.get("/api/tasks", options = options)
    /** Workspace sessions use /api/tasks; /api/tasks/list is reserved for API keys. */
    suspend fun listSummaries(options: RequestOptions = RequestOptions()): List<TaskSummary> {
        if (client.usesSession) return client.get("/api/tasks", options = options)
        return client.get<JsonObject>("/api/tasks/list", options = options).getValue("tasks")
            .let { client.json.decodeFromJsonElement(ListSerializer(TaskSummary.serializer()), it) }
    }
    suspend fun save(task: Task, createVersion: Boolean = false, options: RequestOptions = RequestOptions()): Task = client.send("POST", "/api/tasks", client.json.encodeToJsonElement(Task.serializer(), task), mapOf("version" to if (createVersion) "true" else null), options)
    suspend fun touch(id: String, options: RequestOptions = RequestOptions()): Task = client.send("POST", "/api/tasks/${pathId(id)}/touch", options = options)
    suspend fun update(id: String, patch: JsonObject, options: RequestOptions = RequestOptions()): JsonObject = client.send("PATCH", "/api/tasks/${pathId(id)}", patch, options = options)
    suspend fun delete(id: String, options: RequestOptions = RequestOptions()): JsonObject = client.send("DELETE", "/api/tasks/${pathId(id)}", options = options)
    suspend fun versions(id: String, options: RequestOptions = RequestOptions()): List<TaskVersion> = client.get<JsonObject>("/api/tasks/${pathId(id)}/versions", options = options).getValue("versions").let { client.json.decodeFromJsonElement(ListSerializer(TaskVersion.serializer()), it) }
    suspend fun version(id: String, versionId: String, options: RequestOptions = RequestOptions()): JsonObject = client.get("/api/tasks/${pathId(id)}/versions/${pathId(versionId)}", options = options)
    suspend fun clearVersions(id: String, options: RequestOptions = RequestOptions()): JsonObject = client.send("POST", "/api/tasks/${pathId(id)}/versions/clear", options = options)
    suspend fun rollback(id: String, versionId: String, options: RequestOptions = RequestOptions()): Task = client.send("POST", "/api/tasks/${pathId(id)}/rollback", jsonObject("versionId" to string(versionId)), options = options)
    suspend inline fun <reified T> run(id: String, input: ExecuteTaskOptions = ExecuteTaskOptions(), options: RequestOptions = RequestOptions()): ExecutionResult<T> = client.send("POST", "/tasks/${pathId(id)}/api", client.json.encodeToJsonElement(ExecuteTaskOptions.serializer(), input), options = options)
}

class ExecutionsResource internal constructor(private val client: Figranium) {
    suspend fun list(apiKeyRoute: Boolean? = null, options: RequestOptions = RequestOptions()): List<Execution> = client.get<JsonObject>(if (apiKeyRoute ?: !client.usesSession) "/api/executions/list" else "/api/executions", options = options).getValue("executions").let { client.json.decodeFromJsonElement(ListSerializer(Execution.serializer()), it) }
    suspend fun get(id: String, options: RequestOptions = RequestOptions()): Execution = client.get<JsonObject>("/api/executions/${pathId(id)}", options = options).getValue("execution").let { client.json.decodeFromJsonElement(Execution.serializer(), it) }
    /** Fetches the complete stored result; API keys need results:read and Task access. */
    suspend fun getFullResult(id: String, options: RequestOptions = RequestOptions()): JsonElement = client.get<JsonObject>("/api/executions/${pathId(id)}/result", options = options).getValue("result")
    suspend fun delete(id: String, options: RequestOptions = RequestOptions()): JsonObject = client.send("DELETE", "/api/executions/${pathId(id)}", options = options)
    suspend fun clear(options: RequestOptions = RequestOptions()): JsonObject = client.send("POST", "/api/executions/clear", options = options)
    suspend fun stop(runId: String, options: RequestOptions = RequestOptions()): JsonObject = client.send("POST", "/api/executions/stop", jsonObject("runId" to string(runId)), options = options)
    /** Streams events for a run; requires a workspace session, not an API key. */
    fun stream(runId: String, options: RequestOptions = RequestOptions()): Flow<StreamEvent<JsonElement>> = client.stream("/api/executions/stream", mapOf("runId" to runId), options)
    fun watch(executionId: String, intervalMillis: Long = 1_000, options: RequestOptions = RequestOptions()): Flow<Execution> = flow { var previous: String? = null; while (true) { val execution = get(executionId, options); val status = execution.status; if (status != previous) { emit(execution); previous = status }; if ((execution.outcome ?: status) in TERMINAL_OUTCOMES) break; delay(intervalMillis) } }
}

class SchedulesResource internal constructor(private val client: Figranium) {
    suspend fun list(options: RequestOptions = RequestOptions()): JsonObject = client.get("/api/schedules", options = options)
    suspend fun set(taskId: String, schedule: Schedule, options: RequestOptions = RequestOptions()): JsonObject = client.send("POST", "/api/schedules/${pathId(taskId)}", client.json.encodeToJsonElement(Schedule.serializer(), schedule), options = options)
    suspend fun delete(taskId: String, options: RequestOptions = RequestOptions()): JsonObject = client.send("DELETE", "/api/schedules/${pathId(taskId)}", options = options)
    suspend fun status(taskId: String, options: RequestOptions = RequestOptions()): JsonObject = client.get("/api/schedules/${pathId(taskId)}/status", options = options)
    suspend fun describe(taskId: String, schedule: Schedule, options: RequestOptions = RequestOptions()): JsonObject = client.send("POST", "/api/schedules/${pathId(taskId)}/describe", client.json.encodeToJsonElement(Schedule.serializer(), schedule), options = options)
    suspend fun overallStatus(options: RequestOptions = RequestOptions()): JsonObject = client.get("/api/schedules/status/all", options = options)
}

class CapturesResource internal constructor(private val client: Figranium) {
    suspend fun list(runId: String? = null, options: RequestOptions = RequestOptions()): List<Capture> = client.get<JsonObject>("/api/data/captures", mapOf("runId" to runId), options).getValue("captures").let { client.json.decodeFromJsonElement(ListSerializer(Capture.serializer()), it) }
    suspend fun screenshots(options: RequestOptions = RequestOptions()): List<Capture> = client.get<JsonObject>("/api/data/screenshots", options = options).getValue("screenshots").let { client.json.decodeFromJsonElement(ListSerializer(Capture.serializer()), it) }
    suspend fun delete(name: String, options: RequestOptions = RequestOptions()): JsonObject = client.send("DELETE", "/api/data/captures/${pathId(name)}", options = options)
    suspend fun clear(options: RequestOptions = RequestOptions()): JsonObject = client.send("POST", "/api/data/clear-screenshots", options = options)
}

class CabinetsResource internal constructor(private val client: Figranium) {
    suspend fun list(options: RequestOptions = RequestOptions()): List<Cabinet> = client.get<JsonObject>("/api/cabinets", options = options).getValue("cabinets").let { client.json.decodeFromJsonElement(ListSerializer(Cabinet.serializer()), it) }
    suspend fun create(name: String, options: RequestOptions = RequestOptions()): Cabinet = client.send("POST", "/api/cabinets", jsonObject("name" to string(name)), options = options)
    suspend fun rename(id: String, name: String, options: RequestOptions = RequestOptions()): Cabinet = client.send("PATCH", "/api/cabinets/${pathId(id)}", jsonObject("name" to string(name)), options = options)
    suspend fun delete(id: String, targetCabinetId: String? = null, migrate: Boolean = false, options: RequestOptions = RequestOptions()): JsonObject = client.send("DELETE", "/api/cabinets/${pathId(id)}", jsonObject("targetCabinetId" to string(targetCabinetId), "mode" to if (migrate) JsonPrimitive("migrate") else null), options = options)
    suspend fun listItems(id: String, options: RequestOptions = RequestOptions()): List<CabinetItem> = client.get<JsonObject>("/api/cabinets/${pathId(id)}/items", options = options).getValue("items").let { client.json.decodeFromJsonElement(ListSerializer(CabinetItem.serializer()), it) }
    suspend fun clear(id: String, options: RequestOptions = RequestOptions()): JsonObject = client.send("POST", "/api/cabinets/${pathId(id)}/clear", options = options)
    suspend fun setItemStatus(cabinetId: String, itemIds: List<String>, status: String, options: RequestOptions = RequestOptions()): List<CabinetItem> = client.send<JsonObject>("PATCH", "/api/cabinets/${pathId(cabinetId)}/items/status", jsonObject("itemIds" to client.json.encodeToJsonElement(ListSerializer(String.serializer()), itemIds), "status" to string(status)), options = options).getValue("items").let { client.json.decodeFromJsonElement(ListSerializer(CabinetItem.serializer()), it) }
    suspend fun removeItems(cabinetId: String, itemIds: List<String>, options: RequestOptions = RequestOptions()): JsonObject = client.send("DELETE", "/api/cabinets/${pathId(cabinetId)}/items", jsonObject("itemIds" to client.json.encodeToJsonElement(ListSerializer(String.serializer()), itemIds)), options = options)
    suspend fun zipItems(cabinetId: String, itemIds: List<String>, name: String? = null, options: RequestOptions = RequestOptions()): CabinetItem = client.send("POST", "/api/cabinets/${pathId(cabinetId)}/zip", jsonObject("itemIds" to client.json.encodeToJsonElement(ListSerializer(String.serializer()), itemIds), "name" to string(name)), options = options)
    suspend fun unzipItem(cabinetId: String, itemId: String, options: RequestOptions = RequestOptions()): CabinetItem = client.send("POST", "/api/cabinets/${pathId(cabinetId)}/items/${pathId(itemId)}/unzip", options = options)
    fun downloadUrl(cabinetId: String, itemId: String): String = client.baseUrl.newBuilder().addPathSegments("api/cabinets/${pathId(cabinetId)}/items/${pathId(itemId)}/download").build().toString()
}




class ExecutionResource internal constructor(@PublishedApi internal val client: Figranium) {
    suspend inline fun <reified T> scrape(input: JSONObject, options: RequestOptions = RequestOptions()): ExecutionResult<T> = client.send("POST", "/scrape", JsonObject(input), options = options)
    suspend inline fun <reified T> agent(input: JSONObject, options: RequestOptions = RequestOptions()): ExecutionResult<T> = client.send("POST", "/agent", JsonObject(input), options = options)
    suspend inline fun <reified T> headful(input: JSONObject, options: RequestOptions = RequestOptions()): ExecutionResult<T> = client.send("POST", "/headful", JsonObject(input), options = options)
}
class HealthResource internal constructor(private val client: Figranium) { suspend fun check(options: RequestOptions = RequestOptions()): HealthStatus = client.get("/api/health", options = options) }
private val TERMINAL_OUTCOMES = setOf("success", "error", "stopped", "crashed", "anti_bot")
