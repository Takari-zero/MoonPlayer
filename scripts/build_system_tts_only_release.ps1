#requires -Version 7.0
[CmdletBinding()]
param(
    [string]$Commit,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$AndroidSdk = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }),
    [string]$BuildToolsVersion,
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'check_system_tts_only_apk.ps1')

function Invoke-ToolCapture {
    param([string]$Program, [string[]]$Arguments)
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue' # java/apksigner use stderr for non-fatal diagnostics.
        $toolLines = @(& $Program @Arguments 2>&1 | ForEach-Object { "$_" })
        $toolExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    [pscustomobject]@{ exitCode = $toolExit; lines = $toolLines }
}
function Invoke-GitRead {
    param([string[]]$Arguments)
    $gitResult = Invoke-ToolCapture -Program 'git' -Arguments $Arguments
    if ($gitResult.exitCode -ne 0) { throw "git failed: $($gitResult.lines -join [Environment]::NewLine)" }
    $gitResult.lines
}
function Assert-WithinDirectory {
    param([string]$Child, [string]$Parent)
    $prefix = [IO.Path]::GetFullPath($Parent).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    $resolvedChild = [IO.Path]::GetFullPath($Child)
    if (-not $resolvedChild.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes intended directory: $resolvedChild"
    }
}

$repo = (Invoke-GitRead @('-C', (Split-Path $PSScriptRoot -Parent), 'rev-parse', '--show-toplevel')) -join ''
$repo = [IO.Path]::GetFullPath($repo)
$beforeTracked = @(Invoke-GitRead @('-C', $repo, 'status', '--porcelain=v1', '--untracked-files=no'))
if (-not $PSBoundParameters.ContainsKey('Commit')) {
    if ($beforeTracked.Count) { throw 'Tracked worktree dirty: specify -Commit explicitly; no stash/reset is performed.' }
    $Commit = 'HEAD'
}
if ([string]::IsNullOrWhiteSpace($Commit) -or $Commit.StartsWith('-')) { throw 'Invalid commit.' }
$sourceCommit = (Invoke-GitRead @('-C', $repo, 'rev-parse', '--verify', "$Commit^{commit}")) -join ''
if ($sourceCommit -notmatch '^[0-9a-f]{40}$') { throw 'Cannot resolve immutable source commit.' }
if (-not $JavaHome) { throw 'Supply -JavaHome or JAVA_HOME (JDK 17).' }
if (-not $AndroidSdk) { throw 'Supply -AndroidSdk, ANDROID_HOME or ANDROID_SDK_ROOT.' }
$selectedJavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
$selectedSdk = (Resolve-Path -LiteralPath $AndroidSdk).Path
$javaExe = Join-Path $selectedJavaHome $(if ($IsWindows) { 'bin/java.exe' } else { 'bin/java' })
$javaResult = Invoke-ToolCapture $javaExe @('-version')
$javaVersionText = $javaResult.lines -join [Environment]::NewLine
if ($javaResult.exitCode -ne 0 -or $javaVersionText -notmatch '(?m)(?:openjdk|java) version "17(?:\.|\")') {
    throw "JDK 17 required; detected: $javaVersionText"
}
$toolsRoot = Join-Path $selectedSdk 'build-tools'
if (-not $BuildToolsVersion) {
    $versions = @(Get-ChildItem -LiteralPath $toolsRoot -Directory | Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } | Sort-Object { [version]$_.Name } -Descending)
    if (-not $versions.Count) { throw 'No stable Android build-tools installed.' }
    $BuildToolsVersion = $versions[0].Name
}
if ($BuildToolsVersion -notmatch '^\d+\.\d+\.\d+$') { throw 'Invalid build-tools version.' }
$aapt = Join-Path $toolsRoot "$BuildToolsVersion/$(if ($IsWindows) { 'aapt2.exe' } else { 'aapt2' })"
$apksigner = Join-Path $toolsRoot "$BuildToolsVersion/$(if ($IsWindows) { 'apksigner.bat' } else { 'apksigner' })"
if (-not (Test-Path -LiteralPath $aapt -PathType Leaf) -or -not (Test-Path -LiteralPath $apksigner -PathType Leaf)) { throw 'aapt2/apksigner missing.' }

