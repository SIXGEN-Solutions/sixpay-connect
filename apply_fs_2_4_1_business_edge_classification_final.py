from pathlib import Path
from collections import defaultdict
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

BACKEND = ROOT / "backend"
TARGET = (
    ROOT
    / "documentation/architecture/module-boundaries/"
    / "FS-2.4.1_BUSINESS_EDGE_CLASSIFICATION.md"
)

BUSINESS = [
    "partner",
    "customer",
    "payment",
    "accounting",
    "reporting",
    "notification",
    "security",
    "administration",
]

DISPLAY = {name: name.title() for name in BUSINESS}

IMPORT_RE = re.compile(
    r"^\s*import\s+(?:static\s+)?"
    r"(com\.sixpay\.[A-Za-z0-9_.$*]+)\s*;",
    re.MULTILINE,
)

NS = {"m": "http://maven.apache.org/POM/4.0.0"}

# Security contracts explicitly reviewed in FS-2.4.1.
PUBLIC_SECURITY_AUTHENTICATION_CONTRACTS = {
    "com.sixpay.security.authentication.CurrentUserProvider",
    "com.sixpay.security.authentication.AuthenticatedUser",
    "com.sixpay.security.authentication.SixpayPrincipal",
}


def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)


def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def target_module(import_name):
    parts = import_name.split(".")
    if len(parts) < 3:
        return None
    return parts[2] if parts[2] in BUSINESS else None


def classify(import_name):
    lower = import_name.lower()

    if ".infrastructure." in lower:
        if "repository" in lower:
            return "⚠ repository/infrastructure", "REFACTOR_TO_PORT", "BLOCKING"
        if ".entity." in lower or "jpaentity" in lower:
            return "⚠ JPA entity", "REFACTOR_TO_PUBLIC_CONTRACT", "BLOCKING"
        return "⚠ infrastructure", "REFACTOR_TO_PORT", "BLOCKING"

    if ".domain.repository." in lower:
        return "⚠ domain repository", "REFACTOR_TO_PORT", "BLOCKING"

    if import_name in PUBLIC_SECURITY_AUTHENTICATION_CONTRACTS:
        return (
            "✅ public security authentication-context contract",
            "KEEP_PUBLIC_SECURITY_CONTRACT",
            "ACCEPTED",
        )

    # Concrete Spring Security adapter remains internal.
    if import_name.endswith(
        ".authentication.SecurityContextCurrentUserProvider"
    ):
        return (
            "⚠ concrete SecurityContext adapter",
            "REFACTOR_TO_PUBLIC_SECURITY_CONTRACT",
            "BLOCKING",
        )

    if ".security.authorization." in lower:
        return (
            "✅ public security authorization contract",
            "KEEP_PUBLIC_SECURITY_CONTRACT",
            "ACCEPTED",
        )

    # Repository convention is application.port.in, not .input.
    if ".application.port.in." in lower or ".application.port.input." in lower:
        return (
            "✅ application input port",
            "KEEP_APPLICATION_PORT",
            "ACCEPTED",
        )

    if ".application.port.out." in lower:
        return (
            "⚠ foreign output port",
            "REFACTOR_TO_OWNED_PORT_OR_BOOTSTRAP_ADAPTER",
            "BLOCKING",
        )

    # Security application models returned by its published use cases are
    # application contracts, not persistence/domain internals.
    if ".security.application.model." in lower:
        return (
            "✅ Security application response contract",
            "KEEP_APPLICATION_CONTRACT",
            "ACCEPTED",
        )

    if ".domain.event." in lower or ".events." in lower:
        return "✅ event contract", "KEEP_EVENT", "ACCEPTED"

    if ".domain.model." in lower or ".domain.value." in lower:
        return (
            "✅ published domain value contract",
            "KEEP_PUBLIC_DOMAIN_CONTRACT",
            "ACCEPTED",
        )

    if ".domain." in lower:
        return (
            "✅ published domain contract",
            "KEEP_PUBLIC_DOMAIN_CONTRACT",
            "ACCEPTED",
        )

    if ".api." in lower:
        return (
            "⚠ HTTP/API implementation surface",
            "REFACTOR_TO_PUBLIC_CONTRACT",
            "BLOCKING",
        )

    if ".security.jwt." in lower:
        return (
            "⚠ JWT implementation surface",
            "REFACTOR_TO_PUBLIC_SECURITY_CONTRACT",
            "BLOCKING",
        )

    if ".security.config." in lower or ".security.configuration." in lower:
        return (
            "⚠ Security configuration implementation",
            "REMOVE_DEPENDENCY",
            "BLOCKING",
        )

    return (
        "⚠ unclassified business-module surface",
        "REVIEW_REQUIRED",
        "BLOCKING",
    )


