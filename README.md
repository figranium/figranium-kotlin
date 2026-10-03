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
    implementation("dev.figranium:figranium:0.2.0")
}
```

For Android AppFunctions support, add the optional integration artifact and KSP:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.2.20-2.0.3"
}

dependencies {
    implementation("dev.figranium:figranium-appfunctions:0.2.0")
    ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
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

## Streams and execution watching

```kotlin
client.executions.stream().collect { event ->
    println("${event.event ?: "message"}: ${event.data}")
}

client.executions.watch("execution-id").collect { execution ->
    println(execution.status ?: execution.outcome ?: "unknown")
}
```

`Flow` collection is cancellable, so streams close automatically with the calling coroutine.

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
| `templates` | Browse, search and retrieve templates; report successful imports |\n| `tasks` | Save, update, delete, version, generate, and execute tasks |
| `executions` | List, inspect, stop, delete, clear, stream, and watch runs |
| `schedules` | Configure, describe, disable, and inspect schedules |
| `captures` | List/delete recordings and screenshots; manage cookies |
| `cabinets` | Manage intercepted downloads and other files |
| `credentials` | Manage output credentials and browse Baserow metadata |
| `browser` | Open sessions, highlight selectors, inspect headful sessions, and stream selector events |
| `execution` | Direct `scrape`, `agent`, and `headful` execution endpoints |
| `settings` | Session-protected keys, AI providers/models, themes, user agents, and proxies |
| `auth` | Setup, login, logout, and current-user methods |
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
