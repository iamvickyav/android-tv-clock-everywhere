# Clock Everywhere

A lightweight Android TV clock overlay that stays visible above every other app.

<img width="800" height="576" alt="WhatsApp Image 2026-10-20 at 21 51 44" src="https://github.com/user-attachments/assets/b6ad1094-e188-4985-a8db-5b934fb455ae" />
<img width="800" height="576" alt="WhatsApp Image 2026-10-20 at 21 51 45" src="https://github.com/user-attachments/assets/af38086d-96da-4071-9c5a-c0e317a10c2b" />

## Download

### Latest debug build

For development/testing, download the latest APK from GitHub Actions:

1. Open the repository's **Actions** tab.
2. Select the **Build APK** workflow.
3. Open the latest successful workflow run.
4. Download the latest debug APK artifact.
5. Extract the ZIP and install the APK on your Android TV.

GitHub Repository - https://github.com/iamvickyav/android-tv-clock-everywhere

Download Builds from GitHub Actions - https://github.com/iamvickyav/android-tv-clock-everywhere/actions/workflows/build.yml

### Release build

Stable release APKs are published through GitHub Releases.

## Features

- Clock displayed in the top-right corner.
- 24-hour `HH:mm:ss` display, with optional timezone display.
- Runs above other apps using Android's display-over-other-apps permission.
- Click-through / non-touchable overlay.
- Automatically starts after TV boot when enabled.
- Starts from device-protected settings before the TV is unlocked (after the settings have been migrated on first launch/update).
- On-demand daily and calendar-based weekly/monthly app usage reports using Android UsageStats.
- Current week, previous week, current month and previous month usage reports.
- Daily usage summaries and per-app usage details.
- Optional foreground-app usage in the clock overlay, refreshed automatically when enabled.
- Configurable overall TV usage limit and common app usage limit.
- Usage status indicators: `✔` within limit, `⏳` approaching limit, and `⚠️` limit exceeded.
- Settings are persisted locally on the TV.
- Android TV launcher entry: **Clock Everywhere**.
- Separate debug and release app identities for safe development and testing.

## Usage Limits

Clock Everywhere can show usage-limit status for both overall TV usage and individual app usage.

### Default Limits

- **Overall TV usage:** 3 hours
- **App usage:** 2 hours

The app-level limit is a common limit applied to all tracked apps.

### Status Indicators

- `✔` — Usage is below 80% of the configured limit.
- `⏳` — Usage has reached 80% of the configured limit but has not exceeded it.
- `⚠️` — Usage has reached or exceeded the configured limit.

App and overall TV usage are evaluated independently. For example, an app can remain within its 2-hour limit while overall TV usage has already exceeded its 3-hour limit.

The limits can be changed from the Clock Everywhere settings.

## App Usage Report

Open **App Usage Report** in Clock Everywhere. The first time, choose **Open Usage Access Settings** and allow Usage Access for Clock Everywhere. The report reads Android-reported foreground usage when opened or refreshed; it does not run a tracking service.

The report includes:

- Today's app usage breakdown.
- Current calendar week.
- Previous calendar week.
- Current calendar month.
- Previous calendar month.
- Daily usage summaries.
- Per-app usage details.
- Usage-limit status for overall TV usage and individual apps.

To show the current foreground app's accumulated usage for today in the overlay, enable **Show App Usage** in clock settings and allow Usage Access. This is off by default. Usage Access and Android TV firmware determine which foreground events and history are available; if the foreground app cannot be identified, the app usage portion stays hidden.

When enabled, the overlay keeps the existing clock as the single clock display and shows the current app's usage/status alongside it.

Android TV settings labels vary by manufacturer. The Usage Access screen may be under **Settings → Apps → Special app access → Usage access**. If the direct button is not supported, open that page manually. Android or TV firmware may limit the available history or event details.

## Privacy

Usage reporting relies on Android's local UsageStats data. Clock Everywhere does not upload app usage information to a cloud service.

## Roadmap

- Hourly highlight: subtle glow/color animation for 10 seconds at `HH:00:00`.
- Configurable clock size/position.
- Additional display options as needed.
