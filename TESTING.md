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

## Phase 1: schedule-only blocking (strict-only)

Install the Phase 1 debug APK over Phase 0, open AppLocker, make sure every permission is granted, then on Home tap "Turn protection on". The first-run preset locks Instagram (install it, or lock another app you have) and creates the schedule shown in the Schedule tab. All times are editable. To test without waiting for the clock, add a block for the next few minutes (allowed while it is not yet active), or edit an inactive block to cover now.

Blocking and lock screen:

- [x] With protection on and a block active, opening a locked app shows the AppLocker lock screen over it. The app's content is not usable.
- [x] The lock screen shows the app name and icon, the reason (block name or bedtime), the end time, and one "Back to Home" button. There is no unlock or emergency button anywhere.
- [x] "Back to Home" goes to the launcher. The Back key does the same and never lands inside the locked app.
- [x] Opening a locked app 3 times (Home in between) creates 3 "Blocked by ..." entries in History.
- [x] History shows "lock screen shown after N ms" for each block. Typical value: 12 to 51 ms (measured over adb after the fix). Anything over 1000 ms is a defect to report.
- [x] Outside every block and the bedtime lock, the same app opens normally.
- [x] During a break (for example 09:00 to 10:00 in the preset), the app opens.
- [x] Bedtime: with bedtime 22:30 and lead 45, the lock starts at 21:45 (the Schedule tab shows it). Change the lead to 30 and it starts at 22:00. Change bedtime to 23:00 with lead 45 and it starts at 22:15. Change these outside the bedtime lock only. Retested after the overlay fix: works.
- [ ] The bedtime lock overrides a break and ends at 05:45 (or your edited end time). -Nope
- [x] A block that starts while the locked app is already open locks it within about 15 seconds.
- [x] Dialler, Settings, the launcher, the alarm app, Samsung Health and Maps open during a block. - yes
- [x] An incoming call can be answered and an alarm can be dismissed while a block is active.
- [x] AppLocker itself opens during a block and its screens work.
- [x] Pulling down the notification shade or opening the keyboard does not show or clear the lock screen by mistake.
- [x] Turning Accessibility off in Settings makes Home show "Protection: DEGRADED" and locked apps open freely. Turning it on again resumes blocking without turning protection on again.
- [x] Reboot the phone, do not open AppLocker, then open a locked app during a block. It is blocked. If not, note how long after boot it started working: works after the overlay fix.

Strict-only. During an active block or the bedtime lock, each of these must show "Strict: this cannot be changed until ..." and change nothing:

- [x] Edit, disable or delete the active block.
- [x] Turn off the Instagram switch in Apps.
- [x] Turn off "Lock all apps".
- [x] Add a break that covers the current time, or enable one that does.
- [x] Change bedtime time, lead or end time during the bedtime lock.
- [x] Turn protection off on Home.
- [x] In all-apps mode, allow an app (switch it on).
- [x] With no block active, all of the above work normally. Retested after the overlay fix: works.
- [x] No screen, setting or button opens a locked app during a block. The only way is waiting for it to end.

Apps and schedule screens:

- [x] Apps lists installed apps with name and icon. Search works. Selected apps are listed first, then A to Z, and the screen says so.
- [ ] Locking another app takes effect on its next open, without a reboot. Unlocking it outside a block works.
- [ ] Add, edit and delete a block outside its active time. A block that crosses midnight shows the "runs past midnight" note.
- [x] Changes survive closing the app and a reboot.

Full-day battery test (spec section 10.3, Phase 1 exit). Battery use for AppLocker after a normal day with protection on: ______ %

## Facts to record for the README (Phase 1)

- [x] Typical lock screen delay shown in History: 12 to 51 ms
- [x] Visible flash of the locked app before the lock screen: no, how long: ______
- [x] Blocking works after reboot without opening AppLocker: yes (retested after the overlay fix)
- [x] The lock screen covers the status and navigation bars: no

## Later phases

Checklists for Phases 2 to 6 are added when each phase is built.
