# Ordna — Improvement PRD

Tracking document for fixes, features, and polish items from the codebase review.
Items are ordered by priority within tiers. Tick the status box as we work through them.

**Status legend:** `[ ]` not started · `[~]` in progress · `[x]` done · `[-]` won't do

## Goals
- Keep the app's focus: surface overdue + today tasks from Google Tasks.
- Fix correctness issues before adding features.
- Each item is small enough to ship as a single PR.

## Non-goals
- Becoming a full Google Tasks client (recurrence editor, list management, etc.).
- Adding an "upcoming tasks" view — that changes the app's premise.
- Multi-account support.

---

## P0 — Correctness bugs (do first)

### P0.1 `[x]` Use lifecycleScope in ShareActivity
- **Problem.** `ShareActivity.kt:53` spawns `CoroutineScope(Dispatchers.Main)` not tied to the activity. If torn down mid-share the coroutine keeps running and can touch a destroyed activity.
- **Change.** Replace with `lifecycleScope.launch`; move the API call onto `Dispatchers.IO` inside.
- **Done when.** No raw `CoroutineScope(...)` in the file; ACTION_SEND text intents still create a task.

### P0.2 `[x]` Use PATCH for notes and due-date updates
- **Problem.** `GoogleTasksApi.kt:113-118` (notes) and `:130-141` (due) do GET-then-UPDATE, which clobbers concurrent edits. Every other mutation in the file uses PATCH.
- **Change.** Switch both to `service.tasks().patch(listId, taskId, Task().setNotes(...))` / `.setDue(...)`.
- **Done when.** No `service.tasks().get(...)` calls remain in mutation paths.

### P0.3 `[x]` Detect auth errors reliably
- **Problem.** `TodayViewModel.kt:210-213` string-matches `"401"`/`"auth"`/`"unauthorized"`. Misses `UserRecoverableAuthIOException` (revoked consent).
- **Change.** Add a helper in `data/remote` that returns `AuthError` for `GoogleJsonResponseException.statusCode == 401` and for `UserRecoverableAuthIOException`. ViewModel checks the type, not the string.
- **Done when.** Revoking app access in the Google account, then refreshing, takes the user back to sign-in.

### P0.4 `[x]` Surface per-list sync failures
- **Problem.** `TaskRepository.kt:255-259` silently `continue`s on per-list fetch failures. Stale data with no signal.
- **Change.** Collect failing list titles in `performSync`; return them alongside `TaskListInfo`. ViewModel surfaces a snackbar: "Couldn't sync: Work, Side projects".
- **Done when.** Forcing a 403 on one list shows the warning while other lists still appear.

---

## P1 — High value, low cost

### P1.1 `[ ]` Undo for completion and deletion
- **Problem.** Completions have no undo; deletions require an AlertDialog. Both are friction.
- **Change.** After `toggleTask`/`deleteTask`, show a snackbar with Undo (4s). Undo calls the inverse op. Remove the delete confirmation dialog once Undo works.
- **Done when.** Swipe-to-complete and detail-sheet delete both undoable; AlertDialog gone.

### P1.2 `[ ]` Sync after share-to-app
- **Problem.** `ShareActivity` creates remotely but the task doesn't appear locally until WorkManager fires.
- **Change.** Insert an optimistic local entity (mirror `TaskRepository.createTask`) before calling `finish()`. Reuse the temp-id + reconcile pattern.
- **Done when.** Sharing text into Ordna and opening the app shows the task immediately.

### P1.3 `[ ]` Notification action: complete top task
- **Problem.** `ReminderWorker.kt:88-95` only opens the app.
- **Change.** Add a `NotificationCompat.Action` "Complete top task". Reuses widget-toggle plumbing (`WidgetToggleWorker` is the template).
- **Done when.** Notification shows the action; tapping completes the first task and re-posts the reminder.

### P1.4 `[ ]` Due date in CreateTaskSheet
- **Problem.** `TaskRepository.createTask()` hardcodes `due = today`. Capturing a task for tomorrow forces a postpone afterward.
- **Change.** Add a "Today / Tomorrow / Pick…" affordance to `CreateTaskSheet`, reusing `PostponeDialog` presets. Today stays default.
- **Done when.** New tasks can be created for any date.

### P1.5 `[ ]` Invalidate API cache + reset settings on sign-out
- **Problem.** `TaskRepository.clearAccount()` doesn't touch `GoogleTasksApi.cachedService/cachedEmail`. `ordna_settings` (streak, share list) also survives.
- **Change.** Add `GoogleTasksApi.invalidate()` and call from `clearAccount()`. Reset streak + share-list keys; preserve theme/language/reminder times.
- **Done when.** Signing out + back in with a different account doesn't reuse the old service or show the old streak.

