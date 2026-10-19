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
- On-demand daily and calendar-based weekly/monthly app usage reports using Android UsageStats.
- Optional foreground-app usage in the clock overlay, refreshed every eight seconds when enabled.
- Settings are persisted locally on the TV.
- Android TV launcher entry: **Clock Everywhere**.
- Separate debug and release app identities for safe development and testing.

## Roadmap

- Hourly highlight: subtle glow/color animation for 10 seconds at `HH:00:00`.
- Configurable clock size/position.
- Additional display options as needed.

## App Usage Report

Open **App Usage Report** in Clock Everywhere. The first time, choose **Open Usage Access Settings** and allow Usage Access for Clock Everywhere. The report reads Android-reported foreground usage when opened or refreshed; it does not run a tracking service. It includes today's app breakdown, current/previous calendar week and month totals, daily summaries, and per-app usage details.

To show the current foreground app's accumulated usage for today in the overlay, enable **Show App Usage** in clock settings and allow Usage Access. This is off by default. Usage Access and Android TV firmware determine which foreground events and history are available; if the foreground app cannot be identified, the app usage portion stays hidden.

Android TV settings labels vary by manufacturer. The Usage Access screen may be under **Settings → Apps → Special app access → Usage access**. If the direct button is not supported, open that page manually. Android or TV firmware may limit the available history or event details.
