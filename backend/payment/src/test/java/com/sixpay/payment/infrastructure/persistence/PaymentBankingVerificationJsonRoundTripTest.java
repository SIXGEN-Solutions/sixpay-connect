package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real JSON serialization test; PaymentStateDocument.toState() alone is insufficient. */
class PaymentBankingVerificationJsonRoundTripTest {

    @Test
    void verifiedBankingEvidenceSurvivesActualPaymentJsonPersistenceRoundTrip() {
        Payment original = PaymentAggregateTestFixtures.sixpayAuthorizedPayment();
        var mapper = new PaymentPersistenceMapper(JsonMapper.builder()
                .findAndAddModules()
                .build());

        // Both functions use the same dedicated production JSON mapper.
        // No database is necessary to reproduce the previous failure.
        PaymentJpaEntity entity = mapper.toNewEntity(original);
        assertTrue(entity.statePayload().contains("\"bankingVerificationEvidence\""));

        Payment restored = mapper.toDomain(entity);
        BankingVerificationSnapshot before = original.toState()
                .bankingVerificationEvidence().orElseThrow();
        BankingVerificationSnapshot after = restored.toState()
                .bankingVerificationEvidence().orElseThrow();

        assertEquals(before.verificationId(), after.verificationId());
        assertEquals(before.outcome(), after.outcome());
        assertEquals(before.accountBindingFingerprint(), after.accountBindingFingerprint());
        assertEquals(before.customerReferenceOptional(), after.customerReferenceOptional());
        assertEquals(before.accountReferenceOptional(), after.accountReferenceOptional());
        assertEquals(before.checks(), after.checks());
        assertEquals(before.metadata(), after.metadata());
        assertEquals(original.toState().status(), restored.toState().status());
    }
}
