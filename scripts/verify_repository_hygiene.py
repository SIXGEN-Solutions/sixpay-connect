from collections import defaultdict
from hashlib import sha256
from pathlib import Path
import subprocess
import sys


ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

REQUIRED = [
    "ENGINEERING_CONTEXT.md",
    "documentation/architecture/REPOSITORY_HYGIENE.md",
    "documentation/contracts/CONTRACT_REGISTRY.yaml",
    "backend/customer/CUSTOMER-TEST-COVERAGE.md",
]

FORBIDDEN_EXACT_PATHS = {
    ".gitlab-ci.yml",
    "CHANGELOG.md",
    "LICENSE.md",
    "frontend/DA-10.5-authentication-service.patch.md",
}

FORBIDDEN_PREFIXES = (
    "backend/subscription/",
    "contracts/",
    "deployment/",
    "tools/",
    "scripts/infrastructure/",
)

GENERATED_SEGMENTS = {
    "node_modules",
    "target",
    "dist",
    "coverage",
    "test-results",
    "playwright-report",
    "playwright-report-integration",
}

FORBIDDEN_SUFFIXES = (
    ".bak",
    ".class",
    ".jar",
    ".log",
    ".orig",
    ".pyc",
    ".rej",
    ".tmp",
    ".war",
    ".zip",
)

STALE_BRANCHES = (
    "feat/sixpay-test-validate-pilote",
    "feat/hybrid-authentification-system",
    "feat/sixpay-customer-management-baseline",
    "feat/sixpay-pilot-hybrid-consolidated",
    "feat/integration-contracts",
    "feat/internal-audit-query",
)

ALLOWED_DUPLICATE_GROUPS = {
    frozenset({
        "backend/payment/src/main/resources/openapi/payment-command-api-v1.yaml",
        "documentation/contracts/external/payment-command-api-v1.yaml",
    }),
    frozenset({
        "backend/payment/src/main/resources/openapi/payment-query-api-v1.yaml",
        "documentation/contracts/internal/payment-query-api-v1.yaml",
    }),
    frozenset({
        "documentation/architecture/tresorpay/"
        "Cahier des Charges interopérabilité entre TRESOR PAY et le "
        "core banking_final_schema_final.docx",
        "documentation/requirements/cdc/"
        "Cahier des Charges interopérabilité entre TRESOR PAY et le "
        "core banking_final_schema_final.docx",
    }),
    frozenset({"frontend/.node-version", "frontend/.nvmrc"}),
}


def fail(errors):
    print()
    print("=" * 78)
    print("FS-2.9 REPOSITORY HYGIENE GATE FAILED")
    print("=" * 78)
    print()
    for error in sorted(set(errors)):
        print(" -", error)
    sys.exit(1)


def tracked_files():
    candidate = subprocess.run(
        [
            "git",
            "ls-files",
            "-z",
            "--cached",
            "--others",
            "--exclude-standard",
        ],
        cwd=ROOT,
        check=True,
        capture_output=True,
    )
    deleted = subprocess.run(
        ["git", "ls-files", "-z", "--deleted"],
        cwd=ROOT,
        check=True,
        capture_output=True,
    )
    deleted_files = {
        value.decode("utf-8")
        for value in deleted.stdout.split(b"\0")
        if value
    }
    return [
        value.decode("utf-8")
        for value in candidate.stdout.split(b"\0")
        if value and value.decode("utf-8") not in deleted_files
    ]


def is_generated(relative):
    parts = Path(relative).parts
    return any(
        part in GENERATED_SEGMENTS or part.startswith("playwright-report-")
        for part in parts
    )


def main():
    errors = []
    files = tracked_files()

    for relative in REQUIRED:
        if relative not in files or not (ROOT / relative).is_file():
            errors.append(f"required canonical artifact is missing: {relative}")

    engineering = ROOT / "ENGINEERING_CONTEXT.md"
    if engineering.is_file() and EXPECTED_BRANCH not in engineering.read_text(
        encoding="utf-8", errors="ignore"
    ):
        errors.append(
            "ENGINEERING_CONTEXT.md does not declare the authoritative branch "
            + EXPECTED_BRANCH
        )

    duplicate_candidates = defaultdict(list)

    for relative in files:
        path = ROOT / relative

        if relative in FORBIDDEN_EXACT_PATHS:
            errors.append(f"obsolete placeholder/delivery artifact is tracked: {relative}")

        if relative.endswith("/.gitkeep") or relative == ".gitkeep":
            errors.append(f"empty-directory placeholder is tracked: {relative}")

        if relative.startswith(FORBIDDEN_PREFIXES):
            errors.append(f"obsolete empty tree is tracked: {relative}")

        if is_generated(relative):
            errors.append(f"generated output is tracked: {relative}")

        if relative.lower().endswith(FORBIDDEN_SUFFIXES):
            errors.append(f"temporary/compiled artifact is tracked: {relative}")

        if not path.is_file():
            errors.append(f"tracked path is not a regular file: {relative}")
            continue

        data = path.read_bytes()
        if not data:
            errors.append(f"zero-byte file is tracked: {relative}")
            continue

        duplicate_candidates[sha256(data).hexdigest()].append(relative)

        if b"\0" not in data and relative != "scripts/verify_repository_hygiene.py":
            text = data.decode("utf-8", errors="ignore")
            for branch in STALE_BRANCHES:
                if branch in text:
                    errors.append(
                        f"stale authoritative-branch reference {branch}: {relative}"
                    )

    for paths in duplicate_candidates.values():
        if len(paths) < 2:
            continue
        group = frozenset(paths)
        if group not in ALLOWED_DUPLICATE_GROUPS:
            errors.append(
                "unclassified identical tracked artifacts: "
                + ", ".join(sorted(paths))
            )

    if errors:
        fail(errors)

    print("=" * 78)
    print("FS-2.9 REPOSITORY HYGIENE GATE PASSED")
    print("=" * 78)
    print(f"Tracked files inspected: {len(files)}")
    print("Validated:")
    print(" - no generated/build/test-report output is tracked")
    print(" - no zero-byte or .gitkeep placeholder is tracked")
    print(" - no obsolete empty module/tree remains")
    print(" - no stale authoritative-branch reference remains")
    print(" - identical tracked artifacts are explicitly classified")


if __name__ == "__main__":
    main()
