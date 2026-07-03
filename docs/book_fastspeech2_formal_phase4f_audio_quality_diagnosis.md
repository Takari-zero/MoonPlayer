# Phase 4F Audio Quality Diagnosis Plan

## Goal

Phase 4F diagnoses why the formal FastSpeech2 reader path can produce audible but unclear speech while the earlier ChineseTtsTflite demo / prototype sounded acceptable.

This phase must not change synthesis quality heuristics blindly. It should first prove whether the problem is text source, PCM conversion, sample rate, AudioTrack playback, or model quality.

## Current Known State

- Phase 4E auto advance smoke passed in the debug-only formal path.
- Formal reader can produce audio through FastSpeech2 + MB-MelGAN + BookReaderAudioTrackSink.
- User reports the formal reader audio is hard to understand.
- Earlier demo / prototype audio was subjectively acceptable, so the formal audio path must be compared against the known-good path before assuming the model is unsuitable.

## Diagnostic Questions

1. Is the formal path synthesizing the exact intended text?
2. Is the formal path using the same text normalization and frontend behavior as the earlier demo/prototype?
3. Is the generated PCM numerically healthy?
4. Is float-to-PCM16 conversion correct?
5. Is the playback sample rate correct for the MB-MelGAN output?
6. Is AudioTrack writing the same bytes that were generated?
7. Is the audio being clipped, too quiet, sped up, slowed down, truncated, or distorted?

## Required Test Sentences

Use fixed known sentences before testing novel text:

- Short baseline: 这是一段离线语音试听。
- Medium baseline: 他缓缓抬起头，看向远处翻滚的云海，心中忽然有了答案。
- Reader sample: one exact sentence copied from the currently visible novel paragraph.

## Instrumentation Plan

Add a debug-only audio quality smoke path that:

1. Synthesizes the fixed test sentences through the formal FastSpeech2 engine.
2. Writes the generated PCM to WAV files under the app files directory.
3. Plays the same PCM through BookReaderAudioTrackSink.
4. Logs the exact text input used by FastSpeech2.
5. Logs sampleRate, channel count, PCM byte count, durationMs, and RTF.
6. Logs PCM statistics:
   - min sample
   - max sample
   - RMS
   - peak ratio
   - clipped sample count
   - zero sample ratio
7. Logs whether the source was reader_sentence, debug_baseline, or fallback.

## Comparison Plan

Compare formal output against the earlier known-good route:

1. Use the same sentence: 这是一段离线语音试听。
2. Generate formal WAV from the formal engine path.
3. If available, generate or reuse ChineseTtsTflite demo/prototype WAV for the same sentence.
4. Compare by listening and by PCM stats.
5. If formal WAV itself is unclear, investigate engine conversion/frontend.
6. If formal WAV is clear but AudioTrack playback is unclear, investigate AudioTrack sample rate/write/drain/routing.

## Likely Root Causes To Verify

- Wrong sample rate between MB-MelGAN output and AudioTrack playback.
- Incorrect float-to-PCM16 scaling, clipping, or little-endian byte order.
- Excessive clipping or extremely low RMS volume.
- Text source mismatch: preview/placeholder/chapter title instead of full sentence.
- Text normalization mismatch versus ChineseTtsTflite demo.
- Clause splitting removing punctuation or producing overly short fragments.
- Missing pause/silence between clauses.
- AudioTrack playback truncation or premature release.

## Non-Goals

Do not do these in Phase 4F before diagnostics prove the cause:

- Do not change model files.
- Do not switch engines.
- Do not tune volume blindly.
- Do not rewrite the reader state machine.
- Do not change mini/recent/chapter logic.
- Do not commit model or AAR files.

## Acceptance Criteria

Phase 4F diagnosis is complete when it can answer:

1. What exact text was synthesized?
2. Is formal WAV output clear or unclear?
3. Are PCM stats healthy?
4. Does AudioTrack playback match the WAV file?
5. Is the sample rate correct?
6. Is the issue text source, PCM conversion, playback, or model quality?

Only after this evidence should an audio-quality fix be implemented.
## Phase 4F Run Result

### Code Evidence

- Formal FastSpeech2 engine declares `sampleRate = 24000`.
- Formal AudioTrack sink uses the sample rate passed by the engine result.
- PCM format is mono PCM 16-bit little-endian.
- Float samples are converted with `sample * volume`, clamped to `[-1, 1]`, then written as little-endian signed 16-bit PCM.
- MB-MelGAN output is read from a native-order float buffer.
- Text splitter uses Chinese and ASCII punctuation delimiters, then concatenates clause PCM without inserting extra silence.

