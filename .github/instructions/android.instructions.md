---
applyTo: "app/**/*.java,app/**/*.xml,app/**/*.gradle,app/src/main/**"
---

# Android / Android TV instructions

This is an Android TV app, not a phone-first app. Preserve TV remote navigation and focus behavior.

The overlay is intentionally click-through and non-focusable. Do not add touch handling, focusability, or a full-screen window to the overlay.

Use `SYSTEM_ALERT_WINDOW` and check `Settings.canDrawOverlays()` before starting the overlay. If the permission is missing or the TV firmware rejects a window operation, fail gracefully rather than crashing.

The reference device is Xiaomi MiTV-MOOQ running Android 10 / API 29. The minimum supported API is 26.

The boot receiver must start the overlay service only when the persisted Enable setting is true. It must not launch the settings Activity at boot.

Keep Android TV launcher `icon` and Leanback `banner` resources distinct. When density-specific resources are present, avoid creating duplicate resource names in another resource directory that can make `mergeDebugResources` fail.

Do not change application IDs or signing configuration as part of an ordinary feature change.
