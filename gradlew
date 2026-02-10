#!/usr/bin/env sh
set -eu

# Lightweight, text-only Gradle wrapper shim.
# Uses a locally installed `gradle` command so we can avoid committing
# binary wrapper artifacts in environments that reject binary files in PRs.

if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: 'gradle' command not found in PATH." >&2
  echo "Install Gradle ${GRADLE_VERSION:-8.14.3} or use an environment with Gradle preinstalled." >&2
  exit 1
fi

exec gradle "$@"
