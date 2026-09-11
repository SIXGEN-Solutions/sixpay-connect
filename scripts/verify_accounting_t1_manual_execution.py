#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

def fail(message):
    print(f"[FAIL] {message}")
    sys.exit(1)

def require_text(path, *tokens):
    content = (ROOT / path).read_text(encoding="utf-8")
    for token in tokens:
        if token not in content:
            fail(f"{path}: missing {token!r}")

for path in [
    "documentation/contracts/internal/accounting-t1-manual-execution-api-v1.yaml",
    "backend/accounting/src/main/java/com/sixpay/accounting/api/AccountingT1ManualExecutionController.java",
    "backend/accounting/src/main/java/com/sixpay/accounting/application/service/AccountingT1ManualExecutionService.java",
    "frontend/src/app/features/accounting/models/accounting-t1-execution.ts",
    "frontend/e2e/accounting-t1-manual-execution.spec.ts",
]:
    if not (ROOT / path).is_file():
        fail(f"required file missing: {path}")

for path in [
    "documentation/contracts/external/accounting/accounting-batch-api-v1.yaml",
    "documentation/contracts/external/accounting/accounting-batch-request-v1.schema.json",
    "documentation/contracts/external/accounting/accounting-batch-response-v1.schema.json",
]:
    if (ROOT / path).exists():
        fail(f"superseded provisional contract still present: {path}")

require_text(
    "documentation/contracts/CONTRACT_REGISTRY.yaml",
    'id: "accounting-t1-manual-execution-api-v1"',
    'capability: "ACCOUNTING_T1_MANUAL_EXECUTION"',
    '"accounting.t1.execute"',
)
require_text(
    "backend/security/src/main/java/com/sixpay/security/authorization/SixpayPermission.java",
    'ACCOUNTING_T1_EXECUTE("accounting.t1.execute")',
)
require_text(
    "backend/accounting/src/main/java/com/sixpay/accounting/api/AccountingT1ManualExecutionController.java",
    "hasAnyRole('ADMIN', 'MANAGER')",
    "SCOPE_accounting.t1.execute",
)
require_text(
    "frontend/src/app/features/accounting/components/accounting-overview-page.component.ts",
    "Lancer le traitement T1",
    "accounting.t1.execute",
)

print("[PASS] Accounting T1 manual execution baseline")
