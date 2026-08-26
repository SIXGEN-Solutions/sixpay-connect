package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBaselineGovernanceArchitectureTest {

    private static final Path BACKEND_ROOT =
            Path.of("..");

    private static final Pattern FLYWAY_FILE =
            Pattern.compile(
                    "^V([0-9]+)(?:[._][0-9]+)*__.+\\.sql$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Map<String, VersionRange>
            OWNERSHIP_RANGES =
            new LinkedHashMap<>();

    static {
        OWNERSHIP_RANGES.put(
                "partner",
                new VersionRange(100, 199)
        );
        OWNERSHIP_RANGES.put(
                "customer",
                new VersionRange(200, 299)
        );
        OWNERSHIP_RANGES.put(
                "payment",
                new VersionRange(300, 399)
        );
        OWNERSHIP_RANGES.put(
                "accounting",
                new VersionRange(400, 499)
        );
        OWNERSHIP_RANGES.put(
                "reporting",
                new VersionRange(500, 599)
        );
        OWNERSHIP_RANGES.put(
                "notification",
                new VersionRange(600, 699)
        );
        OWNERSHIP_RANGES.put(
                "security",
                new VersionRange(700, 799)
        );
        OWNERSHIP_RANGES.put(
                "administration",
                new VersionRange(800, 899)
        );
    }

    @Test
    void bootstrapOwnsNoRuntimeFlywaySql()
            throws Exception {

        Path resources =
                BACKEND_ROOT.resolve(
                        "bootstrap/src/main/resources"
                );

        List<Path> violations =
                flywayShapedSqlBelow(resources);

        assertTrue(
                violations.isEmpty(),
                () -> "Bootstrap is runtime assembler only "
                        + "and must own no Flyway SQL: "
                        + violations
        );
    }

    @Test
    void everyMigrationUsesItsOwnerReservedRange()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (var entry :
                OWNERSHIP_RANGES.entrySet()) {

            String owner = entry.getKey();
            VersionRange range = entry.getValue();

            Path migrationDirectory =
                    migrationDirectory(owner);

            assertTrue(
                    Files.isDirectory(
                            migrationDirectory
                    ),
                    () -> owner
                            + " migration directory missing: "
                            + migrationDirectory
            );

            for (Path migration :
                    migrationFiles(
                            migrationDirectory
                    )) {

                int version =
                        primaryVersion(
                                migration
                        );

                if (!range.contains(version)) {
                    violations.add(
                            owner
                                    + "/"
                                    + migration
                                    .getFileName()
                                    + " uses V"
                                    + version
                                    + " outside "
                                    + range
                    );
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Flyway owner-range violations: "
                        + violations
        );
    }

    @Test
    void flywayVersionsAreGloballyUnique()
            throws Exception {

        Map<Integer, Path> firstByVersion =
                new HashMap<>();

        List<String> duplicates =
                new ArrayList<>();

        for (String owner :
                OWNERSHIP_RANGES.keySet()) {

            for (Path migration :
                    migrationFiles(
                            migrationDirectory(
                                    owner
                            )
                    )) {

                int version =
                        primaryVersion(
                                migration
                        );

                Path first =
                        firstByVersion.putIfAbsent(
                                version,
                                migration
                        );

                if (first != null) {
                    duplicates.add(
                            "V"
                                    + version
                                    + " -> "
                                    + first
                                    + " AND "
                                    + migration
                    );
                }
            }
        }

        assertTrue(
                duplicates.isEmpty(),
                () -> "Duplicate global Flyway versions: "
                        + duplicates
        );
    }

    @Test
    void historicalPreBaselineMigrationsCannotReturn()
            throws Exception {

        List<Path> violations =
                new ArrayList<>();

        try (var modules =
                     Files.list(BACKEND_ROOT)) {

            for (Path module :
                    modules
                            .filter(Files::isDirectory)
                            .toList()) {

                Path resources =
                        module.resolve(
                                "src/main/resources"
                        );

                if (!Files.isDirectory(resources)) {
                    continue;
                }

                try (var files =
                             Files.walk(resources)) {

                    files.filter(
                                    Files::isRegularFile
                            )
                            .filter(path ->
                                    path.getFileName()
                                            .toString()
                                            .matches(
                                                    "(?i)^V2026.*\\.sql$"
                                            )
                            )
                            .forEach(
                                    violations::add
                            );
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Historical pre-baseline "
                        + "V2026 migrations restored: "
                        + violations
        );
    }

    @Test
    void everyProductionJpaModuleOwnsMigrations()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        try (var modules =
                     Files.list(BACKEND_ROOT)) {

            for (Path module :
                    modules
                            .filter(Files::isDirectory)
                            .filter(path ->
                                    Files.isRegularFile(
                                            path.resolve(
                                                    "pom.xml"
                                            )
                                    )
                            )
                            .toList()) {

                String moduleName =
                        module.getFileName()
                                .toString();

                Path javaRoot =
                        module.resolve(
                                "src/main/java"
                        );

                if (!containsJpaEntity(javaRoot)) {
                    continue;
                }

                VersionRange range =
                        OWNERSHIP_RANGES.get(
                                moduleName
                        );

                if (range == null) {
                    violations.add(
                            moduleName
                                    + " contains @Entity "
                                    + "but has no reserved "
                                    + "migration ownership range"
                    );
                    continue;
                }

                Path migrations =
                        module.resolve(
                                "src/main/resources/"
                                        + "db/migration"
                        );

                if (!Files.isDirectory(
                        migrations
                )) {
                    violations.add(
                            moduleName
                                    + " contains @Entity "
                                    + "but owns no "
                                    + "db/migration directory"
                    );
                    continue;
                }

                List<Path> files =
                        migrationFiles(
                                migrations
                        );

                if (files.isEmpty()) {
                    violations.add(
                            moduleName
                                    + " contains @Entity "
                                    + "but owns no "
                                    + "Flyway migration"
                    );
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Persistence ownership violations: "
                        + violations
        );
    }

    @Test
    void controlledOwnersHaveAtLeastOneMigration()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (String owner :
                OWNERSHIP_RANGES.keySet()) {

            List<Path> migrations =
                    migrationFiles(
                            migrationDirectory(
                                    owner
                            )
                    );

            if (migrations.isEmpty()) {
                violations.add(
                        owner
                                + " owns a reserved "
                                + "range but has no migration"
                );
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Missing module migration ownership: "
                        + violations
        );
    }

    private Path migrationDirectory(
            String owner
    ) {
        return BACKEND_ROOT.resolve(
                owner
                        + "/src/main/resources/"
                        + "db/migration"
        );
    }

    private List<Path> migrationFiles(
            Path directory
    ) throws IOException {

        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        try (var files =
                     Files.list(directory)) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            FLYWAY_FILE.matcher(
                                    path.getFileName()
                                            .toString()
                            ).matches()
                    )
                    .sorted()
                    .toList();
        }
    }

    private int primaryVersion(
            Path migration
    ) {

        Matcher matcher =
                FLYWAY_FILE.matcher(
                        migration.getFileName()
                                .toString()
                );

        assertTrue(
                matcher.matches(),
                () -> "Invalid Flyway filename: "
                        + migration
        );

        return Integer.parseInt(
                matcher.group(1)
        );
    }

    private List<Path> flywayShapedSqlBelow(
            Path root
    ) throws IOException {

        if (!Files.isDirectory(root)) {
            return List.of();
        }

        try (var files =
                     Files.walk(root)) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            FLYWAY_FILE.matcher(
                                    path.getFileName()
                                            .toString()
                            ).matches()
                    )
                    .toList();
        }
    }

    private boolean containsJpaEntity(
            Path javaRoot
    ) throws IOException {

        if (!Files.isDirectory(javaRoot)) {
            return false;
        }

        try (var files =
                     Files.walk(javaRoot)) {

            for (Path javaFile :
                    files
                            .filter(
                                    Files::isRegularFile
                            )
                            .filter(path ->
                                    path.toString()
                                            .endsWith(
                                                    ".java"
                                            )
                            )
                            .toList()) {

                String source =
                        Files.readString(
                                javaFile
                        );

                if (source.contains("@Entity")
                        || source.contains(
                        "@jakarta.persistence.Entity"
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private record VersionRange(
            int start,
            int end
    ) {

        boolean contains(int version) {
            return version >= start
                    && version <= end;
        }

        @Override
        public String toString() {
            return "V"
                    + start
                    + "–"
                    + end;
        }
    }
}
