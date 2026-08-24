package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapGlobalConfigurationArchitectureTest {

    private static final Path BACKEND_ROOT =
            Path.of("..");

    private static final Path BASE_APPLICATION =
            Path.of("src/main/resources/application.yml");

    private static final List<String> BUSINESS_MODULES =
            List.of(
                    "partner",
                    "customer",
                    "payment",
                    "accounting",
                    "reporting",
                    "notification",
                    "security",
                    "administration"
            );

    private static final Set<String>
            ALLOWED_BASE_SIXPAY_PREFIXES =
            Set.of(
                    "sixpay.messaging.",
                    "sixpay.customer.verification.banking.",
                    "sixpay.security.local.password."
            );

    private static final List<String>
            FORBIDDEN_DOMAIN_GLOBAL_PREFIXES =
            List.of(
                    "server:",
                    "spring.datasource:",
                    "spring.jpa:",
                    "spring.flyway:",
                    "springdoc:",
                    "management:"
            );

    @Test
    void baseApplicationPreservesCanonicalGlobalRuntimeInvariants()
            throws Exception {

        String source =
                Files.readString(BASE_APPLICATION);

        assertContains(
                source,
                "name: sixpay-connect"
        );

        assertContains(
                source,
                "ddl-auto: validate"
        );

        assertContains(
                source,
                "schemas: sixpay"
        );

        assertContains(
                source,
                "default-schema: sixpay"
        );

        assertContains(
                source,
                "locations: classpath:db/migration"
        );

        assertContains(
                source,
                "validate-on-migrate: true"
        );

        assertContains(
                source,
                "clean-disabled: true"
        );

        assertContains(
                source,
                "api-docs:"
        );

        assertContains(
                source,
                "swagger-ui:"
        );

        assertContains(
                source,
                "include: health,info"
        );
    }

    @Test
    void baseApplicationCannotAccumulateNewDomainConfiguration()
            throws Exception {

        List<String> leafPaths =
                yamlLeafPaths(
                        BASE_APPLICATION
                );

        List<String> violations =
                leafPaths.stream()
                        .filter(path ->
                                path.startsWith(
                                        "sixpay."
                                )
                        )
                        .filter(path ->
                                ALLOWED_BASE_SIXPAY_PREFIXES
                                        .stream()
                                        .noneMatch(
                                                path::startsWith
                                        )
                        )
                        .toList();

        assertTrue(
                violations.isEmpty(),
                () -> "New domain-owned configuration "
                        + "was added to base application.yml: "
                        + violations
                        + ". FS-2.5.1 freezes the current "
                        + "transition debt until FS-2.5.2."
        );
    }

    @Test
    void businessModulesCannotOwnMonolithGlobalRuntimeNamespaces()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (String module :
                BUSINESS_MODULES) {

            Path resources =
                    BACKEND_ROOT.resolve(
                            module
                                    + "/src/main/resources"
                    );

            if (!Files.isDirectory(
                    resources
            )) {
                continue;
            }

            try (var files =
                         Files.walk(resources)) {

                for (Path file :
                        files.filter(
                                        Files::isRegularFile
                                )
                                .filter(path -> {
                                    String name =
                                            path.getFileName()
                                                    .toString();

                                    return name.startsWith(
                                            "application"
                                    )
                                            && (
                                            name.endsWith(
                                                    ".yml"
                                            )
                                                    || name.endsWith(
                                                    ".yaml"
                                            )
                                    );
                                })
                                .toList()) {

                    List<String> roots =
                            yamlTopLevelAndSecondLevelPaths(
                                    file
                            );

                    for (String prefix :
                            forbiddenGlobalPaths()) {

                        if (roots.contains(
                                prefix
                        )) {
                            violations.add(
                                    module
                                            + ": "
                                            + file
                                            + " owns "
                                            + prefix
                            );
                        }
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Business modules must not own "
                        + "modular-monolith global runtime "
                        + "configuration: "
                        + violations
        );
    }

    private List<String> forbiddenGlobalPaths() {
        return List.of(
                "server",
                "spring.datasource",
                "spring.jpa",
                "spring.flyway",
                "springdoc",
                "management"
        );
    }

    private void assertContains(
            String source,
            String token
    ) {

        assertTrue(
                source.contains(token),
                () -> "Canonical Bootstrap global "
                        + "configuration missing: "
                        + token
        );
    }

    private List<String> yamlLeafPaths(
            Path file
    ) throws IOException {

        List<String> result =
                new ArrayList<>();

        List<YamlLevel> stack =
                new ArrayList<>();

        List<String> lines =
                Files.readAllLines(file);

        for (String line : lines) {

            String trimmed =
                    line.trim();

            if (trimmed.isEmpty()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith("- ")) {
                continue;
            }

            int separator =
                    trimmed.indexOf(':');

            if (separator <= 0) {
                continue;
            }

            String key =
                    trimmed.substring(
                            0,
                            separator
                    ).trim();

            String remainder =
                    trimmed.substring(
                            separator + 1
                    ).trim();

            int indent =
                    indentation(line);

            while (!stack.isEmpty()
                    && stack.get(
                    stack.size() - 1
            ).indent() >= indent) {
                stack.remove(
                        stack.size() - 1
                );
            }

            stack.add(
                    new YamlLevel(
                            indent,
                            key
                    )
            );

            if (!remainder.isEmpty()) {
                result.add(
                        stack.stream()
                                .map(
                                        YamlLevel::key
                                )
                                .reduce(
                                        (left, right) ->
                                                left
                                                        + "."
                                                        + right
                                )
                                .orElse("")
                );
            }
        }

        return result;
    }

    private List<String>
    yamlTopLevelAndSecondLevelPaths(
            Path file
    ) throws IOException {

        List<String> result =
                new ArrayList<>();

        List<YamlLevel> stack =
                new ArrayList<>();

        for (String line :
                Files.readAllLines(file)) {

            String trimmed =
                    line.trim();

            if (trimmed.isEmpty()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith("- ")) {
                continue;
            }

            int separator =
                    trimmed.indexOf(':');

            if (separator <= 0) {
                continue;
            }

            String key =
                    trimmed.substring(
                            0,
                            separator
                    ).trim();

            int indent =
                    indentation(line);

            while (!stack.isEmpty()
                    && stack.get(
                    stack.size() - 1
            ).indent() >= indent) {
                stack.remove(
                        stack.size() - 1
                );
            }

            stack.add(
                    new YamlLevel(
                            indent,
                            key
                    )
            );

            if (stack.size() == 1) {
                result.add(key);
            } else if (stack.size() == 2) {
                result.add(
                        stack.get(0).key()
                                + "."
                                + stack.get(1).key()
                );
            }
        }

        return result;
    }

    private int indentation(
            String line
    ) {

        int count = 0;

        for (char value :
                line.toCharArray()) {

            if (value == ' ') {
                count++;
            } else if (value == '\t') {
                count += 4;
            } else {
                break;
            }
        }

        return count;
    }

    private record YamlLevel(
            int indent,
            String key
    ) {
    }
}
