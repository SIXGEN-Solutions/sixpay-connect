package com.sixpay.accounting.api.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AccountingT1ManualExecutionRequest(
        @NotNull LocalDate businessDate
) {
}
