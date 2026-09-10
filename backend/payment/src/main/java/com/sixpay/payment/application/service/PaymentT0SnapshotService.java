package com.sixpay.payment.application.service;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public final class PaymentT0SnapshotService {

    private static final String SNAPSHOT_VERSION = "v1";

    private final PaymentFinancialSnapshotRepository repository;
    private final IdentifierGenerator<UUID> identifierGenerator;

    public PaymentT0SnapshotService(
            PaymentFinancialSnapshotRepository repository,
            IdentifierGenerator<UUID> identifierGenerator
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.identifierGenerator =
                Objects.requireNonNull(identifierGenerator);
    }

    public PaymentFinancialEventSnapshot getOrCreateFinalized(
            Payment payment,
            TreasuryAccountReference treasuryAccount,
            Instant createdAt
    ) {
        Objects.requireNonNull(payment, "Payment");
        Objects.requireNonNull(treasuryAccount, "Treasury account");
        Objects.requireNonNull(createdAt, "Created at");

        var existing = repository.findByPaymentId(payment.id());
        if (existing.isPresent()) {
            PaymentFinancialEventSnapshot snapshot =
                    existing.orElseThrow();

            validateExisting(
                    snapshot,
                    payment,
                    treasuryAccount
            );

            return snapshot;
        }

        PaymentFinancialEventSnapshot snapshot =
                PaymentFinancialEventSnapshot.draft(
                        identifierGenerator.generate(),
                        payment.id(),
                        payment.publicPaymentReference(),
                        payment.toState().financialInstitutionCode(),
                        payment.toState()
                                .debtorAccountReference()
                                .integrationAccountToken(),
                        treasuryAccount.accountToken(),
                        payment.toState().requestedAmount(),
                        SNAPSHOT_VERSION,
                        createdAt
                );

        snapshot.addEntry(
                new PaymentFinancialEntrySnapshot(
                        identifierGenerator.generate(),
                        1,
                        FinancialEntryDirection.DEBIT,
                        payment.toState()
                                .debtorAccountReference()
                                .integrationAccountToken(),
                        payment.toState().requestedAmount(),
                        createdAt
                )
        );

        snapshot.addEntry(
                new PaymentFinancialEntrySnapshot(
                        identifierGenerator.generate(),
                        2,
                        FinancialEntryDirection.CREDIT,
                        treasuryAccount.accountToken(),
                        payment.toState().requestedAmount(),
                        createdAt
                )
        );

        snapshot.finalizeAt(createdAt);

        return repository.save(snapshot);
    }

    private static void validateExisting(
            PaymentFinancialEventSnapshot snapshot,
            Payment payment,
            TreasuryAccountReference treasuryAccount
    ) {
        if (!snapshot.paymentId().equals(payment.id())
                || !snapshot.publicPaymentReference().equals(
                        payment.publicPaymentReference()
                )
                || !snapshot.financialInstitutionCode().equals(
                        payment.toState().financialInstitutionCode()
                )
                || !snapshot.requestedAmount().equals(
                        payment.toState().requestedAmount()
                )
                || !snapshot.debtorAccountReference().equals(
                        payment.toState()
                                .debtorAccountReference()
                                .integrationAccountToken()
                )
                || !snapshot.creditorAccountReference().equals(
                        treasuryAccount.accountToken()
                )) {
            throw new IllegalStateException(
                    "Existing T0 financial snapshot conflicts with Payment"
            );
        }
    }
}
