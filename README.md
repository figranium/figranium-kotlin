<div align="center">
  <img src="https://raw.githubusercontent.com/figranium/figranium-kotlin/main/banner.png" alt="Figranium Banner">

  <h1>Figranium Kotlin SDK</h1>

  <a href="https://central.sonatype.com/artifact/dev.figranium/figranium" target="_blank"><img src="https://img.shields.io/maven-central/v/dev.figranium/figranium?style=for-the-badge&label=Maven%20Central&logo=apachemaven&logoColor=white" alt="Maven Central version"></a>
  <a href="https://developer.android.com/ai/appfunctions" target="_blank"><img src="https://img.shields.io/badge/Android-API%2023%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android API 23+"></a>

  <p><strong>Official Kotlin and Android SDK for Figranium, the self-hosted browser automation and web-scraping platform.</strong></p>

  <p><a href="https://figranium.dev/docs/sdk/kotlin" target="_blank"><strong>Documentation</strong></a></p>
</div>

- Coroutine-first API built on OkHttp
- Code-defined Figranium tasks and action builders
- Kotlin serialization request and response models
- API-key/session authentication, timeouts, structured errors, and SSE flows
- Optional Android AppFunctions integration for system assistants and agents
- Maven Central-ready publication with signing and sources

## Requirements

- Kotlin 2.2+
- Android API 23+ (Android API 36+ for AppFunctions)
- A running Figranium instance

## Install

```kotlin
dependencies {
    implementation("dev.figranium:figranium:0.3.0")
}
```

For Android AppFunctions support, add the optional integration artifact and KSP:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.2.20-2.0.3"
}

dependencies {
    implementation("dev.figranium:figranium-appfunctions:0.3.0")
    ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-beta01")
}
```

## Quick start

```kotlin
val client = Figranium(
    baseUrl = "http://localhost:11345",
    authentication = FigraniumAuthentication.ApiKey("YOUR_FIGRANIUM_API_KEY"),
)

val tasks = client.tasks.listSummaries()
val result = client.runTask<JsonElement>(
    tasks.first().id,
    ExecuteTaskOptions(variables = mapOf("query" to JsonPrimitive("Kotlin SDK"))),
)

println(result.data)
println(result.outcome) // success | error | stopped | crashed | anti_bot
```

`baseUrl` defaults to `http://localhost:11345`. API keys use `Authorization: Bearer`; use `FigraniumAuthentication.ApiKey(key, "x-api-key")` for deployments that expect that header.

`tasks.listSummaries()` uses `/api/tasks/list` with API keys and `/api/tasks` with authenticated workspace sessions. Figranium's retired AI selector/script-generation endpoints are not exposed by the SDK.

## Define a task in Kotlin

```kotlin
val task = Task(
    name = "Search and extract",
    url = "https://example.com",
    mode = "agent",
    description = "Searches a page and captures visible results",
    actions = listOf(
        Actions.waitFor("#search"),
        Actions.type("#search", variable("query")),
        Actions.press("Enter", "#search"),
        Actions.waitFor(".results"),
        Actions.getContent(".results", "resultText"),
    ),
    variables = mapOf("query" to TaskVariable("string", JsonPrimitive("figranium"))),
)

val saved = client.tasks.save(task)
val result = client.runTask<JsonElement>(saved.id!!)
```

`variable("query")` creates Figranium’s `{$query}` template token. Action builders assign unique action IDs automatically.

For Figranium v0.21+, a Task can select a saved cookie state or request a fresh one. Kotlin uses `JsonElement?` to distinguish all three wire formats:

```kotlin
val inherited = task.copy(cookieStateId = null) // omitted; instance default
val fresh = task.copy(cookieStateId = JsonNull) // explicit JSON null
val named = task.copy(cookieStateId = JsonPrimitive("cookie-state-id"))

val baserowTask = named.copy(output = TaskOutput(
    credentialId = "baserow-credential-id",
    tableId = "table-id",
    databaseId = "database-id",
    dedicated = true,
))
```

