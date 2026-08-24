from pathlib import Path
from collections import defaultdict
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

BACKEND = ROOT / "backend"
DOC = (
    ROOT
    / "documentation/architecture/module-boundaries/"
    / "FS-2.4.0_MODULE_DEPENDENCY_AUDIT.md"
)

BUSINESS_MODULES = [
    "partner",
    "customer",
    "payment",
    "accounting",
    "reporting",
    "notification",
    "security",
    "administration",
]

PLATFORM_MODULES = [
    "common",
    "shared-kernel",
    "integration",
]

DISPLAY = {
    "partner": "Partner",
    "customer": "Customer",
    "payment": "Payment",
    "accounting": "Accounting",
    "reporting": "Reporting",
    "notification": "Notification",
    "security": "Security",
    "administration": "Administration",
    "common": "Common",
    "shared-kernel": "Shared Kernel",
    "integration": "Integration",
}

PACKAGE_TO_MODULE = {
    "partner": "partner",
    "customer": "customer",
    "payment": "payment",
    "accounting": "accounting",
    "reporting": "reporting",
    "notification": "notification",
    "security": "security",
    "administration": "administration",
    "common": "common",
    "shared": "shared-kernel",
    "integration": "integration",
}

IMPORT_PATTERN = re.compile(
    r"^\s*import\s+(?:static\s+)?(com\.sixpay\.[A-Za-z0-9_.$*]+)\s*;",
    re.MULTILINE,
)

NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)


def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def module_from_import(import_name):
    parts = import_name.split(".")
    if len(parts) < 3 or parts[0:2] != ["com", "sixpay"]:
        return None

    top = parts[2]

    if top in PACKAGE_TO_MODULE:
        return PACKAGE_TO_MODULE[top]

    if top in BUSINESS_MODULES:
        return top

    return None


def classify_import(import_name, target):
    lower = import_name.lower()

    if target == "shared-kernel":
        return "✅ shared-kernel value object"

    if target == "integration":
        return "✅ provider-neutral integration"

    if ".infrastructure." in lower:
        if ".entity." in lower or lower.endswith("entity"):
            return "⚠ JPA entity import"
        if ".repository." in lower or "repository" in lower:
            return "⚠ direct repository access"
        return "⚠ infrastructure import"

    if ".domain.repository." in lower:
        return "⚠ direct repository access"

    if ".entity." in lower or lower.endswith("entity"):
        return "⚠ JPA entity import"

    if ".application.port." in lower:
        return "✅ application port"

    if ".domain.event." in lower or ".event." in lower:
        return "✅ event"

    if ".domain." in lower:
        return "✅ public domain contract"

    if target == "common":
        return "✅ common/platform contract"

    if target == "security":
        return "✅ public security contract (REVIEW API SURFACE)"

    return "⚠ unclassified cross-module import"


def read_maven_dependencies(module):
    pom = BACKEND / module / "pom.xml"
    if not pom.is_file():
        return []

    root = ET.parse(pom).getroot()
    dependencies = []

    for dependency in root.findall(".//m:dependencies/m:dependency", NS):
        group = dependency.findtext("m:groupId", default="", namespaces=NS)
        artifact = dependency.findtext("m:artifactId", default="", namespaces=NS)
        scope = dependency.findtext("m:scope", default="compile", namespaces=NS)

        if group == "com.sixpay":
            dependencies.append((artifact, scope))

    return dependencies


def scan_imports():
    edges = defaultdict(list)

    for source_module in BUSINESS_MODULES:
        java_root = BACKEND / source_module / "src/main/java"
        if not java_root.is_dir():
            continue

        for java_file in java_root.rglob("*.java"):
            text = java_file.read_text(encoding="utf-8")

            for import_name in IMPORT_PATTERN.findall(text):
                target = module_from_import(import_name)

                if target is None or target == source_module:
                    continue

                edges[(source_module, target)].append({
                    "file": str(java_file.relative_to(ROOT)),
                    "import": import_name,
                    "classification": classify_import(import_name, target),
                })

    return edges


