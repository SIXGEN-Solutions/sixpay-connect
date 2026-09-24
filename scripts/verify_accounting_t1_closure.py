#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

def fail(message):
    print("ACCOUNTING STATIC ARCHITECTURE GATE FAILED")
    print(" - " + message)
    sys.exit(1)

def read(path):
    p = ROOT / path
    if not p.is_file():
        fail(f"required file is missing: {path}")
    return p.read_text(encoding="utf-8")

def main():
    required = [
        "documentation/runbooks/accounting/ACCOUNTING_TFJ_RECONCILIATION.md",
        "backend/accounting/src/main/java/com/sixpay/accounting/infrastructure/tfj/observability/AccountingTfjMetrics.java",
        "backend/accounting/src/main/java/com/sixpay/accounting/infrastructure/tfj/observability/AccountingTfjObservabilityAspect.java",
        "backend/accounting/src/test/java/com/sixpay/accounting/application/service/TfjFinalityPublicationServiceTest.java",
        "backend/tests/src/test/java/com/sixpay/tests/assembled/AccountingT1TfjClosureIT.java",
    ]
    for path in required:
        read(path)

    registry = read("documentation/contracts/CONTRACT_REGISTRY.yaml")
    for contract in (
        "amplitude-accounting-entries-api-v1",
        "amplitude-end-of-day-confirmation-api-v1",
        "partner-payment-status-query-api-v1",
    ):
        if f'id: "{contract}"' not in registry:
            fail(f"required Accounting contract missing from registry: {contract}")


    metrics = read(
        "backend/accounting/src/main/java/com/sixpay/accounting/"
        "infrastructure/tfj/observability/AccountingTfjMetrics.java"
    )
    for metric in (
        "sixpay.accounting.tfj.ingestion",
        "sixpay.accounting.tfj.conflicts",
        "sixpay.accounting.tfj.finality.publication",
        "sixpay.accounting.tfj.finality.pending",
    ):
        if metric not in metrics:
            fail(f"TFJ metric missing: {metric}")

    if '.tag("payment' in metrics or '.tag("confirmation' in metrics:
        fail("high-cardinality Payment/confirmation metric tag introduced")

    prompt = read("MASTER_ENGINEERING_PROMPT.md")
    if "PENDING`, `UNMATCHED` et `AMBIGUOUS` restent non finaux" not in prompt:
        fail("Master Prompt is missing the durable TFJ finality rule")

    runbook = read(
        "documentation/runbooks/accounting/ACCOUNTING_TFJ_RECONCILIATION.md"
    )
    for value in (
        "UNMATCHED",
        "AMBIGUOUS",
        "Identical replay",
        "Conflicting replay",
        "finalityPublishedAt",
        "No public or internal HTTP recovery endpoint",
    ):
        if value not in runbook:
            fail(f"TFJ runbook missing operational case: {value}")


    print("Accounting static architecture gate PASSED.")
    print(" - TFJ observability assets: present")
    print(" - TFJ runbook: present")
    print(" - required Accounting contracts: registered")
    print(" - durable TFJ finality rule: synchronized")
    print(" - dynamic Maven/baseline validation remains separate evidence")

if __name__ == "__main__":
    main()
