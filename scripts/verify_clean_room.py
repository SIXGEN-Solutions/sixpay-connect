from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

REQUIRED = [
    "scripts/verify_baseline.py",
    "frontend/package.json",
    "frontend/scripts/run-fullstack-e2e.mjs",
    "frontend/playwright.fullstack.config.ts",
    "frontend/e2e/fullstack-partner-postgresql.spec.ts",
    "frontend/e2e/fullstack-customer-postgresql.spec.ts",
    "backend/bootstrap/src/test/java/com/sixpay/bootstrap/"
    "integration/persistence/FreshPostgreSqlApplicationIT.java",
]


def fail(message):
    print()
    print("=" * 78)
    print("SIXPAY CLEAN-ROOM VALIDATION FAILED")
    print("=" * 78)
    print()
    print(" -", message)
    sys.exit(1)


def require(relative):
    path = ROOT / relative
    if not path.is_file():
        fail(f"required clean-room asset is missing: {relative}")
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
    engineering = require("ENGINEERING_CONTEXT.md").read_text(
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

    docker = executable("docker")
    npm = executable("npm")
    python = sys.executable

    # Clean-room execution must never depend on an existing SIXPAY database.
    # Docker availability is the only external persistence prerequisite.
    run(
        "0/3 — Docker clean-room preflight",
        [
            docker,
            "info",
        ],
    )

    run(
        "1/3 — Canonical repository baseline verification",
        [
            python,
            "scripts/verify_baseline.py",
        ],
    )

    # This command creates a new uniquely named PostgreSQL container, builds
    # the executable bootstrap JAR, starts the backend with the integration
    # profile, waits for actuator health, starts Angular and executes the
    # full-stack Playwright Partner/Customer persistence journeys.
    run(
        "2/3 — Fresh PostgreSQL full-stack functional smoke",
        [
            npm,
            "run",
            "test:e2e:fullstack",
        ],
        ROOT / "frontend",
    )

    # Explicit final assertion that the canonical full-stack assets still
    # exist after the disposable environment is torn down.
    for relative in [
        "frontend/e2e/fullstack-partner-postgresql.spec.ts",
        "frontend/e2e/fullstack-customer-postgresql.spec.ts",
    ]:
        require(relative)

    print()
    print("=" * 78)
    print("SIXPAY CLEAN-ROOM VALIDATION PASSED")
    print("=" * 78)
    print()
    print("Proved:")
    print(" - repository gates pass from the consolidated baseline")
    print(" - backend Maven verification passes")
    print(" - frontend canonical verification passes")
    print(" - PostgreSQL starts from an empty disposable instance")
    print(" - Flyway applies canonical V100..V800 baselines")
    print(" - Spring Boot application starts successfully")
    print(" - Angular integration frontend starts against the real backend")
    print(" - Partner can be created, persisted and reloaded")
    print(" - Customer can be enrolled, persisted and reloaded")
    print(" - no local pre-existing SIXPAY database is required")
    print()
    print(
        "SIXPAY CONNECT now has an automated reproducible clean-room "
        "baseline proof."
    )


if __name__ == "__main__":
    main()