### Generated Files

Local output directory:

```text
E:\gitType\LocalVibe-fastspeech2-formal\test-results\phase4f_audio_quality\tts_quality\
```

Generated WAV files:

```text
baseline_01_engine_rate.wav
baseline_01_22050.wav
baseline_01_24000.wav
baseline_02_engine_rate.wav
baseline_02_22050.wav
baseline_02_24000.wav
baseline_03_engine_rate.wav
baseline_03_22050.wav
baseline_03_24000.wav
```

Generated report:

```text
E:\gitType\LocalVibe-fastspeech2-formal\test-results\phase4f_audio_quality\tts_quality\phase4f_audio_quality_report.json
```

### Test Texts

- baseline_01: 这是一段离线语音试听。
- baseline_02: 许青观察之下，心底有了判断。
- baseline_03: 他们不是修士，但出手的狠辣与时机的把握，还有那关键时刻似不在乎死亡的态度，使他心底有了判断。

### PCM Stats Summary

| Case | Rate | Duration | Peak | RMS dB | Zero Ratio | Clipping |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| baseline_01 | 24000 | 1150 ms | 13254 | -19.62 dB | 0.00348 | 0 |
| baseline_02 | 24000 | 1475 ms | 14576 | -19.80 dB | 0.00627 | 0 |
| baseline_03 | 24000 | 4900 ms | 16339 | -19.42 dB | 0.00938 | 0 |

The same PCM was also exported with 22050 Hz and 24000 Hz WAV headers for sample-rate listening comparison.

### Playback Smoke Result

- Formal sink playback start: 3
- Formal sink playback done: 3
- Reference playback start: 3
- Reference playback done: 3
- AudioTrackSink create: 3
- AudioTrackSink drain done: 3
- AudioTrackSink playPcm return: 3
- `bufferCapacity=4`: 0
- TensorFlowLite buffer error: 0
- FATAL / AndroidRuntime crash: 0
- System TTS / voice package page: 0
- ANR string matches were from Bluetooth/system scan logs, not the formal package process.

### Initial Diagnosis

Current evidence does not point to a broken PCM16 conversion, severe clipping, all-zero audio, AudioTrack write failure, or premature `playPcm` return.

The most likely remaining causes are:

1. Sample-rate perception: compare `*_22050.wav` and `*_24000.wav` by ear.
2. Model/frontend/prosody: the engine removes punctuation during splitting and concatenates clauses without inserted pause.
3. Model voice quality for novel text: if exported WAV is also unclear, the issue is upstream of AudioTrack.
4. Playback route/device processing: if WAV is clear but in-app playback is unclear, inspect device route, Bluetooth processing, and AudioTrack sink.

### Next Minimal Fix Recommendation

Before changing production audio code, listen to one exported pair:

```text
baseline_01_22050.wav
baseline_01_24000.wav
```

If 22050 Hz is clearly more natural, adjust the FastSpeech2 sample-rate contract and AudioTrack sample rate. If both exported WAV files are unclear, focus on frontend/text splitting or the model path. If WAV files are clear but in-app playback is unclear, focus on AudioTrack/device route.

## Phase 4F.1 Root Cause Deep Dive Result

### User Listening Result Before 4F.1

The user listened to:

```text
baseline_01_22050.wav
baseline_01_24000.wav
```

Both were unclear. This rules out a simple WAV header/sample-rate mismatch as the only cause and also makes an AudioTrack-only issue unlikely because the exported WAV itself was unclear.

### Formal vs Prototype Comparison

The formal and benchmark/prototype model files are byte-identical:

| File | SHA-256 |
| --- | --- |
| `baker_mapper.json` | `19cb8d746c1d2c5b7269117cd6832064e44a1e0ebdd7e3fbc86c19e1de535a7e` |
| `fastspeech2_quan.tflite` | `0d6379be824733ecf0c77c344e7e4b7b9e33f210f76e6a55492795c4b48bb1ee` |
| `mb_melgan.tflite` | `79febcd11e9053f7932c9bc024a399a1933467b437bd595f698e452acb41a7c1` |

The important difference found was mapper loading:

- Benchmark/prototype `ZhProcessor` opens `tts-prototype/chinese-tts-tflite/baker_mapper.json`.
- Formal `ZhProcessor(Context, mapperAssetPath)` accepted a mapper path but ignored it and tried to open root `baker_mapper.json`.
- Formal assets only contain `fastspeech2/baker_mapper.json`, not root `baker_mapper.json`.
- When mapper loading fails, `SYMBOL_TO_ID` is empty, so most symbols fall back to unknown id `2`, producing unintelligible speech even though the model and vocoder are correct.

