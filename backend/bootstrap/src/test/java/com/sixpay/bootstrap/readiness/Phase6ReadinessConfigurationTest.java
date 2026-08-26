package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase6ReadinessConfigurationTest {

    private static final Path ROOT =
            Path.of("../..").normalize();

    @Test
    void auditCursorKeyIsMandatoryAndValidated()
            throws Exception {

        String properties = Files.readString(
                ROOT.resolve(
                        "backend/reporting/src/main/java/"
                                + "com/sixpay/reporting/"
                                + "configuration/"
                                + "ReportingAuditQueryProperties.java"
                )
        );

        assertTrue(properties.contains(
                "cursor-hmac-key is required"
        ));
        assertTrue(properties.contains(
                "at least 32 bytes"
        ));
        assertTrue(properties.contains(
                "Base64"
        ));
        assertFalse(properties.contains(
                "defaultCursor"
        ));
    }

    @Test
    void auditExportStorageConfigurationIsExplicit()
            throws Exception {

        String properties = Files.readString(
                ROOT.resolve(
                        "backend/reporting/src/main/java/"
                                + "com/sixpay/reporting/"
                                + "configuration/"
                                + "ReportingAuditExportProperties.java"
                )
        );

        assertTrue(properties.contains(
                "storage-directory is required"
        ));
        assertTrue(properties.contains(
                "retrieval-base-uri must be absolute"
        ));
        assertTrue(properties.contains(
                "retention must be positive"
        ));
    }

    @Test
    void productionFlywayRemainsCentralizedInBootstrap()
            throws Exception {

        String bootstrapConfig = Files.readString(
                ROOT.resolve(
                        "backend/bootstrap/src/main/resources/"
                                + "application.yml"
                )
        );

        String reportingPom = Files.readString(
                ROOT.resolve("backend/reporting/pom.xml")
        );

        assertTrue(bootstrapConfig.contains(
                "flyway:"
        ));
        assertTrue(bootstrapConfig.contains(
                "clean-disabled: true"
        ));
        assertFalse(reportingPom.contains(
                "<scope>runtime</scope>\n"
                        + "            </dependency>\n"
                        + "            <artifactId>"
                        + "spring-boot-starter-flyway"
        ));
    }

    @Test
    void observabilityUsesLowCardinalityOperationTags()
            throws Exception {

        String interceptor = Files.readString(
                ROOT.resolve(
                        "backend/reporting/src/main/java/"
                                + "com/sixpay/reporting/api/"
                                + "observability/"
                                + "PaymentAuditHttpObservationInterceptor.java"
                )
        );

        assertTrue(interceptor.contains(
                "sixpay.reporting.audit.http"
        ));
        assertTrue(interceptor.contains(
                ".tag(\"operation\", operation(request))"
        ));
        assertFalse(interceptor.contains(
                ".tag(\"paymentId\""
        ));
        assertFalse(interceptor.contains(
                ".tag(\"auditId\""
        ));
        assertFalse(interceptor.contains(
                ".tag(\"exportId\""
        ));
    }

    @Test
    void reportingClockInjectionIsExplicit()
            throws Exception {

        String controller = Files.readString(
                ROOT.resolve(
                        "backend/reporting/src/main/java/"
                                + "com/sixpay/reporting/api/controller/"
                                + "PaymentAuditQueryController.java"
                )
        );

        String configuration = Files.readString(
                ROOT.resolve(
                        "backend/reporting/src/main/java/"
                                + "com/sixpay/reporting/configuration/"
                                + "ReportingConfiguration.java"
                )
        );

        assertTrue(
                controller.contains(
                        "@Qualifier(\"reportingAuditClock\")"
                )
        );

        assertTrue(
                configuration.contains(
                        "@Qualifier(\"reportingAuditClock\")"
                )
        );
    }
}