def build_cycles(edges):
    graph = {
        module: set()
        for module in BUSINESS_MODULES
    }

    for (source, target), usages in edges.items():
        if (
            usages
            and source in BUSINESS_MODULES
            and target in BUSINESS_MODULES
        ):
            graph[source].add(target)

    cycles = []

    def visit(node, path):
        if node in path:
            index = path.index(node)
            cycle = path[index:] + [node]
            canonical = tuple(cycle)

            if canonical not in cycles:
                cycles.append(canonical)
            return

        if len(path) > len(BUSINESS_MODULES):
            return

        for nxt in graph[node]:
            visit(nxt, path + [node])

    for node in BUSINESS_MODULES:
        visit(node, [])

    # deduplicate rotations by edge set
    unique = []
    seen = set()

    for cycle in cycles:
        edgeset = frozenset(zip(cycle, cycle[1:]))

        if edgeset not in seen:
            seen.add(edgeset)
            unique.append(cycle)

    return unique


def matrix_cell(source, target, edges):
    if source == target:
        return "—"

    usages = edges.get((source, target), [])

    if not usages:
        return ""

    classes = {usage["classification"] for usage in usages}

    if any(value.startswith("⚠") for value in classes):
        return "⚠"

    return "✅"


def main():
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            + EXPECTED_BRANCH
        )

    missing_modules = [
        module
        for module in BUSINESS_MODULES
        if not (BACKEND / module / "pom.xml").is_file()
    ]

    if missing_modules:
        fail(
            "Missing audited business modules: "
            + ", ".join(missing_modules)
        )

    edges = scan_imports()
    cycles = build_cycles(edges)

    maven = {
        module: read_maven_dependencies(module)
        for module in BUSINESS_MODULES
    }

    lines = []
    lines.append("# FS-2.4.0 — Module Dependency & Boundary Inventory")
    lines.append("")
    lines.append(
        "**Branch:** `feat/repository-baseline-consolidation`  "
    )
    lines.append(
        "**Phase:** `FS-2.4 — Dependency and module boundary audit`  "
    )
    lines.append("**Golden module:** Partner")
    lines.append("")
    lines.append("## Purpose")
    lines.append("")
    lines.append(
        "This inventory is generated from the current Maven descriptors "
        "and production Java imports. It makes no implementation change."
    )
    lines.append("")
    lines.append("## Classification vocabulary")
    lines.append("")
    lines.append("- ✅ application port")
    lines.append("- ✅ public domain contract")
    lines.append("- ✅ shared-kernel value object")
    lines.append("- ✅ event")
    lines.append("- ✅ provider-neutral integration")
    lines.append("- ✅ common/platform contract")
    lines.append("- ⚠ direct repository access")
    lines.append("- ⚠ JPA entity import")
    lines.append("- ⚠ infrastructure import")
    lines.append("- ⚠ unclassified cross-module import")
    lines.append("- ❌ circular dependency")
    lines.append("")
    lines.append("## Business-module Java dependency matrix")
    lines.append("")

    headers = [
        "Source",
        "Ptn",
        "Cus",
        "Pay",
        "Acc",
        "Rep",
        "Not",
        "Sec",
        "Adm",
    ]

    lines.append("| " + " | ".join(headers) + " |")
    lines.append("|" + "|".join(["---"] * len(headers)) + "|")

    abbreviations = {
        "partner": "Ptn",
        "customer": "Cus",
        "payment": "Pay",
        "accounting": "Acc",
        "reporting": "Rep",
        "notification": "Not",
        "security": "Sec",
        "administration": "Adm",
    }

    for source in BUSINESS_MODULES:
        row = [DISPLAY[source]]

        for target in BUSINESS_MODULES:
            row.append(matrix_cell(source, target, edges))

        lines.append("| " + " | ".join(row) + " |")

    lines.append("")
    lines.append(
        "`✅` means all discovered imports on that edge are currently "
        "classified as boundary-safe by the inventory heuristic. "
        "`⚠` requires explicit review."
    )
    lines.append("")

    lines.append("## Maven business-module dependencies")
    lines.append("")

    for module in BUSINESS_MODULES:
        deps = [
            (artifact, scope)
            for artifact, scope in maven[module]
            if artifact in BUSINESS_MODULES
            and artifact != module
        ]

        rendered = (
            ", ".join(
                f"`{artifact}` ({scope})"
                for artifact, scope in deps
            )
            if deps
            else "_none_"
        )

        lines.append(
            f"- **{DISPLAY[module]}** → {rendered}"
        )

    lines.append("")
    lines.append("## Detailed Java cross-module imports")
    lines.append("")

    if not edges:
        lines.append("_No production cross-module imports found._")
    else:
        for source, target in sorted(edges):
            usages = edges[(source, target)]

            lines.append(
                f"### {DISPLAY[source]} → {DISPLAY.get(target, target)}"
            )
            lines.append("")

            grouped = defaultdict(list)

            for usage in usages:
                grouped[usage["classification"]].append(usage)

            for classification, classified_usages in sorted(grouped.items()):
                lines.append(f"**{classification}**")
                lines.append("")

                dedup = {}
                for usage in classified_usages:
                    dedup[
                        (
                            usage["file"],
                            usage["import"],
                        )
                    ] = usage

                for usage in dedup.values():
                    lines.append(
                        f"- `{usage['import']}` — `{usage['file']}`"
                    )

                lines.append("")

    lines.append("## Platform dependencies")
    lines.append("")

    for source in BUSINESS_MODULES:
        for target in PLATFORM_MODULES:
            usages = edges.get((source, target), [])

            if not usages:
                continue

            classifications = sorted({
                usage["classification"]
                for usage in usages
            })

            lines.append(
                f"- **{DISPLAY[source]} → {DISPLAY[target]}**: "
                + ", ".join(classifications)
            )

    lines.append("")
    lines.append("## Circular dependency analysis")
    lines.append("")

    if cycles:
        for cycle in cycles:
            rendered = " → ".join(
                DISPLAY.get(module, module)
                for module in cycle
            )
            lines.append(f"- ❌ `{rendered}`")
    else:
        lines.append(
            "No circular business-module dependency discovered "
            "from production Java imports."
        )

    warnings = []

    for edge, usages in edges.items():
        for usage in usages:
            if usage["classification"].startswith("⚠"):
                warnings.append(usage)

    lines.append("")
    lines.append("## Initial audit result")
    lines.append("")
    lines.append(
        f"- Production cross-module edges discovered: **{len(edges)}**"
    )
    lines.append(
        f"- Warning-level imports requiring review: **{len(warnings)}**"
    )
    lines.append(
        f"- Circular business-module dependencies: **{len(cycles)}**"
    )
    lines.append("")
    lines.append(
        "FS-2.4.0 is an inventory only. A warning does not automatically "
        "mean the implementation is wrong; it identifies an edge that must "
        "be classified against the owning module's public API."
    )
    lines.append("")
    lines.append("## Required next step")
    lines.append("")
    lines.append(
        "FS-2.4.1 must review every business-to-business edge and define "
        "the allowed public package surface. Infrastructure, JPA entity and "
        "repository imports across modules must be eliminated."
    )

    DOC.parent.mkdir(parents=True, exist_ok=True)
    DOC.write_text(
        "\n".join(lines) + "\n",
        encoding="utf-8"
    )

    print("FS-2.4.0 module dependency inventory generated.")
    print("File:", DOC.relative_to(ROOT))
    print()
    print("Summary:")
    print(" - cross-module edges:", len(edges))
    print(" - warning imports:", len(warnings))
    print(" - circular dependencies:", len(cycles))
    print()
    print("Review:")
    print(
        " git diff -- "
        "documentation/architecture/module-boundaries/"
        "FS-2.4.0_MODULE_DEPENDENCY_AUDIT.md"
    )


if __name__ == "__main__":
    main()
