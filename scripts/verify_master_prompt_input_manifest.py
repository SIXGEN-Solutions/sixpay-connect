#!/usr/bin/env python3
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "MASTER_PROMPT_INPUT_MANIFEST.yaml"
CLASSIFICATION = ROOT / "documentation/DOCUMENTATION_CLASSIFICATION.yaml"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
BACKEND_POM = ROOT / "backend/pom.xml"
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
EXPECTED_PRECEDENCE = [
    "authoritative implementation branch",
    "documentation/architecture/",
    "documentation/requirements/",
    "documentation/contracts/",
    "documentation/ai/",
    "engineering assets",
    "ENGINEERING_CONTEXT.md",
]


def fail(message):
    print("ACTIVE MASTER PROMPT MANIFEST GATE FAILED")
    print(" - " + message)
    sys.exit(1)


def read(path):
    if not path.is_file():
        fail(f"required file is missing: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def section(text, heading, indent=0):
    marker = " " * indent + heading + ":"
    lines = text.splitlines()
    try:
        start = next(index for index, line in enumerate(lines) if line == marker)
    except StopIteration:
        fail(f"missing manifest section: {heading}")

    selected = []
    for line in lines[start + 1 :]:
        if line.strip() and len(line) - len(line.lstrip(" ")) <= indent:
            break
        selected.append(line)
    return "\n".join(selected)


def quoted_list(block):
    return re.findall(r'^\s+- "([^"]+)"\s*$', block, re.MULTILINE)


def contract_entries(block):
    entries = []
    chunks = re.split(r'(?m)^\s{4}- id: "([^"]+)"\s*$', block)
    for index in range(1, len(chunks), 2):
        contract_id = chunks[index]
        body = chunks[index + 1]
        path_match = re.search(r'(?m)^\s{6}path: "([^"]+)"\s*$', body)
        if path_match is None:
            fail(f"manifest contract has no path: {contract_id}")
        entries.append((contract_id, path_match.group(1)))
    return entries


def registry_entries(text):
    if "\ncontracts:\n" not in text:
        fail("contract registry has no contracts section")
    body = text.split("\ncontracts:\n", 1)[1]
    # The split alternates id and entry body.
    raw = re.split(r'(?m)^  - id: "([^"]+)"\s*$', body)[1:]
    parsed = []
    for index in range(0, len(raw), 2):
        contract_id = raw[index]
        entry_body = raw[index + 1]

        def field(name):
            match = re.search(
                rf'(?m)^    {re.escape(name)}: (?:"([^"]+)"|([^\n]+))$',
                entry_body,
            )
            if match is None:
                fail(f"registry contract {contract_id} has no {name}")
            return (match.group(1) or match.group(2)).strip()

        parsed.append(
            {
                "id": contract_id,
                "path": field("path"),
                "lifecycle": field("lifecycleStatus"),
            }
        )
    return parsed


def backend_modules(text):
    root = ET.fromstring(text)
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    return {
        element.text.strip()
        for element in root.findall("m:modules/m:module", namespace)
        if element.text
    }


def verify_git_ancestry(baseline_commit):
    completed = subprocess.run(
        ["git", "merge-base", "--is-ancestor", baseline_commit, "HEAD"],
        cwd=ROOT,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if completed.returncode != 0:
        fail("manifest baselineCommit is not an ancestor of HEAD")


def main():
    manifest = read(MANIFEST)
    classification = read(CLASSIFICATION)
    registry = read(REGISTRY)
    backend_pom = read(BACKEND_POM)
    engineering = read(ENGINEERING)

    required_literals = [
        'schemaVersion: "1.0"',
        'kind: "SIXPAY_ACTIVE_MASTER_PROMPT_INPUT"',
        f'authoritativeBranch: "{EXPECTED_BRANCH}"',
        'status: "ACTIVE"',
        'goldenBusinessModule: "backend/partner"',
        'owner: "customer"',
        'implementationRoot: "backend/customer"',
        'contractId: "customer-subscription-management-api-v1"',
        'systemOfRecord: "TRESOR_PAY"',
        'mvpIncluded: false',
        'paymentBoundary: "Payment neither owns nor manages the CustomerSubscription lifecycle."',
        'loadPolicy: "NEVER_LOAD_IN_ACTIVE_MASTER_PROMPT"',
        'expectedCount: 38',
        'status: "PASSED"',
    ]
    for literal in required_literals:
        if literal not in manifest:
            fail(f"missing required manifest rule: {literal}")

    if EXPECTED_BRANCH not in engineering:
        fail("ENGINEERING_CONTEXT.md does not declare the authoritative branch")
    if "activeManifest: MASTER_PROMPT_INPUT_MANIFEST.yaml" not in classification:
        fail("documentation classification does not reference the active manifest")

    precedence = quoted_list(section(manifest, "precedence", 2))
    if precedence != EXPECTED_PRECEDENCE:
        fail("manifest precedence differs from ENGINEERING_CONTEXT.md")

    always_load = quoted_list(section(manifest, "alwaysLoad", 2))
    if len(always_load) != len(set(always_load)):
        fail("contextSelection.alwaysLoad contains duplicates")
    for relative in always_load:
        if not (ROOT / relative).is_file():
            fail(f"always-load source does not exist: {relative}")

    active_contracts = contract_entries(
        section(manifest, "activeContractCapabilities", 0)
    )
    excluded_contracts = contract_entries(section(manifest, "excludedContracts", 0))
    if len(active_contracts) != 17 or len(set(active_contracts)) != 17:
        fail("expected 17 unique active/reference contract capabilities")
    if len(excluded_contracts) != 2 or len(set(excluded_contracts)) != 2:
        fail("expected 2 unique deferred contract capabilities")

    registry_contracts = registry_entries(registry)
    expected_active = {
        (entry["id"], entry["path"])
        for entry in registry_contracts
        if entry["lifecycle"] in {"ACTIVE_MVP", "REFERENCE_MVP"}
    }
    expected_excluded = {
        (entry["id"], entry["path"])
        for entry in registry_contracts
        if entry["lifecycle"] == "DEFERRED_FUTURE"
    }
    if set(active_contracts) != expected_active:
        fail("active contract selection is not synchronized with CONTRACT_REGISTRY.yaml")
    if set(excluded_contracts) != expected_excluded:
        fail("deferred contract exclusions are not synchronized with CONTRACT_REGISTRY.yaml")
    for _, relative in active_contracts + excluded_contracts:
        if not (ROOT / relative).is_file():
            fail(f"registered physical contract does not exist: {relative}")

    manifest_historical = quoted_list(
        section(manifest, "excludedHistoricalDocuments", 0)
    )
    classification_block = classification.split(
        "excludedHistoricalDocuments:", 1
    )[1].split("precedence:", 1)[0]
    classified_historical = re.findall(
        r"^\s+- (documentation/ai/[^\n]+)$",
        classification_block,
        re.MULTILINE,
    )
    if len(manifest_historical) != 38 or len(set(manifest_historical)) != 38:
        fail("expected 38 unique historical exclusions")
    if set(manifest_historical) != set(classified_historical):
        fail("historical exclusions differ from DOCUMENTATION_CLASSIFICATION.yaml")
    if set(always_load) & set(manifest_historical):
        fail("a historical AI document is present in alwaysLoad")
    for relative in manifest_historical:
        if not (ROOT / relative).is_file():
            fail(f"historical exclusion does not exist: {relative}")

    business_modules = set(quoted_list(section(manifest, "businessModules", 2)))
    platform_modules = set(quoted_list(section(manifest, "platformModules", 2)))
    declared_modules = business_modules | platform_modules | {
        "bootstrap",
        "sixpay-bom",
        "tests",
    }
    actual_modules = backend_modules(backend_pom)
    if declared_modules != actual_modules:
        fail("moduleModel is not synchronized with backend/pom.xml")
    if "subscription" in declared_modules or (ROOT / "backend/subscription").exists():
        fail("a standalone subscription module is declared or present")

    baseline_match = re.search(
        r'(?m)^  baselineCommit: "([0-9a-f]{40})"$', manifest
    )
    if baseline_match is None:
        fail("metadata.baselineCommit must be a full Git SHA")
    verify_git_ancestry(baseline_match.group(1))

    readiness = section(manifest, "readinessEvidence", 0)
    required_commands = [
        "verify_repository_hygiene.py",
        "verify_documentation_classification.py",
        "verify_documentation_final.py",
        "verify_feature_flag_registry.py",
        "verify_configuration_final.py",
        "npm run verify:contract-consolidation",
        "npm run verify:sixpay",
        "mvn verify",
        "mvn -Pfull-tests clean verify",
    ]
    for command in required_commands:
        if command not in readiness:
            fail(f"readiness evidence is missing: {command}")
    if re.search(r'(?m)^\s+status: "(?!PASSED")[^"]+"$', readiness):
        fail("readiness evidence contains a non-passed result")

    print("Active Master Prompt manifest gate PASSED.")
    print(f" - always-load sources: {len(always_load)}")
    print(f" - active/reference contract capabilities: {len(active_contracts)}")
    print(f" - deferred contract capabilities excluded: {len(excluded_contracts)}")
    print(f" - historical AI documents excluded: {len(manifest_historical)}")
    print(f" - backend modules classified: {len(actual_modules)}")
    print(" - CustomerSubscription ownership: customer")
    print(" - TRESOR PAY external subscription: deferred outside MVP")


if __name__ == "__main__":
    main()
