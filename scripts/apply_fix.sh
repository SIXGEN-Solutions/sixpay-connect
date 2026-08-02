#!/usr/bin/env bash
set -euo pipefail

DELIVERY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="${1:-$(pwd)}"

python "${DELIVERY_ROOT}/scripts/apply_fix.py" "${REPO_ROOT}"
rm -rf "${REPO_ROOT}/backend/payment/target"

echo "Correction applied."
echo "Run from backend/: ./mvnw clean verify -pl payment -am"
