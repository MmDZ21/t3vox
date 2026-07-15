# T3Vox

An unofficial open-source Android client compatible with TeamSpeak 3 servers, built with Kotlin, Jetpack Compose, [ts3j](https://github.com/Manevolent/ts3j), and [Concentus](https://github.com/lostromb/concentus).

This repository is an alpha-quality community project. Do not rely on it for emergency or production-critical communication.

## Features

- Connect to a single TeamSpeak 3 server by host/IP and UDP port
- Browse channels and users with TS3 predecessor ordering
- Join open or password-protected channels
- Receive OPUS_VOICE audio from one or more speakers
- Transmit with push-to-talk or RMS voice activation
- Foreground voice session with disconnect notification action

## Requirements

- JDK 17+
- Android SDK Platform 36
- Physical Android device API 26+ recommended for voice testing

## SDK Setup

Open the repository root in Android Studio and allow it to create `local.properties` for your local SDK path.

Command-line users may set `ANDROID_HOME` and `ANDROID_SDK_ROOT` to their installed Android SDK.

## Build

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

Install on a connected device:

```powershell
.\gradlew.bat :app:installDebug
```

## Emulator (debug on PC)

Android Studio is required for the emulator. See [docs/ANDROID_EMULATOR.md](docs/ANDROID_EMULATOR.md).

Quick start:

```powershell
.\scripts\open-in-android-studio.ps1
.\scripts\run-emulator-debug.ps1
```

Live connection debug (no phone/emulator):

```powershell
.\scripts\debug-connect.ps1 -ServerHost "voice.example.com"
```

## Test Server Setup

1. Run an official TeamSpeak 3 server on your LAN.
2. Create or use a channel with codec `Opus Voice`.
3. Connect from the app using host, port `9987`, nickname, and optional server password.
4. Validate receive/PTT/VAD against the desktop TeamSpeak client.

## Limitations

- OPUS_VOICE channels only
- One server connection and one persisted identity per install
- No bookmarks, text chat, whispers, auto-reconnect, or Bluetooth routing UI
- Unofficial client; do not use TeamSpeak branding or imply endorsement
- Play Console foreground-service declarations are required before publication

## Identity Storage

The TS3 identity private key and per-install HWID are stored in app-private `noBackupFilesDir/identity.dat`. Saved-server records exclude server and channel passwords. Passwords and imported identities are sensitive; do not include them in logs or bug reports. UI saved-state hardening is tracked in [docs/ROADMAP.md](docs/ROADMAP.md).

## Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/ROADMAP.md](docs/ROADMAP.md)
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)

## Legal

T3Vox is an independent project and is not affiliated with, endorsed by, or sponsored by TeamSpeak Systems GmbH. TeamSpeak is a trademark of TeamSpeak Systems GmbH.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for dependency and trademark notices.
