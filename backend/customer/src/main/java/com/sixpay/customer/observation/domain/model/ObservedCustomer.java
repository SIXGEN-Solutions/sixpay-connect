package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ObservedCustomer {

    private final ObservedCustomerId id;
    private final ObservedCustomerIdentity identity;
    private final List<ObservedCustomerInstitution> institutions;
    private final Instant firstObservedAt;
    private final Instant lastObservedAt;
    private final long totalPayments;
    private final long successfulPayments;
    private final long failedPayments;
    private final ObservedPaymentStatus lastPaymentStatus;
    private final String lastFailureReasonCode;
    private final long projectionVersion;
    private final ProjectionWatermark sourceEventWatermark;
    private final Instant updatedAt;

    private ObservedCustomer(
            ObservedCustomerId id,
            ObservedCustomerIdentity identity,
            List<ObservedCustomerInstitution> institutions,
            Instant firstObservedAt,
            Instant lastObservedAt,
            long totalPayments,
            long successfulPayments,
            long failedPayments,
            ObservedPaymentStatus lastPaymentStatus,
            String lastFailureReasonCode,
            long projectionVersion,
            ProjectionWatermark sourceEventWatermark,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "observedCustomerId is required");
        this.identity = Objects.requireNonNull(identity, "identity is required");
        this.institutions = validateInstitutions(institutions);
        this.firstObservedAt = Objects.requireNonNull(
                firstObservedAt,
                "firstObservedAt is required"
        );
        this.lastObservedAt = Objects.requireNonNull(
                lastObservedAt,
                "lastObservedAt is required"
        );

        if (lastObservedAt.isBefore(firstObservedAt)) {
            throw new ObservedCustomerDomainException(
                    "lastObservedAt must not be before firstObservedAt"
            );
        }
        if (totalPayments < 1) {
            throw new ObservedCustomerDomainException(
                    "totalPayments must be at least one"
            );
        }
        if (successfulPayments < 0 || failedPayments < 0) {
            throw new ObservedCustomerDomainException(
                    "payment counters must not be negative"
            );
        }
        if (successfulPayments + failedPayments > totalPayments) {
            throw new ObservedCustomerDomainException(
                    "successfulPayments plus failedPayments "
                            + "must not exceed totalPayments"
            );
        }

        this.totalPayments = totalPayments;
        this.successfulPayments = successfulPayments;
        this.failedPayments = failedPayments;
        this.lastPaymentStatus = Objects.requireNonNull(
                lastPaymentStatus,
                "lastPaymentStatus is required"
        );
        this.lastFailureReasonCode = normalizeFailureReason(
                lastFailureReasonCode
        );

        if (projectionVersion < 1) {
            throw new ObservedCustomerDomainException(
                    "projectionVersion must be at least one"
            );
        }

        this.projectionVersion = projectionVersion;
        this.sourceEventWatermark = Objects.requireNonNull(
                sourceEventWatermark,
                "sourceEventWatermark is required"
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );

        if (updatedAt.isBefore(lastObservedAt)) {
            throw new ObservedCustomerDomainException(
                    "updatedAt must not be before lastObservedAt"
            );
        }

        for (ObservedCustomerInstitution institution : this.institutions) {
            if (institution.firstObservedAt().isBefore(firstObservedAt)
                    || institution.lastObservedAt().isAfter(lastObservedAt)) {
                throw new ObservedCustomerDomainException(
                        "institution observation interval must fit "
                                + "inside customer observation interval"
                );
            }
        }
    }

    public static ObservedCustomer reconstitute(
            ObservedCustomerId id,
            ObservedCustomerIdentity identity,
            List<ObservedCustomerInstitution> institutions,
            Instant firstObservedAt,
            Instant lastObservedAt,
            long totalPayments,
            long successfulPayments,
            long failedPayments,
            ObservedPaymentStatus lastPaymentStatus,
            String lastFailureReasonCode,
            long projectionVersion,
            ProjectionWatermark sourceEventWatermark,
            Instant updatedAt
    ) {
        return new ObservedCustomer(
                id,
                identity,
                institutions,
                firstObservedAt,
                lastObservedAt,
                totalPayments,
                successfulPayments,
                failedPayments,
                lastPaymentStatus,
                lastFailureReasonCode,
                projectionVersion,
                sourceEventWatermark,
                updatedAt
        );
    }

    public ObservedCustomerId id() { return id; }
    public ObservedCustomerIdentity identity() { return identity; }
    public List<ObservedCustomerInstitution> institutions() { return institutions; }
    public Instant firstObservedAt() { return firstObservedAt; }
    public Instant lastObservedAt() { return lastObservedAt; }
    public long totalPayments() { return totalPayments; }
    public long successfulPayments() { return successfulPayments; }
    public long failedPayments() { return failedPayments; }
    public ObservedPaymentStatus lastPaymentStatus() { return lastPaymentStatus; }
    public Optional<String> lastFailureReasonCode() {
        return Optional.ofNullable(lastFailureReasonCode);
    }
    public long projectionVersion() { return projectionVersion; }
    public ProjectionWatermark sourceEventWatermark() {
        return sourceEventWatermark;
    }
    public Instant updatedAt() { return updatedAt; }

    private static List<ObservedCustomerInstitution> validateInstitutions(
            List<ObservedCustomerInstitution> institutions
    ) {
        List<ObservedCustomerInstitution> copy = List.copyOf(
                Objects.requireNonNull(
                        institutions,
                        "institutions are required"
                )
        );

        if (copy.isEmpty() || copy.stream().anyMatch(Objects::isNull)) {
            throw new ObservedCustomerDomainException(
                    "Observed Customer must contain non-null institutions"
            );
        }

        var codes = new HashSet<String>();
        for (ObservedCustomerInstitution institution : copy) {
            if (!codes.add(institution.financialInstitutionCode())) {
                throw new ObservedCustomerDomainException(
                        "financial institution codes must be unique"
                );
            }
        }

        return copy;
    }

    private static String normalizeFailureReason(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip().toUpperCase();

        if (normalized.isEmpty()) {
            return null;
        }
        if (!normalized.matches("^[A-Z0-9][A-Z0-9._-]{0,63}$")) {
            throw new ObservedCustomerDomainException(
                    "lastFailureReasonCode has an invalid format"
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "ObservedCustomer[id=" + id
                + ", identity=[PROTECTED]"
                + ", institutions=" + institutions.size()
                + ", firstObservedAt=" + firstObservedAt
                + ", lastObservedAt=" + lastObservedAt
                + ", totalPayments=" + totalPayments
                + ", successfulPayments=" + successfulPayments
                + ", failedPayments=" + failedPayments
                + ", lastPaymentStatus=" + lastPaymentStatus
                + ", lastFailureReasonCode=" + lastFailureReasonCode
                + ", projectionVersion=" + projectionVersion
                + ", sourceEventWatermark=[PROTECTED]"
                + ", updatedAt=" + updatedAt + "]";
    }
}
