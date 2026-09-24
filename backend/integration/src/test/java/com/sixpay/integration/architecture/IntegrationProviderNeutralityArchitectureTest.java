package com.sixpay.integration.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationProviderNeutralityArchitectureTest {

    private static final Path INTEGRATION =
            Path.of("src/main/java/com/sixpay/integration");

    @Test
    void integrationDoesNotOwnPartnerPayloadsMappingsOrSemantics() throws Exception {
        try (Stream<Path> paths = Files.walk(INTEGRATION)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(path);
                assertThat(path.getFileName().toString())
                        .as("provider-specific integration type in %s", path)
                        .doesNotContain("Partner").doesNotContain("tresor" + "pay");
                assertThat(content)
                        .as("provider-specific integration semantics in %s", path)
                        .doesNotContain("Partner").doesNotContain("tresor" + "pay")
                        .doesNotContain("partner").doesNotContain("PARTNER")
                        .doesNotContain("PARTNER").doesNotContain(".partner.");
            }
        }
    }
}
