package com.sixpay.administration.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentQueryApiContractTest {

    private static final Path CONTRACT =
            Path.of(
                    "..",
                    "..",
                    "documentation",
                    "contracts",
                    "internal",
                    "administration-operational-api-v1.yaml"
            );

    @Test
    void publishedContractUsesExistingRoleBasedSecurityModel()
            throws IOException {

        assertThat(CONTRACT).isRegularFile();

        String source = Files.readString(CONTRACT);

        assertThat(source)
                .contains("/internal/api/v1/incidents:")
                .contains("/internal/api/v1/incidents/{incidentId}:")
                .contains("model: ROLE_BASED")
                .contains("- ADMIN")
                .contains("- MANAGER")
                .contains("- AUDITOR")
                .contains("bearerAuth:")
                .contains("readOnly: true");

        assertThat(source)
                .doesNotContain(
                        "incident.read",
                        "SCOPE_incident.read",
                        "post:",
                        "put:",
                        "patch:",
                        "delete:"
                );
    }
}
