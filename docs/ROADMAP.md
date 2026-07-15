# Roadmap

This document describes planned milestones for T3Vox, an independent Android client compatible with TeamSpeak 3 servers. Timelines are indicative; scope may shift based on contributor capacity and feedback.

## Milestone 1 — Public-source alpha

**Status:** In progress (open-source release plan)

Goals:

- Publication-safe repository hygiene (ignored build output, no private paths or server references in tracked files)
- Deterministic unit tests with no live network in normal CI
- Auditable patched ts3j provenance and complete third-party license references
- Contributor documentation, security policy, architecture notes, and GitHub templates
- CI gates for publication checks, unit tests, lint, and debug assembly

This milestone does **not** publish binaries, create remotes, or change runtime product behavior beyond test seams.

## Milestone 2 — Usable beta

Goals:

- Remove `rememberSaveable` persistence for `serverPassword` and `importedIdentity` in the UI
- Persist imported identities only after a successful connection
- Fix cancellation, reconnect, and notification state edge cases
- Conditional microphone capture (active session only)
- Bluetooth and audio routing improvements
- Jitter and packet-loss handling improvements
- Broader physical-device test coverage for voice

## Milestone 3 — Distribution readiness

Goals:

- Privacy policy and data-handling documentation suitable for store review
- Accessibility improvements
- Signed, reproducible release builds
- Device and Android version test matrix
- Closed beta program
- Publisher eligibility (Play Console / equivalent)
- Store declarations (permissions, foreground service types, microphone use)

## Non-goals

The following are **out of scope** unless the project direction changes explicitly:

- Text chat
- Whispers
- Server or channel administration
- Multiple simultaneous server connections
- Monetization (ads, in-app purchases, subscriptions)
- TeamSpeak 6 (TS6) protocol support

## How to Propose Changes

Open an issue describing the user problem, affected milestone, and any protocol, audio, or security impact. See [CONTRIBUTING.md](../CONTRIBUTING.md) for coordination expectations.
