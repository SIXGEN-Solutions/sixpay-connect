package com.sixpay.payment.domain.model.financial;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Provider-event construction facts, distinct from bank execution outcomes. */
public record BankingEventDetails(
        String operationCode,
        String nature,
        String technicalUser,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String eventLabel,
        String bankEventNumber,
        LocalDate accountingDate,
        LocalDate operationDate,
        String originBranch
) {
}
