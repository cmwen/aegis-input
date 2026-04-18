# AegisInput — Architecture

This document describes the internal architecture, threading model, component communication, and key design decisions behind AegisInput.

## Module Dependency Graph

```
┌────────┐       ┌───────────────┐
│  :app  │──────►│ :engine-rime  │
│        │       └───────────────┘
│        │       ┌───────────────┐
│        │──────►│  :ui-compose  │
└────────┘       └───────────────┘
```

- **`:app`** depends on both `:engine-rime` and `:ui-compose`.
- **`:engine-rime`** and **`:ui-compose`** are independent of each other. They communicate only through interfaces and data classes exposed by `:engine-rime` and consumed by `:app`, which acts as the coordinator.

This ensures that native code concerns (JNI, CMake, NDK) are fully isolated in `:engine-rime`, and UI code in `:ui-compose` has no native dependencies.

## Threading Model

AegisInput uses a carefully structured threading model to keep the keyboard responsive while performing potentially expensive operations in the background.

```
┌──────────────────────────────────────────────────────┐
│                    Main (UI) Thread                   │
│                                                      │
│  Compose rendering, touch event handling,            │
│  InputConnection commits, DynamicHitbox resolution   │
└──────────────┬────────────────────┬──────────────────┘
               │                    │
               ▼                    ▼
┌──────────────────────┐  ┌────────────────────────────┐
│  Dispatchers.Default │  │     Dispatchers.IO         │
│                      │  │                            │
│  FuzzyMatcher        │  │  Room DB read/write        │
│  Candidate ranking   │  │  Asset file I/O            │
└──────────────────────┘  │  RIME session init         │
                          └────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────┐
│              Native Thread (via JNI)                  │
│                                                      │
│  Librime process_key, get_candidates                 │
│  Called from coroutine on Dispatchers.Default         │
│  JNI calls are synchronized via a single-thread      │
│  dispatcher to avoid concurrent RIME session access  │
└──────────────────────────────────────────────────────┘
```

### Key threading rules

1. **Compose rendering** runs exclusively on the main thread. All state updates that trigger recomposition use `MutableState` or `StateFlow`.
2. **RIME engine calls** are dispatched to a dedicated single-thread coroutine dispatcher (`rimeDispatcher`) to serialize access to the native RIME session, which is not thread-safe.
3. **Room database** operations run on `Dispatchers.IO` via Room's built-in coroutine support.
4. **FuzzyMatcher** runs on `Dispatchers.Default` since it is a pure Kotlin computation with no shared mutable state.
5. Results flow back to the UI thread via `StateFlow` collected in Compose.

## Component Communication

### End-to-End Flow: Touch → Committed Text

```
User Touch
    │
    ▼
KeyboardView.onPointerEvent(offset)          [Main Thread]
    │
    ▼
DynamicHitbox.resolve(offset) → KeyCode       [Main Thread]
    │
    ▼
AegisInputService.onKeyPress(keyCode)          [Main Thread]
    │
    ▼
FuzzyMatcher.expand(keyCode) → List<KeyCode>   [Dispatchers.Default]
    │
    ▼
RimeBridge.processKeys(variants)               [rimeDispatcher]
    │  (JNI call to librime)
    │
    ▼
RimeBridge.getCandidates() → List<Candidate>   [rimeDispatcher]
    │
    ▼
candidatesFlow.emit(candidates)                [rimeDispatcher → Main]
    │
    ▼
CandidateBar recomposes with new candidates    [Main Thread]
    │
    ▼
User taps candidate
    │
    ▼
AegisInputService.onCandidateSelected(text)    [Main Thread]
    │
    ├──► InputConnection.commitText(text)       [Main Thread]
    │
    └──► UserDictDao.recordSelection(text)      [Dispatchers.IO]
```

### State Management

The service maintains a `KeyboardState` sealed interface representing the current input mode:

| State | Description |
|---|---|
| `Composing(buffer, candidates)` | User is building a phonetic sequence; candidates are available |
| `Selecting(candidates, page)` | Candidate bar is expanded for browsing |
| `Idle` | No active input composition |

State transitions are managed in `AegisInputService` and exposed as a `StateFlow<KeyboardState>` consumed by the Compose UI layer.

## Privacy Model

AegisInput is designed with a **zero-trust network architecture**: the application assumes it will never have network access and is built accordingly.

### Guarantees

| Property | Implementation |
|---|---|
| **No network permissions** | `AndroidManifest.xml` declares zero network-related permissions. No `INTERNET`, no `ACCESS_NETWORK_STATE`. |
| **No analytics / telemetry** | No analytics SDKs are included. No crash reporting services. No usage tracking of any kind. |
| **No third-party services** | The app has no runtime dependencies on Google Play Services, Firebase, or any cloud API. |
| **Local-only data** | The Room user dictionary database is stored in the app's private internal storage. It is not backed up to cloud services (`android:allowBackup="false"`). |
| **No data export** | There is no mechanism to export, share, or transmit user dictionary data. |
| **Auditable** | The manifest is the single source of truth for permissions. Anyone can verify the zero-network claim by inspecting `AndroidManifest.xml`. |

### Threat Model

The primary threat AegisInput defends against is **keystroke exfiltration** — the risk that a keyboard app silently transmits everything a user types to a remote server. AegisInput eliminates this threat class entirely by not requesting network access. Even if a vulnerability existed in a dependency, the Android permission system prevents any network communication.

