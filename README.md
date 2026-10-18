# Clock Everywhere

A lightweight Android TV clock overlay that stays visible above other apps.

## Current behavior

- Top-right overlay
- `HH:mm:ss` by default
- Click-through / non-touchable overlay
- Display-over-other-apps permission
- Starts on TV boot when enabled
- Android TV launcher entry: **Clock Everywhere**
- Settings are persisted locally on the TV

## Android compatibility

| Setting | Version |
|---|---|
| Minimum Android | Android 8.0 (API 26) |
| Recommended / tested device | Android 10 (API 29) |
| Compile SDK | API 35 |
| Current target SDK | API 28 |

`minSdk = 26` is the minimum supported version for this project. The app therefore requires Android 8.0 or newer. The current target/compile settings are independent of the minimum version.

### Known test TV

```text
Model:          MiTV-MOOQ
Android:        10
SDK:            29
Kernel:         4.19.79+
Build:          Qt63.200305.006.5283
Security patch: 2022-08-05
```

## Installation

There are three ways to install Clock Everywhere on an Android TV.

### 1. Download APK from GitHub Actions

The project automatically builds a debug APK using GitHub Actions whenever code is pushed to `main`.

1. Open the repository's **Actions** tab.
2. Select the **Build APK** workflow.
3. Open the latest successful workflow run.
4. Scroll to **Artifacts**.
5. Download the APK artifact.
6. Extract the downloaded ZIP and use the APK inside it.

Repository:

https://github.com/iamvickyav/android-tv-clock-everywhere

Builds:

https://github.com/iamvickyav/android-tv-clock-everywhere/actions/workflows/build.yml

> For a normal TV installation, the APK artifact from the latest successful build is the recommended download.

### 2. Install using ADB — recommended for development/testing

#### Enable Developer Options

On the TV:

**Settings → Device Preferences → About → Build**

Press **Build 7 times** to enable Developer Options.

Enable ADB / network debugging if available.

#### Find the TV IP address

On the TV, open:

**Settings → Network & Internet → your connected network**

Example:

```text
192.168.1.42
```

The TV and computer must be on the same network.

#### Install Android platform tools on Mac

```bash
brew install android-platform-tools
adb version
```

#### Connect to the TV

```bash
adb connect <TV_IP>:5555
adb devices
```

Accept the debugging prompt on the TV if shown.

#### Install the APK

```bash
adb install -r clock-everywhere.apk
```

The `-r` option updates an existing installation without uninstalling it.

### Xiaomi / Android package verification

Some Xiaomi Android TV builds may reject a sideload with:

```text
INSTALL_FAILED_VERIFICATION_FAILURE
```

On the test MiTV-MOOQ device, disabling Android package verification temporarily allowed installation:

```bash
adb shell settings put global package_verifier_enable 0
adb install -r clock-everywhere.apk
adb shell settings put global package_verifier_enable 1
```

Verify the final value is `1`:

```bash
adb shell settings get global package_verifier_enable
```

Only use this workaround for an APK you trust.

### 3. Install using a USB drive — easiest for normal TV use

This method does not require a computer-to-TV ADB connection.

1. Download the latest APK from the **Build APK** GitHub Actions artifact on a computer.
2. Extract the downloaded artifact ZIP.
3. Copy the APK to a USB flash drive.
4. Insert the USB drive into the Xiaomi TV.
5. Open the TV's file manager / USB storage browser.
6. Select the Clock Everywhere APK.
7. If the TV asks for permission to install apps from that source, allow it.
8. Confirm installation.
9. Open **Clock Everywhere** from the TV app list.

If the TV blocks installation from the file manager, enable the relevant **Install unknown apps** permission for that file manager and retry.

> USB installation is the simplest option when you just want to install a released/test APK. ADB is more convenient when developing and repeatedly updating the app.

## First-time TV setup

After installing the APK:

1. Open **Clock Everywhere** from the TV app list.
2. Grant:
   **Settings → Apps → Special app access → Display over other apps → Clock Everywhere → Allow**
3. Return to Clock Everywhere.
4. Enable the app.
5. The clock should appear in the top-right corner.

## Build locally

From the repository root:

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The repository also contains a GitHub Actions workflow that builds and uploads the debug APK automatically.

## Roadmap

- Hourly highlight: subtle glow/color animation for 10 seconds at `HH:00:00`
- Configurable clock size/position
- Additional display options as needed
