# AppLocker

An Android app that locks apps you choose until you earn access by walking, and re-locks them on a schedule. Everything runs on the device. There is no account, backend, analytics, or network access.

This is a personal, sideloaded build for one device (Pixel 11 with a Galaxy Watch Ultra feeding steps through Samsung Health and Health Connect).

## Status: Phase 0

Phase 0 is the project skeleton and the first-launch permission flow. Apps are not blocked yet.

What works:

- First launch walks through each permission one at a time with a plain reason and a button to the exact Settings screen: Accessibility, overlay (shown as skipped), Usage Access, Health Connect (steps), battery unrestricted, notifications.
- The checklist re-reads every permission each time the app returns to the foreground, so it updates after you come back from Settings.
- The Home screen shows "Protection: OFF" with the reason. It stays off in this phase because nothing enforces blocking yet.
- Settings are stored in DataStore and seeded once from named `DEFAULT_*` constants. After seeding, the stored value governs behaviour.
- A stub Accessibility service lets the app appear in Settings. It listens for window-state events only and cannot read screen content.

What does not exist yet: app picker, schedules, lock screen, earning rules, timed unlock windows. These come in later phases.

## Build

You need a JDK 21 and the Android SDK (platform 37, build-tools 36 or newer).

1. Create `local.properties` in the project root (it is git-ignored) with your SDK path, for example `sdk.dir=C\:/Users/you/android-sdk`.
2. Run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
3. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests with `./gradlew testDebugUnitTest` and lint with `./gradlew lintDebug`.

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
| Health Connect client | 1.1.0 (latest stable) |
| compileSdk, targetSdk | 37 |
| minSdk | 34 |

minSdk is 34 because Health Connect is part of the Android framework from API 34.

## Verified on the Pixel 11

Phase 0 checklist passed on 11 November 2026.

- Android version: 17. The API level was not read from the device; Android 17 corresponds to API 37, which matches the compileSdk the libraries required.
- Health Connect is built in, not a separate app.
- Restricted Settings appeared for the sideloaded APK, so the guidance screen is needed.

Navigation Compose, Room, CameraX and ML Kit are not added yet. Each is added in the phase that needs it.

## Tuning constants and defaults

Product defaults live in `settings/AppDefaults.kt` as `DEFAULT_*` constants and are only used to seed first-run settings: bedtime time, bedtime lead, day-boundary time, warning thresholds. Earn-window and step defaults are added with the earning rules in Phase 2.

Tuning constants (implementation values, not product settings):

- `notificationTimeout="100"` in `res/xml/accessibility_service_config.xml`: the minimum milliseconds between Accessibility events delivered to the service. Not used for blocking decisions in Phase 0.

## Known limits

- Phase 0 does not block anything.
- Android offers no API to lock another app. Later phases detect the foreground app through an Accessibility service and cover it with a lock screen. A brief flash of the locked app can happen.
- A user can always turn off Accessibility, uninstall the app, or Force Stop it. This is a commitment aid, not a security boundary.
- Unit tests use an in-memory DataStore because the file-backed one fails to replace its file on Windows.

## Open decisions

The spec lists open decisions that gate later phases. Phase 1 needs TBD-01 and TBD-09. Phase 2 needs TBD-17b (late-sync grace).
