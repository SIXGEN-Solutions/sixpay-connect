#!/usr/bin/env bash
set -euo pipefail

DELIVERY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="${1:-$(pwd)}"

python   "${DELIVERY_ROOT}/scripts/apply_complete_corrective.py"   "${REPO_ROOT}"

echo "Run from backend:"
echo "  ./mvnw clean verify -pl payment -am"
echo "  ./mvnw clean verify -pl bootstrap -am"
