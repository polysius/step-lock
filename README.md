# AppLocker

An Android app that locks apps you choose until you earn access by walking, and re-locks them on a schedule. Everything runs on the device. There is no account, backend, analytics, or network access.

This is a personal, sideloaded build for one device (Pixel 11 with a Galaxy Watch Ultra feeding steps through Samsung Health and Health Connect).

## Status: Phase 1 (schedule blocking), tested on the Pixel 11

Phase 0 (the first-launch permission flow) passed on the Pixel 11. Phase 1 adds schedule-only blocking and was tested on the Pixel 11.

What Phase 1 does:

- Pick the apps to lock, or switch on all-apps mode, where every app is locked except the ones you allow.
- Schedule blocks and breaks on chosen days, including blocks that cross midnight, plus a bedtime lock. Bedtime time, lead minutes and end time are all editable.
- When a locked app comes to the front during a block or the bedtime lock, a full-screen lock screen covers it. It shows the reason and the end time and has one button, Back to Home.
- Strict-only: while a block or the bedtime lock is active, nothing can weaken it. Refused changes show "Strict: this cannot be changed until HH:MM". There is no emergency unlock.
- History records every blocked attempt, with the time the lock screen took to appear, and every change.

What does not exist yet: earning access with steps (Phase 2), timed unlock windows (Phase 3), workouts and calories (Phase 4), treadmill scanning (Phase 5).

### How enforcement works

An Accessibility service listens for window-state events and reads only the package name and the window class. When the front app is locked, it adds an Accessibility overlay window with the lock screen. That window type needs no "Draw over other apps" permission and does not start an activity.

This differs from the spec's backup detector. The spec also proposes a UsageStats poller in a foreground service. Android's documentation lists no background-activity-launch exemption for a foreground service, and without the overlay permission a poller could only detect an open app, not lock it. So the poller is not built. If Accessibility is turned off, Home shows "Protection: DEGRADED" and apps are not blocked. The overlay permission stays skipped in the setup flow.

A still-open locked app is re-checked every 15 seconds, so a block that starts while the app is open locks it.

After a reboot, Android re-binds the enabled Accessibility service by itself. This was confirmed on the Pixel 11. The Android documentation does not promise it.

### Decisions recorded (spec section 15)

- TBD-01: personal sideload only.
- TBD-08: same schedule every day. The shower and lunch slots are breaks. The gym slot is a locked block until earning exists.
- TBD-09: Samsung Health and Maps stay open during blocks, on top of the always-allowed system apps.
- TBD-16: Instagram only to start.
- The bedtime lock ends at 05:45. A window that crosses midnight belongs to the day it starts.
- Changes that would unlock an app are refused until the lock ends. That covers unlocking an app, leaving all-apps mode, allowing an app in all-apps mode, turning protection off, adding an active break, editing an active rule, and changing bedtime during the bedtime lock.

### Where this differs from the spec's proposal

- Bedtime settings live in DataStore (decided in Phase 0), not in the schedule table.
- History is one table with a type column, not a separate block-event table.
- Navigation is a tab bar, not Navigation Compose.
- Day filtering and CSV export in History are not built.
- Bedtime applies every day and cannot be switched off. The spec lets its days be edited.
- The UsageStats poller, foreground service and boot receiver are not built (see above).
- The lock screen delay in History is measured until Android accepts the overlay window, not until the first frame is drawn, so it understates the flash a little.

## Build

You need a JDK 21 and the Android SDK (platform 37, build-tools 36 or newer).

1. Create `local.properties` in the project root (it is git-ignored) with your SDK path, for example `sdk.dir=C\:/Users/you/android-sdk`.
2. Run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
3. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests with `./gradlew testDebugUnitTest` and lint with `./gradlew lintDebug`. `./gradlew createDebugUnitTestCoverageReport` writes a coverage report to `app/build/reports/coverage`.

The rules engine (`rules` package) is pure Kotlin. Its tests cover every line and every branch (84 of 84 branches, 82 of 82 lines) as of Phase 1.

## Install on the phone

