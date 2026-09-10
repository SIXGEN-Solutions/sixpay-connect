package com.sixpay.payment.domain.model.financial;

import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable reduced financial-entry fact owned by Payment.
 *
 * <p>This is not an Amplitude bkmvti persistence entity.</p>
 */
public final class PaymentFinancialEntrySnapshot {

    private final UUID entrySnapshotId;
    private final int sequence;
    private final FinancialEntryDirection direction;
    private final String accountReference;
    private final Money amount;
    private final Instant createdAt;

    public PaymentFinancialEntrySnapshot(
            UUID entrySnapshotId,
            int sequence,
            FinancialEntryDirection direction,
            String accountReference,
            Money amount,
            Instant createdAt
    ) {
        this.entrySnapshotId = requireUuid(
                entrySnapshotId,
                "Financial-entry snapshot ID"
        );
        if (sequence <= 0) {
            throw new IllegalArgumentException(
                    "Financial-entry sequence must be positive"
            );
        }
        this.sequence = sequence;
        this.direction = Objects.requireNonNull(
                direction,
                "Financial-entry direction"
        );
        this.accountReference = requireOpaque(
                accountReference,
                256,
                "Financial-entry account reference"
        );
        this.amount = Objects.requireNonNull(
                amount,
                "Financial-entry amount"
        );
        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "Financial-entry amount must be positive"
            );
        }
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Financial-entry creation instant"
        );
    }

    public UUID entrySnapshotId() {
        return entrySnapshotId;
    }

    public int sequence() {
        return sequence;
    }

    public FinancialEntryDirection direction() {
        return direction;
    }

    public String accountReference() {
        return accountReference;
    }

    public Money amount() {
        return amount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static UUID requireUuid(UUID value, String label) {
        UUID validated = Objects.requireNonNull(value, label);
        if (validated.getMostSignificantBits() == 0L
                && validated.getLeastSignificantBits() == 0L) {
            throw new IllegalArgumentException(
                    label + " must not be nil"
            );
        }
        return validated;
    }

    private static String requireOpaque(
            String value,
            int maxLength,
            String label
    ) {
        Objects.requireNonNull(value, label);
        if (value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " must contain 1 to "
                            + maxLength + " characters"
            );
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaymentFinancialEntrySnapshot that)) {
            return false;
        }
        return sequence == that.sequence
                && entrySnapshotId.equals(that.entrySnapshotId)
                && direction == that.direction
                && accountReference.equals(that.accountReference)
                && amount.equals(that.amount)
                && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                entrySnapshotId,
                sequence,
                direction,
                accountReference,
                amount,
                createdAt
        );
    }

    @Override
    public String toString() {
        return "PaymentFinancialEntrySnapshot[id="
                + entrySnapshotId
                + ", sequence=" + sequence
                + ", direction=" + direction
                + ", amount=" + amount.amount()
                + " " + amount.currency().getCurrencyCode()
                + "]";
    }
}
