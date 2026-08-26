package com.sixpay.payment.infrastructure.banking.amplitude.status.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.LookupGateway;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.status.AmplitudePostingStatusClient;
import com.sixpay.payment.infrastructure.banking.amplitude.status.configuration.AmplitudePostingStatusProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.status.dto.AmplitudePostingStatusResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.status.mapper.AmplitudePostingStatusMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RestAmplitudePostingStatusClient
        implements AmplitudePostingStatusClient {

    private final RestClient restClient;
    private final PostingStatusAccessTokenProvider tokenProvider;
    private final AmplitudePostingStatusProperties properties;
    private final AmplitudePostingStatusMapper mapper;
    private final ObjectMapper objectMapper;

    public RestAmplitudePostingStatusClient(
            RestClient restClient,
            PostingStatusAccessTokenProvider tokenProvider,
            AmplitudePostingStatusProperties properties,
            AmplitudePostingStatusMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public Optional<PostingOutcomeSnapshot>
            findByIdempotencyKey(
                    BankingRequestContext context,
                    BankingIdempotencyKey idempotencyKey
            ) {
        Objects.requireNonNull(context, "Banking request context");
        Objects.requireNonNull(
                idempotencyKey,
                "Banking idempotency key"
        );

        return get(
                properties.byIdempotencyPath(),
                idempotencyKey.toString(),
                context,
                EvidenceObservationChannel.IDEMPOTENCY_LOOKUP
        );
    }

    @Override
    public Optional<PostingOutcomeSnapshot>
            findByBankReference(
                    BankingRequestContext context,
                    String bankPostingReference
            ) {
        Objects.requireNonNull(context, "Banking request context");

        return get(
                properties.byBankReferencePath(),
                LookupGateway.requireBankReference(
                        bankPostingReference
                ),
                context,
                EvidenceObservationChannel.BANK_REFERENCE_LOOKUP
        );
    }

    private Optional<PostingOutcomeSnapshot> get(
            String path,
            String lookupValue,
            BankingRequestContext context,
            EvidenceObservationChannel channel
    ) {
        try {
            String payload = restClient.get()
                    .uri(path, lookupValue)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + tokenProvider.accessToken()
                    )
                    .header(
                            IntegrationHttpHeaders.CORRELATION_ID,
                            context.correlationId().value()
                    )
                    .header(
                            IntegrationHttpHeaders.REQUEST_ID,
                            UUID.randomUUID().toString()
                    )
                    .retrieve()
                    .body(String.class);

            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException(
                        "Posting-status response is empty"
                );
            }

            AmplitudePostingStatusResponse response =
                    objectMapper.readValue(
                            payload,
                            AmplitudePostingStatusResponse.class
                    );

            return Optional.of(
                    mapper.toSnapshot(
                            response,
                            context.correlationId(),
                            channel
                    )
            );
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }
}
