# Task Creation — Design Spec

## Philosophy

Ordna is a daily planner. Task creation follows the same principle: every new task is due today. If the user wants it for another day, they postpone it after creation — using the existing postpone flow.

## UI

### FAB
- Circular Material 3 FAB with `+` icon, bottom-right of TodayScreen
- Standard FAB sizing and elevation
- Bottom spacer in LazyColumn increased to 88dp to ensure content scrolls past the FAB

### Bottom Sheet
Tapping the FAB opens a modal bottom sheet containing:
1. **Text field** — auto-focused, placeholder "New task" / "Ny uppgift". Submit on keyboard IME action (guarded: no-op if empty/whitespace).
2. **List picker** — horizontal row of chips (or dropdown) showing the user's Google Tasks lists, pre-selected to the default creation list from settings.
3. **Confirm button** — for users who don't use keyboard submit. Disabled when text field is empty.

Bottom sheet dismisses on successful submit.

### List Data Source
Lists are cached from the last sync or settings load. No API call when opening the bottom sheet. If no cached lists are available, fall back to the default creation list from settings. If that's also unset, disable the confirm button and show a hint to configure in settings.

## Behavior

### Optimistic Creation
Same pattern as toggle and postpone:
1. Generate temporary ID with `"temp-"` prefix (e.g., `"temp-${UUID}"`)
2. Insert `TaskEntity` into Room with all required fields:
   - `id` = temp ID
   - `title` = user input
   - `due` = `LocalDate.now()`
   - `dueDateTime` = `"${LocalDate.now()}T00:00:00.000Z"`
   - `status` = `"needsAction"`
   - `completedAt` = `null`
   - `listId`, `listTitle` = selected list
   - `listColor` = `GoogleTasksApi.colorForListId(listId)`
   - `position` = `""`
   - `updated` = `Instant.now()`
3. Call `updateAllWidgets(context)` so the widget reflects the new task
4. Task appears in the "Due Today" section instantly (Room Flow re-emits)
5. Background: call `GoogleTasksApi.createTask()` with title, listId, and due date
6. On API success: delete temp entity, insert new entity with server-assigned ID and fields
7. On API failure: delete temp entity from Room, call `updateAllWidgets(context)`, show snackbar error

### Sync Safety
The `performSync()` method deletes local tasks not found in the API response (`deleteTasksNotIn`). To prevent it from deleting in-flight optimistic tasks, the deletion query excludes tasks whose ID starts with `"temp-"`. This is a simple prefix check in the SQL WHERE clause.

### Room Primary Key
Room does not allow updating a primary key. When the API returns the server-assigned ID, we delete the temp entity and insert a fresh one with the real ID (delete + insert, not update).

A new `deleteById(taskId: String)` method is needed in `TaskDao`.

### API
Update `GoogleTasksApi.createTask()` to:
- Accept a `due: LocalDate` parameter
- Set the due field as RFC 3339 (`"${due}T00:00:00.000Z"`)
- Return the created `Task` object (explicit return type) so we can get the server-assigned ID

## Settings

### New Setting: Default Creation List
- Location: "App" section in Settings, after completion method
- Title: "Default list" / "Standardlista"
- Subtitle: "Where new tasks are added" / "Var nya uppgifter skapas"
- UI: Same dropdown pattern as Quick Share list picker
- Storage: `createListId` and `createListTitle` keys in `ordna_settings` DataStore
- Separate from the Quick Share default list
- Fallback: if unset, use the first available list

## Data Flow

```
FAB tap -> Bottom sheet opens (lists from cache)
User types title, picks list, submits
  -> Generate "temp-${UUID}" ID
  -> Insert TaskEntity into Room (all fields populated)
  -> Call updateAllWidgets(context)
  -> Dismiss bottom sheet
  -> Room Flow re-emits -> UI updates instantly
  -> Call GoogleTasksApi.createTask(email, listId, title, today)
  -> On success: delete temp entity, insert entity with server ID
  -> On failure: delete temp entity, updateAllWidgets(), show snackbar
```

## Strings

English:
- `create_task_hint`: "New task"
- `create_task_error`: "Couldn't create task"
- `settings_create_list_title`: "Default list"
- `settings_create_list_subtitle`: "Where new tasks are added"
- `cd_add_task`: "Add task"

Swedish:
- `create_task_hint`: "Ny uppgift"
- `create_task_error`: "Kunde inte skapa uppgift"
- `settings_create_list_title`: "Standardlista"
- `settings_create_list_subtitle`: "Var nya uppgifter skapas"
- `cd_add_task`: "L\u00e4gg till uppgift"

## Scope Boundaries

Not included:
- No date picker — always today
- No description/notes field
- No subtasks
- No priority
- No widget task creation (future consideration)
