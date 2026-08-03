package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.LookupGateway;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeBankingClient.class)
public final class AmplitudeLookupAdapter
        implements LookupGateway {

    private final AmplitudeBankingClient client;

    public AmplitudeLookupAdapter(AmplitudeBankingClient client) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude banking client"
        );
    }

    @Override
    public Optional<PostingOutcomeSnapshot>
            findPostingByIdempotencyKey(
                    BankingRequestContext context,
                    BankingIdempotencyKey idempotencyKey
            ) {
        return client.findPostingByIdempotencyKey(
                context,
                idempotencyKey
        );
    }

    @Override
    public Optional<PostingOutcomeSnapshot>
            findPostingByBankReference(
                    BankingRequestContext context,
                    String bankPostingReference
            ) {
        return client.findPostingByBankReference(
                context,
                LookupGateway.requireBankReference(
                        bankPostingReference
                )
        );
    }
}
