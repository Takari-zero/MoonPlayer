# Codex Task Template

## Current task

Read currentTask from .workflow/state.json.
Find the corresponding task in .workflow/queue.md.

## Pre-flight checks

Run:

* git status
* git rev-parse --abbrev-ref HEAD
* git rev-parse --short HEAD

Stop if the working tree is not clean, unless the task explicitly allows the current dirty files.

## Work boundary

* Modify only allowed files.
* Do not modify forbidden files.
* Do not commit.
* Do not push.
* Do not fake success.

## UI rule

Before building any new dialog, new panel, or complex UI, remind the user to create a Stitch or Product Design mockup first.
Implement only after the user confirms the mockup or screenshot.

## Android test flow

Default Android checks:

* .\gradlew.bat :app:assembleDebug --console=plain
* E:\Android\platform-tools\adb.exe -P 62001 install
* E:\Android\platform-tools\adb.exe -P 62001 shell monkey

## Output format

1. Start status
2. Current task
3. Changes made
4. Unchanged confirmation
5. Test results
6. Items requiring user confirmation
7. Recommended next step
