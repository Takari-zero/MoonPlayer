$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workflowDir = Split-Path -Parent $scriptDir
$projectRoot = Split-Path -Parent $workflowDir

Set-Location $projectRoot

& (Join-Path $scriptDir "git-guard.ps1")
if ($LASTEXITCODE -ne 0) {
    Write-Host "git-guard failed. Stop task generation." -ForegroundColor Red
    exit 1
}

$statePath = Join-Path $workflowDir "state.json"
$queuePath = Join-Path $workflowDir "queue.md"
$templatePath = Join-Path $workflowDir "templates\codex_task.md"
$agentsPath = Join-Path $projectRoot "AGENTS.md"
$currentTaskPath = Join-Path $workflowDir "current-task.md"

$state = Get-Content $statePath -Raw | ConvertFrom-Json
$queue = Get-Content $queuePath -Raw
$template = Get-Content $templatePath -Raw
$agents = if (Test-Path $agentsPath) { Get-Content $agentsPath -Raw } else { "" }

$currentTaskId = $state.currentTask
$taskPattern = "(?ms)^### $([regex]::Escape($currentTaskId))\b.*?(?=^---\s*$|^### |\z)"
$taskMatch = [regex]::Match($queue, $taskPattern)
$taskText = if ($taskMatch.Success) { $taskMatch.Value.Trim() } else { "Could not extract the exact task. Full queue follows:`n`n$queue" }

$agentsSummary = ($agents -split "`r?`n" | Where-Object {
    $_ -match "不允许|不主动|假成功|Stitch|Product Design|ADB|logcat|每轮|提交|push|真机"
}) -join "`n"

$content = @"
# LocalVibe current workflow task

## Current task id

$currentTaskId

## state.json summary

* project: $($state.project)
* module: $($state.module)
* currentTask: $($state.currentTask)
* adbPath: $($state.adbPath)
* adbPort: $($state.adbPort)
* requireUserConfirmationBeforeCommit: $($state.requireUserConfirmationBeforeCommit)
* allowAutoPush: $($state.allowAutoPush)
* lastKnownMainCommit: $($state.lastKnownMainCommit)
* uiDesignRule: $($state.uiDesignRule)

## Current queue task

$taskText

## Codex task template

$template

## AGENTS.md key rules

$agentsSummary

## Important reminder

Before building a new dialog, new panel, or complex UI, ask the user to create a Stitch / Product Design mockup first. Implement only after the user confirms the design.

Do not auto-commit.
Do not auto-push.
"@

Set-Content -Path $currentTaskPath -Value $content -Encoding UTF8
Write-Host "Generated .workflow/current-task.md. Copy it to Codex for execution." -ForegroundColor Green

$answer = Read-Host "Run android-check.ps1? Type y to continue"
if ($answer -eq "y") {
    & (Join-Path $scriptDir "android-check.ps1")
}
