---
applyTo: ".github/workflows/**/*.yml,.github/workflows/**/*.yaml"
---

# GitHub Actions instructions

Use Node.js 24-compatible action versions. Current standard:

- `actions/checkout@v6`
- `actions/setup-java@v6`
- `gradle/actions/setup-gradle@v6`
- `actions/upload-artifact@v7`

Do not reintroduce Node.js 20-based action majors.

Keep Java 17 unless a deliberate project-wide Java upgrade is requested.

The debug workflow must use the persistent debug signing secrets. Never generate a new CI signing key on each run because that breaks `adb install -r` updates on the TV.

Never expose signing secrets in logs. Never commit keystores or credentials.

Prefer the Gradle Wrapper if it is present and valid. If the workflow intentionally pins a Gradle version, preserve it unless the task explicitly includes a Gradle upgrade.

Debug and release workflows must remain separate. Do not publish a release from the debug workflow.
