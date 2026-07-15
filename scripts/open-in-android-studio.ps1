# Open this project in Android Studio (installs Studio via winget if missing).
# Usage: .\scripts\open-in-android-studio.ps1

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$studioPaths = @(
    "$env:LOCALAPPDATA\Programs\Android Studio\bin\studio64.exe",
    "$env:ProgramFiles\Android\Android Studio\bin\studio64.exe",
    "${env:ProgramFiles(x86)}\Android\Android Studio\bin\studio64.exe"
)

$studio = $studioPaths | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $studio) {
    Write-Host "Android Studio not found."
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    if ($winget) {
        Write-Host "Installing Android Studio via winget (this can take a while)..."
        winget install --id Google.AndroidStudio -e --accept-package-agreements --accept-source-agreements
        $studio = $studioPaths | Where-Object { Test-Path $_ } | Select-Object -First 1
    }
}

if (-not $studio) {
    Write-Host "Install Android Studio manually: https://developer.android.com/studio"
    Write-Host "Then run: .\scripts\open-in-android-studio.ps1"
    exit 1
}

Write-Host "Opening project in Android Studio..."
Start-Process -FilePath $studio -ArgumentList $projectRoot
Write-Host "Next: follow docs/ANDROID_EMULATOR.md to create an emulator and click Run."
