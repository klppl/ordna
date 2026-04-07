# Ordna

A daily task app for Android that syncs with Google Tasks.

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="60">](http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/klppl/ordna/releases)

## Why this exists

Every todo app I tried wanted to be a project manager, a habit tracker, a second brain, or all three. I wanted to see today's tasks, check them off, and close the app.

Ordna pulls from Google Tasks, so your tasks live next to Google Calendar, you can add them from Gmail, and if you stop using Ordna your data is still in Google. No new account needed.

## What it does

- Shows overdue and today's tasks, grouped by list or flat
- Swipe left to complete, swipe right to postpone to tomorrow
- Two home screen widgets (task list and progress counter)
- Share URLs or text from any app into your default task list
- Edit task titles and notes inline
- Streak tracking when you clear all tasks for the day
- Offline mode with background sync when connectivity returns
- 8 dark themes: System/dynamic color, Catppuccin, Rose Pine, Gruvbox, Tokyo Night, Dracula, Kanagawa, Oxocarbon
- English and Swedish

## How it works

Tasks are cached locally in Room and synced with the Google Tasks API every 15 minutes or when you pull to refresh. Changes show up immediately and revert if the API call fails. If you're offline, operations queue up and retry when you're back.

Kotlin, Jetpack Compose, Hilt, WorkManager. Single-activity MVVM. Min SDK 26.

## Building from source

Requires JDK 17+ and the Android SDK.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Google sign-in requires a keystore whose SHA-1 fingerprint matches the OAuth client registered in Google Cloud Console. See `CLAUDE.md` for signing details.

## License

[GPL-3.0](LICENSE)
