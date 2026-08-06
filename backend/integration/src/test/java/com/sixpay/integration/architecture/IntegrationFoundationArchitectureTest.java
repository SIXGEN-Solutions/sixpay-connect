package com.sixpay.integration.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class IntegrationFoundationArchitectureTest {
    @Test
    void foundationContainsNoProviderSpecificPackages() throws Exception {
        Path source = Path.of("src/main/java");
        if (!Files.exists(source)) return;
        try (var paths = Files.walk(source)) {
            assertThat(paths.map(Path::toString)
                    .map(String::toLowerCase)
                    .filter(path -> path.contains("amplitude") || path.contains("tresorpay"))
                    .toList()).isEmpty();
        }
    }
}
