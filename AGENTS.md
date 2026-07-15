# Repository Guidelines

T3Vox is an independent open-source Android client compatible with TeamSpeak 3 servers. Keep compatibility references descriptive and never imply affiliation with or endorsement by TeamSpeak.

## Project Structure & Module Organization

This is a single-module Android application. Production Kotlin and the patched ts3j Java class live under `app/src/main/java/com/toosarax/ts3client/`; keep UI, client coordination, protocol, audio, settings, and foreground-service code in their existing packages. Android resources and the manifest are in `app/src/main/res/` and `app/src/main/AndroidManifest.xml`. Local unit tests belong in `app/src/test/`; device and emulator tests belong in `app/src/androidTest/`. Supporting instructions and PowerShell helpers live in `docs/` and `scripts/`. Do not edit generated `app/build/` files or commit `.android-sdk/` and `local.properties`.

## Build, Test, and Development Commands

Use JDK 17 and Android SDK Platform 36. From PowerShell at the repository root:

```powershell
.\gradlew.bat :app:testDebugUnitTest   # Run local JUnit tests
.\gradlew.bat :app:lintDebug           # Run Android lint
.\gradlew.bat :app:assembleDebug       # Build the debug APK
.\gradlew.bat :app:connectedDebugAndroidTest # Run device tests
.\gradlew.bat :app:installDebug        # Install on a connected device
```

Use `.\scripts\run-emulator-debug.ps1` for emulator setup/run and `.\scripts\debug-connect.ps1` for an intentional live TS3 connection check.

## Coding Style & Naming Conventions

Follow the official Kotlin style configured in `gradle.properties`: four-space indentation, trailing commas in multiline declarations, and focused files. Use `PascalCase` for classes and Compose functions, `camelCase` for functions and properties, and lowercase package names. Match files to their primary type, such as `ClientViewModel.kt`. Preserve package boundaries and the exact package of the patched ts3j Java class.

## Testing Guidelines

Tests use JUnit 4; instrumentation tests use AndroidX `AndroidJUnit4`. Name files `*Test.kt` and test methods by behavior, for example `ordersSiblingsByPredecessorChain`. Add regression tests with behavior changes. Keep live-server checks separate: `LiveConnectionDebugTest` is intentionally excluded from normal unit tests. Voice changes should also be verified on a physical device when practical.

## Commit & Pull Request Guidelines

The repository has no commit history yet. Use short, imperative commits; scoped messages such as `fix(audio): reset decoder after channel change` are encouraged. Keep each change focused. Pull requests should explain intent, list validation performed, link relevant issues, include screenshots for Compose UI changes, and note device/server conditions used for voice or protocol testing.

## Security & Configuration

Never commit server passwords, imported identities, SDK paths, or signing material. Keep secrets out of debug output and preserve app-private identity storage behavior.
