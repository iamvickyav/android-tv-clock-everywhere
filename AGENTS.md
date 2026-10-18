# Clock Everywhere — Agent Guide

## Mission

Maintain a small, reliable Android TV clock overlay. Prefer simple Android platform APIs and minimal dependencies.

## Before editing

Read:

- `README.md`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- the relevant Java source under `app/src/main/java/com/vicky/clockeverywhere/`
- applicable `.github/instructions/*.instructions.md`

Do not assume that a generic Android phone behavior is identical on Xiaomi Android TV.

## Critical invariants

1. Minimum supported Android remains API 26 unless explicitly changed.
2. The tested reference device is Xiaomi MiTV-MOOQ, Android 10 / API 29.
3. The clock overlay is top-right, click-through, and non-focusable.
4. Overlay permission failures must not crash the app.
5. The Enable setting controls both runtime overlay behavior and boot-time startup.
6. Boot must start the overlay service only when enabled; it must not open the Activity.
7. Debug and release use different application IDs and different persistent signing keys.
8. Never commit signing keys, passwords, tokens, or APKs.
9. Android TV `android:icon` and `android:banner` are separate resources.
10. Avoid duplicate Android resource names such as `drawable/banner.*` and `mipmap-*/banner.*` when both are referenced ambiguously.

## Build

Run:

```bash
./gradlew assembleDebug
```

The GitHub Actions debug workflow uses a persistent keystore supplied through repository secrets. If the signing secrets are unavailable, report the missing secret instead of generating an ephemeral CI key.

## Testing on the reference TV

The reference device is Android 10 / API 29. A typical development update is:

```bash
adb connect <TV_IP>:5555
adb devices
adb install -r clock-everywhere-1.0.0-debug.apk
```

The debug package is `com.vicky.clockeverywhere.debug`.

The old release package is `com.vicky.clockeverywhere`.

If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, investigate signing identity before recommending an uninstall. A persistent key should allow repeated debug updates.

## Overlay testing

Test all of these states:

- permission not granted
- permission granted, app disabled
- permission granted, app enabled
- foreground app such as YouTube
- TV home screen
- reboot with app enabled
- reboot with app disabled

The overlay must not intercept remote-control input.

## Resource/artwork rules

Keep Android TV launcher artwork in the appropriate `mipmap-*` resources when using density-specific launcher/banner assets. Keep the manifest references aligned with those resources. Do not reintroduce the old black launcher background or unrelated placeholder artwork.

## GitHub Actions rules

Use Node.js 24-compatible action versions:

```yaml
uses: actions/checkout@v6
uses: actions/setup-java@v6
uses: gradle/actions/setup-gradle@v6
uses: actions/upload-artifact@v7
```

Keep Java 17 unless the Android build requires a deliberate upgrade.

## Release discipline

Do not create or publish a release merely because a build succeeds. Release builds require the persistent release signing identity and explicit release intent. Debug artifacts are for TV testing.

## Change discipline

Make focused commits. If a requested change exposes a separate bug, fix it only when necessary for the requested behavior and document the reason. Update `README.md` when installation, build, compatibility, signing, or agent workflow changes.
