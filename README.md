# Vibrate Awake

> [!NOTE]
> An Android app that vibrates your phone on a randomized interval to keep a
> drowsy driver awake. Set an interval, a rhythm, and an intensity, press Start,
> and a foreground service keeps buzzing even while the phone is locked. No
> interaction is needed once it is running. This file records what it does, how
> it is built, and how to run it.

---

> ### Table of Contents
> - [What it does](#what-it-does)
> - [Settings](#settings)
> - [How it works](#how-it-works)
> - [Project layout](#project-layout)
> - [Build and run](#build-and-run)
> - [Toolchain](#toolchain)
> - [Design notes](#design-notes)
> - [Privacy](#privacy)

---

## What it does

The whole app is one screen: a settings form with three knobs and a large
Start/Stop button across the bottom. Press Start and the app vibrates your
phone on an interval to fight microsleep. Press Stop, in the app or from the
notification, and it ends.

The design is deliberately distraction-free. There is nothing to dismiss, tap,
or acknowledge while driving. It just vibrates.

- **Fires immediately.** Start plays your chosen pattern at once, so you feel exactly what you picked before setting off.
- **Runs locked.** A foreground service keeps the vibration going with the screen off and the phone locked.
- **Fights habituation.** The interval is jittered by up to 15 seconds each cycle so your brain cannot anticipate the buzz, and a faint pre-warn pulse fires about 15 seconds before each main alert so a hard buzz never startles you mid-steer.
- **Greyscale.** Black and white, following the system light or dark setting. The title and Start/Stop button use the embedded Thunderline font; everything else is Roboto.

---

## Settings

**Fatigue Level.** How often it buzzes. Presets of 3, 5, and 10 minutes, plus a slider from 1 to 10 minutes in 30-second steps. The short end is "Extreme Fatigue", the long end "Preventative". Default is 3 minutes.

**Vibration Rhythm.** The waveform of each alert.
- **Staccato** (default): an escalating heartbeat, soft then medium then a hard final burst.
- **SOS Pulse**: three quick maximum-intensity jabs.
- **Continuous Wave**: a rising and falling siren.

**Road Noise Level.** How hard the pulses hit.
- **Standard (Adaptive)** (default): uses the pattern's ascending amplitudes, gentle to strong.
- **Heavy Cabin Noise (Max Power)**: forces every active pulse to full 255 amplitude to punch through a loud, bumpy cabin.

Settings persist across launches. While the service is running the controls lock, so the only action left is Stop.

---

## How it works

**`VibrationEngine`** owns the schedule. It runs on the main looper, computes the next delay as `interval ± up to 15s` with a 30-second floor, schedules a faint pre-warn one-shot 15 seconds ahead of each main alert, then plays the selected waveform with `VibrationEffect.createWaveform`. Max Power rewrites every active amplitude to 255.

**`VibrateAwakeService`** is a foreground service that houses the engine and a partial wake lock, so alerts keep firing under Doze and while locked. It reads the config from the launch intent's extras, posts an ongoing low-importance notification with a Stop action, and returns `START_REDELIVER_INTENT` so the config survives a process restart.

**`MainViewModel` and `SettingsRepository`** hold the config as a `StateFlow` backed by DataStore, and expose start and stop. **`ServiceState`** is a small singleton `StateFlow<Boolean>` the service flips so the UI knows whether to show Start or Stop.

**`MainActivity`** hosts the Compose UI and handles the runtime prompts: it requests `POST_NOTIFICATIONS` on Android 13+ and offers the battery-optimization exemption the first time you press Start.

---

## Project layout

```
vibrateawake/
├── settings.gradle.kts               Root settings; repositories and module list
├── build.gradle.kts                  Top-level plugin declarations
├── gradle/
│   ├── libs.versions.toml            Version catalog (single source of truth)
│   └── wrapper/                      Pinned Gradle 9.6.1 wrapper
├── gradlew                           Wrapper launcher (use this to build)
├── local.properties                  SDK path; machine-specific, not committed
└── app/
    ├── build.gradle.kts              The app module build script
    └── src/main/
        ├── AndroidManifest.xml       Permissions and the service declaration
        ├── java/style/xero/vibrateawake/
        │   ├── MainActivity.kt       Compose UI + permission/battery prompts
        │   ├── MainViewModel.kt      Config StateFlow + start/stop
        │   ├── SettingsRepository.kt DataStore persistence
        │   ├── ServiceState.kt       Running-state flag the UI observes
        │   ├── VibrationConfig.kt    Knobs, enums, and waveform data
        │   ├── VibrationEngine.kt    Scheduling + the actual vibration
        │   ├── VibrateAwakeService.kt Foreground service, wake lock, notification
        │   └── ui/theme/             Greyscale Material 3 theme, color, type
        └── res/
            ├── font/thunderline.ttf  Embedded display font (title + button)
            ├── drawable-nodpi/        Launcher illustration
            ├── drawable/              Icon layers + notification small icon
            ├── mipmap-anydpi-v26/     Adaptive launcher icon
            └── values, values-night/  Strings, window theme, colors
```

---

## Build and run

```sh
cd ~/.local/src/vibrateawake
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

Install and launch on a connected device or emulator:

```sh
./gradlew installDebug
adb shell am start -n style.xero.vibrateawake/.MainActivity
```

The physical test device is a Pixel 6 connected over Wi-Fi, because the
Jamf-managed Mac blocks USB media. Reconnect it with `adb mdns services` then
`adb devices`. The full ADB workflow is in the ADB cheatsheet in the Atlas
notes. An emulator named `pixel7_api36` is also available:

```sh
emulator -avd pixel7_api36 &
adb wait-for-device
./gradlew installDebug
```

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

**AGP 9 built-in Kotlin.** AGP 9.0 folded Kotlin into the Android plugin, so there is no `org.jetbrains.kotlin.android` plugin. Only `com.android.application` and the Compose compiler plugin are applied, and the Compose plugin is pinned to Kotlin 2.2.10 to match the version AGP 9.3.0 bundles. A mismatch there breaks the build.

**Greyscale theme.** Material You dynamic color is turned off. The color scheme maps background and text to black, white, and off-tones (`#222` and `#efefef`), with a grey accent for the button and controls. `surfaceVariant` and related roles are pinned to greys so the slider and radio buttons never leak the default purple tint.

**Permissions.** `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. The notification permission is requested at runtime on Android 13+, and the battery-optimization exemption is offered the first time you press Start so the OS does not throttle the timer.

---

## Privacy

Vibrate Awake collects no data, has no network access, and contains no ads or
tracking. Your settings stay in the app's private storage on the device. See
[PRIVACY.md](./PRIVACY.md) for the full policy.
