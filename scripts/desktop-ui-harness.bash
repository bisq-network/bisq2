#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

HARNESS_DIR="${HARNESS_DIR:-/tmp/bisq2-ui-harness}"
DATA_DIR="${DATA_DIR:-${HARNESS_DIR}/data}"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-${HARNESS_DIR}/artifacts}"
LOG_FILE="${LOG_FILE:-${HARNESS_DIR}/desktop.log}"
PID_FILE="${PID_FILE:-${HARNESS_DIR}/desktop.pid}"
TOKEN_FILE="${TOKEN_FILE:-${HARNESS_DIR}/automation.token}"
APP_NAME="${APP_NAME:-bisq2_gui_harness}"
AUTOMATION_HOST="${AUTOMATION_HOST:-127.0.0.1}"
AUTOMATION_PORT="${AUTOMATION_PORT:-18180}"
WINDOW_WIDTH="${WINDOW_WIDTH:-1440}"
WINDOW_HEIGHT="${WINDOW_HEIGHT:-900}"
STAGE_TIMEOUT_MS="${STAGE_TIMEOUT_MS:-45000}"
P2P_PORT="${P2P_PORT:-}"
HARNESS_RESET_ON_START="${HARNESS_RESET_ON_START:-1}"

HARNESS_BIN="${REPO_DIR}/apps/desktop/desktop-ui-harness-app/build/install/desktop-ui-harness-app/bin/desktop-ui-harness-app"

# Optional network override for deterministic local clusters.
# Example:
# HARNESS_NETWORK_OPTS="-Dapplication.network.supportedTransportTypes.0=CLEAR -Dapplication.network.configByTransportType.clear.defaultNodePort=18101 -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:18000 -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:18000"
HARNESS_NETWORK_OPTS="${HARNESS_NETWORK_OPTS:-}"

AUTOMATION_URL="http://${AUTOMATION_HOST}:${AUTOMATION_PORT}"
AUTOMATION_HEADER_NAME="X-Bisq-Automation-Token"

usage() {
  cat <<'EOF'
Usage:
  scripts/desktop-ui-harness.bash start
  scripts/desktop-ui-harness.bash stop
  scripts/desktop-ui-harness.bash restart
  scripts/desktop-ui-harness.bash status
  scripts/desktop-ui-harness.bash health
  scripts/desktop-ui-harness.bash validate
  scripts/desktop-ui-harness.bash logs
  scripts/desktop-ui-harness.bash tail
  scripts/desktop-ui-harness.bash nodes
  scripts/desktop-ui-harness.bash screenshot [name]
  scripts/desktop-ui-harness.bash click <selector>
  scripts/desktop-ui-harness.bash type <selector> <text>
  scripts/desktop-ui-harness.bash wait-node <selector> [timeout_ms] [visible]
  scripts/desktop-ui-harness.bash press-key <key> [selector]
  scripts/desktop-ui-harness.bash scenario <scenario-file>

Environment overrides:
  HARNESS_DIR, DATA_DIR, ARTIFACTS_DIR, APP_NAME
  AUTOMATION_HOST, AUTOMATION_PORT
  WINDOW_WIDTH, WINDOW_HEIGHT, STAGE_TIMEOUT_MS, P2P_PORT, HARNESS_RESET_ON_START
  HARNESS_NETWORK_OPTS
EOF
}

ensure_dirs() {
  mkdir -p "${HARNESS_DIR}" "${DATA_DIR}" "${ARTIFACTS_DIR}"
  chmod 700 "${HARNESS_DIR}" "${DATA_DIR}" "${ARTIFACTS_DIR}"
}

cleanup_runtime_files() {
  rm -f "${PID_FILE}" "${TOKEN_FILE}" "${HARNESS_DIR}/p2p-port" >/dev/null 2>&1 || true
}

is_expected_process() {
  local pid="${1:-}"
  [[ -n "${pid}" ]] || return 1
  ps -p "${pid}" -o args= 2>/dev/null | grep -F -- "--app-name=${APP_NAME}" >/dev/null 2>&1
}

is_running() {
  [[ -f "${PID_FILE}" ]] || return 1
  local pid
  pid="$(cat "${PID_FILE}")"
  [[ -n "${pid}" ]] || return 1
  if ! kill -0 "${pid}" >/dev/null 2>&1 || ! is_expected_process "${pid}"; then
    cleanup_runtime_files
    return 1
  fi
  return 0
}

