package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.TfjConfirmation;

import java.time.LocalDate;
import java.util.Optional;

public interface TfjConfirmationLookupGateway {
    Optional<TfjConfirmation> lookup(
            String financialInstitutionCode,
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference,
            AccountingIntegrationContext context
    );
}
