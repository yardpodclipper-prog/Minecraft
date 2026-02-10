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


python3 - <<'PYCODE' "$ARTIFACT_PATH"
import re
import sys
import zipfile

artifact = sys.argv[1]
# Named Yarn classes use package paths like net/minecraft/text/Text.
# Intermediary runtime classes are typically net/minecraft/class_<id>.
named_minecraft_pattern = re.compile(rb"net/minecraft/(?!class_)[a-z0-9_]+/[A-Z][A-Za-z0-9_$]*")

violations = []
with zipfile.ZipFile(artifact) as zf:
    for name in zf.namelist():
        if not name.startswith("com/yourname/gtstracker/") or not name.endswith('.class'):
            continue
        data = zf.read(name)
        if named_minecraft_pattern.search(data):
            violations.append(name)

if violations:
    sample = ', '.join(violations[:5])
    print(
        "Detected named-mapping Minecraft class references in release artifact classes "
        f"(sample: {sample}). This usually means a dev jar was selected or remapping failed.",
        file=sys.stderr,
    )
    raise SystemExit(1)
PYCODE

echo "Release artifact checks passed: $ARTIFACT_PATH (${ARTIFACT_SIZE} bytes)"
