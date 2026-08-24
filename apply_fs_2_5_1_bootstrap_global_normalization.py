from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

INVENTORY = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.0_CONFIGURATION_INVENTORY.md"
)

DOC = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.1_BOOTSTRAP_GLOBAL_CONFIGURATION_NORMALIZATION.md"
)

TEST = (
    ROOT
    / "backend/bootstrap/src/test/java/"
    / "com/sixpay/bootstrap/architecture/"
    / "BootstrapGlobalConfigurationArchitectureTest.java"
)

BASE_APPLICATION = (
    ROOT
    / "backend/bootstrap/src/main/resources/application.yml"
)

REPORT = '# FS-2.5.1 — Bootstrap / Global Configuration Normalization\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Phase:** `FS-2.5 — Configuration consolidation`  \n**Golden module:** Partner\n\n## Purpose\n\nFS-2.5.1 defines the canonical configuration surface owned by the Bootstrap\nruntime without changing functional configuration semantics.\n\nThe guiding distinction is:\n\n```text\nBootstrap\n    = runtime configuration owner\n\nBusiness module\n    = business configuration semantic owner\n```\n\n## Canonical Bootstrap-owned namespaces\n\nBootstrap owns the runtime assembly of:\n\n```text\nserver.*\nspring.application.*\nspring.datasource.*\nspring.jpa.*\nspring.flyway.*\nspringdoc.*\nmanagement.*\nlogging.*\nspring.kafka.*\nspring.mail.*\n```\n\nIt also owns shared runtime assembly namespaces:\n\n```text\nsixpay.messaging.*\n```\n\nAuthentication runtime/profile assembly remains Bootstrap-owned, while Security\npolicy values remain Security-owned.\n\n## Base `application.yml`\n\nThe base configuration is the global runtime baseline.\n\nIt currently owns:\n\n```text\nserver.port\nspring.application.name\nspring.jpa.*\nspring.flyway.*\nspringdoc.*\nmanagement.*\nsixpay.messaging.*\n```\n\nThese are canonical Bootstrap/global concerns.\n\nThe following domain-owned values currently remain in the base file only as\n**explicitly tracked transition debt**:\n\n```text\nsixpay.customer.verification.banking.enabled\nsixpay.security.local.password.*\n```\n\nThey are not reclassified as Bootstrap-owned.\n\nThey are preserved temporarily to avoid changing runtime defaults before\nFS-2.5.2 establishes domain-owned configuration loading.\n\n## Transitional-debt rule\n\nFS-2.5.1 freezes the exact current debt.\n\nNo new `sixpay.<domain>.*` key may be added to the base `application.yml`.\n\nAllowed transitional prefixes are only:\n\n```text\nsixpay.customer.verification.banking.*\nsixpay.security.local.password.*\n```\n\nFS-2.5.2 must decide their final domain-owned loading mechanism and then remove\nthis temporary allow-list.\n\n## Domain modules must not own global runtime configuration\n\nBusiness modules must not introduce their own:\n\n```text\nserver.*\nspring.datasource.*\nspring.jpa.*\nspring.flyway.*\nspringdoc.*\nmanagement.*\n```\n\nThe runtime application owns these globally.\n\nA future extracted microservice may own those namespaces in its own deployable\nruntime, but while the module is part of the SIXPAY modular monolith it must not\nsilently introduce a second runtime configuration root.\n\n## Stable runtime invariants\n\nFS-2.5.1 protects the existing effective baseline:\n\n```text\nspring.jpa.hibernate.ddl-auto = validate\nspring.flyway.schemas = sixpay\nspring.flyway.default-schema = sixpay\nspring.flyway.locations = classpath:db/migration\nspring.flyway.validate-on-migrate = true\nspring.flyway.clean-disabled = true\nspringdoc disabled by default\nmanagement base exposure = health,info\n```\n\nProfile-specific overrides remain valid and are reviewed later in FS-2.5.3.\n\n## Non-regression policy\n\nFS-2.5.1 does not:\n\n- rename property keys;\n- rename environment variables;\n- change defaults;\n- delete profiles;\n- move domain values between files;\n- alter authentication mode;\n- alter feature-flag behavior.\n\nThe workflow is:\n\n```text\nclassify\n  -> freeze global ownership\n  -> prevent new debt\n  -> relocate domain semantics in FS-2.5.2\n  -> validate profiles in FS-2.5.3\n```\n\n## Exit criteria\n\nFS-2.5.1 is DONE when:\n\n- the Bootstrap global namespace whitelist is documented;\n- base `application.yml` global invariants are protected;\n- existing domain debt in the base file is frozen, not expanded;\n- business modules cannot introduce global runtime namespaces;\n- no runtime defaults are changed.\n'
JAVA = 'package com.sixpay.bootstrap.architecture;\n\nimport org.junit.jupiter.api.Test;\n\nimport java.io.IOException;\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.util.ArrayList;\nimport java.util.List;\nimport java.util.Set;\n\nimport static org.junit.jupiter.api.Assertions.assertTrue;\n\nclass BootstrapGlobalConfigurationArchitectureTest {\n\n    private static final Path BACKEND_ROOT =\n            Path.of("..");\n\n    private static final Path BASE_APPLICATION =\n            Path.of("src/main/resources/application.yml");\n\n    private static final List<String> BUSINESS_MODULES =\n            List.of(\n                    "partner",\n                    "customer",\n                    "payment",\n                    "accounting",\n                    "reporting",\n                    "notification",\n                    "security",\n                    "administration"\n            );\n\n    private static final Set<String>\n            ALLOWED_BASE_SIXPAY_PREFIXES =\n            Set.of(\n                    "sixpay.messaging.",\n                    "sixpay.customer.verification.banking.",\n                    "sixpay.security.local.password."\n            );\n\n    private static final List<String>\n            FORBIDDEN_DOMAIN_GLOBAL_PREFIXES =\n            List.of(\n                    "server:",\n                    "spring.datasource:",\n                    "spring.jpa:",\n                    "spring.flyway:",\n                    "springdoc:",\n                    "management:"\n            );\n\n    @Test\n    void baseApplicationPreservesCanonicalGlobalRuntimeInvariants()\n            throws Exception {\n\n        String source =\n                Files.readString(BASE_APPLICATION);\n\n        assertContains(\n                source,\n                "name: sixpay-connect"\n        );\n\n        assertContains(\n                source,\n                "ddl-auto: validate"\n        );\n\n        assertContains(\n                source,\n                "schemas: sixpay"\n        );\n\n        assertContains(\n                source,\n                "default-schema: sixpay"\n        );\n\n        assertContains(\n                source,\n                "locations: classpath:db/migration"\n        );\n\n        assertContains(\n                source,\n                "validate-on-migrate: true"\n        );\n\n        assertContains(\n                source,\n                "clean-disabled: true"\n        );\n\n        assertContains(\n                source,\n                "api-docs:"\n        );\n\n        assertContains(\n                source,\n                "swagger-ui:"\n        );\n\n        assertContains(\n                source,\n                "include: health,info"\n        );\n    }\n\n    @Test\n    void baseApplicationCannotAccumulateNewDomainConfiguration()\n            throws Exception {\n\n        List<String> leafPaths =\n                yamlLeafPaths(\n                        BASE_APPLICATION\n                );\n\n        List<String> violations =\n                leafPaths.stream()\n                        .filter(path ->\n                                path.startsWith(\n                                        "sixpay."\n                                )\n                        )\n                        .filter(path ->\n                                ALLOWED_BASE_SIXPAY_PREFIXES\n                                        .stream()\n                                        .noneMatch(\n                                                path::startsWith\n                                        )\n                        )\n                        .toList();\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "New domain-owned configuration "\n                        + "was added to base application.yml: "\n                        + violations\n                        + ". FS-2.5.1 freezes the current "\n                        + "transition debt until FS-2.5.2."\n        );\n    }\n\n    @Test\n    void businessModulesCannotOwnMonolithGlobalRuntimeNamespaces()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (String module :\n                BUSINESS_MODULES) {\n\n            Path resources =\n                    BACKEND_ROOT.resolve(\n                            module\n                                    + "/src/main/resources"\n                    );\n\n            if (!Files.isDirectory(\n                    resources\n            )) {\n                continue;\n            }\n\n            try (var files =\n                         Files.walk(resources)) {\n\n                for (Path file :\n                        files.filter(\n                                        Files::isRegularFile\n                                )\n                                .filter(path -> {\n                                    String name =\n                                            path.getFileName()\n                                                    .toString();\n\n                                    return name.startsWith(\n                                            "application"\n                                    )\n                                            && (\n                                            name.endsWith(\n                                                    ".yml"\n                                            )\n                                                    || name.endsWith(\n                                                    ".yaml"\n                                            )\n                                    );\n                                })\n                                .toList()) {\n\n                    List<String> roots =\n                            yamlTopLevelAndSecondLevelPaths(\n                                    file\n                            );\n\n                    for (String prefix :\n                            forbiddenGlobalPaths()) {\n\n                        if (roots.contains(\n                                prefix\n                        )) {\n                            violations.add(\n                                    module\n                                            + ": "\n                                            + file\n                                            + " owns "\n                                            + prefix\n                            );\n                        }\n                    }\n                }\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Business modules must not own "\n                        + "modular-monolith global runtime "\n                        + "configuration: "\n                        + violations\n        );\n    }\n\n    private List<String> forbiddenGlobalPaths() {\n        return List.of(\n                "server",\n                "spring.datasource",\n                "spring.jpa",\n                "spring.flyway",\n                "springdoc",\n                "management"\n        );\n    }\n\n    private void assertContains(\n            String source,\n            String token\n    ) {\n\n        assertTrue(\n                source.contains(token),\n                () -> "Canonical Bootstrap global "\n                        + "configuration missing: "\n                        + token\n        );\n    }\n\n    private List<String> yamlLeafPaths(\n            Path file\n    ) throws IOException {\n\n        List<String> result =\n                new ArrayList<>();\n\n        List<YamlLevel> stack =\n                new ArrayList<>();\n\n        List<String> lines =\n                Files.readAllLines(file);\n\n        for (String line : lines) {\n\n            String trimmed =\n                    line.trim();\n\n            if (trimmed.isEmpty()\n                    || trimmed.startsWith("#")\n                    || trimmed.startsWith("- ")) {\n                continue;\n            }\n\n            int separator =\n                    trimmed.indexOf(\':\');\n\n            if (separator <= 0) {\n                continue;\n            }\n\n            String key =\n                    trimmed.substring(\n                            0,\n                            separator\n                    ).trim();\n\n            String remainder =\n                    trimmed.substring(\n                            separator + 1\n                    ).trim();\n\n            int indent =\n                    indentation(line);\n\n            while (!stack.isEmpty()\n                    && stack.get(\n                    stack.size() - 1\n            ).indent() >= indent) {\n                stack.remove(\n                        stack.size() - 1\n                );\n            }\n\n            stack.add(\n                    new YamlLevel(\n                            indent,\n                            key\n                    )\n            );\n\n            if (!remainder.isEmpty()) {\n                result.add(\n                        stack.stream()\n                                .map(\n                                        YamlLevel::key\n                                )\n                                .reduce(\n                                        (left, right) ->\n                                                left\n                                                        + "."\n                                                        + right\n                                )\n                                .orElse("")\n                );\n            }\n        }\n\n        return result;\n    }\n\n    private List<String>\n    yamlTopLevelAndSecondLevelPaths(\n            Path file\n    ) throws IOException {\n\n        List<String> result =\n                new ArrayList<>();\n\n        List<YamlLevel> stack =\n                new ArrayList<>();\n\n        for (String line :\n                Files.readAllLines(file)) {\n\n            String trimmed =\n                    line.trim();\n\n            if (trimmed.isEmpty()\n                    || trimmed.startsWith("#")\n                    || trimmed.startsWith("- ")) {\n                continue;\n            }\n\n            int separator =\n                    trimmed.indexOf(\':\');\n\n            if (separator <= 0) {\n                continue;\n            }\n\n            String key =\n                    trimmed.substring(\n                            0,\n                            separator\n                    ).trim();\n\n            int indent =\n                    indentation(line);\n\n            while (!stack.isEmpty()\n                    && stack.get(\n                    stack.size() - 1\n            ).indent() >= indent) {\n                stack.remove(\n                        stack.size() - 1\n                );\n            }\n\n            stack.add(\n                    new YamlLevel(\n                            indent,\n                            key\n                    )\n            );\n\n            if (stack.size() == 1) {\n                result.add(key);\n            } else if (stack.size() == 2) {\n                result.add(\n                        stack.get(0).key()\n                                + "."\n                                + stack.get(1).key()\n                );\n            }\n        }\n\n        return result;\n    }\n\n    private int indentation(\n            String line\n    ) {\n\n        int count = 0;\n\n        for (char value :\n                line.toCharArray()) {\n\n            if (value == \' \') {\n                count++;\n            } else if (value == \'\\t\') {\n                count += 4;\n            } else {\n                break;\n            }\n        }\n\n        return count;\n    }\n\n    private record YamlLevel(\n            int indent,\n            String key\n    ) {\n    }\n}\n'

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

    require(INVENTORY)

    base = require(BASE_APPLICATION)

    required_tokens = [
        "spring:",
        "jpa:",
        "ddl-auto: validate",
        "flyway:",
        "schemas: sixpay",
        "default-schema: sixpay",
        "locations: classpath:db/migration",
        "validate-on-migrate: true",
        "clean-disabled: true",
        "springdoc:",
        "management:",
        "sixpay:",
        "messaging:",
    ]

    for token in required_tokens:
        if token not in base:
            fail(
                "Canonical Bootstrap baseline token "
                f"missing: {token}"
            )

    # Preserve, do not silently remove, the two currently known
    # base-file domain defaults before FS-2.5.2.
    transitional_tokens = [
        "verification:",
        "banking:",
        "local:",
        "password:",
    ]

    for token in transitional_tokens:
        if token not in base:
            fail(
                "FS-2.5.1 expected transition debt "
                f"changed before ownership migration: {token}"
            )

    DOC.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    TEST.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    if DOC.exists():
        fail(
            "Document already exists: "
            + str(DOC.relative_to(ROOT))
        )

    if TEST.exists():
        fail(
            "Architecture gate already exists: "
            + str(TEST.relative_to(ROOT))
        )

    DOC.write_text(
        REPORT,
        encoding="utf-8"
    )

    TEST.write_text(
        JAVA,
        encoding="utf-8"
    )

    print(
        "FS-2.5.1 Bootstrap/global normalization installed."
    )
    print()
    print("Created:")
    print(" -", DOC.relative_to(ROOT))
    print(" -", TEST.relative_to(ROOT))
    print()
    print("Policy:")
    print(" - Bootstrap global namespaces documented")
    print(" - base application.yml runtime invariants frozen")
    print(" - current base-file domain debt frozen, not expanded")
    print(" - business modules cannot own monolith global runtime config")
    print(" - no property/default/profile behavior changed")
    print()
    print("Run from backend:")
    print(
        "  mvn -pl bootstrap -am "
        "-Dtest=BootstrapGlobalConfigurationArchitectureTest test"
    )
    print()
    print("Then:")
    print("  mvn verify")

if __name__ == "__main__":
    main()
