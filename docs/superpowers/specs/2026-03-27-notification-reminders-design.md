# Notification Reminders — Design Spec

## Overview

Add customizable daily task reminders that notify the user to check their tasks at configurable times. Three fixed time slots (Morning, Midday, Evening) with a master toggle and individual toggles per slot.

## Decisions

- **3 fixed slots** — Morning (08:00), Midday (12:00), Evening (18:00) with user-configurable times
- **Notification content** — Task count only, e.g. "You have 5 tasks left today"
- **Tap action** — Opens app and triggers a sync so data is fresh
- **Master + individual toggles** — Global on/off plus per-slot enable/disable
- **Suppress when 0 tasks** — No notification fires if all tasks are completed
- **Scheduling** — WorkManager OneTimeWorkRequests with calculated delays, self-rescheduling daily

## Settings UI

New **"Reminders"** section in SettingsScreen, placed after the Display section:

- **Master toggle** — "Task reminders" on/off. When off, the 3 slots below are grayed out/disabled.
- **Morning** — toggle + time picker, default 08:00
- **Midday** — toggle + time picker, default 12:00
- **Evening** — toggle + time picker, default 18:00

Tapping the time opens Android's standard `TimePickerDialog`. Each slot shows the set time as subtitle text (e.g. "08:00"). When the master toggle is turned off, all 3 workers are cancelled. When turned back on, only individually-enabled slots are scheduled.

## Data Layer (SettingsRepository)

New preference keys in `SettingsRepository` (DataStore):

| Key | Type | Default |
|-----|------|---------|
| `reminders_enabled` | Boolean | `false` |
| `reminder_morning_enabled` | Boolean | `true` |
| `reminder_morning_hour` | Int | `8` |
| `reminder_morning_minute` | Int | `0` |
| `reminder_midday_enabled` | Boolean | `true` |
| `reminder_midday_hour` | Int | `12` |
| `reminder_midday_minute` | Int | `0` |
| `reminder_evening_enabled` | Boolean | `true` |
| `reminder_evening_hour` | Int | `18` |
| `reminder_evening_minute` | Int | `0` |

Exposed as `Flow<T>` like existing settings. Individual slots default to enabled so that when the user first turns on the master toggle, all 3 are active immediately.

SettingsViewModel gets corresponding StateFlows and setter methods that write to DataStore and trigger worker rescheduling.

## Notification Infrastructure

### Permission

Add `POST_NOTIFICATIONS` to AndroidManifest. On Android 13+, request at runtime when the user first enables the master toggle. If denied, show a snackbar explaining reminders won't work without it, and turn the toggle back off.

### Notification Channel

Create a `"task_reminders"` channel in `OrdnaApplication.onCreate()`:
- Name: "Task Reminders" / "Uppgiftspåminnelser"
- Importance: Normal (shows in shade, makes sound, no heads-up)

### Notification Content

- **Title:** Time-of-day greeting — "Good morning" / "Good afternoon" / "Good evening" (localized)
- **Body:** "You have X tasks left today" (localized)
- Uses the app's locale setting from SettingsRepository
- **Small icon:** Reuse the existing app icon (`R.drawable.ic_launcher_foreground` or existing monochrome icon)
- **Notification IDs:** Morning=1001, Midday=1002, Evening=1003 (fixed per slot, so re-firing replaces the previous one for that slot)

### Tap Action

PendingIntent opens MainActivity with a `SYNC_ON_LAUNCH` extra. MainActivity reads this flag and triggers `TaskRepository.sync()` before rendering the Today screen.

## Worker Architecture

### ReminderWorker

New `CoroutineWorker` with Hilt injection (`@HiltWorker`). Takes a `"slot"` input data key (`"morning"`, `"midday"`, `"evening"`).

**Logic:**
1. Query Room for today's incomplete task count (new `TaskDao` query: `SELECT COUNT(*) WHERE isCompleted = 0 AND due <= today`)
2. If count == 0, skip notification silently
3. If count > 0, build and post notification with count
4. Reschedule itself — calculate delay until same time tomorrow, enqueue new OneTimeWorkRequest for this slot

### ReminderScheduler

Utility class injected via Hilt with methods:
- `scheduleAll()` — reads settings, schedules all enabled slots
- `scheduleSlot(slot, hour, minute)` — calculates delay from now to next occurrence of target time (if time already passed today, schedule for tomorrow). Enqueues OneTimeWorkRequest with unique work name (`"reminder_morning"` / `"reminder_midday"` / `"reminder_evening"`) using `REPLACE` policy
- `cancelSlot(slot)` — cancels by unique work name
- `cancelAll()` — cancels all 3

### Rescheduling Triggers

- Master toggle changed → `scheduleAll()` or `cancelAll()`
- Individual toggle changed → `scheduleSlot()` or `cancelSlot()`
- Time changed → `scheduleSlot()` with new time (REPLACE overwrites old)
- Device reboot → `BootReceiver` calls `scheduleAll()`

### BootReceiver

New `BroadcastReceiver` registered in manifest for `BOOT_COMPLETED`. Calls `ReminderScheduler.scheduleAll()` to restore reminders after reboot. Requires `RECEIVE_BOOT_COMPLETED` permission in manifest.

## Localization

| Key | EN | SV |
|-----|----|----|
| `settings_section_reminders` | Reminders | Påminnelser |
| `reminder_master_toggle` | Task reminders | Uppgiftspåminnelser |
| `reminder_master_subtitle` | Get reminded to check your tasks | Bli påmind om att kolla dina uppgifter |
| `reminder_morning` | Morning | Morgon |
| `reminder_midday` | Midday | Mitt på dagen |
| `reminder_evening` | Evening | Kväll |
| `reminder_channel_name` | Task Reminders | Uppgiftspåminnelser |
| `reminder_title_morning` | Good morning | God morgon |
| `reminder_title_midday` | Good afternoon | God eftermiddag |
| `reminder_title_evening` | Good evening | God kväll |
| `reminder_body` | You have %d tasks left today | Du har %d uppgifter kvar idag |
| `reminder_permission_needed` | Notification permission required for reminders | Aviseringsbehörighet krävs för påminnelser |

## New Files

- `data/sync/ReminderWorker.kt` — the worker
- `data/sync/ReminderScheduler.kt` — scheduling utility
- `data/sync/BootReceiver.kt` — reboot recovery

## Modified Files

- `AndroidManifest.xml` — POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED permissions, BootReceiver registration
- `OrdnaApplication.kt` — notification channel creation
- `data/repository/SettingsRepository.kt` — new preference keys and flows
- `data/local/TaskDao.kt` — new count query
- `ui/settings/SettingsViewModel.kt` — new state/setters, reminder scheduling calls
- `ui/settings/SettingsScreen.kt` — new Reminders section UI
- `ui/today/TodayViewModel.kt` or `MainActivity.kt` — handle SYNC_ON_LAUNCH intent
- `res/values/strings.xml` — EN strings
- `res/values-sv/strings.xml` — SV strings
