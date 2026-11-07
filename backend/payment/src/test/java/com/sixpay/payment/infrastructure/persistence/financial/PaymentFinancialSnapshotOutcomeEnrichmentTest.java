package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.financial.BankingEventDetails;
import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.infrastructure.persistence.PaymentPersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentFinancialSnapshotOutcomeEnrichmentTest {
    @Mock PaymentFinancialEventSnapshotSpringDataRepository events;
    @Mock PaymentFinancialEntrySnapshotSpringDataRepository entries;
    @Mock PaymentFinancialSnapshotPersistenceMapper mapper;
    @Mock PaymentFinancialEventSnapshotJpaEntity entity;
    @Mock PaymentFinancialEventSnapshot snapshot;

    private final PaymentId paymentId =
            new PaymentId(UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"));
    private final UUID snapshotId =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private final LocalDate dco = LocalDate.of(2026, 10, 19);
    private final Instant observedAt = Instant.parse("2026-11-06T03:31:34Z");

    private PaymentFinancialSnapshotPersistenceAdapter adapter() {
        return new PaymentFinancialSnapshotPersistenceAdapter(events, mapper, entries);
    }

    private void existingSnapshot() {
        when(events.findByPaymentId(paymentId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(snapshot);
        when(snapshot.status()).thenReturn(FinancialSnapshotStatus.FINALIZED);
        when(snapshot.entries()).thenReturn(List.of(
                mock(PaymentFinancialEntrySnapshot.class),
                mock(PaymentFinancialEntrySnapshot.class)));
        when(snapshot.bankingDetails()).thenReturn(new BankingEventDetails(
                "787", null, null, null, null, null, null, null, null, null));
    }

    @Test
    void enrichesExistingEventAndBothEntriesWithoutRecreatingSnapshots() {
        existingSnapshot();
        when(snapshot.snapshotId()).thenReturn(snapshotId);
        when(events.enrichCompletedBankingOutcome(
                snapshotId, "000003", dco, LocalDate.of(2026, 11, 6)))
                .thenReturn(1);
        when(entries.enrichAccountingDate(snapshotId, dco)).thenReturn(2);

        adapter().enrichCompletedBankingOutcome(
                paymentId, "787/000003", dco, observedAt);

        verify(events).enrichCompletedBankingOutcome(
                snapshotId, "000003", dco, LocalDate.of(2026, 11, 6));
        verify(entries).enrichAccountingDate(snapshotId, dco);
        verify(events, never()).saveAndFlush(any());
    }

    @Test
    void rejectsConflictingBankOperationReference() {
        existingSnapshot();
        assertThrows(PaymentPersistenceException.class,
                () -> adapter().enrichCompletedBankingOutcome(
                        paymentId, "999/000003", dco, observedAt));
        verify(events, never()).enrichCompletedBankingOutcome(
                any(), any(), any(), any());
    }

    @Test
    void rejectsIncompleteOrConflictingEntryEnrichment() {
        existingSnapshot();
        when(snapshot.snapshotId()).thenReturn(snapshotId);
        when(events.enrichCompletedBankingOutcome(
                snapshotId, "000003", dco, LocalDate.of(2026, 11, 6)))
                .thenReturn(1);
        when(entries.enrichAccountingDate(snapshotId, dco)).thenReturn(1);

        assertThrows(PaymentPersistenceException.class,
                () -> adapter().enrichCompletedBankingOutcome(
                        paymentId, "787/000003", dco, observedAt));
    }
}
