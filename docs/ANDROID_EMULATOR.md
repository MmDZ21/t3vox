# Run the app on an Android emulator

The project SDK at `.android-sdk/` can build APKs but does **not** include the emulator. The easiest way to run and debug on PC is **Android Studio**.

## 1. Install Android Studio

```powershell
.\scripts\open-in-android-studio.ps1
```

Or download manually: https://developer.android.com/studio

## 2. Open this project

- **File → Open** → select the repository root containing settings.gradle.kts
- Let Gradle sync finish (first time may take several minutes)

## 3. Install SDK components (one-time)

**Settings → Languages & Frameworks → Android SDK**

**SDK Platforms** tab:
- Android 16.0 ("Baklava") API 36 — checked

**SDK Tools** tab — check:
- Android Emulator
- Android SDK Platform-Tools
- Google APIs Intel x86_64 Atom System Image (API 36) — or API 34 if 36 is unavailable

Click **Apply** and wait for downloads.

## 4. Create a virtual phone

**Tools → Device Manager → Create Device**

1. Phone: **Pixel 6** (or any)
2. System image: **API 36** (or 34) with **Google APIs** / x86_64
3. Name: `Ts3Debug`
4. Finish

## 5. Run the app

1. Start the emulator (▶ in Device Manager)
2. In Android Studio toolbar, select the `app` configuration and your emulator
3. Click **Run** (green ▶)

Or from terminal (after emulator exists):

```powershell
.\scripts\run-emulator-debug.ps1
```

## 6. Watch connection logs

In Android Studio: **View → Tool Windows → Logcat**

Filter: `Ts3Client`

Or terminal:

```powershell
.\scripts\debug-live.ps1
```

## Notes

- **Microphone / voice** on emulators is limited. Use the emulator to debug **connect UI, SRV resolve, and errors**. Voice is still best tested on a real phone.
- Each fresh emulator install gets a **new TS3 identity** (no need to clear app data).
- PC-side connection test (same protocol code):

```powershell
.\scripts\debug-connect.ps1 -ServerHost "voice.example.com"
```

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Gradle sync fails | Check internet; project uses Aliyun mirrors in `settings.gradle.kts` |
| No system images | SDK Manager → SDK Tools → show package details → Intel system image |
| Emulator won't start | Enable virtualization (BIOS: Intel VT-x / AMD-V); Windows Hypervisor |
| App installs but no network | Emulator uses host network by default; cold boot the AVD |
