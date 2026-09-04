#!/usr/bin/env python3
"""
SIXPAY CONNECT — COMPLETE rollback LOT 2.1, v2 (Option A)

Important correction:
This version does NOT assume package paths for baseline files.
It resolves every pre-existing target by its UNIQUE basename directly from
the authoritative Git tree at:
    d092eddc2ef2b5b6f180808356c35a2e286ae2a8

No branch/worktree-cleanliness checks.
No git add / commit / push.
A local backup is created before mutation.
"""

from __future__ import annotations

from pathlib import Path
import datetime as dt
import shutil
import subprocess
import sys

BASE_SHA = "d092eddc2ef2b5b6f180808356c35a2e286ae2a8"
SCOPE = "backend/payment"

# Pre-existing files modified/deleted by our LOT 2.1 scripts.
# Paths are intentionally NOT hard-coded: they are resolved from BASE_SHA.
BASELINE_BASENAMES = [
    "PaymentAuthorizationUseCase.java",
    "PaymentAuthorizationService.java",
    "RecordAuthorizationDecisionCommand.java",
    "Payment.java",
    "PaymentState.java",
    "PaymentStateDocument.java",
    "PaymentAggregateTestFixtures.java",
    "PaymentPreFinancialLifecycleTest.java",
    "PaymentPendingConfirmationLifecycleTest.java",
    "PaymentStateDocumentV4Test.java",
    "PaymentStateDocumentSchemaArchitectureTest.java",
]

# README is treated separately because basename README.md is not unique repository-wide.
EXACT_BASELINE_PATHS = [
    "backend/payment/README.md",
]

# Files introduced by LOT 2.1 and absent from the authoritative baseline.
LOT21_ADDED = [
    "backend/payment/src/main/java/com/sixpay/payment/application/command/RecordSixpayAuthorizationDecisionCommand.java",
    "backend/payment/src/main/java/com/sixpay/payment/domain/model/authorization/SixpayAuthorizationCheck.java",
    "backend/payment/src/main/java/com/sixpay/payment/domain/model/authorization/SixpayAuthorizationCheckResult.java",
    "backend/payment/src/main/java/com/sixpay/payment/domain/model/authorization/SixpayAuthorizationCheckEvidence.java",
    "backend/payment/src/main/java/com/sixpay/payment/domain/model/authorization/SixpayAuthorizationDecision.java",
    "backend/payment/src/main/java/com/sixpay/payment/domain/model/authorization/SixpayAuthorizationDecisionSnapshot.java",
]


def find_root(start: Path) -> Path:
    cur = start.resolve()
    for p in [cur, *cur.parents]:
        if (p / ".git").exists():
            return p
    raise RuntimeError("Run this script from inside the sixpay-connect repository.")


def git(root: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    p = subprocess.run(
        ["git", *args],
        cwd=root,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        encoding="utf-8",
    )
    if check and p.returncode != 0:
        raise RuntimeError(
            f"git {' '.join(args)} failed ({p.returncode})\n{p.stderr.strip()}"
        )
    return p


root = find_root(Path.cwd())
git(root, "cat-file", "-e", f"{BASE_SHA}^{{commit}}")

# Read authoritative tree, then resolve target basenames from the tree itself.
tree_lines = [
    line.strip().replace("\\", "/")
    for line in git(root, "ls-tree", "-r", "--name-only", BASE_SHA, "--", SCOPE).stdout.splitlines()
    if line.strip()
]

resolved: dict[str, str] = {}
errors: list[str] = []

for basename in BASELINE_BASENAMES:
    matches = [p for p in tree_lines if Path(p).name == basename]
    if len(matches) != 1:
        errors.append(
            f"{basename}: expected exactly one match in {BASE_SHA}:{SCOPE}, found {len(matches)} -> {matches}"
        )
    else:
        resolved[basename] = matches[0]

for rel in EXACT_BASELINE_PATHS:
    if rel not in tree_lines:
        errors.append(f"{rel}: not present in authoritative tree")

# Prove LOT 2.1-added files were absent at baseline.
for rel in LOT21_ADDED:
    if rel in tree_lines:
        errors.append(f"{rel}: unexpectedly exists at authoritative baseline")

if errors:
    print("ABORTED BEFORE ANY WRITE")
    for e in errors:
        print(f"  - {e}")
    sys.exit(2)

restore_paths = list(resolved.values()) + EXACT_BASELINE_PATHS

print("Resolved authoritative paths:")
for basename in BASELINE_BASENAMES:
    print(f"  {basename} -> {resolved[basename]}")
for rel in EXACT_BASELINE_PATHS:
    print(f"  {rel}")

# Load all authoritative contents BEFORE mutation.
baseline: dict[str, str] = {}
for rel in restore_paths:
    baseline[rel] = git(root, "show", f"{BASE_SHA}:{rel}").stdout

# Backup every affected existing file.
stamp = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
backup_root = root / ".sixpay-local-backups" / f"complete_lot_2_1_rollback_v2_{stamp}"

for rel in restore_paths + LOT21_ADDED:
    src = root / rel
    if src.exists():
        dst = backup_root / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)

