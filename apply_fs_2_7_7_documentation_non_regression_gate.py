from pathlib import Path
import sys

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
DOC_INDEX = ROOT / "documentation/README.md"
VERIFY = ROOT / "scripts/verify_documentation_baseline.py"

CONTENT = 'from pathlib import Path\nimport subprocess\nimport sys\n\nROOT = Path.cwd()\nEXPECTED_BRANCH = "feat/repository-baseline-consolidation"\n\nENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"\n\nCANONICAL_DOCS = [\n    "documentation/README.md",\n    "documentation/architecture/README.md",\n    "documentation/architecture/MODULE_BOUNDARIES.md",\n    "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",\n    "documentation/architecture/TESTS_AND_GATES.md",\n    "documentation/requirements/README.md",\n    "documentation/domains/README.md",\n    "documentation/contracts/README.md",\n    "documentation/contracts/CONTRACT_REGISTRY.yaml",\n    "documentation/runbooks/README.md",\n    "documentation/ai/README.md",\n    "documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml",\n]\n\nSPECIALIZED_GATES = [\n    "scripts/verify_documentation_contract_references.py",\n    "scripts/verify_ai_documentation.py",\n]\n\n# Phase-oriented documentation that was absorbed into canonical current-state\n# documents during FS-2.7.6. These names must not return to the baseline.\nFORBIDDEN_ABSORBED_DOCS = [\n    "documentation/architecture/module-boundaries/FS-2.4.0_MODULE_DEPENDENCY_AUDIT.md",\n    "documentation/architecture/module-boundaries/FS-2.4.1_BUSINESS_EDGE_CLASSIFICATION.md",\n    "documentation/architecture/module-boundaries/FS-2.4.2_MODULE_BOUNDARY_NON_REGRESSION_GATE.md",\n    "documentation/architecture/configuration/FS-2.5.0_CONFIGURATION_INVENTORY.md",\n    "documentation/architecture/configuration/FS-2.5.1_BOOTSTRAP_GLOBAL_CONFIGURATION_NORMALIZATION.md",\n    "documentation/architecture/configuration/FS-2.5.2_DOMAIN_CONFIGURATION_OWNERSHIP.md",\n    "documentation/architecture/configuration/FS-2.5.3_PROFILES_CONSOLIDATION.md",\n    "documentation/architecture/configuration/FS-2.5.4_SECURITY_AUTHENTICATION_CONFIGURATION.md",\n    "documentation/architecture/configuration/FS-2.5.5_OPENAPI_SPRINGDOC_CONFIGURATION.md",\n    "documentation/architecture/configuration/FS-2.5.6_ANGULAR_ENVIRONMENTS.md",\n    "documentation/architecture/configuration/FS-2.5.7_FEATURE_FLAG_REGISTRY.md",\n    "documentation/architecture/configuration/FS-2.5.8_CONFIGURATION_NON_REGRESSION_GATE.md",\n    "documentation/architecture/configuration/FS-2.5.9_CONFIGURATION_FINAL_VALIDATION.md",\n    "documentation/architecture/FS-2.6_TESTS_AND_GATES_CONSOLIDATION.md",\n]\n\nINDEX_REFERENCES = {\n    "documentation/README.md": [\n        "documentation/architecture/README.md",\n        "documentation/requirements/README.md",\n        "documentation/domains/README.md",\n        "documentation/contracts/README.md",\n        "documentation/runbooks/README.md",\n        "documentation/ai/README.md",\n    ],\n    "documentation/architecture/README.md": [\n        "documentation/architecture/MODULE_BOUNDARIES.md",\n        "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",\n        "documentation/architecture/TESTS_AND_GATES.md",\n    ],\n}\n\nREQUIRED_ARCHITECTURE_WORDING = {\n    "documentation/architecture/MODULE_BOUNDARIES.md": [\n        "backend/partner",\n        "infrastructure",\n        "JPA entity",\n        "Circular",\n    ],\n    "documentation/architecture/CONFIGURATION_ARCHITECTURE.md": [\n        "Bootstrap",\n        "Business module",\n        "FEATURE_FLAG_REGISTRY.yaml",\n    ],\n    "documentation/architecture/TESTS_AND_GATES.md": [\n        "mvn verify",\n        "npm run verify:sixpay",\n        "scripts/verify_baseline.py",\n    ],\n}\n\n\ndef fail(errors):\n    print()\n    print("=" * 78)\n    print("FS-2.7.7 DOCUMENTATION NON-REGRESSION GATE FAILED")\n    print("=" * 78)\n    print()\n    for error in errors:\n        print(" -", error)\n    sys.exit(1)\n\n\ndef require(relative):\n    path = ROOT / relative\n    if not path.is_file():\n        raise FileNotFoundError(relative)\n    return path\n\n\ndef run(label, command):\n    print()\n    print("=" * 78)\n    print(label)\n    print("=" * 78)\n    print(" ".join(command))\n\n    completed = subprocess.run(\n        command,\n        cwd=ROOT,\n        text=True,\n    )\n\n    if completed.returncode != 0:\n        fail([\n            f"{label} returned exit code {completed.returncode}"\n        ])\n\n\ndef main():\n    errors = []\n\n    if not ENGINEERING.is_file():\n        errors.append("ENGINEERING_CONTEXT.md is missing")\n    else:\n        engineering = ENGINEERING.read_text(\n            encoding="utf-8",\n            errors="ignore",\n        )\n\n        if EXPECTED_BRANCH not in engineering:\n            errors.append(\n                "ENGINEERING_CONTEXT.md does not declare authoritative branch "\n                + EXPECTED_BRANCH\n            )\n\n        if "documentation/README.md" not in engineering:\n            errors.append(\n                "ENGINEERING_CONTEXT.md does not expose canonical "\n                "documentation map"\n            )\n\n    for relative in CANONICAL_DOCS:\n        if not (ROOT / relative).is_file():\n            errors.append(\n                f"canonical documentation artifact is missing: {relative}"\n            )\n\n    for relative in SPECIALIZED_GATES:\n        if not (ROOT / relative).is_file():\n            errors.append(\n                f"specialized documentation gate is missing: {relative}"\n            )\n\n    # Historical phase docs removed by FS-2.7.6 must not be restored.\n    for relative in FORBIDDEN_ABSORBED_DOCS:\n        if (ROOT / relative).exists():\n            errors.append(\n                f"absorbed historical documentation was restored: {relative}"\n            )\n\n    # Canonical index topology must remain navigable.\n    for source, targets in INDEX_REFERENCES.items():\n        path = ROOT / source\n        if not path.is_file():\n            continue\n\n        text = path.read_text(\n            encoding="utf-8",\n            errors="ignore",\n        )\n\n        for target in targets:\n            if target not in text:\n                errors.append(\n                    f"{source} no longer references canonical target: {target}"\n                )\n\n            if not (ROOT / target).is_file():\n                errors.append(\n                    f"{source} references missing canonical target: {target}"\n                )\n\n    # Protect the durable conclusions absorbed from previous FS phases.\n    for relative, required_tokens in REQUIRED_ARCHITECTURE_WORDING.items():\n        path = ROOT / relative\n\n        if not path.is_file():\n            continue\n\n        text = path.read_text(\n            encoding="utf-8",\n            errors="ignore",\n        )\n\n        for token in required_tokens:\n            if token not in text:\n                errors.append(\n                    f"{relative} lost required baseline invariant: {token}"\n                )\n\n    # Requirement source material must not disappear during documentation\n    # cleanup merely because it is binary.\n    requirements = ROOT / "documentation/requirements"\n    requirement_sources = [\n        path for path in requirements.rglob("*")\n        if path.is_file()\n        and path.suffix.lower() in {".pdf", ".docx"}\n    ]\n\n    if not requirement_sources:\n        errors.append(\n            "no binary requirement/reference source remains under "\n            "documentation/requirements"\n        )\n\n    # AI reference assets explicitly linked by the contract registry are\n    # checked by verify_ai_documentation.py; here we only enforce that the\n    # canonical AI area still exists.\n    for area in ["customer", "integration", "payment"]:\n        if not (ROOT / "documentation/ai" / area).is_dir():\n            errors.append(\n                f"canonical AI reference area is missing: documentation/ai/{area}"\n            )\n\n    if errors:\n        fail(sorted(set(errors)))\n\n    print("FS-2.7.7 documentation baseline structure PASSED.")\n    print(\n        f"Canonical artifacts: {len(CANONICAL_DOCS)}; "\n        f"forbidden absorbed docs checked: {len(FORBIDDEN_ABSORBED_DOCS)}; "\n        f"requirement binary sources preserved: {len(requirement_sources)}."\n    )\n\n    python = sys.executable\n\n    run(\n        "1/2 — Contracts / runbooks documentation references",\n        [\n            python,\n            "scripts/verify_documentation_contract_references.py",\n        ],\n    )\n\n    run(\n        "2/2 — AI documentation precedence / traceability",\n        [\n            python,\n            "scripts/verify_ai_documentation.py",\n        ],\n    )\n\n    print()\n    print("=" * 78)\n    print("FS-2.7.7 DOCUMENTATION NON-REGRESSION GATE PASSED")\n    print("=" * 78)\n    print()\n    print("Validated:")\n    print(" - canonical documentation map/index topology")\n    print(" - canonical architecture documents")\n    print(" - absorbed FS-2.4/FS-2.5/FS-2.6 docs are not restored")\n    print(" - Partner golden-module documentation invariant")\n    print(" - configuration and verification baseline invariants")\n    print(" - requirement source documents remain preserved")\n    print(" - contracts/runbooks reference integrity")\n    print(" - AI source precedence and registry traceability")\n\n\nif __name__ == "__main__":\n    main()\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8", errors="ignore")

