from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

CANONICAL_DOCS = [
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
    "documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml",
]

SPECIALIZED_GATES = [
    "scripts/verify_documentation_contract_references.py",
    "scripts/verify_ai_documentation.py",
]

# Phase-oriented documentation that was absorbed into canonical current-state
# documents during FS-2.7.6. These names must not return to the baseline.
FORBIDDEN_ABSORBED_DOCS = [
    "documentation/architecture/module-boundaries/FS-2.4.0_MODULE_DEPENDENCY_AUDIT.md",
    "documentation/architecture/module-boundaries/FS-2.4.1_BUSINESS_EDGE_CLASSIFICATION.md",
    "documentation/architecture/module-boundaries/FS-2.4.2_MODULE_BOUNDARY_NON_REGRESSION_GATE.md",
    "documentation/architecture/configuration/FS-2.5.0_CONFIGURATION_INVENTORY.md",
    "documentation/architecture/configuration/FS-2.5.1_BOOTSTRAP_GLOBAL_CONFIGURATION_NORMALIZATION.md",
    "documentation/architecture/configuration/FS-2.5.2_DOMAIN_CONFIGURATION_OWNERSHIP.md",
    "documentation/architecture/configuration/FS-2.5.3_PROFILES_CONSOLIDATION.md",
    "documentation/architecture/configuration/FS-2.5.4_SECURITY_AUTHENTICATION_CONFIGURATION.md",
    "documentation/architecture/configuration/FS-2.5.5_OPENAPI_SPRINGDOC_CONFIGURATION.md",
    "documentation/architecture/configuration/FS-2.5.6_ANGULAR_ENVIRONMENTS.md",
    "documentation/architecture/configuration/FS-2.5.7_FEATURE_FLAG_REGISTRY.md",
    "documentation/architecture/configuration/FS-2.5.8_CONFIGURATION_NON_REGRESSION_GATE.md",
    "documentation/architecture/configuration/FS-2.5.9_CONFIGURATION_FINAL_VALIDATION.md",
    "documentation/architecture/FS-2.6_TESTS_AND_GATES_CONSOLIDATION.md",
]

INDEX_REFERENCES = {
    "documentation/README.md": [
        "documentation/architecture/README.md",
        "documentation/requirements/README.md",
        "documentation/domains/README.md",
        "documentation/contracts/README.md",
        "documentation/runbooks/README.md",
        "documentation/ai/README.md",
    ],
    "documentation/architecture/README.md": [
        "documentation/architecture/MODULE_BOUNDARIES.md",
        "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",
        "documentation/architecture/TESTS_AND_GATES.md",
        "documentation/architecture/REPOSITORY_HYGIENE.md",
    ],
}

REQUIRED_ARCHITECTURE_WORDING = {
    "documentation/architecture/MODULE_BOUNDARIES.md": [
        "backend/partner",
        "infrastructure",
        "JPA entity",
        "Circular",
    ],
    "documentation/architecture/CONFIGURATION_ARCHITECTURE.md": [
        "Bootstrap",
        "Business module",
        "FEATURE_FLAG_REGISTRY.yaml",
    ],
    "documentation/architecture/TESTS_AND_GATES.md": [
        "mvn verify",
        "npm run verify:sixpay",
        "scripts/verify_baseline.py",
    ],
    "documentation/architecture/REPOSITORY_HYGIENE.md": [
        "backend/partner",
        "scripts/verify_repository_hygiene.py",
        "backend/customer",
    ],
}


def fail(errors):
    print()
    print("=" * 78)
    print("FS-2.7.7 DOCUMENTATION NON-REGRESSION GATE FAILED")
    print("=" * 78)
    print()
    for error in errors:
        print(" -", error)
    sys.exit(1)


def require(relative):
    path = ROOT / relative
    if not path.is_file():
        raise FileNotFoundError(relative)
    return path


def run(label, command):
    print()
    print("=" * 78)
    print(label)
    print("=" * 78)
    print(" ".join(command))

    completed = subprocess.run(
        command,
        cwd=ROOT,
        text=True,
    )

    if completed.returncode != 0:
        fail([
            f"{label} returned exit code {completed.returncode}"
        ])


