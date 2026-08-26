from pathlib import Path
import re
import sys

ROOT = Path.cwd()

CONTRACT_ROOT = ROOT / "documentation/contracts"
RUNBOOK_ROOT = ROOT / "documentation/runbooks"
REGISTRY = CONTRACT_ROOT / "CONTRACT_REGISTRY.yaml"

# Historical names are forbidden in operational runbooks.
# The runbooks root README is policy documentation and may mention
# these names explicitly as examples of forbidden references.
HISTORICAL = {
    "CONTRACT_REGISTRY_LOT0_PATCH.md",
    "payment-query-api-v1-status-alignment.patch",
    "administration-query-api-v1.yaml",
    "incident-query-api-v1.yaml",
}

CONTRACT_EXTENSIONS = {
    ".yaml",
    ".yml",
    ".json",
}

PATH_RE = re.compile(
    r"(documentation/contracts/[A-Za-z0-9_./\-]+"
    r"\.(?:yaml|yml|json|patch|md))"
)


def fail(errors):
    print("\nFS-2.7.4 contracts/runbooks reference validation FAILED:\n")
    for error in errors:
        print(" -", error)
    sys.exit(1)


def registry_paths(text):
    """
    Extract the physical paths explicitly declared by the canonical
    CONTRACT_REGISTRY.yaml.

    FS-2.7.4 deliberately does NOT infer canonicality by scanning every
    yaml/json file under documentation/contracts. Registry/filesystem integrity
    belongs to the existing FS-2.2 contract-consolidation gate.
    """
    paths = set()

    for match in re.finditer(
        r'(?m)^\s*path:\s*["\']?([^"\'\s#]+)["\']?\s*(?:#.*)?$',
        text,
    ):
        value = match.group(1).replace("\\", "/")

        if value.startswith("documentation/contracts/"):
            paths.add(value)
        elif value.startswith("contracts/"):
            paths.add("documentation/" + value)
        else:
            paths.add("documentation/contracts/" + value.lstrip("/"))

    return paths


def operational_runbooks():
    """
    The root README is governance/policy and may quote forbidden names.
    Only specialized operational runbooks are checked for stale references.
    """
    return sorted(
        path
        for path in RUNBOOK_ROOT.rglob("*.md")
        if path.is_file()
        and path != RUNBOOK_ROOT / "README.md"
    )


def main():
    errors = []

    if not REGISTRY.is_file():
        fail(["CONTRACT_REGISTRY.yaml is missing"])

    if not RUNBOOK_ROOT.is_dir():
        fail(["documentation/runbooks is missing"])

    registry_text = REGISTRY.read_text(
        encoding="utf-8",
        errors="ignore",
    )

    registered = registry_paths(registry_text)

    if not registered:
        fail([
            "no physical contract path could be extracted from "
            "CONTRACT_REGISTRY.yaml"
        ])

    # Registry paths referenced by runbooks must exist. This is a local
    # precondition for runbook reference validation, not a replacement for
    # FS-2.2 registry/filesystem integrity.
    for path in sorted(registered):
        if not (ROOT / path).is_file():
            errors.append(
                f"registered contract path does not exist: {path}"
            )

    runbooks = operational_runbooks()

    # Support references written as basenames when they resolve uniquely to
    # a registered physical contract.
    basename_to_paths = {}
    for path in registered:
        basename_to_paths.setdefault(
            Path(path).name,
            set(),
        ).add(path)

    for runbook in runbooks:
        text = runbook.read_text(
            encoding="utf-8",
            errors="ignore",
        )

        rel = runbook.relative_to(ROOT).as_posix()

        # Historical/removed artifacts must not appear in operational docs.
        for historical in HISTORICAL:
            if historical in text:
                errors.append(
                    f"{rel}: stale historical/removed contract reference: "
                    f"{historical}"
                )

        # Explicit documentation/contracts/... references.
        for match in PATH_RE.finditer(text):
            raw = match.group(1).rstrip(").,;:`'")

            if raw.endswith(".patch"):
                errors.append(
                    f"{rel}: patch artifact reference is forbidden: {raw}"
                )
                continue

            target = ROOT / raw

            if not target.is_file():
                errors.append(
                    f"{rel}: referenced contract path does not exist: {raw}"
                )
                continue

            if (
                target.suffix.lower() in CONTRACT_EXTENSIONS
                and target.name != "CONTRACT_REGISTRY.yaml"
                and raw not in registered
            ):
                errors.append(
                    f"{rel}: referenced physical contract is not registered: "
                    f"{raw}"
                )

        # Basename-only references are valid only if they resolve to an
        # explicitly registered contract. Non-registered yaml/json files are
        # ignored unless a runbook actually references them.
        for basename, paths in basename_to_paths.items():
            if basename not in text:
                continue

            if len(paths) > 1:
                errors.append(
                    f"{rel}: ambiguous contract basename reference "
                    f"{basename}; use the canonical full path"
                )

    if errors:
        fail(sorted(set(errors)))

    print("FS-2.7.4 contracts/runbooks reference validation PASSED.")
    print(
        f"Registered contract paths: {len(registered)}; "
        f"operational runbooks checked: {len(runbooks)}."
    )
    print(
        "Runbook references resolve only to existing registry paths; "
        "no stale historical contract reference remains."
    )
    print(
        "Registry <-> filesystem canonicality remains owned by the "
        "FS-2.2 contract-consolidation gate."
    )


if __name__ == "__main__":
    main()
