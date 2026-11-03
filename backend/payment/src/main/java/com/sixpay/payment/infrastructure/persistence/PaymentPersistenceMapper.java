package com.sixpay.payment.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckEvidence;
import com.sixpay.payment.domain.model.evidence.BankingVerificationId;
import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;
import java.util.List;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentState;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Maps complete Payment aggregates to the persistence representation.
 *
 * <p>A dedicated ObjectMapper is rebuilt because the Payment domain contains
 * immutable value objects with field-based state and constructor validation.
 * No global application ObjectMapper setting is modified.</p>
 */
@Component
public final class PaymentPersistenceMapper {

    private final ObjectMapper persistenceObjectMapper;

    public PaymentPersistenceMapper(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "Object mapper");

        persistenceObjectMapper = objectMapper.rebuild()
                // Infrastructure-only creator mapping for an immutable domain
                // snapshot. Keep Jackson annotations out of the Payment domain.
                .addMixIn(BankingVerificationSnapshot.class,
                        BankingVerificationSnapshotMixin.class)
                .changeDefaultVisibility(visibility -> visibility
                        .withVisibility(
                                PropertyAccessor.ALL,
                                JsonAutoDetect.Visibility.NONE
                        )
                        .withVisibility(
                                PropertyAccessor.FIELD,
                                JsonAutoDetect.Visibility.ANY
                        )
                        .withVisibility(
                                PropertyAccessor.CREATOR,
                                JsonAutoDetect.Visibility.ANY
                        )
                )
                .build();
    }

    PaymentJpaEntity toNewEntity(Payment payment) {
        PaymentState state = requirePayment(payment).toState();

        return PaymentJpaEntity.create(
                state,
                serialize(state)
        );
    }

    void synchronize(
            PaymentJpaEntity entity,
            Payment payment
    ) {
        Objects.requireNonNull(entity, "Payment entity");

        PaymentState state = requirePayment(payment).toState();

        entity.synchronize(
                state,
                serialize(state)
        );
    }

    Payment toDomain(PaymentJpaEntity entity) {
        Objects.requireNonNull(entity, "Payment entity");

        try {
            PaymentStateDocument document =
                    persistenceObjectMapper.readValue(
                            entity.statePayload(),
                            PaymentStateDocument.class
                    );

            PaymentState state = document.toState();

            verifyIndexedColumns(entity, state);

            return Payment.reconstitute(state);

        } catch (JacksonException | IllegalArgumentException exception) {
            throw new PaymentPersistenceException(
                    "Cannot reconstitute Payment "
                            + entity.paymentId(),
                    exception
            );
        }
    }

    private String serialize(PaymentState state) {
        try {
            return persistenceObjectMapper.writeValueAsString(
                    PaymentStateDocument.from(state)
            );

        } catch (JacksonException exception) {
            throw new PaymentPersistenceException(
                    "Cannot serialize Payment "
                            + state.paymentId(),
                    exception
            );
        }
    }

    /**
     * The domain offers a legacy five-argument constructor and the canonical
     * seven-argument constructor. Jackson must select the latter explicitly.
     * The persisted names correspond to the existing JSON field names, so no
     * state_payload schema or domain-model change is necessary.
     */
    private abstract static class BankingVerificationSnapshotMixin {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        BankingVerificationSnapshotMixin(
                @JsonProperty("verificationId") BankingVerificationId verificationId,
                @JsonProperty("outcome") BankingVerificationOutcome outcome,
                @JsonProperty("accountBindingFingerprint") String accountBindingFingerprint,
                @JsonProperty("customerReference") String customerReference,
                @JsonProperty("accountReference") String accountReference,
                @JsonProperty("checks") List<BankingVerificationCheckEvidence> checks,
                @JsonProperty("metadata") EvidenceMetadata metadata
        ) {
        }
    }

    private static Payment requirePayment(Payment payment) {
        return Objects.requireNonNull(
                payment,
                "Payment"
        );
    }

    private static void verifyIndexedColumns(
            PaymentJpaEntity entity,
            PaymentState state
    ) {
        if (!entity.paymentId().equals(state.paymentId().value())
                || !entity.publicPaymentReference().equals(
                state.publicPaymentReference().value()
        )
                || entity.source() != state.source()
                || !entity.externalPaymentReference().equals(
                state.externalPaymentReference().value()
        )
                || !Objects.equals(
                entity.partnerIdentifier(),
                state.initiationContext()
                        .map(PaymentInitiationContext::applicationId)
                        .orElse(null)
        )
                || entity.status() != state.status()
                || entity.businessVersion()
                != state.businessVersion()
                || !entity.receivedAt().equals(
                state.receivedAt()
        )
                || !entity.updatedAt().equals(
                state.updatedAt()
        )
                || !Objects.equals(
                entity.finalizedAt(),
                state.finalizedAt().orElse(null)
        )) {

            throw new PaymentPersistenceException(
                    "Payment indexed columns and state payload diverge"
            );
        }
    }
}