package com.sixpay.payment.infrastructure.banking.amplitude.status;

import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.LookupGateway;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@ConditionalOnBean(AmplitudePostingStatusClient.class)
@ConditionalOnMissingBean(LookupGateway.class)
public final class DedicatedAmplitudeLookupAdapter
        implements LookupGateway {

    private final AmplitudePostingStatusClient client;

    public DedicatedAmplitudeLookupAdapter(
            AmplitudePostingStatusClient client
    ) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude posting status client"
        );
    }

    @Override
    public Optional<PostingOutcomeSnapshot>
            findPostingByIdempotencyKey(
                    BankingRequestContext context,
                    BankingIdempotencyKey idempotencyKey
            ) {
        return client.findByIdempotencyKey(
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
        return client.findByBankReference(
                context,
                LookupGateway.requireBankReference(
                        bankPostingReference
                )
        );
    }
}
