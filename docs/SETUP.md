# AegisInput — Setup Guide

## Prerequisites

| Requirement | Version |
|---|---|
| Android Studio | Ladybug (2024.2+) or later |
| JDK | 17+ |
| Android SDK | API 35 |
| Android NDK | Latest stable (e.g., 27.x) |
| CMake | 3.22.1 (install via SDK Manager) |

## Clone & Open

1. Clone this repository:
   ```bash
   git clone https://github.com/cmwen/aegis-input.git
   cd aegis-input
   ```
2. Open the project in Android Studio.
3. Wait for Gradle sync to complete. The first sync downloads dependencies and may take a few minutes.

## NDK Setup

1. In Android Studio, open **SDK Manager** → **SDK Tools** tab.
2. Check **NDK (Side by side)** — install the latest version.
3. Check **CMake** — install version **3.22.1**.
4. The project's `build.gradle.kts` references these automatically via the version catalog. No manual path configuration is required.

## Librime Integration

The `engine-rime` module is pre-configured for Librime but ships with **stub JNI** code so you can develop and test without building the native library.

### Option A: Develop without Librime (stub mode)

No additional setup is needed. The stub JNI layer (`rime_jni_stub.cpp`) echoes input keystrokes as candidates, allowing you to:

- Work on the Compose keyboard UI
- Test the fuzzy matching pipeline
- Develop and iterate on the candidate bar and hitbox logic

This is the recommended mode for most UI and architecture work.

### Option B: Integrate Librime

Follow these steps to enable full RIME engine support:

1. **Build librime for Android (ARM64)**:
   ```bash
   git clone https://github.com/rime/librime.git
   cd librime
   # Follow librime's Android build instructions
   # Output: librime.so and header files
   ```

2. **Place the built library**:
   ```
   engine-rime/src/main/jniLibs/arm64-v8a/librime.so
   ```

3. **Update the CMake configuration** in `engine-rime/src/main/cpp/CMakeLists.txt`:
   - Uncomment the `RIME_ROOT`, `target_link_libraries`, and `target_include_directories` lines.
   - Set the correct path to your librime build output.

4. **Add RIME schema files** to the app assets:
   ```
   app/src/main/assets/rime/
   ├── default.yaml
   ├── bopomofo.schema.yaml
   └── luna_pinyin.schema.yaml
   ```
   You can obtain these from the [rime/rime-bopomofo](https://github.com/rime/rime-bopomofo) and [rime/rime-luna-pinyin](https://github.com/rime/rime-luna-pinyin) repositories.

## Build & Run

```bash
# Debug build
./gradlew :app:assembleDebug

# Install on connected device/emulator
./gradlew :app:installDebug

# Run all unit tests
./gradlew test

# Run connected (instrumented) tests
./gradlew connectedAndroidTest
```

## Enable the IME

After installing the APK on your device:

1. Go to **Settings → System → Languages & Input → On-screen keyboard**.
2. Enable **AegisInput**.
3. Open any text field and switch to AegisInput using the keyboard switcher in the navigation bar.

## Development Tips

- **Compose Preview** — The keyboard UI components include `@Preview` annotations. Use Android Studio's Compose Preview to iterate on layout and theming without deploying to a device.

- **JNI Debug Logs** — View native bridge logs with:
  ```bash
  adb logcat -s AegisInput-JNI
  ```

- **Fuzzy Matcher Logs** — View fuzzy matching decisions:
  ```bash
  adb logcat -s AegisInput-Fuzzy
  ```

- **User Dictionary** — The Room database is stored at the app's internal database path. You can inspect it with Android Studio's Database Inspector while the app is running.

- **Hot Reload** — Compose UI changes support Live Edit in Android Studio for rapid iteration on keyboard layout and styling.

- **Schema Testing** — Place custom `.schema.yaml` files in `app/src/main/assets/rime/` to test different input methods without modifying code.
