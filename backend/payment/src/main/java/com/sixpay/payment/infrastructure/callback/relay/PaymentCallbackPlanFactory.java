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
import com.sixpay.payment.domain.event.PaymentEndOfDayConfirmationRecorded;
import com.sixpay.payment.domain.event.PaymentEventOutcomeRecorded;
import com.sixpay.payment.domain.model.evidence.EndOfDayConfirmationSnapshot;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcome;
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
import java.time.Instant;
import java.time.LocalDate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "sixpay.payment.callback", name = "enabled", havingValue = "true")
public class PaymentCallbackPlanFactory {
    private final PaymentRepository paymentRepository;
    private final PaymentAuditAdapter auditAdapter;
    private final ObjectMapper objectMapper;

    public PaymentCallbackPlanFactory(PaymentRepository paymentRepository,
                                      PaymentAuditAdapter auditAdapter,
                                      ObjectMapper objectMapper) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.auditAdapter = Objects.requireNonNull(auditAdapter);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public PaymentCallbackPlan create(ClaimedPaymentOutboxEvent event) {
        Objects.requireNonNull(event, "Claimed callback event");
        List<PaymentAuditEntry> audit = auditAdapter.findByPaymentId(event.paymentId()).stream()
                .sorted(Comparator.comparingLong(PaymentAuditEntry::businessVersion)
                        .thenComparingInt(PaymentAuditEntry::eventSequence))
                .toList();
        PaymentAuditEntry current = audit.stream()
                .filter(entry -> entry.eventId().equals(event.eventId()))
                .findFirst().orElseThrow(() ->
                        new IllegalStateException("Outbox event has no matching audit entry"));
        String callbackType = callbackType(current);
        if (callbackType == null) return PaymentCallbackPlan.skip();

        // The payload belongs to this immutable outbox event, not to the current
        // Payment version. Unknown/malformed eligible events must retry/dead-letter,
        // never be silently acknowledged as a successful callback.
        JsonNode original = originalEvent(event, current);
        if (PaymentStatusCallbackMessage.CUT_CREDITED.equals(callbackType)
                && !"COMPLETED".equals(required(original, "outcome"))) {
            return PaymentCallbackPlan.skip();
        }
        if (PaymentStatusCallbackMessage.TREASURY_INTEGRATED.equals(callbackType)
                && !"INTEGRATED".equals(required(original, "tfjStatus"))) {
            return PaymentCallbackPlan.skip();
        }

        Payment payment = paymentRepository.findById(new PaymentId(event.paymentId()))
                .orElseThrow(() -> new IllegalStateException("Missing Payment for callback"));
        PaymentState state = payment.toState();
        PaymentInitiationContext context = state.initiationContext().orElse(null);
        if (context == null) return PaymentCallbackPlan.skip();

        PaymentStatusCallbackMessage message = buildMessage(event, current, state,
                callbackType, original);
        return PaymentCallbackPlan.deliver(new PaymentStatusCallbackDelivery(
                context.callbackEndpoint().value(),
                CorrelationId.of(event.correlationId()),
                UUID.randomUUID(), // transport attempt only, never logical event identity
                event.attemptCount(),
                message
        ));
    }

    private JsonNode originalEvent(ClaimedPaymentOutboxEvent event, PaymentAuditEntry audit) {
        if (!"payment.domain-event".equals(event.eventType())) {
            throw new IllegalStateException("Callback event must be a Payment domain event");
        }
        if (event.payload() == null || event.payload().isBlank()) {
            throw new IllegalStateException("Callback outbox payload is missing");
        }
        try {
            JsonNode root = objectMapper.readTree(event.payload());
            JsonNode metadata = root.path("metadata");
            // Event-level data and durable audit metadata must refer to the same fact.
            if (!event.eventId().toString().equals(required(metadata, "eventId"))
                    || !audit.eventId().equals(event.eventId())
                    || !audit.eventType().equals(requiredEventName(root, audit.eventType()))
                    || !audit.paymentStatus().name().equals(required(metadata, "paymentStatus"))
                    || audit.businessVersion() != metadata.path("aggregateVersion").asLong(-1)
                    || audit.eventSequence() != metadata.path("eventSequence").asInt(-1)
                    || !event.occurredAt().equals(Instant.parse(required(metadata, "occurredAt")))) {
                throw new IllegalStateException("Outbox payload does not match durable Payment audit");
            }
            return root;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot parse immutable Payment callback event", exception);
        }
    }