def maven_business_dependencies(module):
    pom = BACKEND / module / "pom.xml"
    if not pom.is_file():
        return set()

    root = ET.parse(pom).getroot()
    result = set()

    for dep in root.findall(".//m:dependencies/m:dependency", NS):
        group = dep.findtext("m:groupId", default="", namespaces=NS)
        artifact = dep.findtext("m:artifactId", default="", namespaces=NS)
        scope = dep.findtext("m:scope", default="compile", namespaces=NS)

        if (
            group == "com.sixpay"
            and artifact in BUSINESS
            and artifact != module
            and scope != "test"
        ):
            result.add(artifact)

    return result


def scan_edges():
    edges = defaultdict(list)

    for source in BUSINESS:
        java_root = BACKEND / source / "src/main/java"
        if not java_root.is_dir():
            continue

        for path in java_root.rglob("*.java"):
            text = path.read_text(encoding="utf-8")

            for imported in IMPORT_RE.findall(text):
                target = target_module(imported)

                if target is None or target == source:
                    continue

                classification, decision, status = classify(imported)

                edges[(source, target)].append({
                    "file": str(path.relative_to(ROOT)),
                    "import": imported,
                    "classification": classification,
                    "decision": decision,
                    "status": status,
                })

    return edges


def detect_cycles(edges):
    graph = {module: set() for module in BUSINESS}
    for (source, target), usages in edges.items():
        if usages:
            graph[source].add(target)

    cycles = []
    path = []
    visited = set()

    def dfs(node):
        if node in path:
            i = path.index(node)
            cycles.append(path[i:] + [node])
            return
        if node in visited:
            return

        path.append(node)
        for target in sorted(graph[node]):
            dfs(target)
        path.pop()
        visited.add(node)

    for module in BUSINESS:
        dfs(module)

    return cycles


