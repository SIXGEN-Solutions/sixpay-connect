from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

TARGET = (
    ROOT
    / "backend/bootstrap/src/test/java/com/sixpay/bootstrap/architecture/"
    / "BootstrapDatabaseMigrationOwnershipArchitectureTest.java"
)

JAVA = r'''package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapDatabaseMigrationOwnershipArchitectureTest {

    private static final Path BOOTSTRAP_RESOURCES =
            Path.of("src/main/resources");

    private static final Path BOOTSTRAP_MIGRATION_DIRECTORY =
            BOOTSTRAP_RESOURCES.resolve("db/migration");

    private static final List<Path> CANONICAL_DOMAIN_BASELINES =
            List.of(
                    Path.of(
                            "../partner/src/main/resources/db/migration/"
                                    + "V100__partner_baseline.sql"
                    ),
                    Path.of(
                            "../customer/src/main/resources/db/migration/"
                                    + "V200__customer_baseline.sql"
                    ),
                    Path.of(
                            "../payment/src/main/resources/db/migration/"
                                    + "V300__payment_baseline.sql"
                    ),
                    Path.of(
                            "../accounting/src/main/resources/db/migration/"
                                    + "V400__accounting_baseline.sql"
                    ),
                    Path.of(
                            "../reporting/src/main/resources/db/migration/"
                                    + "V500__reporting_baseline.sql"
                    ),
                    Path.of(
                            "../notification/src/main/resources/db/migration/"
                                    + "V600__notification_baseline.sql"
                    ),
                    Path.of(
                            "../security/src/main/resources/db/migration/"
                                    + "V700__security_baseline.sql"
                    ),
                    Path.of(
                            "../administration/src/main/resources/db/migration/"
                                    + "V800__administration_baseline.sql"
                    )
            );

    @Test
    void bootstrapOwnsNoFlywayMigration() throws Exception {

        if (!Files.exists(BOOTSTRAP_MIGRATION_DIRECTORY)) {
            return;
        }

        try (var paths = Files.walk(BOOTSTRAP_MIGRATION_DIRECTORY)) {
            var sqlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .toLowerCase()
                                    .endsWith(".sql")
                    )
                    .toList();

            assertTrue(
                    sqlFiles.isEmpty(),
                    () -> "Bootstrap must not own Flyway SQL migrations: "
                            + sqlFiles
            );
        }
    }

    @Test
    void bootstrapResourcesContainNoHiddenFlywayMigration()
            throws Exception {

        assertTrue(
                Files.isDirectory(BOOTSTRAP_RESOURCES),
                () -> "Bootstrap resources directory is missing: "
                        + BOOTSTRAP_RESOURCES
        );

        try (var paths = Files.walk(BOOTSTRAP_RESOURCES)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name =
                                path.getFileName()
                                        .toString()
                                        .toLowerCase();

                        return name.endsWith(".sql")
                                && name.startsWith("v")
                                && name.contains("__");
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Bootstrap contains Flyway-shaped SQL files: "
                            + violations
            );
        }
    }

    @Test
    void everyBusinessDomainOwnsItsCanonicalBaseline() {

        var missing = CANONICAL_DOMAIN_BASELINES
                .stream()
                .filter(path -> !Files.isRegularFile(path))
                .toList();

        assertTrue(
                missing.isEmpty(),
                () -> "Canonical domain baselines missing: "
                        + missing
        );
    }
}
'''


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def main() -> None:
    if not ENGINEERING.is_file():
        fail("Run this script from the sixpay-connect repository root.")

    engineering = ENGINEERING.read_text(encoding="utf-8")

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            + EXPECTED_BRANCH
        )

    bootstrap_migrations = (
        ROOT
        / "backend/bootstrap/src/main/resources/db/migration"
    )

    if bootstrap_migrations.exists():
        sql_files = [
            path
            for path in bootstrap_migrations.rglob("*.sql")
            if path.is_file()
        ]

        if sql_files:
            fail(
                "FS-2.3.4 cannot be closed because Bootstrap still "
                "contains SQL migrations:\n - "
                + "\n - ".join(
                    str(path.relative_to(ROOT))
                    for path in sql_files
                )
            )

    expected_baselines = [
        ROOT / "backend/partner/src/main/resources/db/migration/V100__partner_baseline.sql",
        ROOT / "backend/customer/src/main/resources/db/migration/V200__customer_baseline.sql",
        ROOT / "backend/payment/src/main/resources/db/migration/V300__payment_baseline.sql",
        ROOT / "backend/accounting/src/main/resources/db/migration/V400__accounting_baseline.sql",
        ROOT / "backend/reporting/src/main/resources/db/migration/V500__reporting_baseline.sql",
        ROOT / "backend/notification/src/main/resources/db/migration/V600__notification_baseline.sql",
        ROOT / "backend/security/src/main/resources/db/migration/V700__security_baseline.sql",
        ROOT / "backend/administration/src/main/resources/db/migration/V800__administration_baseline.sql",
    ]

    missing = [
        str(path.relative_to(ROOT))
        for path in expected_baselines
        if not path.is_file()
    ]

    if missing:
        fail(
            "Canonical domain baselines are incomplete:\n - "
            + "\n - ".join(missing)
        )

    TARGET.parent.mkdir(parents=True, exist_ok=True)

    if TARGET.exists():
        existing = TARGET.read_text(encoding="utf-8")
        if existing == JAVA:
            print("FS-2.3.4 gate already installed.")
            return

        fail(
            f"Target already exists with different content: "
            f"{TARGET.relative_to(ROOT)}"
        )

    TARGET.write_text(JAVA, encoding="utf-8")

    print("FS-2.3.4 Bootstrap evacuation gate installed.")
    print()
    print(f"Created: {TARGET.relative_to(ROOT)}")
    print()
    print("Policy enforced:")
    print(" - bootstrap/src/main/resources/db/migration = absent or empty")
    print(" - no Flyway-shaped SQL anywhere under bootstrap resources")
    print(" - V100..V800 canonical baselines must exist in owning modules")
    print()
    print("Run:")
    print("  cd backend")
    print("  mvn -pl bootstrap -am test")


if __name__ == "__main__":
    main()
