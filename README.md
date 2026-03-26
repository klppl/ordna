# Taskig

A daily task app that does less, on purpose.

## Why

I tried all the todo apps. They want to be project managers, habit trackers, second brains, or all three. I just wanted to see what I need to do today, check things off, and move on.

Taskig shows overdue and due-today tasks. No projects, no priorities, no tags. If it's not due today, it's not on your screen.

The backend is Google Tasks. Your tasks live next to your Google Calendar, you can add them from Gmail, and you don't need another account. If you stop using Taskig, your tasks are still in Google.

## What it does

- Overdue + today's tasks, grouped by list or flat
- Swipe left to complete, swipe right to postpone
- Home screen widget
- Share URLs and text from any app straight into your default list
- 7 dark themes (Catppuccin, Rosé Pine, Gruvbox, Tokyo Night, Dracula, Kanagawa, Oxocarbon) plus system/dynamic color
- Swedish and English

## Setup

1. Create a Google Cloud project, enable the Google Tasks API
2. Create an Android OAuth 2.0 client ID with package `com.taskig.android` and your debug SHA-1:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
   ```
3. Build: `./gradlew assembleDebug`
4. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Tech

Jetpack Compose, Material 3, Room, Hilt, Glance widget, WorkManager, Google Tasks API.
