# Architecture

TS3 Mobile (Unofficial) is a single-module Android application that connects to **one** TeamSpeak 3 server at a time, receives and transmits **OPUS_VOICE** audio, and manages a **single local identity**. It does not implement text chat, whispers, or server administration.

Source code lives under `app/src/main/java/com/toosarax/ts3client/` with package boundaries for UI, client session coordination, protocol, audio, settings, and foreground service code.

## Entry and Wiring

| Component | Role |
| --- | --- |
| `Ts3Application` | Application entry. Initializes DNS bootstrap, constructs `AppContainer`, and warms identity preparation in non-debug builds. |
| `AppContainer` | Manual dependency container. Wires `SettingsStore`, `Ts3IdentityStore`, `ForegroundServiceCoordinator`, `DnsJavaTs3ServerResolver`, `Ts3jProtocolFactory`, and lazy `DefaultClientRuntime`. |
| `MainActivity` | Requests `RECORD_AUDIO` and (on API 33+) `POST_NOTIFICATIONS`, then hosts Compose UI via `Ts3App` and a `ClientViewModel` factory bound to `AppContainer`. |

## UI Layer (`ui/`)

| Component | Role |
| --- | --- |
| `Ts3App` | Root Compose navigation between connect and channel experiences. |
| `ConnectScreen` | Server host, port, nickname, optional password and imported identity, saved-server list, and connect actions. |
| `ChannelScreen` | Channel tree, user list, join-channel flow, voice mode controls, and disconnect. |
| `PttButton` | Push-to-talk press/hold interaction wired to the view model. |
| `ClientViewModel` | Exposes `ClientController` state and settings flows; forwards connect, disconnect, channel join, and voice-mode commands. |

Supporting UI: `UserErrorMessages.kt` maps `ClientError` values to user-facing strings; `theme/Theme.kt` defines Material 3 theming.

## Session Layer (`client/`)

| Component | Role |
| --- | --- |
| `ClientController` | Interface for connect, disconnect, directory refresh, channel join, and voice controls. |
| `DefaultClientRuntime` | Primary implementation. Owns protocol instance, audio engines, wake lock, foreground-service lease, and `ClientState` updates. |
| `ClientModels` | `ClientState`, `ConnectionStatus`, channel and client directory models. |
| `ClientError` | Typed failure cases surfaced to the UI. |
| `ChannelTreeBuilder` | Orders channels by TS3 predecessor chain for tree display. |

## Protocol Layer (`protocol/`)

| Component | Role |
| --- | --- |
| `DnsJavaTs3ServerResolver` | Resolves server host and port; performs `_ts3._udp` SRV lookup on the default port with injectable lookup seam for tests. |
| `Ts3ServerResolver` / `ProtocolModels` | Resolver interface and shared protocol types (`ConnectRequest`, `VoiceMode`, events). |
| `Ts3jProtocol` | ts3j-backed `Ts3Protocol` implementation: connection lifecycle, directory sync, channel join, voice packet routing. |
| `Ts3IdentityStore` | Loads, creates, or imports a single local identity; persists private key material separately from UI settings. |
| `Ts3jMicrophone` | Bridges transmit engine output to ts3j voice transmission. |
| `Ts3jVoiceMapper` | Maps incoming ts3j voice packets to the receive audio pipeline. |
| `AndroidDnsBootstrap` | Initializes dnsjava resolver context on application start. |
| `Ts3DebugLog` | Debug-only logging helper. |

The vendored ts3j JAR is patched for Init1 puzzle handling. See [third_party/ts3j/README.md](../third_party/ts3j/README.md) for upstream commit, hashes, removed JAR entry, and replacement Java source.

## Receive Audio (`audio/rx/`)

| Component | Role |
| --- | --- |
| `IncomingVoiceEngine` | Decodes and mixes incoming voice for active speakers. |
| `ConcentusVoiceDecoder` | Opus decode via Concentus. |
| `PcmMixer` | Mixes multiple speaker PCM streams. |
| `SpeakerStream` | Per-client receive stream state. |
| `AndroidPcmOutput` | `AudioTrack` playback on Android. |

