#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
BASE = "28b00d72c05d3b5cebc184d26d08486ba92925c5"

def fail(message):
    print("ACCOUNTING_T1 CLOSURE GATE FAILED")
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
        "documentation/ai/accounting/ACCOUNTING_T1_AI_CONTEXT.md",
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
        "tresorpay-payment-status-query-api-v1",
    ):
        if f'id: "{contract}"' not in registry:
            fail(f"required Accounting contract missing from registry: {contract}")

    ai = read("documentation/ai/accounting/ACCOUNTING_T1_AI_CONTEXT.md")
    forbidden_ai = [
        "## TO_DEFINE / DEFERRED",
        "Accounting candidate persistence schema for T1.2",
        "physical Core Banking T1 contract is still undefined",
    ]
    for value in forbidden_ai:
        if value in ai:
            fail(f"stale Accounting AI context remains: {value}")

    required_ai = [
        "amplitude-accounting-entries-api-v1.yaml",
        "amplitude-end-of-day-confirmation-api-v1.yaml",
        "sixpay.accounting.tfj.finality.pending",
        "IMPLEMENTED_PENDING_FINAL_VALIDATION",
        "REFERENCE_MVP",
    ]
    for value in required_ai:
        if value not in ai:
            fail(f"final Accounting AI context missing: {value}")

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
    if f"| Commit source | `{BASE}` |" not in prompt:
        fail("Master Prompt source revision is not synchronized to T1.7 base")
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

    head = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if head.returncode != 0:
        fail("Git HEAD cannot be observed")

    source_ancestor = subprocess.run(
        ["git", "merge-base", "--is-ancestor", BASE, "HEAD"],
        cwd=ROOT,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if source_ancestor.returncode != 0:
        fail("T1.7 source revision is not an ancestor of HEAD")

    print("ACCOUNTING_T1 closure static gate PASSED.")
    print(" - TFJ observability assets: present")
    print(" - TFJ runbook: present")
    print(" - Accounting AI context: consolidated")
    print(" - Master Prompt source/rules: synchronized")
    print(" - T1.1 reference-only restriction: preserved")
    print(" - dynamic Maven/baseline/clean-room validation remains separate evidence")

if __name__ == "__main__":
    main()
