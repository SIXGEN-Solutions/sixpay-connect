package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.BankingCustomerPreviewUseCase;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class BankingCustomerPreviewService
        implements BankingCustomerPreviewUseCase {

    private final BankingCustomerLookupPort lookupPort;

    public BankingCustomerPreviewService(
            BankingCustomerLookupPort lookupPort
    ) {
        this.lookupPort = Objects.requireNonNull(lookupPort);
    }

    @Override
    public BankingCustomerPreview preview(
            BankingCustomerPreviewQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        var profile = lookupPort.lookup(
                new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                        query.financialInstitutionCode(),
                        query.niu(),
                        query.customerNumber(),
                        query.accountReference(),
                        query.correlationId()
                )
        );

        return new BankingCustomerPreview(
                profile.financialInstitutionCode(),
                profile.customerReference(),
                profile.customerNumber(),
                profile.niu(),
                profile.legalName(),
                profile.email(),
                profile.phoneNumber(),
                profile.account().accountReference(),
                profile.account().maskedAccountIdentifier(),
                profile.account().currency(),
                profile.account().accountType(),
                profile.account().retrievedAt()
        );
    }
}
