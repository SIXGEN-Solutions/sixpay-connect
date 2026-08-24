from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

DOC = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.8_CONFIGURATION_NON_REGRESSION_GATE.md"
)

VERIFY = (
    ROOT
    / "scripts/"
    / "verify_configuration_consolidation.py"
)

DOC_CONTENT = '# FS-2.5.8 — Configuration Non-Regression Gate\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Phase:** `FS-2.5 — Configuration consolidation`  \n**Golden module:** Partner\n\n## Purpose\n\nFS-2.5.8 does not redefine the detailed configuration rules already established\nin FS-2.5.1 through FS-2.5.7.\n\nIt provides one canonical orchestration gate:\n\n```text\nscripts/verify_configuration_consolidation.py\n```\n\nThe gate executes the existing backend, frontend and feature-flag controls as\none configuration baseline.\n\n## Covered policies\n\n### Bootstrap/global configuration\n\nProtects:\n\n- base runtime invariants;\n- Bootstrap global namespace ownership;\n- absence of new domain configuration debt in the base application file.\n\n### Domain configuration ownership\n\nProtects:\n\n- `sixpay.<domain>.*` semantic ownership;\n- absence of direct cross-domain configuration consumption.\n\n### Runtime profiles\n\nProtects:\n\n- canonical Flyway locations;\n- absence of historical migration paths;\n- local/hybrid authentication profile semantics;\n- absence of destructive Hibernate schema profiles.\n\n### Security/authentication\n\nProtects:\n\n- Security-owned property binding/defaults;\n- Bootstrap-owned OAuth2/session runtime assembly;\n- no direct foreign consumption of `sixpay.security.*` configuration.\n\n### OpenAPI/Springdoc\n\nProtects:\n\n- Bootstrap-owned `GroupedOpenApi` assembly;\n- canonical OpenAPI groups;\n- Payment timeline ownership by Reporting;\n- Springdoc disabled by default and enabled only in reviewed profiles.\n\n### Angular environments\n\nProtects:\n\n- production/integration API-only policy;\n- development/netlify explicit mock policy;\n- authentication environment matrix;\n- Angular CLI file-replacement mappings;\n- absence of API-to-mock fallback.\n\n### Feature-flag registry\n\nProtects:\n\n- explicit ownership;\n- namespace/owner consistency;\n- no `REVIEW_REQUIRED` owners;\n- no unqualified parser artifacts;\n- reviewed Angular/Security/runtime flags.\n\n## Gate composition\n\nThe canonical gate executes:\n\n```text\nBackend architecture tests\n    BootstrapGlobalConfigurationArchitectureTest\n    DomainConfigurationOwnershipArchitectureTest\n    RuntimeProfileConfigurationArchitectureTest\n    SecurityAuthenticationConfigurationArchitectureTest\n    OpenApiSpringdocConfigurationArchitectureTest\n\nFrontend\n    verify:angular-environments\n    verify:runtime-datasource-policy\n\nRepository\n    verify_feature_flag_registry.py\n```\n\n## Important design rule\n\nThis gate orchestrates existing rules. It does not duplicate them.\n\n```text\ndetailed rule\n    = owning architecture test / verifier\n\nFS-2.5.8\n    = orchestration + completeness check\n```\n\nThis prevents two independent implementations of the same configuration rule.\n\n## Non-regression policy\n\nA gate failure means:\n\n```text\ndetect\n  -> inspect\n  -> prove regression\n  -> minimal correction\n  -> rerun gate\n```\n\nIt does not authorize automatic changes to functional code, property defaults,\nenvironment variables, profiles or authentication behavior.\n\n## Exit criteria\n\nFS-2.5.8 is complete when:\n\n- all FS-2.5 architecture artifacts exist;\n- the consolidated gate executes successfully;\n- backend configuration architecture tests pass;\n- frontend environment/runtime datasource gates pass;\n- feature-flag registry validation passes.\n'
VERIFY_CONTENT = 'from pathlib import Path\nimport shutil\nimport subprocess\nimport sys\n\nROOT = Path.cwd()\n\nEXPECTED_BRANCH = "feat/repository-baseline-consolidation"\n\nENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"\n\nREQUIRED_DOCS = [\n    "documentation/architecture/configuration/FS-2.5.0_CONFIGURATION_INVENTORY.md",\n    "documentation/architecture/configuration/FS-2.5.1_BOOTSTRAP_GLOBAL_CONFIGURATION_NORMALIZATION.md",\n    "documentation/architecture/configuration/FS-2.5.2_DOMAIN_CONFIGURATION_OWNERSHIP.md",\n    "documentation/architecture/configuration/FS-2.5.3_PROFILES_CONSOLIDATION.md",\n    "documentation/architecture/configuration/FS-2.5.4_SECURITY_AUTHENTICATION_CONFIGURATION.md",\n    "documentation/architecture/configuration/FS-2.5.5_OPENAPI_SPRINGDOC_CONFIGURATION.md",\n    "documentation/architecture/configuration/FS-2.5.6_ANGULAR_ENVIRONMENTS.md",\n    "documentation/architecture/configuration/FS-2.5.7_FEATURE_FLAG_REGISTRY.md",\n    "documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml",\n]\n\nBACKEND_TESTS = [\n    "BootstrapGlobalConfigurationArchitectureTest",\n    "DomainConfigurationOwnershipArchitectureTest",\n    "RuntimeProfileConfigurationArchitectureTest",\n    "SecurityAuthenticationConfigurationArchitectureTest",\n    "OpenApiSpringdocConfigurationArchitectureTest",\n]\n\nBACKEND_TEST_FILES = [\n    "backend/bootstrap/src/test/java/com/sixpay/bootstrap/architecture/"\n    + test + ".java"\n    for test in BACKEND_TESTS\n]\n\nREQUIRED_VERIFIERS = [\n    "frontend/scripts/verify-angular-environment-policy.mjs",\n    "frontend/scripts/verify-runtime-datasource-policy.mjs",\n    "scripts/verify_feature_flag_registry.py",\n]\n\ndef fail(message):\n    print("\\nFS-2.5.8 configuration non-regression gate FAILED:\\n")\n    print(" -", message)\n    sys.exit(1)\n\ndef require(path):\n    absolute = ROOT / path\n    if not absolute.is_file():\n        fail(f"required configuration baseline artifact is missing: {path}")\n    return absolute\n\ndef run(label, command, cwd):\n    print()\n    print("=" * 78)\n    print(label)\n    print("=" * 78)\n    print(" ".join(command))\n\n    completed = subprocess.run(\n        command,\n        cwd=cwd,\n        text=True,\n    )\n\n    if completed.returncode != 0:\n        fail(\n            f"{label} failed with exit code {completed.returncode}"\n        )\n\ndef executable(name):\n    resolved = shutil.which(name)\n\n    if resolved is None:\n        fail(\n            f"required executable is not available on PATH: {name}"\n        )\n\n    return resolved\n\ndef main():\n    if not ENGINEERING.is_file():\n        fail("ENGINEERING_CONTEXT.md is missing")\n\n    engineering = ENGINEERING.read_text(encoding="utf-8")\n\n    if EXPECTED_BRANCH not in engineering:\n        fail(\n            "ENGINEERING_CONTEXT.md does not declare the authoritative "\n            f"branch {EXPECTED_BRANCH}"\n        )\n\n    for path in REQUIRED_DOCS:\n        require(path)\n\n    for path in BACKEND_TEST_FILES:\n        require(path)\n\n    for path in REQUIRED_VERIFIERS:\n        require(path)\n\n    mvn = executable("mvn")\n    npm = executable("npm")\n    python = sys.executable\n\n    print("FS-2.5.8 configuration baseline completeness PASSED.")\n    print(\n        f"Documents: {len(REQUIRED_DOCS)}; "\n        f"backend architecture tests: {len(BACKEND_TESTS)}; "\n        f"standalone verifiers: {len(REQUIRED_VERIFIERS)}."\n    )\n\n    run(\n        "Feature-flag registry",\n        [\n            python,\n            "scripts/verify_feature_flag_registry.py",\n        ],\n        ROOT,\n    )\n\n    run(\n        "Angular environment policy",\n        [\n            npm,\n            "run",\n            "verify:angular-environments",\n        ],\n        ROOT / "frontend",\n    )\n\n    run(\n        "Frontend runtime datasource policy",\n        [\n            npm,\n            "run",\n            "verify:runtime-datasource-policy",\n        ],\n        ROOT / "frontend",\n    )\n\n    test_selector = ",".join(BACKEND_TESTS)\n\n    run(\n        "Backend configuration architecture tests",\n        [\n            mvn,\n            "-pl",\n            "bootstrap",\n            "-am",\n            f"-Dtest={test_selector}",\n            "-Dsurefire.failIfNoSpecifiedTests=false",\n            "test",\n        ],\n        ROOT / "backend",\n    )\n\n    print()\n    print("=" * 78)\n    print("FS-2.5.8 CONFIGURATION NON-REGRESSION GATE PASSED")\n    print("=" * 78)\n    print()\n    print("Validated:")\n    print(" - Bootstrap/global configuration ownership")\n    print(" - domain configuration ownership")\n    print(" - runtime profile safety")\n    print(" - Security/authentication configuration ownership")\n    print(" - OpenAPI/Springdoc runtime topology")\n    print(" - Angular environment matrix")\n    print(" - frontend runtime datasource policy")\n    print(" - feature-flag registry ownership")\n\nif __name__ == "__main__":\n    main()\n'

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
        / "FS-2.5.7_FEATURE_FLAG_REGISTRY.md",
        ROOT
        / "documentation/architecture/configuration/"
        / "FEATURE_FLAG_REGISTRY.yaml",
        ROOT
        / "scripts/verify_feature_flag_registry.py",
        ROOT
        / "frontend/scripts/verify-angular-environment-policy.mjs",
    ]

    for prerequisite in prerequisites:
        require(prerequisite)

    DOC.parent.mkdir(parents=True, exist_ok=True)
    VERIFY.parent.mkdir(parents=True, exist_ok=True)

    if DOC.exists():
        fail(
            "FS-2.5.8 documentation already exists: "
            + str(DOC.relative_to(ROOT))
        )

    if VERIFY.exists():
        fail(
            "Configuration consolidation verifier already exists: "
            + str(VERIFY.relative_to(ROOT))
        )

    DOC.write_text(
        DOC_CONTENT,
        encoding="utf-8"
    )

    VERIFY.write_text(
        VERIFY_CONTENT,
        encoding="utf-8"
    )

    print("FS-2.5.8 configuration non-regression gate installed.")
    print("Created:")
    print(" -", DOC.relative_to(ROOT))
    print(" -", VERIFY.relative_to(ROOT))
    print()
    print("The gate orchestrates existing FS-2.5 controls;")
    print("it does not change runtime configuration.")
    print()
    print("Run from repository root:")
    print("  py scripts/verify_configuration_consolidation.py")

if __name__ == "__main__":
    main()
