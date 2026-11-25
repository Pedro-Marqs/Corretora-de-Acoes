#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage: check-http-readiness.sh [options] --check service=url [--check service=url ...]

Options:
  --check service=url              Endpoint to verify; may be repeated
  --max-wait-seconds seconds       Total deadline for all checks (default: 120)
  --max-attempts attempts          Maximum attempts per endpoint (default: 40)
  --interval-seconds seconds       Delay between attempts (default: 3)
  --request-timeout-seconds secs   Per-request timeout, capped by deadline (default: 15)
USAGE
}

max_wait_seconds=120
max_attempts=40
interval_seconds=3
request_timeout_seconds=15
curl_bin="${CURL_BIN:-curl}"
clock_bin="${CLOCK_BIN:-date}"
sleep_bin="${SLEEP_BIN:-sleep}"
checks=()

while (($# > 0)); do
  case "$1" in
    --check)
      (($# >= 2)) || { echo "[readiness] error=missing_check_value" >&2; exit 2; }
      checks+=("$2")
      shift 2
      ;;
    --max-wait-seconds)
      (($# >= 2)) || { echo "[readiness] error=missing_max_wait" >&2; exit 2; }
      max_wait_seconds="$2"
      shift 2
      ;;
    --max-attempts)
      (($# >= 2)) || { echo "[readiness] error=missing_max_attempts" >&2; exit 2; }
      max_attempts="$2"
      shift 2
      ;;
    --interval-seconds)
      (($# >= 2)) || { echo "[readiness] error=missing_interval" >&2; exit 2; }
      interval_seconds="$2"
      shift 2
      ;;
    --request-timeout-seconds)
      (($# >= 2)) || { echo "[readiness] error=missing_request_timeout" >&2; exit 2; }
      request_timeout_seconds="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "[readiness] error=unknown_argument" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if ((${#checks[@]} == 0)); then
  echo "[readiness] error=no_checks" >&2
  usage >&2
  exit 2
fi

is_non_negative_integer() {
  [[ "$1" =~ ^[0-9]+$ ]]
}

is_positive_integer() {
  is_non_negative_integer "$1" && ((10#$1 > 0))
}

if ! is_positive_integer "$max_wait_seconds" ||
   ! is_positive_integer "$max_attempts" ||
   ! is_non_negative_integer "$interval_seconds" ||
   ! is_positive_integer "$request_timeout_seconds"; then
  echo "[readiness] error=invalid_limits" >&2
  exit 2
fi

now_epoch() {
  "$clock_bin" +%s
}

safe_endpoint() {
  local endpoint="$1"
  endpoint="${endpoint%%\?*}"
  endpoint="${endpoint%%#*}"
  redact_userinfo "$endpoint"
}

redact_userinfo() {
  local value="$1"
  if [[ "$value" =~ ^(.*)([a-zA-Z][a-zA-Z0-9+.-]*://)([^/@[:space:]]+@)([^[:space:]]*)$ ]]; then
    value="${BASH_REMATCH[1]}${BASH_REMATCH[2]}[redacted]@${BASH_REMATCH[4]}"
  fi
  printf '%s' "$value"
}

safe_detail() {
  local detail="$1"
  detail="${detail%%\?*}"
  detail="${detail%%#*}"
  detail="${detail//$'\r'/ }"
  detail="${detail//$'\n'/ }"
  detail="$(redact_userinfo "$detail")"
  printf '%.160s' "$detail"
}

check_endpoint() {
  local service="$1"
  local endpoint="$2"
  local deadline="$3"
  local display_endpoint display_service
  local attempt=1
  local now elapsed remaining request_timeout
  local response status curl_rc error_file error reason

  display_endpoint="$(safe_endpoint "$endpoint")"
  display_service="$(safe_detail "$service")"

  while :; do
    now="$(now_epoch)"
    elapsed=$((now - start_epoch))
    if ((now >= deadline)); then
      echo "[readiness] service=$display_service endpoint=$display_endpoint attempt=$attempt result=failed reason=deadline elapsed=${elapsed}s"
      return 1
    fi

    remaining=$((deadline - now))
    request_timeout="$request_timeout_seconds"
    if ((request_timeout > remaining)); then
      request_timeout="$remaining"
    fi
    if ((request_timeout < 1)); then
      request_timeout=1
    fi

    error_file="$(mktemp)"
    set +e
    response="$("$curl_bin" --silent --show-error --output /dev/null \
      --write-out '%{http_code}' --max-time "$request_timeout" "$endpoint" \
      2>"$error_file")"
    curl_rc=$?
    set -e
    error="$(<"$error_file")"
    rm -f "$error_file"
    status="${response:-000}"
    now="$(now_epoch)"
    elapsed=$((now - start_epoch))

    if ((curl_rc == 0)); then
      reason="http_status=$status"
    else
      reason="transport_code=$curl_rc"
      if [[ -n "$error" ]]; then
        reason+=" detail=$(safe_detail "$error")"
      fi
    fi

    if ((now >= deadline)); then
      echo "[readiness] service=$display_service endpoint=$display_endpoint attempt=$attempt status=$status result=failed reason=deadline $reason elapsed=${elapsed}s"
      return 1
    fi

    if ((curl_rc == 0)) && [[ "$status" =~ ^2[0-9][0-9]$ ]]; then
      echo "[readiness] service=$display_service endpoint=$display_endpoint attempt=$attempt status=$status result=ready elapsed=${elapsed}s"
      return 0
    fi

    echo "[readiness] service=$display_service endpoint=$display_endpoint attempt=$attempt status=$status result=retry reason=$reason elapsed=${elapsed}s"

    if ((attempt >= max_attempts)); then
      echo "[readiness] service=$display_service endpoint=$display_endpoint attempt=$attempt result=failed reason=attempt_limit elapsed=${elapsed}s"
      return 1
    fi

    now="$(now_epoch)"
    elapsed=$((now - start_epoch))
    if ((elapsed >= max_wait_seconds)); then
      echo "[readiness] service=$display_service endpoint=$display_endpoint attempt=$attempt result=failed reason=deadline elapsed=${elapsed}s"
      return 1
    fi

    remaining=$((deadline - now))
    sleep_seconds="$interval_seconds"
    if ((sleep_seconds > remaining)); then
      sleep_seconds="$remaining"
    fi
    "$sleep_bin" "$sleep_seconds"
    attempt=$((attempt + 1))
  done
}

start_epoch="$(now_epoch)"
deadline=$((start_epoch + max_wait_seconds))
overall_status=0

for check in "${checks[@]}"; do
  if [[ "$check" != *=* ]]; then
    echo "[readiness] error=invalid_check" >&2
    overall_status=1
    continue
  fi
  service="${check%%=*}"
  endpoint="${check#*=}"
  if [[ -z "$service" || -z "$endpoint" ]]; then
    echo "[readiness] error=invalid_check service=$(safe_detail "$service") endpoint=$(safe_endpoint "$endpoint")" >&2
    overall_status=1
    continue
  fi
  if ! check_endpoint "$service" "$endpoint" "$deadline"; then
    overall_status=1
  fi
done

if ((overall_status != 0)); then
  echo "[readiness] result=failed reason=service_not_ready"
  exit 1
fi

echo "[readiness] result=ready reason=all_services_ready elapsed=$(($(now_epoch) - start_epoch))s"