    private static String requiredEventName(JsonNode root, String auditedName) {
        // The persisted logical outbox type is payment.domain-event; concrete
        // subtype is established by the audit entry and validated by its shape.
        if (PaymentEventOutcomeRecorded.class.getSimpleName().equals(auditedName)) {
            required(root, "outcome");
        } else if (PaymentEndOfDayConfirmationRecorded.class.getSimpleName().equals(auditedName)) {
            required(root, "tfjStatus");
        }
        return auditedName;
    }

    private static String required(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new IllegalStateException("Missing callback event field: " + name);
        }
        return value.asText();
    }

    private static PaymentStatusCallbackMessage buildMessage(
            ClaimedPaymentOutboxEvent event, PaymentAuditEntry current,
            PaymentState state, String callbackType, JsonNode original) {
        Object data;
        if (PaymentStatusCallbackMessage.CUT_CREDITED.equals(callbackType)) {
            PaymentEventOutcomeSnapshot durable = state.paymentEventOutcomeEvidence()
                    .filter(snapshot -> snapshot.outcome() == PaymentEventOutcome.COMPLETED)
                    .orElseThrow(() -> new IllegalStateException(
                            "CUT_CREDITED requires durable COMPLETED T0 evidence"));
            String originalReference = required(original, "principalBankReference");
            Instant originalObservedAt = Instant.parse(required(original, "observedAt"));
            LocalDate originalDco = LocalDate.parse(required(original, "accountingDate"));
            if (!durable.bankPostingReference()
                    .map(ref -> originalReference.equals(ref.principalPostingReference()))
                    .orElse(false)
                    || !durable.observedAt().equals(originalObservedAt)
                    || !durable.accountingDate().equals(originalDco)) {
                throw new IllegalStateException("Original T0 callback conflicts with durable outcome evidence");
            }
            data = new PaymentStatusCallbackMessage.CutCreditedData(
                    new PaymentStatusCallbackMessage.MoneyData(
                            state.requestedAmount().amount().stripTrailingZeros().toPlainString(),
                            state.requestedAmount().currency().getCurrencyCode()),
                    originalReference,
                    originalObservedAt,
                    "POSTED_PENDING_TFJ",
                    false);
        } else {
            EndOfDayConfirmationSnapshot durable = state.endOfDayConfirmationEvidence()
                    .filter(snapshot -> snapshot.tfjStatus() == TfjStatus.INTEGRATED)
                    .orElseThrow(() -> new IllegalStateException(
                            "TREASURY_INTEGRATED requires durable integrated TFJ evidence"));
            // Do NOT test the latest Payment status: newer transitions must not
            // change or suppress an already committed TFJ callback.
            String originalRef = required(original, "principalPostingReference");
            LocalDate businessDate = LocalDate.parse(required(original, "businessDate"));
            Instant matchedAt = Instant.parse(required(original, "matchedAt"));
            if (!originalRef.equals(durable.principalBankPostingReference())
                    || !businessDate.equals(durable.businessDate())
                    || !matchedAt.equals(durable.matchedAt())) {
                throw new IllegalStateException("Original TFJ callback conflicts with durable TFJ evidence");
            }
            data = new PaymentStatusCallbackMessage.TreasuryIntegratedData(
                    businessDate,
                    originalRef,
                    durable.confirmationId().value(),
                    durable.tfjBatchReference().orElse(null),
                    matchedAt);
        }
        return new PaymentStatusCallbackMessage(
                "1.0", event.eventId(), callbackType, event.occurredAt(),
                UUID.fromString(event.correlationId()), current.causationId(),
                event.paymentId(), state.publicPaymentReference().value(),
                state.externalPaymentReference().value(), state.financialInstitutionCode().value(),
                current.businessVersion(), data);
    }

    private static String callbackType(PaymentAuditEntry current) {
        if (current.paymentStatus() == PaymentStatus.TREASURY_INTEGRATED) {
            return PaymentEndOfDayConfirmationRecorded.class.getSimpleName()
                    .equals(current.eventType())
                    ? PaymentStatusCallbackMessage.TREASURY_INTEGRATED
                    : null;
        }

        if (current.paymentStatus() != PaymentStatus.POSTED_PENDING_TFJ) {
            return null;
        }

        return PaymentEventOutcomeRecorded.class.getSimpleName().equals(current.eventType())
                ? PaymentStatusCallbackMessage.CUT_CREDITED
                : null;
    }

}
