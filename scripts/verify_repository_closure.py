#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]

REQUIRED = (
    "scripts/verify_master_prompt_input_manifest.py",
    "scripts/verify_master_engineering_prompt.py",
    "scripts/verify_documentation_contract_references.py",
    "scripts/verify_documentation_final.py",
    "scripts/verify_clean_room.py",
    "scripts/verify_partner_naming_eradication.py",
)


def fail(message):
    print()
    print("=" * 78)
    print("SIXPAY REPOSITORY CLOSURE FAILED")
    print("=" * 78)
    print()
    print(" -", message)
    sys.exit(1)


def require(relative):
    path = ROOT / relative
    if not path.is_file():
        fail(f"required closure asset is missing: {relative}")


def run(label, relative):
    command = [sys.executable, relative]
    print()
    print("=" * 78)
    print(label)
    print("=" * 78)
    print(" ".join(command))
    completed = subprocess.run(command, cwd=ROOT, text=True)
    if completed.returncode != 0:
        fail(f"{label} returned exit code {completed.returncode}")


def main():
    for relative in REQUIRED:
        require(relative)

    run("1/6 — Master Prompt input manifest", "scripts/verify_master_prompt_input_manifest.py")
    run("2/6 — Master Engineering Prompt", "scripts/verify_master_engineering_prompt.py")
    run("3/6 — Documentation / contract references", "scripts/verify_documentation_contract_references.py")
    run("4/6 — Documentation final baseline", "scripts/verify_documentation_final.py")
    run("5/6 — Clean-room baseline + backend/frontend + global full-stack E2E", "scripts/verify_clean_room.py")
    run("6/6 — Final permanent Partner-neutral content/path scan", "scripts/verify_partner_naming_eradication.py")

    print()
    print("=" * 78)
    print("SIXPAY REPOSITORY CLOSURE PASSED")
    print("=" * 78)
    print()
    print("Validated on the selected working revision:")
    print(" - Master Prompt selection and active prompt")
    print(" - documentation and contract-reference integrity")
    print(" - canonical repository baseline")
    print(" - backend unit / architecture verification")
    print(" - frontend format / contracts / lint / coverage / builds")
    print(" - fresh PostgreSQL bootstrap")
    print(" - Partner, Customer, Payment, Accounting and Administration/Identity/Incidents full-stack E2E")
    print(" - final tracked content and path scan for provider-specific naming")
    print()
    print("A closure may be reported only when this command exits with code 0.")


if __name__ == "__main__":
    main()