def main():
    engineering = require(ENGINEERING)
    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            + EXPECTED_BRANCH
        )

    edges = scan_edges()
    cycles = detect_cycles(edges)

    maven_edges = {
        module: maven_business_dependencies(module)
        for module in BUSINESS
    }

    blocking = [
        (source, target, usage)
        for (source, target), usages in edges.items()
        for usage in usages
        if usage["status"] == "BLOCKING"
    ]

    unused_maven = [
        (source, target)
        for source, targets in maven_edges.items()
        for target in targets
        if not edges.get((source, target))
    ]

    lines = [
        "# FS-2.4.1 — Business-to-Business Edge Classification",
        "",
        "**Branch:** `feat/repository-baseline-consolidation`  ",
        "**Phase:** `FS-2.4 — Dependency and module boundary audit`  ",
        "**Golden module:** Partner",
        "",
        "## Reviewed public seams",
        "",
        "The following Security surfaces are explicitly treated as published "
        "module contracts:",
        "",
        "- `security.authentication.CurrentUserProvider`",
        "- `security.authentication.AuthenticatedUser`",
        "- `security.authentication.SixpayPrincipal`",
        "- `security.authorization.*`",
        "- `security.application.port.in.*` / `security.application.port.input.*`",
        "- `security.application.model.*` when returned by published use cases",
        "",
        "`application.port.in` and `application.port.input` are treated as the same "
        "architectural input-port convention during baseline consolidation; no package "
        "rename is required by FS-2.4.1.",
        "",
        "`SecurityContextCurrentUserProvider`, Security infrastructure, JWT "
        "implementation and configuration remain internal.",
        "",
        "**Regression policy:** FS-2.4.1 is classification-first. Existing functional "
        "code is not moved or renamed merely to satisfy package naming consistency. "
        "Only proven boundary violations may trigger a targeted refactor, followed by "
        "module/consumer tests and full reactor verification.",
        "",
        "## Edge matrix",
        "",
        "| Source | Target | Maven | Imports | Decision | Status |",
        "|---|---|---:|---:|---|---|",
    ]

    for source, target in sorted(edges):
        usages = edges[(source, target)]
        decisions = {u["decision"] for u in usages}
        is_blocking = any(u["status"] == "BLOCKING" for u in usages)

        decision = (
            "BLOCKING_REFACTOR"
            if is_blocking
            else (
                next(iter(decisions))
                if len(decisions) == 1
                else "KEEP_COMPOSED_PUBLIC_CONTRACTS"
            )
        )

        lines.append(
            f"| {DISPLAY[source]} | {DISPLAY[target]} | "
            f"{'yes' if target in maven_edges[source] else 'no'} | "
            f"{len(usages)} | `{decision}` | "
            f"{'⚠ REFACTOR' if is_blocking else '✅ ACCEPT'} |"
        )

    lines += ["", "## Detailed classification", ""]

    for source, target in sorted(edges):
        lines += [
            f"### {DISPLAY[source]} → {DISPLAY[target]}",
            "",
        ]

        unique = {}
        for usage in edges[(source, target)]:
            unique[(usage["file"], usage["import"])] = usage

        for usage in unique.values():
            lines.append(
                f"- {usage['classification']} — `{usage['import']}` — "
                f"`{usage['file']}` — `{usage['decision']}`"
            )

        lines.append("")

    lines += ["## Maven-only dependency review", ""]

    if unused_maven:
        for source, target in unused_maven:
            lines.append(
                f"- ⚠ {DISPLAY[source]} → {DISPLAY[target]}: "
                "`REMOVE_UNUSED_DEPENDENCY`"
            )
    else:
        lines.append("No unused business Maven dependency detected.")

    lines += [
        "",
        "## Result",
        "",
        f"- Business edges: **{len(edges)}**",
        f"- Blocking imports: **{len(blocking)}**",
        f"- Maven-only dependencies: **{len(unused_maven)}**",
        f"- Circular dependencies: **{len(cycles)}**",
        "",
    ]

    if not blocking and not cycles:
        lines.append(
            "**Code-level business edges PASS.** "
            "Any Maven-only dependency remains cleanup debt."
        )
    else:
        lines.append(
            "**REFACTOR REQUIRED before FS-2.4.1 closure.**"
        )

    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print("FS-2.4.1 classification regenerated with repository conventions.")
    print("File:", TARGET.relative_to(ROOT))
    print()
    print("Result:")
    print(" - business edges:", len(edges))
    print(" - blocking imports:", len(blocking))
    print(" - Maven-only dependencies:", len(unused_maven))
    print(" - circular dependencies:", len(cycles))

    if blocking:
        print("\nBLOCKING:")
        for source, target, usage in blocking:
            print(
                f" - {DISPLAY[source]} -> {DISPLAY[target]}: "
                f"{usage['import']}"
            )

    if unused_maven:
        print("\nMaven cleanup:")
        for source, target in unused_maven:
            print(
                f" - {DISPLAY[source]} -> {DISPLAY[target]} "
                "(REMOVE_UNUSED_DEPENDENCY)"
            )


if __name__ == "__main__":
    main()