Shared audio types: `AudioInterfaces.kt`, `AudioConstants.kt`, `AndroidAudioSession.kt` (focus and session management).

## Transmit Audio (`audio/tx/`)

| Component | Role |
| --- | --- |
| `TransmitEngine` | Encodes and buffers outgoing voice; supports push-to-talk and RMS voice activation. |
| `AndroidPcmInput` | `AudioRecord` capture on Android. |
| `ConcentusVoiceEncoder` | Opus encode via Concentus. |
| `PcmFrameAssembler` | Frames PCM for encoding. |
| `EncodedPacketBuffer` | Queues encoded packets for protocol transmission. |
| `RmsVad` | RMS-based voice-activity detection for VAD mode. |

## Foreground Service (`service/`)

| Component | Role |
| --- | --- |
| `VoiceForegroundService` | Foreground service with microphone and media-playback types; hosts disconnect notification action. |
| `ForegroundServiceCoordinator` | Starts/stops service and coordinates readiness with the client runtime. |
| `ForegroundServiceLease` | Reference-counted lease so the service stays alive during an active voice session. |
| `VoiceNotification` | Notification channel, builder, and disconnect broadcast action. |

## Settings vs Identity Storage

| Store | Location | Contents |
| --- | --- | --- |
| `SettingsStore` | DataStore preferences (`ts3_settings`) | Voice mode, VAD threshold, last host/port/nickname, saved server list (no passwords) |
| `Ts3IdentityStore` | `context.noBackupFilesDir/identity.dat` | Private identity key material and HWID; excluded from backup |

Passwords and imported identity INI content are session inputs and must not be written to `SettingsStore`. Identity persistence is app-private and separate from user-facing saved-server metadata.

## Data Flow

```text
UI (Ts3App / screens / PttButton)
  → ClientViewModel
    → ClientController (DefaultClientRuntime)
      → Ts3ServerResolver (SRV + host normalization)
      → Ts3Protocol (Ts3jProtocol + Ts3IdentityStore)
      → TransmitEngine / IncomingVoiceEngine
      → ClientState (StateFlow)
      → ForegroundServiceCoordinator / VoiceForegroundService
```

Typical connect flow:

1. User submits connection fields on `ConnectScreen`.
2. `ClientViewModel` builds a `ConnectRequest` and calls `controller.connect`.
3. `DefaultClientRuntime` resolves the endpoint, acquires audio focus, starts the foreground service, and opens `Ts3jProtocol`.
4. Protocol events update directory state; channel join selects the voice codec path.
5. Incoming packets flow through `Ts3jVoiceMapper` → `IncomingVoiceEngine` → `AndroidPcmOutput`.
6. Outgoing audio flows `AndroidPcmInput` → `TransmitEngine` → `Ts3jMicrophone` → server.
7. Disconnect tears down protocol, audio, wake lock, and service lease.

## Product Boundaries

- **One server** per session; no multi-server or connection switching without disconnect.
- **OPUS_VOICE** codec path only; other codecs are not supported in this MVP.
- **One identity** per install (`Ts3IdentityStore`); desktop import replaces the stored identity.
- **No text chat**, whispers, file transfer, or server/channel administration features.
- **Unofficial** client; not affiliated with TeamSpeak Systems GmbH.

## Tests

Unit tests live in `app/src/test/` mirroring production packages. Live connection checks (`LiveConnectionDebugTest`) require explicit environment variables and are excluded from normal unit test runs.

## Related Documentation

- [third_party/ts3j/README.md](../third_party/ts3j/README.md) — patched dependency provenance
- [ROADMAP.md](ROADMAP.md) — planned hardening and non-goals
- [CONTRIBUTING.md](../CONTRIBUTING.md) — contributor workflow
