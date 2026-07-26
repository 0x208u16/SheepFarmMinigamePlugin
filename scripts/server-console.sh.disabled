#!/usr/bin/env bash

set -uo pipefail

readonly PROJECT_DIR="$(dirname "$(dirname "$(realpath "${BASH_SOURCE[0]}")")")"
readonly SERVER_DIR="$PROJECT_DIR/server"
readonly RUNTIME_DIR="$SERVER_DIR/.control"
readonly PID_FILE="$RUNTIME_DIR/supervisor.pid"
readonly INPUT_PIPE="$RUNTIME_DIR/console-input"
readonly CONSOLE_LOG="$SERVER_DIR/logs/console.log"

if [[ ! -s "$PID_FILE" ]]; then
  echo "Server is not running. Start it with scripts/server-start.sh." >&2
  exit 1
fi

supervisor_pid="$(<"$PID_FILE")"
if [[ ! "$supervisor_pid" =~ ^[0-9]+$ ]] || ! kill -0 "$supervisor_pid" 2>/dev/null; then
  echo "Server control state is stale. Start it with scripts/server-start.sh." >&2
  exit 1
fi
if [[ ! -p "$INPUT_PIPE" ]]; then
  echo "Server console input pipe is missing." >&2
  exit 1
fi

printf 'Attached to the Paper console. Type commands normally; Ctrl+C detaches without stopping the server.\n'
printf 'Use scripts/server-stop.sh for a graceful shutdown.\n\n'

tail -n 80 -F "$CONSOLE_LOG" &
tail_pid=$!
cleanup() {
  kill "$tail_pid" 2>/dev/null || true
  wait "$tail_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

while kill -0 "$supervisor_pid" 2>/dev/null; do
  if ! IFS= read -r command; then
    break
  fi
  [[ -n "$command" ]] || continue
  printf '%s\n' "$command" > "$INPUT_PIPE"
done
