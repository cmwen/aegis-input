# AegisInput architecture

The Android IME is split into three modules:

- `app`: `AegisInputService` owns Android lifecycle and a session per editor; `TypingController` handles editing rules through small editor/composer interfaces, allowing JVM regression tests without an Android device. `InputConnectionWrapper` commits or replaces composition, deletes selected text or Unicode code points, and dispatches editor actions. The launcher provides enable/picker actions and a real Android text field for testing the actual IME.
- `ui-compose`: familiar QWERTY English/Pinyin layouts, numbers and punctuation, composition text, scrollable candidates, language switch, and keyboard controls. The candidate strip resets scroll position when results change. The keyboard supports dark/light themes and navigation-bar insets.
- `engine-rime`: the historical module and bridge names are retained, but its implementation is an independent native offline Pinyin decoder rather than Librime. It loads the bundled full weighted Rime Pinyin dictionary and assembles candidates across dictionary words. JNI converts strings between Java and Unicode native representations. Native host acceptance tests exercise the same decoder and actual dictionary used on Android.

## Editing behavior

Letters build Pinyin composition in Chinese mode. Candidate selection and space replace it with Chinese text. Enter and language changes preserve the visible Roman text. Punctuation selects the best candidate before inserting punctuation. Backspace edits the current composition before deleting committed text. Changes to editor selection clear internal composition so later keys cannot modify an old composing region. Each new editor starts with a fresh session; password fields force English and disable Chinese composition.

The app keeps no network permission, telemetry, or remote services. Suggestions are dictionary-based and local; adaptive user learning and cloud prediction are not implemented. Zhuyin is not advertised or available in the current input-method subtype list.

## Native packaging

CMake builds the decoder for `arm64-v8a`, `armeabi-v7a`, and `x86_64`, with 16KB ELF page alignment. NDK version and Android SDK requirements are pinned in module Gradle files. The dictionary is an application asset loaded at initialization; the APK includes all data needed for offline use.
