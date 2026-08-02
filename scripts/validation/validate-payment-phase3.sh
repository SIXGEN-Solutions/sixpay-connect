#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_ROOT="${REPOSITORY_ROOT}/backend"

echo "== SIXPAY CONNECT / Payment Phase 3 final validation =="
echo "Repository: ${REPOSITORY_ROOT}"

cd "${BACKEND_ROOT}"

echo "== Build environment =="
java -version
mvn --version

echo "== Unit, architecture and contract validation =="
mvn --batch-mode --no-transfer-progress \
  clean verify \
  -pl payment \
  -am

echo "== Integration, concurrency and coverage validation =="
mvn --batch-mode --no-transfer-progress \
  clean verify \
  -Pfull-tests,coverage \
  -pl payment \
  -am

echo "== Required report checks =="
test -d payment/target/surefire-reports
test -d payment/target/failsafe-reports
test -f payment/target/site/jacoco/jacoco.xml

echo "Payment Phase 3 validation completed successfully."
