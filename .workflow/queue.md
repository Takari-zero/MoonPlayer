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

Status: done

Goal:
Implement the first real video picture adjustment panel from the confirmed Stitch or Product Design mockup.

Completed scope:

* brightness
* contrast
* saturation
* color temperature
* effect toggle
* presets: Default, Bright, Cinema, Eye Care, Vivid
* Done

Rules:

* Do not modify system brightness.
* Do not fake success.
* Android 12+ uses RenderEffect / ColorMatrixColorFilter.
* Android 12 and below must show unsupported state and disabled controls.
* The bottom duplicate reset entry is removed; the Default preset remains the reset path.

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

---

### TODO-006 Video playback regression review

Status: done

Goal:
Review video playback after equalizer and picture adjustment changes.

Scope:

* playback
* subtitles
* screenshots
* gestures
* AB repeat
* sleep timer
* speed control
* audio tracks
* playlist
* equalizer
* picture adjustment
* background pause behavior

Rules:

* Do not start music or novel modules yet.
* Do not add new features in this task.
* Only verify and document remaining video playback issues.

Needs real-device confirmation: yes

---

### TODO-007 Video remaining feature planning

Status: done

Goal:
Review the remaining video-module features and decide the next work item.

Rules:

* Do not enter music or novel modules yet.
* Do not start coding a new dialog or panel directly.
* If the next item is a new dialog, panel, or complex UI, first ask the user to create a Stitch / Product Design mockup.
* Keep all Codex descriptions in Chinese by default.
* Preserve necessary English terms only when useful.

Needs real-device confirmation: no

---

### TODO-008 Video subtitle time sync feasibility review

Status: doing

Goal:
Review the real feasibility of subtitle time synchronization before implementation.

Scope:

* Check current subtitle sources and rendering path.
* Check whether Media3 subtitle offset is supported in the current architecture.
* Check whether external subtitle parsing can safely support offset.
* Identify the smallest safe implementation path.
* Identify risks and unsupported cases.
* Do not create fake sliders or fake success.
* Do not implement UI in this task.
* Do not modify app code in this task.

Rules:

* Do not enter music or novel modules.
* Do not implement a subtitle sync panel yet.
* If a new subtitle sync panel is needed later, ask the user to create a Stitch / Product Design mockup first.
* Use Chinese for future Codex task descriptions by default.
* Preserve necessary English technical terms only when useful.

Allowed files:

* docs/DEVELOPMENT_PITFALLS.md
* docs/PROJECT_ROADMAP.md
* docs/MODULE_TECH_STACK.md
* .workflow/queue.md
* .workflow/state.json

Forbidden files:

* app/**
* gradle/**
* build.gradle
* settings.gradle
* AndroidManifest.xml

Needs real-device confirmation: no
