package com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper;

import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentProviderEvent;

import java.util.List;
import java.util.Objects;

/** Owns the ONLY mapping from finalized SIXPAY financial snapshot to Regional T0 wire DTO. */
public final class AmplitudePaymentEventMapper {

    public AmplitudePaymentEventRequest toRequest(
            PaymentFinancialEventSnapshot snapshot,
            AmplitudePaymentEventMappingContext context
    ) {
        Objects.requireNonNull(snapshot, "Financial event snapshot");
        Objects.requireNonNull(context, "Banking mapping context");
        if (snapshot.status() != FinancialSnapshotStatus.FINALIZED) {
            throw new IllegalArgumentException("Finalized financial snapshot required");
        }
        if (!"VIRPAY".equals(context.nature())) {
            throw new IllegalArgumentException("Unsupported banking nature");
        }
        String currency = snapshot.requestedAmount().currency().getCurrencyCode();
        System.out.println("currency: " + currency);

        List<PaymentFinancialEntrySnapshot> entries = snapshot.entries();
        if (entries.size() != 2) {
            throw new IllegalArgumentException("Exactly two financial snapshot entries required");
        }
        PaymentFinancialEntrySnapshot debit = requireOne(entries, FinancialEntryDirection.DEBIT);
        PaymentFinancialEntrySnapshot credit = requireOne(entries, FinancialEntryDirection.CREDIT);
        validateAccount(snapshot.debtorAccountReference());
        validateAccount(snapshot.creditorAccountReference());
        if (!snapshot.debtorAccountReference().equals(debit.accountReference())
                || !snapshot.creditorAccountReference().equals(credit.accountReference())
                || !snapshot.requestedAmount().equals(debit.amount())
                || !snapshot.requestedAmount().equals(credit.amount())) {
            throw new IllegalArgumentException("Financial snapshot accounts or amounts mismatch");
        }
        String paymentReference = snapshot.publicPaymentReference().value();
        var providerEvent = new AmplitudePaymentProviderEvent();
        providerEvent.setPaymentReference(paymentReference);
        providerEvent.setOperationCode(context.operationCode());
        // Bank DEV currency convention (001); SIXPAY financial snapshot remains ISO XAF.
        providerEvent.setCurrency("001");
        providerEvent.setNature(context.nature());
        providerEvent.setTechnicalUser(context.technicalUser());
        providerEvent.setDebtorAccountReference(snapshot.debtorAccountReference());
        providerEvent.setCreditorAccountReference(snapshot.creditorAccountReference());
        providerEvent.setDebitAmount(debit.amount().amount());
        providerEvent.setCreditAmount(credit.amount().amount());
        providerEvent.setLabel("PAIEMENT/" + paymentReference);
        return new AmplitudePaymentEventRequest(
                paymentReference, snapshot.snapshotVersion(), providerEvent, context.requestedAt());
    }

    private static PaymentFinancialEntrySnapshot requireOne(
            List<PaymentFinancialEntrySnapshot> entries,
            FinancialEntryDirection direction
    ) {
        List<PaymentFinancialEntrySnapshot> matches = entries.stream()
                .filter(entry -> entry.direction() == direction).toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Exactly one " + direction + " entry required");
        }
        return matches.getFirst();
    }

    private static void validateAccount(String account) {
        if (account == null || !account.matches("[^-]+-[^-]+-[^-]+")) {
            throw new IllegalArgumentException("Account must use age-ncp-clc format");
        }
    }
}
