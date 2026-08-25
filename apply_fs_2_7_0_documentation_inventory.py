from pathlib import Path
from collections import defaultdict
import re
import sys

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
DOC_ROOT = ROOT / "documentation"
OUT = ROOT / "documentation/architecture/FS-2.7.0_DOCUMENTATION_INVENTORY.md"

TEXT_EXTENSIONS = {".md", ".yaml", ".yml", ".json", ".txt", ".properties"}
BINARY_DOC_EXTENSIONS = {".docx", ".pdf", ".xlsx", ".pptx"}

HISTORY_PATTERNS = [
    re.compile(r"(^|[_\-.])PATCH([_\-.]|$)", re.I),
    re.compile(r"(^|[_\-.])OLD([_\-.]|$)", re.I),
    re.compile(r"(^|[_\-.])BACKUP([_\-.]|$)", re.I),
    re.compile(r"\.bak$", re.I),
    re.compile(r"(^|[_\-.])DRAFT([_\-.]|$)", re.I),
    re.compile(r"FS-\d", re.I),
]

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8", errors="ignore")

def normalized_name(path):
    return re.sub(r"[^a-z0-9]+", "", path.stem.lower())

def classify(path):
    rel = path.relative_to(ROOT).as_posix()
    if any(p.search(path.name) for p in HISTORY_PATTERNS):
        return "REVIEW_HISTORY"
    if path.suffix.lower() in BINARY_DOC_EXTENSIONS:
        return "REVIEW_BINARY_SOURCE"
    if rel.startswith("documentation/contracts/"):
        return "CANONICAL_CONTRACT"
    if rel.startswith("documentation/requirements/"):
        return "CANONICAL_REQUIREMENT"
    if rel.startswith("documentation/architecture/"):
        return "CANONICAL_ARCHITECTURE_CANDIDATE"
    if rel.startswith("documentation/ai/"):
        return "CANONICAL_AI_CANDIDATE"
    if rel.startswith("documentation/runbooks/"):
        return "CANONICAL_RUNBOOK_CANDIDATE"
    return "DOCUMENTATION_OTHER"

def find_references(docs):
    refs = defaultdict(list)
    names = {p.name: p.relative_to(ROOT).as_posix() for p in docs}

    text_files = [
        p for p in ROOT.rglob("*")
        if p.is_file()
        and p.suffix.lower() in TEXT_EXTENSIONS
        and "target" not in p.parts
        and "node_modules" not in p.parts
        and ".git" not in p.parts
    ]

    for source in text_files:
        text = source.read_text(encoding="utf-8", errors="ignore")
        source_rel = source.relative_to(ROOT).as_posix()
        for name, target in names.items():
            if source_rel != target and name in text:
                refs[target].append(source_rel)
    return refs

def main():
    engineering = require(ENGINEERING)
    if EXPECTED_BRANCH not in engineering:
        fail("ENGINEERING_CONTEXT.md does not declare authoritative branch")

    docs = sorted(p for p in DOC_ROOT.rglob("*") if p.is_file())
    refs = find_references(docs)

    by_class = defaultdict(list)
    by_ext = defaultdict(int)
    normalized = defaultdict(list)

    for p in docs:
        by_class[classify(p)].append(p)
        by_ext[p.suffix.lower() or "<none>"] += 1
        normalized[normalized_name(p)].append(p)

    duplicate_groups = [
        paths for key, paths in normalized.items()
        if key and len(paths) > 1
    ]
    binaries = [p for p in docs if p.suffix.lower() in BINARY_DOC_EXTENSIONS]
    unreferenced_binaries = [
        p for p in binaries
        if not refs.get(p.relative_to(ROOT).as_posix())
    ]
    historical = by_class.get("REVIEW_HISTORY", [])

    lines = [
        "# FS-2.7.0 — Documentation Inventory",
        "",
        f"**Branch:** `{EXPECTED_BRANCH}`  ",
        "**Phase:** `FS-2.7 — Documentation consolidation`",
        "",
        "## Purpose",
        "",
        "Inventory and classify documentation before any merge, rename or deletion.",
        "",
        "No documentation file is deleted by FS-2.7.0.",
        "",
        "## Source-of-truth order",
        "",
        "```text",
        "1. authoritative implementation branch",
        "2. documentation/architecture/",
        "3. documentation/requirements/",
        "4. documentation/contracts/",
        "5. documentation/ai/",
        "6. engineering assets",
        "7. ENGINEERING_CONTEXT.md",
        "```",
        "",
        "## Inventory summary",
        "",
        f"- documentation files: **{len(docs)}**",
        f"- normalized-name duplicate groups: **{len(duplicate_groups)}**",
        f"- binary documents: **{len(binaries)}**",
        f"- unreferenced binary documents: **{len(unreferenced_binaries)}**",
        f"- historical/transitional candidates: **{len(historical)}**",
        "",
        "### By extension",
        "",
    ]

    for ext in sorted(by_ext):
        lines.append(f"- `{ext}`: **{by_ext[ext]}**")

    lines += ["", "### By classification", ""]
    for category in sorted(by_class):
        lines.append(f"- `{category}`: **{len(by_class[category])}**")

    lines += [
        "",
        "## Duplicate / near-duplicate names",
        "",
        "Similar names are review candidates, not automatic deletion candidates.",
        "",
    ]
    if duplicate_groups:
        for group in duplicate_groups:
            lines.append("- Group:")
            for p in group:
                rel = p.relative_to(ROOT).as_posix()
                lines.append(f"  - `{rel}` — references: **{len(refs.get(rel, []))}**")
    else:
        lines.append("None.")

    lines += ["", "## Binary sources requiring semantic review", ""]
    for p in binaries:
        rel = p.relative_to(ROOT).as_posix()
        lines.append(f"- `{rel}` — references: **{len(refs.get(rel, []))}**")

    lines += ["", "## Historical/transitional candidates", ""]
    if historical:
        for p in historical:
            rel = p.relative_to(ROOT).as_posix()
            lines.append(f"- `{rel}` — references: **{len(refs.get(rel, []))}**")
    else:
        lines.append("None.")

    lines += [
        "",
        "## FS-2.7 decision vocabulary",
        "",
        "```text",
        "KEEP_CANONICAL",
        "MERGE_INTO_CANONICAL",
        "KEEP_REFERENCE_SOURCE",
        "ARCHIVE_HISTORY",
        "DELETE_ABSORBED_HISTORY",
        "REVIEW_SEMANTIC_DUPLICATE",
        "```",
        "",
        "## Next step",
        "",
        "FS-2.7.1 defines the canonical documentation map/index and ownership before destructive cleanup.",
    ]

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print("FS-2.7.0 documentation inventory generated.")
    print("File:", OUT.relative_to(ROOT))
    print("Summary:")
    print(" - documentation files:", len(docs))
    print(" - duplicate groups:", len(duplicate_groups))
    print(" - binary documents:", len(binaries))
    print(" - unreferenced binary documents:", len(unreferenced_binaries))
    print(" - historical/transitional candidates:", len(historical))

if __name__ == "__main__":
    main()
