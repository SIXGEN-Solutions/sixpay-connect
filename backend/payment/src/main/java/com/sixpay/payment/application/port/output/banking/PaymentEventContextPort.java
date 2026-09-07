package com.sixpay.payment.application.port.output.banking;

import java.time.LocalDate;

public interface PaymentEventContextPort {

    long allocateNextEventNumber(
            String operationCode,
            BankingRequestContext context
    );

    LocalDate getAccountingDate(
            BankingRequestContext context
    );

    boolean getNightMode(
            BankingRequestContext context
    );
}
