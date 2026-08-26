package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase5ReadinessConfigurationTest {

    @Test
    void standaloneDoesNotEnableRealExternalEmailByDefault()
            throws Exception {
        String configuration = standalone();

        assertTrue(
                configuration.contains(
                        "SIXPAY_NOTIFICATION_OPERATIONAL_EMAIL_ENABLED:false"
                )
        );
    }

    @Test
    void standaloneContainsNoCommittedProductionSecret()
            throws Exception {
        String configuration = standalone();

        for (String forbidden : List.of(
                "BEGIN PRIVATE KEY",
                "BEGIN RSA PRIVATE KEY",
                "client-secret: ey",
                "password: supersecret",
                "api-key-value: sk_"
        )) {
            assertFalse(
                    configuration.contains(forbidden),
                    () -> "Committed secret material found: "
                            + forbidden
            );
        }
    }

    @Test
    void sandboxProfilesRemainExplicitlySeparate()
            throws Exception {
        for (String path : List.of(
                "application-amplitude-sandbox.yml",
                "application-amplitude-payment-sandbox.yml",
                "application-accounting-api-sandbox.yml"
        )) {
            assertTrue(
                    Files.isRegularFile(
                            Path.of(
                                    "src/main/resources/"
                                            + path
                            )
                    ),
                    () -> "Missing sandbox profile: "
                            + path
            );
        }
    }

    @Test
    void operationalMetricsAreExposedForReadiness()
            throws Exception {
        String configuration = standalone();

        assertTrue(
                configuration.contains(
                        "health,info,flyway,mappings,metrics"
                )
        );
    }

    private static String standalone()
            throws Exception {
        return Files.readString(
                Path.of(
                        "src/main/resources/"
                                + "application-standalone.yml"
                )
        );
    }
}