---

## P2 — Medium value

### P2.1 `[ ]` Tests for performSync + streak math
- **Problem.** No JVM tests. `performSync` pending-preservation logic (TaskRepository.kt:296-313) is the kind of code that quietly breaks.
- **Change.** Add fakes for `GoogleTasksApi` + in-memory `TaskDao`. Cover: pending preservation, completed-today filter, streak increment, streak reset on overdue.
- **Done when.** `./gradlew test` passes; tests run in CI (see P2.3).

### P2.2 `[ ]` Refactor TodayUiState combine
- **Problem.** `TodayViewModel.kt:69-115` reaches into positional `Array<Any?>` with `@Suppress("UNCHECKED_CAST")`. Renumbering inputs breaks silently.
- **Change.** Split into two derived StateFlows or wrap each sub-combine's output in a tiny named record. Behavior unchanged.
- **Done when.** No `Array<Any?>` indexing remains; Paparazzi screenshots unchanged.

### P2.3 `[ ]` ktlint + CI on PR
- **Problem.** No lint; existing workflows are manual-dispatch only.
- **Change.** Add `ktlint-gradle` and a `.github/workflows/ci.yml` running `assembleDebug` + `ktlintCheck` + unit tests on `pull_request`.
- **Done when.** A new PR fails if lint or build fails.

### P2.4 `[ ]` Per-list filter chips on TodayScreen
- **Problem.** Many lists → hard to focus on one.
- **Change.** Horizontal chip row above the LazyColumn, collapsed if >3 lists. Toggles visibility; persists in `SettingsRepository`. Top-bar counts stay honest (reflect filter).
- **Done when.** Filtering hides tasks; refresh persists across app restart.

### P2.5 `[ ]` Quick Settings tile
- **Problem.** No glanceable entry outside the home screen.
- **Change.** `TileService` showing "X due today"; tap opens app; long-press toggles vacation mode.
- **Done when.** Tile addable from QS panel; reflects state on the next sync.

### P2.6 `[ ]` Crash reporting
- **Problem.** Released app has no telemetry; silent crashes invisible.
- **Change.** Add Sentry free tier (release builds only). Strip task titles from breadcrumbs.
- **Done when.** A forced crash in a release build appears in the dashboard.

---

## P3 — Polish

### P3.1 `[ ]` TalkBack: section headers as headings
- `Modifier.semantics { heading() }` on `SectionHeader` (TodayScreen.kt:839-885).
- Custom accessibility action for swipe-to-complete (TalkBack users can't swipe).

### P3.2 `[ ]` Empty-state polish
- Replace plain "no tasks due today" text (TodayScreen.kt:240-244) with a small celebration block — streak preview, time-of-day greeting, quick add.

### P3.3 `[ ]` Streak history sheet
- Persist last 30 day completions; tapping the streak badge opens a mini-calendar bottom sheet.

### P3.4 `[ ]` CreateTaskSheet chip auto-scroll
- Scroll selected chip into view when the sheet opens (TodayScreen.kt:1298-1313).

### P3.5 `[ ]` flatComparator: new lists at top, not bottom
- Replace `Int.MAX_VALUE` fallback with `-1` so unknown lists surface instead of sinking.

### P3.6 `[ ]` Magic strings → TaskStatus enum
- Replace `"needsAction"`/`"completed"` literals across DAO/API/widget with an enum + Room converter.

### P3.7 `[ ]` Verify MainActivity handles SYNC_ON_LAUNCH in onNewIntent
- Confirm intent extras from reminders/widget are consumed in both `onCreate` and `onNewIntent`.

---

## P4 — Considered, not committed
Good ideas that need a real signal before building.

- **Light theme.** One well-chosen palette? Worth doing if you (or a user) actually want it.
- **Sub-task indentation (read-only).** Visual cost on small widgets is unclear.
- **Calendar-week strip.** Risk of drifting toward an "upcoming tasks" view.
- **Google Assistant intent.** Share intent already covers most of this.
- **Widget dynamic font scaling.** Only worth it if accessibility-scale users complain.

---

## Working order
1. **Session 1.** P0.1–P0.4 (all four are <30 min each; safe to bundle).
2. **Then.** P1 in order; each is its own PR.
3. **Opportunistic.** P2 + P3 mixed in based on appetite.
4. **Quarterly.** Revisit P4.
