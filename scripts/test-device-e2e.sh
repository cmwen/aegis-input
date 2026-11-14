#!/usr/bin/env bash
# Requires an unlocked, dedicated Android 8+ test device. No app data is cleared.
set -Eeuo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
SERIAL=""; ARTIFACTS=""; BUILD=1; TEST_CLASS=""
usage() { echo "Usage: $0 --serial SERIAL [--artifacts DIR] [--skip-build] [--class TEST_CLASS_OR_METHOD]"; }
while (($#)); do
  case "$1" in
    --serial) SERIAL=$2; shift 2;;
    --artifacts) ARTIFACTS=$2; shift 2;;
    --skip-build) BUILD=0; shift;;
    --class) TEST_CLASS=$2; shift 2;;
    *) usage; exit 2;;
  esac
done
[[ -n "$SERIAL" ]] || { usage; exit 2; }
ARTIFACTS=${ARTIFACTS:-"$ROOT/out/e2e-$(date +%Y%m%d-%H%M%S)"}
mkdir -p "$ARTIFACTS"; ARTIFACTS=$(cd "$ARTIFACTS" && pwd)
adb_device() { adb -s "$SERIAL" "$@"; }
[[ $(adb_device get-state) == device ]] || exit 1
if ((BUILD)); then
  "${GRADLE:-gradle}" -p "$ROOT" --stacktrace -PaegisTestBuildType=e2e \
    :app:assembleE2e :app:assembleE2eAndroidTest :e2e-host:assembleDebug :e2e-host:assembleDebugAndroidTest \
    >"$ARTIFACTS/build.log" 2>&1 || { tail -60 "$ARTIFACTS/build.log"; exit 1; }
fi
IME=com.aegisinput.app.e2e/com.aegisinput.app.AegisInputService
LOG_START=$(adb_device shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')
ORIGINAL_IME=$(adb_device shell settings get secure default_input_method | tr -d '\r')
ORIGINAL_ENABLED=$(adb_device shell settings get secure enabled_input_methods | tr -d '\r')
ORIGINAL_ACCEL=$(adb_device shell settings get system accelerometer_rotation | tr -d '\r')
ORIGINAL_ROTATION=$(adb_device shell settings get system user_rotation | tr -d '\r')
: >"$ARTIFACTS/restored-settings.txt"
printf '%s\n' "default_input_method=$ORIGINAL_IME" "enabled_input_methods=$ORIGINAL_ENABLED" \
  "accelerometer_rotation=$ORIGINAL_ACCEL" "user_rotation=$ORIGINAL_ROTATION" >"$ARTIFACTS/original-settings.txt"
restore_setting() {
  local scope=$1 name=$2 value=$3
  if [[ "$value" == null ]]; then adb_device shell settings delete "$scope" "$name";
  else adb_device shell settings put "$scope" "$name" "'$value'"; fi
}
finish() {
  local status=$? restore_failed=0
  trap - EXIT INT TERM
  set +e
  adb_device logcat -d -T "$LOG_START" -v threadtime -s AndroidRuntime:E AegisInputService:V >"$ARTIFACTS/logcat.txt" 2>&1
  adb_device exec-out run-as com.aegisinput.e2ehost tar -c -f - files/diagnostics >"$ARTIFACTS/diagnostics.tar" 2>"$ARTIFACTS/diagnostics-error.txt"
  if [[ "$ORIGINAL_IME" != null && -n "$ORIGINAL_IME" ]]; then
    adb_device shell ime set "$ORIGINAL_IME" >>"$ARTIFACTS/restore.log" 2>&1 || restore_failed=1
  fi
  restore_setting secure default_input_method "$ORIGINAL_IME" >>"$ARTIFACTS/restore.log" 2>&1 || restore_failed=1
  restore_setting secure enabled_input_methods "$ORIGINAL_ENABLED" >>"$ARTIFACTS/restore.log" 2>&1 || restore_failed=1
  restore_setting system accelerometer_rotation "$ORIGINAL_ACCEL" >>"$ARTIFACTS/restore.log" 2>&1 || restore_failed=1
  restore_setting system user_rotation "$ORIGINAL_ROTATION" >>"$ARTIFACTS/restore.log" 2>&1 || restore_failed=1
  # Verify exact values, including an originally missing setting.
  for spec in "secure default_input_method $ORIGINAL_IME" "secure enabled_input_methods $ORIGINAL_ENABLED" \
              "system accelerometer_rotation $ORIGINAL_ACCEL" "system user_rotation $ORIGINAL_ROTATION"; do
    read -r scope name expected <<<"$spec"
    actual=$(adb_device shell settings get "$scope" "$name" | tr -d '\r')
    printf '%s=%s\n' "$name" "$actual" >>"$ARTIFACTS/restored-settings.txt"
    [[ "$actual" == "$expected" ]] || restore_failed=1
  done
  ((restore_failed == 0)) || { echo "Device setting restoration failed; see $ARTIFACTS" >&2; status=1; }
  echo "Artifacts: $ARTIFACTS (exit $status)"
  exit "$status"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
adb_device install -r "$ROOT/app/build/outputs/apk/e2e/app-e2e.apk" >"$ARTIFACTS/install-ime.txt"
adb_device install -r "$ROOT/app/build/outputs/apk/androidTest/e2e/app-e2e-androidTest.apk" >"$ARTIFACTS/install-jni-tests.txt"
adb_device install -r "$ROOT/e2e-host/build/outputs/apk/debug/e2e-host-debug.apk" >"$ARTIFACTS/install-host.txt"
adb_device install -r "$ROOT/e2e-host/build/outputs/apk/androidTest/debug/e2e-host-debug-androidTest.apk" >"$ARTIFACTS/install-host-tests.txt"
run_tests() {
  local component=$1 log=$2; shift 2
  adb_device shell am instrument -w -r "$@" "$component" | tee "$log"
  # am instrument commonly exits zero even after an assertion failure or crash.
  python3 - "$log" <<'PY_CHECK'
import pathlib,re,sys
text=pathlib.Path(sys.argv[1]).read_text()
if not re.search(r'OK \([1-9][0-9]* tests?\)',text) or re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed|INSTRUMENTATION_ABORTED',text):
    raise SystemExit('Instrumentation did not finish with passing nonempty tests')
PY_CHECK
}
# Run packaged JNI/Room acceptance in the isolated variant too.
run_tests com.aegisinput.app.e2e.test/androidx.test.runner.AndroidJUnitRunner "$ARTIFACTS/jni-instrumentation.txt"
adb_device shell am force-stop com.aegisinput.app.e2e
adb_device shell ime enable "$IME" >"$ARTIFACTS/select-ime.txt"
adb_device shell ime set "$IME" >>"$ARTIFACTS/select-ime.txt"
adb_device shell run-as com.aegisinput.e2ehost rm -rf files/diagnostics
adb_device shell settings put system accelerometer_rotation 0
adb_device shell settings put system user_rotation 0
EXTRA=()
[[ -z "$TEST_CLASS" ]] || EXTRA=(-e class "$TEST_CLASS")
run_tests com.aegisinput.e2ehost.test/androidx.test.runner.AndroidJUnitRunner "$ARTIFACTS/ui-instrumentation.txt" "${EXTRA[@]}"
