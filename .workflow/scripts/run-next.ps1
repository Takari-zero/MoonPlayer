$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workflowDir = Split-Path -Parent $scriptDir
$projectRoot = Split-Path -Parent $workflowDir

Set-Location $projectRoot

& (Join-Path $scriptDir "git-guard.ps1")
if ($LASTEXITCODE -ne 0) {
    $dirtyFiles = git status --short | ForEach-Object {
        if ($_.Length -ge 4) { $_.Substring(3).Trim() } else { $_.Trim() }
    }
    $allowedDirty = $dirtyFiles | Where-Object {
        $_ -eq "AGENTS.md" -or
        $_ -like ".workflow/*" -or
        $_ -like ".workflow\*"
    }
    if ($dirtyFiles.Count -eq 0 -or $allowedDirty.Count -ne $dirtyFiles.Count) {
        Write-Host "git-guard failed and dirty files are outside the workflow maintenance scope. Stop task generation." -ForegroundColor Red
        exit 1
    }
    Write-Host "git-guard reported a dirty tree, but only workflow maintenance files are dirty. Continue for workflow validation." -ForegroundColor Yellow
}

$statePath = Join-Path $workflowDir "state.json"
$queuePath = Join-Path $workflowDir "queue.md"
$templatePath = Join-Path $workflowDir "templates\codex_task.md"
$agentsPath = Join-Path $projectRoot "AGENTS.md"
$currentTaskPath = Join-Path $workflowDir "current-task.md"

try {
    $state = Get-Content $statePath -Raw -Encoding UTF8 | ConvertFrom-Json
} catch {
    Write-Host "ERROR: Failed to parse .workflow/state.json. Check the file for invalid JSON or encoding problems." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

$queue = Get-Content $queuePath -Raw -Encoding UTF8
$template = Get-Content $templatePath -Raw -Encoding UTF8
$agents = if (Test-Path $agentsPath) { Get-Content $agentsPath -Raw -Encoding UTF8 } else { "" }

$currentTaskId = $state.currentTask
$taskPattern = "(?ms)^### $([regex]::Escape($currentTaskId))\b.*?(?=^---\s*$|^### |\z)"
$taskMatch = [regex]::Match($queue, $taskPattern)
$taskText = if ($taskMatch.Success) { $taskMatch.Value.Trim() } else { "Could not extract the exact task. Full queue follows:`n`n$queue" }

$agentsSummary = ($agents -split "`r?`n" | Where-Object {
    $_ -match "Do not|Stitch|Product Design|ADB|logcat|Every task|auto-commit|auto-push|Real-device"
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
