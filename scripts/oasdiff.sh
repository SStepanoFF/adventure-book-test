#!/usr/bin/env bash
# Contract test, level 2 (spec vs spec): fails when the CURRENT OpenAPI document contains BREAKING changes
# compared with the approved baseline (contract/baseline/openapi.yaml). Needs Docker.
#
# Usage (from the tests folder):  bash scripts/oasdiff.sh [path/to/current/openapi.yaml]
# After an INTENTIONAL API change: copy the new openapi.yaml over contract/baseline/openapi.yaml and commit it.
set -euo pipefail

# Автоматично визначаємо корінь модуля (папку, де лежить скрипт, рівнем вище)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

CURRENT="${1:-$PROJECT_ROOT/src/test/resources/openapi/openapi.yaml}"
BASELINE="$PROJECT_ROOT/contract/baseline/openapi.yaml"

cd "$PROJECT_ROOT"
mkdir -p target
cp "$CURRENT" target/oasdiff-current.yaml   # the container only sees the current directory

echo "== All changes (informational) =="
docker run --rm -v "$PWD":/work -w /work tufin/oasdiff changelog contract/baseline/openapi.yaml target/oasdiff-current.yaml || true

echo "== Breaking changes (fails the build) =="
docker run --rm -v "$PWD":/work -w /work tufin/oasdiff breaking contract/baseline/openapi.yaml target/oasdiff-current.yaml --fail-on ERR