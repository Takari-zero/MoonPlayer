$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workflowDir = Split-Path -Parent $scriptDir
$projectRoot = Split-Path -Parent $workflowDir

Set-Location $projectRoot

$branch = git rev-parse --abbrev-ref HEAD
$head = git rev-parse --short HEAD
$status = git status --short

Write-Host "Branch: $branch"
Write-Host "HEAD: $head"
Write-Host "git status --short:"
if ($status) {
    $status | ForEach-Object { Write-Host $_ }
    Write-Host "ERROR: working tree is not clean. Stop." -ForegroundColor Red
    exit 1
}

Write-Host "OK: working tree is clean." -ForegroundColor Green
exit 0
