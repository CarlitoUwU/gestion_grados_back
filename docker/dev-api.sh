#!/usr/bin/env sh
set -eu

POLL_INTERVAL="${HOT_RELOAD_POLL_INTERVAL:-1}"
STAMP_FILE="/tmp/gestion-grados-last-compile"
WATCH_PID=""

cleanup() {
	if [ -n "$WATCH_PID" ]; then
		kill "$WATCH_PID" 2>/dev/null || true
		wait "$WATCH_PID" 2>/dev/null || true
	fi
}

compile_classes() {
	echo "[dev-api] Compiling classes..."
	gradle --no-daemon classes -x test
	touch "$STAMP_FILE"
}

watch_sources() {
	while true; do
		if find src/main build.gradle settings.gradle -type f -newer "$STAMP_FILE" | grep -q .; then
			echo "[dev-api] Change detected. Recompiling..."
			if ! compile_classes; then
				echo "[dev-api] Compilation failed. Waiting for the next change..."
			fi
		fi
		sleep "$POLL_INTERVAL"
	done
}

trap cleanup INT TERM EXIT

compile_classes
watch_sources &
WATCH_PID="$!"

echo "[dev-api] Starting Spring Boot. Hot reload poll interval: ${POLL_INTERVAL}s"
gradle --no-daemon --rerun-tasks bootRun
