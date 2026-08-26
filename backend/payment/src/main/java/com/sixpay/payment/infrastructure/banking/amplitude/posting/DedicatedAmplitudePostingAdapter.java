package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.payment.application.port.output.banking.PostingGateway;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudePostingClient.class)
@ConditionalOnMissingBean(PostingGateway.class)
public final class DedicatedAmplitudePostingAdapter
        implements PostingGateway {

    private final AmplitudePostingClient client;

    public DedicatedAmplitudePostingAdapter(
            AmplitudePostingClient client
    ) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude posting client"
        );
    }

    @Override
    public PostingOutcomeSnapshot post(
            PostingRequest request
    ) {
        return client.post(request);
    }
}
