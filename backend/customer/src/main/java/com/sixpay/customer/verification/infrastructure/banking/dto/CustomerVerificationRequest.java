package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Outbound Regional Core Banking V1 wire representation, owned by Customer. */
public record CustomerVerificationRequest(
        String accountReference,
        String expectedNiu,
        String expectedAccountHolder,
        List<String> requiredKycFields,
        Instant requestedAt
) {
    public static CustomerVerificationRequest from(
            AmplitudeCustomerVerificationRequest request
    ) {
        Objects.requireNonNull(request, "request is required");
        var customer = request.customer();
        var account = request.account();
        return new CustomerVerificationRequest(
                account == null ? null : account.accountReference(),
                customer == null ? null : customer.niu(),
                customer == null ? null : customer.legalName(),
                request.requiredKycFields(),
                request.requestedAt()
        );
    }
}
