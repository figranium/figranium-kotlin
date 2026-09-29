package dev.figranium.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

typealias JSONObject = Map<String, JsonElement>
typealias RuntimeVariables = Map<String, JsonElement>
typealias TaskMode = String
typealias TaskOutcome = String

data class RequestOptions(val headers: Map<String, String> = emptyMap(), val timeoutMillis: Long? = null)

@Serializable data class TaskVariable(val type: String, val value: JsonElement, val autoCreated: Boolean? = null)
@Serializable data class StealthConfig(
    val allowTypos: Boolean? = null, val idleMovements: Boolean? = null, val overscroll: Boolean? = null,
    val deadClicks: Boolean? = null, val fatigue: Boolean? = null, val naturalTyping: Boolean? = null,
    val cursorGlide: Boolean? = null, val randomizeClicks: Boolean? = null,
)
@Serializable data class TaskTranslation(val enabled: Boolean, val targetLanguage: String)
@Serializable data class Schedule(
    val enabled: Boolean, val frequency: String? = null, val intervalMinutes: Int? = null, val hour: Int? = null,
    val minute: Int? = null, val daysOfWeek: List<Int>? = null, val dayOfMonth: Int? = null, val cron: String? = null,
    val lastRun: Double? = null, val lastRunStatus: String? = null, val lastRunDurationMs: Double? = null, val nextRun: Double? = null,
)
@Serializable data class TaskOutput(val provider: String = "baserow", val credentialId: String, val tableId: String, val onError: String? = null)

/** Every public action field from Figranium's task API. Prefer [Actions] for common actions. */
@Serializable data class Action(
    val type: String, val id: String? = null, val disabled: Boolean? = null, val selector: String? = null,
    val targetSelector: String? = null, val value: String? = null, val clickType: String? = null,
    val typeMode: String? = null, val key: String? = null, val varName: String? = null, val method: String? = null,
    val headers: String? = null, val body: String? = null, val conditionVar: String? = null,
    val conditionVarType: String? = null, val conditionOp: String? = null, val conditionValue: String? = null,
    val captchaType: String? = null, val timeout: Double? = null, val cabinetId: String? = null, val markAsUploaded: Boolean? = null,
)

@Serializable data class Task(
    val name: String, val url: String, val mode: String, val description: String = "", val id: String? = null,
    val wait: Double? = null, val selector: String? = null, val rotateUserAgents: Boolean? = null,
    val rotateProxies: Boolean? = null, val rotateViewport: Boolean? = null, val humanTyping: Boolean? = null,
    val stealth: StealthConfig? = null, val autoSolveCaptcha: Boolean? = null, val translation: TaskTranslation? = null,
    val actions: List<Action>? = null, val variables: Map<String, TaskVariable>? = null, val schedule: Schedule? = null,
    val output: TaskOutput? = null, val extractionScript: String? = null, val extractionFormat: String? = null,
    val includeHtml: Boolean? = null, val includeShadowDom: Boolean? = null, val disableRecording: Boolean? = null,
    val statelessExecution: Boolean? = null, val downloadCabinetId: String? = null, val cabinetId: String? = null,
)

@Serializable data class ExecuteTaskOptions(val variables: RuntimeVariables? = null, val taskVariables: RuntimeVariables? = null, val webhookUrl: String? = null, val runId: String? = null)
@Serializable data class ExecutionResult<T>(val data: T? = null, val outcome: String? = null, val success: Boolean? = null, val error: String? = null, val runId: String? = null)
data class StreamEvent<T>(val data: T, val event: String? = null, val id: String? = null, val retry: Int? = null, val raw: String)
@Serializable data class Execution(val id: String, val timestamp: Double, val method: String? = null, val path: String? = null, val status: String? = null, val outcome: String? = null, val durationMs: Double? = null, val source: String? = null, val mode: String? = null, val taskId: String? = null, val taskName: String? = null, val url: String? = null, val result: JsonElement? = null)
@Serializable data class TaskSummary(val id: String, val name: String, val description: String? = null)
@Serializable data class TaskVersion(val id: String, val timestamp: Double, val name: String? = null, val mode: String? = null)
@Serializable data class Cabinet(val id: String, val name: String, val isDefault: Boolean? = null, val itemCount: Int? = null, val createdAt: Double? = null)
@Serializable data class CabinetItem(val id: String, val name: String, val kind: String, val status: String, val size: Int? = null, val createdAt: Double? = null, val sourceTaskId: String? = null, val sourceRunId: String? = null)
@Serializable data class Capture(val name: String, val url: String, val size: Int, val modified: Double, val type: String)
@Serializable data class CredentialInput(val name: String, val provider: String = "baserow", val config: Map<String, String>)
@Serializable data class Credential(val id: String, val name: String, val provider: String, val config: Map<String, String>)
@Serializable data class BrowserSession(val sessionId: String, val status: String, val wsEndpoint: String? = null)
@Serializable data class SelectorCandidate(val css: String, val xpath: String? = null, val confidence: Double? = null)
@Serializable data class HealthStatus(val status: String, val version: String? = null)
@Serializable data class User(val id: String, val name: String? = null, val email: String? = null)
@Serializable data class ProxyInput(val server: String, val username: String? = null, val password: String? = null, val label: String? = null, val isRotatingPool: Boolean? = null, val estimatedPoolSize: Int? = null)
