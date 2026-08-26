from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

REQUIRED = [
    "frontend/package.json",
    "scripts/verify_repository_hygiene.py",
    "scripts/verify_spring_configuration_hygiene.py",
    "scripts/verify_feature_flag_registry.py",
    "frontend/scripts/verify-angular-environment-policy.mjs",
    "frontend/scripts/verify-runtime-datasource-policy.mjs",
    "frontend/scripts/verify-contract-consolidation.mjs",
    "frontend/scripts/verify-full-stack-conformance.mjs",
    "frontend/scripts/verify-contract-backed-integration.mjs",
    "backend/bootstrap/src/test/java/com/sixpay/bootstrap/"
    "integration/persistence/FreshPostgreSqlApplicationIT.java",
]

def fail(message):
    print()
    print("=" * 78)
    print("SIXPAY BASELINE VERIFICATION FAILED")
    print("=" * 78)
    print()
    print(" -", message)
    sys.exit(1)

def executable(name):
    value = shutil.which(name)
    if value is None:
        fail(f"required executable is not available on PATH: {name}")
    return value

def require(relative):
    path = ROOT / relative
    if not path.is_file():
        fail(f"required baseline asset is missing: {relative}")
    return path

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

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            f"{EXPECTED_BRANCH}"
        )

    for relative in REQUIRED:
        require(relative)

    mvn = executable("mvn")
    npm = executable("npm")
    python = sys.executable

    run(
        "1/6 — Repository hygiene",
        [
            python,
            "scripts/verify_repository_hygiene.py",
        ],
        ROOT,
    )

    run(
        "2/6 — Spring runtime-configuration hygiene",
        [
            python,
            "scripts/verify_spring_configuration_hygiene.py",
        ],
        ROOT,
    )

    run(
        "3/6 — Configuration / feature-flag registry",
        [
            python,
            "scripts/verify_feature_flag_registry.py",
        ],
        ROOT,
    )

    run(
        "4/6 — Backend canonical verification",
        [
            mvn,
            "verify",
        ],
        ROOT / "backend",
    )

    run(
        "5/6 — Frontend canonical verification",
        [
            npm,
            "run",
            "verify:sixpay",
        ],
        ROOT / "frontend",
    )

    run(
        "6/6 — Fresh PostgreSQL canonical bootstrap",
        [
            mvn,
            "-pl",
            "bootstrap",
            "-am",
            "-Pfull-tests",
            "-DskipITs=false",
            "-Dit.test=FreshPostgreSqlApplicationIT",
            "-Dfailsafe.failIfNoSpecifiedTests=false",
            "verify",
        ],
        ROOT / "backend",
    )

    print()
    print("=" * 78)
    print("SIXPAY BASELINE VERIFICATION PASSED")
    print("=" * 78)
    print()
    print("Validated:")
    print(" - tracked repository hygiene and artifact classification")
    print(" - Spring runtime-configuration ownership and deduplication")
    print(" - configuration/feature-flag registry")
    print(" - full backend unit/architecture reactor verify")
    print(" - frontend lint/tests/build and conformance gates")
    print(" - contract consolidation via frontend build:all")
    print(" - runtime datasource policy")
    print(" - full-stack static conformance")
    print(" - integration contract-backed policy")
    print(" - fresh PostgreSQL V100..V800 bootstrap")
    print()
    print("Canonical commands:")
    print(" - backend:  mvn verify")
    print(" - frontend: npm run verify:sixpay")
    print(" - repo:     py scripts/verify_baseline.py")

if __name__ == "__main__":
    main()
