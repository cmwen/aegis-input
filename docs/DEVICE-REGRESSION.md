# Repeatable device regression

Run on an unlocked Android 8+ physical device or emulator with a software keyboard, JDK 17, Android SDK/NDK and Gradle from [setup](SETUP.md). Explicit serial selection prevents testing the wrong device:

```sh
scripts/test-device-e2e.sh --serial 192.168.1.13:5555
# Optional: GRADLE=/absolute/path/to/gradle, --artifacts /path/to/results
# After building: --skip-build
# Focus one host test: --class 'com.aegisinput.e2ehost.KeyboardRegressionTest#fourRotationsPreserveEditorAndAllowRefocus'
```

The command builds and installs four APKs, runs 5 existing packaged JNI/Room tests and 6 editor/IME tests, and returns nonzero for assertions, crashes, empty/incomplete instrumentation, builds, installs or failed restoration. Android's `am instrument` status alone is insufficient, so the runner checks its final nonempty JUnit success result. Failure evidence includes test stack traces, UI hierarchy and screenshots in `diagnostics.tar`, instrumentation output and filtered crash logs. Build logs are saved too. `--skip-build` requires all four previously built APKs. Wi-Fi ADB is recommended for large APK transfers here.

On the Lenovo TB-8505F (Android 10, ARM64), first deployment of the isolated JNI suite took 103 seconds; the complete warm six-case UI suite took about 212 seconds. Allow roughly four minutes for a warm command including installation, plus any build or cold dictionary deployment time. These timings are observations, not performance assertions.

The `e2e` app build type initializes from debug and changes only application ID/version suffix. It compiles the same production service, controller, Compose keys, JNI and schema assets. The separate package `com.aegisinput.app.e2e` owns all test preferences, dictionaries and Room data. The editor lives in `com.aegisinput.e2ehost`, which is also the instrumentation target, so killing the IME is a genuine process restart without killing the test runner. No test Activity or exported test API is added to the production app. The host reads its own real Editable/composing spans and editor actions; UI Automator taps visible IME accessibility nodes by package and key description/text. It never injects text into the editor to satisfy a typing assertion. Clearing between cases only empties the test host's editor.

The script saves and restores the original default IME, exact enabled-IME string, accelerometer rotation and user rotation on success, failure, SIGINT and SIGTERM. It verifies the restored settings. As with any shell trap, recovery cannot run after SIGKILL, device disconnection or power loss; original values remain in `original-settings.txt`. Test APKs/data remain installed for warm reruns; neither personal application data nor test learning is cleared. Learning assertions compare deltas and work on repeated runs. The test host is left in the foreground. Use a test device: rotation and keyboard selection affect the active display during execution.

## Manual rebuild coverage mapped to automation

| Previous manual check | Repeatable assertion |
| --- | --- |
| English `hello`, shift, punctuation, backspace, Enter | Actual target Editable equals `hello World.`, deletion and newline + subsequent typing |
| Pinyin `nihao` → `你好`, fuzzy `zongwen` → `中文` | Real keys, composing span, visible candidate selection, exact committed target text |
| Zhuyin `ㄋㄧˇㄏㄠˇ` → `你好`, fuzzy `ㄗㄨㄥˉㄨㄣˊ` → `中文` | Same path through actual IME/InputConnection; both candidate tap and space selection |
| Composition/backspace/schema switching | Active target composing spans, deletion changes text, switch commits raw composition and next schema converts |
| Portrait/landscape crash regression | Four alternating orientation changes while composing, retained target text, unchanged IME process PID, refocus/reopen and successful English typing after each; final Zhuyin commit |
| Room learning survives force-stop | Snapshot real private SQLite database + WAL, selected-word frequency increments, identical learned-word frequencies after confirmed process death |
| Zhuyin preference survives restart | On-disk preference XML unchanged, different new process PID, Zhuyin keys active without choosing schema, successful conversion |
| Password/editor behavior (previous device gap) | Saved Chinese preference overridden by password English, exact private target text, no composing span or Room learning; Search action callback receives ID 3 without newline |
| JNI/schema/database acceptance | Original 5 `RimeAcceptanceTest` cases also run in isolated e2e package; unit/host decoder tests remain complementary |

Remaining manual checks: visual colors/contrast, clipping and touch comfort across screen sizes, candidate-strip scrolling/long candidate discoverability, accessibility spoken feedback, alternate OEM/Android lifecycle behavior, and cold dictionary-deployment timing as a benchmark. The suite waits up to three minutes for engine readiness rather than asserting a device-specific startup speed. Candidate taps exercise the real pointer dispatcher at semantic node locations; they do not exhaust adaptive hitbox boundaries. Orientation tests permit Android to dismiss the IME and explicitly refocus, matching the observed target recreation behavior. These tests assert editor text and renewed input; preserving an unfinished native composing session across Activity recreation is not a claimed requirement.

`gradle :app:connectedDebugAndroidTest` still runs only the original JNI/Room tests against the regular debug app. It is not equivalent to this end-to-end command. The runner uses `-PaegisTestBuildType=e2e` when building that existing test source set for the isolated package. Normal debug/release builds and the required repository unit/build validation remain unchanged.

On Android 10, unchanged UiAutomation service info is reapplied before semantic lookups to invalidate stale accessibility nodes; the public setter clears this cache in [AOSP](https://android.googlesource.com/platform/frameworks/base/+/a1474266bf1d563cc9fbc42ca875a8fba2140ed9/core/java/android/app/UiAutomation.java). Typing waits for UI/editor idle, including tone keys that may normalize without changing visible preedit. Failure diagnostics are captured before rotation cleanup.

UI Automator cross-app selectors and bounded waits follow the [Android testing guide](https://developer.android.com/training/testing/other-components/ui-automator-legacy) and [UiDevice API](https://developer.android.com/reference/androidx/test/uiautomator/UiDevice). No new CI emulator job is required; the same explicit-serial command can target an emulator with a software keyboard and sufficient deployment time.

Verified on 2026-11-14: two consecutive complete runs passed all 5 JNI and 6 editor/IME tests (UI suite 211.5s and 209.8s), with exact setting restoration each time. A temporary return of the old cached ComposeView reproduced the original orientation crash; the PID assertion failed and the command returned 1. That temporary change was removed, and no production service, keyboard, controller, native or schema changes are included.
