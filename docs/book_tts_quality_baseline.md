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
- Android System TTS: `BookTtsController.speakSentence()` maps user speed through `BookSpeechRate` and calls `TextToSpeech.setSpeechRate()`.
- Aishell3/Sherpa: `Aishell3SegmentedStreamingTtsEngine` maps `StreamingTtsParams.speed` through `BookSpeechRate` and passes it into `OfflineTts.generate(speed=...)`.
- BuiltInOffline: `BuiltInOfflineTtsEngine.speak()` accepts `BookSpeechRate` and passes its Sherpa speed into `OfflineTts.generate(speed=...)`.
- PCM playback: `StreamingPcmAudioPlayer` does not use `PlaybackParams` and does not change sample rate to fake speech speed.

## Sherpa API Findings

The checked wrapper exposes both:

- `OfflineTtsVitsModelConfig.lengthScale`
- `OfflineTts.generate(speed = ...)`

Current LocalVibe code uses `generate(speed = ...)`; it does not set `lengthScale`. The exact relationship between `lengthScale` and audible speed should not be assumed without a separate native/Sherpa validation task.

`BookSpeechRate` is the current product-level speed model. Provider-specific mappings are:

- System TTS: user multiplier maps directly to `TextToSpeech.setSpeechRate()`.
- Sherpa/Aishell3/BuiltInOffline: user multiplier maps directly to `OfflineTts.generate(speed=...)`.
- PCM playback: remains `1.0x`; no sample-rate, frame-dropping, or `AudioTrack PlaybackParams` speed hack is used.

Changing speed during playback updates the state used by future synthesis requests and cache keys. Already queued PCM is not time-stretched or restarted automatically.

## Text Processing Chain

- Original text is read by `TxtBookReader`.
- TXT bytes are decoded as strict UTF-8 first, then GB18030 fallback.
- Newlines are normalized.
- Paragraphs are split by newline boundaries and long paragraphs are chunked around punctuation.
- Reader sentences are built in `BookListenScreen.kt` and are also used for highlight/progress mapping.
- The displayed text remains the original reader sentence. It is not rewritten for highlight, progress, sentence indices, or UI display.
- `BookTtsTextNormalizer` produces provider-facing `spokenText` from the original sentence.
- The normalizer is intentionally low risk: it removes BOM/zero-width/invisible control characters, collapses whitespace, and shortens only long runs of the same punctuation mark.
- The normalizer does not rewrite numbers, dates, English, heteronyms, or Chinese words.
- Android System TTS, BuiltInOffline, and Aishell3/Sherpa all receive the normalized spoken text boundary.
- Aishell3 additionally splits normalized spoken text into short synthesis segments before calling Sherpa.
- Sherpa model assets include lexicon and rule FST files, but their pronunciation coverage has not been human-validated in this project.

## Rule FST Configuration

- Aishell3/Sherpa loads `phone.fst`, `date.fst`, `number.fst`, and `new_heteronym.fst` through `OfflineTtsConfig.ruleFsts`.
- BuiltInOffline requires/copies `phone.fst`, `number.fst`, `date.fst`, and `new_heteronym.fst`.
- BuiltInOffline currently configures `OfflineTtsConfig.ruleFsts` with `phone.fst`, `date.fst`, and `number.fst`; `new_heteronym.fst` is present in the required file set but is not part of the configured rule FST list.
- No project-level hardcoded number, date, English, or heteronym rewrite table is currently applied before provider synthesis.

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

The normalizer tests additionally lock the current text boundary:

- Original text is preserved for display and highlight.
- Spoken text is deterministic and idempotent.
- Control characters, BOM, zero-width characters, repeated whitespace, and long same-punctuation runs are cleaned.
- Numbers, dates, decimals, percentages, and Chinese-English samples are not rewritten.

Human listening is still required for:

- Heteronym accuracy.
- Number/date naturalness.
- English and Chinese-English pronunciation.
- Prosody, pauses, and sentence continuity.
- Whether provider-specific FST behavior sounds acceptable on a real device.

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
