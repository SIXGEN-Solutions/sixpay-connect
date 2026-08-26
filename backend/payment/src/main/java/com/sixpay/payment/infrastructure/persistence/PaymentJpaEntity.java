package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payments_public_reference",
                        columnNames = "public_payment_reference"
                ),
                @UniqueConstraint(
                        name = "uk_payments_source_external_reference",
                        columnNames = {
                                "payment_source",
                                "external_payment_reference"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_payments_status_updated_at",
                        columnList = "status, updated_at"
                ),
                @Index(
                        name = "idx_payments_subscription_reference",
                        columnList = "external_subscription_reference"
                ),
                @Index(
                        name = "idx_payments_financial_institution",
                        columnList = "financial_institution_code"
                )
        }
)
public class PaymentJpaEntity {

    @Id
    @Column(
            name = "payment_id",
            nullable = false,
            updatable = false
    )
    private UUID paymentId;

    @Column(
            name = "public_payment_reference",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String publicPaymentReference;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_source",
            nullable = false,
            updatable = false,
            length = 32
    )
    private PaymentSource source;

    @Column(
            name = "external_payment_reference",
            nullable = false,
            updatable = false,
            length = 128
    )
    private String externalPaymentReference;

    @Column(
            name = "external_subscription_reference",
            nullable = false,
            updatable = false,
            length = 128
    )
    private String externalSubscriptionReference;

    @Column(
            name = "financial_institution_code",
            nullable = false,
            updatable = false,
            length = 32
    )
    private String financialInstitutionCode;

    @Column(
            name = "requested_amount",
            nullable = false,
            updatable = false,
            precision = 38,
            scale = 18
    )
    private BigDecimal requestedAmount;

    @Column(
            name = "requested_currency",
            nullable = false,
            updatable = false,
            length = 3
    )
    private String requestedCurrency;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 48
    )
    private PaymentStatus status;

    @Column(
            name = "business_version",
            nullable = false
    )
    private long businessVersion;

    @Column(
            name = "received_at",
            nullable = false,
            updatable = false
    )
    private Instant receivedAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "state_payload",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String statePayload;

    @Version
    @Column(
            name = "persistence_version",
            nullable = false
    )
    private long persistenceVersion;

    protected PaymentJpaEntity() {
    }

    static PaymentJpaEntity create(
            PaymentState state,
            String statePayload
    ) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.paymentId = state.paymentId().value();
        entity.publicPaymentReference =
                state.publicPaymentReference().value();
        entity.source = state.source();
        entity.externalPaymentReference =
                state.externalPaymentReference().value();
        entity.externalSubscriptionReference =
                state.externalSubscriptionReference().value();
        entity.financialInstitutionCode =
                state.financialInstitutionCode().value();
        entity.requestedAmount = state.requestedAmount().amount();
        entity.requestedCurrency =
                state.requestedAmount().currency().getCurrencyCode();
        entity.receivedAt = state.receivedAt();
        entity.synchronize(state, statePayload);
        return entity;
    }

    void synchronize(
            PaymentState state,
            String newStatePayload
    ) {
        Objects.requireNonNull(state, "Payment state");
        Objects.requireNonNull(
                newStatePayload,
                "Payment state payload"
        );

        if (!paymentId.equals(state.paymentId().value())) {
            throw new PaymentPersistenceException(
                    "Payment identifier cannot change"
            );
        }
        if (!publicPaymentReference.equals(
                state.publicPaymentReference().value()
        )) {
            throw new PaymentPersistenceException(
                    "Public Payment reference cannot change"
            );
        }
        if (source != state.source()
                || !externalPaymentReference.equals(
                        state.externalPaymentReference().value()
                )) {
            throw new PaymentPersistenceException(
                    "External Payment identity cannot change"
            );
        }
        if (state.businessVersion() < businessVersion) {
            throw new PaymentPersistenceException(
                    "Cannot persist an older Payment business version"
            );
        }
        if (state.businessVersion() == businessVersion
                && statePayload != null
                && !statePayload.equals(newStatePayload)) {
            throw new PaymentPersistenceException(
                    "Same Payment business version has a different state"
            );
        }

        status = state.status();
        businessVersion = state.businessVersion();
        updatedAt = state.updatedAt();
        finalizedAt = state.finalizedAt().orElse(null);
        statePayload = newStatePayload;
    }

    UUID paymentId() {
        return paymentId;
    }

    String publicPaymentReference() {
        return publicPaymentReference;
    }

    PaymentSource source() {
        return source;
    }

    String externalPaymentReference() {
        return externalPaymentReference;
    }

    PaymentStatus status() {
        return status;
    }

    long businessVersion() {
        return businessVersion;
    }

    Instant receivedAt() {
        return receivedAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    Instant finalizedAt() {
        return finalizedAt;
    }

    String statePayload() {
        return statePayload;
    }

    long persistenceVersion() {
        return persistenceVersion;
    }
}
