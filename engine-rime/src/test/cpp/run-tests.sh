#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd "$(dirname "$0")/../../../../" && pwd)
python3 "$repo_root/scripts/native/bootstrap-rime.py"
cmake_bin="${CMAKE_BIN:-${ANDROID_HOME:-$HOME/Android/Sdk}/cmake/3.22.1/bin/cmake}"
if command -v cmake >/dev/null; then cmake_bin=$(command -v cmake); fi
build="$repo_root/engine-rime/build/host-rime"
"$cmake_bin" -S "$repo_root/engine-rime/src/main/cpp" -B "$build" -DCMAKE_BUILD_TYPE=Release
"$cmake_bin" --build "$build" --target aegis-rime-test --parallel 2
data=$(mktemp -d /tmp/aegis-rime-test.XXXXXX)
trap 'rm -rf "$data"' EXIT
mkdir -p "$data/shared" "$data/user"
cp "$repo_root"/app/src/main/assets/rime/*.yaml "$data/shared/"
cp "$repo_root/app/src/main/assets/rime/essay.txt" "$data/shared/"
"$build/aegis-rime-test" "$data/shared" "$data/user"
