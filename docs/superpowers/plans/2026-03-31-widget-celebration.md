# Widget Celebration & Streak Display

> **For agentic workers:** Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Show a clean celebration state in both widgets when all tasks are complete, with streak counter visible.

**Architecture:** Add a `streakFlow` companion function to SettingsRepository (matching existing `widgetSettingsFlow` pattern). Both widgets collect it and display streak when all tasks are done. OrdnaWidget replaces the task list with a centered celebration. CounterWidget adds streak below "All done".

**Tech Stack:** Glance widgets, DataStore, Kotlin Flows

---

### Task 1: Add streakFlow to SettingsRepository companion

**Files:**
- Modify: `app/src/main/java/com/ordna/android/data/repository/SettingsRepository.kt:333-348`

- [ ] **Step 1: Add streakFlow companion function**

Add inside the `companion object` block, after `listOrderFlow`:

```kotlin
/** Reactive Flow for widget — emits whenever streak count changes. */
fun streakFlow(context: Context): Flow<Int> =
    context.settingsDataStore.data.map { prefs ->
        prefs[intPreferencesKey("streak_count")] ?: 0
    }
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add widget celebration strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-sv/strings.xml`

- [ ] **Step 1: Add English strings**

Add after `widget_refresh` in the Widget section:

```xml
<string name="widget_all_done">All done</string>
<string name="widget_all_done_subtitle">Nothing left for today</string>
<string name="widget_streak">%1$d-day streak</string>
```

- [ ] **Step 2: Add Swedish strings**

Add after `widget_refresh` in the Widget section:

```xml
<string name="widget_all_done">Allt klart</string>
<string name="widget_all_done_subtitle">Inget kvar för idag</string>
<string name="widget_streak">%1$d dagar i rad</string>
```

---

### Task 3: OrdnaWidget celebration state

**Files:**
- Modify: `app/src/main/java/com/ordna/android/widget/OrdnaWidget.kt`

- [ ] **Step 1: Collect streak in provideGlance**

Add streak flow alongside existing flows in `provideGlance`:

```kotlin
val streakFlow = SettingsRepository.streakFlow(context)
// in snapshots:
val initialStreak = streakFlow.first()
// in provideContent:
val streak by streakFlow.collectAsState(initial = initialStreak)
```

Pass `streak` to `WidgetContent`.

- [ ] **Step 2: Add streak parameter to WidgetContent**

Add `streak: Int` parameter to `WidgetContent` composable.

- [ ] **Step 3: Add allCompleted branch**

Inside `WidgetContent`, after the progress bar section, replace the current `if (totalCount == 0) ... else ...` block. Add a new branch for `allCompleted`:

```kotlin
val allCompleted = totalCount > 0 && completedCount == totalCount

if (totalCount == 0) {
    // existing "No tasks today" — keep as-is
} else if (allCompleted) {
    // Celebration state — centered, clean
    Spacer(modifier = GlanceModifier.defaultWeight())
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "✓",
            style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ColorProvider(completedColor)),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = ctx.getString(R.string.widget_all_done),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = ctx.getString(R.string.widget_all_done_subtitle),
            style = TextStyle(fontSize = 12.sp, color = subtextColor),
        )
        if (streak > 0) {
            Spacer(modifier = GlanceModifier.height(12.dp))
            Text(
                text = ctx.getString(R.string.widget_streak, streak),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subtextColor),
            )
        }
    }
    Spacer(modifier = GlanceModifier.defaultWeight())
} else {
    // existing task list — keep as-is
}
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 4: CounterWidget streak display

**Files:**
- Modify: `app/src/main/java/com/ordna/android/widget/CounterWidget.kt`

- [ ] **Step 1: Collect streak in CounterWidget.provideGlance**

Add streak flow alongside existing flows:

```kotlin
val streakFlow = SettingsRepository.streakFlow(context)
val initialStreak = streakFlow.first()
// in provideContent:
val streak by streakFlow.collectAsState(initial = initialStreak)
```

Pass `streak` to `CounterContent`.

- [ ] **Step 2: Add streak parameter and display in CounterContent**

Add `streak: Int` parameter. After the "All done" text (when remaining == 0), add streak display:

```kotlin
Text(
    text = if (remaining > 0) "$remaining" else ctx.getString(R.string.counter_widget_done),
    style = TextStyle(
        fontSize = if (remaining > 0) 32.sp else 14.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
    ),
)
if (remaining > 0) {
    Text(
        text = ctx.getString(R.string.counter_widget_left),
        style = TextStyle(fontSize = 12.sp, color = subtextColor),
    )
} else if (streak > 0) {
    Spacer(modifier = GlanceModifier.height(4.dp))
    Text(
        text = ctx.getString(R.string.widget_streak, streak),
        style = TextStyle(fontSize = 11.sp, color = subtextColor),
    )
}
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 5: Build release APK

- [ ] **Step 1: Build signed release APK**

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL, signed APK at `app/build/outputs/apk/release/app-release.apk`
