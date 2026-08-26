package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeProfileConfigurationArchitectureTest {

    private static final Path RESOURCES =
            Path.of("src/main/resources");

    @Test
    void profilesNeverReferenceHistoricalFlywayLocations()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (Path profile : applicationProfiles()) {

            String source = Files.readString(profile);

            if (source.contains("db/security/migration")) {
                violations.add(
                        profile
                                + " references obsolete "
                                + "db/security/migration"
                );
            }

            if (source.contains("baseline-on-migrate: true")) {
                violations.add(
                        profile
                                + " enables forbidden "
                                + "baseline-on-migrate"
                );
            }

            if (source.matches("(?s).*V2026[^\\n]*\\.sql.*")) {
                violations.add(
                        profile
                                + " references historical V2026 migration"
                );
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Historical Flyway profile references: "
                        + violations
        );
    }

    @Test
    void profileFlywayLocationsRemainCanonical()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (Path profile : applicationProfiles()) {

            String source = Files.readString(profile);

            if (!source.contains("flyway:")) {
                continue;
            }

            if (source.contains("locations:")
                    && !source.contains(
                    "classpath:db/migration"
            )) {
                violations.add(
                        profile
                                + " defines Flyway locations "
                                + "without canonical classpath:db/migration"
                );
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Non-canonical Flyway profile locations: "
                        + violations
        );
    }

    @Test
    void profilesCannotIntroduceDestructiveHibernateSchemaCreation()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (Path profile : applicationProfiles()) {

            String source =
                    Files.readString(profile)
                            .toLowerCase();

            if (source.contains("ddl-auto: create-drop")
                    || source.contains("ddl-auto: create")) {
                violations.add(profile.toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Destructive Hibernate schema profile: "
                        + violations
        );
    }

    @Test
    void localAuthenticationProfileKeepsLocalOnlySemantics()
            throws Exception {

        String source =
                Files.readString(
                        RESOURCES.resolve(
                                "application-local-auth.yml"
                        )
                );

        int local = source.indexOf("local:");
        int oidc = source.indexOf("oidc:");

        assertTrue(local >= 0);
        assertTrue(oidc >= 0);

        assertTrue(
                source.substring(local, oidc)
                        .contains("enabled: true"),
                "local-auth must keep local enabled"
        );

        assertTrue(
                source.substring(oidc)
                        .contains("enabled: false"),
                "local-auth must keep OIDC disabled"
        );
    }

    @Test
    void hybridAuthenticationProfileKeepsLocalAndOidcEnabled()
            throws Exception {

        String source =
                Files.readString(
                        RESOURCES.resolve(
                                "application-hybrid-auth.yml"
                        )
                );

        int local = source.indexOf("local:");
        int oidc = source.indexOf("oidc:");

        assertTrue(local >= 0);
        assertTrue(oidc >= 0);

        assertTrue(
                source.substring(local, oidc)
                        .contains("enabled: true"),
                "hybrid-auth must keep local enabled"
        );

        assertTrue(
                source.substring(oidc)
                        .contains("enabled: true"),
                "hybrid-auth must keep OIDC enabled"
        );
    }

    @Test
    void standaloneDeveloperProfileRemainsExplicitlyDevelopmentOriented()
            throws Exception {

        Path standalone =
                RESOURCES.resolve(
                        "application-standalone.yml"
                );

        assertTrue(Files.isRegularFile(standalone));

        String source = Files.readString(standalone);

        assertTrue(
                source.contains(
                        "SPRING_JPA_HIBERNATE_DDL_AUTO:update"
                ),
                "FS-2.5.3 preserves existing standalone developer default"
        );
    }

    private List<Path> applicationProfiles()
            throws Exception {

        try (var files = Files.list(RESOURCES)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name =
                                path.getFileName().toString();

                        return name.startsWith("application")
                                && (
                                name.endsWith(".yml")
                                        || name.endsWith(".yaml")
                        );
                    })
                    .sorted()
                    .toList();
        }
    }
}
