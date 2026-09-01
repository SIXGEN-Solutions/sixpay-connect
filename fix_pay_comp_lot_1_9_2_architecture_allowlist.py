#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
EXPECTED_HEAD = "e9af93cd0e96e5f843a90c7137f5cf00dd8b1f1a"

ARCH_TEST = Path(
    "backend/payment/src/test/java/com/sixpay/payment/"
    "architecture/PaymentFoundationArchitectureTest.java"
)
REVOCATION_SERVICE = Path(
    "backend/payment/src/main/java/com/sixpay/payment/"
    "application/service/PaymentConfirmationRevocationService.java"
)


def run(*args: str, check: bool = True) -> str:
    proc = subprocess.run(
        list(args),
        check=check,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return proc.stdout.strip()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def main() -> None:
    require(Path(".git").exists(), "Run from repository root.")
    require(ARCH_TEST.is_file(), f"Missing file: {ARCH_TEST}")
    require(
        REVOCATION_SERVICE.is_file(),
        "PaymentConfirmationRevocationService.java is not present locally. "
        "Apply LOT 1.9.2 first.",
    )

    branch = run("git", "branch", "--show-current")
    require(
        branch == EXPECTED_BRANCH,
        f"Unexpected branch: {branch}. Expected {EXPECTED_BRANCH}",
    )

    local_head = run("git", "rev-parse", "HEAD")
    require(
        local_head == EXPECTED_HEAD,
        f"Unexpected local HEAD: {local_head}. Expected {EXPECTED_HEAD}",
    )

    remote = run(
        "git",
        "ls-remote",
        "origin",
        f"refs/heads/{EXPECTED_BRANCH}",
    )
    require(remote, f"Remote branch not found: {EXPECTED_BRANCH}")
    remote_head = remote.split()[0]
    require(
        remote_head == EXPECTED_HEAD,
        "Remote HEAD moved since this corrective script was generated.\n"
        f"Observed: {remote_head}\n"
        f"Expected: {EXPECTED_HEAD}\n"
        "Regenerate the corrective patch against the latest GitHub HEAD.",
    )

    arch_status = run(
        "git",
        "status",
        "--porcelain",
        "--",
        str(ARCH_TEST),
    )
    require(
        not arch_status,
        "Architecture test already contains local changes. "
        "Aborting to preserve user work:\n" + arch_status,
    )

    service_text = REVOCATION_SERVICE.read_text(encoding="utf-8")
    require(
        "class PaymentConfirmationRevocationService" in service_text,
        "Local revocation service does not match LOT 1.9.2.",
    )

    text = ARCH_TEST.read_text(encoding="utf-8")
    old = '''                "PaymentAuthorizationService.java",
                "PaymentConfirmationService.java",
'''
    new = '''                "PaymentAuthorizationService.java",
                "PaymentConfirmationRevocationService.java",
                "PaymentConfirmationService.java",
'''
    count = text.count(old)
    require(
        count == 1,
        f"Expected one architecture allowlist insertion point, found {count}",
    )

    text = text.replace(old, new, 1)
    ARCH_TEST.write_text(text, encoding="utf-8", newline="\n")

    run("git", "diff", "--check", "--", str(ARCH_TEST))

    print("LOT 1.9.2 architecture allowlist correction applied.")
    print(f"Modified: {ARCH_TEST}")
    print("git diff --check: PASSED")
    print()
    print("Recommended targeted gate from backend/:")
    print("mvn -pl payment -Dtest=PaymentFoundationArchitectureTest,PaymentConfirmationRevocationServiceTest test")
    print()
    print("Then resume:")
    print("mvn -pl payment -am verify")


if __name__ == "__main__":
    main()
