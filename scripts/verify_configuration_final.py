from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path.cwd()

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

REQUIRED_PHASE_DOCS = [
    "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",
    "documentation/architecture/TESTS_AND_GATES.md",
    "documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml",
]

REQUIRED_GATES = [
    "scripts/verify_feature_flag_registry.py",
    "scripts/verify_configuration_consolidation.py",
    "frontend/scripts/verify-angular-environment-policy.mjs",
    "frontend/scripts/verify-runtime-datasource-policy.mjs",
    "frontend/scripts/verify-contract-consolidation.mjs",
    "frontend/scripts/verify-full-stack-conformance.mjs",
    "frontend/scripts/verify-contract-backed-integration.mjs",
]

def fail(message):
    print()
    print("=" * 78)
    print("FS-2.5.9 FINAL VALIDATION FAILED")
    print("=" * 78)
    print()
    print(" -", message)
    sys.exit(1)

def require(relative):
    path = ROOT / relative
    if not path.is_file():
        fail(f"required FS-2.5 asset is missing: {relative}")
    return path

def executable(name):
    value = shutil.which(name)
    if value is None:
        fail(f"required executable is not available on PATH: {name}")
    return value

def run(label, command, cwd):
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

    engineering = ENGINEERING.read_text(encoding="utf-8")

    if "**Authoritative implementation revision:**" not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            "the authoritative revision policy"
        )

    for relative in REQUIRED_PHASE_DOCS:
        require(relative)

    for relative in REQUIRED_GATES:
        require(relative)

    mvn = executable("mvn")
    npm = executable("npm")
    python = sys.executable

    print("FS-2.5.9 baseline completeness PASSED.")
    print(
        f"Phase documents/assets: {len(REQUIRED_PHASE_DOCS)}; "
        f"gate assets: {len(REQUIRED_GATES)}."
    )

    run(
        "1/4 — FS-2.5.8 configuration non-regression gate",
        [
            python,
            "scripts/verify_configuration_consolidation.py",
        ],
        ROOT,
    )

    run(
        "2/4 — Full backend Maven reactor verify",
        [
            mvn,
            "verify",
        ],
        ROOT / "backend",
    )

    run(
        "3/4 — Frontend unit tests",
        [
            npm,
            "test",
        ],
        ROOT / "frontend",
    )

    run(
        "4/4 — Frontend canonical build:all",
        [
            npm,
            "run",
            "build:all",
        ],
        ROOT / "frontend",
    )

    print()
    print("=" * 78)
    print("FS-2.5.9 FINAL VALIDATION PASSED")
    print("=" * 78)
    print()
    print("Validated:")
    print(" - FS-2.5 configuration non-regression gate")
    print(" - full Maven reactor verify")
    print(" - frontend unit tests")
    print(" - Angular environment policy")
    print(" - contract consolidation")
    print(" - frontend runtime datasource policy")
    print(" - frontend full-stack conformance")
    print(" - integration contract-backed policy")
    print(" - integration build")
    print(" - Netlify/demo build")
    print(" - production build")
    print()
    print("FS-2.5 — Configuration consolidation may be CLOSED.")

if __name__ == "__main__":
    main()
