# Task Creation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users create tasks due today via a FAB + bottom sheet, with optimistic Room insert and background Google Tasks API sync.

**Architecture:** FAB on TodayScreen opens a modal bottom sheet (title + list picker). Task is inserted into Room immediately with a `temp-` prefixed ID, then synced to the API in the background. On API success, the temp entity is replaced with the server entity. Sync's `deleteTasksNotIn` is updated to skip `temp-` IDs.

**Tech Stack:** Jetpack Compose, Material 3, Room, Google Tasks API, Hilt, DataStore

**Spec:** `docs/superpowers/specs/2026-03-26-task-creation-design.md`

---

### Task 1: Add `deleteById` to TaskDao and update `deleteTasksNotIn` for sync safety

**Files:**
- Modify: `app/src/main/java/com/ordna/android/data/local/TaskDao.kt`

- [ ] **Step 1: Add `deleteById` method**

Add before the closing brace of the `TaskDao` interface:

```kotlin
@Query("DELETE FROM tasks WHERE id = :taskId")
suspend fun deleteById(taskId: String)
```

- [ ] **Step 2: Update `deleteTasksNotIn` to skip temp tasks**

Replace the existing `deleteTasksNotIn` query:

```kotlin
@Query("DELETE FROM tasks WHERE id NOT IN (:ids) AND id NOT LIKE 'temp-%'")
suspend fun deleteTasksNotIn(ids: List<String>)
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/local/TaskDao.kt
git commit -m "feat: add deleteById and make sync skip temp tasks"
```

---

### Task 2: Update `GoogleTasksApi.createTask()` to accept due date and return Task

**Files:**
- Modify: `app/src/main/java/com/ordna/android/data/remote/GoogleTasksApi.kt`

- [ ] **Step 1: Update the method signature and body**

Replace the existing `createTask` method with:

```kotlin
suspend fun createTask(
    accountEmail: String,
    listId: String,
    title: String,
    due: java.time.LocalDate? = null,
): com.google.api.services.tasks.model.Task = withContext(Dispatchers.IO) {
    val task = Task().setTitle(title).setStatus("needsAction")
    if (due != null) {
        task.due = "${due}T00:00:00.000Z"
    }
    buildService(accountEmail).tasks().insert(listId, task).execute()
}
```

The `due` parameter defaults to `null` so existing callers (`ShareActivity`) still work without changes.