## Key Design Decisions

### 1. Why Jetpack Compose for an IME

Traditional Android IMEs use XML layouts inflated into the `InputMethodService` window. AegisInput uses Jetpack Compose instead, hosted via a `ComposeView` attached to the service's window.

**Rationale:**

- **Declarative UI** makes complex keyboard layouts (Zhuyin has 37+ keys plus modifiers) easier to define, modify, and maintain than equivalent XML.
- **State-driven rendering** naturally fits the IME model: keyboard state changes (composing, selecting, switching layouts) trigger recomposition without manual view updates.
- **Animation support** enables smooth transitions between layouts and candidate bar expansion.
- **Lifecycle management** is handled by setting the `ComposeView`'s lifecycle owner to a custom `LifecycleOwner` tied to the service's `onCreate`/`onDestroy` cycle.

**Trade-off:** Compose adds ~2–3 MB to APK size compared to a pure XML approach. This is acceptable for the development velocity and maintainability gains.

### 2. Why Multi-Module Architecture

The three-module split (`:app`, `:engine-rime`, `:ui-compose`) is driven by practical engineering concerns:

- **Native code isolation**: The `:engine-rime` module contains all JNI, CMake, and NDK configuration. Changes to native build settings don't trigger recompilation of the UI or service modules.
- **Independent iteration**: UI developers can work on `:ui-compose` with the stub JNI backend and never need the NDK installed. Engine developers can test `:engine-rime` in isolation with unit tests.
- **Build performance**: Gradle's module-level caching means that unchanged modules are not recompiled. Since native builds are slow, isolating them in `:engine-rime` significantly improves incremental build times.
- **Future extensibility**: Additional engine backends (e.g., a local ML-based predictor) can be added as separate modules without touching the UI layer.

### 3. Why Room for the User Dictionary

The user dictionary stores phrase selections with frequency counts to boost commonly used candidates in future sessions.

**Why Room over raw SQLite or file-based storage:**

- **Structured queries**: Room's DAO pattern makes frequency-based queries (e.g., "top 5 candidates matching this prefix, ordered by frequency") type-safe and readable.
- **Frequency boosting**: A simple `UPDATE ... SET frequency = frequency + 1` on selection provides effective personalization with minimal logic.
- **Pruning**: Periodic pruning of low-frequency entries prevents unbounded database growth. Room's query support makes this a single `DELETE FROM user_dict WHERE frequency < ? AND last_used < ?` statement.
- **Coroutine support**: Room's native coroutine support integrates cleanly with the app's coroutine-based threading model.
- **Migration support**: Room's schema migration system handles database upgrades across app versions without data loss.

### 4. Dynamic Hitbox Rationale

The standard Zhuyin (Bopomofo) keyboard layout packs 37 phonetic symbols plus tone marks, space, backspace, and enter into a compact grid. At typical phone widths (360–412 dp), individual key targets can be as small as 32×42 dp — well below the recommended 48 dp minimum touch target.

**DynamicHitbox** addresses this by:

- **Expanding touch targets** of keys adjacent to empty space, borrowing from unused regions.
- **Biasing toward high-frequency keys**: Keys like ㄅ, ㄇ, ㄈ that appear in many common words get slightly larger touch zones than rarely used keys.
- **Contextual adjustment**: When certain keys are impossible in the current phonetic context (e.g., a tone mark cannot follow another tone mark), their hitbox area is temporarily redistributed to neighboring keys.
- **Post-hoc correction**: If a touch falls near a key boundary and the resulting input produces zero candidates, the system can silently retry with the neighboring key.

This approach keeps the visual layout standard and familiar while significantly reducing mis-tap rates.

### 5. Fuzzy Matching as a Pre-Processing Layer

Fuzzy matching in AegisInput operates **before** the RIME engine, not inside it. When a user types a phonetic key, `FuzzyMatcher` generates a set of plausible variants based on common Mandarin pronunciation confusions:

| Confusion Pair | Example |
|---|---|
| ㄥ ↔ ㄣ (eng/en) | 風 (fēng) vs 分 (fēn) |
| ㄓ ↔ ㄗ (zh/z) | 知 (zhī) vs 資 (zī) |
| ㄕ ↔ ㄙ (sh/s) | 詩 (shī) vs 思 (sī) |
| ㄔ ↔ ㄘ (ch/c) | 吃 (chī) vs 刺 (cì) |
| ㄖ ↔ ㄌ (r/l) | 人 (rén) vs 鄰 (lín) |
| ㄈ ↔ ㄏ (f/h) | 飛 (fēi) vs 灰 (huī) |

**Why pre-processing instead of RIME-level fuzzy:**

- **Schema-agnostic**: The fuzzy layer works identically whether the user is on a Bopomofo or Pinyin schema. RIME's built-in fuzzy matching requires per-schema configuration.
- **Controllable**: The app can offer granular user settings (e.g., enable ㄥ/ㄣ fuzzy but not ㄓ/ㄗ) without modifying RIME schema files.
- **Transparent**: Candidates generated via fuzzy expansion are tagged, allowing the UI to subtly indicate when a candidate came from a fuzzy match.
- **Testable**: `FuzzyMatcher` is a pure Kotlin class with no native dependencies, making it straightforward to unit test with comprehensive coverage.

The expanded key set is sent to RIME as multiple candidate queries, and results are merged and deduplicated before being presented to the user.
