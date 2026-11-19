package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Structural regression checks for P0 safeguards; behavioral tests remain mandatory. */
class PaymentConcurrencyP0ArchitectureTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src/main/java/com/sixpay/payment/" + relative));
    }

    @Test
    void optimisticConflictHasApplicationLevelSignal() throws Exception {
        assertTrue(source("infrastructure/persistence/PaymentRepositoryAdapter.java")
                .contains("new PaymentConcurrentMutationException("));
        assertTrue(source("infrastructure/persistence/PaymentJpaEntity.java")
                .contains("new PaymentConcurrentMutationException("));
    }

    @Test
    void customerConflictRecoveryRequiresDurableMatchingEvidence() throws Exception {
        String service = source("application/service/PaymentCustomerVerificationService.java");
        assertTrue(service.contains("catch (PaymentConcurrentMutationException conflict)"));
        assertTrue(service.contains("bankingVerificationEvidence().orElse(null)"));
        assertTrue(service.contains("candidate.metadata().evidenceFingerprint()"));
        assertTrue(service.contains("throw conflict;"));
        assertTrue(service.contains("gate.lock();"));
        assertTrue(service.contains("gate.unlock();"));
    }

    @Test
    void treasuryEvidenceIncludesBankAccountAndReplayComparesIt() throws Exception {
        assertTrue(source("application/service/PaymentT0OrchestrationService.java")
                .contains("resolved.accountToken()"));
        assertTrue(source("domain/model/Payment.java")
                .contains("candidate.treasuryAccountReference().orElse(null)"));
    }
}
