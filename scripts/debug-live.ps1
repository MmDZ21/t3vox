# Stream live TS3 client logs from a connected phone.
# Usage: .\scripts\debug-live.ps1

$adb = Join-Path $PSScriptRoot "..\.android-sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb"
    exit 1
}

$devices = & $adb devices | Select-String "device$"
if (-not $devices) {
    Write-Host "No phone connected. Plug in your device with USB debugging enabled."
    Write-Host ""
    Write-Host "Or run the PC-side live test with an explicit server:"
    Write-Host '  .\scripts\debug-connect.ps1 -ServerHost "voice.example.com"'
    exit 1
}

Write-Host "Watching Ts3Client logs — connect from the app now (Ctrl+C to stop)"
& $adb logcat -c
& $adb logcat -s Ts3Client:D AndroidRuntime:E
