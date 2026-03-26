# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Install

```bash
# JAVA_HOME must be set (sdkman)
export JAVA_HOME=/home/alex/.sdkman/candidates/java/current

# Build debug APK
./gradlew assembleDebug

# Install on connected device
sudo adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No test infrastructure exists yet. No linter is configured.

## Architecture

Single-activity MVVM app using Jetpack Compose, Hilt DI, and the Google Tasks API. Displays overdue + today tasks with completion tracking.

**Data flow:** Compose UI → ViewModel (StateFlow) → Repository → Room (local cache) + Google Tasks API (remote)

**Key layers:**
- `data/remote/GoogleTasksApi` — OAuth2-authenticated REST client using Google's official Tasks API client library. All calls run on `Dispatchers.IO`.
- `data/repository/TaskRepository` — Single source of truth. Sync fetches all lists, filters to `due <= today` or `completedToday`, upserts into Room, deletes stale entries. Toggle/postpone use optimistic updates with revert on API failure.
- `data/repository/SettingsRepository` — DataStore preferences for app settings, widget settings, and share list config. Two DataStore files: `taskig_prefs` (auth, sync) and `taskig_settings` (UI preferences).
- `data/local/TaskEntity` — Flat Room table. Each task carries its `listId`, `listTitle`, and `listColor` (deterministic palette from listId hash). No separate lists table.
- `widget/TaskigWidget` — Jetpack Glance widget with its own `PreferencesGlanceStateDefinition`. Uses a separate Room singleton (`getWidgetInstance`) to avoid contention with the main app. Settings are synced from app DataStore into Glance state via `updateAllWidgets()`.

**Navigation:** Compose Navigation with 3 routes: `signin` → `today` → `settings`. `AuthCheckViewModel` reads DataStore on startup to skip sign-in if already authenticated.

**Sync:** WorkManager periodic sync (15 min). `SyncWorker` calls `repository.sync()` then `updateAllWidgets()`. Widget refresh button calls `TaskRepository.syncForWidget()` directly (not via WorkManager) for instant updates.

**Share:** `ShareActivity` receives `ACTION_SEND` text intents, creates a task in the configured default list (no due date), shows a toast, finishes. Transparent theme, no UI.

## Localization

All user-facing strings are in `res/values/strings.xml` (English) and `res/values-sv/strings.xml` (Swedish). Always update both when adding strings.

## Widget

The Glance widget has its own state management separate from the app. When widget-related code changes:
1. Settings flow: app DataStore → `updateAllWidgets()` syncs into Glance preferences → `widget.update()` triggers re-render
2. Task data: widget reads directly from Room via `TaskDatabase.getWidgetInstance()`
3. Actions (toggle, refresh) run as `ActionCallback` suspend functions — can do DB/API work directly
