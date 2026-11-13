# AegisInput — Privacy-First Android IME

An offline Android keyboard built with Kotlin, Material 3 / Jetpack Compose, and **real Librime through JNI**. English, Pinyin and Zhuyin (Bopomofo) work without accounts or network permission.

- **English:** QWERTY, shift, numbers, punctuation and editor actions.
- **Pinyin:** type `nihao` → `你好`; use `'` between syllables and `v` for ü.
- **Zhuyin:** a complete single-screen Bopomofo keyboard with all 37 symbols and five tone marks. Try `ㄋㄧˇㄏㄠˇ` → `你好`.
- **Fuzzy matching:** Rime spelling algebra handles zh/z, ch/c, sh/s, en/eng, an/ang, n/l and f/h in the corresponding input schemas.
- **Adaptive touch targets:** keys resolve touches in their surrounding gaps using distance and bounded local frequency weights.
- **Local learning:** Rime's user dictionary plus a Room database track selected candidates and boost frequent words. Multiple words for one pronunciation remain available.
- **Privacy:** no network permission, analytics or cloud backup. Password fields force English and do not compose or learn Chinese.

Choose **拼音 / 注音** above the keys to change Chinese modes. **EN** opens English; **中** returns to your selected Chinese mode. Tap or scroll candidates; space selects the first candidate. Enter preserves the visible composing text. Backspace edits composition first, then selected text or one Unicode code point.

## Build and test

Use JDK 17, Python 3.12+, Gradle 8.9 or compatible 8.x, Android SDK/build-tools 35, NDK `28.2.13676358` and CMake `3.22.1`. No wrapper is included.

```sh
gradle --stacktrace :engine-rime:testDebugUnitTest :ui-compose:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
bash engine-rime/src/test/cpp/run-tests.sh
```

The first native build downloads checksum-pinned upstream Librime/Boost/dependency sources. Gradle runs `scripts/native/bootstrap-rime.py` automatically. Downloads are needed **at build time only**; dictionaries and native code ship in the APK. APK: `app/build/outputs/apk/debug/app-debug.apk`. Android 8.0+; ARM64, ARMv7 and x86_64; 16KB native page alignment.

Android acceptance tests exercise the packaged JNI library, real Pinyin and Zhuyin conversion, fuzzy matching and Room learning:

```sh
gradle :app:connectedDebugAndroidTest
```

## Install

Install the APK, open AegisInput, enable it in Android settings and choose it from the keyboard picker. Tap the launcher's text field to test the actual IME. First startup prepares offline dictionaries in the background and can take a minute or more on slower devices; English remains available while it works.

The three modules are `app` (Android lifecycle/editor integration), `engine-rime` (Librime/JNI/fuzzy schemas/Room), and `ui-compose` (keyboard/candidates/touch resolution). See [architecture](docs/ARCHITECTURE.md), [setup](docs/SETUP.md) and [native provenance](engine-rime/OFFLINE-PINYIN.md).

App code is Apache-2.0. Third-party engines and dictionaries retain their own licenses; notices are bundled in `app/src/main/assets/notices` and beside the dictionaries.
