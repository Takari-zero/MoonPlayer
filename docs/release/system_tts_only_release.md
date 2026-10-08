# TIER0 System TTS-only Release baseline (P18d)

TIER0 guarantees the App's **System TTS integration**, provided the device has a usable engine and language data. It does not bundle a speech engine/model and does not promise that the device engine is network-free. AISHELL3 is **NOT_BUNDLED**; Matcha is **NOT_BUNDLED / EXPERIMENTAL**; iFlytek is **NOT_BUNDLED**.

P18b remains `MULTIPLE_LICENSE_BLOCKERS`: the private offline native/model/resource inventory is not cleared for distribution. This workflow does not resolve that audit or implement a voice pack/installer.

## Required clean source boundary

Never use `assembleRelease` directly in the developer checkout as the official workflow. Local excluded `app/src/main/jniLibs`, `assets/offline_tts`, `assets/iflytek`, benchmark models, and staging files can otherwise enter an APK despite a clean Git status. The script creates a new detached checkout of an immutable commit and copies **no ignored/untracked/private input**, including `local.properties`. Existing P18a/P18c audit helpers were untracked, machine-specific diagnostics, not a release script framework.

The build uses the installed SDK and shared Gradle/Maven/task caches; it is not an air-gapped or fresh-dependency-cache proof. `--build-cache` speeds repeated clean-checkout validation. Repeatability means the same pinned source and policy can be rebuilt safely; byte-for-byte APK reproducibility is not claimed.

## Build

Use PowerShell 7+, Git, JDK **17**, Android SDK platform 35 and stable build-tools including `aapt2`/`apksigner`. Resolve tools from parameters or environment, not a tracked developer-machine path.

```powershell
./scripts/build_system_tts_only_release.ps1 -Commit <full-commit-sha> -JavaHome <jdk17-directory> -AndroidSdk <sdk-directory>
```

`-Commit` defaults to HEAD only if the calling tracked worktree is clean. An explicit commit deliberately permits local changes while building only that commit. JAVA_HOME and ANDROID_HOME / ANDROID_SDK_ROOT are fallback inputs. `-BuildToolsVersion` optionally selects an installed stable tool version. `-OutputDirectory` must be a new Git-ignored directory within the caller repository; default is `test-results/p18d/<commit-prefix>-<UTC>-<random-id>`.

The command runs `:app:assembleRelease` plus a read-only AGP model audit, with temporary Gradle/Kotlin heaps **4g/4g**, one worker, no daemon reuse and no tracked `gradle.properties` changes. Success copies only the generated APK and metadata to the report directory, verifies the copied hash, and removes only the validated temporary checkout. After creating the run directory, failures record the remaining checkout/path in `failure-report.json` (a failed cleanup can leave a partially deleted, unregistered checkout); preflight failures create no checkout. Windows worktree add/remove use invocation-local `-c core.longpaths=true`, never change Git configuration, and never force-remove. No `git clean`, reset, stash or automatic Git commit/push is performed. Existing outputs are never overwritten.

## Hard payload gate

```powershell
./scripts/check_system_tts_only_apk.ps1 -ApkPath <apk> -ReportPath <optional-json-path>
```

Exit 0 requires PASS. Forbidden entries cause exit 1 / `FAIL_FORBIDDEN_PAYLOAD`, listing every matching entry. APK structure must include AndroidManifest.xml and classes.dex. Gate categories/counts: OFFLINE_ASSET, IFLYTEK_ASSET, SHERPA_NATIVE, OFFLINE_ORT, MATCHA_MODEL, and unique FORBIDDEN_ENTRY_COUNT (an entry can match multiple categories).

The policy covers complete `offline_tts` and `iflytek` directory segments, `bench_models`, known Matcha/Melo model directories, `model-steps-*.onnx`, Vocos ONNX/ORT names, Sherpa native names, ONNX Runtime native names, and the two known P18a iFlytek IRF filenames even if relocated. P18a's 26 AISHELL3/Melo asset entries and four private native libraries are covered. All ABI copies are rejected. **There is no global `.onnx`/`.so` ban**: unrelated lawful model payloads and AndroidX/Datastore native dependencies stay legal. Arbitrarily renamed/repackaged unknown private files cannot be identified by a filename policy alone; future resource additions require inventory/policy review.

## Outputs and signing

The ignored run directory contains the unsigned APK, `output-metadata.json`, `release-config.json`, `payload-gate.json`, manifest/badging/signature output, build log and `release-report.json`. Artifact identity is Git source commit + applicationId/version + relative APK name + size/SHA-256. Absolute local paths are operational locations, not release identity. Invocation-start tooling hashes, JDK/Gradle versions, AGP-resolved build-tools and separately selected inspection build-tools, timestamps and elapsed seconds are recorded.

Current baseline source: `829d1b4a95eff8f076065d6a1fb805f7895b52f8`. Namespace/applicationId `com.shenghui.localvibe`, version 1 / 1.0, minSdk 26, target/compileSdk 35. Release minify is explicitly false; resolved debuggable/shrinkResources/signingConfig are recorded from AGP rather than guessed. The TTS_SERVICE query must survive the actual release manifest. Permissions are checked against pinned source plus known merged dependency permissions; unexpected permissions fail for review.

This P18d workflow intentionally delivers **UNSIGNED_RELEASE**, not an installable candidate: no signingConfig, `*-unsigned.apk`, and apksigner rejects it for absence of signatures. It rejects debug signing, unexpectedly signed output and unrelated signing-tool failures. Never create/store passwords, keystores or signatures here. `SYSTEM_TTS_ONLY_RELEASE_BASELINE_READY=YES` does not imply `FINAL_SIGNED_RELEASE_READY=YES`.

## Runtime evidence and observations

P18c side-by-side clean debug smoke passed with observations: Xiaomi `com.xiaomi.mibrain.speech` actually spoke three targets, sessions 3→4→5 / targets 0→1→2, three completions, exactly two natural next dispatches, one正文 click, no duplicate/stale accepted observed. The user confirmed all three had sound. Offline options were unavailable/disabled; missing Sherpa probe was caught; smoke crash/FATAL/ANR were zero. Main App was not replaced or its data touched.

Limitations remain: second entry was HOT, not a second process cold start; external floating-window interference affected some UI observations; typed callback layers were partly cross-checked statically. Xiaomi attempted cloud then its own local engine, which is not LocalVibe's bundled/preview LOCAL playback. No adversarial stale callback was injected. Debug runtime evidence is **not** final signed release runtime approval, even with minify=false.

`SIGNED_RELEASE_RUNTIME_SMOKE_REQUIRED=YES`, `FINAL_SIGNED_RELEASE_READY=NO`, `OFFLINE_TTS_RELEASE_READY=NO`, `VOICE_PACK_REQUIRED_FOR_OFFLINE=YES`.

## Next boundary: P18e only

Recommended next stage: `RELEASE_SIGNING_AND_FINAL_INSTALLABLE_CANDIDATE`. Review signing/key custody, generate a signed candidate, then verify installation/upgrade/cold start/SystemTTS and package delivery. This script never installs an APK, covers neither installer nor voice packs, and does not open AISHELL3/Matcha runtime tests. Offline packs require independent provenance/license closure and a separately reviewed integration.
