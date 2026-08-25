from pathlib import Path
import sys

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
DOC_INDEX = ROOT / "documentation/README.md"
TARGET = ROOT / "scripts/verify_documentation_final.py"

CONTENT = 'from pathlib import Path\nimport shutil\nimport subprocess\nimport sys\n\nROOT = Path.cwd()\nEXPECTED_BRANCH = "feat/repository-baseline-consolidation"\n\nENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"\n\nREQUIRED = [\n    "documentation/README.md",\n    "documentation/architecture/README.md",\n    "documentation/architecture/MODULE_BOUNDARIES.md",\n    "documentation/architecture/CONFIGURATION_ARCHITECTURE.md",\n    "documentation/architecture/TESTS_AND_GATES.md",\n    "documentation/requirements/README.md",\n    "documentation/domains/README.md",\n    "documentation/contracts/README.md",\n    "documentation/contracts/CONTRACT_REGISTRY.yaml",\n    "documentation/runbooks/README.md",\n    "documentation/ai/README.md",\n    "scripts/verify_documentation_baseline.py",\n    "scripts/verify_configuration_consolidation.py",\n    "frontend/scripts/verify-contract-consolidation.mjs",\n]\n\n\ndef fail(message):\n    print()\n    print("=" * 78)\n    print("FS-2.7.8 FINAL DOCUMENTATION VALIDATION FAILED")\n    print("=" * 78)\n    print()\n    print(" -", message)\n    sys.exit(1)\n\n\ndef require(relative):\n    path = ROOT / relative\n    if not path.is_file():\n        fail(f"required FS-2.7 asset is missing: {relative}")\n    return path\n\n\ndef executable(name):\n    value = shutil.which(name)\n    if value is None:\n        fail(f"required executable is not available on PATH: {name}")\n    return value\n\n\ndef run(label, command, cwd=ROOT):\n    print()\n    print("=" * 78)\n    print(label)\n    print("=" * 78)\n    print(" ".join(command))\n\n    completed = subprocess.run(\n        command,\n        cwd=cwd,\n        text=True,\n    )\n\n    if completed.returncode != 0:\n        fail(\n            f"{label} returned exit code "\n            f"{completed.returncode}"\n        )\n\n\ndef main():\n    if not ENGINEERING.is_file():\n        fail("ENGINEERING_CONTEXT.md is missing")\n\n    engineering = ENGINEERING.read_text(\n        encoding="utf-8",\n        errors="ignore",\n    )\n\n    if EXPECTED_BRANCH not in engineering:\n        fail(\n            "ENGINEERING_CONTEXT.md does not declare "\n            + EXPECTED_BRANCH\n        )\n\n    for relative in REQUIRED:\n        require(relative)\n\n    python = sys.executable\n    npm = executable("npm")\n\n    print("FS-2.7.8 documentation final-validation prerequisites PASSED.")\n    print(f"Required canonical assets: {len(REQUIRED)}.")\n\n    run(\n        "1/3 — Documentation non-regression baseline",\n        [\n            python,\n            "scripts/verify_documentation_baseline.py",\n        ],\n    )\n\n    run(\n        "2/3 — Canonical contract registry / consolidation integrity",\n        [\n            npm,\n            "run",\n            "verify:contract-consolidation",\n        ],\n        ROOT / "frontend",\n    )\n\n    run(\n        "3/3 — Configuration documentation / gate alignment",\n        [\n            python,\n            "scripts/verify_configuration_consolidation.py",\n        ],\n    )\n\n    print()\n    print("=" * 78)\n    print("FS-2.7.8 FINAL DOCUMENTATION VALIDATION PASSED")\n    print("=" * 78)\n    print()\n    print("Validated:")\n    print(" - canonical documentation navigation")\n    print(" - architecture current-state documents")\n    print(" - requirements/domain documentation ownership")\n    print(" - contracts/runbooks reference integrity")\n    print(" - AI documentation precedence and Partner golden-module rule")\n    print(" - absorbed historical FS documentation remains absent")\n    print(" - contract registry remains structurally valid")\n    print(" - configuration gates no longer depend on deleted phase documents")\n    print()\n    print("FS-2.7 — Documentation consolidation may be CLOSED.")\n\n\nif __name__ == "__main__":\n    main()\n'

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
        ROOT / "scripts/verify_documentation_baseline.py",
        ROOT / "scripts/verify_configuration_consolidation.py",
        ROOT / "frontend/scripts/verify-contract-consolidation.mjs",
    ]

    for path in prerequisites:
        require(path)

    if TARGET.exists():
        fail(
            "Final documentation verifier already exists: "
            + str(TARGET.relative_to(ROOT))
        )

    TARGET.write_text(
        CONTENT,
        encoding="utf-8",
    )

    final_section = """
## Final documentation validation

To validate the complete consolidated documentation baseline:

```bash
py scripts/verify_documentation_final.py
```

This is the canonical FS-2.7 closure command. It composes the documentation
non-regression gate, contract-registry integrity and configuration-documentation
alignment without reimplementing their specialized rules.
"""

    if "verify_documentation_final.py" not in documentation:
        DOC_INDEX.write_text(
            documentation.rstrip()
            + "\n"
            + final_section,
            encoding="utf-8",
        )

    print("FS-2.7.8 final documentation validation installed.")
    print()
    print("Created:")
    print(" - scripts/verify_documentation_final.py")
    print()
    print("Updated:")
    print(" - documentation/README.md")
    print()
    print("No new phase-history document was created.")
    print()
    print("Run:")
    print("  py scripts/verify_documentation_final.py")

if __name__ == "__main__":
    main()
