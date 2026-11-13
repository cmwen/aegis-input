# Build and use AegisInput

Install JDK 17, Python 3.12+, Gradle 8.9 (or compatible 8.x), Android SDK/build-tools 35, NDK `28.2.13676358` and CMake `3.22.1`. Set `ANDROID_HOME`. No Gradle wrapper is included.

```sh
gradle --stacktrace :engine-rime:testDebugUnitTest :ui-compose:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
bash engine-rime/src/test/cpp/run-tests.sh
```

Gradle's native configure tasks bootstrap pinned/checksummed upstream sources using `scripts/native/bootstrap-rime.py`. Source archives are cached under `.native-downloads/`; extracted sources under `engine-rime/src/main/cpp/vendor/`; neither is committed. A clean CI build follows the same path. The host test script accepts `CMAKE_BIN` or uses Android SDK's CMake; the native host decoder tests use a temporary user directory and real bundled schemas/dictionaries.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Enable/select the installed IME from its setup screen, then tap its real text field. Choose 拼音, type `nihao`, then space or candidate `你好`. Choose 注音 and enter `ㄋㄧˇㄏㄠˇ`; select `你好`. Both languages support fuzzy pronunciations and learn selected words locally. English remains available while first-start dictionaries are being prepared. Failures appear in the keyboard's status line.

For real Android JNI/database acceptance checks:

```sh
gradle :app:connectedDebugAndroidTest
```

These tests cover Pinyin `nihao`, fuzzy `zongwen`, Zhuyin `ㄋㄧˇㄏㄠˇ`, fuzzy `ㄗㄨㄥˉㄨㄣˊ`, candidate selection/reset and multiple Room words for one code. Physical UI verification should additionally type English, switch languages, select/scroll candidates, delete text and check a password field.

Debug builds use Android's debug keystore. Existing GitHub Actions release signing/publication remains in place. Local builds do not publish a release.

## First-start timing

On the tested Lenovo TB-8505F (Android 10, ARM64), the five real JNI/database acceptance tests including first-time essay/dictionary deployment completed in about 97 seconds. This is the whole test-suite duration, not an isolated initialization measurement. Initial dictionary preparation can take a minute or more on slower devices; the keyboard shows its preparation status and English remains available. Compiled data persists for later launches.