def main():
    engineering = require(ENGINEERING)
    documentation = require(DOC_INDEX)

    if EXPECTED_BRANCH not in engineering:
        fail("ENGINEERING_CONTEXT.md does not declare authoritative branch")

    prerequisites = [
        ROOT / "documentation/architecture/README.md",
        ROOT / "documentation/architecture/MODULE_BOUNDARIES.md",
        ROOT / "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",
        ROOT / "documentation/architecture/TESTS_AND_GATES.md",
        ROOT / "documentation/requirements/README.md",
        ROOT / "documentation/domains/README.md",
        ROOT / "documentation/runbooks/README.md",
        ROOT / "documentation/ai/README.md",
        ROOT / "scripts/verify_documentation_contract_references.py",
        ROOT / "scripts/verify_ai_documentation.py",
    ]

    for path in prerequisites:
        require(path)

    if VERIFY.exists():
        fail(
            "Documentation baseline verifier already exists: "
            + str(VERIFY.relative_to(ROOT))
        )

    VERIFY.parent.mkdir(parents=True, exist_ok=True)
    VERIFY.write_text(
        CONTENT,
        encoding="utf-8",
    )

    gate_section = """
## Documentation verification

Canonical documentation non-regression gate:

```bash
py scripts/verify_documentation_baseline.py
```

This gate protects the canonical documentation topology, absorbed historical
cleanup, contracts/runbooks references and AI documentation precedence.
"""

    if "verify_documentation_baseline.py" not in documentation:
        DOC_INDEX.write_text(
            documentation.rstrip()
            + "\n"
            + gate_section,
            encoding="utf-8",
        )

    print("FS-2.7.7 documentation non-regression gate installed.")
    print()
    print("Created:")
    print(" - scripts/verify_documentation_baseline.py")
    print()
    print("Updated:")
    print(" - documentation/README.md")
    print()
    print("Composes existing specialized gates:")
    print(" - scripts/verify_documentation_contract_references.py")
    print(" - scripts/verify_ai_documentation.py")
    print()
    print("Run:")
    print("  py scripts/verify_documentation_baseline.py")

if __name__ == "__main__":
    main()
