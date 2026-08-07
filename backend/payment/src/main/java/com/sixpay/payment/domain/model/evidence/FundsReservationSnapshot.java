package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class FundsReservationSnapshot
        implements ValueObject {

    private final FundsReservationOutcome outcome;
    private final FundsReservationReference reservationReference;
    private final Money reservedAmount;
    private final String accountBindingFingerprint;
    private final Instant expiresAt;
    private final FailureCode reasonCode;
    private final EvidenceMetadata metadata;

    public FundsReservationSnapshot(
            FundsReservationOutcome outcome,
            FundsReservationReference reservationReference,
            Money reservedAmount,
            String accountBindingFingerprint,
            Instant expiresAt,
            FailureCode reasonCode,
            EvidenceMetadata metadata
    ) {
        this.outcome = EvidenceValueObjectRules.requireNonNull(
                outcome,
                "Funds reservation outcome"
        );
        this.reservedAmount =
                EvidenceValueObjectRules.requireNonNull(
                        reservedAmount,
                        "Reserved amount"
                );
        if (!reservedAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "Reserved amount must be positive"
            );
        }
        this.accountBindingFingerprint =
                EvidenceValueObjectRules
                        .requireAccountBindingFingerprint(
                                accountBindingFingerprint
                        );
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Funds reservation metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE) {
            throw new IllegalArgumentException(
                    "Funds reservation source must be AMPLITUDE"
            );
        }

        this.reservationReference = reservationReference;
        this.expiresAt = expiresAt;
        this.reasonCode = reasonCode;

        validateState();
    }

    private void validateState() {
        switch (outcome) {
            case RESERVED -> {
                if (reservationReference == null) {
                    throw new IllegalArgumentException(
                            "Reserved funds require a reservation reference"
                    );
                }
                if (expiresAt == null) {
                    throw new IllegalArgumentException(
                            "Reserved funds require an expiry instant"
                    );
                }
                EvidenceValueObjectRules.requireNotBefore(
                        expiresAt,
                        metadata.acceptedAt(),
                        "Reservation expiry must not precede acceptance"
                );
                if (reasonCode != null) {
                    throw new IllegalArgumentException(
                            "Reserved funds must not expose a failure code"
                    );
                }
            }
            case REJECTED -> {
                if (reservationReference != null
                        || expiresAt != null
                        || reasonCode == null) {
                    throw new IllegalArgumentException(
                            "Rejected reservation requires only a reason code"
                    );
                }
            }
            case UNKNOWN -> {
                if (expiresAt != null) {
                    throw new IllegalArgumentException(
                            "Unknown reservation must not expose an expiry"
                    );
                }
            }
        }
    }

    public FundsReservationOutcome outcome() {
        return outcome;
    }

    public Optional<FundsReservationReference>
            reservationReferenceOptional() {
        return Optional.ofNullable(reservationReference);
    }

    public Money reservedAmount() {
        return reservedAmount;
    }

    public String accountBindingFingerprint() {
        return accountBindingFingerprint;
    }

    public Optional<Instant> expiresAtOptional() {
        return Optional.ofNullable(expiresAt);
    }

    public Optional<FailureCode> reasonCodeOptional() {
        return Optional.ofNullable(reasonCode);
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FundsReservationSnapshot that)) {
            return false;
        }
        return outcome == that.outcome
                && Objects.equals(
                        reservationReference,
                        that.reservationReference
                )
                && reservedAmount.equals(that.reservedAmount)
                && accountBindingFingerprint.equals(
                        that.accountBindingFingerprint
                )
                && Objects.equals(expiresAt, that.expiresAt)
                && Objects.equals(reasonCode, that.reasonCode)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                outcome,
                reservationReference,
                reservedAmount,
                accountBindingFingerprint,
                expiresAt,
                reasonCode,
                metadata
        );
    }
}
