package com.sixpay.accounting.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TfjOperationalSnapshotTest {

    @Test
    void mapsUnmatchedConfirmationToQuarantineCategory() {
        var confirmation = confirmation(
                TfjStatus.PENDING,
                TfjMatchStatus.UNMATCHED,
                null,
                null
        );

        var snapshot = TfjOperationalSnapshot.from(confirmation);

        assertThat(snapshot.category())
                .isEqualTo(TfjOperationalCategory.QUARANTINED_UNMATCHED);
        assertThat(snapshot.matchedPaymentId()).isNull();
    }

    @Test
    void mapsAmbiguousConfirmationToQuarantineCategory() {
        var confirmation = confirmation(
                TfjStatus.PENDING,
                TfjMatchStatus.AMBIGUOUS,
                null,
                null
        );

        var snapshot = TfjOperationalSnapshot.from(confirmation);

        assertThat(snapshot.category())
                .isEqualTo(TfjOperationalCategory.QUARANTINED_AMBIGUOUS);
    }

    @Test
    void mapsTerminalUnpublishedConfirmationToFinalityPending() {
        var confirmation = confirmation(
                TfjStatus.INTEGRATED,
                TfjMatchStatus.MATCHED,
                null,
                UUID.fromString("22222222-2222-4222-8222-222222222222")
        );

        var snapshot = TfjOperationalSnapshot.from(confirmation);

        assertThat(snapshot.category())
                .isEqualTo(
                        TfjOperationalCategory.FINALITY_PUBLICATION_PENDING
                );
    }

    @Test
    void mapsIntegratedPublishedConfirmationToCompleted() {
        var confirmation = confirmation(
                TfjStatus.INTEGRATED,
                TfjMatchStatus.MATCHED,
                Instant.parse("2026-09-11T23:30:00Z"),
                UUID.fromString("22222222-2222-4222-8222-222222222222")
        );

        var snapshot = TfjOperationalSnapshot.from(confirmation);

        assertThat(snapshot.category())
                .isEqualTo(TfjOperationalCategory.COMPLETED);
    }

    private static TfjConfirmation confirmation(
            TfjStatus status,
            TfjMatchStatus matchStatus,
            Instant finalityPublishedAt,
            UUID matchedPaymentId
    ) {
        return new TfjConfirmation(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "idem-key-001",
                "payload-hash-001",
                "LAREGIONALE",
                LocalDate.of(2026, 9, 11),
                "PAY-001",
                "BANK-POST-001",
                "TFJ-BATCH-001",
                status,
                Instant.parse("2026-09-11T23:00:00Z"),
                null,
                null,
                null,
                Instant.parse("2026-09-11T23:05:00Z"),
                TfjObservationChannel.ASYNC_CALLBACK,
                "corr-001",
                matchStatus,
                matchedPaymentId,
                finalityPublishedAt
        );
    }
}
