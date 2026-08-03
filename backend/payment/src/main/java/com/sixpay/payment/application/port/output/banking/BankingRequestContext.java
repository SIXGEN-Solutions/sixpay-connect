package com.sixpay.payment.application.port.output.banking;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;

import java.util.Objects;

public record BankingRequestContext(
        CorrelationId correlationId,
        FinancialInstitutionCode financialInstitutionCode
) {
    public BankingRequestContext {
        correlationId = Objects.requireNonNull(correlationId, "Correlation ID");
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
    }
}
