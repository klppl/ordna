# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Install

```bash
# JAVA_HOME must be set (sdkman)
export JAVA_HOME=/home/alex/.sdkman/candidates/java/current

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No test infrastructure exists yet. No linter is configured.

## Signing

Both debug and release builds use the same release keystore so APKs are interchangeable on device (no signature mismatch). The signing config reads from `keystore.properties` in the project root (gitignored). On a new machine, create this file:

```properties
storeFile=/path/to/ordna-release.keystore
storePassword=<password>
keyAlias=ordna
keyPassword=<password>
```

The keystore must be the same one registered in Google Cloud Console for the Google Tasks API OAuth client (SHA1 fingerprint). Without `keystore.properties`, builds will be unsigned and Google sign-in won't work.

## CI/CD

GitHub Actions workflows (manual dispatch):
- **build.yml** — builds debug APK, uploads as artifact
- **release.yml** — builds release APK, creates GitHub Release with tag. Takes `version_name` and `version_code` as inputs. Requires secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Architecture

Single-activity MVVM app using Jetpack Compose, Hilt DI, and the Google Tasks API. Package: `io.github.klppl.ordna`. Displays overdue + today tasks with completion tracking.

**Data flow:** Compose UI → ViewModel (StateFlow) → Repository → Room (local cache) + Google Tasks API (remote)

**Key layers:**
- `data/remote/GoogleTasksApi` — OAuth2-authenticated REST client using Google's official Tasks API client library. All calls run on `Dispatchers.IO`.
- `data/repository/TaskRepository` — Single source of truth. Sync fetches all lists, filters to `due <= today` or `completedToday`, upserts into Room, deletes stale entries. Toggle/postpone/notes use optimistic updates with revert on API failure. Caches task lists in a `MutableStateFlow`.
- `data/repository/SettingsRepository` — DataStore preferences for app settings, widget settings, reminders, and share list config. Two DataStore files: `ordna_prefs` (account email, sync timestamp) and `ordna_settings` (all UI/widget/reminder preferences).
- `data/local/TaskEntity` — Flat Room table (`ordna.db`, version 4, destructive migration). Each task carries its `listId`, `listTitle`, and `listColor` (deterministic 8-color palette from listId hash). No separate lists table. Indexed on `(status, due)` and `(status, completedAt)`.
- `data/sync/SyncWorker` — WorkManager periodic sync (15 min, network required). Enqueued in `OrdnaApplication.onCreate()`.
- `data/sync/ReminderWorker` + `ReminderScheduler` — Notification reminders with 3 configurable time slots (morning/midday/evening). Scheduled as OneTimeWork with daily rescheduling. `BootReceiver` reschedules on device reboot.

**Navigation:** Compose Navigation with 3 routes: `signin` → `today` → `settings`. `AuthCheckViewModel` reads DataStore on startup to skip sign-in if already authenticated.

**Widgets:** Two Jetpack Glance widgets:
- `widget/OrdnaWidget` — Full task list widget (4x4, resizable). Shows overdue/today/completed sections, progress bar, streak. Refresh button calls `TaskRepository.syncForWidget()` directly (not WorkManager).
- `widget/CounterWidget` — Compact progress widget (2x1, resizable). Shows "X/Y done" count and streak. Tap opens app.
- `widget/WidgetUpdater` — `updateAllWidgets()` with 300ms debounce, called after sync/toggle/postpone/notes. Updates both widgets in parallel.

Widget settings: background color (auto/white/dark/black), opacity, show completed, layout density, sorting (flat/by list), inherits app theme.

**Streak tracking:** Automatic — records when all tasks completed for the day, resets if overdue appears. Vacation mode pauses streak.

## Localization

All user-facing strings are in `res/values/strings.xml` (English) and `res/values-sv/strings.xml` (Swedish). Always update both when adding strings. Locale config in `res/xml/locales_config.xml`.

## Themes

8 themes defined in `ui/theme/ThemePalettes.kt`: System (dynamic color), Catppuccin, Rosé Pine, Gruvbox, Tokyo Night, Dracula, Kanagawa, Oxocarbon. All dark-only. Each defines a full Material3 `ColorScheme` plus custom colors (`overdueRed`, `completedGreen`, `completedGray`).
