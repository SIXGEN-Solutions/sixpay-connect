package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackDelivery;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackMessage;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.repository.PaymentRepository;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntry;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "sixpay.payment.callback",
        name = "enabled",
        havingValue = "true"
)
public class PaymentCallbackPlanFactory {

    private final PaymentRepository paymentRepository;
    private final PaymentAuditAdapter auditAdapter;

    public PaymentCallbackPlanFactory(
            PaymentRepository paymentRepository,
            PaymentAuditAdapter auditAdapter
    ) {
        this.paymentRepository =
                Objects.requireNonNull(paymentRepository);
        this.auditAdapter =
                Objects.requireNonNull(auditAdapter);
    }

    public PaymentCallbackPlan create(
            ClaimedPaymentOutboxEvent event
    ) {
        List<PaymentAuditEntry> audit = auditAdapter
                .findByPaymentId(event.paymentId())
                .stream()
                .sorted(
                        Comparator
                                .comparingLong(
                                        PaymentAuditEntry
                                                ::businessVersion
                                )
                                .thenComparingInt(
                                        PaymentAuditEntry
                                                ::eventSequence
                                )
                )
                .toList();

        int currentIndex = indexOf(audit, event);

        if (currentIndex < 0) {
            throw new IllegalStateException(
                    "Outbox event has no matching audit entry"
            );
        }

        PaymentAuditEntry current =
                audit.get(currentIndex);

        if (!isLastEventOfVersion(audit, currentIndex)
                || current.paymentStatus()
                == PaymentStatus.RECEIVED
                || current.paymentStatus()
                == PaymentStatus.PENDING_CONFIRMATION) {
            return PaymentCallbackPlan.skip();
        }

        PaymentStatus previousStatus =
                previousDistinctStatus(
                        audit,
                        currentIndex,
                        current.paymentStatus()
                );

        if (previousStatus == null) {
            return PaymentCallbackPlan.skip();
        }

        Payment payment = paymentRepository
                .findById(
                        new PaymentId(event.paymentId())
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Missing Payment for callback"
                        )
                );

        PaymentState state = payment.toState();

        PaymentInitiationContext context = state
                .initiationContext()
                .orElse(null);

        if (context == null) {
            return PaymentCallbackPlan.skip();
        }

        PaymentStatusCallbackMessage message =
                new PaymentStatusCallbackMessage(
                        event.eventId(),
                        "PAYMENT_STATUS_CHANGED",
                        event.occurredAt(),
                        state.publicPaymentReference().value(),
                        state.externalPaymentReference().value(),
                        state.bankPostingReference()
                                .map(Object::toString)
                                .orElse(null),
                        previousStatus,
                        current.paymentStatus(),
                        state.failure()
                                .map(Object::toString)
                                .orElse(null),
                        description(
                                previousStatus,
                                current.paymentStatus()
                        ),
                        null
                );

        return PaymentCallbackPlan.deliver(
                new PaymentStatusCallbackDelivery(
                        context.callbackEndpoint().value(),
                        CorrelationId.of(
                                event.correlationId()
                        ),
                        message
                )
        );
    }

    private static int indexOf(
            List<PaymentAuditEntry> audit,
            ClaimedPaymentOutboxEvent event
    ) {
        for (int index = 0;
             index < audit.size();
             index++) {
            if (audit.get(index)
                    .eventId()
                    .equals(event.eventId())) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isLastEventOfVersion(
            List<PaymentAuditEntry> audit,
            int currentIndex
    ) {
        PaymentAuditEntry current =
                audit.get(currentIndex);

        return audit.stream()
                .filter(entry ->
                        entry.businessVersion()
                                == current.businessVersion()
                )
                .mapToInt(
                        PaymentAuditEntry::eventSequence
                )
                .max()
                .orElse(current.eventSequence())
                == current.eventSequence();
    }

    private static PaymentStatus previousDistinctStatus(
            List<PaymentAuditEntry> audit,
            int currentIndex,
            PaymentStatus currentStatus
    ) {
        for (int index = currentIndex - 1;
             index >= 0;
             index--) {
            PaymentStatus candidate =
                    audit.get(index).paymentStatus();

            if (candidate != currentStatus) {
                return candidate;
            }
        }

        return null;
    }

    private static String description(
            PaymentStatus previous,
            PaymentStatus current
    ) {
        return "Payment status changed from "
                + previous
                + " to "
                + current;
    }
}
