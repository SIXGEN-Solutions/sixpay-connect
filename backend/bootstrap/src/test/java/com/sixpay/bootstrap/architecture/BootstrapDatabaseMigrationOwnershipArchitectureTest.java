package com.sixpay.bootstrap.architecture;

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
