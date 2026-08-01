package com.sixpay.payment.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.sixpay.payment.domain.model.Payment;
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