require_token() {
  if [[ ! -f "${TOKEN_FILE}" ]]; then
    echo "Token file missing: ${TOKEN_FILE}"
    exit 1
  fi
}

generate_token() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 24
  else
    echo "openssl is required to generate the automation token." >&2
    return 1
  fi
}

is_port_bound() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
  elif command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "${port}" >/dev/null 2>&1
  else
    echo "Neither lsof nor nc is available to verify free ports." >&2
    return 2
  fi
}

find_free_port() {
  local start="${1:-19101}"
  local end="${2:-19250}"
  local port status
  for port in $(seq "${start}" "${end}"); do
    is_port_bound "${port}" && continue
    status=$?
    if (( status == 2 )); then
      return 2
    fi
    echo "${port}"
    return 0
  done
  return 1
}

auth_header() {
  local token
  token="$(cat "${TOKEN_FILE}")"
  printf '%s: %s' "${AUTOMATION_HEADER_NAME}" "${token}"
}

api_request() {
  local response_file
  response_file="$(mktemp "${HARNESS_DIR}/api-response.XXXXXX")"
  local code
  code="$(curl -sS -o "${response_file}" -w '%{http_code}' "$@")" || {
    rm -f "${response_file}" >/dev/null 2>&1 || true
    return 1
  }
  cat "${response_file}"
  rm -f "${response_file}" >/dev/null 2>&1 || true
  if (( code < 200 || code >= 300 )); then
    return 1
  fi
}

run_api_request() {
  local response_var_name="$1"
  shift

  local response_body
  if ! response_body="$(api_request "$@")"; then
    printf -v "${response_var_name}" '%s' "${response_body:-}"
    if [[ -n "${response_body:-}" ]]; then
      echo "${response_body}"
    fi
    return 1
  fi

  printf -v "${response_var_name}" '%s' "${response_body}"
}

split_scenario_line() {
  local line="${1:-}"
  python3 - "${line}" <<'PY'
import shlex
import sys

for part in shlex.split(sys.argv[1]):
    sys.stdout.buffer.write(part.encode("utf-8"))
    sys.stdout.buffer.write(b"\0")
PY
}

require_ok_status_json() {
  local response="${1:-}"
  if [[ "${response}" != *"\"status\":\"ok\""* ]]; then
    return 1
  fi
}

health_status_code() {
  require_token
  curl -sS -o /dev/null -w '%{http_code}' \
    -H "$(auth_header)" \
    "${AUTOMATION_URL}/health" || true
}

wait_for_health() {
  local timeout_sec="${1:-40}"
  local start now code
  start="$(date +%s)"
  while true; do
    code="$(health_status_code)"
    if [[ "${code}" == "200" ]]; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start >= timeout_sec )); then
      return 1
    fi
    sleep 1
  done
}

require_binary() {
  if [[ ! -x "${HARNESS_BIN}" ]]; then
    echo "Missing desktop UI harness binary. Build first:"
    echo "  ./gradlew :apps:desktop:desktop-ui-harness-app:installDist"
    exit 1
  fi
}

