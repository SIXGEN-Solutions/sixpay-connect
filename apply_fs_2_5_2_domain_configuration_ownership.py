from pathlib import Path
from collections import defaultdict
import re
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

BACKEND = ROOT / "backend"

DOC = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.2_DOMAIN_CONFIGURATION_OWNERSHIP.md"
)

TEST = (
    ROOT
    / "backend/bootstrap/src/test/java/com/sixpay/bootstrap/architecture/"
    / "DomainConfigurationOwnershipArchitectureTest.java"
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

DOMAIN_PREFIX = {
    module: f"sixpay.{module}."
    for module in BUSINESS
}

CONFIG_PROPERTIES_RE = re.compile(
    r'@ConfigurationProperties\s*\([^)]*prefix\s*=\s*"([^"]+)"',
    re.DOTALL,
)

VALUE_RE = re.compile(
    r'@Value\s*\(\s*"\$\{([^}:]+)'
)

CONDITIONAL_RE = re.compile(
    r'@ConditionalOnProperty\s*\((.*?)\)',
    re.DOTALL,
)

BASE_DOC = "# FS-2.5.2 — Domain Configuration Ownership\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Phase:** `FS-2.5 — Configuration consolidation`  \n**Golden module:** Partner\n\n## Canonical ownership rule\n\n```text\nsixpay.partner.*        -> Partner\nsixpay.customer.*       -> Customer\nsixpay.payment.*        -> Payment\nsixpay.accounting.*     -> Accounting\nsixpay.reporting.*      -> Reporting\nsixpay.notification.*   -> Notification\nsixpay.security.*       -> Security\nsixpay.administration.* -> Administration\n```\n\nPhysical YAML location and semantic ownership are deliberately separated during\nconsolidation. Bootstrap may still provide runtime values, but it does not become\nthe semantic owner of domain keys.\n\n## Rules\n\n1. A business module owns the meaning, defaults and validation of its\n   `sixpay.<domain>.*` configuration.\n2. Bootstrap may assemble values but must not define business semantics for\n   those keys.\n3. A business module must not read another domain's configuration directly.\n4. Cross-domain behavior must go through application/domain contracts, not\n   through shared property reads.\n5. Property keys and environment-variable names remain unchanged in FS-2.5.2.\n6. Existing profile behavior remains unchanged.\n7. Package/configuration-class renames are out of scope unless needed to fix a\n   proven ownership violation.\n\n## Physical relocation policy\n\nFS-2.5.2 does not require moving all domain YAML into module resources. The\ncurrent modular-monolith runtime may keep profile values in Bootstrap while\nsemantic ownership and Java binding stay with the domain. Physical profile\nconsolidation is handled in FS-2.5.3.\n"
JAVA = 'package com.sixpay.bootstrap.architecture;\n\nimport org.junit.jupiter.api.Test;\n\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.util.ArrayList;\nimport java.util.LinkedHashMap;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.regex.Matcher;\nimport java.util.regex.Pattern;\n\nimport static org.junit.jupiter.api.Assertions.assertTrue;\n\nclass DomainConfigurationOwnershipArchitectureTest {\n\n    private static final Path BACKEND_ROOT =\n            Path.of("..");\n\n    private static final List<String> BUSINESS_MODULES =\n            List.of(\n                    "partner",\n                    "customer",\n                    "payment",\n                    "accounting",\n                    "reporting",\n                    "notification",\n                    "security",\n                    "administration"\n            );\n\n    private static final Map<String, String>\n            DOMAIN_PREFIXES =\n            new LinkedHashMap<>();\n\n    static {\n        for (String module : BUSINESS_MODULES) {\n            DOMAIN_PREFIXES.put(\n                    module,\n                    "sixpay." + module + "."\n            );\n        }\n    }\n\n    private static final Pattern CONFIG_PROPERTIES =\n            Pattern.compile(\n                    "@ConfigurationProperties\\\\s*\\\\([^)]*"\n                            + "prefix\\\\s*=\\\\s*\\"([^\\"]+)\\"",\n                    Pattern.DOTALL\n            );\n\n    private static final Pattern VALUE =\n            Pattern.compile(\n                    "@Value\\\\s*\\\\(\\\\s*\\"\\\\$\\\\{([^}:]+)"\n            );\n\n    private static final Pattern CONDITIONAL =\n            Pattern.compile(\n                    "@ConditionalOnProperty\\\\s*\\\\((.*?)\\\\)",\n                    Pattern.DOTALL\n            );\n\n    @Test\n    void businessModulesDoNotConsumeOtherDomainConfiguration()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (String module : BUSINESS_MODULES) {\n\n            Path javaRoot =\n                    BACKEND_ROOT.resolve(\n                            module + "/src/main/java"\n                    );\n\n            if (!Files.isDirectory(javaRoot)) {\n                continue;\n            }\n\n            try (var files = Files.walk(javaRoot)) {\n                for (Path javaFile :\n                        files.filter(Files::isRegularFile)\n                                .filter(path ->\n                                        path.toString()\n                                                .endsWith(".java")\n                                )\n                                .toList()) {\n\n                    String source =\n                            Files.readString(javaFile);\n\n                    for (String key :\n                            propertyKeys(source)) {\n\n                        String owner =\n                                ownerOf(key);\n\n                        if (owner != null\n                                && !owner.equals(module)) {\n\n                            violations.add(\n                                    module\n                                            + " consumes "\n                                            + key\n                                            + " owned by "\n                                            + owner\n                                            + " in "\n                                            + javaFile\n                            );\n                        }\n                    }\n                }\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Cross-domain configuration ownership "\n                        + "violations: "\n                        + violations\n        );\n    }\n\n    private List<String> propertyKeys(\n            String source\n    ) {\n\n        List<String> keys =\n                new ArrayList<>();\n\n        Matcher properties =\n                CONFIG_PROPERTIES.matcher(source);\n\n        while (properties.find()) {\n            keys.add(properties.group(1));\n        }\n\n        Matcher values =\n                VALUE.matcher(source);\n\n        while (values.find()) {\n            keys.add(values.group(1));\n        }\n\n        Matcher conditionals =\n                CONDITIONAL.matcher(source);\n\n        while (conditionals.find()) {\n\n            String block =\n                    conditionals.group(1);\n\n            Matcher prefix =\n                    Pattern.compile(\n                            "prefix\\\\s*=\\\\s*\\"([^\\"]+)\\""\n                    ).matcher(block);\n\n            if (!prefix.find()) {\n                continue;\n            }\n\n            String key =\n                    prefix.group(1);\n\n            Matcher name =\n                    Pattern.compile(\n                            "name\\\\s*=\\\\s*\\"([^\\"]+)\\""\n                    ).matcher(block);\n\n            if (name.find()) {\n                key += "." + name.group(1);\n            }\n\n            keys.add(key);\n        }\n\n        return keys;\n    }\n\n    private String ownerOf(\n            String key\n    ) {\n\n        String normalized =\n                key.endsWith(".")\n                        ? key\n                        : key + ".";\n\n        for (var entry :\n                DOMAIN_PREFIXES.entrySet()) {\n\n            if (normalized.startsWith(\n                    entry.getValue()\n            )) {\n                return entry.getKey();\n            }\n        }\n\n        return null;\n    }\n}\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def extract_property_keys(source):
    found = []

    for prefix in CONFIG_PROPERTIES_RE.findall(source):
        found.append(("ConfigurationProperties", prefix))

    for key in VALUE_RE.findall(source):
        found.append(("Value", key))

    for block in CONDITIONAL_RE.findall(source):
        pm = re.search(r'prefix\s*=\s*"([^"]+)"', block)
        nm = re.search(r'name\s*=\s*"([^"]+)"', block)

        if pm:
            key = pm.group(1)
            if nm:
                key += "." + nm.group(1)
            found.append(("ConditionalOnProperty", key))

    return found

def owner_of(key):
    normalized = key.rstrip(".") + "."

    for module, prefix in DOMAIN_PREFIX.items():
        if normalized.startswith(prefix):
            return module

    return None

def scan():
    rows = []
    violations = []

    for module in BUSINESS:
        java_root = BACKEND / module / "src/main/java"

        if not java_root.is_dir():
            continue

        for java_file in java_root.rglob("*.java"):
            source = java_file.read_text(
                encoding="utf-8",
                errors="ignore"
            )

            for kind, key in extract_property_keys(source):
                owner = owner_of(key)

                row = {
                    "module": module,
                    "kind": kind,
                    "key": key,
                    "owner": owner,
                    "file": java_file.relative_to(ROOT).as_posix(),
                }

                rows.append(row)

                if owner is not None and owner != module:
                    violations.append(row)

    return rows, violations

def main():
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            + EXPECTED_BRANCH
        )

    require(
        ROOT
        / "documentation/architecture/configuration/"
        / "FS-2.5.0_CONFIGURATION_INVENTORY.md"
    )

    require(
        ROOT
        / "documentation/architecture/configuration/"
        / "FS-2.5.1_BOOTSTRAP_GLOBAL_CONFIGURATION_NORMALIZATION.md"
    )

    rows, violations = scan()

    owned_rows = [
        row for row in rows
        if row["owner"] is not None
    ]

    by_module = defaultdict(int)
    for row in owned_rows:
        by_module[row["module"]] += 1

    lines = [BASE_DOC, "", "## Domain consumer matrix", ""]
    lines += [
        "| Consumer module | Property/prefix | Semantic owner | Kind | Source |",
        "|---|---|---|---|---|",
    ]

    if owned_rows:
        for row in sorted(
            owned_rows,
            key=lambda r: (
                r["module"],
                r["key"],
                r["file"],
            ),
        ):
            lines.append(
                f"| {row['module'].title()} | `{row['key']}` | "
                f"{row['owner'].title()} | {row['kind']} | "
                f"`{row['file']}` |"
            )
    else:
        lines.append("| _none detected_ | | | | |")

    lines += ["", "## Ownership decisions", ""]

    for module in BUSINESS:
        lines.append(
            f"- **{module.title()}** owns "
            f"`{DOMAIN_PREFIX[module]}*` — detected Java consumers: "
            f"**{by_module[module]}**."
        )

    lines += ["", "## Cross-domain configuration consumption", ""]

    if violations:
        for row in violations:
            lines.append(
                f"- ⚠ **{row['module'].title()}** consumes "
                f"`{row['key']}` owned by "
                f"**{row['owner'].title()}** in `{row['file']}`."
            )
    else:
        lines.append(
            "No direct `sixpay.<other-domain>.*` configuration consumption "
            "was detected in production Java."
        )

    lines += [
        "",
        "## Result",
        "",
        f"- Domain property consumers detected: **{len(owned_rows)}**",
        f"- Cross-domain ownership violations: **{len(violations)}**",
        "",
    ]

    if violations:
        lines.append(
            "**FS-2.5.2 requires review before closure.**"
        )
    else:
        lines.append(
            "**FS-2.5.2 ownership model is consistent with the current code.**"
        )

    DOC.parent.mkdir(parents=True, exist_ok=True)
    TEST.parent.mkdir(parents=True, exist_ok=True)

    DOC.write_text(
        "\n".join(lines) + "\n",
        encoding="utf-8"
    )

    if TEST.exists():
        fail(
            "Architecture gate already exists: "
            + str(TEST.relative_to(ROOT))
        )

    TEST.write_text(
        JAVA,
        encoding="utf-8"
    )

    print("FS-2.5.2 domain configuration ownership installed.")
    print("Created:")
    print(" -", DOC.relative_to(ROOT))
    print(" -", TEST.relative_to(ROOT))
    print()
    print("Result:")
    print(" - domain property consumers:", len(owned_rows))
    print(" - cross-domain ownership violations:", len(violations))
    print()
    print("Run from backend:")
    print(
        "  mvn -pl bootstrap -am "
        "-Dtest=DomainConfigurationOwnershipArchitectureTest test"
    )
    print()
    print("Then:")
    print("  mvn verify")

    if violations:
        sys.exit(2)

if __name__ == "__main__":
    main()
