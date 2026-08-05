package com.sixpay.bootstrap.configuration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigurationArchitectureTest {

    private static final Path OPENAPI_CONFIGURATION =
            Path.of(
                    "src/main/java/com/sixpay/bootstrap/"
                            + "configuration/OpenApiConfiguration.java"
            );

    private static final Path STANDALONE_PROFILE =
            Path.of(
                    "src/main/resources/application-standalone.yml"
            );

    @Test
    void swaggerExposesPartnerPaymentAndCustomerGroups()
            throws Exception {

        String source =
                Files.readString(OPENAPI_CONFIGURATION);

        for (String required : List.of(
                ".group(\"partner\")",
                ".group(\"payment\")",
                ".group(\"customer\")",
                "/api/v1/partners/**",
                "/v1/payments/**",
                "/internal/api/v1/payments/**",
                "/internal/api/v1/observed-customers/**"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing OpenAPI group or path: "
                            + required
            );
        }
    }

    @Test
    void standaloneProfileEnablesCustomerQueryAndSwagger()
            throws Exception {

        String source =
                Files.readString(STANDALONE_PROFILE);

        for (String required : List.of(
                "springdoc:",
                "api-docs:",
                "swagger-ui:",
                "observation:",
                "query:",
                "enabled: true",
                "cursor-key-base64:",
                "protection-key-base64:",
                "audit:",
                "resilience:"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing standalone setting: "
                            + required
            );
        }
    }
}
