[CmdletBinding()]
param(
    [string]$OutputPath = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$upstreamUrl = "https://jitpack.io/com/github/manevolent/ts3j/db57d60/ts3j-db57d60.jar"
$upstreamSha256 = "50515411094C5872FD8B62BDFB6C14AC6C5F6C5347CC4FD2A086327EFD7EC294"
$patchedSha256 = "D360466A61B92A131EF2CEF0CFC81D6DD5035FD23082B7E6D63CF69242C8E004"
$removedEntry = "com/github/manevolent/ts3j/protocol/packet/handler/client/LocalClientHandlerConnecting.class"

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $PSScriptRoot "..\app\libs\ts3j-db57d60-patched.jar"
}

$temporaryDirectory = Join-Path ([IO.Path]::GetTempPath()) ("ts3j-" + [guid]::NewGuid().ToString("N"))
$upstreamJar = Join-Path $temporaryDirectory "ts3j-db57d60.jar"
$patchedJar = Join-Path $temporaryDirectory "ts3j-db57d60-patched.jar"
New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null

try {
    Invoke-WebRequest -Uri $upstreamUrl -OutFile $upstreamJar -UseBasicParsing
    $downloadedHash = (Get-FileHash -Algorithm SHA256 $upstreamJar).Hash
    if ($downloadedHash -ne $upstreamSha256) {
        throw "Upstream SHA-256 mismatch. Expected $upstreamSha256, received $downloadedHash."
    }

    Copy-Item -LiteralPath $upstreamJar -Destination $patchedJar
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::Open(
        $patchedJar,
        [System.IO.Compression.ZipArchiveMode]::Update
    )
    try {
        $entry = $archive.GetEntry($removedEntry)
        if ($null -eq $entry) {
            throw "Expected JAR entry was not found: $removedEntry"
        }
        $entry.Delete()
    } finally {
        $archive.Dispose()
    }

    $resultHash = (Get-FileHash -Algorithm SHA256 $patchedJar).Hash
    if ($resultHash -ne $patchedSha256) {
        throw "Patched SHA-256 mismatch. Expected $patchedSha256, received $resultHash."
    }

    $outputDirectory = Split-Path -Parent $OutputPath
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    Copy-Item -LiteralPath $patchedJar -Destination $OutputPath -Force
    Write-Host "Rebuilt $OutputPath"
    Write-Host "SHA-256: $resultHash"
} finally {
    if (Test-Path -LiteralPath $temporaryDirectory) {
        Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
    }
}
