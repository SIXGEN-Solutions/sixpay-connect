package com.sixpay.customer.verification.infrastructure.banking;

import com.sixpay.customer.verification.application.port.out.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.out.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.out.BankingVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import com.sixpay.customer.verification.infrastructure.banking.mapper.AmplitudeCustomerVerificationMapper;

import java.util.Objects;
import java.util.UUID;

public final class AmplitudeCustomerVerificationAdapter
        implements BankingCustomerVerificationPort {

    private final AmplitudeCustomerVerificationClient client;
    private final AmplitudeCustomerVerificationMapper mapper;

    public AmplitudeCustomerVerificationAdapter(
            AmplitudeCustomerVerificationClient client,
            AmplitudeCustomerVerificationMapper mapper
    ) {
        this.client = Objects.requireNonNull(client);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public BankingVerificationResponse verify(
            BankingVerificationQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        return mapper.toInternalResponse(
                client.verify(
                        mapper.toExternalRequest(query),
                        query.context().correlationId().value(),
                        UUID.randomUUID()
                )
        );
    }
}
