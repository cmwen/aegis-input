# Build and use AegisInput

## Requirements

- JDK 17
- Gradle 8.9 (CI), or a compatible Gradle 8.x installation
- Android SDK platform and build-tools 35
- Android NDK `28.2.13676358`
- CMake `3.22.1`

Set `ANDROID_HOME` to the installed Android SDK. The project does not include a Gradle wrapper. The offline Pinyin dictionary is already bundled; no Librime build or downloaded runtime assets are required.

## Validation

```sh
gradle --stacktrace :engine-rime:testDebugUnitTest :ui-compose:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
bash engine-rime/src/test/cpp/run-tests.sh
```

The native acceptance tests use the actual bundled lexicon and test Chinese phrases, composed multi-word input, apostrophe boundaries, editing, selection, reset, and load failure. The JVM tests cover layout and editor behavior. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## On a device

1. Install the debug APK and open AegisInput.
2. Tap **Enable keyboard** and enable AegisInput in Android settings.
3. Tap **Choose AegisInput** and select it in the system picker.
4. Tap the actual text field on the setup screen to test the installed IME.
5. Choose **中** for Chinese Pinyin, type `nihao`, and choose `你好` or press space. Choose **EN** for English.

The Chinese keyboard uses standard QWERTY, tone-free Pinyin, `v` for ü, and `'` for explicit syllable boundaries. Tap a candidate to commit it; swipe the candidate strip to see more. Enter commits the Roman composing text without sending the editor action until composition is empty. Switching languages preserves Roman composing text. Password fields use English and cannot activate Chinese composition.

Only Pinyin Chinese input is supported in this version. The previous Zhuyin preview and mock engine have been removed.

## Signing and releases

Debug builds use Android's debug keystore. Release signing and publication remain configured in the existing GitHub Actions release workflow; rebuilding locally does not publish a release.
