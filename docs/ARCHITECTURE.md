# AegisInput architecture

`app` owns the Android InputMethodService, Compose lifecycle owners, editor connections and a fresh native Rime session per editor. `TypingController` implements composition, raw commit, candidate selection, Unicode deletion and editor actions through JVM-testable interfaces. Password fields force English and block Chinese input.

`engine-rime` builds the real Librime API and its dependencies from checksum-pinned upstream source. JNI serializes native calls and preserves Unicode text. `aegis_pinyin` uses the full bundled simplified Pinyin dictionary; `aegis_zhuyin` uses the tonal Terra Pinyin dictionary plus the official Rime essay frequency/phrase corpus and Rime's phonetic spelling algebra/key mapping. Fuzzy rules run in the active native schema, not through a mock lookup table.

Initialization and first dictionary compilation run on an IO coroutine. Shared schemas/data and writable user dictionaries live in separate private directories. Initialization failures appear in the keyboard status line. English typing remains usable while Chinese initializes. Unchanged asset contents are retained to avoid needless redeployment.

A Room database stores a composite `(mode + composition, word)` key and frequency. Selections update it in an IO coroutine. Candidate ranking preserves the original native indices, and asynchronous results are discarded if the editor/session/composition changed. Rime also maintains its native local user dictionary. Version 1 Room data migrates to the composite-key schema without losing prior entries.

`ui-compose` renders Material 3 English/Pinyin QWERTY and a complete Zhuyin layout. The header switches Pinyin and Zhuyin explicitly. Every keyboard row resolves actual pointer events through DynamicHitbox; bounded touch-frequency weights adapt gap targets. Accessible key click actions use the same key dispatcher. The candidate strip scrolls horizontally; system controls switch/hide the keyboard.

No network permission, telemetry, cloud service or Android backup is enabled. Native libraries cover ARM64/ARMv7/x86_64 with 16KB ELF page alignment. JVM tests cover editing/layout rules; host C++ and Android instrumented acceptance tests exercise actual Librime, dictionaries, schema deployment, fuzzy conversion and reset. Instrumented SQLite tests cover multiple learned candidates and frequency ordering.
