from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

DOC = (
    ROOT
    / "documentation/architecture/persistence/"
    / "FS-2.3.8_DATABASE_BASELINE_GOVERNANCE_GATE.md"
)

TEST = (
    ROOT
    / "backend/bootstrap/src/test/java/"
    / "com/sixpay/bootstrap/architecture/"
    / "DatabaseBaselineGovernanceArchitectureTest.java"
)

CROSS_DOMAIN_TEST = (
    ROOT
    / "backend/bootstrap/src/test/java/"
    / "com/sixpay/bootstrap/architecture/"
    / "CrossDomainForeignKeyArchitectureTest.java"
)

BOOTSTRAP_GATE = (
    ROOT
    / "backend/bootstrap/src/test/java/"
    / "com/sixpay/bootstrap/architecture/"
    / "BootstrapDatabaseMigrationOwnershipArchitectureTest.java"
)

REPORT = "# FS-2.3.8 — Database Baseline Governance Gate\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Gate:** `FS-2.3 — Database baseline consolidation`  \n**Status:** Non-regression gate  \n**Golden module:** Partner\n\n## Purpose\n\nFS-2.3.8 converts the FS-2.3 persistence decisions into permanent architecture\nrules.\n\nThe gate must fail on:\n\n```text\nbusiness *.sql in bootstrap resources\nmigration outside owner range\nduplicate Flyway version\nunapproved cross-domain FK\npersistence entity without migration ownership\nhistorical pre-baseline migration restored\n```\n\n## Canonical ranges\n\n| Owner | Range |\n|---|---:|\n| Partner | 100–199 |\n| Customer | 200–299 |\n| Payment | 300–399 |\n| Accounting | 400–499 |\n| Reporting | 500–599 |\n| Notification | 600–699 |\n| Security | 700–799 |\n| Administration | 800–899 |\n| Platform | 900–999 |\n\nPlatform is exceptional and has no migration by default.\n\n## Gate composition\n\nFS-2.3.8 is intentionally composed from two complementary architecture tests.\n\n### DatabaseBaselineGovernanceArchitectureTest\n\nChecks:\n\n1. Bootstrap contains no Flyway-shaped SQL.\n2. Every migration lives in the range reserved to its owning module.\n3. Flyway versions are globally unique across module resources.\n4. No historical `V2026...` SQL exists anywhere under runtime resources.\n5. Every backend module containing a production `@Entity` has explicit\n   migration ownership and at least one canonical Flyway migration.\n6. A module with persistence entities but no reserved range fails immediately.\n\n### CrossDomainForeignKeyArchitectureTest\n\nExisting FS-2.3.5 gate checks:\n\n```text\nREFERENCES target\n```\n\nand requires the target table to be created by the same canonical domain\nbaseline.\n\nCurrent explicit cross-domain FK allow-list:\n\n```text\nEMPTY\n```\n\nTherefore every physical cross-domain FK is rejected.\n\nIf a future exception is ever accepted, it must be introduced through an\nexplicit architecture decision and an explicit allow-list change; it must never\nbecome legal merely because two modules share the same PostgreSQL database.\n\n## Historical migration policy\n\nThe pre-baseline migration generation is identified by the former timestamp\nversions:\n\n```text\nV2026...\n```\n\nNo such SQL file may be restored under any production runtime resource path,\nincluding non-standard paths such as:\n\n```text\ndb/security/migration\n```\n\nCanonical runtime migration ownership is exclusively the V100–V899 domain\nranges plus explicitly justified V900–V999 platform migrations.\n\n## Persistence ownership rule\n\nThe gate scans each backend module's:\n\n```text\nsrc/main/java\n```\n\nfor production JPA entities.\n\nIf `@Entity` is present, that module must:\n\n- be present in the controlled ownership/range map;\n- own `src/main/resources/db/migration`;\n- contain at least one Flyway migration in its own range.\n\nThis prevents a future module from adding JPA persistence while silently\nplacing its DDL in Bootstrap or another module.\n\n## Duplicate version rule\n\nFlyway versions are global on the assembled Bootstrap classpath.\n\nTherefore:\n\n```text\nPartner/V101\nPayment/V101\n```\n\nis illegal even though the files live in different JARs.\n\nThe gate parses every canonical module migration and rejects any duplicate\nnumeric Flyway version.\n\n## Exit criteria\n\nFS-2.3.8 is DONE when:\n\n- the consolidated governance test is installed;\n- the existing cross-domain FK gate remains green;\n- Bootstrap migration ownership gate remains green;\n- `mvn -pl bootstrap -am test` is green;\n- the full reactor `mvn verify` remains green.\n\n## Decision\n\n```text\nFS-2.3 persistence policy\n        ↓\narchitecture tests\n        ↓\nautomatic regression prevention\n```\n"
JAVA = 'package com.sixpay.bootstrap.architecture;\n\nimport org.junit.jupiter.api.Test;\n\nimport java.io.IOException;\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.util.ArrayList;\nimport java.util.HashMap;\nimport java.util.LinkedHashMap;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.regex.Matcher;\nimport java.util.regex.Pattern;\n\nimport static org.junit.jupiter.api.Assertions.assertTrue;\n\nclass DatabaseBaselineGovernanceArchitectureTest {\n\n    private static final Path BACKEND_ROOT =\n            Path.of("..");\n\n    private static final Pattern FLYWAY_FILE =\n            Pattern.compile(\n                    "^V([0-9]+)(?:[._][0-9]+)*__.+\\\\.sql$",\n                    Pattern.CASE_INSENSITIVE\n            );\n\n    private static final Map<String, VersionRange>\n            OWNERSHIP_RANGES =\n            new LinkedHashMap<>();\n\n    static {\n        OWNERSHIP_RANGES.put(\n                "partner",\n                new VersionRange(100, 199)\n        );\n        OWNERSHIP_RANGES.put(\n                "customer",\n                new VersionRange(200, 299)\n        );\n        OWNERSHIP_RANGES.put(\n                "payment",\n                new VersionRange(300, 399)\n        );\n        OWNERSHIP_RANGES.put(\n                "accounting",\n                new VersionRange(400, 499)\n        );\n        OWNERSHIP_RANGES.put(\n                "reporting",\n                new VersionRange(500, 599)\n        );\n        OWNERSHIP_RANGES.put(\n                "notification",\n                new VersionRange(600, 699)\n        );\n        OWNERSHIP_RANGES.put(\n                "security",\n                new VersionRange(700, 799)\n        );\n        OWNERSHIP_RANGES.put(\n                "administration",\n                new VersionRange(800, 899)\n        );\n    }\n\n    @Test\n    void bootstrapOwnsNoRuntimeFlywaySql()\n            throws Exception {\n\n        Path resources =\n                BACKEND_ROOT.resolve(\n                        "bootstrap/src/main/resources"\n                );\n\n        List<Path> violations =\n                flywayShapedSqlBelow(resources);\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Bootstrap is runtime assembler only "\n                        + "and must own no Flyway SQL: "\n                        + violations\n        );\n    }\n\n    @Test\n    void everyMigrationUsesItsOwnerReservedRange()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (var entry :\n                OWNERSHIP_RANGES.entrySet()) {\n\n            String owner = entry.getKey();\n            VersionRange range = entry.getValue();\n\n            Path migrationDirectory =\n                    migrationDirectory(owner);\n\n            assertTrue(\n                    Files.isDirectory(\n                            migrationDirectory\n                    ),\n                    () -> owner\n                            + " migration directory missing: "\n                            + migrationDirectory\n            );\n\n            for (Path migration :\n                    migrationFiles(\n                            migrationDirectory\n                    )) {\n\n                int version =\n                        primaryVersion(\n                                migration\n                        );\n\n                if (!range.contains(version)) {\n                    violations.add(\n                            owner\n                                    + "/"\n                                    + migration\n                                    .getFileName()\n                                    + " uses V"\n                                    + version\n                                    + " outside "\n                                    + range\n                    );\n                }\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Flyway owner-range violations: "\n                        + violations\n        );\n    }\n\n    @Test\n    void flywayVersionsAreGloballyUnique()\n            throws Exception {\n\n        Map<Integer, Path> firstByVersion =\n                new HashMap<>();\n\n        List<String> duplicates =\n                new ArrayList<>();\n\n        for (String owner :\n                OWNERSHIP_RANGES.keySet()) {\n\n            for (Path migration :\n                    migrationFiles(\n                            migrationDirectory(\n                                    owner\n                            )\n                    )) {\n\n                int version =\n                        primaryVersion(\n                                migration\n                        );\n\n                Path first =\n                        firstByVersion.putIfAbsent(\n                                version,\n                                migration\n                        );\n\n                if (first != null) {\n                    duplicates.add(\n                            "V"\n                                    + version\n                                    + " -> "\n                                    + first\n                                    + " AND "\n                                    + migration\n                    );\n                }\n            }\n        }\n\n        assertTrue(\n                duplicates.isEmpty(),\n                () -> "Duplicate global Flyway versions: "\n                        + duplicates\n        );\n    }\n\n    @Test\n    void historicalPreBaselineMigrationsCannotReturn()\n            throws Exception {\n\n        List<Path> violations =\n                new ArrayList<>();\n\n        try (var modules =\n                     Files.list(BACKEND_ROOT)) {\n\n            for (Path module :\n                    modules\n                            .filter(Files::isDirectory)\n                            .toList()) {\n\n                Path resources =\n                        module.resolve(\n                                "src/main/resources"\n                        );\n\n                if (!Files.isDirectory(resources)) {\n                    continue;\n                }\n\n                try (var files =\n                             Files.walk(resources)) {\n\n                    files.filter(\n                                    Files::isRegularFile\n                            )\n                            .filter(path ->\n                                    path.getFileName()\n                                            .toString()\n                                            .matches(\n                                                    "(?i)^V2026.*\\\\.sql$"\n                                            )\n                            )\n                            .forEach(\n                                    violations::add\n                            );\n                }\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Historical pre-baseline "\n                        + "V2026 migrations restored: "\n                        + violations\n        );\n    }\n\n    @Test\n    void everyProductionJpaModuleOwnsMigrations()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        try (var modules =\n                     Files.list(BACKEND_ROOT)) {\n\n            for (Path module :\n                    modules\n                            .filter(Files::isDirectory)\n                            .filter(path ->\n                                    Files.isRegularFile(\n                                            path.resolve(\n                                                    "pom.xml"\n                                            )\n                                    )\n                            )\n                            .toList()) {\n\n                String moduleName =\n                        module.getFileName()\n                                .toString();\n\n                Path javaRoot =\n                        module.resolve(\n                                "src/main/java"\n                        );\n\n                if (!containsJpaEntity(javaRoot)) {\n                    continue;\n                }\n\n                VersionRange range =\n                        OWNERSHIP_RANGES.get(\n                                moduleName\n                        );\n\n                if (range == null) {\n                    violations.add(\n                            moduleName\n                                    + " contains @Entity "\n                                    + "but has no reserved "\n                                    + "migration ownership range"\n                    );\n                    continue;\n                }\n\n                Path migrations =\n                        module.resolve(\n                                "src/main/resources/"\n                                        + "db/migration"\n                        );\n\n                if (!Files.isDirectory(\n                        migrations\n                )) {\n                    violations.add(\n                            moduleName\n                                    + " contains @Entity "\n                                    + "but owns no "\n                                    + "db/migration directory"\n                    );\n                    continue;\n                }\n\n                List<Path> files =\n                        migrationFiles(\n                                migrations\n                        );\n\n                if (files.isEmpty()) {\n                    violations.add(\n                            moduleName\n                                    + " contains @Entity "\n                                    + "but owns no "\n                                    + "Flyway migration"\n                    );\n                }\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Persistence ownership violations: "\n                        + violations\n        );\n    }\n\n    @Test\n    void controlledOwnersHaveAtLeastOneMigration()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (String owner :\n                OWNERSHIP_RANGES.keySet()) {\n\n            List<Path> migrations =\n                    migrationFiles(\n                            migrationDirectory(\n                                    owner\n                            )\n                    );\n\n            if (migrations.isEmpty()) {\n                violations.add(\n                        owner\n                                + " owns a reserved "\n                                + "range but has no migration"\n                );\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Missing module migration ownership: "\n                        + violations\n        );\n    }\n\n    private Path migrationDirectory(\n            String owner\n    ) {\n        return BACKEND_ROOT.resolve(\n                owner\n                        + "/src/main/resources/"\n                        + "db/migration"\n        );\n    }\n\n    private List<Path> migrationFiles(\n            Path directory\n    ) throws IOException {\n\n        if (!Files.isDirectory(directory)) {\n            return List.of();\n        }\n\n        try (var files =\n                     Files.list(directory)) {\n\n            return files\n                    .filter(Files::isRegularFile)\n                    .filter(path ->\n                            FLYWAY_FILE.matcher(\n                                    path.getFileName()\n                                            .toString()\n                            ).matches()\n                    )\n                    .sorted()\n                    .toList();\n        }\n    }\n\n    private int primaryVersion(\n            Path migration\n    ) {\n\n        Matcher matcher =\n                FLYWAY_FILE.matcher(\n                        migration.getFileName()\n                                .toString()\n                );\n\n        assertTrue(\n                matcher.matches(),\n                () -> "Invalid Flyway filename: "\n                        + migration\n        );\n\n        return Integer.parseInt(\n                matcher.group(1)\n        );\n    }\n\n    private List<Path> flywayShapedSqlBelow(\n            Path root\n    ) throws IOException {\n\n        if (!Files.isDirectory(root)) {\n            return List.of();\n        }\n\n        try (var files =\n                     Files.walk(root)) {\n\n            return files\n                    .filter(Files::isRegularFile)\n                    .filter(path ->\n                            FLYWAY_FILE.matcher(\n                                    path.getFileName()\n                                            .toString()\n                            ).matches()\n                    )\n                    .toList();\n        }\n    }\n\n    private boolean containsJpaEntity(\n            Path javaRoot\n    ) throws IOException {\n\n        if (!Files.isDirectory(javaRoot)) {\n            return false;\n        }\n\n        try (var files =\n                     Files.walk(javaRoot)) {\n\n            for (Path javaFile :\n                    files\n                            .filter(\n                                    Files::isRegularFile\n                            )\n                            .filter(path ->\n                                    path.toString()\n                                            .endsWith(\n                                                    ".java"\n                                            )\n                            )\n                            .toList()) {\n\n                String source =\n                        Files.readString(\n                                javaFile\n                        );\n\n                if (source.contains("@Entity")\n                        || source.contains(\n                        "@jakarta.persistence.Entity"\n                )) {\n                    return true;\n                }\n            }\n        }\n\n        return false;\n    }\n\n    private record VersionRange(\n            int start,\n            int end\n    ) {\n\n        boolean contains(int version) {\n            return version >= start\n                    && version <= end;\n        }\n\n        @Override\n        public String toString() {\n            return "V"\n                    + start\n                    + "–"\n                    + end;\n        }\n    }\n}\n'

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

    require(CROSS_DOMAIN_TEST)
    require(BOOTSTRAP_GATE)

    expected = {
        "partner": (100, 199),
        "customer": (200, 299),
        "payment": (300, 399),
        "accounting": (400, 499),
        "reporting": (500, 599),
        "notification": (600, 699),
        "security": (700, 799),
        "administration": (800, 899),
    }

    import re

    flyway = re.compile(
        r"^V([0-9]+)(?:[._][0-9]+)*__.+\.sql$",
        re.IGNORECASE
    )

    versions = {}
    preflight_errors = []

    for owner, (start, end) in expected.items():
        directory = (
            ROOT
            / f"backend/{owner}/src/main/resources/db/migration"
        )

        if not directory.is_dir():
            preflight_errors.append(
                f"{owner}: migration directory missing"
            )
            continue

        migrations = [
            path
            for path in directory.iterdir()
            if path.is_file()
            and flyway.match(path.name)
        ]

        if not migrations:
            preflight_errors.append(
                f"{owner}: no canonical migration"
            )

        for path in migrations:
            match = flyway.match(path.name)
            version = int(match.group(1))

            if not start <= version <= end:
                preflight_errors.append(
                    f"{owner}/{path.name} outside "
                    f"V{start}-{end}"
                )

            previous = versions.get(version)

            if previous is not None:
                preflight_errors.append(
                    f"duplicate V{version}: "
                    f"{previous} and {path}"
                )
            else:
                versions[version] = path

    bootstrap_resources = (
        ROOT
        / "backend/bootstrap/src/main/resources"
    )

    if bootstrap_resources.exists():
        for path in bootstrap_resources.rglob("*.sql"):
            if flyway.match(path.name):
                preflight_errors.append(
                    "Bootstrap owns Flyway SQL: "
                    + str(path.relative_to(ROOT))
                )

    for module in (
        ROOT / "backend"
    ).iterdir():
        resources = module / "src/main/resources"

        if not resources.is_dir():
            continue

        for path in resources.rglob("V2026*.sql"):
            preflight_errors.append(
                "historical migration restored: "
                + str(path.relative_to(ROOT))
            )

    if preflight_errors:
        fail(
            "FS-2.3.8 preflight failed:\n - "
            + "\n - ".join(preflight_errors)
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
            "Governance test already exists: "
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
        "FS-2.3.8 database baseline governance gate installed."
    )
    print()
    print(
        "Created: "
        + str(DOC.relative_to(ROOT))
    )
    print(
        "Created: "
        + str(TEST.relative_to(ROOT))
    )
    print()
    print("Covered:")
    print(" - Bootstrap business/Flyway SQL forbidden")
    print(" - module reserved ranges")
    print(" - global duplicate Flyway versions")
    print(" - historical V2026 migrations forbidden")
    print(" - @Entity module requires migration ownership")
    print(" - controlled owner requires migrations")
    print(" - cross-domain FK delegated to existing FS-2.3.5 gate")
    print()
    print("Run:")
    print("  cd backend")
    print(
        "  mvn -pl bootstrap -am "
        "-Dtest=DatabaseBaselineGovernanceArchitectureTest,"
        "CrossDomainForeignKeyArchitectureTest,"
        "BootstrapDatabaseMigrationOwnershipArchitectureTest test"
    )
    print()
    print("Then:")
    print("  mvn verify")

if __name__ == "__main__":
    main()
