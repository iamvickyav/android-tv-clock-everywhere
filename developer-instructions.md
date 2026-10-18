# Clock Everywhere — Developer Instructions

This document is the technical reference for developers and coding agents working on Clock Everywhere. Keep the public `README.md` user-focused; put implementation, build, testing, architecture, and release details here.

## Project purpose

Clock Everywhere is a lightweight Android TV application that displays a persistent clock overlay above other applications.

The primary tested device is a Xiaomi Android TV:

```text
Model:          MiTV-MOOQ
Android:        10
SDK:            29
Kernel:         4.19.79+
Build:          Qt63.200305.006.5283
Security patch: 2022-08-05
```

## Android compatibility

| Setting | Version |
|---|---|
| Minimum Android | Android 8.0 (API 26) |
| Recommended / tested device | Android 10 (API 29) |
| Compile SDK | API 35 |
| Current target SDK | API 34 |

`minSdk = 26` is the minimum supported version. Do not raise it without an explicit product decision.

## Architecture / behavior

Core components include:

- `MainActivity` — settings/configuration UI.
- `ClockOverlayService` — owns the always-on-top clock overlay.
- `ClockDreamService` — independent full-screen Android screensaver using the existing 12/24-hour and timezone preferences.
- `FlipClockView` — lays out a responsive centered row of six digit cards and two static colon separators.
- `FlipDigitView` — renders and independently animates one digit card.
- `ClockTimeFormatter` — formats actual wall-clock time into six digits using the configured timezone.
- `BootReceiver` — handles boot broadcasts and starts the overlay when the app is enabled and the overlay permission is available.
- Android manifest — declares the overlay permission, boot receiver, services, Android TV launcher entry, and DreamService metadata.

The app should not open its settings Activity automatically at TV boot. Boot should start the overlay service only when configured to do so.

The overlay should remain click-through/non-touchable so it does not interfere with the application underneath it.

The overlay requires Android's `SYSTEM_ALERT_WINDOW` / Display over other apps permission.

### Flip-clock screensaver

`ClockDreamService` is registered using the `android.service.dreams.DreamService` action, the `android.permission.BIND_DREAM_SERVICE` permission, and `@xml/clock_dream` metadata. Android TV controls activation; the app must not add an inactivity timer. To configure it on a TV, open **Settings → Device Preferences → Screen saver** (manufacturer menu names vary) and choose **Clock Everywhere Flip Clock**. Some manufacturers may hide third-party DreamService implementations or expose the selector elsewhere.

The dream uses a black full-screen background and a centered, width/height-constrained horizontal layout. It reads the existing `24_hour` and `timezone` values from `ClockPreferences`; seconds are always shown. Twelve-hour mode includes the clock's AM/PM marker. `ClockTimeFormatter` recalculates all six digits from `System.currentTimeMillis()` at each actual second boundary. `FlipClockView` updates each digit only when its value differs. `FlipDigitView` uses a 400 ms native `ValueAnimator` and a `Camera`/`Matrix` rotationX transform around the card's horizontal hinge. Stopping/detaching the dream cancels the handler callback, unregisters the preference listener, and cancels digit animations.

The bundled `app/src/main/assets/fonts/DSEG7Classic-Bold.ttf` is the DSEG7 Classic Bold digital display font. It is distributed under SIL Open Font License 1.1; retain the adjacent `DSEG-LICENSE.txt` when distributing the application.

Run `./gradlew testDebugUnitTest` for 12/24-hour formatting, second/minute/hour rollovers, midnight, and changed-digit assertions. Verify DreamService activation and exit on physical target TVs; Android's API-level support does not guarantee that each TV manufacturer's settings UI exposes third-party dreams.

## TV launcher assets

The Android TV launcher **icon** and **banner** are separate resources. Do not treat them as interchangeable.

Current working assets are density-specific resources under `app/src/main/res/mipmap-*`.

The manifest should reference:

```xml
android:icon="@mipmap/ic_launcher"
android:banner="@mipmap/banner"
```

Do not reintroduce duplicate `drawable/banner.png`, `drawable/banner.jpg`, or similarly named resources. Android resource merging treats files with the same resource name/type as duplicates.

### Image asset rule

When new artwork is needed, generate/provide the image file and its exact destination/name to the project owner. **Do not modify or replace image assets in the GitHub repository unless explicitly requested.** The project owner manually manages image files.

## Build variants and signing

Clock Everywhere deliberately has separate debug and release identities:

| Variant | Application ID | Version | Signing | Purpose |
|---|---|---|---|---|
| Debug | `com.vicky.clockeverywhere.debug` | `1.0.0-debug` | Persistent debug key | Development/testing |
| Release | `com.vicky.clockeverywhere` | `1.0.0` | Persistent release key | Stable releases |

The debug and release variants can coexist because they use different application IDs.

