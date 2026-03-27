# Notification Reminders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add daily task reminders with 3 configurable time slots (Morning/Midday/Evening) that notify the user of remaining task count.

**Architecture:** WorkManager OneTimeWorkRequests self-reschedule daily. Settings stored in DataStore. Notification channel created at app start. BootReceiver restores schedules after reboot. Settings UI with master + individual toggles and time pickers.

**Tech Stack:** WorkManager, Hilt, DataStore Preferences, NotificationCompat, Jetpack Compose, Room

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `data/local/TaskDao.kt` | Modify | Add `getActiveTaskCount()` query |
| `data/repository/SettingsRepository.kt` | Modify | Add reminder preference keys, flows, and setters |
| `data/sync/ReminderScheduler.kt` | Create | Schedule/cancel WorkManager reminder jobs |
| `data/sync/ReminderWorker.kt` | Create | Query task count, post notification, reschedule |
| `data/sync/BootReceiver.kt` | Create | Restore reminder schedules after reboot |
| `OrdnaApplication.kt` | Modify | Create notification channel |
| `MainActivity.kt` | Modify | Handle SYNC_ON_LAUNCH intent extra |
| `ui/settings/SettingsViewModel.kt` | Modify | Add reminder state/setters, trigger scheduling |
| `ui/settings/SettingsScreen.kt` | Modify | Add Reminders section UI |
| `AndroidManifest.xml` | Modify | Add permissions and BootReceiver |
| `res/values/strings.xml` | Modify | Add EN reminder strings |
| `res/values-sv/strings.xml` | Modify | Add SV reminder strings |

---

### Task 1: Add TaskDao count query

**Files:**
- Modify: `app/src/main/java/com/ordna/android/data/local/TaskDao.kt:39`

- [ ] **Step 1: Add the count query to TaskDao**

Add this method after the existing `getActiveTasks` query (line 40):

```kotlin
@Query("SELECT COUNT(*) FROM tasks WHERE status = 'needsAction' AND due IS NOT NULL AND due <= :today")
suspend fun getActiveTaskCount(today: LocalDate): Int
```

- [ ] **Step 2: Build to verify the query compiles**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/local/TaskDao.kt
git commit -m "feat: add active task count query to TaskDao for reminders"
```

---

### Task 2: Add reminder preferences to SettingsRepository

**Files:**
- Modify: `app/src/main/java/com/ordna/android/data/repository/SettingsRepository.kt`

- [ ] **Step 1: Add preference keys**

Add after the `widgetSortingKey` declaration (around line 60):

```kotlin
// -- Reminder settings --
private val remindersEnabledKey = booleanPreferencesKey("reminders_enabled")
private val reminderMorningEnabledKey = booleanPreferencesKey("reminder_morning_enabled")
private val reminderMorningHourKey = intPreferencesKey("reminder_morning_hour")
private val reminderMorningMinuteKey = intPreferencesKey("reminder_morning_minute")
private val reminderMiddayEnabledKey = booleanPreferencesKey("reminder_midday_enabled")
private val reminderMiddayHourKey = intPreferencesKey("reminder_midday_hour")
private val reminderMiddayMinuteKey = intPreferencesKey("reminder_midday_minute")
private val reminderEveningEnabledKey = booleanPreferencesKey("reminder_evening_enabled")
private val reminderEveningHourKey = intPreferencesKey("reminder_evening_hour")
private val reminderEveningMinuteKey = intPreferencesKey("reminder_evening_minute")
```

- [ ] **Step 2: Add flows and setters**

Add after the widget settings section (before the `companion object`):

```kotlin
// -- Reminder settings --

val remindersEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
    prefs[remindersEnabledKey] ?: false
}

suspend fun setRemindersEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[remindersEnabledKey] = enabled }
}

val reminderMorningEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderMorningEnabledKey] ?: true
}

val reminderMorningHour: Flow<Int> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderMorningHourKey] ?: 8
}

val reminderMorningMinute: Flow<Int> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderMorningMinuteKey] ?: 0
}

suspend fun setReminderMorningEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[reminderMorningEnabledKey] = enabled }
}

suspend fun setReminderMorningTime(hour: Int, minute: Int) {
    context.settingsDataStore.edit {
        it[reminderMorningHourKey] = hour
        it[reminderMorningMinuteKey] = minute
    }
}

val reminderMiddayEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderMiddayEnabledKey] ?: true
}

val reminderMiddayHour: Flow<Int> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderMiddayHourKey] ?: 12
}