def main():
    errors = []

    if not ENGINEERING.is_file():
        errors.append("ENGINEERING_CONTEXT.md is missing")
    else:
        engineering = ENGINEERING.read_text(
            encoding="utf-8",
            errors="ignore",
        )

        if EXPECTED_BRANCH not in engineering:
            errors.append(
                "ENGINEERING_CONTEXT.md does not declare authoritative branch "
                + EXPECTED_BRANCH
            )

        if "documentation/README.md" not in engineering:
            errors.append(
                "ENGINEERING_CONTEXT.md does not expose canonical "
                "documentation map"
            )

    for relative in CANONICAL_DOCS:
        if not (ROOT / relative).is_file():
            errors.append(
                f"canonical documentation artifact is missing: {relative}"
            )

    for relative in SPECIALIZED_GATES:
        if not (ROOT / relative).is_file():
            errors.append(
                f"specialized documentation gate is missing: {relative}"
            )

    # Historical phase docs removed by FS-2.7.6 must not be restored.
    for relative in FORBIDDEN_ABSORBED_DOCS:
        if (ROOT / relative).exists():
            errors.append(
                f"absorbed historical documentation was restored: {relative}"
            )

    # Canonical index topology must remain navigable.
    for source, targets in INDEX_REFERENCES.items():
        path = ROOT / source
        if not path.is_file():
            continue

        text = path.read_text(
            encoding="utf-8",
            errors="ignore",
        )

        for target in targets:
            if target not in text:
                errors.append(
                    f"{source} no longer references canonical target: {target}"
                )

            if not (ROOT / target).is_file():
                errors.append(
                    f"{source} references missing canonical target: {target}"
                )

    # Protect the durable conclusions absorbed from previous FS phases.
    for relative, required_tokens in REQUIRED_ARCHITECTURE_WORDING.items():
        path = ROOT / relative

        if not path.is_file():
            continue

        text = path.read_text(
            encoding="utf-8",
            errors="ignore",
        )

        for token in required_tokens:
            if token not in text:
                errors.append(
                    f"{relative} lost required baseline invariant: {token}"
                )

    # Requirement source material must not disappear during documentation
    # cleanup merely because it is binary.
    requirements = ROOT / "documentation/requirements"
    requirement_sources = [
        path for path in requirements.rglob("*")
        if path.is_file()
        and path.suffix.lower() in {".pdf", ".docx"}
    ]

    if not requirement_sources:
        errors.append(
            "no binary requirement/reference source remains under "
            "documentation/requirements"
        )

    # AI reference assets explicitly linked by the contract registry are
    # checked by verify_ai_documentation.py; here we only enforce that the
    # canonical AI area still exists.
    for area in ["customer", "integration", "payment"]:
        if not (ROOT / "documentation/ai" / area).is_dir():
            errors.append(
                f"canonical AI reference area is missing: documentation/ai/{area}"
            )

    if errors:
        fail(sorted(set(errors)))

    print("FS-2.7.7 documentation baseline structure PASSED.")
    print(
        f"Canonical artifacts: {len(CANONICAL_DOCS)}; "
        f"forbidden absorbed docs checked: {len(FORBIDDEN_ABSORBED_DOCS)}; "
        f"requirement binary sources preserved: {len(requirement_sources)}."
    )

    python = sys.executable

    run(
        "1/2 — Contracts / runbooks documentation references",
        [
            python,
            "scripts/verify_documentation_contract_references.py",
        ],
    )

    run(
        "2/2 — AI documentation precedence / traceability",
        [
            python,
            "scripts/verify_ai_documentation.py",
        ],
    )

    print()
    print("=" * 78)
    print("FS-2.7.7 DOCUMENTATION NON-REGRESSION GATE PASSED")
    print("=" * 78)
    print()
    print("Validated:")
    print(" - canonical documentation map/index topology")
    print(" - canonical architecture documents")
    print(" - absorbed FS-2.4/FS-2.5/FS-2.6 docs are not restored")
    print(" - Partner golden-module documentation invariant")
    print(" - configuration and verification baseline invariants")
    print(" - requirement source documents remain preserved")
    print(" - contracts/runbooks reference integrity")
    print(" - AI source precedence and registry traceability")


if __name__ == "__main__":
    main()
