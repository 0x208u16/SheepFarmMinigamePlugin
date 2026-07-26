#!/usr/bin/env bash

set -uo pipefail

readonly PROJECT_DIR="$(dirname "$(dirname "$(realpath "${BASH_SOURCE[0]}")")")"
readonly SERVER_DIR="$PROJECT_DIR/server"
readonly RUNTIME_DIR="$SERVER_DIR/.control"
readonly PID_FILE="$RUNTIME_DIR/supervisor.pid"
readonly PAPER_PID_FILE="$RUNTIME_DIR/paper.pid"
readonly INPUT_PIPE="$RUNTIME_DIR/console-input"
readonly STOP_TIMEOUT_SECONDS=60

if [[ ! -s "$PID_FILE" ]]; then
  echo "Server is not running."
  exit 0
fi

supervisor_pid="$(<"$PID_FILE")"
if [[ ! "$supervisor_pid" =~ ^[0-9]+$ ]] || ! kill -0 "$supervisor_pid" 2>/dev/null; then
  echo "Removing stale server control files."
  rm -f "$PID_FILE" "$PAPER_PID_FILE" "$INPUT_PIPE"
  exit 0
fi

if [[ ! -p "$INPUT_PIPE" ]]; then
  echo "Console input pipe is missing; refusing to terminate Paper ungracefully." >&2
  exit 1
fi

printf 'stop\n' > "$INPUT_PIPE"
echo "Sent a graceful stop command to Paper."

for ((elapsed = 0; elapsed < STOP_TIMEOUT_SECONDS; elapsed++)); do
  if ! kill -0 "$supervisor_pid" 2>/dev/null; then
    echo "Server stopped cleanly."
    exit 0
  fi
  sleep 1
done

echo "Paper did not stop within ${STOP_TIMEOUT_SECONDS}s." >&2
if [[ -s "$PAPER_PID_FILE" ]]; then
  paper_pid="$(<"$PAPER_PID_FILE")"
  if [[ "$paper_pid" =~ ^[0-9]+$ ]] && kill -0 "$paper_pid" 2>/dev/null; then
    echo "Send SIGTERM to Paper with: kill $paper_pid" >&2
  fi
fi
exit 1
