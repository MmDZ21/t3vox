# Contributing to TS3 Mobile (Unofficial)

Thank you for your interest in contributing. This repository is an **alpha-quality community project**. It is not affiliated with or endorsed by TeamSpeak Systems GmbH.

## Before You Start

Open an issue before substantial work on **protocol behavior**, **identity handling**, **audio pipelines**, **dependency changes**, or **architecture refactors**. That keeps coordination visible and avoids duplicate or conflicting efforts.

Small fixes (typos, isolated test gaps, documentation corrections) may proceed without a prior issue when the scope is obvious.

## Development Environment

- **JDK 17**
- **Android SDK Platform 36**
- Open the repository root in Android Studio and allow it to create `local.properties` for your local SDK path.
- Command-line users may set `ANDROID_HOME` and `ANDROID_SDK_ROOT` to their installed SDK.

## Validation

From the repository root in PowerShell, run the commands relevant to your change:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

Use `.\scripts\run-emulator-debug.ps1` for emulator setup and `.\scripts\debug-connect.ps1 -ServerHost "voice.example.com"` only for intentional live-server checks.

## Testing Guidelines

- Unit tests use **JUnit 4**.
- Name test files `*Test.kt` and test methods by behavior (for example, `ordersSiblingsByPredecessorChain`).
- Add **regression tests** when fixing bugs or changing behavior.
- Normal unit tests must **not** perform live network, DNS, or server connections. Live checks are environment-driven and excluded from the default test task.
- Voice or microphone changes should be validated on a **physical Android device** when practical.

## Code Style

- Follow the official Kotlin style configured in `gradle.properties`: four-space indentation, trailing commas in multiline declarations, and focused files.
- Use `PascalCase` for classes and Compose functions, `camelCase` for functions and properties, and lowercase package names.
- Match files to their primary type (for example, `ClientViewModel.kt`).
- Preserve existing package boundaries under `app/src/main/java/com/toosarax/ts3client/` and the exact package of the patched ts3j Java class.

## Security and Sensitive Data

Do **not** commit or include in issues, PRs, or logs:

- Server or channel passwords
- Imported identity exports
- Local machine paths (`local.properties`, `.android-sdk/`)
- Diagnostic logs containing connection details
- Signing keys or keystore material
- Private server hostnames or addresses

Saved-server records exclude passwords by design. Treat passwords and imported identities as sensitive in all contributor artifacts.

## Pull Requests

Keep each pull request **focused** on one logical change.

Every PR should:

- Link a related issue when one exists
- List validation performed (tests, lint, build, device checks)
- Include **screenshots** for Compose UI changes
- Note **device and server conditions** used for voice or protocol testing
- Update `THIRD_PARTY_NOTICES.md` and related license files when dependencies change

## Commit Messages

Use short, imperative, scoped messages when possible:

```text
fix(audio): reset decoder after channel change
test(protocol): make SRV resolution deterministic
docs: document patched ts3j provenance
```

## Further Reading

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — module layout and data flow
- [docs/ROADMAP.md](docs/ROADMAP.md) — planned milestones and non-goals
- [SECURITY.md](SECURITY.md) — vulnerability reporting
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — community expectations
- [AGENTS.md](AGENTS.md) — repository conventions for automated contributors
