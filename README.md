# Clock Everywhere

A lightweight Android TV clock overlay that stays visible above other apps.

## Download

### Latest debug build

For development/testing, download the latest APK from GitHub Actions:

1. Open the repository's **Actions** tab.
2. Select the **Build APK** workflow.
3. Open the latest successful workflow run.
4. Download the `clock-everywhere-1.0.0-debug` artifact.
5. Extract the ZIP and install the APK on your Android TV.

urlGitHub Repositoryhttps://github.com/iamvickyav/android-tv-clock-everywhere

urlDownload Builds from GitHub Actionshttps://github.com/iamvickyav/android-tv-clock-everywhere/actions/workflows/build.yml

### Release build

Stable release APKs will be published through GitHub Releases once the first release is locked.

## Features

- Clock displayed in the top-right corner.
- 24-hour `HH:mm:ss` display.
- Runs above other apps using Android's display-over-other-apps permission.
- Click-through / non-touchable overlay.
- Automatically starts after TV boot when enabled.
- Starts from device-protected settings before the TV is unlocked (after the settings have been migrated on first launch/update).
- Settings are persisted locally on the TV.
- Android TV launcher entry: **Clock Everywhere**.
- Separate debug and release app identities for safe development and testing.

## Roadmap

- Hourly highlight: subtle glow/color animation for 10 seconds at `HH:00:00`.
- Configurable clock size/position.
- Additional display options as needed.
