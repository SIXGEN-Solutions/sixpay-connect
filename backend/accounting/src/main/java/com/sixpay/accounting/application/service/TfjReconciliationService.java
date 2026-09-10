package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.TfjConfirmationLookupGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Service
@ConditionalOnBean(TfjConfirmationLookupGateway.class)
public final class TfjReconciliationService {

    private final TfjConfirmationLookupGateway lookupGateway;
    private final TfjIngestionService ingestionService;

    public TfjReconciliationService(
            TfjConfirmationLookupGateway lookupGateway,
            TfjIngestionService ingestionService
    ) {
        this.lookupGateway = Objects.requireNonNull(lookupGateway);
        this.ingestionService = Objects.requireNonNull(ingestionService);
    }

    public Optional<TfjIngestionResult> reconcile(
            String financialInstitutionCode,
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference,
            AccountingIntegrationContext context
    ) {
        return lookupGateway.lookup(
                        financialInstitutionCode,
                        businessDate,
                        paymentReference,
                        bankPostingReference,
                        context
                )
                .map(ingestionService::ingest);
    }
}
