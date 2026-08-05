#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT/backend"

echo "[1/4] Customer and Bootstrap verification"
mvn -pl customer,bootstrap -am clean verify

echo "[2/4] Forbidden dependency scan"
if grep -R --line-number \
  --include='*.java' \
  -E 'import com\.sixpay\.payment\.|Amplitude|amplitude' \
  customer/src/main/java/com/sixpay/customer/observation; then
  echo "Forbidden Customer Observation dependency detected."
  exit 1
fi

echo "[3/4] Application/domain framework boundary scan"
if grep -R --line-number \
  --include='*.java' \
  -E 'import org\.springframework\.|import jakarta\.persistence\.|import org\.hibernate\.|Thread\.sleep\(' \
  customer/src/main/java/com/sixpay/customer/observation/application \
  customer/src/main/java/com/sixpay/customer/observation/domain; then
  echo "Framework or blocking retry leaked into application/domain."
  exit 1
fi

echo "[4/4] Documentation presence"
for file in \
  ../documentation/implementation/customer-observation/README.md \
  ../documentation/implementation/customer-observation/E2E-ACCEPTANCE-MATRIX.md \
  ../documentation/implementation/customer-observation/OPERATIONS-RUNBOOK.md \
  ../documentation/implementation/customer-observation/PHASE-CLOSURE-CHECKLIST.md
do
  test -f "$file"
done

echo "Customer Observation phase validation passed."
