from pathlib import Path
import re
import sys

ROOT = Path.cwd()

AI_ROOT = ROOT / "documentation/ai"
AI_README = AI_ROOT / "README.md"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
DOC_INDEX = ROOT / "documentation/README.md"

REQUIRED_AREAS = {
    "customer",
    "integration",
    "payment",
}

HIGHER_PRIORITY_WORDING = [
    "authoritative implementation revision",
    "documentation/architecture/",
    "documentation/requirements/",
    "documentation/contracts/",
]

def fail(errors):
    print("\nFS-2.7.5 AI documentation validation FAILED:\n")
    for error in errors:
        print(" -", error)
    sys.exit(1)

def main():
    errors = []

    if not AI_README.is_file():
        errors.append("documentation/ai/README.md is missing")
    else:
        text = AI_README.read_text(
            encoding="utf-8",
            errors="ignore",
        )

        for wording in HIGHER_PRIORITY_WORDING:
            if wording not in text:
                errors.append(
                    f"AI README does not preserve source precedence: {wording}"
                )

        if "backend/partner" not in text:
            errors.append(
                "AI README does not preserve Partner golden-module rule"
            )

        if "HISTORICAL_AI_WORKING_ASSET" not in text:
            errors.append(
                "AI README does not classify historical working assets"
            )

    for area in REQUIRED_AREAS:
        if not (AI_ROOT / area).is_dir():
            errors.append(
                f"required AI area is missing: documentation/ai/{area}"
            )

    if not REGISTRY.is_file():
        errors.append("CONTRACT_REGISTRY.yaml is missing")
    else:
        registry = REGISTRY.read_text(
            encoding="utf-8",
            errors="ignore",
        )

        # Any explicit documentation/ai path referenced from the registry
        # must continue to exist.
        for match in re.finditer(
            r'documentation/ai/[A-Za-z0-9_./\-]+',
            registry,
        ):
            raw = match.group(0).rstrip(").,;:`'\"")
            if not (ROOT / raw).is_file():
                errors.append(
                    f"registry references missing AI traceability file: {raw}"
                )

    if not DOC_INDEX.is_file():
        errors.append("documentation/README.md is missing")
    else:
        index = DOC_INDEX.read_text(
            encoding="utf-8",
            errors="ignore",
        )

        if "documentation/ai/README.md" not in index:
            errors.append(
                "documentation/README.md does not point to AI canonical index"
            )

    if errors:
        fail(sorted(set(errors)))

    ai_files = [
        p for p in AI_ROOT.rglob("*")
        if p.is_file()
    ]

    historical = [
        p for p in ai_files
        if re.search(
            r"(?:LOT_|_LOT_|GATE_|PLAN|GAP_ANALYSIS|PREFLIGHT)",
            p.name,
            re.I,
        )
    ]

    print("FS-2.7.5 AI documentation validation PASSED.")
    print(
        f"AI files: {len(ai_files)}; "
        f"historical/working candidates for FS-2.7.6: {len(historical)}."
    )
    print(
        "AI source precedence, Partner golden-module rule and registry "
        "traceability references are preserved."
    )

if __name__ == "__main__":
    main()
