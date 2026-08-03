package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.event.CustomerVerificationCompleted;
import com.sixpay.customer.verification.domain.event.CustomerVerificationDomainEvent;
import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.model.AggregateRoot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight aggregate representing one Customer Verification attempt.
 *
 * <p>The aggregate has exactly two lifecycle states: REQUESTED and COMPLETED.
 * Retry, timeout and processing states belong to application orchestration,
 * not to this domain lifecycle.</p>
 */
public final class CustomerVerification
        extends AggregateRoot<CustomerVerificationId> {

    private final CustomerVerificationRequest request;
    private VerificationStatus status;
    private CustomerVerificationResult result;
    private Instant updatedAt;

    private CustomerVerification(
            CustomerVerificationRequest request,
            VerificationStatus status,
            CustomerVerificationResult result,
            Instant updatedAt
    ) {
        super(
                Objects.requireNonNull(
                        request,
                        "request is required"
                ).verificationId()
        );
        this.request = request;
        this.status = Objects.requireNonNull(
                status,
                "status is required"
        );
        this.result = result;
        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );

        validateState();
    }

    public static CustomerVerification request(
            CustomerVerificationRequest request
    ) {
        Objects.requireNonNull(request, "request is required");

        return new CustomerVerification(
                request,
                VerificationStatus.REQUESTED,
                null,
                request.requestedAt()
        );
    }

    public static CustomerVerification reconstitute(
            CustomerVerificationRequest request,
            VerificationStatus status,
            CustomerVerificationResult result,
            Instant updatedAt
    ) {
        return new CustomerVerification(
                request,
                status,
                result,
                updatedAt
        );
    }

    public CustomerVerificationResult complete(
            VerificationEvidence evidence,
            UUID completionEventId,
            Instant completedAt
    ) {
        requireRequested();
        Objects.requireNonNull(evidence, "evidence is required");
        Objects.requireNonNull(
                completionEventId,
                "completionEventId is required"
        );
        Objects.requireNonNull(completedAt, "completedAt is required");

        if (evidence.observedAt().isBefore(request.requestedAt())) {
            throw new CustomerVerificationDomainException(
                    "Evidence observedAt must not be before requestedAt"
            );
        }
        if (completedAt.isBefore(request.requestedAt())) {
            throw new CustomerVerificationDomainException(
                    "completedAt must not be before requestedAt"
            );
        }
        if (evidence.validUntilOptional()
                .filter(validUntil -> validUntil.isBefore(completedAt))
                .isPresent()) {
            throw new CustomerVerificationDomainException(
                    "Expired evidence cannot complete verification"
            );
        }

        CustomerVerificationResult completedResult =
                CustomerVerificationResult.from(
                        evidence,
                        completedAt
                );

        this.result = completedResult;
        this.status = VerificationStatus.COMPLETED;
        this.updatedAt = completedAt;

        registerDomainEvent(
                new CustomerVerificationCompleted(
                        completionEventId,
                        id(),
                        completedResult.outcome(),
                        completedResult.evidence().checks(),
                        completedResult.evidence().fingerprint(),
                        request.accountBindingFingerprint(),
                        completedAt
                )
        );

        return completedResult;
    }

    public List<CustomerVerificationDomainEvent> pullDomainEvents() {
        return releaseDomainEvents().stream()
                .map(CustomerVerificationDomainEvent.class::cast)
                .toList();
    }

    public CustomerVerificationRequest request() {
        return request;
    }

    public VerificationStatus status() {
        return status;
    }

    public Optional<CustomerVerificationResult> result() {
        return Optional.ofNullable(result);
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void requireRequested() {
        if (status != VerificationStatus.REQUESTED) {
            throw new CustomerVerificationDomainException(
                    "Customer Verification can be completed only once"
            );
        }
    }

    private void validateState() {
        switch (status) {
            case REQUESTED -> {
                if (result != null) {
                    throw new CustomerVerificationDomainException(
                            "REQUESTED verification must not have a result"
                    );
                }
                if (!updatedAt.equals(request.requestedAt())) {
                    throw new CustomerVerificationDomainException(
                            "REQUESTED verification updatedAt must equal "
                                    + "requestedAt"
                    );
                }
            }
            case COMPLETED -> {
                if (result == null) {
                    throw new CustomerVerificationDomainException(
                            "COMPLETED verification requires a result"
                    );
                }
                if (!updatedAt.equals(result.completedAt())) {
                    throw new CustomerVerificationDomainException(
                            "COMPLETED verification updatedAt must equal "
                                    + "result completedAt"
                    );
                }
                if (result.evidence().observedAt()
                        .isBefore(request.requestedAt())) {
                    throw new CustomerVerificationDomainException(
                            "Completed evidence must not predate request"
                    );
                }
            }
        }
    }
}
