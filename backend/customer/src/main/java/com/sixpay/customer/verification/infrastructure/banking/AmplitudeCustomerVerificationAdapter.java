package com.sixpay.customer.verification.infrastructure.banking;

import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.output.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import com.sixpay.customer.verification.infrastructure.banking.error.BankingVerificationErrorClassifier;
import com.sixpay.customer.verification.infrastructure.banking.mapper.AmplitudeCustomerVerificationMapper;

import java.util.Objects;
import java.util.UUID;

/**
 * Core Banking output adapter.
 *
 * <p>Business-negative evidence is returned normally as canonical FAIL checks.
 * Only technical/protocol failures are translated to internal exceptions.</p>
 */
public final class AmplitudeCustomerVerificationAdapter
        implements BankingCustomerVerificationPort {

    private final AmplitudeCustomerVerificationClient client;
    private final AmplitudeCustomerVerificationMapper mapper;
    private final BankingVerificationErrorClassifier errorClassifier;

    public AmplitudeCustomerVerificationAdapter(
            AmplitudeCustomerVerificationClient client,
            AmplitudeCustomerVerificationMapper mapper,
            BankingVerificationErrorClassifier errorClassifier
    ) {
        this.client = Objects.requireNonNull(client);
        this.mapper = Objects.requireNonNull(mapper);
        this.errorClassifier = Objects.requireNonNull(errorClassifier);
    }

    @Override
    public BankingVerificationResponse verify(
            BankingVerificationQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        try {
            return mapper.toInternalResponse(
                    client.verify(
                            mapper.toExternalRequest(query),
                            query.context().correlationId().value(),
                            UUID.randomUUID()
                    )
            );
        } catch (RuntimeException failure) {
            throw errorClassifier.classify(failure);
        }
    }
}
