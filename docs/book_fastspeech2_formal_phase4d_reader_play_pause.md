# Phase 4D Formal Reader Play/Pause

## Result

Formal reader playback now uses the real `BookReaderPlaybackController` path in the debug package. The debug app can synthesize and play the current reader sentence through FastSpeech2 and `BookReaderAudioTrackSink`.

## Verified

- Formal reader play entered the real controller path.
- `sentenceText` is used as synthesis input.
- `sentencePreview` is only for UI/logging.
- The play button no longer resumes an empty selected target after `jumpToSentence(..., resumeIfPlaying=false)`.
- Logs showed `reader play decision action=play` for the guarded case.
- `build target text source=reader_sentence` used the real reader sentence.
- `FastSpeech2 synth start` and `synth done success=true` appeared.
- `AudioTrackSink write main=false` appeared.
- No `chapterIndex=-1` was observed in the verified path.
- No `bufferCapacity=4` or TensorFlowLite buffer copy error was observed.
- No voice-package settings jump was observed from the formal playback path.
- No formal package ANR/FATAL/crash was observed.

## Remaining At End Of Phase 4D

- Phase 4D only played one sentence.
- Auto advance was not connected until Phase 4E.
- Voice quality/listening clarity remains a separate risk and is not addressed in Phase 4D.
