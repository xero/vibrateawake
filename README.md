# Vibrate Awake

![Vibrate Awake](./store/feature-graphic.png)

> A simple, offline Android app that buzzes your phone on a set interval to help
> you stay alert on long drives, night shifts, and late trips home. Set it once,
> press Start, and put the phone down; a foreground service keeps it vibrating
> even with the screen off and locked, until you press Stop. If a Wear OS watch
> is paired, it buzzes your wrist on the same schedule too, automatically. Nothing
> to tap or dismiss while you drive. This file documents what it does, how it is
> built, and how to run it.

---

> ### Table of Contents
> - [What it does](#what-it-does)
> - [Settings](#settings)
> - [How it works](#how-it-works)
> - [Wear OS companion](#wear-os-companion)
> - [Screenshots](#screenshots)
> - [Play Store beta testing](#play-store-beta-testing)
> - [Project layout](#project-layout)
> - [Build and run](#build-and-run)
> - [Toolchain](#toolchain)
> - [Design notes](#design-notes)
> - [Privacy](#privacy)
> - [License](#license)

---

## What it does

The whole app is one screen: a settings form with four knobs and a large
Start/Stop button across the bottom. Press Start and it buzzes your phone on
your chosen interval to help you stay alert on long drives, night shifts, and
late trips home. Press Stop, in the app or from the notification, and it ends.

The design is deliberately distraction-free. There is nothing to tap or dismiss
while you drive. It just vibrates.

The published Play Store listing text lives in
[store/listing/full-description.txt](./store/listing/full-description.txt), with
the short description and title alongside it.

- **Fires immediately.** Start plays your chosen pattern at once, so you feel exactly what you picked before setting off.
- **Runs locked.** A foreground service keeps the vibration going with the screen off and the phone locked.
- **Buzzes your watch too.** If a Wear OS watch is paired, the companion buzzes your wrist on the same schedule, with no setup and no extra taps. See [Wear OS companion](#wear-os-companion).
- **Fights habituation.** The interval is jittered by up to 15 seconds each cycle so your brain cannot anticipate the buzz, and a faint pre-warn pulse fires about 15 seconds before each main alert so a hard buzz never startles you mid-steer.
- **Greyscale with an orange accent.** A black-and-white UI that follows the system light or dark setting. A fixed orange (`#D16A00`) picks out the title, the slider fills, the selected options, and the Stop button while running. The title and Start/Stop button use the embedded Thunderline font; everything else is Roboto.

---

## Settings

**Vibration Interval.** How often it buzzes. A slider from 1 to 25 minutes in 30-second steps, defaulting to 3 minutes. Captions at 1, 7, 13, 19, and 25 minutes mark the scale, and the current value shows above it.

**Vibration Rhythm.** The waveform of each alert.
- **Staccato** (default): an escalating heartbeat, soft then medium then a hard final burst.
- **SOS Pulse**: three quick maximum-intensity jabs.
- **Continuous Wave**: a rising and falling siren.

**Vibration Intensity.** How hard the pulses hit.
- **Standard** (default): uses each rhythm's natural amplitude curve, gentle to strong.
- **Maximum**: fires every active pulse at full 255 amplitude.

**Vibration Length.** How long each buzz runs, shown as a Duration from 1/2 to 4, defaulting to 1. A longer buzz also feels stronger, so this doubles as an intensity control where the hardware caps amplitude. Testers found the original buzzes too short, so the default already stretches the waveform to twice its raw length and the slider reaches well beyond.

Settings persist across launches. While the service is running every control dims and locks, leaving the orange Stop button as the only action.

---

## How it works

**`VibrationEngine`** owns the schedule. It runs on the main looper, computes the next delay as `interval ± up to 15s` with a 30-second floor, schedules a faint pre-warn one-shot 15 seconds ahead of each main alert, then plays the waveform with `VibrationEffect.createWaveform`. The actual `(timings, amplitudes)` come from `VibrationConfig.buildWaveform()` in the shared `:core` module, which time-stretches the rhythm by the Duration setting and, for Maximum, rewrites every active amplitude to 255.

**`VibrateAwakeService`** is a foreground service that houses the engine and a partial wake lock, so alerts keep firing under Doze and while locked. It reads the config from the launch intent's extras, posts an ongoing low-importance notification with a Stop action, and returns `START_REDELIVER_INTENT` so the config survives a process restart. On start and stop it also signals any paired watch through `WatchSessionBridge` (see [Wear OS companion](#wear-os-companion)).

**`MainViewModel` and `SettingsRepository`** hold the config as a `StateFlow` backed by DataStore, and expose start and stop. **`ServiceState`** is a small singleton `StateFlow<Boolean>` the service flips so the UI knows whether to show Start or Stop.

**`MainActivity`** hosts the Compose UI and handles the runtime prompts: it requests `POST_NOTIFICATIONS` on Android 13+ and offers the battery-optimization exemption the first time you press Start. It also renders the in-app privacy policy (reached from the shield icon) and opens the contact links in the default browser with no referrer.

---

## Wear OS companion

If a Wear OS watch is paired, it buzzes on the same schedule as the phone, automatically. There is nothing to install by hand, no watch screen to drive, and no setting to flip. The phone stays the controller; the watch executes on its own once told to start.

**How it connects.** When you press Start, `WatchSessionBridge` on the phone writes one `/session` item over the Wear Data Layer (`DataClient`) carrying the state and your config; Stop writes a stopped item. On the watch, a `WearableListenerService` observes that item and cold-starts, even if the watch app was not already running.

**Why the watch runs itself.** Rather than have the phone ping the watch on every buzz, which would fail whenever Bluetooth dropped, the watch runs its own foreground service, its own `AlarmManager` tick loop, and its own `VibrationEffect`, built from the same `:core` waveform code as the phone. After the one start handshake, Bluetooth can drop for the whole drive and every buzz still fires on the watch's local clock. It stops on the phone's stopped item, or on a safety cap if the phone ever goes away without sending one.

**Watch haptics are weaker.** Amplitude is capped by the OS at 255, and watch actuators are smaller and often report no amplitude control at all. Where they do not, the tuned amplitude envelope degrades to an on/off timing pattern so the rhythm still lands. Expect the wrist buzz to be gentler than the phone; the Duration setting does the heavy lifting there.

**Packaging.** The companion is a separate `:wear` module that shares the phone's package name and signing key, declared non-standalone since the phone drives it. Google Play ships it to paired watches from the one listing, on a dedicated Wear release track independent of the phone track.

Notification mirroring, the "post a high-priority notification and let the phone forward it to the watch" approach, was considered and rejected: ongoing service notifications are never bridged, a per-buzz alerting notification would also disturb the phone, and the custom vibration pattern does not carry to the wrist. Only a real watch app can deliver a strong, tuned buzz.

---

## Screenshots

| Main screen | Custom options | Running, locked |
| --- | --- | --- |
| ![Main screen](./store/screenshots/01-main-dark.png) | ![Custom options](./store/screenshots/02-options.png) | ![Running and locked](./store/screenshots/03-running.png) |

| Privacy policy | Notification | Light theme |
| --- | --- | --- |
| ![Privacy policy](./store/screenshots/04-privacy.png) | ![Notification](./store/screenshots/05-notification.png) | ![Light theme](./store/screenshots/06-main-light.png) |

---

## Play Store beta testing

The app is in closed testing on Google Play, so access is invite-only and takes two steps.

**1. Join the testers group.** The group is public, but its membership list is private, so you will not see other members. Join at [groups.google.com/g/vibrate-awake-testing](https://groups.google.com/g/vibrate-awake-testing).

**2. Opt in and install.** Once you are in the group, use either link with the same Google account:

- Android: [play.google.com/store/apps/details?id=style.xero.vibrateawake](https://play.google.com/store/apps/details?id=style.xero.vibrateawake)
- Web: [play.google.com/apps/testing/style.xero.vibrateawake](https://play.google.com/apps/testing/style.xero.vibrateawake)

The Play links only work for accounts that have joined the group first.

> [!IMPORTANT]
> We need 12 testers to keep the app installed for 14 days to pass beta testing and you only _need_ to run it once. Your testing support helps get this app to the Play Store as a free tool for everyone.

---

## Project layout

```
vibrateawake/
├── settings.gradle.kts               Root settings; includes :core, :app, :wear
├── build.gradle.kts                  Top-level plugin declarations
├── gradle/
│   ├── libs.versions.toml            Version catalog (single source of truth)
│   └── wrapper/                      Pinned Gradle 9.6.1 wrapper
├── gradlew                           Wrapper launcher (use this to build)
├── local.properties                  SDK path; machine-specific, not committed
├── keystore.properties               Release signing config; not committed
├── store/                            Play listing text, screenshots, feature graphic
│
├── core/                             Shared library: model, waveform, wire format
│   └── src/main/java/style/xero/vibrateawake/core/
│       ├── VibrationConfig.kt        Knobs, enums, waveform data, buildWaveform()
│       └── SessionSync.kt            Phone <-> watch Data Layer payload format
│
├── app/                              Phone app
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml       Permissions and the service declaration
│       ├── java/style/xero/vibrateawake/
│       │   ├── MainActivity.kt       Compose UI + permission/battery prompts
│       │   ├── MainViewModel.kt      Config StateFlow + start/stop
│       │   ├── SettingsRepository.kt DataStore persistence
│       │   ├── ServiceState.kt       Running-state flag the UI observes
│       │   ├── VibrationEngine.kt    Scheduling + the actual vibration
│       │   ├── VibrateAwakeService.kt Foreground service, wake lock, notification
│       │   ├── WatchSessionBridge.kt Signals a paired watch over the Data Layer
│       │   └── ui/theme/             Greyscale Material 3 theme, color, type
│       └── res/                      Fonts, icons, strings, window themes
│
└── wear/                             Wear OS companion (non-standalone)
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml       Watch permissions, standalone=false, services
        └── java/style/xero/vibrateawake/wear/
            ├── WatchActivity.kt          Minimal "controlled from your phone" screen
            ├── SessionListenerService.kt Cold-starts on the phone's /session signal
            ├── WatchVibrateService.kt    Foreground service + tick loop
            ├── TickScheduler.kt          Exact, doze-proof tick and pre-warn alarms
            ├── WatchVibrator.kt          Fires VibrationEffect (amplitude fallback)
            └── WatchSession.kt           Persisted session state on the watch
```

---

## Build and run

```sh
cd ~/.local/src/vibrateawake
./gradlew assembleDebug          # builds :core, :app, and :wear
```

The debug APKs land at `app/build/outputs/apk/debug/app-debug.apk` and
`wear/build/outputs/apk/debug/wear-debug.apk`.

Install and launch the phone app on a connected phone or emulator:

```sh
./gradlew :app:installDebug
adb shell am start -n style.xero.vibrateawake/.MainActivity
```

Install the watch app on a paired Wear OS device or emulator:

```sh
./gradlew :wear:installDebug
```

The physical test device is a Pixel 6 connected over Wi-Fi, because the
Jamf-managed Mac blocks USB media. Reconnect it with `adb mdns services` then
`adb devices`. The full ADB workflow is in the ADB cheatsheet in the Atlas
notes. A phone emulator `pixel7_api36` and a Wear OS emulator `wear_api34` are
also available:

```sh
emulator -avd pixel7_api36 &
adb wait-for-device
./gradlew :app:installDebug
```

Exercising the companion end to end needs the phone and a watch paired over the
Wear Data Layer, which is set up through Android Studio's Pair Wearable
assistant. The phone app runs fine on its own; the watch signal is best-effort
and silently no-ops when no watch is connected.

---

## Toolchain

Installed with Homebrew on Apple Silicon. The Android SDK lives at
`~/.local/share/android/sdk` under `$XDG_DATA_HOME`. The build pins JDK 21;
Homebrew pulled in JDK 26 as a `gradle` dependency, but AGP 9.3.0 is not
validated against 26. The relevant environment lives in
`~/.config/zsh/01-environment.zsh`.

| Component   | Version    |
| ----------- | ---------- |
| AGP         | 9.3.0      |
| Kotlin      | 2.2.10     |
| Gradle      | 9.6.1      |
| JDK         | 21         |
| Compose BOM | 2026.06.01 |
| compileSdk  | 37         |
| minSdk      | 26         |
| targetSdk   | 36         |

---

## Design notes

**Vibrations use `USAGE_ALARM`.** A vibration triggered from a background or foreground service with the default usage is attenuated to a barely-perceptible buzz and is dropped while the phone is locked. Tagging the effect with `AudioAttributes` set to `USAGE_ALARM` plays it at full strength from the background, with the screen off, and lets it through Do Not Disturb, which is exactly what a stay-awake alert needs.

**Special-use foreground service.** The service declares `foregroundServiceType="specialUse"` with the required subtype property, and on Android 14+ it calls the typed `startForeground` overload. It holds a `PARTIAL_WAKE_LOCK` for its lifetime so the timer keeps firing under Doze.

**AGP 9 built-in Kotlin.** AGP 9.0 folded Kotlin into the Android plugin, so there is no `org.jetbrains.kotlin.android` plugin. The phone and watch modules apply `com.android.application`, the shared library applies `com.android.library`, and both app modules add the Compose compiler plugin pinned to Kotlin 2.2.10 to match the version AGP 9.3.0 bundles. A mismatch there breaks the build.

**Shared `:core` module.** The config model and its waveform builder (`VibrationConfig.buildWaveform()`) live in a small library both the phone and watch depend on, so the two never drift as the rhythms are tuned. The watch layers its own `AlarmManager` loop and an amplitude-control fallback on top of that shared code; see [Wear OS companion](#wear-os-companion).

**Greyscale with an orange accent.** Material You dynamic color is turned off. The scheme maps background and text to black, white, and off-tones (`#222` and `#efefef`), and `surfaceVariant` and related roles are pinned to greys so nothing leaks the default purple tint. The one accent is orange (`#D16A00`, chosen to read on both black and white): the title, the slider active fill, selected radio buttons, and the Stop button while running. Locked controls dim, and a selected control's orange fades to `#BE5900`.

**Portrait lock.** `MainActivity` sets `android:screenOrientation="portrait"`. The single-column form is built for portrait and this is a set-and-forget utility, so the app stays upright instead of reflowing awkwardly in landscape.

**In-app privacy policy.** The shield icon opens a privacy screen that replaces the form while keeping the title, rendered as native Compose text. Its links open in the default browser via `ACTION_VIEW` with `EXTRA_REFERRER` cleared, so the destination gets no app referrer.

**Permissions.** `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. The notification permission is requested at runtime on Android 13+, and the battery-optimization exemption is offered the first time you press Start so the OS does not throttle the timer.

---

## Privacy

Vibrate Awake collects no data, has no network access, and contains no ads or
tracking. Your settings stay in the app's private storage on the device. See
[PRIVACY.md](./PRIVACY.md) for the full policy.

---

## License

**Vibrate Awake** is released under the [MIT License](./LICENSE.txt), by [xero](https://x-e.ro)
