[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$failures = [System.Collections.Generic.List[string]]::new()
$files = @(git ls-files --cached --others --exclude-standard)
$forbiddenFiles = @("local.properties", "live-debug-output.txt", "keystore.properties")

foreach ($file in $files) {
    $normalized = $file.Replace("\", "/")
    if ($forbiddenFiles -contains $normalized) {
        $failures.Add("Forbidden file is visible to Git: $file")
    }
    if ($normalized -match "(^|/)(app/build|build|\.android-sdk)/") {
        $failures.Add("Generated or SDK file is visible to Git: $file")
    }
    if ($normalized -match "\.(jks|keystore|pem|p12)$") {
        $failures.Add("Credential or signing file is visible to Git: $file")
    }
}

$textExtensions = @(
    ".java", ".json", ".kt", ".kts", ".md", ".properties",
    ".ps1", ".txt", ".xml", ".yaml", ".yml"
)
$forbiddenPatterns = [ordered]@{
    "personal Windows path" = "C:\\Users\\"
    "private test server" = ("bazitory" + "\.com")
    "private SRV target" = ("ts" + "\.hxts\.ir")
    "private debug nickname" = ("Cursor" + "Debug")
}

foreach ($file in $files) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { continue }
    $extension = [IO.Path]::GetExtension($file).ToLowerInvariant()
    $isGitIgnore = [IO.Path]::GetFileName($file) -eq ".gitignore"
    if (-not $isGitIgnore -and $textExtensions -notcontains $extension) { continue }
    foreach ($entry in $forbiddenPatterns.GetEnumerator()) {
        if (Select-String -LiteralPath $file -Pattern $entry.Value -Quiet) {
            $failures.Add("$($entry.Key) found in $file")
        }
    }
}

$jarPath = "app/libs/ts3j-db57d60-patched.jar"
$expectedJarHash = "D360466A61B92A131EF2CEF0CFC81D6DD5035FD23082B7E6D63CF69242C8E004"
$removedEntry = "com/github/manevolent/ts3j/protocol/packet/handler/client/LocalClientHandlerConnecting.class"

if (-not (Test-Path -LiteralPath $jarPath)) {
    $failures.Add("Patched ts3j JAR is missing.")
} else {
    $actualJarHash = (Get-FileHash -Algorithm SHA256 $jarPath).Hash
    if ($actualJarHash -ne $expectedJarHash) {
        $failures.Add("Patched ts3j SHA-256 mismatch: $actualJarHash")
    }
    $jarEntries = @(jar tf $jarPath)
    if ($LASTEXITCODE -ne 0) {
        $failures.Add("Unable to inspect patched ts3j JAR with jar.")
    } elseif ($jarEntries -contains $removedEntry) {
        $failures.Add("Patched ts3j JAR still contains $removedEntry")
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}
Write-Host "Publication readiness checks passed."