1. Enable Developer options and USB debugging on the phone, then connect it.
2. Run `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or copy the APK to the phone and open it.
3. Follow the checklist in `TESTING.md`.

If Accessibility or Usage Access is greyed out, Android is applying Restricted Settings to a sideloaded app. In the app, tap Open app info, tap the three-dot menu at the top, and enable restricted settings (the Settings label reads "Restricted settings"). Verified on the Pixel 11 for the Phase 0 APK.

## Versions used

Checked against Google Maven on 11 November 2026.

| Component | Version |
| --- | --- |
| Android Gradle Plugin | 9.5.1 |
| Gradle | 9.9.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Activity Compose | 1.13.0 |
| Lifecycle | 2.11.0 |
| DataStore | 1.3.0 |
| Room | 2.8.5 |
| KSP | 2.3.13 |
| kotlinx.coroutines | 1.11.0 |
| Health Connect client | 1.1.0 (latest stable) |
| compileSdk, targetSdk | 37 |
| minSdk | 34 |

minSdk is 34 because Health Connect is part of the Android framework from API 34.

CameraX and ML Kit are not added yet. Each is added in the phase that needs it.

## Verified on the Pixel 11

Phase 0 checklist passed on 11 November 2026.

- Android version: 17. The API level was not read from the device; Android 17 corresponds to API 37, which matches the compileSdk the libraries required.
- Health Connect is built in, not a separate app.
- Restricted Settings appeared for the sideloaded APK, so the guidance screen is needed.

Phase 1 results on the Pixel 11 (Android 17):

- The lock screen appears 12 to 51 ms after a locked app opens, with no visible flash.
- Back and Back to Home both land on the launcher.
- Blocking resumes after a reboot without opening AppLocker.
- The status bar still shows above the lock screen. The blocked app is not visible through it.
- The first build crashed when the overlay appeared, because the lifecycle owner was set on the wrong view. That is fixed.
- Open: the owner reported that the bedtime lock did not override a break. The rules engine tests say it should, so the test setup is the first suspect, since a break had been changed into a block on the device. It has not been reproduced or fixed.
- Not measured: battery use over a full day.

## Tuning constants and defaults

Product defaults live in `settings/AppDefaults.kt` and `settings/SchedulePreset.kt` as `DEFAULT_*` constants. They only seed first-run settings: bedtime time, lead and end, day-boundary time, warning thresholds, the first locked app, the allowed apps and the schedule. Earn-window and step defaults are added with the earning rules in Phase 2.

The preset schedule uses generic names because this repository is public. Edit it in `SchedulePreset.kt` to change the first-run schedule, or edit it in the app after install.

Tuning constants (implementation values, not product settings):

- `notificationTimeout="0"` in `res/xml/accessibility_service_config.xml`: no throttling of window-state events, so a quick keyboard or system event cannot replace the event for the locked app.
- `RECHECK_INTERVAL_MILLIS = 15_000` in `AppLockerAccessibilityService`: how often a still-open app is re-checked.
- `CACHE_MILLIS = 60_000` in `AlwaysAllowedApps`: how long the looked-up dialler, launcher, clock and Settings packages are reused.
- `NOW_REFRESH_MILLIS = 20_000` in `ui/Formatting.kt`: how often on-screen "locked until" text refreshes.
- `HISTORY_LIMIT = 200` in `MainViewModel`: how many History entries the screen loads.
- `MAX_LEAD_DIGITS = 3` and the new-rule form start and end times in `ScheduleScreen.kt`: limits and starting values for the editors, not rules.

## Known limits

- A brief flash of the locked app before the lock screen appears is possible. History reports the measured delay for each block.
- Enforcement is paused while the process is dead or Accessibility is off.
- Android offers no API to lock another app. A user can always turn off Accessibility, uninstall the app, or Force Stop it. This is a commitment aid, not a security boundary.
- Unit tests use an in-memory DataStore because the file-backed one fails to replace its file on Windows.

## Open decisions

Phase 2 needs TBD-17b (late-sync grace). Phase 3 needs TBD-05 (wall-clock or usage-deducted minutes). The rest are listed in section 15 of the spec.
