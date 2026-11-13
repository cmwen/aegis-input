# Offline Pinyin engine and bundled dictionary

The `RimeBridge` / `RimeSession` names remain for source compatibility. This build
uses AegisInput's independent C++ full-Pinyin decoder, **not librime**. It needs no
external native downloads, schema compilation, network access, or runtime setup.

## Dictionary provenance

- Project: Rime's official `rime-pinyin-simp`, https://github.com/rime/rime-pinyin-simp
- Revision: `0c6861ef7420ee780270ca6d993d18d4101049d0`
- Upstream file: https://github.com/rime/rime-pinyin-simp/blob/0c6861ef7420ee780270ca6d993d18d4101049d0/pinyin_simp.dict.yaml
- Bundled verbatim as `app/src/main/assets/rime/pinyin_simp.dict.yaml` (1,266,216 bytes).
- SHA-256: `e341598343a0f0f2035bb1aafc34a7f3bb7887deeecb3f60796262aaa2983e6b`
- Upstream identifies this dictionary as derived from Android Pinyin IME.
- License: Apache License 2.0. Verbatim upstream license and author attribution are
  bundled alongside the data as `PINYIN-SIMP-LICENSE` and `PINYIN-SIMP-AUTHORS`.
- Dictionary contains over 65,000 weighted characters and phrases. No handpicked
  demonstration entries or per-word code overrides are used.

## Behavior and scope

The dictionary is installed atomically in private app storage and loaded once.
Initialization fails visibly if it is missing or incomplete. Exact phrase matches
rank first by upstream frequency. A bounded dynamic-programming beam combines
multiple dictionary words using log frequencies; the final incomplete word can
receive prefix completions. Apostrophes enforce syllable boundaries (`xi'an`).
Up to 12 distinct full-composition candidates are returned. Selecting one commits
the whole candidate and clears the session. Raw commit returns the typed Pinyin.
Backspace edits one ASCII composing character. Input is bounded at 64 characters.
Native state is guarded by a mutex; separate sessions isolate composing text.
Candidate strings cross JNI as UTF-16, preserving supplementary characters.

Supported input is lowercase full Pinyin and apostrophe, including `v` for ü.
Output follows the simplified Chinese dictionary (which also includes uncommon
traditional character variants). This version does not implement Zhuyin, tone
numbers, adaptive language-model learning, or librime schema/fuzzy-rule execution.
The older schema assets are historical resources and are not loaded by this decoder.

## Verification

Run `bash engine-rime/src/test/cpp/run-tests.sh` from the repository. It compiles
and exercises the actual native decoder against the bundled complete dictionary.
Checks cover direct phrases, multiple-word composition (`nihaozhongwen`), explicit
syllable boundaries, deletion/retyping, prefix candidates, selection/reset, raw
commit, invalid keys and indices, input limits, and missing-data failure. Android
JNI/build validation is covered by the repository's normal Gradle checks.
