package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase6AcceptanceScenarioCatalogTest {

    private static final Path ROOT =
            Path.of("../..").normalize();

    @Test
    void catalogCoversContractSecurityPersistenceAndExport()
            throws Exception {

        String catalog = Files.readString(
                ROOT.resolve(
                        "documentation/implementation/phase6/"
                                + "ACCEPTANCE-SCENARIOS.md"
                )
        );

        for (String scenario : List.of(
                "P6-A01",
                "P6-A02",
                "P6-A03",
                "P6-A04",
                "P6-A05",
                "P6-A06",
                "P6-A07",
                "P6-A08",
                "P6-A09",
                "P6-A10",
                "P6-A11",
                "P6-A12"
        )) {
            assertTrue(
                    catalog.contains(scenario),
                    () -> "Missing Phase 6 scenario: "
                            + scenario
            );
        }
    }
}
