# AegisInput

A private Android keyboard with the same familiar QWERTY layout for English and Chinese Pinyin. Typing works entirely offline; the app declares no network permission.

- **English:** direct text input, shift, punctuation, numbers, and editor actions.
- **Chinese:** tone-free Pinyin, weighted phrase candidates, multi-word composition, and explicit syllable boundaries with `'`. Use `v` for ü.
- **中 / EN:** switch languages without losing visible composing text.
- **Space:** choose the first Chinese candidate; **Enter:** keep the Pinyin letters. Tap or scroll candidates to choose another result.
- Password fields start in English and disable Chinese composition. Backspace removes composing letters first, then selected text or a Unicode code point.

The Chinese engine is an independent native dictionary decoder using the full Rime `pinyin-simp` lexicon. It is **not Librime** and does not provide Rime schemas, adaptive learning, cloud prediction, or Zhuyin. See the engine's dictionary provenance and license documentation. The earlier mock engine and misleading Zhuyin option have been removed.

## Build

Use JDK 17, Android SDK/build-tools 35, NDK `28.2.13676358`, CMake `3.22.1`, and Gradle 8.9 or compatible 8.x. No Gradle wrapper is included.

```sh
gradle --stacktrace :engine-rime:testDebugUnitTest :ui-compose:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
bash engine-rime/src/test/cpp/run-tests.sh
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Minimum Android version: 8.0 (API 26). Native builds cover ARM64, ARMv7, and x86_64.

## Try it

Install the APK and open AegisInput. Enable it in Android settings, choose it from the keyboard picker, then tap the setup screen's real text field. In Chinese mode, try `nihao` → `你好` and `zhongwen` → `中文`. In English mode, type normally.

## Modules

- `app`: Android input-method lifecycle, composing/commit behavior, setup and test field.
- `engine-rime`: offline native Pinyin decoder and bundled lexicon; the module name remains for compatibility.
- `ui-compose`: keyboard, key layouts, and scrollable candidates using Material 3.

See [setup](docs/SETUP.md), [architecture](docs/ARCHITECTURE.md), and the bundled third-party notices for implementation and attribution.

Licensed under Apache-2.0. Dictionary and third-party attribution are included separately.
