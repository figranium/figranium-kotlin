package dev.figranium.sdk

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Figranium v0.20 template catalog; tracking is separate from saving a task. */
class TemplatesResource(private val client: Figranium) {
    @Serializable data class Page(val items: List<JsonElement>, val total: Int)

    suspend fun list(options: RequestOptions = RequestOptions()): List<JsonElement> =
        client.request("GET", "/api/templates", kotlinx.serialization.builtins.ListSerializer(JsonElement.serializer()), options = options)

    suspend fun search(limit: Int = 12, offset: Int = 0, sort: String = "popular",
                       category: String = "all", search: String = "",
                       options: RequestOptions = RequestOptions()): Page =
        client.request("GET", "/api/templates", Page.serializer(),
            query = mapOf("limit" to limit.toString(), "offset" to offset.toString(),
                          "sort" to sort, "category" to category, "search" to search), options = options)

    suspend fun get(id: String, options: RequestOptions = RequestOptions()): JsonElement =
        client.request("GET", "/api/templates/${encodeTemplateId(id)}", JsonElement.serializer(), options = options)

    /** Call only after a successful local import; each instance contributes once per template. */
    suspend fun recordImport(id: String, options: RequestOptions = RequestOptions()): JsonElement =
        client.request("POST", "/api/templates/${encodeTemplateId(id)}/import", JsonElement.serializer(), options = options)

    private fun encodeTemplateId(id: String): String {
        require(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(id)) {
            "Invalid template ID"
        }
        return id
    }
}
