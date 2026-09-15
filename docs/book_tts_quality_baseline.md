# Book TTS Quality And Performance Baseline

## Scope

This document separates product speech speed from TTS synthesis performance and records the current LocalVibe book-listening baseline. It is not a real-device acceptance report. Voice quality, prosody, and pronunciation still require human listening on device.

## Speed Terms

- Playback speed: the speed the user hears, such as `0.75x`, `1.0x`, `1.25x`, `1.5x`, `1.75x`, or `2.0x`.
- Synthesis performance: how fast the TTS engine generates audio.
- First audio latency: time from play request to the first playable PCM chunk.
- Real time factor: `synthesisDurationMs / generatedAudioDurationMs`. Values below `1.0` are better for continuous playback.

## Current Speech Rate Support

- UI: the book reader already has a speech-rate slider. Current slider range is `0.6x..1.8x`.
- Persistence: the book speech rate is screen-local state today. It is not persisted in DataStore or book settings.
- Android System TTS: `BookTtsController.speakSentence()` calls `TextToSpeech.setSpeechRate()` and clamps to `0.5x..2.0x`.
- Aishell3/Sherpa: `Aishell3SegmentedStreamingTtsEngine` passes `StreamingTtsParams.speed` into `OfflineTts.generate(speed=...)` and clamps to `0.5x..2.0x`.
- BuiltInOffline: the current preview path calls `OfflineTts.generate(..., speed = 1.0f)`. It does not yet use the UI speech-rate value.
- PCM playback: `StreamingPcmAudioPlayer` does not use `PlaybackParams` and does not change sample rate to fake speech speed.

## Sherpa API Findings

The checked wrapper exposes both:

- `OfflineTtsVitsModelConfig.lengthScale`
- `OfflineTts.generate(speed = ...)`

Current LocalVibe code uses `generate(speed = ...)`; it does not set `lengthScale`. The exact relationship between `lengthScale` and audible speed should not be assumed without a separate native/Sherpa validation task.

## Text Processing Chain

- Original text is read by `TxtBookReader`.
- TXT bytes are decoded as strict UTF-8 first, then GB18030 fallback.
- Newlines are normalized.
- Paragraphs are split by newline boundaries and long paragraphs are chunked around punctuation.
- Reader sentences are built in `BookListenScreen.kt` and are also used for highlight/progress mapping.
- Aishell3 additionally splits each displayed sentence into short synthesis segments before calling Sherpa.
- There is no project-level Chinese text normalizer for numbers, dates, English, or heteronyms.
- Sherpa model assets include lexicon and rule FST files, but their pronunciation coverage has not been human-validated in this project.

## Accuracy Baseline Categories

The JVM test corpus intentionally uses handwritten text, not copyrighted book content. It covers:

- Plain Chinese.
- Heteronyms: examples around `银行 / 行走`, `重庆 / 重要`, `音乐 / 快乐`, `长大`.
- Numbers: `123`, `2026`, `3.14`, `50%`.
- Date and time: `2026年9月16日`, `上午8点30分`.
- Chinese plus English: `LocalVibe`, `Android Media3`.
- Punctuation: comma, period, question mark, exclamation mark, colon.
- Long sentence: one 80 to 150 Chinese-character sample.

These corpus tests only ensure the baseline exists. They do not prove pronunciation or prosody quality.

## Current Performance Observability

- Aishell3 logs initialization cost, segment synthesis cost, segment audio duration, first audio start cost, and queue/write lifecycle.
- Prepared audio cache and segment cache already exist.
- Current sentence and next sentence segment prewarm already exist in the reader path.
- First audio latency can be approximated from existing logs, but there is no dedicated benchmark framework yet.
- RTF can be computed from synthesis duration and generated audio duration, but it is not yet emitted as a single structured metric in runtime logs.

## Boundaries

- Do not fake speed by changing PCM sample-rate metadata.
- Do not claim prosody or pronunciation PASS without human listening on a real device.
- Do not introduce network NLP, model changes, or large dictionaries as part of this baseline.
- Keep provider-specific details out of `BookListenScreen` in future refactors.
