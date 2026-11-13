#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd "$(dirname "$0")/../../../../" && pwd)
binary=$(mktemp /tmp/aegis-pinyin-test.XXXXXX)
trap 'rm -f "$binary"' EXIT
c++ -std=c++17 -O2 -Wall -Wextra -Werror \
  -I "$repo_root/engine-rime/src/main/cpp" \
  "$repo_root/engine-rime/src/test/cpp/pinyin_engine_test.cpp" -o "$binary"
"$binary" "$repo_root/app/src/main/assets/rime/pinyin_simp.dict.yaml"