The legacy `statelessExecution` field is deprecated. Scoped API keys must have the relevant Task permissions and cannot bypass Task allowlists. Cookie-state administration is session-only; the SDK can use an already authenticated OkHttp client but does not perform interactive login:

```kotlin
val sessionClient = Figranium(
    baseUrl = "https://figranium.example",
    authentication = FigraniumAuthentication.Session,
    httpClient = authenticatedOkHttpClient, // supply workspace cookies via CookieJar
)
val states = sessionClient.cookieStates.list()
val created = sessionClient.cookieStates.create(
    "login",
    kotlinx.serialization.json.buildJsonObject {
        put("cookies", kotlinx.serialization.json.JsonArray(emptyList()))
        put("origins", kotlinx.serialization.json.JsonArray(emptyList()))
    },
)
```

Session-authenticated mutations automatically include the CSRF hint `X-Requested-With: XMLHttpRequest` unless an Origin, Referer, or X-Requested-With header is supplied. Scoped API keys cannot administer cookie states. Manage scoped keys, 1Password credentials, Baserow connections, imports/exports and Cloud sign-in in the authenticated Figranium workspace UI.

## Streams and execution watching

```kotlin
client.executions.stream("run-id").collect { event ->
    println("${event.event ?: "message"}: ${event.data}")
}

client.executions.watch("execution-id").collect { execution ->
    println(execution.status ?: execution.outcome ?: "unknown")
}
```

`Flow` collection is cancellable, so streams close automatically with the calling coroutine. The execution stream requires a workspace session and a run ID; an API key alone cannot open `/api/executions/stream`.

`client.executions.list()` selects the workspace-session route when authenticated with `Session`, or the `results:read` API-key route otherwise. Set `apiKeyRoute` explicitly to override it. The server's numeric HTTP execution statuses are decoded as strings for compatibility. `client.executions.getFullResult("execution-id")` retrieves a retained complete result as `JsonElement`; scoped API keys need `results:read` and access to that execution's Task.

## Error handling

```kotlin
try {
    client.runTask<JsonElement>("missing-task")
} catch (error: FigraniumException) {
    println(error.status)    // HTTP status, or 0 for transport failures
    println(error.code)
    println(error.details)
    println(error.requestId)
}
```

Pass `RequestOptions(headers = ..., timeoutMillis = ...)` to any resource call to override request settings.

## Client resources

| Resource | Purpose |
| --- | --- |
| `templates` | Browse, search and retrieve templates; report successful imports |
| `tasks` | Save, update, delete, version, and execute tasks |
| `cookieStates` | Session-only list/get/create/rename/update/delete browser storage states |
| `executions` | List, inspect, stop, delete, clear, stream, and watch runs |
| `schedules` | Configure, describe, disable, and inspect schedules |
| `captures` | List/delete recordings and screenshots |
| `cabinets` | Manage intercepted downloads and other files |
| `execution` | Direct `scrape`, `agent`, and `headful` execution endpoints |
| `health` | Service health check |

## Android AppFunctions

AppFunctions is Android’s equivalent of App Intents: it lets Android assistants and agents discover narrow, app-owned capabilities. Configure it at launch, keep keys in Android Keystore-backed storage, and enable only known deterministic task IDs:

```kotlin
FigraniumAppFunctionConfiguration.configure {
    Figranium("https://figranium.example", FigraniumAuthentication.ApiKey(loadKeyFromKeystore()))
}
FigraniumAppFunctions.configure(setOf("lookup-order", "check-inventory"))
FigraniumAppFunctions.setEnabled(applicationContext, enabled = true)
```

The AppFunction is disabled by default and enforces the allowlist. It cannot construct arbitrary browser actions or receive Figranium credentials as a parameter. Android AppFunctions require Android 16/API 36+; the base SDK does not.

## Publishing

The Gradle build is configured for Maven Central Portal publishing. Set `mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey`, and `signingInMemoryKeyPassword` in `~/.gradle/gradle.properties` or CI secrets, then run:

```bash
./gradlew publishToMavenCentral
```

## License

Apache-2.0
