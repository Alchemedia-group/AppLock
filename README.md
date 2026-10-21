# AppLock

An Android app locker. Pick the apps you want to protect; AppLock covers them with a PIN / pattern /
fingerprint screen and **silently photographs anyone who fails to unlock**.

## Features

- Lock any launchable app with a **PIN (4–8 digits)** or **3×3 pattern**, plus optional fingerprint/face unlock
- **Intruder selfie**: front-camera photo after N failed attempts (1–5, configurable), including failed
  fingerprints and failed attempts at AppLock's own entry screen. Photos are stored privately with time and the
  app that was targeted; browse and delete them in the *Intruders* tab
- **Navigation bar auto-hides** on every screen (swipe from the edge to reveal it briefly) — see `ImmersiveMode`
- Relock policy: as soon as you leave the app, after 1 / 5 minutes, or when the screen turns off
- Optional shuffled PIN keypad, 30-second lockout after every 5 wrong attempts
- **Onboarding**: welcome, choose a PIN or pattern, save a recovery code, then grant the service, camera and notification access
- **Recovery code**: a one-time 12-character code (stored only as a salted hash) resets a forgotten PIN or pattern from the lock screen, and can be regenerated in Settings. Wrong codes count as failed attempts and trigger the intruder photo
- **Fake crash screen** (optional): locked apps look like they crashed with "keeps stopping". Long-press *Close app* to reveal the real lock
- **Hidden notifications** (optional): notifications from locked apps are replaced with "New notification" (needs notification access)
- **Share and save intruder photos** from the viewer, through the system share sheet or a file you choose
- **Auto-delete** intruder photos after 7, 30 or 90 days
- **Backup and restore** of locked apps and preferences as a JSON file. It never contains your PIN, recovery code or photos
- Lock/overview screens are `FLAG_SECURE` (no screenshots, blank recents thumbnail)
- No `INTERNET` permission. The secret is stored only as a salted PBKDF2-SHA256 hash

## Pro and languages

- **AppLock Pro** is a one-time in-app purchase (£4.99) that unlocks the intruder photo. Everything else is free.
  Billing uses Google Play Billing (`billing/BillingManager`, product id `applock_pro`). To make it purchasable,
  create a one-time product with that id in Play Console, set its GBP price to £4.99, activate it, and install a
  build from an internal-testing track. The app shows the price Google Play returns for the user's country, and
  re-checks ownership each time it opens. Debug builds have an *Enable Pro for testing* button; release builds do not.
- **Languages:** English, Spanish, German, French, Italian, Portuguese and Polish. The app follows the phone language;
  on Android 13+ you can also pick a language in Settings. Strings live in `res/values*/strings.xml` and a unit test
  checks every language has every string and matching placeholders.

## How it works

An `AccessibilityService` (`service/AppLockAccessibilityService`) listens for window-state changes — nothing else,
it can't read screen content — and starts `LockActivity` over a protected app in its own task. A successful unlock
is remembered in memory (`UnlockSession`) according to the relock policy. `LockActivity` keeps the front camera
bound (no preview) so a photo can be taken instantly on a failed attempt.

## Stack

Kotlin 2.4, Jetpack Compose (Material 3), CameraX 1.6, AndroidX Biometric, DataStore, AGP 9.4, Gradle 9.8,
`compileSdk`/`targetSdk` 37, `minSdk` 26. Versions live in `gradle/libs.versions.toml`.

## Build

Requires JDK 17+ and the Android SDK (platform 37).

```
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

## First run

1. Choose a PIN or pattern.
2. Open the **Apps** tab and follow the cards: enable the *AppLock* accessibility service and allow the camera.
   On Android 13+ for sideloaded builds the accessibility switch may be greyed out: open AppLock's *App info* →
   ⋮ → **Allow restricted settings**, then try again.
3. Flip the switch next to the apps you want to protect.

## Known limits

- Android shows a green camera indicator while the camera is in use; this can't be hidden.
- Someone who knows your device PIN can disable the accessibility service or uninstall AppLock. A device-admin
  uninstall guard is not implemented yet.
- Google Play restricts accessibility-service apps; publishing there needs a Permissions Declaration.
- Built and unit-tested on a build machine only; not yet exercised on a physical device or emulator.
- Hiding notifications works by dismissing the original and posting a placeholder, so the original's quick actions are lost until you open the app.
