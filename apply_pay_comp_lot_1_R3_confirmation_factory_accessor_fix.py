#!/usr/bin/env python3
from pathlib import Path
import subprocess

EXPECTED_HEAD = "e53450766b495302923f4545080a87c00a682522"

TARGET = Path(
    "backend/payment/src/main/java/com/sixpay/payment/application/confirmation/"
    "PaymentConfirmationChallengeFactory.java"
)

def run(*args: str) -> str:
    completed = subprocess.run(
        args,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"Command failed ({completed.returncode}): {' '.join(args)}\n"
            f"{completed.stdout}\n{completed.stderr}"
        )
    return completed.stdout.strip()

def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)

def main() -> None:
    repo_root = Path(run("git", "rev-parse", "--show-toplevel")).resolve()
    require(
        repo_root == Path.cwd().resolve(),
        "Run this script from the repository root."
    )

    head = run("git", "rev-parse", "HEAD")
    require(
        head == EXPECTED_HEAD,
        f"Expected HEAD {EXPECTED_HEAD}, actual={head}. No file modified."
    )

    require(TARGET.is_file(), f"Missing file: {TARGET}")

    text = TARGET.read_text(encoding="utf-8")
    original = text

    requested_count = text.count("payment.requestedAmount()")
    updated_count = text.count("payment.updatedAt()")

    require(
        requested_count in (0, 1),
        f"Unexpected payment.requestedAmount() occurrences: {requested_count}"
    )
    require(
        updated_count in (0, 2),
        f"Unexpected payment.updatedAt() occurrences: {updated_count}"
    )

    text = text.replace(
        "payment.requestedAmount()",
        "payment.toState().requestedAmount()"
    )
    text = text.replace(
        "payment.updatedAt()",
        "payment.toState().updatedAt()"
    )

    require(
        "payment.requestedAmount()" not in text,
        "payment.requestedAmount() still present after replacement."
    )
    require(
        "payment.updatedAt()" not in text,
        "payment.updatedAt() still present after replacement."
    )

    if text == original:
        print("UNCHANGED: factory already uses PaymentState accessors.")
        return

    TARGET.write_text(text, encoding="utf-8")

    print("PaymentConfirmationChallengeFactory accessor fix applied.")
    print(f"HEAD validated: {EXPECTED_HEAD}")
    print(f"Updated: {TARGET}")
    print("No commit, push or PR performed.")

if __name__ == "__main__":
    main()
