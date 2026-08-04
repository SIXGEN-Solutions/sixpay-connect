package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One framework-free observation applied to an Observed Customer projection.
 */
public record ObservedCustomerObservation(
        UUID sourceEventId,
        ObservedCustomerIdentity identity,
        ObservedCustomerInstitution institution,
        ObservedPaymentReference payment,
        ProjectionWatermark watermark,
        Instant observedAt,
        Instant appliedAt
) implements ValueObject {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObservedCustomerObservation {
        sourceEventId = Objects.requireNonNull(
                sourceEventId,
                "sourceEventId is required"
        );
        if (NIL_UUID.equals(sourceEventId)) {
            throw new ObservedCustomerDomainException(
                    "sourceEventId must not be nil"
            );
        }

        identity = Objects.requireNonNull(
                identity,
                "identity is required"
        );
        institution = Objects.requireNonNull(
                institution,
                "institution is required"
        );
        payment = Objects.requireNonNull(
                payment,
                "payment is required"
        );
        watermark = Objects.requireNonNull(
                watermark,
                "watermark is required"
        );
        observedAt = Objects.requireNonNull(
                observedAt,
                "observedAt is required"
        );
        appliedAt = Objects.requireNonNull(
                appliedAt,
                "appliedAt is required"
        );

        if (appliedAt.isBefore(observedAt)) {
            throw new ObservedCustomerDomainException(
                    "appliedAt must not be before observedAt"
            );
        }

        if (!institution.financialInstitutionCode().equals(
                payment.financialInstitutionCode()
        )) {
            throw new ObservedCustomerDomainException(
                    "institution and payment financial institution "
                            + "codes must match"
            );
        }
    }

    @Override
    public String toString() {
        return "ObservedCustomerObservation[sourceEventId="
                + sourceEventId
                + ", identity=[PROTECTED]"
                + ", institution="
                + institution.financialInstitutionCode()
                + ", paymentId="
                + payment.paymentId()
                + ", watermark=[PROTECTED]"
                + ", observedAt="
                + observedAt
                + ", appliedAt="
                + appliedAt
                + "]";
    }
}
