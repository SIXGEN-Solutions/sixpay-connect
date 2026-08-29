package com.sixpay.payment.infrastructure.idempotency;

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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "payment_idempotency",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_idempotency_operation_key",
                columnNames = {
                        "operation",
                        "idempotency_key"
                }
        ),
        indexes = {
                @Index(
                        name = "idx_payment_idempotency_status_updated",
                        columnList = "status, updated_at"
                ),
                @Index(
                        name = "idx_payment_idempotency_payment",
                        columnList = "payment_id"
                )
        }
)
public class PaymentIdempotencyEntity {

    public enum Status {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "operation",
            nullable = false,
            updatable = false,
            length = 160
    )
    private String operation;

    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false,
            length = 150
    )
    private String idempotencyKey;

    @Column(
            name = "request_hash",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 16
    )
    private Status status;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(
            name = "response_status",
            length = 64
    )
    private String responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "response_payload",
            columnDefinition = "jsonb"
    )
    private String responsePayload;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(
            name = "persistence_version",
            nullable = false
    )
    private long persistenceVersion;

    protected PaymentIdempotencyEntity() {
    }

    static PaymentIdempotencyEntity start(
            String operation,
            String idempotencyKey,
            String requestHash,
            Instant startedAt
    ) {
        PaymentIdempotencyEntity entity =
                new PaymentIdempotencyEntity();

        entity.id = UUID.randomUUID();
        entity.operation = requireText(
                operation,
                160,
                "Idempotency operation"
        );
        entity.idempotencyKey = requireText(
                idempotencyKey,
                150,
                "Idempotency key"
        );
        entity.requestHash = requireHash(requestHash);
        entity.status = Status.IN_PROGRESS;
        entity.createdAt = Objects.requireNonNull(
                startedAt,
                "Idempotency start instant"
        );
        entity.updatedAt = startedAt;

        return entity;
    }

    void restart(Instant restartedAt) {
        if (status != Status.FAILED) {
            throw new IllegalStateException(
                    "Only a failed idempotency record can restart"
            );
        }

        status = Status.IN_PROGRESS;
        paymentId = null;
        responseStatus = null;
        responsePayload = null;
        failureReason = null;
        completedAt = null;
        updatedAt = Objects.requireNonNull(
                restartedAt,
                "Idempotency restart instant"
        );
    }

    void complete(
            UUID paymentId,
            String responseStatus,
            String responsePayload,
            Instant completedAt
    ) {
        requireInProgress();

        this.paymentId = Objects.requireNonNull(
                paymentId,
                "Completed Payment ID"
        );
        this.responseStatus = requireText(
                responseStatus,
                64,
                "Idempotency response status"
        );
        this.responsePayload = requireJsonPayload(
                responsePayload
        );
        this.completedAt = Objects.requireNonNull(
                completedAt,
                "Idempotency completion instant"
        );
        this.updatedAt = completedAt;
        this.failureReason = null;
        this.status = Status.COMPLETED;
    }

    void fail(
            String failureReason,
            Instant failedAt
    ) {
        requireInProgress();

        this.failureReason = normalizeFailureReason(
                failureReason
        );
        this.updatedAt = Objects.requireNonNull(
                failedAt,
                "Idempotency failure instant"
        );
        this.status = Status.FAILED;
    }

    private void requireInProgress() {
        if (status != Status.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Idempotency record is not input progress"
            );
        }
    }

    private static String requireHash(String value) {
        if (value == null
                || !value.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Request hash must be a lowercase SHA-256 value"
            );
        }
        return value;
    }

    private static String requireJsonPayload(String value) {
        return requireText(
                value,
                Integer.MAX_VALUE,
                "Idempotency response payload"
        );
    }

    private static String requireText(
            String value,
            int maximumLength,
            String label
    ) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    label + " must be non-blank and at most "
                            + maximumLength + " characters"
            );
        }
        return value;
    }

    private static String normalizeFailureReason(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "payment processing failed";
        }
        return value.length() <= 1000
                ? value
                : value.substring(0, 1000);
    }

    public UUID id() {
        return id;
    }

    public String operation() {
        return operation;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String requestHash() {
        return requestHash;
    }

    public Status status() {
        return status;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public String responseStatus() {
        return responseStatus;
    }

    public String responsePayload() {
        return responsePayload;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public long persistenceVersion() {
        return persistenceVersion;
    }
}