# Restore baseline files.
for rel, content in baseline.items():
    dst = root / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(content, encoding="utf-8", newline="\n")

# Remove only files proven absent at BASE_SHA.
deleted = []
for rel in LOT21_ADDED:
    p = root / rel
    if p.exists():
        p.unlink()
        deleted.append(rel)

# Verify exact text equivalence for restored files.
verification_errors = []
for rel, expected in baseline.items():
    p = root / rel
    if not p.exists():
        verification_errors.append(f"{rel}: missing after restore")
    elif p.read_text(encoding="utf-8") != expected:
        verification_errors.append(f"{rel}: differs from authoritative baseline after restore")

for rel in LOT21_ADDED:
    if (root / rel).exists():
        verification_errors.append(f"{rel}: LOT 2.1-added file still exists")

# Cross-file semantic consistency checks specifically covering the failures seen.
payment_state_path = resolved["PaymentState.java"]
state_doc_path = resolved["PaymentStateDocument.java"]

payment_state = (root / payment_state_path).read_text(encoding="utf-8")
state_doc = (root / state_doc_path).read_text(encoding="utf-8")

required_checks = [
    (
        "AuthorizationEvidenceSnapshot authorizationEvidence",
        payment_state,
        "PaymentState baseline authorizationEvidence field missing",
    ),
    (
        "authorizationEvidence()",
        payment_state,
        "PaymentState authorizationEvidence() accessor missing",
    ),
    (
        "authorizationEvidence(AuthorizationEvidenceSnapshot",
        payment_state,
        "PaymentState.Builder authorizationEvidence(...) missing",
    ),
    (
        "CURRENT_SCHEMA_VERSION = 4",
        state_doc,
        "PaymentStateDocument is not schema v4",
    ),
]

for marker, content, message in required_checks:
    if marker not in content:
        verification_errors.append(message)

if "SixpayAuthorizationDecisionSnapshot" in payment_state:
    verification_errors.append("SixpayAuthorizationDecisionSnapshot remains in PaymentState")
if "SixpayAuthorizationDecisionSnapshot" in state_doc:
    verification_errors.append("SixpayAuthorizationDecisionSnapshot remains in PaymentStateDocument")

if verification_errors:
    print()
    print("ROLLBACK VERIFICATION FAILED")
    for e in verification_errors:
        print(f"  - {e}")
    print(f"Backup retained at: {backup_root}")
    sys.exit(3)

print()
print("COMPLETE LOT 2.1 ROLLBACK V2 SUCCESSFUL")
print(f"Authoritative baseline: {BASE_SHA}")
print(f"Backup: {backup_root}")
print(f"Restored files: {len(restore_paths)}")
print(f"Deleted LOT 2.1-added files: {len(deleted)}")
print()
print("Verified coherent baseline:")
print("  PaymentState.authorizationEvidence restored")
print("  PaymentState.Builder.authorizationEvidence(...) restored")
print("  PaymentStateDocument schema v4 restored")
print("  experimental SixpayAuthorizationDecisionSnapshot removed from core state/persistence")
print()
print("No branch operation, git add, commit or push was performed.")
print()
print("Next:")
print("  mvn -pl backend/payment -am test")
print("  git diff --check")
print(f"  git diff --name-status {BASE_SHA} -- backend/payment")
