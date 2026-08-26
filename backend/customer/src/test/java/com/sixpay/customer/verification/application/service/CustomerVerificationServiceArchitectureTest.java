package com.sixpay.customer.verification.application.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerVerificationServiceArchitectureTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/sixpay/customer/verification/"
                    + "application/service/CustomerVerificationService.java"
    );

    @Test
    void serviceDependsOnlyOnCustomerDomainAndPorts()
            throws Exception {

        String source = Files.readString(SERVICE);

        for (String forbidden : List.of(
                "import org.springframework.",
                "import jakarta.",
                "import com.sixpay.payment.",
                "import com.sixpay.customer.verification.infrastructure.",
                "Amplitude",
                "RestClient",
                "HttpClient",
                "HttpStatus",
                "Thread.sleep",
                "Instant.now(",
                "UUID.randomUUID("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Application service contains forbidden concept: "
                            + forbidden
            );
        }

        for (String required : List.of(
                "implements VerifyCustomerUseCase",
                "BankingCustomerVerificationPort",
                "CustomerVerificationRepository",
                "CustomerVerificationDomainEventPublisher",
                "CustomerVerificationEventIdGenerator",
                "CustomerVerificationTimeProvider"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing orchestration dependency: "
                            + required
            );
        }
    }

    @Test
    void eventPublicationOccursAfterCompletedPersistence()
            throws Exception {

        String source = Files.readString(SERVICE);

        int complete = source.indexOf("verification.complete(");
        int completedSave = source.indexOf(
                "repository.save(verification);",
                complete
        );
        int publish = source.indexOf(
                "eventPublisher.publish(events);"
        );

        assertTrue(complete >= 0);
        assertTrue(completedSave > complete);
        assertTrue(publish > completedSave);
    }
}
