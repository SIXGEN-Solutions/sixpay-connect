package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCustomerVerificationWorkflowArchitectureTest {

    private static final Path PAYMENT_ROOT = Path.of(
            "src/main/java/com/sixpay/payment"
    );

    @Test
    void paymentNeverImportsCustomerOrAmplitudeInfrastructure()
            throws Exception {

        try (var paths = Files.walk(PAYMENT_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return List.of(
                                            "import com.sixpay.customer.",
                                            "AmplitudeCustomerVerificationClient",
                                            "AmplitudeCustomerVerificationRequest",
                                            "AmplitudeCustomerVerificationResponse",
                                            "AmplitudeVerificationCheckResponse",
                                            "AmplitudeErrorResponse",
                                            "CoreBankingAccessTokenProvider",
                                            "OAuth2CoreBankingAccessTokenProvider",
                                            "com.sixpay.customer.verification."
                                                    + "infrastructure.banking",
                                            "sixpay.customer.verification.banking"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Payment boundary violations: "
                            + violations
            );
        }
    }

    @Test
    void workflowUsesAtomicCoordinatorAndNeverPersistsDirectly()
            throws Exception {

        String source = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentCustomerVerificationService.java"
                )
        );

        assertTrue(
                source.contains("coordinator.mutate(")
        );
        assertTrue(
                source.contains(
                        "payment.recordBankingVerification("
                )
        );
        assertFalse(
                source.contains(
                        "PaymentAtomicPersistencePort"
                )
        );
        assertFalse(
                source.contains("paymentRepository")
        );
        assertFalse(
                source.contains("outbox")
        );
    }

    @Test
    void aggregateKeepsCanonicalOutcomeTransitionsAndReplayProtection()
            throws Exception {

        String source = Files.readString(
                PAYMENT_ROOT.resolve(
                        "domain/model/Payment.java"
                )
        );

        int sameEvidence = source.indexOf(
                "if (sameBankingEvidence(evidence))"
        );
        int conflictingEvidence = source.indexOf(
                "Conflicting banking-verification evidence"
        );
        int requiredStatus = source.indexOf(
                "\"recordBankingVerification\""
        );

        assertTrue(sameEvidence >= 0);
        assertTrue(conflictingEvidence > sameEvidence);
        assertTrue(requiredStatus > sameEvidence);

        assertTrue(
                source.contains(
                        "PaymentStatus.FUNDS_CONTROL_PENDING"
                )
        );
        assertTrue(
                source.contains(
                        "PaymentStatus.REJECTED"
                )
        );
        assertTrue(
                source.contains(
                        "PaymentStatus.BANKING_VERIFICATION_PENDING"
                )
        );
        assertTrue(
                source.contains(
                        "requireRecoverableFailure("
                )
        );
        assertTrue(
                source.contains(
                        "requireRejectionFailure("
                )
        );
    }

    @Test
    void technicalFailureIsRetryableWithoutSyntheticEvidence()
            throws Exception {

        String source = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentCustomerVerificationService.java"
                )
        );

        int technicalCatch = source.indexOf(
                "catch (CustomerVerificationTechnicalException"
        );
        int recordEvidence = source.indexOf(
                "payment.recordBankingVerification("
        );

        assertTrue(technicalCatch >= 0);
        assertTrue(recordEvidence > technicalCatch);
        assertTrue(
                source.contains(
                        "PaymentCustomerVerificationRetryableException"
                )
        );
        assertFalse(
                source.contains(
                        "CustomerVerificationResponse.Outcome.REJECTED"
                )
        );
    }

    @Test
    void replayReusesStablePaymentOwnedIdentifiers()
            throws Exception {

        String service = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentCustomerVerificationService.java"
                )
        );

        String generator = Files.readString(
                PAYMENT_ROOT.resolve(
                        "infrastructure/customer/"
                                + "DeterministicPaymentCustomerVerificationIdGenerator.java"
                )
        );

        String factory = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentCustomerVerificationRequestFactory.java"
                )
        );

        assertTrue(
                service.contains(
                        "idGenerator.forPayment(paymentId)"
                )
        );
        assertTrue(
                service.contains(
                        "response.completedAt()"
                )
        );
        assertTrue(
                generator.contains(
                        "UUID.nameUUIDFromBytes("
                )
        );
        assertFalse(
                generator.contains(
                        "UUID.randomUUID("
                )
        );
        assertTrue(
                factory.contains(
                        "integrationAccountToken()"
                )
        );
        assertTrue(
                factory.contains(
                        "bindingFingerprint()"
                )
        );
        assertTrue(
                factory.contains(
                        "correlationId()"
                )
        );
    }

    @Test
    void atomicPersistencePortOwnsPaymentAuditAndOutboxWrite()
            throws Exception {

        String port = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/port/output/"
                                + "PaymentAtomicPersistencePort.java"
                )
        );

        String coordinator = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentMutationCoordinator.java"
                )
        );

        assertTrue(
                port.contains(
                        "Persists one changed Payment together "
                                + "with its audit and outbox records"
                )
        );
        assertTrue(
                coordinator.contains(
                        "atomicPersistencePort.persist("
                )
        );
        assertTrue(
                coordinator.contains(
                        "List<PaymentDomainEvent> events"
                )
        );
        assertTrue(
                coordinator.contains(
                        "requireEvents(payment)"
                )
        );
    }
}