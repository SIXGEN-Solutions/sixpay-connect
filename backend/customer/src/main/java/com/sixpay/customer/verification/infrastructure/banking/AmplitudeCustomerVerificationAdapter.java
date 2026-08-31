package com.sixpay.customer.verification.infrastructure.banking;

import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.output.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeResponseValidator;
import com.sixpay.customer.verification.infrastructure.banking.error.BankingVerificationErrorClassifier;
import com.sixpay.customer.verification.infrastructure.banking.mapper.AmplitudeCustomerVerificationMapper;

import java.util.Objects;

public final class AmplitudeCustomerVerificationAdapter
        implements BankingCustomerVerificationPort {

    private final AmplitudeCustomerVerificationClient client;
    private final AmplitudeCustomerVerificationMapper mapper;
    private final AmplitudeResponseValidator responseValidator;
    private final BankingVerificationErrorClassifier errorClassifier;

    public AmplitudeCustomerVerificationAdapter(
            AmplitudeCustomerVerificationClient client,
            AmplitudeCustomerVerificationMapper mapper,
            AmplitudeResponseValidator responseValidator,
            BankingVerificationErrorClassifier errorClassifier
    ) {
        this.client = Objects.requireNonNull(client);
        this.mapper = Objects.requireNonNull(mapper);
        this.responseValidator = Objects.requireNonNull(
                responseValidator
        );
        this.errorClassifier = Objects.requireNonNull(
                errorClassifier
        );
    }

    @Override
    public BankingVerificationResponse verify(
            BankingVerificationQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        try {
            return mapper.toInternalResponse(
                    responseValidator.validate(
                            client.verify(
                                    mapper.toExternalRequest(query),
                                    query.context()
                                            .correlationId()
                                            .value()
                            )
                    )
            );
        } catch (RuntimeException failure) {
            throw errorClassifier.classify(failure);
        }
    }
}
