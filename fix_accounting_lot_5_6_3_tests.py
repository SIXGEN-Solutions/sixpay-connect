#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
EXPECTED_HEAD = "9b5f1d920731c0a18616e465e617fe7dc1ca2e48"
ROOT = Path(__file__).resolve().parent

TEST_FILE = Path(
    "backend/accounting/src/test/java/com/sixpay/accounting/api/"
    "AccountingBatchQueryControllerTest.java"
)

def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()

def require(condition, message):
    if not condition:
        raise RuntimeError(message)

def main():
    branch = run("git", "branch", "--show-current")
    head = run("git", "rev-parse", "HEAD")

    require(branch == EXPECTED_BRANCH,
            f"Unexpected branch: {branch}. Expected {EXPECTED_BRANCH}")
    require(head == EXPECTED_HEAD,
            f"Unexpected HEAD: {head}. Expected {EXPECTED_HEAD}")

    full = ROOT / TEST_FILE
    require(full.exists(), f"Missing expected file: {TEST_FILE}")
    text = full.read_text(encoding="utf-8")

    replacements = [
        (
'''@WithMockUser(
            username = "admin@sixpay",
            roles = "ADMIN",
            authorities = "SCOPE_accounting.read"
    )''',
'''@WithMockUser(
            username = "admin@sixpay",
            authorities = {
                    "ROLE_ADMIN",
                    "SCOPE_accounting.read"
            }
    )'''
        ),
        (
'''@WithMockUser(
            username = "auditor@sixpay",
            roles = "AUDITOR",
            authorities = "SCOPE_accounting.read"
    )''',
'''@WithMockUser(
            username = "auditor@sixpay",
            authorities = {
                    "ROLE_AUDITOR",
                    "SCOPE_accounting.read"
            }
    )'''
        ),
        (
'''@WithMockUser(
            username = "manager@sixpay",
            roles = "MANAGER",
            authorities = "SCOPE_accounting.read"
    )''',
'''@WithMockUser(
            username = "manager@sixpay",
            authorities = {
                    "ROLE_MANAGER",
                    "SCOPE_accounting.read"
            }
    )'''
        ),
        (
'''@WithMockUser(
            username = "partner@sixpay",
            roles = "PARTNER",
            authorities = "SCOPE_accounting.read"
    )''',
'''@WithMockUser(
            username = "partner@sixpay",
            authorities = {
                    "ROLE_PARTNER",
                    "SCOPE_accounting.read"
            }
    )'''
        ),
    ]

    for old, new in replacements:
        require(old in text, f"Expected annotation not found in {TEST_FILE}:\n{old}")
        text = text.replace(old, new, 1)

    full.write_text(text, encoding="utf-8", newline="\n")
    print(f"[MOD] {TEST_FILE}")
    print()
    print("LOT 5.6.3 test-security fix applied.")
    print("No commit or push performed.")
    print("No clean-worktree guard was applied.")

if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(1)