- [ ] **Step 2: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/remote/GoogleTasksApi.kt
git commit -m "feat: createTask now accepts due date and returns Task object"
```

---

### Task 3: Add `createTask` method to `TaskRepository`

**Files:**
- Modify: `app/src/main/java/com/ordna/android/data/repository/TaskRepository.kt`

- [ ] **Step 1: Add the createTask method**

Add after the `toggleTask` method, before the `companion object`:

```kotlin
suspend fun createTask(title: String, listId: String, listTitle: String): Result<Unit> = runCatching {
    val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")
    val today = LocalDate.now()
    val tempId = "temp-${java.util.UUID.randomUUID()}"
    val listColor = GoogleTasksApi.colorForListId(listId)

    // Optimistic insert
    val tempEntity = TaskEntity(
        id = tempId,
        title = title,
        due = today,
        dueDateTime = "${today}T00:00:00.000Z",
        status = "needsAction",
        completedAt = null,
        listId = listId,
        listTitle = listTitle,
        listColor = listColor,
        position = "",
        updated = Instant.now(),
    )
    taskDao.upsertAll(listOf(tempEntity))
    updateAllWidgets(context)

    try {
        val created = api.createTask(email, listId, title, today)
        // Replace temp entity with server entity (can't update PK, so delete + insert)
        taskDao.deleteById(tempId)
        taskDao.upsertAll(listOf(
            tempEntity.copy(
                id = created.id,
                position = created.position ?: "",
                updated = GoogleTasksApi.parseCompletedAt(created.updated) ?: Instant.now(),
            )
        ))
        updateAllWidgets(context)
    } catch (e: Exception) {
        // Revert optimistic insert
        taskDao.deleteById(tempId)
        updateAllWidgets(context)
        throw e
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/repository/TaskRepository.kt
git commit -m "feat: add optimistic createTask with temp ID pattern"
```

---

### Task 4: Add creation list setting to `SettingsRepository`

**Files:**
- Modify: `app/src/main/java/com/ordna/android/data/repository/SettingsRepository.kt`

- [ ] **Step 1: Add preference keys**

Add after `shareListTitleKey` (around line 47):

```kotlin
private val createListIdKey = stringPreferencesKey("create_list_id")
private val createListTitleKey = stringPreferencesKey("create_list_title")
```

- [ ] **Step 2: Add flows and setters**

Add after the `getShareListTitle()` method (around line 139), before the streak section:

```kotlin
// -- Create list settings --

val createListId: Flow<String?> = context.settingsDataStore.data.map { prefs ->
    prefs[createListIdKey]
}

val createListTitle: Flow<String?> = context.settingsDataStore.data.map { prefs ->
    prefs[createListTitleKey]
}

suspend fun setCreateList(listId: String, listTitle: String) {
    context.settingsDataStore.edit {
        it[createListIdKey] = listId
        it[createListTitleKey] = listTitle
    }
}

suspend fun getCreateListId(): String? =
    context.settingsDataStore.data.first()[createListIdKey]

suspend fun getCreateListTitle(): String? =
    context.settingsDataStore.data.first()[createListTitleKey]
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/repository/SettingsRepository.kt
git commit -m "feat: add create list setting to SettingsRepository"
```

---

### Task 5: Add creation list picker to Settings UI

**Files:**
- Modify: `app/src/main/java/com/ordna/android/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/ordna/android/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-sv/strings.xml`

- [ ] **Step 1: Add strings**

In `app/src/main/res/values/strings.xml`, add after the completion method strings (after `settings_completion_both`):

```xml
<string name="settings_create_list_title">Default list</string>
<string name="settings_create_list_subtitle">Where new tasks are added</string>
```

In `app/src/main/res/values-sv/strings.xml`, add in the corresponding location:

```xml
<string name="settings_create_list_title">Standardlista</string>
<string name="settings_create_list_subtitle">Var nya uppgifter skapas</string>
```

- [ ] **Step 2: Add ViewModel state and setter**

In `SettingsViewModel.kt`, add after the `shareListTitle` state (around line 52-53):

```kotlin
val createListTitle: StateFlow<String?> = settingsRepository.createListTitle
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

Add a setter after `setShareList` (around line 156-158):

```kotlin
fun setCreateList(listId: String, listTitle: String) {
    viewModelScope.launch { settingsRepository.setCreateList(listId, listTitle) }
}
```

- [ ] **Step 3: Add UI to SettingsScreen**

Collect the new state at the top of `SettingsScreen`, after the `shareListTitle` collection:

```kotlin
val createListTitle by viewModel.createListTitle.collectAsState()
```

Add the creation list picker after the completion method section, before the Quick Share section header. Insert before the `Spacer(modifier = Modifier.height(24.dp))` that precedes `SettingSectionHeader(stringResource(R.string.settings_section_share))` (around line 222-224):

```kotlin
// Default creation list
Spacer(modifier = Modifier.height(20.dp))

SettingLabel(
    title = stringResource(R.string.settings_create_list_title),
    subtitle = stringResource(R.string.settings_create_list_subtitle),
)
ShareListPicker(
    selectedTitle = createListTitle,
    availableLists = availableLists,
    isLoading = listsLoading,
    onExpand = { viewModel.loadAvailableLists() },
    onSelect = { viewModel.setCreateList(it.id, it.title) },
)
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ordna/android/ui/settings/SettingsViewModel.kt \
       app/src/main/java/com/ordna/android/ui/settings/SettingsScreen.kt \
       app/src/main/res/values/strings.xml \
       app/src/main/res/values-sv/strings.xml
git commit -m "feat: add default creation list setting"
```

---

### Task 6: Add strings, `createTask` to `TodayViewModel`, and inject API

**Files:**
- Modify: `app/src/main/java/com/ordna/android/ui/today/TodayViewModel.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-sv/strings.xml`

- [ ] **Step 1: Add task creation strings**

These strings are needed by the ViewModel error handling, so they must be added before Task 7.

In `app/src/main/res/values/strings.xml`, add after the `cd_uncomplete` string:

```xml
<!-- Task creation -->
<string name="create_task_hint">New task</string>
<string name="create_task_error">Couldn\'t create task</string>
<string name="cd_add_task">Add task</string>
```

In `app/src/main/res/values-sv/strings.xml`, add in the corresponding location:

```xml
<!-- Task creation -->
<string name="create_task_hint">Ny uppgift</string>
<string name="create_task_error">Kunde inte skapa uppgift</string>
<string name="cd_add_task">L\u00e4gg till uppgift</string>
```

- [ ] **Step 2: Add `GoogleTasksApi` as a Hilt constructor dependency**

Change the `TodayViewModel` constructor to inject `api`:

```kotlin
@HiltViewModel
class TodayViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val api: GoogleTasksApi,
) : AndroidViewModel(application) {
```

Add the import at the top:

```kotlin
import com.ordna.android.data.remote.GoogleTasksApi
```

- [ ] **Step 3: Add available lists state and create list settings**

Add after the `_streakRecorded` field:

```kotlin
private val _availableLists = MutableStateFlow<List<com.ordna.android.ui.settings.TaskListOption>>(emptyList())
val availableLists: StateFlow<List<com.ordna.android.ui.settings.TaskListOption>> = _availableLists.asStateFlow()

val createListId: StateFlow<String?> = settingsRepository.createListId
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

val createListTitle: StateFlow<String?> = settingsRepository.createListTitle
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

- [ ] **Step 4: Load lists in init**

Add at the end of the `init` block (before the closing `}`):

```kotlin
// Load available lists for task creation
viewModelScope.launch {
    try {
        val email = repository.getAccountEmail() ?: return@launch
        val lists = api.fetchTaskLists(email)
        _availableLists.value = lists.mapNotNull { list ->
            val id = list.id ?: return@mapNotNull null
            val title = list.title ?: "Untitled"
            com.ordna.android.ui.settings.TaskListOption(
                id, title, GoogleTasksApi.colorForListId(id)
            )
        }
    } catch (_: Exception) { }
}
```

- [ ] **Step 5: Add createTask method**

Add after `postponeTask`:

```kotlin
fun createTask(title: String, listId: String, listTitle: String) {
    viewModelScope.launch {
        repository.createTask(title, listId, listTitle).onFailure { e ->
            _error.value = e.localizedMessage ?: getString(R.string.create_task_error)
        }
    }
}
```

- [ ] **Step 6: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/ordna/android/ui/today/TodayViewModel.kt \
       app/src/main/res/values/strings.xml \
       app/src/main/res/values-sv/strings.xml
git commit -m "feat: add createTask to TodayViewModel with list loading"
```

---

### Task 7: Add FAB and bottom sheet to TodayScreen

**Files:**
- Modify: `app/src/main/java/com/ordna/android/ui/today/TodayScreen.kt`

- [ ] **Step 1: Add imports to TodayScreen.kt**

Add these imports (skip any already present):

```kotlin
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
```

- [ ] **Step 2: Add bottom sheet state and FAB to Scaffold**

In the `TodayScreen` composable, add state after the `postponeTask` state:

```kotlin
var showCreateSheet by remember { mutableStateOf(false) }
```

Add a `floatingActionButton` parameter to the `Scaffold` call, after `snackbarHost`:

```kotlin
floatingActionButton = {
    FloatingActionButton(
        onClick = { showCreateSheet = true },
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_task))
    }
},
```

- [ ] **Step 3: Increase bottom spacer for FAB clearance**

Change the bottom spacer from `16.dp` to `88.dp`:

```kotlin
item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(88.dp)) }
```

- [ ] **Step 4: Add the bottom sheet invocation**

Add after the PostponeDialog block, before the closing `}` of `TodayScreen`:

```kotlin
if (showCreateSheet) {
    val availableLists by viewModel.availableLists.collectAsState()
    val defaultListId by viewModel.createListId.collectAsState()
    val defaultListTitle by viewModel.createListTitle.collectAsState()

    CreateTaskSheet(
        availableLists = availableLists,
        defaultListId = defaultListId,
        defaultListTitle = defaultListTitle,
        onDismiss = { showCreateSheet = false },
        onCreate = { title, listId, listTitle ->
            viewModel.createTask(title, listId, listTitle)
            showCreateSheet = false
        },
    )
}
```

- [ ] **Step 5: Add the CreateTaskSheet composable**

Add as a new private composable at the bottom of the file:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskSheet(
    availableLists: List<com.ordna.android.ui.settings.TaskListOption>,
    defaultListId: String?,
    defaultListTitle: String?,
    onDismiss: () -> Unit,
    onCreate: (title: String, listId: String, listTitle: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Determine initial selected list
    val initialList = availableLists.find { it.id == defaultListId }
        ?: availableLists.firstOrNull()
    var selectedList by remember { mutableStateOf(initialList) }

    val canSubmit = title.isNotBlank() && selectedList != null

    fun submit() {
        val list = selectedList ?: return
        if (title.isBlank()) return
        onCreate(title.trim(), list.id, list.title)
    }

    // Auto-focus the text field
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.create_task_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() }),
            )

            if (availableLists.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (list in availableLists) {
                        FilterChip(
                            selected = selectedList?.id == list.id,
                            onClick = { selectedList = list },
                            label = { Text(list.title) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { submit() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cd_add_task))
            }
        }
    }
}
```

- [ ] **Step 6: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/ordna/android/ui/today/TodayScreen.kt
git commit -m "feat: add FAB and bottom sheet for task creation"
```

---

### Task 8: Final build and manual test

- [ ] **Step 1: Clean build**

Run: `./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install**

Run: `sudo adb install -r app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Manual verification checklist**

1. FAB visible in bottom-right of today screen
2. Tapping FAB opens bottom sheet with auto-focused text field and list chips
3. Default list is pre-selected
4. Typing a task name and tapping the button creates the task
5. Keyboard "Done" action also submits (and does nothing when field is empty)
6. Task appears immediately in "Due Today" section
7. Pull-to-refresh doesn't delete the temp task before API completes
8. Settings shows "Default list" picker under App section
9. Changing language updates all new strings correctly

- [ ] **Step 4: Commit any fixes if needed**
