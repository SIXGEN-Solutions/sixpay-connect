package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase5AcceptanceScenarioCatalogTest {

    private static final Path SCENARIOS =
            Path.of(
                    "../../documentation/architecture/"
                            + "integration/"
                            + "phase5-e2e-scenarios.md"
            );

    @Test
    void mandatoryHappyAndFailureScenariosAreDocumented()
            throws Exception {
        String source =
                Files.readString(SCENARIOS);

        for (String scenario : List.of(
                "E2E-01", "E2E-02", "E2E-03",
                "E2E-04", "E2E-05", "E2E-06",
                "E2E-07", "E2E-08", "E2E-09",
                "E2E-10", "E2E-11", "E2E-12"
        )) {
            assertTrue(
                    source.contains(scenario),
                    () -> "Missing scenario "
                            + scenario
            );
        }
    }

    @Test
    void readinessSeparatesLocalAndExternalCertification()
            throws Exception {
        String source =
                Files.readString(SCENARIOS);

        assertTrue(
                source.contains(
                        "MODULAR_MONOLITH"
                )
        );

        assertTrue(
                source.contains(
                        "EXTERNAL_SANDBOX"
                )
        );
    }
}
