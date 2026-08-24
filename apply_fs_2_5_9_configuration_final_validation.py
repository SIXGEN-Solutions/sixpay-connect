from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

DOC = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.9_CONFIGURATION_FINAL_VALIDATION.md"
)

VERIFY = (
    ROOT
    / "scripts/"
    / "verify_configuration_final.py"
)

DOC_CONTENT = "# FS-2.5.9 — Configuration Consolidation Final Validation\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Phase:** `FS-2.5 — Configuration consolidation`  \n**Golden module:** Partner\n\n## Purpose\n\nFS-2.5.9 is the exit gate for the complete configuration-consolidation phase.\n\nIt adds no new configuration rule. It proves that the rules established from\nFS-2.5.0 through FS-2.5.8 coexist with the complete backend and frontend\nbaseline.\n\n## Final validation chain\n\n```text\nFS-2.5.8 configuration non-regression gate\n        ↓\nbackend full Maven reactor verify\n        ↓\nfrontend unit tests\n        ↓\nfrontend build:all\n        ↓\nFS-2.5 FINAL VALIDATION PASSED\n```\n\n## What `build:all` already proves\n\nThe canonical frontend `build:all` pipeline executes the environment gate and\nthe integration build gates before producing all reviewed build variants.\n\nIt covers:\n\n```text\nAngular environment policy\ncontract consolidation\nruntime datasource policy\nfull-stack static conformance\nintegration contract-backed policy\nintegration build\nNetlify/demo build\nproduction build\n```\n\nTherefore FS-2.5.9 does not duplicate those commands individually.\n\n## Backend proof\n\n`mvn verify` is mandatory after the targeted FS-2.5 architecture gates.\n\nThis validates the complete Maven reactor rather than only Bootstrap's\nconfiguration architecture tests.\n\n## Regression policy\n\nA final-validation failure does not authorize broad cleanup or semantic\nrewrites.\n\nThe workflow remains:\n\n```text\nidentify failing gate\n  -> inspect concrete regression\n  -> minimal correction\n  -> rerun targeted check\n  -> rerun FS-2.5.9\n```\n\n## Exit criteria\n\nFS-2.5 can be closed only when all of the following are green:\n\n- FS-2.5.8 consolidated configuration gate;\n- full backend `mvn verify`;\n- frontend unit tests;\n- frontend `build:all`;\n- no missing FS-2.5 documentation or gate asset.\n\n## Closure statement\n\nWhen the script prints:\n\n```text\nFS-2.5.9 FINAL VALIDATION PASSED\n```\n\nthe configuration-consolidation phase can be marked:\n\n```text\nFS-2.5 — Configuration consolidation\nSTATUS: CLOSED\n```\n"
VERIFY_CONTENT = 'from pathlib import Path\nimport shutil\nimport subprocess\nimport sys\n\nROOT = Path.cwd()\nEXPECTED_BRANCH = "feat/repository-baseline-consolidation"\n\nENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"\n\nREQUIRED_PHASE_DOCS = [\n    "documentation/architecture/configuration/FS-2.5.0_CONFIGURATION_INVENTORY.md",\n    "documentation/architecture/configuration/FS-2.5.1_BOOTSTRAP_GLOBAL_CONFIGURATION_NORMALIZATION.md",\n    "documentation/architecture/configuration/FS-2.5.2_DOMAIN_CONFIGURATION_OWNERSHIP.md",\n    "documentation/architecture/configuration/FS-2.5.3_PROFILES_CONSOLIDATION.md",\n    "documentation/architecture/configuration/FS-2.5.4_SECURITY_AUTHENTICATION_CONFIGURATION.md",\n    "documentation/architecture/configuration/FS-2.5.5_OPENAPI_SPRINGDOC_CONFIGURATION.md",\n    "documentation/architecture/configuration/FS-2.5.6_ANGULAR_ENVIRONMENTS.md",\n    "documentation/architecture/configuration/FS-2.5.7_FEATURE_FLAG_REGISTRY.md",\n    "documentation/architecture/configuration/FS-2.5.8_CONFIGURATION_NON_REGRESSION_GATE.md",\n    "documentation/architecture/configuration/FS-2.5.9_CONFIGURATION_FINAL_VALIDATION.md",\n    "documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml",\n]\n\nREQUIRED_GATES = [\n    "scripts/verify_feature_flag_registry.py",\n    "scripts/verify_configuration_consolidation.py",\n    "frontend/scripts/verify-angular-environment-policy.mjs",\n    "frontend/scripts/verify-runtime-datasource-policy.mjs",\n    "frontend/scripts/verify-contract-consolidation.mjs",\n    "frontend/scripts/verify-full-stack-conformance.mjs",\n    "frontend/scripts/verify-contract-backed-integration.mjs",\n]\n\ndef fail(message):\n    print()\n    print("=" * 78)\n    print("FS-2.5.9 FINAL VALIDATION FAILED")\n    print("=" * 78)\n    print()\n    print(" -", message)\n    sys.exit(1)\n\ndef require(relative):\n    path = ROOT / relative\n    if not path.is_file():\n        fail(f"required FS-2.5 asset is missing: {relative}")\n    return path\n\ndef executable(name):\n    value = shutil.which(name)\n    if value is None:\n        fail(f"required executable is not available on PATH: {name}")\n    return value\n\ndef run(label, command, cwd):\n    print()\n    print("=" * 78)\n    print(label)\n    print("=" * 78)\n    print(" ".join(command))\n\n    completed = subprocess.run(\n        command,\n        cwd=cwd,\n        text=True,\n    )\n\n    if completed.returncode != 0:\n        fail(\n            f"{label} returned exit code "\n            f"{completed.returncode}"\n        )\n\ndef main():\n    if not ENGINEERING.is_file():\n        fail("ENGINEERING_CONTEXT.md is missing")\n\n    engineering = ENGINEERING.read_text(encoding="utf-8")\n\n    if EXPECTED_BRANCH not in engineering:\n        fail(\n            "ENGINEERING_CONTEXT.md does not declare "\n            f"{EXPECTED_BRANCH}"\n        )\n\n    for relative in REQUIRED_PHASE_DOCS:\n        require(relative)\n\n    for relative in REQUIRED_GATES:\n        require(relative)\n\n    mvn = executable("mvn")\n    npm = executable("npm")\n    python = sys.executable\n\n    print("FS-2.5.9 baseline completeness PASSED.")\n    print(\n        f"Phase documents/assets: {len(REQUIRED_PHASE_DOCS)}; "\n        f"gate assets: {len(REQUIRED_GATES)}."\n    )\n\n    run(\n        "1/4 — FS-2.5.8 configuration non-regression gate",\n        [\n            python,\n            "scripts/verify_configuration_consolidation.py",\n        ],\n        ROOT,\n    )\n\n    run(\n        "2/4 — Full backend Maven reactor verify",\n        [\n            mvn,\n            "verify",\n        ],\n        ROOT / "backend",\n    )\n\n    run(\n        "3/4 — Frontend unit tests",\n        [\n            npm,\n            "test",\n        ],\n        ROOT / "frontend",\n    )\n\n    run(\n        "4/4 — Frontend canonical build:all",\n        [\n            npm,\n            "run",\n            "build:all",\n        ],\n        ROOT / "frontend",\n    )\n\n    print()\n    print("=" * 78)\n    print("FS-2.5.9 FINAL VALIDATION PASSED")\n    print("=" * 78)\n    print()\n    print("Validated:")\n    print(" - FS-2.5 configuration non-regression gate")\n    print(" - full Maven reactor verify")\n    print(" - frontend unit tests")\n    print(" - Angular environment policy")\n    print(" - contract consolidation")\n    print(" - frontend runtime datasource policy")\n    print(" - frontend full-stack conformance")\n    print(" - integration contract-backed policy")\n    print(" - integration build")\n    print(" - Netlify/demo build")\n    print(" - production build")\n    print()\n    print("FS-2.5 — Configuration consolidation may be CLOSED.")\n\nif __name__ == "__main__":\n    main()\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def main():
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            + EXPECTED_BRANCH
        )

    prerequisites = [
        ROOT
        / "documentation/architecture/configuration/"
        / "FS-2.5.8_CONFIGURATION_NON_REGRESSION_GATE.md",
        ROOT
        / "scripts/verify_configuration_consolidation.py",
        ROOT
        / "frontend/package.json",
    ]

    for prerequisite in prerequisites:
        require(prerequisite)

    DOC.parent.mkdir(parents=True, exist_ok=True)
    VERIFY.parent.mkdir(parents=True, exist_ok=True)

    if DOC.exists():
        fail(
            "FS-2.5.9 documentation already exists: "
            + str(DOC.relative_to(ROOT))
        )

    if VERIFY.exists():
        fail(
            "FS-2.5.9 final validator already exists: "
            + str(VERIFY.relative_to(ROOT))
        )

    DOC.write_text(DOC_CONTENT, encoding="utf-8")
    VERIFY.write_text(VERIFY_CONTENT, encoding="utf-8")

    print("FS-2.5.9 final validation installed.")
    print("Created:")
    print(" -", DOC.relative_to(ROOT))
    print(" -", VERIFY.relative_to(ROOT))
    print()
    print("No runtime configuration was changed.")
    print()
    print("Run from repository root:")
    print("  py scripts/verify_configuration_final.py")

if __name__ == "__main__":
    main()
