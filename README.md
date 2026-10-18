# Clock Everywhere

A lightweight Android TV clock overlay that stays visible above every other app.

<img width="800" height="500" alt="2" src="https://github.com/user-attachments/assets/6d630e98-791c-4734-9157-8e0794faebec" />

<img width="800" height="500" alt="1" src="https://github.com/user-attachments/assets/2aaa44b3-5d9e-4d36-907f-a62425be4d4a" />


## Download

### Latest debug build

For development/testing, download the latest APK from GitHub Actions:

1. Open the repository's **Actions** tab.
2. Select the **Build APK** workflow.
3. Open the latest successful workflow run.
4. Download the `clock-everywhere-1.0.0-debug` artifact.
5. Extract the ZIP and install the APK on your Android TV.

GitHub Repository - https://github.com/iamvickyav/android-tv-clock-everywhere

Download Builds from GitHub Actions - https://github.com/iamvickyav/android-tv-clock-everywhere/actions/workflows/build.yml

### Release build

Stable release APKs will be published through GitHub Releases once the first release is locked.

## Features

- Clock displayed in the top-right corner.
- 24-hour `HH:mm:ss` display.
- Runs above other apps using Android's display-over-other-apps permission.
- Click-through / non-touchable overlay.
- Automatically starts after TV boot when enabled.
- Starts from device-protected settings before the TV is unlocked (after the settings have been migrated on first launch/update).
- Includes a separate Android DreamService flip-clock screensaver.
- Settings are persisted locally on the TV.
- Android TV launcher entry: **Clock Everywhere**.
- Separate debug and release app identities for safe development and testing.

## Roadmap

- Hourly highlight: subtle glow/color animation for 10 seconds at `HH:00:00`.
- Configurable clock size/position.
- Additional display options as needed.

## Flip-clock screensaver

To use the full-screen flip clock, open the TV's **Settings → Device Preferences → Screen saver** (the exact menu name varies by manufacturer), choose **Clock Everywhere Flip Clock**, and configure the TV's own screensaver timeout. The screensaver uses the app's existing 12/24-hour and timezone choices. Some TV firmware may hide third-party screensavers or use a different settings path.
