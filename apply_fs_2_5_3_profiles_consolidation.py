from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

RESOURCES = ROOT / "backend/bootstrap/src/main/resources"
INTEGRATION_PROFILE = RESOURCES / "application-integration.yml"

DOC = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.3_PROFILES_CONSOLIDATION.md"
)

TEST = (
    ROOT
    / "backend/bootstrap/src/test/java/com/sixpay/bootstrap/architecture/"
    / "RuntimeProfileConfigurationArchitectureTest.java"
)

REPORT = '# FS-2.5.3 — Runtime Profiles Consolidation\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Phase:** `FS-2.5 — Configuration consolidation`  \n**Golden module:** Partner\n\n## Purpose\n\nFS-2.5.3 normalizes the role of Bootstrap `application-*.yml` files without\nrenaming existing profiles or changing functional defaults unnecessarily.\n\nA Spring profile is a runtime composition, not a business capability owner.\n\n## Canonical profile categories\n\n- `BASE_RUNTIME`: `application.yml`\n- `ENVIRONMENT_RUNTIME`: executable environment profiles such as standalone/integration\n- `AUTHENTICATION_COMPOSITION`: local-auth / hybrid-auth\n- `TRANSPORT_COMPOSITION`: Kafka/runtime transport composition\n- `SANDBOX_COMPOSITION`: provider/capability sandbox fixtures\n- `CAPABILITY_COMPOSITION`: capability-specific composition profiles\n\n## Critical Flyway normalization\n\n`classpath:db/security/migration` is obsolete after FS-2.3.\n\nAll canonical migrations now come from:\n\n```text\nclasspath:db/migration\n```\n\nTherefore `application-integration.yml` must no longer include the old Security\nmigration path.\n\n## Profile safety rules\n\n- No profile may restore `db/security/migration`.\n- No profile may enable `baseline-on-migrate: true`.\n- No profile may reference historical `V2026...` migrations.\n- No profile may introduce destructive Hibernate `create` or `create-drop`.\n- Existing standalone `ddl-auto=update` developer default is preserved for now.\n- `local-auth` keeps local enabled / OIDC disabled.\n- `hybrid-auth` keeps local enabled / OIDC enabled.\n- Domain values in profiles remain semantically domain-owned per FS-2.5.2.\n\n## Non-regression policy\n\nFS-2.5.3 does not rename or delete profiles merely for cleanliness.\nThe only immediate cleanup is removal of the proven obsolete Flyway location.\n'
JAVA = 'package com.sixpay.bootstrap.architecture;\n\nimport org.junit.jupiter.api.Test;\n\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.util.ArrayList;\nimport java.util.List;\n\nimport static org.junit.jupiter.api.Assertions.assertTrue;\n\nclass RuntimeProfileConfigurationArchitectureTest {\n\n    private static final Path RESOURCES =\n            Path.of("src/main/resources");\n\n    @Test\n    void profilesNeverReferenceHistoricalFlywayLocations()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (Path profile : applicationProfiles()) {\n\n            String source = Files.readString(profile);\n\n            if (source.contains("db/security/migration")) {\n                violations.add(\n                        profile\n                                + " references obsolete "\n                                + "db/security/migration"\n                );\n            }\n\n            if (source.contains("baseline-on-migrate: true")) {\n                violations.add(\n                        profile\n                                + " enables forbidden "\n                                + "baseline-on-migrate"\n                );\n            }\n\n            if (source.matches("(?s).*V2026[^\\\\n]*\\\\.sql.*")) {\n                violations.add(\n                        profile\n                                + " references historical V2026 migration"\n                );\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Historical Flyway profile references: "\n                        + violations\n        );\n    }\n\n    @Test\n    void profileFlywayLocationsRemainCanonical()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (Path profile : applicationProfiles()) {\n\n            String source = Files.readString(profile);\n\n            if (!source.contains("flyway:")) {\n                continue;\n            }\n\n            if (source.contains("locations:")\n                    && !source.contains(\n                    "classpath:db/migration"\n            )) {\n                violations.add(\n                        profile\n                                + " defines Flyway locations "\n                                + "without canonical classpath:db/migration"\n                );\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Non-canonical Flyway profile locations: "\n                        + violations\n        );\n    }\n\n    @Test\n    void profilesCannotIntroduceDestructiveHibernateSchemaCreation()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (Path profile : applicationProfiles()) {\n\n            String source =\n                    Files.readString(profile)\n                            .toLowerCase();\n\n            if (source.contains("ddl-auto: create-drop")\n                    || source.contains("ddl-auto: create")) {\n                violations.add(profile.toString());\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Destructive Hibernate schema profile: "\n                        + violations\n        );\n    }\n\n    @Test\n    void localAuthenticationProfileKeepsLocalOnlySemantics()\n            throws Exception {\n\n        String source =\n                Files.readString(\n                        RESOURCES.resolve(\n                                "application-local-auth.yml"\n                        )\n                );\n\n        int local = source.indexOf("local:");\n        int oidc = source.indexOf("oidc:");\n\n        assertTrue(local >= 0);\n        assertTrue(oidc >= 0);\n\n        assertTrue(\n                source.substring(local, oidc)\n                        .contains("enabled: true"),\n                "local-auth must keep local enabled"\n        );\n\n        assertTrue(\n                source.substring(oidc)\n                        .contains("enabled: false"),\n                "local-auth must keep OIDC disabled"\n        );\n    }\n\n    @Test\n    void hybridAuthenticationProfileKeepsLocalAndOidcEnabled()\n            throws Exception {\n\n        String source =\n                Files.readString(\n                        RESOURCES.resolve(\n                                "application-hybrid-auth.yml"\n                        )\n                );\n\n        int local = source.indexOf("local:");\n        int oidc = source.indexOf("oidc:");\n\n        assertTrue(local >= 0);\n        assertTrue(oidc >= 0);\n\n        assertTrue(\n                source.substring(local, oidc)\n                        .contains("enabled: true"),\n                "hybrid-auth must keep local enabled"\n        );\n\n        assertTrue(\n                source.substring(oidc)\n                        .contains("enabled: true"),\n                "hybrid-auth must keep OIDC enabled"\n        );\n    }\n\n    @Test\n    void standaloneDeveloperProfileRemainsExplicitlyDevelopmentOriented()\n            throws Exception {\n\n        Path standalone =\n                RESOURCES.resolve(\n                        "application-standalone.yml"\n                );\n\n        assertTrue(Files.isRegularFile(standalone));\n\n        String source = Files.readString(standalone);\n\n        assertTrue(\n                source.contains(\n                        "SPRING_JPA_HIBERNATE_DDL_AUTO:update"\n                ),\n                "FS-2.5.3 preserves existing standalone developer default"\n        );\n    }\n\n    private List<Path> applicationProfiles()\n            throws Exception {\n\n        try (var files = Files.list(RESOURCES)) {\n            return files\n                    .filter(Files::isRegularFile)\n                    .filter(path -> {\n                        String name =\n                                path.getFileName().toString();\n\n                        return name.startsWith("application")\n                                && (\n                                name.endsWith(".yml")\n                                        || name.endsWith(".yaml")\n                        );\n                    })\n                    .sorted()\n                    .toList();\n        }\n    }\n}\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def classify_profile(name):
    if name == "application.yml":
        return "BASE_RUNTIME"

    lower = name.lower()

    if "local-auth" in lower or "hybrid-auth" in lower:
        return "AUTHENTICATION_COMPOSITION"

    if "standalone" in lower or name == "application-integration.yml":
        return "ENVIRONMENT_RUNTIME"

    if "kafka" in lower:
        return "TRANSPORT_COMPOSITION"

    if "sandbox" in lower:
        return "SANDBOX_COMPOSITION"

    return "CAPABILITY_COMPOSITION"

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
        / "FS-2.5.2_DOMAIN_CONFIGURATION_OWNERSHIP.md"
    )

    integration = require(INTEGRATION_PROFILE)

    if "classpath:db/security/migration" in integration:
        lines = integration.splitlines()

        lines = [
            line
            for line in lines
            if "classpath:db/security/migration" not in line
        ]

        updated = "\n".join(lines) + "\n"

        if "classpath:db/security/migration" in updated:
            fail(
                "Unable to remove obsolete Security Flyway location."
            )

        INTEGRATION_PROFILE.write_text(
            updated,
            encoding="utf-8"
        )

        print(
            "Removed obsolete db/security/migration "
            "from application-integration.yml."
        )

    profiles = sorted(
        path
        for path in RESOURCES.iterdir()
        if path.is_file()
        and path.name.startswith("application")
        and path.suffix in {".yml", ".yaml"}
    )

    historical = []

    for profile in profiles:
        source = profile.read_text(encoding="utf-8")

        if "db/security/migration" in source:
            historical.append(
                profile.name + " -> db/security/migration"
            )

        if "baseline-on-migrate: true" in source:
            historical.append(
                profile.name + " -> baseline-on-migrate=true"
            )

    if historical:
        fail(
            "Historical profile configuration remains:\n - "
            + "\n - ".join(historical)
        )

    classification = {}

    for profile in profiles:
        classification.setdefault(
            classify_profile(profile.name),
            []
        ).append(profile.name)

    rendered = REPORT + "\n\n## Current profile inventory\n\n"

    for category in sorted(classification):
        rendered += f"### {category}\n\n"
        for name in classification[category]:
            rendered += f"- `{name}`\n"
        rendered += "\n"

    DOC.parent.mkdir(parents=True, exist_ok=True)
    TEST.parent.mkdir(parents=True, exist_ok=True)

    if DOC.exists():
        fail(
            "Documentation already exists: "
            + str(DOC.relative_to(ROOT))
        )

    if TEST.exists():
        fail(
            "Architecture gate already exists: "
            + str(TEST.relative_to(ROOT))
        )

    DOC.write_text(
        rendered,
        encoding="utf-8"
    )

    TEST.write_text(
        JAVA,
        encoding="utf-8"
    )

    print()
    print("FS-2.5.3 profiles consolidation installed.")
    print("Created:")
    print(" -", DOC.relative_to(ROOT))
    print(" -", TEST.relative_to(ROOT))
    print()
    print("Profiles classified:", len(profiles))

    for category in sorted(classification):
        print(
            " - "
            + category
            + ": "
            + str(len(classification[category]))
        )

    print()
    print("Run from backend:")
    print(
        "  mvn -pl bootstrap -am "
        "-Dtest=RuntimeProfileConfigurationArchitectureTest test"
    )
    print()
    print("Then:")
    print("  mvn verify")

if __name__ == "__main__":
    main()
