# T3Vox Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the public-facing Android project from TS3 Mobile (Unofficial) / Voice Chat to T3Vox while retaining clear TeamSpeak 3 compatibility and independence disclosures.

**Architecture:** Treat this as a presentation and documentation rename only. Update Android resources, Gradle project metadata, contributor/release documents, and publication assertions; preserve `com.toosarax.ts3client`, source paths, internal TS3 protocol identifiers, and historical task records.

**Tech Stack:** Android, Kotlin, Jetpack Compose, Gradle Kotlin DSL, PowerShell, Markdown

## Global Constraints

- Use `T3Vox` as the product and launcher name and `t3vox` as the Gradle/repository slug.
- Describe it as an unofficial open-source Android client compatible with TeamSpeak 3 servers.
- State that the project is independent and is not affiliated with, endorsed by, or sponsored by TeamSpeak.
- Do not rename the Android namespace, application ID, Kotlin packages, internal TS3 protocol types, or historical `ai/tasks.md` evidence.
- Do not push, create a remote, tag, or publish binaries.

---

### Task 1: Rename app and project metadata

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `settings.gradle.kts`

- [x] **Step 1:** Change `app_name` and user-facing microphone copy from `Voice Chat` to `T3Vox`.
- [x] **Step 2:** Change `rootProject.name` from `ts3` to `t3vox`.
- [x] **Step 3:** Confirm `applicationId`, namespace, package paths, and launch component remain unchanged.

### Task 2: Rename public documentation and legal notices

**Files:**
- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Modify: `NOTICE`
- Modify: `THIRD_PARTY_NOTICES.md`
- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/ROADMAP.md`
- Modify: `AGENTS.md`

- [x] **Step 1:** Replace the former product title with `T3Vox`.
- [x] **Step 2:** Use compatibility wording in descriptions and preserve necessary factual references to TeamSpeak 3.
- [x] **Step 3:** Add consistent independent-project and trademark wording without claiming endorsement.
- [x] **Step 4:** Keep package-path guidance unchanged.

### Task 3: Align publication safety and verify

**Files:**
- Modify if required: `scripts/check-publication-readiness.ps1`
- Modify: `docs/PUBLICATION_CHECKLIST.md`

- [x] **Step 1:** Search tracked files for stale public-facing names and review every remaining match.
- [x] **Step 2:** Run `git diff --check` and the publication-readiness script.
- [x] **Step 3:** Run `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`.
- [x] **Step 4:** Record the rename validation result in the publication checklist and review the final diff.
