# Megs Consultation – Consultation & Appointment Manager (Android)

A simple, offline-first Android app that replaces WhatsApp-based consultation tracking
(`Dr. Athila – ₹12,000`, `Ekaa – ₹9,400` …) with appointments, alarm-style reminders and earnings history.

## Features
| Screen | What it does |
|---|---|
| **Home** | Greeting, Today / Upcoming-this-week / This-month-fees cards, "Today: N consultations • ₹X", Today / Upcoming / Completed tabs, appointment cards with Snooze, Off, Call, WhatsApp |
| **Add / Edit** | Name, type, date, time, place, optional fee, paid toggle, notes (200 chars), optional phone. Name auto-suggest fills last place/fee/phone. Recent-place chips. Multiple reminders (1 day / 1 hour / 30 min / at time / custom). Sound+Notification or Notification-only |
| **Alarm** | Full-screen alert over lock screen, keeps ringing until Snooze/Dismiss, Snooze 5/10/15/30 min; also Snooze/Dismiss buttons in the notification |
| **Calendar** | Month grid with appointment dots, per-day list, add on selected day, month total |
| **Alerts** | Setup checks (notifications, exact alarms, full-screen, battery optimisation), test alarm, all scheduled reminders with on/off |
| **History** | Search, month / all-time, paid / unpaid, place filter, totals, group by date or by name, share summary to WhatsApp |

Entering a consultation dated in the past saves it directly as **Completed** – quick logging like the WhatsApp workflow.
Past appointments still marked Upcoming can be closed in one tap from Home ("Mark all").

## Tech
Kotlin · Jetpack Compose (Material 3) · Room (local SQLite) · AlarmManager `setAlarmClock` (exact, Doze-safe) ·
BroadcastReceivers for alarm / snooze / reboot rescheduling · minSdk 26 (Android 8.0) · targetSdk 34.

## Build & install
1. Install **Android Studio** (Koala 2024.1 or newer).
2. *File → Open* → select the `MegsConsultation` folder. Let Gradle sync (downloads Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20).
   - If Studio asks about the Gradle wrapper, accept *"Use Gradle wrapper / create it"*; or run `gradle wrapper` once.
3. Connect the phone (Developer options → USB debugging) and press **Run ▶**.
4. Shareable APK: *Build → Build App Bundle(s)/APK(s) → Build APK(s)* → `app/build/outputs/apk/debug/app-debug.apk`.
   For a signed release: *Build → Generate Signed App Bundle / APK*.

## First-run checklist on the phone
Open **Alerts** tab and fix anything in amber:
- Allow notifications
- Allow full-screen alerts (Android 14+)
- Battery: set app to *Unrestricted / Don't optimise* (important on Xiaomi, Oppo, Vivo, Realme, Samsung)
- Tap **Test reminder (5 sec)** and lock the screen to verify the alarm.

## Customising
`util/AppConfig.kt` – app name, doctor name, appointment types.
Theme colours – `ui/Theme.kt`.

## Project layout
```
app/src/main/java/com/megs/consultation/
├── MainActivity.kt
├── data/        Appointment entity, DAO, Room database
├── reminder/    ReminderScheduler, NotificationHelper, Alarm/Action/Boot receivers
├── ui/          MegsApp (nav), Theme, Components, AlarmActivity, MainViewModel
│   └── screens/ Home, AddEdit, Calendar, Alerts, History
└── util/        Formatting (₹ Indian grouping), dates, call/WhatsApp/share
```

## Roadmap
- Backup / restore (JSON export to Google Drive) and CSV export
- Settings screen (doctor name, default snooze, default reminders)
- Home-screen widget for today's appointments
- Optional cloud sync

## License
MIT – see `LICENSE`.

## Build without Android Studio (GitHub Actions → shareable link)
1. Create a GitHub repo and upload this folder (including `.github/workflows/build-apk.yml`).
2. Every push to `main` builds the APK in the cloud (~5–8 min, see the **Actions** tab).
3. The APK is published to the **latest** release. Shareable download link:
   `https://github.com/<your-username>/<repo>/releases/download/latest/MegsConsultation.apk`
