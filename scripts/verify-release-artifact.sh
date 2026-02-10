#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_PATH="${1:-}"
MIN_SIZE_BYTES="${MIN_SIZE_BYTES:-10240}"

if [[ -z "$ARTIFACT_PATH" ]]; then
  echo "Usage: $0 <artifact-jar-path>"
  exit 1
fi

if [[ ! -f "$ARTIFACT_PATH" ]]; then
  echo "Artifact does not exist: $ARTIFACT_PATH"
  exit 1
fi

if [[ "$ARTIFACT_PATH" != build/libs/* ]]; then
  echo "Artifact must be in build/libs: $ARTIFACT_PATH"
  exit 1
fi

if [[ "$ARTIFACT_PATH" == *-dev.jar ]]; then
  echo "Refusing to publish development jar: $ARTIFACT_PATH"
  exit 1
fi

ARTIFACT_SIZE=$(stat -c%s "$ARTIFACT_PATH")
if (( ARTIFACT_SIZE < MIN_SIZE_BYTES )); then
  echo "Artifact is too small (${ARTIFACT_SIZE} bytes); expected at least ${MIN_SIZE_BYTES} bytes"
  exit 1
fi

if ! jar tf "$ARTIFACT_PATH" | grep -qx 'fabric.mod.json'; then
  echo "fabric.mod.json missing from artifact: $ARTIFACT_PATH"
  exit 1
fi

FABRIC_MOD_JSON=$(unzip -p "$ARTIFACT_PATH" fabric.mod.json)

ENTRYPOINT_CLASSES=$(python3 - <<'PY' "$FABRIC_MOD_JSON"
import json
import sys

raw = sys.argv[1]
try:
    data = json.loads(raw)
except json.JSONDecodeError as exc:
    print(f"fabric.mod.json is invalid JSON: {exc}", file=sys.stderr)
    raise SystemExit(2)

entrypoints = data.get("entrypoints", {})
if not isinstance(entrypoints, dict):
    print("fabric.mod.json entrypoints field must be an object", file=sys.stderr)
    raise SystemExit(2)

classes = []
for value in entrypoints.values():
    if isinstance(value, str):
        classes.append(value)
        continue
    if isinstance(value, list):
        for item in value:
            if isinstance(item, str):
                classes.append(item)
            elif isinstance(item, dict) and isinstance(item.get("value"), str):
                classes.append(item["value"])

if not classes:
    print("No entrypoint classes declared in fabric.mod.json", file=sys.stderr)
    raise SystemExit(2)

for class_name in classes:
    print(class_name)
PY
)

while IFS= read -r entrypoint; do
  [[ -z "$entrypoint" ]] && continue
  class_file="${entrypoint//./\/}.class"
  if ! jar tf "$ARTIFACT_PATH" | grep -qx "$class_file"; then
    echo "Entrypoint class missing from artifact: $entrypoint ($class_file)"
    exit 1
  fi
done <<< "$ENTRYPOINT_CLASSES"

TMP_DIR=$(mktemp -d)
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

mapfile -t TRACKER_CLASSES < <(jar tf "$ARTIFACT_PATH" | grep '^com/yourname/gtstracker/.*\.class$' || true)

if (( ${#TRACKER_CLASSES[@]} == 0 )); then
  echo "No classes found under com/yourname/gtstracker in artifact: $ARTIFACT_PATH"
  exit 1
fi

LEAKED_MAPPINGS=()
for class_file in "${TRACKER_CLASSES[@]}"; do
  class_output_path="$TMP_DIR/$class_file"
  mkdir -p "$(dirname "$class_output_path")"
  unzip -p "$ARTIFACT_PATH" "$class_file" > "$class_output_path"

  if javap -verbose "$class_output_path" | grep -q 'net/minecraft/'; then
    LEAKED_MAPPINGS+=("$class_file")
  fi
done

if (( ${#LEAKED_MAPPINGS[@]} > 0 )); then
  echo "Named-mapping leakage detected in release artifact bytecode."
  echo "Likely cause: dev jar selected / remap missing."
  echo "Classes containing net/minecraft/ symbols:"
  printf '  - %s\n' "${LEAKED_MAPPINGS[@]}"
  exit 1
fi

echo "Release artifact checks passed: $ARTIFACT_PATH (${ARTIFACT_SIZE} bytes)"
