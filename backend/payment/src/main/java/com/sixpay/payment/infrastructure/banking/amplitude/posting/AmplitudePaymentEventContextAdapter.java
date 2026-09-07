package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentEventContextPort;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.AmplitudePaymentEventContextClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.CoreBankingStringDataResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class AmplitudePaymentEventContextAdapter
        implements PaymentEventContextPort {

    private final AmplitudePaymentEventContextClient client;

    public AmplitudePaymentEventContextAdapter(
            AmplitudePaymentEventContextClient client
    ) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public long allocateNextEventNumber(
            String operationCode,
            BankingRequestContext context
    ) {
        String data = requireData(
                client.allocateNextEventNumber(
                        operationCode,
                        context.correlationId().value(),
                        context.financialInstitutionCode().value()
                ),
                "allocated event number"
        );

        try {
            long value = Long.parseLong(data);
            if (value <= 0) {
                throw new IllegalStateException(
                        "Core Banking allocated event number "
                                + "must be positive"
                );
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Invalid Core Banking allocated event number",
                    exception
            );
        }
    }

    @Override
    public LocalDate getAccountingDate(
            BankingRequestContext context
    ) {
        String data = requireData(
                client.getAccountingDate(
                        context.correlationId().value(),
                        context.financialInstitutionCode().value()
                ),
                "accounting date"
        );

        try {
            return LocalDate.parse(
                    data,
                    DateTimeFormatter.ISO_LOCAL_DATE
            );
        } catch (DateTimeParseException first) {
            try {
                return LocalDate.parse(
                        data,
                        DateTimeFormatter.BASIC_ISO_DATE
                );
            } catch (DateTimeParseException second) {
                second.addSuppressed(first);
                throw new IllegalStateException(
                        "Unsupported Core Banking accounting date format",
                        second
                );
            }
        }
    }

    @Override
    public boolean getNightMode(
            BankingRequestContext context
    ) {
        String code = client.getNightMode(
                        context.correlationId().value(),
                        context.financialInstitutionCode().value()
                )
                .code();

        return "200".equals(code);
    }

    private static String requireData(
            CoreBankingStringDataResponse response,
            String label
    ) {
        Objects.requireNonNull(response, label + " response");

        if (response.data() == null || response.data().isBlank()) {
            throw new IllegalStateException(
                    "Core Banking " + label + " is missing"
            );
        }

        return response.data().strip();
    }
}