val reminderMiddayMinute: Flow<Int> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderMiddayMinuteKey] ?: 0
}

suspend fun setReminderMiddayEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[reminderMiddayEnabledKey] = enabled }
}

suspend fun setReminderMiddayTime(hour: Int, minute: Int) {
    context.settingsDataStore.edit {
        it[reminderMiddayHourKey] = hour
        it[reminderMiddayMinuteKey] = minute
    }
}

val reminderEveningEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderEveningEnabledKey] ?: true
}

val reminderEveningHour: Flow<Int> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderEveningHourKey] ?: 18
}

val reminderEveningMinute: Flow<Int> = context.settingsDataStore.data.map { prefs ->
    prefs[reminderEveningMinuteKey] ?: 0
}

suspend fun setReminderEveningEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[reminderEveningEnabledKey] = enabled }
}

suspend fun setReminderEveningTime(hour: Int, minute: Int) {
    context.settingsDataStore.edit {
        it[reminderEveningHourKey] = hour
        it[reminderEveningMinuteKey] = minute
    }
}
```

- [ ] **Step 3: Add snapshot reads for ReminderScheduler**

Add inside the `companion object`, after the existing `readWidgetSettings` method:

```kotlin
/** Snapshot read of all reminder settings for scheduling. */
suspend fun readReminderSettings(context: Context): ReminderSettings {
    val prefs = context.settingsDataStore.data.first()
    return ReminderSettings(
        enabled = prefs[booleanPreferencesKey("reminders_enabled")] ?: false,
        morningEnabled = prefs[booleanPreferencesKey("reminder_morning_enabled")] ?: true,
        morningHour = prefs[intPreferencesKey("reminder_morning_hour")] ?: 8,
        morningMinute = prefs[intPreferencesKey("reminder_morning_minute")] ?: 0,
        middayEnabled = prefs[booleanPreferencesKey("reminder_midday_enabled")] ?: true,
        middayHour = prefs[intPreferencesKey("reminder_midday_hour")] ?: 12,
        middayMinute = prefs[intPreferencesKey("reminder_midday_minute")] ?: 0,
        eveningEnabled = prefs[booleanPreferencesKey("reminder_evening_enabled")] ?: true,
        eveningHour = prefs[intPreferencesKey("reminder_evening_hour")] ?: 18,
        eveningMinute = prefs[intPreferencesKey("reminder_evening_minute")] ?: 0,
    )
}
```

- [ ] **Step 4: Add ReminderSettings data class**

Add at the top of the file, after the existing enum/data class declarations (after `WidgetSettings`):

```kotlin
data class ReminderSettings(
    val enabled: Boolean = false,
    val morningEnabled: Boolean = true,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val middayEnabled: Boolean = true,
    val middayHour: Int = 12,
    val middayMinute: Int = 0,
    val eveningEnabled: Boolean = true,
    val eveningHour: Int = 18,
    val eveningMinute: Int = 0,
)
```

- [ ] **Step 5: Build to verify**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/repository/SettingsRepository.kt
git commit -m "feat: add reminder preferences to SettingsRepository"
```

---

### Task 3: Add localized strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-sv/strings.xml`

- [ ] **Step 1: Add EN strings**

Add before the closing `</resources>` tag in `app/src/main/res/values/strings.xml`:

```xml
<!-- Reminders -->
<string name="settings_section_reminders">Reminders</string>
<string name="reminder_master_toggle">Task reminders</string>
<string name="reminder_master_subtitle">Get reminded to check your tasks</string>
<string name="reminder_morning">Morning</string>
<string name="reminder_midday">Midday</string>
<string name="reminder_evening">Evening</string>
<string name="reminder_channel_name">Task Reminders</string>
<string name="reminder_title_morning">Good morning</string>
<string name="reminder_title_midday">Good afternoon</string>
<string name="reminder_title_evening">Good evening</string>
<plurals name="reminder_body">
    <item quantity="one">You have %1$d task left today</item>
    <item quantity="other">You have %1$d tasks left today</item>
</plurals>
<string name="reminder_permission_needed">Notification permission required for reminders</string>
```

- [ ] **Step 2: Add SV strings**

Add before the closing `</resources>` tag in `app/src/main/res/values-sv/strings.xml`:

```xml
<!-- Reminders -->
<string name="settings_section_reminders">Påminnelser</string>
<string name="reminder_master_toggle">Uppgiftspåminnelser</string>
<string name="reminder_master_subtitle">Bli påmind om att kolla dina uppgifter</string>
<string name="reminder_morning">Morgon</string>
<string name="reminder_midday">Mitt på dagen</string>
<string name="reminder_evening">Kväll</string>
<string name="reminder_channel_name">Uppgiftspåminnelser</string>
<string name="reminder_title_morning">God morgon</string>
<string name="reminder_title_midday">God eftermiddag</string>
<string name="reminder_title_evening">God kväll</string>
<plurals name="reminder_body">
    <item quantity="one">Du har %1$d uppgift kvar idag</item>
    <item quantity="other">Du har %1$d uppgifter kvar idag</item>
</plurals>
<string name="reminder_permission_needed">Aviseringsbehörighet krävs för påminnelser</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-sv/strings.xml
git commit -m "feat: add reminder localization strings (EN + SV)"
```

---

### Task 4: Create ReminderScheduler

**Files:**
- Create: `app/src/main/java/com/ordna/android/data/sync/ReminderScheduler.kt`

- [ ] **Step 1: Create ReminderScheduler.kt**

```kotlin
package com.ordna.android.data.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ordna.android.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val SLOT_MORNING = "morning"
        const val SLOT_MIDDAY = "midday"
        const val SLOT_EVENING = "evening"

        private fun workName(slot: String) = "reminder_$slot"

        /** Static version for use from BootReceiver (no DI). */
        suspend fun scheduleAllFromContext(context: Context) {
            val settings = SettingsRepository.readReminderSettings(context)
            if (!settings.enabled) return
            val scheduler = ReminderScheduler(context)
            if (settings.morningEnabled) scheduler.scheduleSlot(SLOT_MORNING, settings.morningHour, settings.morningMinute)
            if (settings.middayEnabled) scheduler.scheduleSlot(SLOT_MIDDAY, settings.middayHour, settings.middayMinute)
            if (settings.eveningEnabled) scheduler.scheduleSlot(SLOT_EVENING, settings.eveningHour, settings.eveningMinute)
        }
    }

    suspend fun scheduleAll() {
        val settings = SettingsRepository.readReminderSettings(context)
        if (!settings.enabled) {
            cancelAll()
            return
        }
        if (settings.morningEnabled) scheduleSlot(SLOT_MORNING, settings.morningHour, settings.morningMinute)
        else cancelSlot(SLOT_MORNING)
        if (settings.middayEnabled) scheduleSlot(SLOT_MIDDAY, settings.middayHour, settings.middayMinute)
        else cancelSlot(SLOT_MIDDAY)
        if (settings.eveningEnabled) scheduleSlot(SLOT_EVENING, settings.eveningHour, settings.eveningMinute)
        else cancelSlot(SLOT_EVENING)
    }

    fun scheduleSlot(slot: String, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val delayMillis = Duration.between(now, target).toMillis()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf("slot" to slot))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(slot),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelSlot(slot: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(slot))
    }

    fun cancelAll() {
        cancelSlot(SLOT_MORNING)
        cancelSlot(SLOT_MIDDAY)
        cancelSlot(SLOT_EVENING)
    }
}
```

