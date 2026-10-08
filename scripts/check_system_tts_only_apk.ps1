#requires -Version 7.0
[CmdletBinding()]
param(
    [string]$ApkPath,
    [string]$ReportPath
)

function Get-SystemTtsForbiddenCategories {
    param([Parameter(Mandatory)][string]$EntryPath)
    $entry = $EntryPath.Replace('\', '/').ToLowerInvariant()
    $leaf = ($entry -split '/')[-1]
    # P18a's entire AISHELL3/Melo directories include tokens, lexicon, FST and dict.
    if ($entry -match '(^|/)(offline_tts|vits-melo-tts[^/]*)(/|$)') { 'OFFLINE_ASSET' }
    if ($entry -match '(^|/)iflytek(/|$)' -or
        $leaf -in @('e2ebe0251_1.0.0_ivtts_ce+pe_xiaofeng.16k.irf',
                    'e6762d222_1.0.0_ivtts_ce+pe_xiaoyan.16k.irf')) { 'IFLYTEK_ASSET' }
    if ($leaf -match '^libsherpa-onnx.*\.so(\.\d+)*$') { 'SHERPA_NATIVE' }
    if ($leaf -match '^libonnxruntime.*\.so(\.\d+)*$') { 'OFFLINE_ORT' }
    if ($entry -match '(^|/)bench_models(/|$)|(^|/)matcha[^/]*/' -or
        $leaf -match '^matcha[^/]*\.(onnx|ort|bin)$|^model-steps-\d+\.onnx$|^vocos[^/]*\.(onnx|ort)$') { 'MATCHA_MODEL' }
    # Deliberately NOT a global *.onnx/*.so ban: unrelated models and AndroidX stay legal.
}

function Test-SystemTtsOnlyApk {
    param([Parameter(Mandatory)][string]$Path)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $resolvedApk = (Resolve-Path -LiteralPath $Path -ErrorAction Stop).Path
    $zip = [IO.Compression.ZipFile]::OpenRead($resolvedApk)
    try {
        $forbidden = @(foreach ($item in $zip.Entries) {
            # Include directory entries too, so even an empty forbidden folder fails.
            $categories = @(Get-SystemTtsForbiddenCategories -EntryPath $item.FullName)
            if ($categories.Count) {
                [pscustomobject]@{ entry = $item.FullName; categories = $categories; size = $item.Length }
            }
        })
        $allNames = @($zip.Entries.FullName)
        [pscustomobject][ordered]@{
            APK_SHA256 = (Get-FileHash -LiteralPath $resolvedApk -Algorithm SHA256).Hash.ToLowerInvariant()
            APK_SIZE = (Get-Item -LiteralPath $resolvedApk).Length
            OFFLINE_ASSET_COUNT = @($forbidden | Where-Object { $_.categories -contains 'OFFLINE_ASSET' }).Count
            IFLYTEK_ASSET_COUNT = @($forbidden | Where-Object { $_.categories -contains 'IFLYTEK_ASSET' }).Count
            SHERPA_NATIVE_COUNT = @($forbidden | Where-Object { $_.categories -contains 'SHERPA_NATIVE' }).Count
            OFFLINE_ORT_COUNT = @($forbidden | Where-Object { $_.categories -contains 'OFFLINE_ORT' }).Count
            MATCHA_MODEL_COUNT = @($forbidden | Where-Object { $_.categories -contains 'MATCHA_MODEL' }).Count
            FORBIDDEN_ENTRY_COUNT = $forbidden.Count
            FORBIDDEN_ENTRIES = $forbidden
            ARCHIVE_STRUCTURE_VALID = ($allNames -ccontains 'AndroidManifest.xml') -and ($allNames -ccontains 'classes.dex')
            RESULT = if ($forbidden.Count) { 'FAIL_FORBIDDEN_PAYLOAD' }
                elseif (-not (($allNames -ccontains 'AndroidManifest.xml') -and ($allNames -ccontains 'classes.dex'))) { 'FAIL_INVALID_APK' }
                else { 'PASS' }
        }
    } finally { $zip.Dispose() }
}

# Dot sourcing exposes the shared policy to the build script without inspecting an APK.
if ($MyInvocation.InvocationName -ne '.') {
    $ErrorActionPreference = 'Stop'
    if ([string]::IsNullOrWhiteSpace($ApkPath)) { throw '-ApkPath is required.' }
    $result = Test-SystemTtsOnlyApk -Path $ApkPath
    $json = $result | ConvertTo-Json -Depth 8
    if ($ReportPath) { $json | Set-Content -LiteralPath $ReportPath -Encoding utf8 }
    $json
    if ($result.RESULT -ne 'PASS') { exit 1 }
}
