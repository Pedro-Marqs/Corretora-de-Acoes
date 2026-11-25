#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="$script_dir/../.github/scripts/check-http-readiness.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT

cat >"$test_dir/fake-curl" <<'FAKE_CURL'
#!/usr/bin/env bash
set -Eeuo pipefail
mode="$FAKE_CURL_MODE"
count_file="$FAKE_CURL_COUNT"
count=0
if [[ -f "$count_file" ]]; then
  count="$(<"$count_file")"
fi
count=$((count + 1))
printf '%s' "$count" >"$count_file"
case "$mode" in
  immediate)
    printf '200'
    ;;
  immediate-late)
    printf 'late' >"$POST_DEADLINE_CLOCK_COUNT"
    printf '200'
    ;;
  transport-late)
    printf 'late' >"$POST_DEADLINE_CLOCK_COUNT"
    echo 'Empty reply from server' >&2
    exit 52
    ;;
  late)
    if ((count < 3)); then
      echo 'Empty reply from server' >&2
      exit 52
    fi
    printf '200'
    ;;
  http)
    printf '503'
    ;;
  transport)
    echo 'Connection refused' >&2
    exit 7
    ;;
  transport-secret)
    echo 'request failed for https://user:secret@host/path?token=secret' >&2
    exit 7
    ;;
  *)
    echo "unknown fake mode: $mode" >&2
    exit 99
    ;;
esac
FAKE_CURL
chmod +x "$test_dir/fake-curl"

cat >"$test_dir/fake-clock" <<'FAKE_CLOCK'
#!/usr/bin/env bash
printf '1000\n'
FAKE_CLOCK
chmod +x "$test_dir/fake-clock"

cat >"$test_dir/advancing-clock" <<'ADVANCING_CLOCK'
#!/usr/bin/env bash
set -Eeuo pipefail
count_file="$ADVANCING_CLOCK_COUNT"
count=0
if [[ -f "$count_file" ]]; then
  count="$(<"$count_file")"
fi
printf '%s' "$((count + 1))" >"$count_file"
printf '%s\n' "$((1000 + count))"
ADVANCING_CLOCK
chmod +x "$test_dir/advancing-clock"

cat >"$test_dir/post-deadline-clock" <<'POST_DEADLINE_CLOCK'
#!/usr/bin/env bash
set -Eeuo pipefail
count_file="$POST_DEADLINE_CLOCK_COUNT"
count=0
if [[ -f "$count_file" ]]; then
  count="$(<"$count_file")"
fi
if [[ "$count" == late ]]; then
  printf '1002\n'
else
  printf '1000\n'
fi
POST_DEADLINE_CLOCK
chmod +x "$test_dir/post-deadline-clock"

cat >"$test_dir/fake-sleep" <<'FAKE_SLEEP'
#!/usr/bin/env bash
if [[ -n "${FAKE_SLEEP_LOG:-}" ]]; then
  printf '%s\n' "$1" >>"$FAKE_SLEEP_LOG"
fi
exit 0
FAKE_SLEEP
chmod +x "$test_dir/fake-sleep"

assert_contains() {
  local text="$1"
  local expected="$2"
  [[ "$text" == *"$expected"* ]] || {
    printf 'Expected output to contain: %s\n%s\n' "$expected" "$text" >&2
    exit 1
  }
}

run_readiness() {
  local mode="$1"
  export FAKE_CURL_MODE="$mode"
  export FAKE_CURL_COUNT="$test_dir/$mode.count"
  : >"$FAKE_CURL_COUNT"
  set +e
  output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/fake-clock" \
    SLEEP_BIN="$test_dir/fake-sleep" bash "$script" \
    --max-wait-seconds 30 --max-attempts 3 --interval-seconds 0 \
    --request-timeout-seconds 2 --check "backend=http://127.0.0.1:8080/api/csrf?token=secret")"
  rc=$?
  set -e
}

run_readiness immediate
[[ "$rc" -eq 0 ]]
assert_contains "$output" 'status=200 result=ready'

run_readiness late
[[ "$rc" -eq 0 ]]
assert_contains "$output" 'transport_code=52'
assert_contains "$output" 'attempt=3 status=200 result=ready'
[[ "$output" != *'secret'* ]]

run_readiness http
[[ "$rc" -ne 0 ]]
assert_contains "$output" 'status=503 result=retry'
assert_contains "$output" 'reason=attempt_limit'

run_readiness transport
[[ "$rc" -ne 0 ]]
assert_contains "$output" 'transport_code=7'
assert_contains "$output" 'reason=attempt_limit'

