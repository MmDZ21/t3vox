# Live PC-side connection debug. This always requires an explicit server.
# Usage:
#   .\scripts\debug-connect.ps1 -ServerHost "voice.example.com"
#   .\scripts\debug-connect.ps1 -ServerHost "voice.example.com" -PromptForPassword

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ServerHost,
    [ValidateRange(1, 65535)]
    [int]$Port = 9987,
    [ValidateNotNullOrEmpty()]
    [string]$Nick = "Ts3MobileDebug",
    [switch]$PromptForPassword
)

$previousHost = $env:TS3_HOST
$previousPort = $env:TS3_PORT
$previousNick = $env:TS3_NICK
$previousPassword = $env:TS3_PASSWORD

try {
    $env:TS3_HOST = $ServerHost
    $env:TS3_PORT = "$Port"
    $env:TS3_NICK = $Nick
    if ($PromptForPassword) {
        $securePassword = Read-Host "Server password" -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try {
            $env:TS3_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
    } elseif ($null -eq $env:TS3_PASSWORD) {
        $env:TS3_PASSWORD = ""
    }

    Set-Location (Join-Path $PSScriptRoot "..")
    Write-Host "Running: .\gradlew.bat :app:liveConnectDebug"
    Write-Host ("Target: {0}:{1}" -f $ServerHost, $Port)
    .\gradlew.bat :app:liveConnectDebug --console=plain
    exit $LASTEXITCODE
} finally {
    $env:TS3_HOST = $previousHost
    $env:TS3_PORT = $previousPort
    $env:TS3_NICK = $previousNick
    $env:TS3_PASSWORD = $previousPassword
}
