package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ObservedCustomerLink {

    private static final int MAX_ACTOR_LENGTH = 200;
    private static final int MAX_CORRELATION_LENGTH = 150;
    private static final int MAX_REASON_LENGTH = 500;

    private final UUID observedCustomerId;
    private CustomerId customerId;
    private ObservedCustomerLinkStatus status;

    private String linkedBy;
    private String linkCorrelationId;
    private String linkReason;
    private Instant linkedAt;

    private String unlinkedBy;
    private String unlinkCorrelationId;
    private String unlinkReason;
    private Instant unlinkedAt;

    private ObservedCustomerLink(
            UUID observedCustomerId,
            CustomerId customerId,
            ObservedCustomerLinkStatus status,
            String linkedBy,
            String linkCorrelationId,
            String linkReason,
            Instant linkedAt,
            String unlinkedBy,
            String unlinkCorrelationId,
            String unlinkReason,
            Instant unlinkedAt
    ) {
        this.observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );
        this.customerId = Objects.requireNonNull(
                customerId,
                "customerId is required"
        );
        this.status = Objects.requireNonNull(
                status,
                "status is required"
        );
        this.linkedBy = requireText(
                linkedBy, "linkedBy", MAX_ACTOR_LENGTH
        );
        this.linkCorrelationId = requireText(
                linkCorrelationId,
                "linkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        this.linkReason = requireText(
                linkReason, "linkReason", MAX_REASON_LENGTH
        );
        this.linkedAt = Objects.requireNonNull(
                linkedAt,
                "linkedAt is required"
        );

        this.unlinkedBy = normalizeText(
                unlinkedBy, "unlinkedBy", MAX_ACTOR_LENGTH
        );
        this.unlinkCorrelationId = normalizeText(
                unlinkCorrelationId,
                "unlinkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        this.unlinkReason = normalizeText(
                unlinkReason,
                "unlinkReason",
                MAX_REASON_LENGTH
        );
        this.unlinkedAt = unlinkedAt;

        validateState();
    }

    public static ObservedCustomerLink create(
            UUID observedCustomerId,
            CustomerId customerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        return new ObservedCustomerLink(
                observedCustomerId,
                customerId,
                ObservedCustomerLinkStatus.LINKED,
                actorId,
                correlationId,
                reason,
                Objects.requireNonNull(now, "now is required"),
                null,
                null,
                null,
                null
        );
    }

    public static ObservedCustomerLink reconstitute(
            UUID observedCustomerId,
            CustomerId customerId,
            ObservedCustomerLinkStatus status,
            String linkedBy,
            String linkCorrelationId,
            String linkReason,
            Instant linkedAt,
            String unlinkedBy,
            String unlinkCorrelationId,
            String unlinkReason,
            Instant unlinkedAt
    ) {
        return new ObservedCustomerLink(
                observedCustomerId,
                customerId,
                status,
                linkedBy,
                linkCorrelationId,
                linkReason,
                linkedAt,
                unlinkedBy,
                unlinkCorrelationId,
                unlinkReason,
                unlinkedAt
        );
    }

    public void unlink(
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        if (status != ObservedCustomerLinkStatus.LINKED) {
            throw new CustomerDomainException(
                    "observed customer link is already unlinked"
            );
        }

        requireChronology(now);

        status = ObservedCustomerLinkStatus.UNLINKED;
        unlinkedBy = requireText(
                actorId, "unlinkedBy", MAX_ACTOR_LENGTH
        );
        unlinkCorrelationId = requireText(
                correlationId,
                "unlinkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        unlinkReason = requireText(
                reason, "unlinkReason", MAX_REASON_LENGTH
        );
        unlinkedAt = now;
    }

    public void relink(
            CustomerId targetCustomerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        if (status != ObservedCustomerLinkStatus.UNLINKED) {
            throw new CustomerDomainException(
                    "observed customer is already linked"
            );
        }

        requireChronology(now);

        customerId = Objects.requireNonNull(
                targetCustomerId,
                "targetCustomerId is required"
        );
        status = ObservedCustomerLinkStatus.LINKED;
        linkedBy = requireText(
                actorId, "linkedBy", MAX_ACTOR_LENGTH
        );
        linkCorrelationId = requireText(
                correlationId,
                "linkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        linkReason = requireText(
                reason, "linkReason", MAX_REASON_LENGTH
        );
        linkedAt = now;

        unlinkedBy = null;
        unlinkCorrelationId = null;
        unlinkReason = null;
        unlinkedAt = null;
    }

    public boolean isLinked() {
        return status == ObservedCustomerLinkStatus.LINKED;
    }

    private void requireChronology(Instant now) {
        Objects.requireNonNull(now, "now is required");
        Instant latest = unlinkedAt == null ? linkedAt : unlinkedAt;

        if (now.isBefore(latest)) {
            throw new CustomerDomainException(
                    "link operation time must not precede previous link state"
            );
        }
    }

    private void validateState() {
        if (status == ObservedCustomerLinkStatus.LINKED) {
            if (unlinkedBy != null
                    || unlinkCorrelationId != null
                    || unlinkReason != null
                    || unlinkedAt != null) {
                throw new CustomerDomainException(
                        "LINKED correlation must not contain unlink metadata"
                );
            }
            return;
        }

        if (unlinkedBy == null
                || unlinkCorrelationId == null
                || unlinkReason == null
                || unlinkedAt == null) {
            throw new CustomerDomainException(
                    "UNLINKED correlation requires unlink metadata"
            );
        }

        if (unlinkedAt.isBefore(linkedAt)) {
            throw new CustomerDomainException(
                    "unlinkedAt must not precede linkedAt"
            );
        }
    }

    private static String requireText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new CustomerDomainException(
                    field + " is required"
            );
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new CustomerDomainException(
                    field + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }

    private static String normalizeText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return requireText(value, field, maxLength);
    }

    public UUID observedCustomerId() {
        return observedCustomerId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public ObservedCustomerLinkStatus status() {
        return status;
    }

    public String linkedBy() {
        return linkedBy;
    }

    public String linkCorrelationId() {
        return linkCorrelationId;
    }

    public String linkReason() {
        return linkReason;
    }

    public Instant linkedAt() {
        return linkedAt;
    }

    public Optional<String> unlinkedBy() {
        return Optional.ofNullable(unlinkedBy);
    }

    public Optional<String> unlinkCorrelationId() {
        return Optional.ofNullable(unlinkCorrelationId);
    }

    public Optional<String> unlinkReason() {
        return Optional.ofNullable(unlinkReason);
    }

    public Optional<Instant> unlinkedAt() {
        return Optional.ofNullable(unlinkedAt);
    }
}
