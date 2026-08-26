from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path.cwd()

EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

REQUIRED_DOCS = [
    "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",
    "documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml",
]

BACKEND_TESTS = [
    "BootstrapGlobalConfigurationArchitectureTest",
    "DomainConfigurationOwnershipArchitectureTest",
    "RuntimeProfileConfigurationArchitectureTest",
    "SecurityAuthenticationConfigurationArchitectureTest",
    "OpenApiSpringdocConfigurationArchitectureTest",
]

BACKEND_TEST_FILES = [
    "backend/bootstrap/src/test/java/com/sixpay/bootstrap/architecture/"
    + test + ".java"
    for test in BACKEND_TESTS
]

REQUIRED_VERIFIERS = [
    "scripts/verify_spring_configuration_hygiene.py",
    "frontend/scripts/verify-angular-environment-policy.mjs",
    "frontend/scripts/verify-runtime-datasource-policy.mjs",
    "scripts/verify_feature_flag_registry.py",
]

def fail(message):
    print("\nFS-2.5.8 configuration non-regression gate FAILED:\n")
    print(" -", message)
    sys.exit(1)

def require(path):
    absolute = ROOT / path
    if not absolute.is_file():
        fail(f"required configuration baseline artifact is missing: {path}")
    return absolute

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
            f"{label} failed with exit code {completed.returncode}"
        )

def executable(name):
    resolved = shutil.which(name)

    if resolved is None:
        fail(
            f"required executable is not available on PATH: {name}"
        )

    return resolved

def main():
    if not ENGINEERING.is_file():
        fail("ENGINEERING_CONTEXT.md is missing")

    engineering = ENGINEERING.read_text(encoding="utf-8")

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare the authoritative "
            f"branch {EXPECTED_BRANCH}"
        )

    for path in REQUIRED_DOCS:
        require(path)

    for path in BACKEND_TEST_FILES:
        require(path)

    for path in REQUIRED_VERIFIERS:
        require(path)

    mvn = executable("mvn")
    npm = executable("npm")
    python = sys.executable

    print("FS-2.5.8 configuration baseline completeness PASSED.")
    print(
        f"Documents: {len(REQUIRED_DOCS)}; "
        f"backend architecture tests: {len(BACKEND_TESTS)}; "
        f"standalone verifiers: {len(REQUIRED_VERIFIERS)}."
    )

    run(
        "Spring runtime-configuration hygiene",
        [
            python,
            "scripts/verify_spring_configuration_hygiene.py",
        ],
        ROOT,
    )

    run(
        "Feature-flag registry",
        [
            python,
            "scripts/verify_feature_flag_registry.py",
        ],
        ROOT,
    )

    run(
        "Angular environment policy",
        [
            npm,
            "run",
            "verify:angular-environments",
        ],
        ROOT / "frontend",
    )

    run(
        "Frontend runtime datasource policy",
        [
            npm,
            "run",
            "verify:runtime-datasource-policy",
        ],
        ROOT / "frontend",
    )

    test_selector = ",".join(BACKEND_TESTS)

    run(
        "Backend configuration architecture tests",
        [
            mvn,
            "-pl",
            "bootstrap",
            "-am",
            f"-Dtest={test_selector}",
            "-Dsurefire.failIfNoSpecifiedTests=false",
            "test",
        ],
        ROOT / "backend",
    )

    print()
    print("=" * 78)
    print("FS-2.5.8 CONFIGURATION NON-REGRESSION GATE PASSED")
    print("=" * 78)
    print()
    print("Validated:")
    print(" - Bootstrap/global configuration ownership")
    print(" - domain configuration ownership")
    print(" - runtime profile safety")
    print(" - Security/authentication configuration ownership")
    print(" - OpenAPI/Springdoc runtime topology")
    print(" - Angular environment matrix")
    print(" - frontend runtime datasource policy")
    print(" - feature-flag registry ownership")

if __name__ == "__main__":
    main()