- [ ] **Step 2: Build to verify** (will fail — ReminderWorker doesn't exist yet, that's expected)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/sync/ReminderScheduler.kt
git commit -m "feat: add ReminderScheduler for WorkManager reminder jobs"
```

---

### Task 5: Create ReminderWorker

**Files:**
- Create: `app/src/main/java/com/ordna/android/data/sync/ReminderWorker.kt`

- [ ] **Step 1: Create ReminderWorker.kt**

```kotlin
package com.ordna.android.data.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ordna.android.MainActivity
import com.ordna.android.R
import com.ordna.android.data.local.TaskDao
import com.ordna.android.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val NOTIFICATION_ID_MORNING = 1001
        const val NOTIFICATION_ID_MIDDAY = 1002
        const val NOTIFICATION_ID_EVENING = 1003
        const val CHANNEL_ID = "task_reminders"
    }

    override suspend fun doWork(): Result {
        val slot = inputData.getString("slot") ?: return Result.failure()

        val count = taskDao.getActiveTaskCount(LocalDate.now())

        if (count > 0) {
            postNotification(slot, count)
        }

        // Reschedule for tomorrow
        reschedule(slot)

        return Result.success()
    }

    private fun postNotification(slot: String, taskCount: Int) {
        val titleRes = when (slot) {
            ReminderScheduler.SLOT_MORNING -> R.string.reminder_title_morning
            ReminderScheduler.SLOT_MIDDAY -> R.string.reminder_title_midday
            ReminderScheduler.SLOT_EVENING -> R.string.reminder_title_evening
            else -> R.string.reminder_title_morning
        }

        val notificationId = when (slot) {
            ReminderScheduler.SLOT_MORNING -> NOTIFICATION_ID_MORNING
            ReminderScheduler.SLOT_MIDDAY -> NOTIFICATION_ID_MIDDAY
            ReminderScheduler.SLOT_EVENING -> NOTIFICATION_ID_EVENING
            else -> NOTIFICATION_ID_MORNING
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            putExtra("SYNC_ON_LAUNCH", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = appContext.getString(titleRes)
        val body = appContext.resources.getQuantityString(
            R.plurals.reminder_body, taskCount, taskCount
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission not granted — silently skip
        }
    }

    private suspend fun reschedule(slot: String) {
        val settings = SettingsRepository.readReminderSettings(appContext)
        if (!settings.enabled) return

        val (enabled, hour, minute) = when (slot) {
            ReminderScheduler.SLOT_MORNING -> Triple(settings.morningEnabled, settings.morningHour, settings.morningMinute)
            ReminderScheduler.SLOT_MIDDAY -> Triple(settings.middayEnabled, settings.middayHour, settings.middayMinute)
            ReminderScheduler.SLOT_EVENING -> Triple(settings.eveningEnabled, settings.eveningHour, settings.eveningMinute)
            else -> return
        }

        if (enabled) {
            ReminderScheduler(appContext).scheduleSlot(slot, hour, minute)
        }
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/sync/ReminderWorker.kt
git commit -m "feat: add ReminderWorker with notification posting and self-rescheduling"
```

---

### Task 6: Create BootReceiver

**Files:**
- Create: `app/src/main/java/com/ordna/android/data/sync/BootReceiver.kt`

- [ ] **Step 1: Create BootReceiver.kt**

```kotlin
package com.ordna.android.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                ReminderScheduler.scheduleAllFromContext(context)
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/ordna/android/data/sync/BootReceiver.kt
git commit -m "feat: add BootReceiver to restore reminder schedules after reboot"
```

---

### Task 7: Update AndroidManifest.xml

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add permissions**

Add after the existing `INTERNET` permission:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

- [ ] **Step 2: Add BootReceiver registration**

Add inside the `<application>` tag, after the widget receiver:

```xml
<receiver
    android:name=".data.sync.BootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add notification and boot permissions, register BootReceiver"
```

---

### Task 8: Create notification channel in OrdnaApplication

**Files:**
- Modify: `app/src/main/java/com/ordna/android/OrdnaApplication.kt`

- [ ] **Step 1: Add notification channel creation**

Replace the `OrdnaApplication` class with:

```kotlin
package com.ordna.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ordna.android.data.sync.ReminderWorker
import com.ordna.android.data.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OrdnaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        SyncWorker.enqueuePeriodicSync(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.reminder_channel_name)
            val channel = NotificationChannel(
                ReminderWorker.CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ordna/android/OrdnaApplication.kt
git commit -m "feat: create notification channel for task reminders"
```

---

### Task 9: Handle SYNC_ON_LAUNCH in MainActivity

**Files:**
- Modify: `app/src/main/java/com/ordna/android/MainActivity.kt`

- [ ] **Step 1: Add sync-on-launch handling**

Add after the locale restoration block (after line 38, before `setContent`), and inject `TaskRepository`:

```kotlin
@Inject lateinit var taskRepository: TaskRepository
```

Then in `onCreate`, after the locale restoration block and before `setContent`:

```kotlin
// Trigger sync if launched from notification
if (intent?.getBooleanExtra("SYNC_ON_LAUNCH", false) == true) {
    lifecycleScope.launch {
        try { taskRepository.sync() } catch (_: Exception) { }
    }
}
```

Add the import:
```kotlin
import com.ordna.android.data.repository.TaskRepository
import kotlinx.coroutines.launch
```

- [ ] **Step 2: Build to verify**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ordna/android/MainActivity.kt
git commit -m "feat: handle SYNC_ON_LAUNCH intent from reminder notifications"
```

---

### Task 10: Add reminder state and setters to SettingsViewModel

**Files:**
- Modify: `app/src/main/java/com/ordna/android/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Add ReminderScheduler injection**

Add `ReminderScheduler` to the constructor:

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val api: GoogleTasksApi,
    private val reminderScheduler: ReminderScheduler,
) : AndroidViewModel(application) {
```

Add import:
```kotlin
import com.ordna.android.data.sync.ReminderScheduler
```

- [ ] **Step 2: Add StateFlows for reminder settings**

Add after the `streak` StateFlow:

```kotlin
// -- Reminders --
val remindersEnabled: StateFlow<Boolean> = settingsRepository.remindersEnabled
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

val reminderMorningEnabled: StateFlow<Boolean> = settingsRepository.reminderMorningEnabled
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

val reminderMorningHour: StateFlow<Int> = settingsRepository.reminderMorningHour
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 8)

val reminderMorningMinute: StateFlow<Int> = settingsRepository.reminderMorningMinute
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

val reminderMiddayEnabled: StateFlow<Boolean> = settingsRepository.reminderMiddayEnabled
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

val reminderMiddayHour: StateFlow<Int> = settingsRepository.reminderMiddayHour
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 12)

val reminderMiddayMinute: StateFlow<Int> = settingsRepository.reminderMiddayMinute
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

val reminderEveningEnabled: StateFlow<Boolean> = settingsRepository.reminderEveningEnabled
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

val reminderEveningHour: StateFlow<Int> = settingsRepository.reminderEveningHour
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 18)

val reminderEveningMinute: StateFlow<Int> = settingsRepository.reminderEveningMinute
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
```

- [ ] **Step 3: Add setter methods**

Add after the `moveListInOrder` method:

```kotlin
fun setRemindersEnabled(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setRemindersEnabled(enabled)
        if (enabled) reminderScheduler.scheduleAll()
        else reminderScheduler.cancelAll()
    }
}

fun setReminderMorningEnabled(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setReminderMorningEnabled(enabled)
        if (enabled) {
            val hour = reminderMorningHour.value
            val minute = reminderMorningMinute.value
            reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MORNING, hour, minute)
        } else {
            reminderScheduler.cancelSlot(ReminderScheduler.SLOT_MORNING)
        }
    }
}

fun setReminderMorningTime(hour: Int, minute: Int) {
    viewModelScope.launch {
        settingsRepository.setReminderMorningTime(hour, minute)
        if (remindersEnabled.value && reminderMorningEnabled.value) {
            reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MORNING, hour, minute)
        }
    }
}

fun setReminderMiddayEnabled(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setReminderMiddayEnabled(enabled)
        if (enabled) {
            val hour = reminderMiddayHour.value
            val minute = reminderMiddayMinute.value
            reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MIDDAY, hour, minute)
        } else {
            reminderScheduler.cancelSlot(ReminderScheduler.SLOT_MIDDAY)
        }
    }
}

fun setReminderMiddayTime(hour: Int, minute: Int) {
    viewModelScope.launch {
        settingsRepository.setReminderMiddayTime(hour, minute)
        if (remindersEnabled.value && reminderMiddayEnabled.value) {
            reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MIDDAY, hour, minute)
        }
    }
}

fun setReminderEveningEnabled(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setReminderEveningEnabled(enabled)
        if (enabled) {
            val hour = reminderEveningHour.value
            val minute = reminderEveningMinute.value
            reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_EVENING, hour, minute)
        } else {
            reminderScheduler.cancelSlot(ReminderScheduler.SLOT_EVENING)
        }
    }
}

fun setReminderEveningTime(hour: Int, minute: Int) {
    viewModelScope.launch {
        settingsRepository.setReminderEveningTime(hour, minute)
        if (remindersEnabled.value && reminderEveningEnabled.value) {
            reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_EVENING, hour, minute)
        }
    }
}
```

- [ ] **Step 4: Build to verify**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ordna/android/ui/settings/SettingsViewModel.kt
git commit -m "feat: add reminder state and scheduling to SettingsViewModel"
```

---

### Task 11: Add Reminders section to SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/ordna/android/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Add state collection for reminders**

Add after the existing `streak` state collection (around line 92):

```kotlin
val remindersEnabled by viewModel.remindersEnabled.collectAsState()
val morningEnabled by viewModel.reminderMorningEnabled.collectAsState()
val morningHour by viewModel.reminderMorningHour.collectAsState()
val morningMinute by viewModel.reminderMorningMinute.collectAsState()
val middayEnabled by viewModel.reminderMiddayEnabled.collectAsState()
val middayHour by viewModel.reminderMiddayHour.collectAsState()
val middayMinute by viewModel.reminderMiddayMinute.collectAsState()
val eveningEnabled by viewModel.reminderEveningEnabled.collectAsState()
val eveningHour by viewModel.reminderEveningHour.collectAsState()
val eveningMinute by viewModel.reminderEveningMinute.collectAsState()
```

- [ ] **Step 2: Add notification permission launcher**

Add imports at the top of the file:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
```

Inside the `SettingsScreen` composable, before the `Scaffold`, add:

```kotlin
val context = LocalContext.current
val snackbarHostState = remember { SnackbarHostState() }
val scope = rememberCoroutineScope()
val permissionDeniedMsg = stringResource(R.string.reminder_permission_needed)

val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) {
        viewModel.setRemindersEnabled(true)
    } else {
        scope.launch { snackbarHostState.showSnackbar(permissionDeniedMsg) }
    }
}
```

- [ ] **Step 3: Add SnackbarHost to Scaffold**

Update the `Scaffold` call to include the snackbarHost parameter:

```kotlin
Scaffold(
    topBar = { /* ... existing ... */ },
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { padding ->
```

- [ ] **Step 4: Add Reminders section UI**

Add after the completion method section and before the "Default creation list" section. Insert after the closing `}` of the `SingleChoiceSegmentedButtonRow` for completion method (around line 225), before the `// Default creation list` comment:

```kotlin
Spacer(modifier = Modifier.height(24.dp))

// ── Reminders ──
SettingSectionHeader(stringResource(R.string.settings_section_reminders))

SettingToggle(
    title = stringResource(R.string.reminder_master_toggle),
    subtitle = stringResource(R.string.reminder_master_subtitle),
    checked = remindersEnabled,
    onCheckedChange = { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.setRemindersEnabled(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.setRemindersEnabled(enabled)
        }
    },
)

ReminderSlotRow(
    label = stringResource(R.string.reminder_morning),
    enabled = remindersEnabled && morningEnabled,
    masterEnabled = remindersEnabled,
    hour = morningHour,
    minute = morningMinute,
    onToggle = { viewModel.setReminderMorningEnabled(it) },
    onTimeChange = { h, m -> viewModel.setReminderMorningTime(h, m) },
)

ReminderSlotRow(
    label = stringResource(R.string.reminder_midday),
    enabled = remindersEnabled && middayEnabled,
    masterEnabled = remindersEnabled,
    hour = middayHour,
    minute = middayMinute,
    onToggle = { viewModel.setReminderMiddayEnabled(it) },
    onTimeChange = { h, m -> viewModel.setReminderMiddayTime(h, m) },
)

ReminderSlotRow(
    label = stringResource(R.string.reminder_evening),
    enabled = remindersEnabled && eveningEnabled,
    masterEnabled = remindersEnabled,
    hour = eveningHour,
    minute = eveningMinute,
    onToggle = { viewModel.setReminderEveningEnabled(it) },
    onTimeChange = { h, m -> viewModel.setReminderEveningTime(h, m) },
)
```

- [ ] **Step 5: Add ReminderSlotRow composable**

Add as a new private composable, after the existing `SettingLabel` composable:

```kotlin
@Composable
private fun ReminderSlotRow(
    label: String,
    enabled: Boolean,
    masterEnabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timeText = String.format("%02d:%02d", hour, minute)
    val alpha = if (masterEnabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { this.alpha = alpha },
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(enabled = masterEnabled) { showTimePicker = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .graphicsLayer { this.alpha = alpha },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            enabled = masterEnabled,
        )
    }

    if (showTimePicker) {
        ReminderTimePicker(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                onTimeChange(h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cd_back))
            }
        },
        text = {
            TimePicker(state = timePickerState)
        },
    )
}
```

Add required imports:

```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.graphics.graphicsLayer
```

- [ ] **Step 6: Build to verify**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/ordna/android/ui/settings/SettingsScreen.kt
git commit -m "feat: add Reminders settings UI with time pickers and permission handling"
```

---

### Task 12: Build APK and verify

**Files:** None (verification only)

- [ ] **Step 1: Full debug build**

Run: `cd /home/alex/GitHub/taskig-android && JAVA_HOME=/home/alex/.sdkman/candidates/java/current ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install on device**

Run: `sudo adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: Success

- [ ] **Step 3: Commit all if any remaining changes**

Verify with `git status` that everything is committed.
