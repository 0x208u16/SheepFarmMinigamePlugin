#!/usr/bin/env bash

set -uo pipefail

readonly SCRIPT_PATH="$(realpath "${BASH_SOURCE[0]}")"
readonly PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_PATH")")"
readonly SERVER_DIR="$PROJECT_DIR/server"
readonly RUNTIME_DIR="$SERVER_DIR/.control"
readonly PID_FILE="$RUNTIME_DIR/supervisor.pid"
readonly PAPER_PID_FILE="$RUNTIME_DIR/paper.pid"
readonly INPUT_PIPE="$RUNTIME_DIR/console-input"
readonly LOCK_FILE="$RUNTIME_DIR/supervisor.lock"
readonly CONSOLE_LOG="$SERVER_DIR/logs/console.log"
readonly SUPERVISOR_LOG="$SERVER_DIR/logs/supervisor.log"
readonly DEFAULT_JAVA="$HOME/.local/share/PrismLauncher/java/java-runtime-gamma/bin/java"
readonly MIN_JAVA_VERSION=17
readonly INITIAL_RESTART_DELAY=5
readonly MAX_RESTART_DELAY=60
readonly STABLE_RUNTIME_SECONDS=60

is_running() {
  [[ -s "$PID_FILE" ]] || return 1
  local pid
  pid="$(<"$PID_FILE")"
  [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null
}

log_supervisor() {
  printf '[%(%Y-%m-%d %H:%M:%S)T] [supervisor] %s\n' -1 "$1" | tee -a "$SUPERVISOR_LOG"
}

resolve_java() {
  local candidate
  for candidate in \
    "${SERVER_JAVA:-}" \
    "$DEFAULT_JAVA" \
    "${JAVA_HOME:+$JAVA_HOME/bin/java}" \
    "$(command -v java 2>/dev/null || true)"; do
    if [[ -n "$candidate" && -x "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

run_supervisor() {
  cd "$SERVER_DIR"
  mkdir -p "$RUNTIME_DIR" logs

  exec 9>"$LOCK_FILE"
  if ! flock -n 9; then
    echo "A server supervisor is already running."
    exit 1
  fi

  printf '%s\n' "$$" > "$PID_FILE"
  trap 'rm -f "$PID_FILE" "$PAPER_PID_FILE"' EXIT

  local java_bin java_version restart_delay shutdown_requested child_pid
  java_bin="$(resolve_java)" || {
    log_supervisor "Java was not found. Set SERVER_JAVA to a Java $MIN_JAVA_VERSION+ executable."
    exit 1
  }
  java_version="$($java_bin -version 2>&1 | head -n 1 | grep -oE '[0-9]+' | head -n 1)"
  if [[ -z "$java_version" || "$java_version" -lt "$MIN_JAVA_VERSION" ]]; then
    log_supervisor "Java $MIN_JAVA_VERSION+ is required; found ${java_version:-unknown} at $java_bin."
    exit 1
  fi

  shutdown_requested=0
  child_pid=""
  request_shutdown() {
    shutdown_requested=1
    if [[ -n "$child_pid" ]] && kill -0 "$child_pid" 2>/dev/null; then
      kill -TERM "$child_pid" 2>/dev/null || true
    fi
  }
  trap request_shutdown INT TERM

  exec 0<>"$INPUT_PIPE"

  restart_delay=$INITIAL_RESTART_DELAY
  log_supervisor "Using Java $java_version at $java_bin."

  while (( ! shutdown_requested )); do
    local started_at exit_code runtime
    started_at=$SECONDS
    log_supervisor "Starting Paper."

    "$java_bin" \
      -Xms512M \
      -Xmx4G \
      -XX:+UseG1GC \
      -jar server.jar \
      --nogui <&0 9>&- &
    child_pid=$!
    printf '%s\n' "$child_pid" > "$PAPER_PID_FILE"

    wait "$child_pid"
    exit_code=$?
    child_pid=""
    rm -f "$PAPER_PID_FILE"
    runtime=$((SECONDS - started_at))

    if (( shutdown_requested )); then
      log_supervisor "Shutdown requested; supervisor exiting."
      exit 0
    fi
    if (( exit_code == 0 )); then
      log_supervisor "Paper stopped cleanly; supervisor will not restart it."
      exit 0
    fi

    log_supervisor "Paper exited abnormally with code $exit_code after ${runtime}s; restarting in ${restart_delay}s."
    sleep "$restart_delay" &
    wait $!

    if (( runtime >= STABLE_RUNTIME_SECONDS )); then
      restart_delay=$INITIAL_RESTART_DELAY
    else
      restart_delay=$((restart_delay * 2))
      if (( restart_delay > MAX_RESTART_DELAY )); then
        restart_delay=$MAX_RESTART_DELAY
      fi
    fi
  done
}

start_detached() {
  if [[ ! -d "$SERVER_DIR" || ! -f "$SERVER_DIR/server.jar" ]]; then
    echo "Server directory or server.jar not found: $SERVER_DIR" >&2
    exit 1
  fi

  mkdir -p "$RUNTIME_DIR" "$SERVER_DIR/logs"
  if is_running; then
    echo "Server is already running (supervisor PID $(<"$PID_FILE"))."
    exit 0
  fi

  rm -f "$PID_FILE" "$PAPER_PID_FILE" "$INPUT_PIPE"
  mkfifo -m 600 "$INPUT_PIPE"

  nohup "$SCRIPT_PATH" --supervise >> "$CONSOLE_LOG" 2>&1 &
  local launcher_pid=$!

  for _ in {1..50}; do
    if is_running; then
      echo "Server supervisor started (PID $(<"$PID_FILE"))."
      echo "Open the console with: scripts/server-console.sh"
      return 0
    fi
    if ! kill -0 "$launcher_pid" 2>/dev/null; then
      echo "Server supervisor failed to start. See $CONSOLE_LOG" >&2
      return 1
    fi
    sleep 0.1
  done

  echo "Timed out waiting for the server supervisor. See $CONSOLE_LOG" >&2
  return 1
}

if [[ "${1:-}" == "--supervise" ]]; then
  run_supervisor
else
  start_detached
fi
