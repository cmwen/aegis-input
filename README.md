# AegisInput — Privacy-First Android IME

A high-performance Android Input Method Editor built with **Kotlin**, **Jetpack Compose**, and **Librime**. AegisInput supports **Zhuyin (Bopomofo)** and **Pinyin** input with fuzzy matching for common pronunciation confusions. All text processing happens entirely on-device — no data ever leaves your phone.

## Features

- **On-device Chinese input** via Librime (RIME engine) — no cloud dependency
- **Zhuyin and Pinyin** input support with seamless switching
- **Installable launcher experience** with enable/switch-keyboard actions and a built-in demo surface
- **Fuzzy matching** for common pronunciation confusions (ㄥ/ㄣ, ㄓ/ㄗ, ㄕ/ㄙ, ㄔ/ㄘ, etc.)
- **Dynamic hitbox system** that adapts touch targets to reduce mis-taps on dense layouts
- **Material 3 keyboard UI** built entirely with Jetpack Compose
- **User dictionary learning** via Room database with frequency boosting
- **Privacy-first**: zero network permissions declared, no analytics, no telemetry — all data stays local

## Architecture Overview

AegisInput is organized into three Gradle modules with clear responsibility boundaries:

| Module | Responsibility |
|---|---|
| `:app` | `InputMethodService` implementation, application entry point, asset management |
| `:engine-rime` | Librime JNI bridge, fuzzy matching engine, user dictionary (Room) |
| `:ui-compose` | Compose-based keyboard UI, dynamic hitbox resolution, candidate bar |

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Android System                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │            AegisInputService (:app)              │    │
│  │                                                  │    │
│  │  Touch Event ──► KeyboardView (:ui-compose)     │    │
│  │                      │                           │    │
│  │              Key Code / Gesture                  │    │
│  │                      │                           │    │
│  │                      ▼                           │    │
│  │              FuzzyMatcher (:engine-rime)         │    │
│  │                      │                           │    │
│  │              Fuzzy Variants                      │    │
│  │                      │                           │    │
│  │                      ▼                           │    │
│  │              RimeBridge (JNI) (:engine-rime)     │    │
│  │                      │                           │    │
│  │              ┌───────┴───────┐                   │    │
│  │              ▼               ▼                   │    │
│  │        Librime (C++)   UserDictDB (Room)         │    │
│  │              │                                   │    │
│  │              ▼                                   │    │
│  │        Candidates                                │    │
│  │              │                                   │    │
│  │              ▼                                   │    │
│  │        CandidateBar (:ui-compose)                │    │
│  │              │                                   │    │
│  │         User taps candidate                      │    │
│  │              │                                   │    │
│  │              ▼                                   │    │
│  │    InputConnectionWrapper ──► Target App          │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## Data Flow

1. **Touch** — The user touches a key on the Compose keyboard surface.
2. **Hitbox Resolution** — `DynamicHitbox` resolves the intended key from raw touch coordinates, compensating for the dense Zhuyin layout by expanding targets around less-frequently mis-tapped keys.
3. **Fuzzy Matching** — The resolved key code passes through `FuzzyMatcher`, which generates pronunciation variants for commonly confused phonemes (e.g., ㄥ↔ㄣ, ㄓ↔ㄗ).
4. **RIME Processing** — `RimeBridge` sends the key (and its fuzzy variants) to Librime via JNI. Librime performs dictionary lookup and candidate generation using the active schema (Bopomofo or Pinyin).
5. **Candidate Display** — The resulting candidates appear in the `CandidateBar` composable, ranked by frequency and user history.
6. **Commit** — The user selects a candidate. `InputConnectionWrapper` commits the text to the target application via Android's `InputConnection` API.
7. **Learning** — `UserDictDatabase` records the selection, boosting its frequency for future sessions.

## Project Structure

```
aegis-input/
├── app/
│   ├── src/main/
│   │   ├── java/dev/cmwen/aegisinput/
│   │   │   ├── AegisInputApp.kt
│   │   │   ├── AegisInputService.kt
│   │   │   └── InputConnectionWrapper.kt
│   │   ├── assets/rime/
│   │   │   ├── default.yaml
│   │   │   ├── bopomofo.schema.yaml
│   │   │   └── luna_pinyin.schema.yaml
│   │   ├── res/
│   │   │   ├── xml/method.xml
│   │   │   └── values/strings.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── engine-rime/
│   ├── src/main/
│   │   ├── java/dev/cmwen/aegisinput/engine/
│   │   │   ├── RimeBridge.kt
│   │   │   ├── FuzzyMatcher.kt
│   │   │   ├── UserDictDatabase.kt
│   │   │   ├── UserDictDao.kt
│   │   │   └── UserDictEntry.kt
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt
│   │   │   ├── rime_jni.cpp
│   │   │   └── rime_jni_stub.cpp
│   │   └── jniLibs/
│   │       └── arm64-v8a/
│   └── build.gradle.kts
├── ui-compose/
│   ├── src/main/
│   │   ├── java/dev/cmwen/aegisinput/ui/
│   │   │   ├── KeyboardView.kt
│   │   │   ├── CandidateBar.kt
│   │   │   ├── DynamicHitbox.kt
│   │   │   ├── KeyButton.kt
│   │   │   ├── theme/
│   │   │   │   ├── Theme.kt
│   │   │   │   ├── Color.kt
│   │   │   │   └── Type.kt
│   │   │   └── layout/
│   │   │       ├── ZhuyinLayout.kt
│   │   │       └── PinyinLayout.kt
│   │   └── res/
│   └── build.gradle.kts
├── docs/
│   ├── SETUP.md
│   └── ARCHITECTURE.md
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md
└── LICENSE
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.1+ |
| UI | Jetpack Compose, Material 3 |
| IME Engine | Librime (RIME) via JNI / NDK |
| User Dictionary | Room (SQLite) |
| Build System | Gradle with Version Catalogs, Kotlin DSL |
| Native Build | CMake 3.22.1 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

## Getting Started

See the [Setup Guide](docs/SETUP.md) for build prerequisites and development instructions.

For a deeper look at the system design, see the [Architecture Document](docs/ARCHITECTURE.md).

## CI/CD

- **CI** — GitHub Actions runs unit tests for all modules and assembles a debug APK on pushes and pull requests to `master`.
- **Release** — Pushing a tag like `v0.1.2`, or running the **Release** workflow from the GitHub Actions UI with a tag like `v0.1.2`, builds a release APK, uploads it to the workflow run, and publishes it to a GitHub Release.
- **Release signing** — Configure the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` to keep releases signed with a stable keystore. If they are missing, the release workflow generates a temporary keystore and uploads a `signing-credentials-*` artifact so you can persist it for future releases.

## License

```
Copyright 2025 cmwen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
