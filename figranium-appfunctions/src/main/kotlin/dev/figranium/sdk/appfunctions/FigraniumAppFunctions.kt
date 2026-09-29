@file:Suppress("UnstableApiUsage")

package dev.figranium.sdk.appfunctions

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunction
import dev.figranium.sdk.Figranium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/** Supplies an app-owned client. Store credentials in Android Keystore-backed storage, never as AppFunction parameters. */
fun interface FigraniumAppFunctionClientProvider { fun client(): Figranium }

/** Configure once from [android.app.Application.onCreate] before enabling Figranium functions. */
object FigraniumAppFunctionConfiguration {
    @Volatile private var provider: FigraniumAppFunctionClientProvider? = null
    fun configure(clientProvider: FigraniumAppFunctionClientProvider) { provider = clientProvider }
    internal fun client(): Figranium = checkNotNull(provider) { "Configure FigraniumAppFunctionConfiguration before invoking Figranium AppFunctions." }.client()
}

@AppFunctionSerializable
data class RunFigraniumTaskParameters(
    /** The ID of an app-approved deterministic Figranium task. */
    val taskId: String,
)

@AppFunctionSerializable
data class FigraniumAppFunctionResult(
    /** The Figranium execution outcome, such as success or error. */
    val outcome: String,
    /** JSON output from the approved task. */
    val data: String? = null,
    /** The run ID, if returned by Figranium. */
    val runId: String? = null,
)

/**
 * Android's AppFunctions equivalent to the Swift SDK's App Intents and Foundation Models tool.
 *
 * This service is disabled by default. Its allowlist means an assistant can run only the tasks
 * your app explicitly approves—it cannot create arbitrary browser actions or read credentials.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(serviceName = "FigraniumAppFunctionService", appFunctionXmlFileName = "figranium_app_functions")
abstract class BaseFigraniumAppFunctionService : AppFunctionService() {
    /** Runs one explicitly approved deterministic Figranium task. */
    @AppFunction(isEnabled = false, isDescribedByKDoc = true)
    suspend fun runFigraniumTask(parameters: RunFigraniumTaskParameters): FigraniumAppFunctionResult = withContext(Dispatchers.IO) {
        check(FigraniumAppFunctions.allowedTaskIds().contains(parameters.taskId)) { "This task is not approved for AppFunctions execution." }
        val result = FigraniumAppFunctionConfiguration.client().runTask<JsonObject>(parameters.taskId)
        FigraniumAppFunctionResult(result.outcome ?: "unknown", result.data?.toString(), result.runId)
    }
}

/** Controls the SDK AppFunction's explicit task allowlist and availability. */
object FigraniumAppFunctions {
    @Volatile private var allowedIds: Set<String> = emptySet()
    fun configure(allowedTaskIds: Set<String>) { allowedIds = allowedTaskIds.toSet() }
    fun allowedTaskIds(): Set<String> = allowedIds

    /** Enables or disables the generated AppFunction. Safe on unsupported Android versions. */
    suspend fun setEnabled(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT < 36) return
        AppFunctionManager.getInstance(context)?.setAppFunctionEnabled(
            BaseFigraniumAppFunctionServiceIds.RUN_FIGRANIUM_TASK_ID,
            if (enabled) AppFunctionManager.APP_FUNCTION_STATE_ENABLED else AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
        )
    }
}
