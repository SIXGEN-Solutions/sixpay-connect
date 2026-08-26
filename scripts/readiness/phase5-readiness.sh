#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(
  cd "$(dirname "${BASH_SOURCE[0]}")"
  pwd
)"
REPO_ROOT="$(
  cd "${SCRIPT_DIR}/../.."
  pwd
)"
BACKEND="${REPO_ROOT}/backend"
REPORT_DIR="${REPO_ROOT}/target/readiness"
REPORT="${REPORT_DIR}/phase5-readiness-report.md"

mkdir -p "${REPORT_DIR}"

echo "==> Full backend verification"
(
  cd "${BACKEND}"
  mvn -U clean verify
)

echo "==> Phase 5 readiness guards"
(
  cd "${BACKEND}"
  mvn -U -pl bootstrap -am \
    -Dtest='com.sixpay.bootstrap.readiness.*' \
    test
)

required=(
  TRESORPAY_BASE_URL
  TRESORPAY_CLIENT_ID
  AMPLITUDE_BASE_URL
  AMPLITUDE_TOKEN_URL
  AMPLITUDE_CLIENT_ID
  ACCOUNTING_API_BASE_URL
  ACCOUNTING_API_CLIENT_ID
  SIXPAY_NOTIFICATION_OPERATIONS_ADMIN_EMAIL
)

missing=()

for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    missing+=("${name}")
  fi
done

if (( ${#missing[@]} == 0 )); then
  sandbox="INPUTS_PRESENT_NOT_YET_CERTIFIED"
else
  sandbox="BLOCKED_EXTERNAL"
fi

{
  echo "# Phase 5 readiness report"
  echo
  echo "- Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  echo "- MODULAR_MONOLITH_READINESS: PASS"
  echo "- EXTERNAL_SANDBOX_CERTIFICATION: ${sandbox}"
  echo "- PRODUCTION_READINESS: NOT_ASSERTED"
  echo
  echo "## External inputs"

  if (( ${#missing[@]} == 0 )); then
    echo "- Required environment variables are present."
    echo "- Their correctness still requires sandbox certification."
  else
    for name in "${missing[@]}"; do
      echo "- MISSING: \`${name}\`"
    done
  fi

  echo
  echo "## Important"
  echo
  echo "Secret values are never printed."
  echo "A modular-monolith PASS does not imply external-provider certification."
} > "${REPORT}"

echo "Readiness report: ${REPORT}"
echo "MODULAR_MONOLITH_READINESS=PASS"
echo "EXTERNAL_SANDBOX_CERTIFICATION=${sandbox}"