The debug signing key must remain identical between debug builds. Changing the debug key causes Android to report `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and requires a one-time uninstall/migration.

### Debug signing secrets

GitHub Actions uses:

```text
CLOCK_DEBUG_KEYSTORE_B64
CLOCK_DEBUG_STORE_PASSWORD
CLOCK_DEBUG_KEY_ALIAS
CLOCK_DEBUG_KEY_PASSWORD
```

Release signing uses:

```text
CLOCK_RELEASE_KEYSTORE_PATH
CLOCK_RELEASE_STORE_PASSWORD
CLOCK_RELEASE_KEY_ALIAS
CLOCK_RELEASE_KEY_PASSWORD
```

Never commit a keystore, private signing material, passwords, or tokens.

## Installation/testing

### ADB

Enable Developer Options and ADB/network debugging on the TV, then:

```bash
adb connect <TV_IP>:5555
adb devices
adb install -r clock-everywhere-1.0.0-debug.apk
```

Use `-r` for normal debug updates. Do not uninstall/reinstall every debug build; persistent signing is specifically intended to allow updates.

### Xiaomi package verification

Some Xiaomi Android TV firmware may return:

```text
INSTALL_FAILED_VERIFICATION_FAILURE
```

A temporary workaround used on the test TV is:

```bash
adb shell settings put global package_verifier_enable 0
adb install -r clock-everywhere-1.0.0-debug.apk
adb shell settings put global package_verifier_enable 1
```

Verify afterward:

```bash
adb shell settings get global package_verifier_enable
```

Only use this workaround for trusted APKs.

### ADB authorization troubleshooting

If the TV reports `unauthorized`, accept the RSA debugging prompt on the TV. If necessary:

```bash
adb kill-server
adb start-server
adb connect <TV_IP>:5555
adb devices
```

Do not treat an ADB authorization problem as an application installation/signing problem.

### USB installation

For normal manual installation, copy the APK to a USB drive, open it with the TV file manager, allow installation from that source if requested, and install it.

## Local build

From the repository root:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For updates to an existing debug installation, the local build must use the same persistent debug signing key.

The current checkout may require the Gradle wrapper script to be restored if it is absent. The wrapper distribution is pinned in `gradle/wrapper/gradle-wrapper.properties`; do not upgrade Gradle or the Android Gradle Plugin as part of an application feature.

## GitHub Actions

The project builds APKs through GitHub Actions. Keep Actions dependencies current and avoid deprecated Node.js action runtimes.

The repository currently uses modern versions of:

```text
actions/checkout

actions/setup-java
gradle/actions/setup-gradle
actions/upload-artifact
```

Use Node.js 24-compatible action generations. `actions/setup-java@v4` and other Node.js 20-based action versions should not be reintroduced.

When changing Gradle/AGP versions, verify compatibility rather than upgrading blindly. Build-tool upgrades should be isolated from application behavior changes when possible.

## Gradle / Android Gradle Plugin

The project currently uses Android Gradle Plugin configuration in the root `build.gradle` and the Gradle wrapper. Before changing Gradle or AGP:

1. Check the official AGP/Gradle compatibility requirements.
2. Keep the JDK version compatible with the selected AGP.
3. Run a complete debug build.
4. Install the resulting APK on the test Xiaomi TV.
5. Verify overlay, settings, boot behavior, and launcher assets.

Do not upgrade to a new major build-tool generation solely to silence a warning without checking compatibility.

## Release discipline

The goal for the first stable release is `1.0.0`.

Before declaring `1.0.0` final:

- Debug build succeeds.
- Debug APK installs over the previous debug APK without signature errors.
- Release APK uses the persistent release key.
- Overlay permission flow works.
- Enable/disable behavior works.
- Clock appears in the correct top-right position.
- Clock remains visible above other apps.
- Boot behavior works when enabled.
- Android TV Apps-list icon is correct.
- Android TV banner is correct.
- No duplicate Android resources exist.
- README remains user-focused.
- Technical details remain in this document.

Do not change application ID or signing keys after publishing a stable release unless a deliberate migration plan exists.

## Coding-agent rules

Agents working on this repository should:

1. Read this file before changing build configuration or Android TV behavior.
2. Preserve the debug/release separation.
3. Preserve the persistent debug signing configuration.
4. Avoid changing minimum SDK without explicit approval.
5. Avoid changing launcher artwork unless explicitly requested.
6. Avoid modifying image files in GitHub unless explicitly requested.
7. Do not commit secrets, keystores, passwords, or tokens.
8. Avoid unrelated refactors while preparing a release.
9. Run/build-test after build configuration changes.
10. Keep `README.md` concise and user-facing; put technical details here.

## Known issues / lessons

### Duplicate resource names

Do not keep both:

```text
drawable/banner.png
drawable/banner.jpg
```

They both map to the same Android resource name `banner` and will fail `mergeDebugResources`.

### Signature mismatch

If Android reports:

```text
INSTALL_FAILED_UPDATE_INCOMPATIBLE
```

the installed package and APK were signed with different keys. For the normal debug workflow, the persistent debug key must be used. Only perform a one-time uninstall when migrating from an APK signed with an old/ephemeral key.

### Boot behavior

Use `BOOT_COMPLETED`/`LOCKED_BOOT_COMPLETED` through `BootReceiver`. The receiver should start the overlay service when the app is enabled and required permission is available. Do not launch the settings Activity at boot.

## Future features

Planned/considered features include:

- Hourly highlight: subtle glow/color animation for 10 seconds at `HH:00:00`.
- Configurable clock size/position.
- 12-hour/24-hour format.
- Optional timezone display.
- Selectable timezone.
- Additional display options as needed.
