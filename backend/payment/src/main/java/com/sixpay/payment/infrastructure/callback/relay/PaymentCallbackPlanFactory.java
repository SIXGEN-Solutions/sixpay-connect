package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackDelivery;
import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackMessage;
import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.EndOfDayConfirmationSnapshot;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcomeSnapshot;
import com.sixpay.payment.domain.model.evidence.TfjStatus;
import com.sixpay.payment.domain.repository.PaymentRepository;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "sixpay.payment.callback", name = "enabled", havingValue = "true")
public class PaymentCallbackPlanFactory {
    private final PaymentRepository paymentRepository;
    private final PaymentAuditAdapter auditAdapter;

    public PaymentCallbackPlanFactory(PaymentRepository paymentRepository, PaymentAuditAdapter auditAdapter) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.auditAdapter = Objects.requireNonNull(auditAdapter);
    }

    public PaymentCallbackPlan create(ClaimedPaymentOutboxEvent event) {
        List<PaymentAuditEntry> audit = auditAdapter.findByPaymentId(event.paymentId()).stream()
                .sorted(Comparator.comparingLong(PaymentAuditEntry::businessVersion).thenComparingInt(PaymentAuditEntry::eventSequence))
                .toList();
        int currentIndex = indexOf(audit, event);
        if (currentIndex < 0) throw new IllegalStateException("Outbox event has no matching audit entry");
        PaymentAuditEntry current = audit.get(currentIndex);
        if (!isLastEventOfVersion(audit, currentIndex)) return PaymentCallbackPlan.skip();
        String callbackType = callbackType(current.paymentStatus());
        if (callbackType == null) return PaymentCallbackPlan.skip();

        Payment payment = paymentRepository.findById(new PaymentId(event.paymentId()))
                .orElseThrow(() -> new IllegalStateException("Missing Payment for callback"));
        PaymentState state = payment.toState();
        PaymentInitiationContext context = state.initiationContext().orElse(null);
        if (context == null) return PaymentCallbackPlan.skip();

        return PaymentCallbackPlan.deliver(new PaymentStatusCallbackDelivery(
                context.callbackEndpoint().value(),
                CorrelationId.of(event.correlationId()),
                UUID.randomUUID(),
                event.attemptCount(),
                buildMessage(event, current, state, callbackType)
        ));
    }

    private static PaymentStatusCallbackMessage buildMessage(ClaimedPaymentOutboxEvent event, PaymentAuditEntry current, PaymentState state, String callbackType) {
        Object data;
        if (PaymentStatusCallbackMessage.CUT_CREDITED.equals(callbackType)) {
            PaymentEventOutcomeSnapshot outcome = state.paymentEventOutcomeEvidence()
                    .orElseThrow(() -> new IllegalStateException("CUT_CREDITED requires Payment event outcome"));
            BankPostingReference bankReference = state.bankPostingReference()
                    .orElseThrow(() -> new IllegalStateException("CUT_CREDITED requires bank posting reference"));
            data = new PaymentStatusCallbackMessage.CutCreditedData(
                    new PaymentStatusCallbackMessage.MoneyData(
                            state.requestedAmount().amount().stripTrailingZeros().toPlainString(),
                            state.requestedAmount().currency().getCurrencyCode()),
                    bankReference.principalPostingReference(),
                    outcome.observedAt(),
                    "POSTED_PENDING_TFJ",
                    false);
        } else {
            EndOfDayConfirmationSnapshot tfj = state.endOfDayConfirmationEvidence()
                    .filter(snapshot -> snapshot.tfjStatus() == TfjStatus.INTEGRATED)
                    .orElseThrow(() -> new IllegalStateException("TREASURY_INTEGRATED requires integrated TFJ evidence"));
            data = new PaymentStatusCallbackMessage.TreasuryIntegratedData(
                    tfj.businessDate(),
                    tfj.principalBankPostingReference(),
                    tfj.confirmationId().value(),
                    tfj.tfjBatchReference().orElse(null),
                    tfj.matchedAt());
        }
        return new PaymentStatusCallbackMessage(
                "1.0",
                event.eventId(),
                callbackType,
                event.occurredAt(),
                UUID.fromString(event.correlationId()),
                current.causationId(),
                event.paymentId(),
                state.publicPaymentReference().value(),
                state.externalPaymentReference().value(),
                state.financialInstitutionCode().value(),
                current.businessVersion(),
                data);
    }

    private static String callbackType(PaymentStatus status) {
        return switch (status) {
            case POSTED_PENDING_TFJ -> PaymentStatusCallbackMessage.CUT_CREDITED;
            case TREASURY_INTEGRATED -> PaymentStatusCallbackMessage.TREASURY_INTEGRATED;
            default -> null;
        };
    }

    private static int indexOf(List<PaymentAuditEntry> audit, ClaimedPaymentOutboxEvent event) {
        for (int index = 0; index < audit.size(); index++) {
            if (audit.get(index).eventId().equals(event.eventId())) return index;
        }
        return -1;
    }

    private static boolean isLastEventOfVersion(List<PaymentAuditEntry> audit, int currentIndex) {
        PaymentAuditEntry current = audit.get(currentIndex);
        return audit.stream().filter(entry -> entry.businessVersion() == current.businessVersion())
                .mapToInt(PaymentAuditEntry::eventSequence).max().orElse(current.eventSequence()) == current.eventSequence();
    }
}
