# Testing

Tick each box on the Pixel 11. Record anything that differs from what is written, including the exact labels Android shows.

## Phase 0: first-launch permission flow

Install the debug APK (see `README.md`), then open AppLocker.

- [x] Fresh install opens on "Set up AppLocker", step 1 of 6 (Accessibility).
- [x] Step 1: "Open settings" opens the Accessibility screen. AppLocker is listed.
- [x] If the AppLocker switch is greyed out, the "Switch greyed out?" card is shown. Note the exact path you used to allow restricted settings: Clicked "Open app info -> Clicked the 3 dots up top -> Enabled restricted access"
- [x] After turning Accessibility on and returning, step 1 shows "Granted" without leaving the screen.
- [x] Step 2 (Draw over other apps) shows "Not needed" and no button.
- [x] Step 3: "Open settings" opens Usage access. AppLocker is listed and can be turned on. Returning shows "Granted".
- [x] Step 4: "Allow" shows the Health Connect steps prompt. Granting it shows "Granted". Denying it keeps "Missing" and leaves the Health Connect settings button available.
- [x] Step 4: the Health Connect privacy link opens "How AppLocker uses your health data".
- [x] Step 5: "Open settings" shows the battery dialog. Allowing it shows "Granted".
- [x] Step 6: "Allow" shows the notification prompt. Allowing it shows "Granted".
- [x] While any permission is missing, a red "Protection stays OFF" line is shown.
- [x] "Not now" goes to Home. Home shows "Protection: OFF — Accessibility disabled" when Accessibility is off, and "Protection: OFF — blocking is not built yet (Phase 0)" when it is on.
- [x] Home lists all six items with their state, and "Continue setup" appears while any is missing.
- [x] Turn Accessibility off in Settings, return to AppLocker: the checklist shows it Missing again.
- [x] Close and reopen the app with a permission missing: it opens on setup again.
- [x] With every permission granted, reopening the app goes straight to Home.

## Facts to record for the README

- [x] Android version and API level on the Pixel 11: android 17 and idk api level
- [x] Health Connect is built in (not a separate app): yes 
- [x] Restricted Settings appeared for the installed APK: yes
- [x] Exact Settings labels seen for the Restricted Settings steps: Restricted settings

## Later phases

Checklists for Phases 1 to 6 are added when each phase is built.
