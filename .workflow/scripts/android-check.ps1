$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workflowDir = Split-Path -Parent $scriptDir
$projectRoot = Split-Path -Parent $workflowDir

Set-Location $projectRoot

$env:JAVA_HOME = "E:\Program Files\JDK"
$env:ANDROID_HOME = "E:\Android"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"

Write-Host "java -version:"
java -version

Write-Host "Building debug APK..."
.\gradlew.bat :app:assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed. Stop before install to avoid installing a stale APK." -ForegroundColor Red
    exit 1
}

$adb = "E:\Android\platform-tools\adb.exe"
$adbPort = 62001

Write-Host "Checking ADB devices..."
$devices = & $adb -P $adbPort devices
$devices | ForEach-Object { Write-Host $_ }
$deviceLines = $devices | Select-String -Pattern "`tdevice$"
if (-not $deviceLines) {
    Write-Host "No device detected. Skip install and launch." -ForegroundColor Yellow
    exit 0
}

Write-Host "Installing debug APK..."
& $adb -P $adbPort install -r .\app\build\outputs\apk\debug\app-debug.apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed." -ForegroundColor Red
    exit 1
}

Write-Host "Launching app..."
& $adb -P $adbPort shell monkey -p com.shenghui.localvibe 1

Write-Host "Clearing logcat..."
& $adb -P $adbPort logcat -c

Read-Host "Manually test the feature, then press Enter to capture logcat"

& $adb -P $adbPort logcat -d |
    Select-String -Pattern "com.shenghui.localvibe|FATAL EXCEPTION|AndroidRuntime|ANR|NullPointerException|IllegalStateException|SecurityException|ExoPlaybackException|Equalizer|AudioEffect|BassBoost|Virtualizer" -Context 3,3

Write-Host "Only treat crashes with Process: com.shenghui.localvibe as app crashes. Do not misread system, MIUI, or other-process logs."