start() {
  require_binary
  ensure_dirs

  if is_running; then
    echo "Harness already running (pid=$(cat "${PID_FILE}"))."
    status
    return 0
  fi

  if [[ "${HARNESS_RESET_ON_START}" == "1" ]]; then
    rm -rf "${DATA_DIR}"
    mkdir -p "${DATA_DIR}"
    chmod 700 "${DATA_DIR}"
  fi

  local token
  token="$(generate_token)"
  (
    umask 077
    printf '%s\n' "${token}" > "${TOKEN_FILE}"
  )

  local automation_opts
  automation_opts="-Dbisq.desktopUiHarness.bind.host=${AUTOMATION_HOST} -Dbisq.desktopUiHarness.bind.port=${AUTOMATION_PORT} -Dbisq.desktopUiHarness.token=${token} -Dbisq.desktopUiHarness.artifacts.dir=${ARTIFACTS_DIR} -Dbisq.desktopUiHarness.window.width=${WINDOW_WIDTH} -Dbisq.desktopUiHarness.window.height=${WINDOW_HEIGHT} -Dbisq.desktopUiHarness.stage.timeoutMs=${STAGE_TIMEOUT_MS}"

  local network_opts
  network_opts="${HARNESS_NETWORK_OPTS}"
  if [[ -z "${network_opts}" ]]; then
    local selected_port
    selected_port="${P2P_PORT}"
    if [[ -z "${selected_port}" ]]; then
      if ! selected_port="$(find_free_port 19101 19250)"; then
        cleanup_runtime_files
        echo "No free P2P port found in 19101-19250; set P2P_PORT or HARNESS_NETWORK_OPTS." >&2
        exit 1
      fi
    fi
    if [[ -z "${selected_port}" ]]; then
      echo "No free P2P port found in 19101-19250; set P2P_PORT or HARNESS_NETWORK_OPTS."
      exit 1
    fi
    printf '%s\n' "${selected_port}" > "${HARNESS_DIR}/p2p-port"
    network_opts="-Dapplication.network.supportedTransportTypes.0=CLEAR -Dapplication.network.configByTransportType.clear.defaultNodePort=${selected_port} -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:${selected_port} -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:${selected_port}"
  fi

  local combined_opts
  combined_opts="${JAVA_OPTS:-} ${network_opts} ${automation_opts}"

  (
    umask 077
    : > "${LOG_FILE}"
  )
  if command -v setsid >/dev/null 2>&1; then
    nohup setsid env JAVA_OPTS="${combined_opts}" \
      "${HARNESS_BIN}" \
      --app-name="${APP_NAME}" \
      --data-dir="${DATA_DIR}" >> "${LOG_FILE}" 2>&1 < /dev/null &
  else
    nohup env JAVA_OPTS="${combined_opts}" \
      "${HARNESS_BIN}" \
      --app-name="${APP_NAME}" \
      --data-dir="${DATA_DIR}" >> "${LOG_FILE}" 2>&1 < /dev/null &
  fi
  local pid=$!
  (
    umask 077
    printf '%s\n' "${pid}" > "${PID_FILE}"
  )

  if wait_for_health 45; then
    echo "Desktop UI harness started."
    status
  else
    cleanup_runtime_files
    echo "Harness failed to become healthy. Last log lines:"
    tail -n 120 "${LOG_FILE}" || true
    exit 1
  fi
}

stop() {
  if ! is_running; then
    cleanup_runtime_files
    echo "Harness not running."
    return 0
  fi

  local pid
  pid="$(cat "${PID_FILE}")"
  kill "${pid}" >/dev/null 2>&1 || true

  for _ in $(seq 1 20); do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      break
    fi
    sleep 0.2
  done

  if kill -0 "${pid}" >/dev/null 2>&1; then
    kill -9 "${pid}" >/dev/null 2>&1 || true
  fi

  cleanup_runtime_files
  echo "Desktop UI harness stopped."
}

status() {
  echo "Harness paths:"
  echo "  dir: ${HARNESS_DIR}"
  echo "  data: ${DATA_DIR}"
  echo "  artifacts: ${ARTIFACTS_DIR}"
  echo "  log: ${LOG_FILE}"
  if [[ -f "${HARNESS_DIR}/p2p-port" ]]; then
    echo "  p2p-port: $(cat "${HARNESS_DIR}/p2p-port")"
  fi
  if is_running; then
    echo "Process:"
    echo "  pid=$(cat "${PID_FILE}") running"
  else
    echo "Process:"
    echo "  not running"
  fi
  if [[ -f "${TOKEN_FILE}" ]]; then
    echo "Automation:"
    echo "  ${AUTOMATION_URL} -> $(health_status_code)"
  else
    echo "Automation:"
    echo "  token not present"
  fi
}

health() {
  require_token
  local response
  run_api_request response -H "$(auth_header)" "${AUTOMATION_URL}/health"
  echo "${response}"
  require_ok_status_json "${response}"
}

nodes() {
  require_token
  local response
  run_api_request response -H "$(auth_header)" "${AUTOMATION_URL}/nodes"
  echo "${response}"
}

validate() {
  require_token
  local response
  run_api_request response -H "$(auth_header)" "${AUTOMATION_URL}/automation/validate"
  echo "${response}"
  require_ok_status_json "${response}"
}

screenshot() {
  require_token
  local name="${1:-shot}"
  local response
  run_api_request response --request POST --get \
    --data-urlencode "name=${name}" \
    -H "$(auth_header)" \
    "${AUTOMATION_URL}/screenshot"
  echo "${response}"
  require_ok_status_json "${response}"
}

click() {
  require_token
  local selector="${1:-}"
  if [[ -z "${selector}" ]]; then
    echo "Usage: scripts/desktop-ui-harness.bash click <selector>"
    exit 1
  fi
  local response
  run_api_request response --request POST --get \
    --data-urlencode "selector=${selector}" \
    -H "$(auth_header)" \
    "${AUTOMATION_URL}/action/click"
  echo "${response}"
  require_ok_status_json "${response}"
}

