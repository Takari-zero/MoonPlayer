# FastSpeech2 Third-Party Notice Draft

## Scope

Phase 1 introduces only the FastSpeech2 engine boundary. It does not connect the engine to `BookListenScreen`, the novel-home mini console, reader UI, or any playback controller.

## Source Route

Parts of the implementation are based on the ChineseTtsTflite / TensorFlowTTS-style FastSpeech2 + MB-MelGAN Android route, including:

- Chinese text preprocessing / pinyin id mapping.
- FastSpeech2 TFLite inference wrapper.
- MB-MelGAN TFLite vocoder wrapper.

## License

ChineseTtsTflite-derived code is treated as Apache-2.0 for this phase.

Phase 1 does not make the final license audit conclusion. Before production release, LocalVibe must:

- Preserve source headers where available.
- Add Apache-2.0 attribution / NOTICE text.
- Document model origin and redistribution terms.
- Confirm whether bundled model files are redistributable in the target app distribution channel.

## Model And AAR Policy

Phase 1 does not commit model files or local AAR files.

Current known model size:

- FastSpeech2 + MB-MelGAN: about `22.72 MiB`.

Formal packaging still needs a decision for:

- Asset packaging versus downloaded/internal artifacts.
- Maven TensorFlow Lite dependencies versus local AARs.
- Git LFS or other large-file handling if models are checked in.
- APK size impact.