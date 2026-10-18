# Clock Everywhere — Copilot Instructions

## Project purpose

Clock Everywhere is a lightweight Android TV application that displays a non-touchable clock overlay in the top-right corner of the screen, including while other apps are running. It is designed and tested primarily for Xiaomi MiTV-MOOQ running Android 10 / API 29.

## Repository structure

- `app/src/main/java/com/vicky/clockeverywhere/` — Android application code, including the settings Activity, overlay Service, and boot Receiver.
- `app/src/main/res/` — Android resources, including TV launcher/banner artwork and themes.
- `app/build.gradle` — Android SDK, application IDs, versions, build types, and signing configuration.
- `.github/workflows/` — GitHub Actions CI/build workflows.
- `.github/instructions/` — path-specific guidance for Android, Gradle, and GitHub Actions changes.
- `AGENTS.md` — general agent workflow and project guardrails.

## Product behavior

- Clock is displayed at the top-right of the screen.
- Overlay must be click-through and must not steal focus from the foreground TV app.
- Overlay requires Android's `SYSTEM_ALERT_WINDOW` / Display over other apps permission.
- The app has an Enable/Disable setting. Disabled means no overlay, including after boot.
- When enabled, the clock starts after Android TV boot without opening the settings Activity.
- Current default clock format is `HH:mm:ss`.
- Existing settings and behavior must not be removed while implementing new features.

## Android compatibility

- Minimum Android: 8.0 / API 26.
- Tested device: Xiaomi MiTV-MOOQ, Android 10 / API 29.
- Compile SDK: 35.
- Target SDK: 28 unless there is an explicit decision to change it.
- Do not raise `minSdk` without an explicit product decision.

## Build variants and signing

There are two intentionally separate identities:

- Debug: application ID `com.vicky.clockeverywhere.debug`, version `1.0.0-debug`, persistent debug signing key.
- Release: application ID `com.vicky.clockeverywhere`, version `1.0.0`, persistent release signing key.

Never change the debug application ID or replace the persistent debug key casually. A changed signing key causes `INSTALL_FAILED_UPDATE_INCOMPATIBLE` on the TV.

Never commit keystores, passwords, GitHub secrets, tokens, or private keys.

The debug GitHub Actions workflow expects these repository secrets:

- `CLOCK_DEBUG_KEYSTORE_B64`
- `CLOCK_DEBUG_STORE_PASSWORD`
- `CLOCK_DEBUG_KEY_ALIAS`
- `CLOCK_DEBUG_KEY_PASSWORD`

Release signing is intentionally separate and should only be added to a release workflow when the release key has been created and stored securely.

## Build and validation

Preferred local build:

```bash
./gradlew assembleDebug
```

For CI, preserve the persistent debug signing setup. Do not silently fall back to a generated ephemeral key when building the debug APK.

Before proposing a change:

1. Inspect the existing implementation and README.
2. Make the smallest safe change.
3. Build the affected variant.
4. Check for Android resource-name collisions, especially duplicate `banner` or launcher resources across `drawable` and `mipmap` directories.
5. For overlay changes, verify that the service handles missing/revoked overlay permission without crashing.
6. For boot behavior, verify the receiver respects the Enable/Disable setting and does not launch the UI.

## TV UI and artwork

Android TV launcher assets are separate concepts:

- `android:icon` is the Apps-list launcher icon.
- `android:banner` is the Leanback TV banner.

Do not substitute one for the other. Preserve the approved Clock Everywhere artwork and the simple, single-color clock branding unless a new design is explicitly requested.

## Overlay safety

The overlay must use a non-focusable, non-touchable window. It must never block interaction with YouTube or another foreground TV application. Handle `WindowManager` failures gracefully because TV firmware can report overlay permission inconsistently.

## GitHub Actions

Use current Node.js 24-compatible major versions of official actions. Current project standard:

- `actions/checkout@v6`
- `actions/setup-java@v6`
- `gradle/actions/setup-gradle@v6`
- `actions/upload-artifact@v7`

Do not downgrade these actions to Node.js 20-based versions just to silence a warning.

Prefer the Gradle Wrapper when the repository has a valid wrapper. If the project intentionally uses a pinned installed Gradle version, preserve that choice unless upgrading it is part of the task.

## Feature roadmap

The next planned feature is an hourly highlight: at exactly `HH:00:00`, the clock should subtly glow/change color for approximately 10 seconds, then return to its normal appearance. Do not implement this unless explicitly requested.

## Agent behavior

- Do not invent device capabilities or Android TV APIs.
- Do not delete existing functionality to make a build pass.
- Do not change package IDs or signing behavior without explaining the migration impact.
- Do not commit generated APKs, keystores, credentials, or machine-specific files.
- Keep changes focused and document important build/install implications in `README.md` when appropriate.