export ADVANCING_CLOCK_COUNT="$test_dir/advancing-clock.count"
export FAKE_SLEEP_LOG="$test_dir/sleep.log"
: >"$ADVANCING_CLOCK_COUNT"
: >"$FAKE_SLEEP_LOG"
export FAKE_CURL_MODE=http
export FAKE_CURL_COUNT="$test_dir/deadline.count"
: >"$FAKE_CURL_COUNT"
set +e
output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/advancing-clock" \
  SLEEP_BIN="$test_dir/fake-sleep" bash "$script" \
  --max-wait-seconds 4 --max-attempts 10 --interval-seconds 3 \
  --request-timeout-seconds 2 --check 'backend=http://127.0.0.1:8080/api/csrf')"
rc=$?
set -e
[[ "$rc" -ne 0 ]]
[[ "$(head -n 1 "$FAKE_SLEEP_LOG")" == '1' ]]

export POST_DEADLINE_CLOCK_COUNT="$test_dir/post-deadline-clock.count"
: >"$POST_DEADLINE_CLOCK_COUNT"
export FAKE_CURL_MODE=immediate-late
export FAKE_CURL_COUNT="$test_dir/post-deadline.count"
: >"$FAKE_CURL_COUNT"
set +e
output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/post-deadline-clock" \
  SLEEP_BIN="$test_dir/fake-sleep" bash "$script" \
  --max-wait-seconds 2 --max-attempts 1 --interval-seconds 0 \
  --request-timeout-seconds 2 --check 'backend=http://127.0.0.1:8080/api/csrf')"
rc=$?
set -e
[[ "$rc" -ne 0 ]]
assert_contains "$output" 'result=failed reason=deadline'
[[ "$output" != *'result=ready'* ]]

: >"$POST_DEADLINE_CLOCK_COUNT"
export FAKE_CURL_MODE=transport-late
export FAKE_CURL_COUNT="$test_dir/post-deadline-transport.count"
: >"$FAKE_CURL_COUNT"
set +e
output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/post-deadline-clock" \
  SLEEP_BIN="$test_dir/fake-sleep" bash "$script" \
  --max-wait-seconds 2 --max-attempts 1 --interval-seconds 0 \
  --request-timeout-seconds 2 --check 'backend=http://127.0.0.1:8080/api/csrf')"
rc=$?
set -e
[[ "$rc" -ne 0 ]]
assert_contains "$output" 'result=failed reason=deadline transport_code=52'
assert_contains "$output" 'detail=Empty reply from server'

export FAKE_CURL_MODE=transport-secret
export FAKE_CURL_COUNT="$test_dir/transport-secret.count"
: >"$FAKE_CURL_COUNT"
set +e
output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/fake-clock" \
  SLEEP_BIN="$test_dir/fake-sleep" bash "$script" \
  --max-wait-seconds 30 --max-attempts 1 --interval-seconds 0 \
  --request-timeout-seconds 2 --check 'backend=http://127.0.0.1:8080/api/csrf')"
rc=$?
set -e
[[ "$rc" -ne 0 ]]
[[ "$output" != *'user:secret'* ]]
[[ "$output" != *'token=secret'* ]]

set +e
output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/fake-clock" \
  SLEEP_BIN="$test_dir/fake-sleep" FAKE_CURL_MODE=immediate bash "$script" \
  --max-wait-seconds 30 --max-attempts 1 --interval-seconds 0 \
  --request-timeout-seconds 2 --check 'https://user:secret@host/path?token=' 2>&1)"
rc=$?
set -e
[[ "$rc" -ne 0 ]]
[[ "$output" != *'user:secret'* ]]
[[ "$output" != *'token=secret'* ]]

set +e
output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/fake-clock" \
  SLEEP_BIN="$test_dir/fake-sleep" FAKE_CURL_MODE=immediate bash "$script" \
  --max-wait-seconds 30 --max-attempts 1 --interval-seconds 0 \
  --request-timeout-seconds 2 --check 'backend=http://user:secret@127.0.0.1:8080/api/csrf?token=secret')"
rc=$?
set -e
[[ "$rc" -eq 0 ]]
[[ "$output" == *'http://[redacted]@127.0.0.1:8080/api/csrf'* ]]
[[ "$output" != *'user:secret'* ]]
[[ "$output" != *'token=secret'* ]]

set +e
output="$(CURL_BIN="$test_dir/fake-curl" CLOCK_BIN="$test_dir/fake-clock" \
  SLEEP_BIN="$test_dir/fake-sleep" FAKE_CURL_MODE=immediate bash "$script" \
  --max-wait-seconds 30 --max-attempts 1 --interval-seconds 0 \
  --request-timeout-seconds 2 --check 'https://user:secret@host/path?token=secret=1')"
rc=$?
set -e
[[ "$rc" -eq 0 ]]
[[ "$output" == *'service=https://[redacted]@host/path'* ]]
[[ "$output" != *'user:secret'* ]]
[[ "$output" != *'token=secret'* ]]

printf 'check-http-readiness tests: PASS\n'
