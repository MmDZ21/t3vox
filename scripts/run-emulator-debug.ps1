# Run the TS3 app on an Android emulator (after Android Studio + AVD are set up).
# Usage: .\scripts\run-emulator-debug.ps1
#
# IMPORTANT: In Android Studio, select run configuration "app" (NOT "All Tests").
# Unit tests run on your PC, not on the emulator.

param(
    [string]$SdkRoot = "",
    [string]$AvdName = "Ts3Debug"
)

$projectRoot = Join-Path $PSScriptRoot ".."
Set-Location $projectRoot

function Resolve-SdkRoot {
    param([string]$Override)
    if ($Override) { return $Override }
    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:ANDROID_HOME",
        "$env:ANDROID_SDK_ROOT",
        (Join-Path $projectRoot ".android-sdk")
    )
    foreach ($path in $candidates) {
        if ($path -and (Test-Path (Join-Path $path "platform-tools\adb.exe"))) {
            return $path
        }
    }
    return $null
}

$sdk = Resolve-SdkRoot -Override $SdkRoot
if (-not $sdk) {
    Write-Host "No Android SDK found."
    Write-Host "Install Android Studio first, then re-run this script."
    Write-Host "Guide: docs\ANDROID_EMULATOR.md"
    exit 1
}

$adb = Join-Path $sdk "platform-tools\adb.exe"
$emulator = Join-Path $sdk "emulator\emulator.exe"
$avdmanager = Join-Path $sdk "cmdline-tools\latest\bin\avdmanager.bat"

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

Write-Host "SDK: $sdk"

if (-not (Test-Path $emulator)) {
    Write-Host "Emulator is not installed in this SDK."
    Write-Host "Open Android Studio -> Settings -> Languages & Frameworks -> Android SDK"
    Write-Host "SDK Tools tab -> check 'Android Emulator' -> Apply"
    Write-Host "Guide: docs\ANDROID_EMULATOR.md"
    exit 1
}

$running = & $adb devices | Select-String "emulator-" | ForEach-Object { ($_ -split "\s+")[0] }
if (-not $running) {
    $avds = & $emulator -list-avds 2>$null
    if (-not $avds) {
        Write-Host "No AVD found. Create one in Android Studio Device Manager (name suggestion: $AvdName)"
        Write-Host "Guide: docs\ANDROID_EMULATOR.md"
        exit 1
    }
    $targetAvd = if ($avds -contains $AvdName) { $AvdName } else { $avds[0] }
    Write-Host "Starting emulator: $targetAvd"
    Start-Process -FilePath $emulator -ArgumentList @("-avd", $targetAvd, "-netdelay", "none", "-netspeed", "full") -WindowStyle Normal
    Write-Host "Waiting for emulator to boot..."
    & $adb wait-for-device | Out-Null
    $deadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 2
        $booted = & $adb shell getprop sys.boot_completed 2>$null
    } while ($booted -ne "1" -and (Get-Date) -lt $deadline)
    if ($booted -ne "1") {
        Write-Host "Emulator started but did not finish booting in time."
        exit 1
    }
}

Write-Host "Installing debug APK..."
.\gradlew.bat :app:installDebug --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Launching app and streaming logs (Ctrl+C to stop)..."
& $adb shell am start -n com.toosarax.ts3client/.MainActivity | Out-Null
& $adb logcat -c
& $adb logcat -s Ts3Client:D AndroidRuntime:E
