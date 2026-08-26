package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSpringdocConfigurationArchitectureTest {

    private static final Path BACKEND_ROOT =
            Path.of("..");

    private static final Path BOOTSTRAP_RESOURCES =
            Path.of("src/main/resources");

    private static final Path OPENAPI_CONFIGURATION =
            Path.of(
                    "src/main/java/com/sixpay/bootstrap/"
                            + "configuration/OpenApiConfiguration.java"
            );

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

    private static final Set<String> EXPECTED_GROUPS =
            Set.of(
                    "partner",
                    "customer",
                    "payment",
                    "administration",
                    "reporting",
                    "accounting"
            );

    private static final Pattern GROUP_PATTERN =
            Pattern.compile(
                    "\\.group\\(\"([^\"]+)\"\\)"
            );

    @Test
    void bootstrapOwnsCanonicalOpenApiAssembly()
            throws Exception {

        assertTrue(
                Files.isRegularFile(
                        OPENAPI_CONFIGURATION
                ),
                "Bootstrap OpenApiConfiguration must exist"
        );

        String source =
                Files.readString(
                        OPENAPI_CONFIGURATION
                );

        assertTrue(
                source.contains(
                        "@OpenAPIDefinition"
                )
        );

        assertTrue(
                source.contains(
                        "@SecurityScheme"
                )
        );

        assertTrue(
                source.contains(
                        "name = \"bearerAuth\""
                )
        );

        assertTrue(
                source.contains(
                        "type = SecuritySchemeType.HTTP"
                )
        );

        assertTrue(
                source.contains(
                        "bearerFormat = \"JWT\""
                )
        );
    }

    @Test
    void groupedOpenApiTopologyRemainsCanonical()
            throws Exception {

        String source =
                Files.readString(
                        OPENAPI_CONFIGURATION
                );

        Matcher matcher =
                GROUP_PATTERN.matcher(
                        source
                );

        Set<String> groups =
                new java.util.LinkedHashSet<>();

        while (matcher.find()) {
            groups.add(
                    matcher.group(1)
            );
        }

        assertEquals(
                EXPECTED_GROUPS,
                groups,
                "Runtime OpenAPI groups changed without "
                        + "FS-2.5 architecture review"
        );
    }

    @Test
    void paymentTimelineRemainsOwnedByReportingGroup()
            throws Exception {

        String source =
                Files.readString(
                        OPENAPI_CONFIGURATION
                );

        String paymentBean =
                beanSection(
                        source,
                        "GroupedOpenApi paymentOpenApi()",
                        "GroupedOpenApi administrationOpenApi()"
                );

        String reportingBean =
                beanSection(
                        source,
                        "GroupedOpenApi reportingOpenApi()",
                        "GroupedOpenApi accountingOpenApi()"
                );

        assertTrue(
                paymentBean.contains(
                        ".pathsToExclude("
                )
                        && paymentBean.contains(
                        "/internal/api/v1/payments/*/timeline"
                ),
                "Payment timeline must remain excluded "
                        + "from Payment OpenAPI group"
        );

        assertTrue(
                reportingBean.contains(
                        "/internal/api/v1/payments/*/timeline"
                ),
                "Payment timeline must remain included "
                        + "in Reporting OpenAPI group"
        );
    }

    @Test
    void administrationGroupKeepsOperationalAndIncidentSurface()
            throws Exception {

        String source =
                Files.readString(
                        OPENAPI_CONFIGURATION
                );

        String administration =
                beanSection(
                        source,
                        "GroupedOpenApi administrationOpenApi()",
                        "GroupedOpenApi reportingOpenApi()"
                );

        for (String path :
                List.of(
                        "/internal/api/v1/administration/users",
                        "/internal/api/v1/administration/overview",
                        "/internal/api/v1/administration/settings",
                        "/internal/api/v1/administration/integrations",
                        "/internal/api/v1/incidents"
                )) {

            assertTrue(
                    administration.contains(
                            path
                    ),
                    () -> "Administration OpenAPI group "
                            + "lost canonical path: "
                            + path
            );
        }
    }

    @Test
    void springdocIsDisabledByDefaultAndEnabledForStandalone()
            throws Exception {

        String base =
                Files.readString(
                        BOOTSTRAP_RESOURCES.resolve(
                                "application.yml"
                        )
                );

        String standalone =
                Files.readString(
                        BOOTSTRAP_RESOURCES.resolve(
                                "application-standalone.yml"
                        )
                );

        assertTrue(
                base.contains(
                        "api-docs:"
                )
                        && base.contains(
                        "swagger-ui:"
                ),
                "Base runtime must explicitly configure Springdoc"
        );

        assertTrue(
                countOccurrences(
                        base,
                        "enabled: false"
                ) >= 2,
                "Base runtime must keep API docs and Swagger UI disabled"
        );

        assertTrue(
                standalone.contains(
                        "springdoc:"
                )
                        && countOccurrences(
                        standalone,
                        "enabled: true"
                ) >= 2,
                "Standalone must keep API docs and Swagger UI enabled"
        );
    }

    @Test
    void businessModulesDoNotOwnGroupedOpenApiRuntimeAssembly()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (String module :
                BUSINESS_MODULES) {

            Path javaRoot =
                    BACKEND_ROOT.resolve(
                            module
                                    + "/src/main/java"
                    );

            if (!Files.isDirectory(
                    javaRoot
            )) {
                continue;
            }

            try (var files =
                         Files.walk(
                                 javaRoot
                         )) {

                for (Path javaFile :
                        files.filter(
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

                    if (source.contains(
                            "org.springdoc.core.models.GroupedOpenApi"
                    )
                            || source.contains(
                            "GroupedOpenApi.builder()"
                    )) {

                        violations.add(
                                module
                                        + " owns runtime OpenAPI grouping in "
                                        + javaFile
                        );
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "GroupedOpenApi runtime assembly "
                        + "must remain Bootstrap-owned: "
                        + violations
        );
    }

    private String beanSection(
            String source,
            String startToken,
            String endToken
    ) {

        int start =
                source.indexOf(
                        startToken
                );

        assertTrue(
                start >= 0,
                () -> "Missing OpenAPI bean: "
                        + startToken
        );

        int end =
                source.indexOf(
                        endToken,
                        start
                );

        assertTrue(
                end > start,
                () -> "Unable to delimit OpenAPI bean: "
                        + startToken
        );

        return source.substring(
                start,
                end
        );
    }

    private int countOccurrences(
            String source,
            String token
    ) {

        int count = 0;
        int index = 0;

        while ((index =
                source.indexOf(
                        token,
                        index
                )) >= 0) {

            count++;
            index += token.length();
        }

        return count;
    }
}
