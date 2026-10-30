package dev.figranium.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Permissions accepted by the v0.21 scoped API-key endpoint. */
@Serializable enum class ApiKeyPermission {
    @SerialName("tasks:read") TASKS_READ,
    @SerialName("tasks:run") TASKS_RUN,
    @SerialName("tasks:manage") TASKS_MANAGE,
}

/** Public metadata only; the plaintext secret is returned once at creation. */
@Serializable data class ScopedApiKey(
    val id: String,
    val name: String,
    val permissions: List<ApiKeyPermission>,
    /** An empty allowlist means all Tasks. */
    val taskIds: List<String>,
    val createdAt: String,
    val legacy: Boolean? = null,
)

@Serializable data class ApiKeyListResponse(
    val keys: List<ScopedApiKey>,
    /** May contain a sensitive legacy plaintext secret. */
    val legacySecret: String? = null,
    val permissions: List<ApiKeyPermission>,
)

@Serializable data class ApiKeyCreateInput(
    val permissions: List<ApiKeyPermission>,
    val name: String? = null,
    /** Omitted or empty means all Tasks. */
    val taskIds: List<String>? = null,
)

@Serializable data class ApiKeyCreateResponse(val key: ScopedApiKey, val secret: String)
@Serializable data class ApiKeyRenameResponse(val key: ScopedApiKey)
@Serializable data class ApiKeyRevokeResponse(val ok: Boolean)

/** API-key administration requires an authenticated workspace session, never another API key. */
class ApiKeysResource internal constructor(private val client: Figranium) {
    private fun requireSession() {
        if (!client.usesSession) throw FigraniumException(
            code = "SESSION_REQUIRED",
            message = "API key administration requires a workspace session",
        )
    }

    suspend fun list(options: RequestOptions = RequestOptions()): ApiKeyListResponse {
        requireSession()
        return client.get("/api/api-keys", options = options)
    }

    suspend fun create(input: ApiKeyCreateInput, options: RequestOptions = RequestOptions()): ApiKeyCreateResponse {
        requireSession()
        return client.send("POST", "/api/api-keys", client.json.encodeToJsonElement(ApiKeyCreateInput.serializer(), input), options = options)
    }

    suspend fun rename(id: String, name: String, options: RequestOptions = RequestOptions()): ApiKeyRenameResponse {
        requireSession()
        return client.send("PATCH", "/api/api-keys/${pathId(id)}", buildJsonObject { put("name", name) }, options = options)
    }

    suspend fun revoke(id: String, options: RequestOptions = RequestOptions()): ApiKeyRevokeResponse {
        requireSession()
        return client.send("DELETE", "/api/api-keys/${pathId(id)}", options = options)
    }
}
