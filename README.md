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
- Lock/overview screens are `FLAG_SECURE` (no screenshots, blank recents thumbnail)
- No `INTERNET` permission. The secret is stored only as a salted PBKDF2-SHA256 hash

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
./gradlew assembleDebug
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
- Not yet built or run on a device: this code was written without an Android SDK available.
