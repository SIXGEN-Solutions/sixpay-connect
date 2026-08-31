package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentConfirmationCanonicalizerTest {

    private final PaymentConfirmationCanonicalizer canonicalizer =
            new PaymentConfirmationCanonicalizer();

    @Test
    void revokeCanonicalFormIncludesChallengeAndReason() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.fromString(
                                "33333333-3333-4333-8333-333333333333"
                        )
                );

        String canonical = canonicalizer.revoke(
                paymentId,
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                ),
                new ConfirmationChallengeReference(
                        "challenge-revoke"
                ),
                "PAYMENT_REJECTED"
        );

        assertThat(canonical).isEqualTo(
                "v1|REVOKE|"
                        + paymentId
                        + "|PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                        + "|challenge-revoke"
                        + "|PAYMENT_REJECTED"
        );
    }

    @Test
    void revokeCanonicalFormChangesWhenReasonChanges() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.fromString(
                                "44444444-4444-4444-8444-444444444444"
                        )
                );

        String rejected = canonicalizer.revoke(
                paymentId,
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                ),
                new ConfirmationChallengeReference(
                        "challenge-revoke"
                ),
                "PAYMENT_REJECTED"
        );

        String failed = canonicalizer.revoke(
                paymentId,
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                ),
                new ConfirmationChallengeReference(
                        "challenge-revoke"
                ),
                "PAYMENT_FAILED"
        );

        assertThat(rejected).isNotEqualTo(failed);
    }
}
