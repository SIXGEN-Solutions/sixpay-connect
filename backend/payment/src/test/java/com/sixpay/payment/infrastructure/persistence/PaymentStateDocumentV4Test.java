package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckEvidence;
import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckType;
import com.sixpay.payment.domain.model.evidence.BankingVerificationId;
import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentStateDocumentV4Test {

    private static final String CUSTOMER_REFERENCE =
            "AMPLITUDE-CUSTOMER-001";
    private static final String ACCOUNT_REFERENCE =
            "AMPLITUDE-ACCOUNT-001";

    @Test
    void v4PreservesCanonicalBankingReferencesAcrossDocumentRoundTrip() {
        PaymentStateDocument base = PaymentStateDocument.from(
                PaymentAggregateTestFixtures.newPayment().toState()
        );

        BankingVerificationSnapshot evidence =
                bankingEvidence(
                        base,
                        CUSTOMER_REFERENCE,
                        ACCOUNT_REFERENCE
                );

        PaymentState restored =
                withBankingEvidence(base, evidence).toState();

        BankingVerificationSnapshot restoredEvidence =
                restored.bankingVerificationEvidence().orElseThrow();

        assertEquals(
                CUSTOMER_REFERENCE,
                restoredEvidence.customerReferenceOptional().orElseThrow()
        );
        assertEquals(
                ACCOUNT_REFERENCE,
                restoredEvidence.accountReferenceOptional().orElseThrow()
        );
    }

    @Test
    void v4RejectsVerifiedBankingEvidenceWithoutCanonicalReferences() {
        PaymentStateDocument base = PaymentStateDocument.from(
                PaymentAggregateTestFixtures.newPayment().toState()
        );

        BankingVerificationSnapshot legacyStyleVerifiedEvidence =
                bankingEvidence(base, null, null);

        PaymentStateDocument invalid =
                withBankingEvidence(
                        base,
                        legacyStyleVerifiedEvidence
                );

        assertThrows(
                PaymentPersistenceException.class,
                invalid::toState
        );
    }

    private static BankingVerificationSnapshot bankingEvidence(
            PaymentStateDocument base,
            String customerReference,
            String accountReference
    ) {
        Instant observedAt = base.receivedAt().plusSeconds(1);

        return new BankingVerificationSnapshot(
                new BankingVerificationId(
                        UUID.fromString(
                                "0e30f18e-45d8-4c4d-8f18-3114d81fc60e"
                        )
                ),
                BankingVerificationOutcome.VERIFIED,
                base.debtorAccountReference().bindingFingerprint(),
                customerReference,
                accountReference,
                List.of(
                        new BankingVerificationCheckEvidence(
                                BankingVerificationCheckType.CUSTOMER_EXISTS,
                                EvidenceCheckResult.PASS,
                                null,
                                observedAt
                        )
                ),
                new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        base.requestIdentity().correlationId(),
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        EvidenceFingerprint.of(
                                "v1:sha256:" + "d".repeat(64)
                        ),
                        observedAt,
                        observedAt
                )
        );
    }

    private static PaymentStateDocument withBankingEvidence(
            PaymentStateDocument base,
            BankingVerificationSnapshot evidence
    ) {
        return new PaymentStateDocument(
                PaymentStateDocument.CURRENT_SCHEMA_VERSION,
                base.paymentId(),
                base.publicPaymentReference(),
                base.source(),
                base.externalPaymentReference(),
                base.externalSubscriptionReference(),
                base.requestIdentity(),
                base.financialInstitutionCode(),
                base.debtorAccountReference(),
                base.requestedAmount(),
                base.treasuryAllocationIntent(),
                base.allocationIntentFingerprint(),
                base.initiationContext(),
                base.customerConfirmationEvidence(),
                base.confirmationChallenge(),
                base.status(),
                base.authorizationEvidence(),
                base.sixpayAuthorizationDecision(),
                evidence,
                base.fundsControlEvidence(),
                base.treasuryResolutionEvidence(),
                base.treasuryAccountReference(),
                base.postingInstruction(),
                base.postingOutcomeEvidence(),
                base.bankPostingReference(),
                base.endOfDayConfirmationEvidence(),
                base.reversalInstruction(),
                base.reversalAuthorizationEvidence(),
                base.reversalEvidence(),
                base.failure(),
                base.businessVersion(),
                base.receivedAt(),
                base.updatedAt(),
                base.finalizedAt()
        );
    }
}
