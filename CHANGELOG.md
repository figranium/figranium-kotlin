# Changelog

## Unreleased

- Added session-only `tasks.deleteVersion(id, versionId)` for Figranium v0.21 Task version deletion, with response, encoding and CSRF regression coverage.
- Corrected Cabinet list decoding for the v0.21 `{ defaultCabinetId, cabinets }` response envelope.
- Corrected Cabinet ZIP extraction to decode a single `CabinetItem` rather than an `items` wrapper.
- Added the optional `unuploadedCount` Cabinet field and response-shape regression tests.
- Fixed no-payload POST/PUT/PATCH requests to send `{}` instead of an invalid null OkHttp request body (including Cabinet unzip and Task touch).

## [0.3.0] - 2026-10-09

- Removed remote authentication, credentials, settings, and interactive browser administration resources.
- Removed legacy cookie administration from captures.
- Preserved automation tasks, executions, schedules, templates, cabinets, captures, and health checks.


All notable changes to Figranium Kotlin are documented in this file.

## [0.2.0] - 2026-10-03

### Added

- v0.20 Templates API client with catalog listing, paginated search, detail retrieval and explicit successful-import tracking.
- Template import tracking preserves server-side per-installation counting.



## [0.1.0] - 2026-09-29

### Added

- Coroutine-first Kotlin and Android client for Figranium, with API-key/session authentication, timeouts, structured `FigraniumException` failures, and SSE `Flow`s.
- Typed models and action builders for every current Figranium task action and resource.
- Resource clients for tasks, executions, schedules, captures, cabinets, credentials, browser sessions, settings, auth, direct execution, and health.
- `figranium-appfunctions`, an optional Android 16+ AppFunctions integration for allowlisted task execution by system assistants and agents.
- Maven Central publication, signing, sources, and POM configuration.