# Pre-build source policy. Never transfer ignored/untracked/private inputs.
$sourcePaths = @(Invoke-GitRead @('-C', $repo, 'ls-tree', '-r', '--name-only', $sourceCommit))
foreach ($sourcePath in $sourcePaths) {
    if ($sourcePath -match '^app/src/[^/]+/(assets|jniLibs)/') {
        $payloadPath = $sourcePath -replace '^app/src/[^/]+/assets/', 'assets/' -replace '^app/src/[^/]+/jniLibs/', 'lib/'
        if (@(Get-SystemTtsForbiddenCategories $payloadPath).Count) { throw "FAIL_FORBIDDEN_PAYLOAD: tracked source $sourcePath" }
    }
}

$runId = "$($sourceCommit.Substring(0,12))-$([DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))-$([Guid]::NewGuid().ToString('N').Substring(0,8))"
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $repo "test-results/p18d/$runId" }
$outputRoot = [IO.Path]::GetFullPath($OutputDirectory)
Assert-WithinDirectory $outputRoot $repo
$relativeOutput = [IO.Path]::GetRelativePath($repo, $outputRoot).Replace('\', '/')
$ignoreResult = Invoke-ToolCapture 'git' @('-C', $repo, 'check-ignore', '-q', '--', "$relativeOutput/")
if ($ignoreResult.exitCode -ne 0) { throw 'OutputDirectory must be Git-ignored inside the calling repository.' }
if (Test-Path -LiteralPath $outputRoot) { throw 'OutputDirectory exists; use a new directory to preserve old evidence.' }
New-Item -ItemType Directory -Path $outputRoot | Out-Null
$worktreeRoot = Join-Path $outputRoot 'checkout'
Assert-WithinDirectory $worktreeRoot $outputRoot
$buildLog = Join-Path $outputRoot 'assemble-release.log'
$configReport = Join-Path $outputRoot 'release-config.json'
$savedEnvironment = @{}
foreach ($name in @('JAVA_HOME', 'ANDROID_HOME', 'ANDROID_SDK_ROOT')) { $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$worktreeCreated = $false
$buildStart = [DateTime]::UtcNow
$watch = [Diagnostics.Stopwatch]::StartNew()
$releaseReport = $null
$toolingHashes = @('build_system_tts_only_release.ps1', 'check_system_tts_only_apk.ps1', 'system_tts_release_audit.init.gradle') | ForEach-Object {
    [pscustomobject]@{ name = $_; sha256 = (Get-FileHash (Join-Path $PSScriptRoot $_) -Algorithm SHA256).Hash.ToLowerInvariant() }
}
try {
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $selectedJavaHome, 'Process')
    [Environment]::SetEnvironmentVariable('ANDROID_HOME', $selectedSdk, 'Process')
    [Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', $selectedSdk, 'Process')
    Invoke-GitRead @('-c', 'core.longpaths=true', '-C', $repo, 'worktree', 'add', '--detach', $worktreeRoot, $sourceCommit) | Write-Host
    $worktreeCreated = $true
    if ((Invoke-GitRead @('-C', $worktreeRoot, 'rev-parse', 'HEAD')) -ne $sourceCommit) { throw 'Worktree commit mismatch.' }
    if (@(Invoke-GitRead @('-C', $worktreeRoot, 'status', '--porcelain=v1', '--untracked-files=all')).Count) { throw 'New checkout is not clean.' }
    foreach ($privatePath in @('app/src/main/assets/offline_tts', 'app/src/main/assets/iflytek', 'app/src/main/assets/bench_models', 'third_party', 'local_secrets', 'app/libs/iflytek', 'local.properties')) {
        if (Test-Path -LiteralPath (Join-Path $worktreeRoot $privatePath)) { throw "Private input unexpectedly present: $privatePath" }
    }
    $gradleWrapper = Join-Path $worktreeRoot $(if ($IsWindows) { 'gradlew.bat' } else { 'gradlew' })
    $gradleArguments = @(':app:assembleRelease', ':app:writeSystemTtsReleaseAudit', '--no-daemon', '--max-workers=1', '--build-cache',
        '-Dorg.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8', '-Pkotlin.daemon.jvmargs=-Xmx4g',
        '--init-script', (Join-Path $PSScriptRoot 'system_tts_release_audit.init.gradle'), "-PsystemTtsReleaseAuditOutput=$configReport")
    Write-Host "Clean release source=$sourceCommit; worktree=$worktreeRoot; log=$buildLog"
    Push-Location $worktreeRoot
    $oldPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $gradleWrapper @gradleArguments 2>&1 | Tee-Object -FilePath $buildLog -ErrorAction Stop | Write-Host
        $buildExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $oldPreference; Pop-Location }
    $watch.Stop()
    if ($buildExit -ne 0) { throw "RELEASE_BUILD_FAILURE: Gradle exit $buildExit; see $buildLog" }
    $config = Get-Content -LiteralPath $configReport -Raw | ConvertFrom-Json
    $metadataPath = Join-Path $worktreeRoot 'app/build/outputs/apk/release/output-metadata.json'
    $metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
    if ($metadata.variantName -ne 'release' -or @($metadata.elements).Count -ne 1) { throw 'Expected one release APK; split/flavor support requires a reviewed extension.' }
    $element = $metadata.elements[0]
    $apkRoot = Split-Path $metadataPath -Parent
    $builtApk = [IO.Path]::GetFullPath((Join-Path $apkRoot $element.outputFile))
    Assert-WithinDirectory $builtApk $apkRoot
    $payload = Test-SystemTtsOnlyApk -Path $builtApk
    $payload | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $outputRoot 'payload-gate.json') -Encoding utf8
    if ($payload.RESULT -ne 'PASS') { throw $payload.RESULT }

    $badging = Invoke-ToolCapture $aapt @('dump', 'badging', $builtApk)
    $manifest = Invoke-ToolCapture $aapt @('dump', 'xmltree', $builtApk, '--file', 'AndroidManifest.xml')
    if ($badging.exitCode -ne 0 -or $manifest.exitCode -ne 0) { throw 'Release manifest inspection failed.' }
    $badging.lines | Set-Content (Join-Path $outputRoot 'apk-badging.txt') -Encoding utf8
    $manifest.lines | Set-Content (Join-Path $outputRoot 'apk-manifest.txt') -Encoding utf8
    $packageLine = @($badging.lines | Where-Object { $_ -like 'package:*' }) -join ''
    if ($metadata.applicationId -ne 'com.shenghui.localvibe' -or $config.applicationId -ne $metadata.applicationId -or
        $packageLine -notmatch "name='com\.shenghui\.localvibe'" -or
        $packageLine -notmatch "versionCode='$($element.versionCode)'" -or $packageLine -notmatch "versionName='$([regex]::Escape($element.versionName))'") { throw 'Release identity mismatch.' }
    if ($config.debuggable -or ($badging.lines -contains 'application-debuggable')) { throw 'Release must not be debuggable.' }
    # Restrict the query check to the queries subtree, not a service/action elsewhere.
    $queryLines = [Collections.Generic.List[string]]::new()
    $inQueries = $false
    $queryIndent = -1
    foreach ($manifestLine in $manifest.lines) {
        if ($manifestLine -match '^(\s*)E: queries\b') { $inQueries = $true; $queryIndent = $Matches[1].Length }
        elseif ($inQueries -and $manifestLine -match '^(\s*)E:' -and $Matches[1].Length -le $queryIndent) { $inQueries = $false }
        if ($inQueries) { $queryLines.Add($manifestLine) }
    }
    if (($queryLines -join "`n") -cnotmatch '(?s)E: intent.*?E: action.*?android\.intent\.action\.TTS_SERVICE') { throw 'TTS_SERVICE query missing in release APK.' }
    $permissions = @($badging.lines | Where-Object { $_ -match '^uses-permission(?:-sdk-\d+)?:' } | ForEach-Object { if ($_ -match "name='([^']+)'") { $Matches[1] } })
    # Compare resolved release permissions against the pinned merged/source manifest set.
    [xml]$sourceManifest = Get-Content (Join-Path $worktreeRoot 'app/src/main/AndroidManifest.xml') -Raw
    $sourcePermissions = @($sourceManifest.SelectNodes('/manifest/uses-permission') | ForEach-Object { $_.GetAttribute('name', 'http://schemas.android.com/apk/res/android') })
    $knownDependencyPermissions = @('android.permission.ACCESS_NETWORK_STATE', 'com.shenghui.localvibe.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION')
    $unexpectedPermissions = @($permissions | Where-Object { $_ -notin $sourcePermissions -and $_ -notin $knownDependencyPermissions })
    if ($unexpectedPermissions.Count) { throw "Unexpected release permissions: $($unexpectedPermissions -join ', ')" }
    $signature = Invoke-ToolCapture $apksigner @('verify', '--verbose', '--print-certs', $builtApk)
    $signature.lines | Set-Content (Join-Path $outputRoot 'apksigner-verify.txt') -Encoding utf8
    if ($config.signingConfig -eq 'debug') { throw 'Debug signing is not a release baseline.' }
    if ($signature.exitCode -eq 0) { throw 'This unsigned baseline workflow must not deliver a signed APK; audit signing separately in P18e.' }
    if ($config.signingConfig -or $element.outputFile -notmatch '-unsigned\.apk$' -or
        ($signature.lines -join "`n") -notmatch 'DOES NOT VERIFY|Missing META-INF/MANIFEST.MF|No signatures') { throw 'Unexpected signing verification failure (not a confirmed unsigned APK).' }
    if (@(Invoke-GitRead @('-C', $worktreeRoot, 'status', '--porcelain=v1', '--untracked-files=all')).Count) { throw 'Build mutated clean checkout source.' }
    $afterTracked = @(Invoke-GitRead @('-C', $repo, 'status', '--porcelain=v1', '--untracked-files=no'))
    if (($beforeTracked -join "`n") -ne ($afterTracked -join "`n")) { throw 'Calling tracked worktree changed during build; inspect, never reset.' }
    $outputApk = Join-Path $outputRoot $element.outputFile
    Copy-Item -LiteralPath $builtApk -Destination $outputApk
    Copy-Item -LiteralPath $metadataPath -Destination (Join-Path $outputRoot 'output-metadata.json')
    if ((Get-FileHash $outputApk -Algorithm SHA256).Hash.ToLowerInvariant() -ne $payload.APK_SHA256) { throw 'Published artifact hash mismatch.' }
    $releaseReport = [ordered]@{
        result = 'SYSTEM_TTS_ONLY_RELEASE_BASELINE_READY_UNSIGNED'
        gitCommit = $sourceCommit
        buildStartedUtc = $buildStart.ToString('o')
        buildFinishedUtc = [DateTime]::UtcNow.ToString('o')
        elapsedSeconds = [math]::Round($watch.Elapsed.TotalSeconds, 3)
        applicationId = $metadata.applicationId
        versionCode = $element.versionCode
        versionName = $element.versionName
        apk = $element.outputFile # Identity is relative name + commit + SHA, NOT a machine path.
        apkSize = $payload.APK_SIZE
        apkSha256 = $payload.APK_SHA256
        javaVersion = $javaVersionText
        gradleVersion = $config.gradleVersion
        inspectionBuildToolsVersion = $BuildToolsVersion
        releaseConfig = $config
        payloadGate = $payload
        signed = $false
        apksignerExitCode = $signature.exitCode
        ttsServiceQuery = $true
        permissions = $permissions
        privateInputsCopied = $false
        callerTrackedStatusUnchanged = $true
        sharedDependencyAndTaskCaches = $true
        signedReleaseRuntimeSmokeRequired = $true
        toolingSha256 = $toolingHashes
    }
    # Only remove our validated UUID checkout, never the calling repository.
    Assert-WithinDirectory $worktreeRoot $outputRoot
    $cleanupTop = (Invoke-GitRead @('-C', $worktreeRoot, 'rev-parse', '--show-toplevel')) -join ''
    if ([IO.Path]::GetFullPath($cleanupTop) -ne [IO.Path]::GetFullPath($worktreeRoot)) { throw 'Cleanup target identity mismatch.' }
    Invoke-GitRead @('-c', 'core.longpaths=true', '-C', $repo, 'worktree', 'remove', '--', $worktreeRoot) | Write-Host
    $worktreeCreated = $false
    $releaseReport.worktreeRemoved = $true
    $releaseReport | ConvertTo-Json -Depth 12 | Set-Content (Join-Path $outputRoot 'release-report.json') -Encoding utf8
    Write-Host "PASS: unsigned APK=$outputApk; report=$(Join-Path $outputRoot 'release-report.json')"
} catch {
    [ordered]@{
        result = 'FAIL'; error = $_.Exception.Message; gitCommit = $sourceCommit
        retainedWorktree = if ($worktreeCreated) { $worktreeRoot } else { $null }
        buildLog = $buildLog; outputDirectory = $outputRoot
    } | ConvertTo-Json | Set-Content (Join-Path $outputRoot 'failure-report.json') -Encoding utf8
    throw
} finally {
    foreach ($name in $savedEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process') }
}
