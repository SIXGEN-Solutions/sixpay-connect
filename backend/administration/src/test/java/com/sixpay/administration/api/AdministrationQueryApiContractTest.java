package com.sixpay.administration.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrationQueryApiContractTest {

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
    void publishedContractContainsImplementedReadOnlyEndpoints()
            throws IOException {

        assertThat(CONTRACT).isRegularFile();

        String source = Files.readString(CONTRACT);

        assertThat(source)
                .contains("/internal/api/v1/administration/overview:")
                .contains("/internal/api/v1/administration/settings:")
                .contains("/internal/api/v1/administration/integrations:")
                .contains("model: ROLE_BASED")
                .contains("- ADMIN")
                .contains("bearerAuth:")
                .contains("readOnly: true");

        assertThat(source)
                .doesNotContain(
                        "administration.read",
                        "SCOPE_administration.read",
                        "put:",
                        "post:",
                        "delete:",
                        "patch:"
                );
    }
}
