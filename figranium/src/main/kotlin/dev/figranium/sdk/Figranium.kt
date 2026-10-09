package dev.figranium.sdk

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A normalized API or transport failure from Figranium. */
class FigraniumException(val status: Int = 0, val code: String? = null, val details: JsonElement? = null, val requestId: String? = null, message: String) : IOException(message)

sealed interface FigraniumAuthentication {
    data class ApiKey(val value: String, val header: String = "authorization") : FigraniumAuthentication
    data object Session : FigraniumAuthentication
    data object None : FigraniumAuthentication
}

/** Coroutine-first client for a self-hosted Figranium instance. */
class Figranium @JvmOverloads constructor(
    baseUrl: String = DEFAULT_BASE_URL,
    private val authentication: FigraniumAuthentication = FigraniumAuthentication.None,
    private val headers: Map<String, String> = emptyMap(),
    private val timeoutMillis: Long = 30_000,
    private val httpClient: OkHttpClient = OkHttpClient(),
    @PublishedApi internal val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false },
) {
    @PublishedApi internal val baseUrl: HttpUrl = baseUrl.trim().removeSuffix("/").toHttpUrl()
    val tasks = TasksResource(this); val executions = ExecutionsResource(this)
    val schedules = SchedulesResource(this); val captures = CapturesResource(this); val cabinets = CabinetsResource(this)
    
    val execution = ExecutionResource(this); val health = HealthResource(this); val templates = TemplatesResource(this)

    suspend inline fun <reified T> runTask(id: String, input: ExecuteTaskOptions = ExecuteTaskOptions(), options: RequestOptions = RequestOptions()): ExecutionResult<T> = tasks.run(id, input, options)
    suspend inline fun <reified T> scrape(input: JSONObject, options: RequestOptions = RequestOptions()): ExecutionResult<T> = execution.scrape(input, options)
    suspend inline fun <reified T> agent(input: JSONObject, options: RequestOptions = RequestOptions()): ExecutionResult<T> = execution.agent(input, options)
    suspend inline fun <reified T> headful(input: JSONObject, options: RequestOptions = RequestOptions()): ExecutionResult<T> = execution.headful(input, options)

    @PublishedApi internal suspend fun <T> request(method: String, path: String, serializer: KSerializer<T>, body: JsonElement? = null, query: Map<String, String?> = emptyMap(), options: RequestOptions = RequestOptions()): T {
        val url = baseUrl.newBuilder().addPathSegments(path.trim('/')).apply { query.forEach { (key, value) -> if (!value.isNullOrEmpty()) addQueryParameter(key, value) } }.build()
        val request = Request.Builder().url(url).method(method, body?.let { json.encodeToString(JsonElement.serializer(), it).toRequestBody(JSON_MEDIA_TYPE) }).apply {
            header("accept", "application/json")
            if (body != null) header("content-type", "application/json")
            headers.forEach { (key, value) -> header(key, value) }; options.headers.forEach { (key, value) -> header(key, value) }
            if (authentication is FigraniumAuthentication.ApiKey) header(authentication.header, if (authentication.header.equals("authorization", true)) "Bearer ${authentication.value}" else authentication.value)
        }.build()
        val client = if (options.timeoutMillis == null) httpClient.newBuilder().callTimeout(timeoutMillis, TimeUnit.MILLISECONDS).build() else httpClient.newBuilder().callTimeout(options.timeoutMillis, TimeUnit.MILLISECONDS).build()
        val response = client.newCall(request).await()
        response.use {
            val raw = it.body.string()
            if (!it.isSuccessful) throw error(raw, it)
            if (raw.isBlank() || it.code == 204) throw FigraniumException(it.code, "EMPTY_RESPONSE", message = "Figranium returned an empty response")
            return try { json.decodeFromString(serializer, raw) } catch (cause: Exception) { throw FigraniumException(it.code, "INVALID_RESPONSE", message = "Figranium returned an invalid response: ${cause.message}") }
        }
    }

    internal fun stream(path: String, options: RequestOptions = RequestOptions()): Flow<StreamEvent<JsonElement>> = callbackFlow {
        val request = Request.Builder().url(baseUrl.newBuilder().addPathSegments(path.trim('/')).build()).header("accept", "text/event-stream").apply {
            headers.forEach { (key, value) -> header(key, value) }; options.headers.forEach { (key, value) -> header(key, value) }
            if (authentication is FigraniumAuthentication.ApiKey) header(authentication.header, if (authentication.header.equals("authorization", true)) "Bearer ${authentication.value}" else authentication.value)
        }.build()
        val call = httpClient.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build().newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { close(FigraniumException(code = "NETWORK_ERROR", message = "Unable to open Figranium stream: ${e.message}")) }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) { close(error(it.body.string(), it)); return }
                    val lines = mutableListOf<String>()
                    try { it.body.charStream().buffered().forEachLine { line -> if (line.isEmpty()) { parseSse(lines)?.let(::trySend); lines.clear() } else lines += line }; parseSse(lines)?.let(::trySend); close() }
                    catch (e: IOException) { close(e) }
                }
            }
        })
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    private fun parseSse(lines: List<String>): StreamEvent<JsonElement>? {
        val raw = lines.filter { it.startsWith("data:") }.joinToString("\n") { it.removePrefix("data:").trimStart() }
        if (raw.isBlank()) return null
        val data = runCatching { json.parseToJsonElement(raw) }.getOrElse { JsonPrimitive(raw) }
        return StreamEvent(data, lines.firstOrNull { it.startsWith("event:") }?.removePrefix("event:")?.trim(), lines.firstOrNull { it.startsWith("id:") }?.removePrefix("id:")?.trim(), lines.firstOrNull { it.startsWith("retry:") }?.removePrefix("retry:")?.trim()?.toIntOrNull(), raw)
    }
    private fun error(raw: String, response: Response): FigraniumException {
        val obj = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        val message = obj?.get("message")?.jsonPrimitiveOrNull() ?: obj?.get("error")?.jsonPrimitiveOrNull() ?: raw.ifBlank { "Figranium request failed" }
        return FigraniumException(response.code, obj?.get("error")?.jsonPrimitiveOrNull(), obj?.get("details") ?: obj?.get("detail"), response.header("x-request-id"), message)
    }
    companion object { const val DEFAULT_BASE_URL = "http://localhost:11345"; private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType() }
}
private fun JsonElement.jsonPrimitiveOrNull() = (this as? JsonPrimitive)?.contentOrNull
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation -> enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }; override fun onResponse(call: Call, response: Response) { continuation.resume(response) } }); continuation.invokeOnCancellation { cancel() } }
