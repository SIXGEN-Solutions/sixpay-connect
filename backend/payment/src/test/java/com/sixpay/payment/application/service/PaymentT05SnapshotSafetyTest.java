package com.sixpay.payment.application.service;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.financial.*;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentT05SnapshotSafetyTest {
    private static final Instant AT = Instant.parse("2026-09-06T18:00:00Z");
    private static final PaymentId ID = new PaymentId(UUID.fromString("bbbbb000-bbbb-4bbb-8bbb-bbbbbbbbbbbb"));
    private static final PublicPaymentReference REF = PublicPaymentReference.of("PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV");
    private static final FinancialInstitutionCode BANK = FinancialInstitutionCode.of("LRB");
    private static final Money AMOUNT = Money.of(new BigDecimal("1000.00"), "XAF");

    @Test
    void finalizedSnapshotIsReusedWithoutNewFinancialEntries() {
        Fixture f = fixture();
        PaymentFinancialEventSnapshot good = snapshot(true);
        when(f.repository.findByPaymentId(ID)).thenReturn(Optional.of(good));
        assertSame(good, f.service.getOrCreateFinalized(f.payment, f.treasury, AT));
        verify(f.repository, never()).save(any());
        verifyNoInteractions(f.ids);
    }

    @Test
    void rejectsIncompletePreviouslyFinalizedSnapshotWithoutWriting() {
        Fixture f = fixture();
        when(f.repository.findByPaymentId(ID)).thenReturn(Optional.of(snapshot(false)));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> f.service.getOrCreateFinalized(f.payment, f.treasury, AT));
        assertTrue(error.getMessage().contains("invalid debit/credit legs"));
        verify(f.repository, never()).save(any());
        verifyNoInteractions(f.ids);
    }

    private static PaymentFinancialEventSnapshot snapshot(boolean complete) {
        var entries = new java.util.ArrayList<PaymentFinancialEntrySnapshot>();
        entries.add(new PaymentFinancialEntrySnapshot(
                UUID.fromString("11111111-1111-4111-8111-111111111111"), 1,
                FinancialEntryDirection.DEBIT, "vault:debtor:0001", AMOUNT, AT));
        if (complete) entries.add(new PaymentFinancialEntrySnapshot(
                UUID.fromString("22222222-2222-4222-8222-222222222222"), 2,
                FinancialEntryDirection.CREDIT, "vault:treasury:0001", AMOUNT, AT));
        return PaymentFinancialEventSnapshot.reconstitute(
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                ID, REF, BANK, "vault:debtor:0001", "vault:treasury:0001",
                AMOUNT, "v1", FinancialSnapshotStatus.FINALIZED, AT, AT, entries);
    }

    @SuppressWarnings("unchecked")
    private static Fixture fixture() {
        PaymentFinancialSnapshotRepository repository = mock(PaymentFinancialSnapshotRepository.class);
        IdentifierGenerator<UUID> ids = mock(IdentifierGenerator.class);
        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        DebtorAccountReference debtor = mock(DebtorAccountReference.class);
        TreasuryAccountReference treasury = mock(TreasuryAccountReference.class);
        when(payment.id()).thenReturn(ID);
        when(payment.publicPaymentReference()).thenReturn(REF);
        when(payment.toState()).thenReturn(state);
        when(state.financialInstitutionCode()).thenReturn(BANK);
        when(state.requestedAmount()).thenReturn(AMOUNT);
        when(state.debtorAccountReference()).thenReturn(debtor);
        when(debtor.integrationAccountToken()).thenReturn("vault:debtor:0001");
        when(treasury.accountToken()).thenReturn("vault:treasury:0001");
        return new Fixture(new PaymentT0SnapshotService(repository, ids), repository, ids, payment, treasury);
    }

    private record Fixture(PaymentT0SnapshotService service,
            PaymentFinancialSnapshotRepository repository,
            IdentifierGenerator<UUID> ids, Payment payment, TreasuryAccountReference treasury) { }
}