Phase 4F.1 fixes this root cause by making `ZhProcessor` open the provided `mapperAssetPath`.

### PCM / Float Verification

The current formal path does not treat Float32 raw bytes as PCM16:

- MB-MelGAN output type: `FloatArray/FLOAT32`.
- Formal PCM conversion: clamp float to `[-1, 1]`, multiply by `Short.MAX_VALUE`, write signed PCM16 little-endian.
- `formalUsesFloat32RawBytesAsPcm16 = false` in `phase4f1_audio_root_cause_report.json`.
- Diagnostic raw-Float-as-PCM output was generated separately as a deliberately bad comparison file.

Baseline 01 after mapper fix:

| Metric | Formal wrapper | Direct reference |
| --- | ---: | ---: |
| duration | 1800 ms | 2075 ms |
| RMS dB | -22.10 dB | -22.55 dB |
| clipping | 0 | 0 |
| zero ratio | 0.00306 | 0.00829 |

The diagnostic raw Float32 bytes interpreted as PCM16 shows very different stats (`rmsDb` about `-5.35 dB`, length doubled), confirming why that bug would sound badly distorted and also confirming the formal path is not doing that.

### Frontend Verification

Baseline 01 direct reference data:

```text
text: 这是一段离线语音试听。
normalizedText: 这是一段离线语音试听#3 
pinyinSequencePreview: zhe4 shi4 yi1 duan4 li2 xian4 yu3 yin1 shi4 ting1 #3
idSequenceLength: 32
melShape: [1, 166, 80]
vocoderOutputShape: [49800]
```

Baseline 02 direct reference data:

```text
text: 许青观察之下，心底有了判断。
normalizedText: 许青观察之下#3 心底有了判断#3 
idSequenceLength: 38
melShape: [1, 234, 80]
vocoderOutputShape: [70200]
```

There is no `chapterIndex=-1`, no missing sentence text, and no TensorFlowLite buffer error in the generated report.

### Generated Phase 4F.1 Files

Local output directory:

```text
E:\gitType\LocalVibe-fastspeech2-formal\test-results\phase4f1_audio_root_cause\tts_quality\
```

Required output files generated:

```text
formal_current_pcm16.wav
formal_float_converted_pcm16.wav
formal_raw_bytes_interpreted_as_pcm16.wav
phase4f1_audio_root_cause_report.json
ab_formal_baseline_01.wav
ab_reference_baseline_01.wav
ab_formal_baseline_02.wav
ab_reference_baseline_02.wav
```

Additional baseline 03 A/B files were also generated.

### Verification

- `:app:testDebugUnitTest`: passed.
- `:app:assembleDebug`: passed.
- APK installed as `com.shenghui.localvibe.fastspeech2formal` without replacing `com.shenghui.localvibe`.
- Diagnostic Activity generated and pulled 21 files.
- `bufferCapacity=4`: not observed in report.
- `Cannot copy from a TensorFlowLite tensor`: not observed in report.
- Voice package / system TTS page: not involved in this diagnosis path.
- Formal package FATAL / AndroidRuntime crash: not observed. System logcat included unrelated Bluetooth/network/system noise.

### Most Likely Root Cause

The primary root cause is mapper loading in formal `ZhProcessor`: it ignored the configured asset path and looked for a root-level `baker_mapper.json` that does not exist in the formal asset layout. That causes text frontend ids to degrade to unknown-token-heavy input, which matches the symptom of audio being audible but unintelligible.

### Next Minimal Check

Ask the user to listen only to these two files, not the full app:

```text
E:\gitType\LocalVibe-fastspeech2-formal\test-results\phase4f1_audio_root_cause\tts_quality\ab_formal_baseline_01.wav
E:\gitType\LocalVibe-fastspeech2-formal\test-results\phase4f1_audio_root_cause\tts_quality\ab_reference_baseline_01.wav
```

If these are now intelligible, proceed to rerun the formal reader path with the mapper fix. If `ab_reference_baseline_01.wav` is intelligible but `ab_formal_baseline_01.wav` is weaker, investigate clause splitting and punctuation pause preservation next. If both remain unclear, inspect model frontend compatibility beyond mapper loading.

## Phase 4F.2 Mapper Fix Validation Result

### Scope

Phase 4F.2 landed the mapper-path fix as production code, not debug-only code, then reran after-mapper A/B diagnosis and auto-advance regression.

No volume change, AudioTrack tuning, model replacement, benchmark worktree modification, commit, or push was done.

