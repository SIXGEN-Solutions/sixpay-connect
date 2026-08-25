from pathlib import Path
import sys

ROOT = Path.cwd()
TARGET = ROOT / "scripts/verify_documentation_contract_references.py"

CONTENT = 'from pathlib import Path\nimport re\nimport sys\n\nROOT = Path.cwd()\n\nCONTRACT_ROOT = ROOT / "documentation/contracts"\nRUNBOOK_ROOT = ROOT / "documentation/runbooks"\nREGISTRY = CONTRACT_ROOT / "CONTRACT_REGISTRY.yaml"\n\n# Historical names are forbidden in operational runbooks.\n# The runbooks root README is policy documentation and may mention\n# these names explicitly as examples of forbidden references.\nHISTORICAL = {\n    "CONTRACT_REGISTRY_LOT0_PATCH.md",\n    "payment-query-api-v1-status-alignment.patch",\n    "administration-query-api-v1.yaml",\n    "incident-query-api-v1.yaml",\n}\n\nCONTRACT_EXTENSIONS = {\n    ".yaml",\n    ".yml",\n    ".json",\n}\n\nPATH_RE = re.compile(\n    r"(documentation/contracts/[A-Za-z0-9_./\\-]+"\n    r"\\.(?:yaml|yml|json|patch|md))"\n)\n\n\ndef fail(errors):\n    print("\\nFS-2.7.4 contracts/runbooks reference validation FAILED:\\n")\n    for error in errors:\n        print(" -", error)\n    sys.exit(1)\n\n\ndef registry_paths(text):\n    """\n    Extract the physical paths explicitly declared by the canonical\n    CONTRACT_REGISTRY.yaml.\n\n    FS-2.7.4 deliberately does NOT infer canonicality by scanning every\n    yaml/json file under documentation/contracts. Registry/filesystem integrity\n    belongs to the existing FS-2.2 contract-consolidation gate.\n    """\n    paths = set()\n\n    for match in re.finditer(\n        r\'(?m)^\\s*path:\\s*["\\\']?([^"\\\'\\s#]+)["\\\']?\\s*(?:#.*)?$\',\n        text,\n    ):\n        value = match.group(1).replace("\\\\", "/")\n\n        if value.startswith("documentation/contracts/"):\n            paths.add(value)\n        elif value.startswith("contracts/"):\n            paths.add("documentation/" + value)\n        else:\n            paths.add("documentation/contracts/" + value.lstrip("/"))\n\n    return paths\n\n\ndef operational_runbooks():\n    """\n    The root README is governance/policy and may quote forbidden names.\n    Only specialized operational runbooks are checked for stale references.\n    """\n    return sorted(\n        path\n        for path in RUNBOOK_ROOT.rglob("*.md")\n        if path.is_file()\n        and path != RUNBOOK_ROOT / "README.md"\n    )\n\n\ndef main():\n    errors = []\n\n    if not REGISTRY.is_file():\n        fail(["CONTRACT_REGISTRY.yaml is missing"])\n\n    if not RUNBOOK_ROOT.is_dir():\n        fail(["documentation/runbooks is missing"])\n\n    registry_text = REGISTRY.read_text(\n        encoding="utf-8",\n        errors="ignore",\n    )\n\n    registered = registry_paths(registry_text)\n\n    if not registered:\n        fail([\n            "no physical contract path could be extracted from "\n            "CONTRACT_REGISTRY.yaml"\n        ])\n\n    # Registry paths referenced by runbooks must exist. This is a local\n    # precondition for runbook reference validation, not a replacement for\n    # FS-2.2 registry/filesystem integrity.\n    for path in sorted(registered):\n        if not (ROOT / path).is_file():\n            errors.append(\n                f"registered contract path does not exist: {path}"\n            )\n\n    runbooks = operational_runbooks()\n\n    # Support references written as basenames when they resolve uniquely to\n    # a registered physical contract.\n    basename_to_paths = {}\n    for path in registered:\n        basename_to_paths.setdefault(\n            Path(path).name,\n            set(),\n        ).add(path)\n\n    for runbook in runbooks:\n        text = runbook.read_text(\n            encoding="utf-8",\n            errors="ignore",\n        )\n\n        rel = runbook.relative_to(ROOT).as_posix()\n\n        # Historical/removed artifacts must not appear in operational docs.\n        for historical in HISTORICAL:\n            if historical in text:\n                errors.append(\n                    f"{rel}: stale historical/removed contract reference: "\n                    f"{historical}"\n                )\n\n        # Explicit documentation/contracts/... references.\n        for match in PATH_RE.finditer(text):\n            raw = match.group(1).rstrip(").,;:`\'")\n\n            if raw.endswith(".patch"):\n                errors.append(\n                    f"{rel}: patch artifact reference is forbidden: {raw}"\n                )\n                continue\n\n            target = ROOT / raw\n\n            if not target.is_file():\n                errors.append(\n                    f"{rel}: referenced contract path does not exist: {raw}"\n                )\n                continue\n\n            if (\n                target.suffix.lower() in CONTRACT_EXTENSIONS\n                and target.name != "CONTRACT_REGISTRY.yaml"\n                and raw not in registered\n            ):\n                errors.append(\n                    f"{rel}: referenced physical contract is not registered: "\n                    f"{raw}"\n                )\n\n        # Basename-only references are valid only if they resolve to an\n        # explicitly registered contract. Non-registered yaml/json files are\n        # ignored unless a runbook actually references them.\n        for basename, paths in basename_to_paths.items():\n            if basename not in text:\n                continue\n\n            if len(paths) > 1:\n                errors.append(\n                    f"{rel}: ambiguous contract basename reference "\n                    f"{basename}; use the canonical full path"\n                )\n\n    if errors:\n        fail(sorted(set(errors)))\n\n    print("FS-2.7.4 contracts/runbooks reference validation PASSED.")\n    print(\n        f"Registered contract paths: {len(registered)}; "\n        f"operational runbooks checked: {len(runbooks)}."\n    )\n    print(\n        "Runbook references resolve only to existing registry paths; "\n        "no stale historical contract reference remains."\n    )\n    print(\n        "Registry <-> filesystem canonicality remains owned by the "\n        "FS-2.2 contract-consolidation gate."\n    )\n\n\nif __name__ == "__main__":\n    main()\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def main():
    if not TARGET.is_file():
        fail(
            "FS-2.7.4 verifier not found: "
            + str(TARGET)
        )

    TARGET.write_text(
        CONTENT,
        encoding="utf-8",
    )

    print("FS-2.7.4 reference verifier corrected.")
    print()
    print("Corrected semantics:")
    print(" - registry paths are read from CONTRACT_REGISTRY.yaml")
    print(" - no second physical-contract canonicality inventory")
    print(" - root runbooks README excluded from stale-reference scan")
    print(" - specialized runbooks remain strictly validated")
    print(" - FS-2.2 remains owner of registry/filesystem integrity")
    print()
    print("No contract, registry or runbook content was changed.")
    print()
    print("Run:")
    print("  py scripts/verify_documentation_contract_references.py")
    print("  cd frontend && npm run verify:contract-consolidation")

if __name__ == "__main__":
    main()
