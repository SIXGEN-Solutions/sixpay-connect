#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"

echo "== SIXPAY CONNECT Payment LOT 3 final validation =="
echo "Repository root: ${ROOT_DIR}"

cd "${BACKEND_DIR}"

echo
echo "[1/4] Targeted T0 orchestration/recovery/architecture tests"
mvn -pl payment -am \
  -Dtest=PaymentPostPersistenceOrchestrationServiceTest,PaymentT0RecoveryServiceTest,AmplitudePaymentEventRecoveryAdapterTest,PaymentEventLifecycleExecutionGuardTest,PaymentT0ClosureArchitectureTest,PaymentFoundationArchitectureTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test

echo
echo "[2/4] Payment module unit test gate"
mvn -pl payment -am test

echo
echo "[3/4] Payment integration/full verification gate"
mvn -pl payment -am -Pfull-tests clean verify

echo
echo "[4/4] Explicit LOT 2.9/3 concurrency evidence"
mvn -pl payment \
  -Dtest=PaymentFinancialSnapshotConcurrencyIT \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test

echo
echo "PAYMENT LOT 3 FINAL VALIDATION PASSED"
