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

## Install on Android TV using ADB

### 1. Enable Developer Options

On the TV:

**Settings → Device Preferences → About → Build**

Press **Build 7 times** to enable Developer Options.

Enable ADB / network debugging if available.

### 2. Find the TV IP address

On the TV, open:

**Settings → Network & Internet → your connected network**

Example:

```text
192.168.1.42
```

The TV and computer must be on the same network.

### 3. Install Android platform tools on Mac

```bash
brew install android-platform-tools
adb version
```

### 4. Connect to the TV

```bash
adb connect <TV_IP>:5555
adb devices
```

Accept the debugging prompt on the TV if shown.

### 5. Install the APK

Download the APK produced by the GitHub Actions **Build APK** workflow, then:

```bash
adb install -r clock-everywhere.apk
```

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

### 6. Grant overlay permission

On the TV:

**Settings → Apps → Special app access → Display over other apps → Clock Everywhere → Allow**

### 7. Launch

Open **Clock Everywhere** from the TV app list. The overlay should appear in the top-right corner.

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
