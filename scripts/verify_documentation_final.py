from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

REQUIRED = [
    "documentation/README.md",
    "documentation/architecture/README.md",
    "documentation/architecture/MODULE_BOUNDARIES.md",
    "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",
    "documentation/architecture/TESTS_AND_GATES.md",
    "documentation/architecture/REPOSITORY_HYGIENE.md",
    "documentation/requirements/README.md",
    "documentation/domains/README.md",
    "documentation/contracts/README.md",
    "documentation/contracts/CONTRACT_REGISTRY.yaml",
    "documentation/runbooks/README.md",
    "documentation/ai/README.md",
    "scripts/verify_documentation_baseline.py",
    "scripts/verify_configuration_consolidation.py",
    "frontend/scripts/verify-contract-consolidation.mjs",
]


def fail(message):
    print()
    print("=" * 78)
    print("FS-2.7.8 FINAL DOCUMENTATION VALIDATION FAILED")
    print("=" * 78)
    print()
    print(" -", message)
    sys.exit(1)


def require(relative):
    path = ROOT / relative
    if not path.is_file():
        fail(f"required FS-2.7 asset is missing: {relative}")
    return path


def executable(name):
    value = shutil.which(name)
    if value is None:
        fail(f"required executable is not available on PATH: {name}")
    return value


def run(label, command, cwd=ROOT):
    print()
    print("=" * 78)
    print(label)
    print("=" * 78)
    print(" ".join(command))

    completed = subprocess.run(
        command,
        cwd=cwd,
        text=True,
    )

    if completed.returncode != 0:
        fail(
            f"{label} returned exit code "
            f"{completed.returncode}"
        )


def main():
    if not ENGINEERING.is_file():
        fail("ENGINEERING_CONTEXT.md is missing")

    engineering = ENGINEERING.read_text(
        encoding="utf-8",
        errors="ignore",
    )

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            + EXPECTED_BRANCH
        )

    for relative in REQUIRED:
        require(relative)

    python = sys.executable
    npm = executable("npm")

    print("FS-2.7.8 documentation final-validation prerequisites PASSED.")
    print(f"Required canonical assets: {len(REQUIRED)}.")

    run(
        "1/3 — Documentation non-regression baseline",
        [
            python,
            "scripts/verify_documentation_baseline.py",
        ],
    )

    run(
        "2/3 — Canonical contract registry / consolidation integrity",
        [
            npm,
            "run",
            "verify:contract-consolidation",
        ],
        ROOT / "frontend",
    )

    run(
        "3/3 — Configuration documentation / gate alignment",
        [
            python,
            "scripts/verify_configuration_consolidation.py",
        ],
    )

    print()
    print("=" * 78)
    print("FS-2.7.8 FINAL DOCUMENTATION VALIDATION PASSED")
    print("=" * 78)
    print()
    print("Validated:")
    print(" - canonical documentation navigation")
    print(" - architecture current-state documents")
    print(" - requirements/domain documentation ownership")
    print(" - contracts/runbooks reference integrity")
    print(" - AI documentation precedence and Partner golden-module rule")
    print(" - absorbed historical FS documentation remains absent")
    print(" - contract registry remains structurally valid")
    print(" - configuration gates no longer depend on deleted phase documents")
    print()
    print("FS-2.7 — Documentation consolidation may be CLOSED.")


if __name__ == "__main__":
    main()
