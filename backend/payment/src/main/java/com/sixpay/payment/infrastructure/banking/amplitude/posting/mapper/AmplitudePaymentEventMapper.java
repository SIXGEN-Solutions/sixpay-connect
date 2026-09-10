package com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper;

import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentProviderEntry;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentProviderEvent;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AmplitudePaymentEventMapper {

    private static final String REQUIRED_NATURE = "VIRPAY";
    private static final String DEBIT_CODE = "D";
    private static final String CREDIT_CODE = "C";
    private static final String REQUIRED_CURRENCY = "XAF";

    private static final Pattern ACCOUNT_REFERENCE = Pattern.compile(
            "^[^-]+-[^-]+-[^-]+$"
    );

    public AmplitudePaymentEventRequest toRequest(
            PaymentFinancialEventSnapshot snapshot,
            AmplitudePaymentEventMappingContext context
    ) {
        Objects.requireNonNull(
                snapshot,
                "Financial event snapshot"
        );
        Objects.requireNonNull(
                context,
                "Amplitude mapping context"
        );

        if (snapshot.status() != FinancialSnapshotStatus.FINALIZED) {
            throw new IllegalArgumentException(
                    "Amplitude mapping requires a finalized financial snapshot"
            );
        }
        if (!REQUIRED_NATURE.equals(context.nature())) {
            throw new IllegalArgumentException(
                    "Amplitude nature must be VIRPAY for TRESOR PAY"
            );
        }
        if (!REQUIRED_CURRENCY.equals(
                snapshot.requestedAmount()
                        .currency()
                        .getCurrencyCode()
        )) {
            throw new IllegalArgumentException(
                    "Amplitude Payment event currency must be XAF"
            );
        }

        List<PaymentFinancialEntrySnapshot> entries =
                snapshot.entries();

        if (entries.size() != 2) {
            throw new IllegalArgumentException(
                    "Amplitude Payment event requires exactly two financial entries"
            );
        }

        PaymentFinancialEntrySnapshot debit = requireEntry(
                entries,
                FinancialEntryDirection.DEBIT
        );
        PaymentFinancialEntrySnapshot credit = requireEntry(
                entries,
                FinancialEntryDirection.CREDIT
        );

        validateAccountReference(
                snapshot.debtorAccountReference(),
                "Debtor"
        );
        validateAccountReference(
                snapshot.creditorAccountReference(),
                "Creditor"
        );

        if (!snapshot.debtorAccountReference()
                .equals(debit.accountReference())) {
            throw new IllegalArgumentException(
                    "Debit entry account must match the snapshot debtor account"
            );
        }
        if (!snapshot.creditorAccountReference()
                .equals(credit.accountReference())) {
            throw new IllegalArgumentException(
                    "Credit entry account must match the snapshot creditor account"
            );
        }
        if (!snapshot.requestedAmount().equals(debit.amount())
                || !snapshot.requestedAmount().equals(credit.amount())) {
            throw new IllegalArgumentException(
                    "Debit and credit entries must match the requested amount"
            );
        }

        String paymentReference =
                snapshot.publicPaymentReference().value();

        AmplitudePaymentProviderEvent providerEvent =
                new AmplitudePaymentProviderEvent(
                        paymentReference,
                        context.operationCode(),
                        context.eventNumber(),
                        snapshot.requestedAmount()
                                .currency()
                                .getCurrencyCode(),
                        context.nature(),
                        context.accountingDate(),
                        context.technicalUser(),
                        snapshot.debtorAccountReference(),
                        snapshot.creditorAccountReference(),
                        snapshot.requestedAmount().amount(),
                        buildLabel(
                                context.accountingDate().toString(),
                                paymentReference,
                                snapshot.debtorAccountReference()
                        ),
                        context.nightMode()
                );

        List<AmplitudePaymentProviderEntry> providerEntries =
                entries.stream()
                        .map(entry -> mapEntry(
                                entry,
                                paymentReference
                        ))
                        .toList();

        return new AmplitudePaymentEventRequest(
                paymentReference,
                snapshot.snapshotVersion(),
                providerEvent,
                providerEntries,
                context.requestedAt()
        );
    }

    private static PaymentFinancialEntrySnapshot requireEntry(
            List<PaymentFinancialEntrySnapshot> entries,
            FinancialEntryDirection direction
    ) {
        List<PaymentFinancialEntrySnapshot> matching =
                entries.stream()
                        .filter(entry -> entry.direction() == direction)
                        .toList();

        if (matching.size() != 1) {
            throw new IllegalArgumentException(
                    "Amplitude Payment event requires exactly one "
                            + direction + " entry"
            );
        }

        return matching.getFirst();
    }

    private static AmplitudePaymentProviderEntry mapEntry(
            PaymentFinancialEntrySnapshot entry,
            String paymentReference
    ) {
        String direction = switch (entry.direction()) {
            case DEBIT -> DEBIT_CODE;
            case CREDIT -> CREDIT_CODE;
        };

        validateAccountReference(
                entry.accountReference(),
                "Entry"
        );

        String currency =
                entry.amount().currency().getCurrencyCode();

        if (!REQUIRED_CURRENCY.equals(currency)) {
            throw new IllegalArgumentException(
                    "Amplitude provider entries must use XAF"
            );
        }

        return new AmplitudePaymentProviderEntry(
                entry.sequence(),
                direction,
                entry.accountReference(),
                entry.amount().amount(),
                currency,
                paymentReference
        );
    }

    private static String buildLabel(
            String accountingDate,
            String paymentReference,
            String debtorAccountReference
    ) {
        String debtorNcp =
                debtorAccountReference.split("-", -1)[1];

        return accountingDate
                + "/PAIEMENT/"
                + paymentReference
                + "/"
                + debtorNcp;
    }

    private static void validateAccountReference(
            String value,
            String label
    ) {
        if (value == null
                || !ACCOUNT_REFERENCE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    label
                            + " account reference must use age-ncp-clc format"
            );
        }

        String[] parts = value.split("-", -1);
        if (parts.length != 3
                || parts[0].isBlank()
                || parts[1].isBlank()
                || parts[2].isBlank()) {
            throw new IllegalArgumentException(
                    label
                            + " account reference must use age-ncp-clc format"
            );
        }
    }
}
