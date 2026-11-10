param(
    [string]$ApiBaseUrl = "http://localhost:8080/api/v1"
)

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome) -or -not (Test-Path (Join-Path $javaHome "bin\jpackage.exe"))) {
    throw "Set JAVA_HOME to a JDK 21 installation before packaging."
}

$env:Path = "$(Join-Path $javaHome 'bin');$env:Path"
$target = Join-Path $projectRoot "target"
$inputDir = Join-Path $target "release-input"
$javafxDir = Join-Path $target "release-javafx"
$imageDir = Join-Path $target "release-image"
$appImage = Join-Path $imageDir "MusomiManager"
$zipPath = Join-Path $target "MusomiManager-1.0.0-Windows-x64.zip"

if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    throw "ApiBaseUrl cannot be empty."
}

Push-Location $projectRoot
try {
    & .\mvnw.cmd clean verify
    if ($LASTEXITCODE -ne 0) { throw "Desktop clean verify failed." }

    Remove-Item $inputDir, $javafxDir, $imageDir, $zipPath -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force $inputDir, $javafxDir, $imageDir | Out-Null

    & .\mvnw.cmd dependency:copy-dependencies -DincludeScope=runtime "-DoutputDirectory=$inputDir"
    if ($LASTEXITCODE -ne 0) { throw "Copying runtime dependencies failed." }

    Copy-Item (Join-Path $target "desktop-1.0.0.jar") $inputDir

    Get-ChildItem $inputDir -Filter "javafx-*-win.jar" |
        Move-Item -Destination $javafxDir

    Get-ChildItem $inputDir -Filter "javafx-*.jar" |
        Remove-Item -Force

    $modulePath = "$javaHome\jmods;$(Resolve-Path $javafxDir)"
    $requiredModules = "javafx.controls,javafx.fxml,java.naming,java.net.http"
    $jpackageArgs = @(
        "--type", "app-image",
        "--name", "MusomiManager",
        "--app-version", "1.0.0",
        "--vendor", "Musomi Manager",
        "--icon", (Join-Path $projectRoot "src\main\resources\images\musomi-logo.ico"),
        "--input", $inputDir,
        "--dest", $imageDir,
        "--main-jar", "desktop-1.0.0.jar",
        "--main-class", "com.musomi.desktop.DesktopLauncher",
        "--module-path", $modulePath,
        "--add-modules", $requiredModules,
        "--java-options", "--add-modules=$requiredModules",
        "--java-options", "-Dmusomi.api.baseUrl=$ApiBaseUrl"
    )

    & (Join-Path $javaHome "bin\jpackage.exe") @jpackageArgs
    if ($LASTEXITCODE -ne 0) { throw "Creating the Windows application image failed." }
    if (-not (Test-Path (Join-Path $appImage "MusomiManager.exe"))) {
        throw "The packaged MusomiManager.exe was not created."
    }

    Compress-Archive -Path (Join-Path $appImage "*") -DestinationPath $zipPath -CompressionLevel Optimal
    if (-not (Test-Path $zipPath)) { throw "The Windows package ZIP was not created." }

    $file = Get-Item $zipPath
    Write-Output "Windows portable package created: $($file.FullName)"
    Write-Output "Size (MB): $([math]::Round($file.Length / 1MB, 1))"
    Write-Output "Run MusomiManager.exe after extracting the ZIP. The backend must be running at: $ApiBaseUrl"
}
finally {
    Pop-Location
}


