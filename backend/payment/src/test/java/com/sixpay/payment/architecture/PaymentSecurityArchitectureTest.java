package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentSecurityArchitectureTest {

    private static final Path SECURITY_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/application/security"
    );

    private static final Path API_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/api"
    );

    @Test
    void paymentDoesNotDuplicatePlatformRoles()
            throws IOException {
        try (Stream<Path> paths = Files.walk(SECURITY_ROOT)) {
            List<Path> duplicateRoles = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .equals("PaymentRole.java")
                    )
                    .toList();

            assertEquals(List.of(), duplicateRoles);
        }
    }

    @Test
    void partnerIsolationNeverUsesPaymentSourceAsOwnership()
            throws IOException {
        String policy = Files.readString(
                SECURITY_ROOT.resolve(
                        "PaymentPartnerIsolationPolicy.java"
                )
        );

        assertTrue(policy.contains("partnerSubjectOptional"));
        assertFalse(policy.contains(
                "source() == PaymentSource.TRESOR_PAY"
        ));
        assertFalse(policy.contains(
                "source() != PaymentSource.TRESOR_PAY"
        ));
    }

    @Test
    void objectAccessDoesNotLoadAggregate()
            throws IOException {
        Path port = Path.of(
                "src/main/java/com/sixpay/payment/"
                        + "application/port/output/security/"
                        + "PaymentObjectAccessPort.java"
        );

        String source = Files.readString(port);

        assertFalse(source.contains("PaymentRepository"));
        assertFalse(source.contains("domain.model.Payment;"));
        assertFalse(source.contains("PaymentJpaEntity"));
    }

    @Test
    void controllerUsesPaymentPolicy()
            throws IOException {
        String source = Files.readString(
                API_ROOT.resolve(
                        "PaymentQueryController.java"
                )
        );

        assertTrue(source.contains(
                "@paymentAccessPolicy.canSearch()"
        ));
        assertTrue(source.contains(
                "@paymentAccessPolicy.canRead()"
        ));
    }

    @Test
    void partnerOwnershipRemainsFailClosedInPersistence()
            throws IOException {
        String source = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "infrastructure/query/"
                                + "PaymentObjectAccessAdapter.java"
                )
        );

        assertTrue(source.contains(
                "new PaymentObjectAccessDescriptor"
        ));
        assertTrue(source.contains(
                "null"
        ));
        assertFalse(source.contains(
                "external_subscription_reference AS partner"
        ));
    }
}
