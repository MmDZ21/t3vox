# Publication Checklist

Passing this checklist prepares source for review; it does not authorize pushing, publication, tagging, or binary distribution.

Use this list during Milestone 1 source-release preparation and before any owner or legal review.

## Engineering

- [x] Repository hygiene: `.gitignore` excludes build output, SDK paths, credentials, and diagnostic artifacts
- [x] Deterministic unit tests: normal tests make no external DNS or live-server requests
- [x] Patched ts3j provenance: JAR hash matches documented value; rebuild script reproduces byte-for-byte
- [x] License files: `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`, and `third_party/` copies are present and consistent
- [x] Contributor docs: `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`
- [x] CI workflow runs publication checks, unit tests, lint, and debug assembly
- [x] Clean build: `.\gradlew.bat clean :app:assembleDebug` succeeds from a fresh tree
- [x] Lint: `:app:lintDebug` completes without errors
- [x] Public-data scan: no personal absolute paths, private test servers, debug nicknames, or credential files in tracked text

## Legal and ownership (human approval required)

- [ ] Legal review completed for trademark, protocol, reverse-engineering, and distribution questions
- [x] Final project name and branding approved: **T3Vox**
- [x] GitHub owner account confirmed: **mmdz21**
- [ ] Security reporting channel configured (private vulnerability reporting or Security Advisory)
- [x] Repository visibility approved: **public source repository**
- [ ] Binary distribution approval (APK/AAB upload, signing, store listing)

## Notes

- Engineering items should be evidenced locally (test output, script results, `git status`) before checking them off.
- Unchecked gates block their corresponding action unless the owner records an explicit risk acceptance below; binary distribution always requires its own approval.
- T3Vox rename verification on 2026-07-16: publication readiness passed; 25 unit tests passed; debug lint completed with 0 errors and 29 warnings; debug assembly succeeded.
- On 2026-07-16, the owner explicitly approved publishing the source without formal legal clearance after reviewing the recorded trademark and protocol risks. This is a risk acceptance, not a completed legal review. Tags, releases, APKs, and AABs remain unauthorized.
