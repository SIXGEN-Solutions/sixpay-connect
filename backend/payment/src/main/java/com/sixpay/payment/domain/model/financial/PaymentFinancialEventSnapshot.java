package com.sixpay.payment.domain.model.financial;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Payment-owned aggregate that freezes reduced financial facts required for
 * deterministic T0 provider mapping and later T1 reuse.
 *
 * <p>The aggregate does not reproduce the historical Amplitude
 * bkeve/bkmvti schema.</p>
 */
public final class PaymentFinancialEventSnapshot {

    private final UUID snapshotId;
    private final PaymentId paymentId;
    private final PublicPaymentReference publicPaymentReference;
    private final FinancialInstitutionCode financialInstitutionCode;
    private final String debtorAccountReference;
    private final String creditorAccountReference;
    private final Money requestedAmount;
    private final String snapshotVersion;
    private final Instant createdAt;
    private final List<PaymentFinancialEntrySnapshot> entries;

    private FinancialSnapshotStatus status;
    private Instant finalizedAt;

    private PaymentFinancialEventSnapshot(
            UUID snapshotId,
            PaymentId paymentId,
            PublicPaymentReference publicPaymentReference,
            FinancialInstitutionCode financialInstitutionCode,
            String debtorAccountReference,
            String creditorAccountReference,
            Money requestedAmount,
            String snapshotVersion,
            FinancialSnapshotStatus status,
            Instant createdAt,
            Instant finalizedAt,
            List<PaymentFinancialEntrySnapshot> entries
    ) {
        this.snapshotId = requireUuid(
                snapshotId,
                "Financial-event snapshot ID"
        );
        this.paymentId = Objects.requireNonNull(
                paymentId,
                "Payment ID"
        );
        this.publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        this.financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
        this.debtorAccountReference = requireOpaque(
                debtorAccountReference,
                256,
                "Debtor account reference"
        );
        this.creditorAccountReference = requireOpaque(
                creditorAccountReference,
                256,
                "Creditor account reference"
        );
        this.requestedAmount = Objects.requireNonNull(
                requestedAmount,
                "Requested amount"
        );
        if (!requestedAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "Financial snapshot amount must be positive"
            );
        }
        this.snapshotVersion = requireOpaque(
                snapshotVersion,
                32,
                "Snapshot version"
        );
        this.status = Objects.requireNonNull(
                status,
                "Financial snapshot status"
        );
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Financial snapshot creation instant"
        );
        this.finalizedAt = finalizedAt;
        this.entries = new ArrayList<>(
                Objects.requireNonNull(
                        entries,
                        "Financial snapshot entries"
                )
        );

        validateStructuralState();
    }

    public static PaymentFinancialEventSnapshot draft(
            UUID snapshotId,
            PaymentId paymentId,
            PublicPaymentReference publicPaymentReference,
            FinancialInstitutionCode financialInstitutionCode,
            String debtorAccountReference,
            String creditorAccountReference,
            Money requestedAmount,
            String snapshotVersion,
            Instant createdAt
    ) {
        return new PaymentFinancialEventSnapshot(
                snapshotId,
                paymentId,
                publicPaymentReference,
                financialInstitutionCode,
                debtorAccountReference,
                creditorAccountReference,
                requestedAmount,
                snapshotVersion,
                FinancialSnapshotStatus.DRAFT,
                createdAt,
                null,
                List.of()
        );
    }

    public static PaymentFinancialEventSnapshot reconstitute(
            UUID snapshotId,
            PaymentId paymentId,
            PublicPaymentReference publicPaymentReference,
            FinancialInstitutionCode financialInstitutionCode,
            String debtorAccountReference,
            String creditorAccountReference,
            Money requestedAmount,
            String snapshotVersion,
            FinancialSnapshotStatus status,
            Instant createdAt,
            Instant finalizedAt,
            List<PaymentFinancialEntrySnapshot> entries
    ) {
        return new PaymentFinancialEventSnapshot(
                snapshotId,
                paymentId,
                publicPaymentReference,
                financialInstitutionCode,
                debtorAccountReference,
                creditorAccountReference,
                requestedAmount,
                snapshotVersion,
                status,
                createdAt,
                finalizedAt,
                entries
        );
    }

    public void addEntry(PaymentFinancialEntrySnapshot entry) {
        requireDraft();

        PaymentFinancialEntrySnapshot validated =
                Objects.requireNonNull(
                        entry,
                        "Financial-entry snapshot"
                );

        if (validated.createdAt().isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Financial-entry creation cannot precede "
                            + "event snapshot creation"
            );
        }

        boolean duplicateId = entries.stream().anyMatch(
                existing -> existing.entrySnapshotId()
                        .equals(validated.entrySnapshotId())
        );
        boolean duplicateSequence = entries.stream().anyMatch(
                existing -> existing.sequence()
                        == validated.sequence()
        );

        if (duplicateId || duplicateSequence) {
            throw new IllegalArgumentException(
                    "Financial-entry IDs and sequences "
                            + "must be unique per event snapshot"
            );
        }

        entries.add(validated);
        sortEntries();
    }

    public void finalizeAt(Instant instant) {
        requireDraft();

        Instant validated = Objects.requireNonNull(
                instant,
                "Financial snapshot finalization instant"
        );

        if (entries.isEmpty()) {
            throw new IllegalStateException(
                    "Financial snapshot requires at least "
                            + "one entry before finalization"
            );
        }
        if (validated.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Financial snapshot finalization "
                            + "cannot precede creation"
            );
        }

        status = FinancialSnapshotStatus.FINALIZED;
        finalizedAt = validated;
    }

    private void validateStructuralState() {
        Set<UUID> entryIds = new HashSet<>();
        Set<Integer> sequences = new HashSet<>();

        for (PaymentFinancialEntrySnapshot entry : entries) {
            Objects.requireNonNull(
                    entry,
                    "Financial-entry snapshot"
            );
            if (entry.createdAt().isBefore(createdAt)) {
                throw new IllegalArgumentException(
                        "Financial-entry creation cannot precede "
                                + "event snapshot creation"
                );
            }
            if (!entryIds.add(entry.entrySnapshotId())
                    || !sequences.add(entry.sequence())) {
                throw new IllegalArgumentException(
                        "Financial-entry IDs and sequences "
                                + "must be unique per event snapshot"
                );
            }
        }

        sortEntries();

        if (status == FinancialSnapshotStatus.DRAFT) {
            if (finalizedAt != null) {
                throw new IllegalArgumentException(
                        "Draft financial snapshot must not "
                                + "have finalizedAt"
                );
            }
            return;
        }

        if (entries.isEmpty()) {
            throw new IllegalArgumentException(
                    "Finalized financial snapshot requires entries"
            );
        }
        if (finalizedAt == null
                || finalizedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Finalized financial snapshot requires "
                            + "a valid finalizedAt"
            );
        }
    }

    private void sortEntries() {
        entries.sort(
                (left, right) -> Integer.compare(
                        left.sequence(),
                        right.sequence()
                )
        );
    }

    private void requireDraft() {
        if (status != FinancialSnapshotStatus.DRAFT) {
            throw new IllegalStateException(
                    "Finalized financial snapshot is immutable"
            );
        }
    }

    public boolean sameSnapshot(
            PaymentFinancialEventSnapshot other
    ) {
        Objects.requireNonNull(
                other,
                "Other financial snapshot"
        );

        return snapshotId.equals(other.snapshotId)
                && paymentId.equals(other.paymentId)
                && publicPaymentReference.equals(
                        other.publicPaymentReference
                )
                && financialInstitutionCode.equals(
                        other.financialInstitutionCode
                )
                && debtorAccountReference.equals(
                        other.debtorAccountReference
                )
                && creditorAccountReference.equals(
                        other.creditorAccountReference
                )
                && requestedAmount.equals(other.requestedAmount)
                && snapshotVersion.equals(other.snapshotVersion)
                && status == other.status
                && createdAt.equals(other.createdAt)
                && Objects.equals(
                        finalizedAt,
                        other.finalizedAt
                )
                && entries.equals(other.entries);
    }

    public UUID snapshotId() {
        return snapshotId;
    }

    public PaymentId paymentId() {
        return paymentId;
    }

    public PublicPaymentReference publicPaymentReference() {
        return publicPaymentReference;
    }

    public FinancialInstitutionCode financialInstitutionCode() {
        return financialInstitutionCode;
    }

    public String debtorAccountReference() {
        return debtorAccountReference;
    }

    public String creditorAccountReference() {
        return creditorAccountReference;
    }

    public Money requestedAmount() {
        return requestedAmount;
    }

    public String snapshotVersion() {
        return snapshotVersion;
    }

    public FinancialSnapshotStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> finalizedAt() {
        return Optional.ofNullable(finalizedAt);
    }

    public List<PaymentFinancialEntrySnapshot> entries() {
        return List.copyOf(entries);
    }

    private static UUID requireUuid(
            UUID value,
            String label
    ) {
        UUID validated = Objects.requireNonNull(
                value,
                label
        );
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
        if (value.isBlank()
                || value.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " must contain 1 to "
                            + maxLength + " characters"
            );
        }
        return value;
    }

    @Override
    public String toString() {
        return "PaymentFinancialEventSnapshot[id="
                + snapshotId
                + ", paymentId=" + paymentId
                + ", status=" + status
                + ", entryCount=" + entries.size()
                + "]";
    }
}
