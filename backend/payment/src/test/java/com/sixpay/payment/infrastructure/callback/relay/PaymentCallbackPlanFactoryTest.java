package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackMessage;
import com.sixpay.payment.domain.event.PaymentEventOutcomeRecorded;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCallbackPlanFactoryTest {

    @Test
    void cutCreditedIsBoundToDurableCompletedT0OutcomeEvent() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry completedT0 = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(),
                PaymentStatus.POSTED_PENDING_TFJ
        );

        assertThat(method.invoke(null, completedT0))
                .isEqualTo(PaymentStatusCallbackMessage.CUT_CREDITED);
    }

    @Test
    void postedPendingTfjStatusFromAnotherEventDoesNotEmitCutCredited() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry unrelated = audit(
                "PaymentFinalResultAvailable",
                PaymentStatus.POSTED_PENDING_TFJ
        );

        assertThat(method.invoke(null, unrelated)).isNull();
    }

    @Test
    void nonT0StatusesDoNotEmitCutCredited() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry unknown = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(),
                PaymentStatus.POSTING_OUTCOME_UNKNOWN
        );

        assertThat(method.invoke(null, unknown)).isNull();
    }

    private static PaymentAuditEntry audit(
            String eventType,
            PaymentStatus status
    ) {
        return new PaymentAuditEntry(
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "PAY-1234567890ABCDEFGHJKMNPQRS",
                eventType,
                status,
                10L,
                1,
                "11111111-1111-1111-1111-111111111111",
                null,
                Instant.parse("2026-09-18T10:00:00Z")
        );
    }
}
