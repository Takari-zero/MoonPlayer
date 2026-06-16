# LocalVibe Workflow Queue

## Current module: Video

### TODO-001 Workflow smoke test

Status: done

Goal:
Verify the local workflow scripts:

* check clean git state
* read state.json
* generate current-task.md
* optionally run Android build/install/start checks

Allowed files:

* .workflow/**
* AGENTS.md

Forbidden files:

* app/**
* gradle/**
* build.gradle
* settings.gradle
* AndroidManifest.xml

Acceptance:

* git-guard.ps1 runs
* android-check.ps1 exists and is parseable
* run-next.ps1 generates .workflow/current-task.md
* current-task.md is readable
* current-task.md is ignored by git
* no auto commit
* no auto push

Needs real-device confirmation: no

---

### TODO-002 Equalizer documentation sync

Status: done

Goal:
Document the completed system equalizer panel:

* Equalizer
* BassBoost
* Virtualizer
* translucent panel
* thin horizontal sliders
* 4dp track
* 16dp round thumb
* compact one-page-first layout
* ADB 62001 test flow

Allowed files:

* docs/DEVELOPMENT_PITFALLS.md
* docs/PROJECT_ROADMAP.md
* docs/MODULE_TECH_STACK.md

Forbidden files:

* app/**
* gradle/**
* AndroidManifest.xml

Needs real-device confirmation: no

---

### TODO-003 New UI mockup reminder

Status: done

Goal:
Before any new dialog, panel, or complex UI work, remind the user to create a Stitch or Product Design mockup first.

Allowed files:

* .workflow/queue.md
* AGENTS.md

Forbidden files:

* app/**

Needs real-device confirmation: no

---

### TODO-004 Video picture adjustment first real version

Status: doing

Goal:
Before implementation, ask the user to create a Stitch or Product Design mockup for the dialog or panel.

Initial feature direction:

* brightness
* contrast
* saturation
* color temperature
* reset defaults

Rules:

* Research low-risk implementation first.
* Do not modify system brightness.
* Do not fake success.

Needs real-device confirmation: yes

---

### TODO-005 FFmpeg audio extension sample validation

Status: blocked

Blocker:

* Missing AC3 / DTS / EAC3 / TrueHD samples.
* GPL-3.0 risk is not confirmed.

Experiment branch:
experiment/ffmpeg-audio-decoder

Local experiment commit:
d1b2022