### Mapper Fix

`ZhProcessor(Context, mapperAssetPath)` now opens the provided `mapperAssetPath` instead of the root-level `baker_mapper.json`.

The mapper loader now fails fast:

- logs `loading mapper assetPath=...`
- logs `mapper loaded assetPath=... mapperSize=... pinyinCount=... symbol count=...`
- throws `IllegalStateException` if mapper loading fails
- does not silently continue with empty `PINYIN_DICT` / `SYMBOL_TO_ID`

Observed runtime mapper log:

```text
mapper loaded assetPath=fastspeech2/baker_mapper.json mapperSize=14252 pinyinCount=417 symbol count=219
```

### Punctuation / Clause Compatibility

`FastSpeech2TextSplitter` now keeps delimiter characters at the end of each clause before calling `ZhProcessor`, so Chinese punctuation can still become `#3` pause markers. This is a minimal compatibility fix for prosody and does not change displayed novel text.

### After-Mapper Output

Local output directory:

```text
E:\gitType\LocalVibe-fastspeech2-formal\test-results\phase4f2_mapper_fix_validation\tts_quality\
```

Generated after-mapper files:

```text
after_mapper_formal_baseline_01.wav
after_mapper_reference_baseline_01.wav
after_mapper_formal_baseline_02.wav
after_mapper_reference_baseline_02.wav
after_mapper_formal_baseline_03.wav
after_mapper_reference_baseline_03.wav
phase4f2_mapper_fix_validation_report.json
```

### Model / Mapper Identity

Formal and reference use the same mapper/model assets:

| File | SHA-256 |
| --- | --- |
| `fastspeech2/baker_mapper.json` | `19cb8d746c1d2c5b7269117cd6832064e44a1e0ebdd7e3fbc86c19e1de535a7e` |
| `fastspeech2/fastspeech2_quan.tflite` | `0d6379be824733ecf0c77c344e7e4b7b9e33f210f76e6a55492795c4b48bb1ee` |
| `fastspeech2/mb_melgan.tflite` | `79febcd11e9053f7932c9bc024a399a1933467b437bd595f698e452acb41a7c1` |

### Text / ID Sequence Result

| Case | Formal normalized text | Reference normalized text | ID sequence equal |
| --- | --- | --- | --- |
| baseline_01 | `这是一段离线语音试听#3 ` | `这是一段离线语音试听#3 ` | true |
| baseline_02 | `许青观察之下#3 |心底有了判断#3 ` | `许青观察之下#3 心底有了判断#3 ` | false |
| baseline_03 | split by clauses with `#3` retained | full sentence with `#3` retained | false |

Interpretation:

- Short single-clause baseline is now fully aligned with reference.
- Medium/long text still differs because formal intentionally splits into clauses; this is now a prosody/segmentation difference, not a mapper corruption issue.
- This is acceptable for the mapper fix phase. If the user still hears weaker quality after mapper fix, the next narrow target is clause boundary pause / full-sentence prosody, not AudioTrack or volume.

### Auto Advance Regression

Phase 4F.2 reran `BookReaderAutoAdvanceSmokeTestActivity` after the mapper and punctuation changes.

Observed result from `phase4f2_auto_ringbuffer_dump.txt`:

```text
pause gate passed: 1
resume gate passed: 1
sentence done: 5
complete=true count=5
write timeout: 0
bufferCapacity=4: 0
Cannot copy from a TensorFlowLite tensor: 0
FATAL EXCEPTION: 0
AndroidRuntime: 0
语音包: 0
系统 TTS: 0
```

### Verification

- `:app:testDebugUnitTest`: passed.
- `:app:assembleDebug`: passed.
- APK installed successfully as `com.shenghui.localvibe.fastspeech2formal`.
- Main app package `com.shenghui.localvibe` remained installed and was not replaced.
- After-mapper A/B files were generated and pulled locally.
- Auto-advance smoke still completed 5 sentences.

### Next Minimal User Check

Only one final manual check is needed now:

1. Listen to `after_mapper_formal_baseline_01.wav`.
2. Listen to `after_mapper_reference_baseline_01.wav`.
3. Open `Moon播放器 Formal`, enter reader, tap one visible sentence, then play.
4. Confirm whether speech is now understandable.
5. Confirm it continues to the next sentence.
6. Confirm no voice-package settings page, no crash, no black screen.

If baseline files are clear but reader remains unclear, inspect reader text source and clause boundaries. If baseline and reader are both clear, proceed to the next formal integration phase. If baseline remains unclear, investigate frontend/model compatibility beyond mapper loading.