type_text() {
  require_token
  local selector="${1:-}"
  shift || true
  local text="${*:-}"
  if [[ -z "${selector}" || -z "${text}" ]]; then
    echo "Usage: scripts/desktop-ui-harness.bash type <selector> <text>"
    exit 1
  fi
  local response
  run_api_request response --request POST --get \
    --data-urlencode "selector=${selector}" \
    --data-urlencode "text=${text}" \
    -H "$(auth_header)" \
    "${AUTOMATION_URL}/action/type"
  echo "${response}"
  require_ok_status_json "${response}"
}

wait_node() {
  require_token
  local selector="${1:-}"
  local timeout_ms="${2:-5000}"
  local visible="${3:-false}"
  if [[ -z "${selector}" ]]; then
    echo "Usage: scripts/desktop-ui-harness.bash wait-node <selector> [timeout_ms] [visible]"
    exit 1
  fi
  local response
  run_api_request response --request POST --get \
    --data-urlencode "selector=${selector}" \
    --data-urlencode "timeoutMs=${timeout_ms}" \
    --data-urlencode "visible=${visible}" \
    -H "$(auth_header)" \
    "${AUTOMATION_URL}/wait/node"
  echo "${response}"
  require_ok_status_json "${response}"
}

press_key() {
  require_token
  local key="${1:-}"
  local selector="${2:-}"
  if [[ -z "${key}" ]]; then
    echo "Usage: scripts/desktop-ui-harness.bash press-key <key> [selector]"
    exit 1
  fi
  local response
  run_api_request response --request POST --get \
    --data-urlencode "key=${key}" \
    --data-urlencode "selector=${selector}" \
    -H "$(auth_header)" \
    "${AUTOMATION_URL}/action/pressKey"
  echo "${response}"
  require_ok_status_json "${response}"
}

run_scenario() {
  local scenario_file="${1:-}"
  if [[ -z "${scenario_file}" ]]; then
    echo "Usage: scripts/desktop-ui-harness.bash scenario <scenario-file>"
    exit 1
  fi
  if [[ ! -f "${scenario_file}" ]]; then
    echo "Scenario file not found: ${scenario_file}"
    exit 1
  fi

  local line
  local line_no=0
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line_no=$((line_no + 1))
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    if [[ -z "${line}" || "${line:0:1}" == "#" ]]; then
      continue
    fi

    local parts=()
    while IFS= read -r -d '' token; do
      parts+=("${token}")
    done < <(split_scenario_line "${line}")
    set -- "${parts[@]}"
    local cmd="${1:-}"
    shift || true

    case "${cmd}" in
      health)
        health
        ;;
      validate)
        validate
        ;;
      nodes)
        nodes
        ;;
      wait-node)
        wait_node "${1:-}" "${2:-5000}" "${3:-false}"
        ;;
      click)
        click "${1:-}"
        ;;
      type)
        local selector="${1:-}"
        shift || true
        type_text "${selector}" "$*"
        ;;
      press-key)
        press_key "${1:-}" "${2:-}"
        ;;
      screenshot)
        screenshot "${1:-shot}"
        ;;
      sleep)
        local ms="${1:-}"
        if [[ ! "${ms}" =~ ^[0-9]+$ ]]; then
          echo "Invalid sleep value at line ${line_no}: ${line}"
          return 1
        fi
        local sec
        sec="$(awk "BEGIN {printf \"%.3f\", ${ms} / 1000}")"
        sleep "${sec}"
        ;;
      *)
        echo "Unknown scenario command at line ${line_no}: ${cmd}"
        return 1
        ;;
    esac
  done < "${scenario_file}"

  echo "Scenario completed: ${scenario_file}"
}

logs() {
  sed -n '1,200p' "${LOG_FILE}"
}

tail_logs() {
  tail -n 120 "${LOG_FILE}"
}

restart() {
  stop
  start
}

main() {
  local cmd="${1:-}"
  shift || true
  case "${cmd}" in
    start) start ;;
    stop) stop ;;
    restart) restart ;;
    status) status ;;
    health) health ;;
    validate) validate ;;
    nodes) nodes ;;
    screenshot) screenshot "$@" ;;
    click) click "$@" ;;
    type) type_text "$@" ;;
    wait-node) wait_node "$@" ;;
    press-key) press_key "$@" ;;
    scenario) run_scenario "$@" ;;
    logs) logs ;;
    tail) tail_logs ;;
    *) usage; exit 1 ;;
  esac
}

main "$@"
