package com.sixpay.payment.application.service;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.BankingEntryDetails;
import com.sixpay.payment.domain.model.financial.BankingEventDetails;
import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
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
            Payment payment, TreasuryAccountReference treasuryAccount, Instant createdAt
    ) {
        return getOrCreateFinalized(payment, treasuryAccount, createdAt, null);
    }

    public PaymentFinancialEventSnapshot getOrCreateFinalized(
            Payment payment, TreasuryAccountReference treasuryAccount, Instant createdAt,
            PaymentEventExecutionPort.PaymentEventMappingContext bankingContext
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
                        createdAt,
                        eventDetails(payment, bankingContext)
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
                        createdAt,
                        entryDetails(payment.toState().debtorAccountReference().integrationAccountToken(),
                                payment.publicPaymentReference().value(), 1, bankingContext)
                )
        );

        snapshot.addEntry(
                new PaymentFinancialEntrySnapshot(
                        identifierGenerator.generate(),
                        2,
                        FinancialEntryDirection.CREDIT,
                        treasuryAccount.accountToken(),
                        payment.toState().requestedAmount(),
                        createdAt,
                        entryDetails(treasuryAccount.accountToken(),
                                payment.publicPaymentReference().value(), 2, bankingContext)
                )
        );

        snapshot.finalizeAt(createdAt);

        return repository.save(snapshot);
    }

    private static BankingEventDetails eventDetails(
            Payment payment, PaymentEventExecutionPort.PaymentEventMappingContext context
    ) {
        if (context == null) return null; // compatibility path for pre-existing callers
        var amount = payment.toState().requestedAmount().amount();
        return new BankingEventDetails(context.operationCode(), "VIRPAY",
                context.technicalUser(), amount, amount,
                "PAIEMENT/" + payment.publicPaymentReference().value(),
                null, null, null, null);
    }

    private static BankingEntryDetails entryDetails(
            String account, String paymentReference, int sequence,
            PaymentEventExecutionPort.PaymentEventMappingContext context
    ) {
        if (context == null) return null;
        String[] parts = account.split("-", -1);
        if (parts.length != 3 || java.util.Arrays.stream(parts).anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Verified bank account must use age-ncp-clc");
        }
        return new BankingEntryDetails(
                parts[0], null, parts[1], null, parts[2],
                context.operationCode(), Integer.toString(sequence),
                context.technicalUser(), null, null, null,
                "PAIEMENT/" + paymentReference, null, null,
                null, null, null, null, "VIRPAY", null);
    }

    private static void validateExisting(
            PaymentFinancialEventSnapshot snapshot,
            Payment payment,
            TreasuryAccountReference treasuryAccount
    ) {
        // Never reuse a persisted snapshot with incomplete or inconsistent T0 legs.
        if (snapshot.status() != com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus.FINALIZED
                || snapshot.entries().size() != 2
                || snapshot.entries().get(0).sequence() != 1
                || snapshot.entries().get(0).direction() != FinancialEntryDirection.DEBIT
                || snapshot.entries().get(1).sequence() != 2
                || snapshot.entries().get(1).direction() != FinancialEntryDirection.CREDIT
                || !snapshot.entries().get(0).accountReference().equals(snapshot.debtorAccountReference())
                || !snapshot.entries().get(1).accountReference().equals(snapshot.creditorAccountReference())
                || !snapshot.entries().get(0).amount().equals(snapshot.requestedAmount())
                || !snapshot.entries().get(1).amount().equals(snapshot.requestedAmount())) {
            throw new IllegalStateException("Existing T0 financial snapshot has invalid debit/credit legs");
        }
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
