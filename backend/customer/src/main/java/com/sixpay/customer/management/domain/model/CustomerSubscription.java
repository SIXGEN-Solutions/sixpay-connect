package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.sharedkernel.domain.model.AggregateRoot;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CustomerSubscription
        extends AggregateRoot<CustomerSubscriptionId> {

    private static final int MAX_REASON_LENGTH = 500;

    private final CustomerId customerId;
    private final UUID partnerId;
    private final CustomerBankAccountId bankAccountId;
    private final Instant createdAt;

    private CustomerSubscriptionStatus status;
    private String statusReason;
    private Instant activatedAt;
    private Instant updatedAt;
    private Instant closedAt;

    private CustomerSubscription(
            CustomerSubscriptionId id,
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            CustomerSubscriptionStatus status,
            String statusReason,
            Instant createdAt,
            Instant activatedAt,
            Instant updatedAt,
            Instant closedAt
    ) {
        super(id);
        this.customerId = Objects.requireNonNull(
                customerId,
                "customerId is required"
        );
        this.partnerId = Objects.requireNonNull(
                partnerId,
                "partnerId is required"
        );
        this.bankAccountId = Objects.requireNonNull(
                bankAccountId,
                "bankAccountId is required"
        );
        this.status = Objects.requireNonNull(
                status,
                "status is required"
        );
        this.statusReason = normalizeReason(statusReason);
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt is required"
        );
        this.activatedAt = activatedAt;
        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );
        this.closedAt = closedAt;

        validateTimeline();
        validateStatusState();
    }

    public static CustomerSubscription create(
            CustomerSubscriptionId id,
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now is required");

        return new CustomerSubscription(
                id,
                customerId,
                partnerId,
                bankAccountId,
                CustomerSubscriptionStatus.PENDING_ACTIVATION,
                null,
                now,
                null,
                now,
                null
        );
    }

    public static CustomerSubscription reconstitute(
            CustomerSubscriptionId id,
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            CustomerSubscriptionStatus status,
            String statusReason,
            Instant createdAt,
            Instant activatedAt,
            Instant updatedAt,
            Instant closedAt
    ) {
        return new CustomerSubscription(
                id,
                customerId,
                partnerId,
                bankAccountId,
                status,
                statusReason,
                createdAt,
                activatedAt,
                updatedAt,
                closedAt
        );
    }

    public void activate(Instant now) {
        requireTime(now);

        if (status != CustomerSubscriptionStatus.PENDING_ACTIVATION
                && status != CustomerSubscriptionStatus.SUSPENDED) {
            throw new CustomerDomainException(
                    "cannot activate subscription in status " + status
            );
        }

        status = CustomerSubscriptionStatus.ACTIVE;
        statusReason = null;

        if (activatedAt == null) {
            activatedAt = now;
        }

        updatedAt = now;
    }

    public void suspend(String reason, Instant now) {
        requireStatus(
                CustomerSubscriptionStatus.ACTIVE,
                "suspend"
        );
        requireTime(now);

        status = CustomerSubscriptionStatus.SUSPENDED;
        statusReason = requireReason(reason);
        updatedAt = now;
    }

    public void close(String reason, Instant now) {
        if (status == CustomerSubscriptionStatus.CLOSED) {
            throw new CustomerDomainException(
                    "cannot close subscription already in status CLOSED"
            );
        }

        requireTime(now);

        status = CustomerSubscriptionStatus.CLOSED;
        statusReason = requireReason(reason);
        updatedAt = now;
        closedAt = now;
    }

    public boolean acceptsPayments() {
        return status == CustomerSubscriptionStatus.ACTIVE;
    }

    private void requireStatus(
            CustomerSubscriptionStatus expected,
            String operation
    ) {
        if (status != expected) {
            throw new CustomerDomainException(
                    "cannot "
                            + operation
                            + " subscription in status "
                            + status
                            + "; expected "
                            + expected
            );
        }
    }

    private void requireTime(Instant now) {
        Objects.requireNonNull(now, "now is required");

        if (now.isBefore(updatedAt)) {
            throw new CustomerDomainException(
                    "operation time must not precede updatedAt"
            );
        }
    }

    private void validateTimeline() {
        if (updatedAt.isBefore(createdAt)) {
            throw new CustomerDomainException(
                    "updatedAt must not precede createdAt"
            );
        }

        if (activatedAt != null
                && activatedAt.isBefore(createdAt)) {
            throw new CustomerDomainException(
                    "activatedAt must not precede createdAt"
            );
        }

        if (closedAt != null
                && closedAt.isBefore(createdAt)) {
            throw new CustomerDomainException(
                    "closedAt must not precede createdAt"
            );
        }
    }

    private void validateStatusState() {
        if (status == CustomerSubscriptionStatus.PENDING_ACTIVATION
                && (statusReason != null
                || activatedAt != null
                || closedAt != null)) {
            throw new CustomerDomainException(
                    "pending subscription has invalid lifecycle state"
            );
        }

        if (status == CustomerSubscriptionStatus.ACTIVE
                && statusReason != null) {
            throw new CustomerDomainException(
                    "ACTIVE subscription must not have a status reason"
            );
        }

        if (status == CustomerSubscriptionStatus.SUSPENDED
                && statusReason == null) {
            throw new CustomerDomainException(
                    "SUSPENDED subscription requires a reason"
            );
        }

        if (status == CustomerSubscriptionStatus.CLOSED
                && (statusReason == null || closedAt == null)) {
            throw new CustomerDomainException(
                    "CLOSED subscription requires reason and closedAt"
            );
        }
    }

    private static String requireReason(String value) {
        String normalized = normalizeReason(value);
        if (normalized == null) {
            throw new CustomerDomainException(
                    "a reason is required"
            );
        }
        return normalized;
    }

    private static String normalizeReason(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new CustomerDomainException(
                    "reason must not exceed "
                            + MAX_REASON_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public UUID partnerId() {
        return partnerId;
    }

    public CustomerBankAccountId bankAccountId() {
        return bankAccountId;
    }

    public CustomerSubscriptionStatus status() {
        return status;
    }

    public Optional<String> statusReason() {
        return Optional.ofNullable(statusReason);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> activatedAt() {
        return Optional.ofNullable(activatedAt);
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }
}
