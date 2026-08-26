package com.sixpay.payment.infrastructure.banking.amplitude.reversal.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.application.port.output.banking.ReversalGateway;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.compensation.*;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.AmplitudeReversalClient;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.dto.AmplitudeReversalResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.mapper.AmplitudeReversalMapper;
import org.springframework.http.*;
import org.springframework.web.client.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;

public final class RestAmplitudeReversalClient
        implements AmplitudeReversalClient {

    private final RestClient restClient;
    private final CompensationAccessTokenProvider tokenProvider;
    private final AmplitudeCompensationProperties properties;
    private final AmplitudeReversalMapper mapper;
    private final ObjectMapper objectMapper;

    public RestAmplitudeReversalClient(
            RestClient restClient,
            CompensationAccessTokenProvider tokenProvider,
            AmplitudeCompensationProperties properties,
            AmplitudeReversalMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public ReversalSnapshot reverse(
            ReversalGateway.ReversalRequest request
    ) {
        try {
            String payload = restClient.post()
                    .uri(properties.reversalPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + tokenProvider.accessToken()
                    )
                    .header(
                            IntegrationHttpHeaders.CORRELATION_ID,
                            request.context().correlationId().value()
                    )
                    .header(
                            IntegrationHttpHeaders.REQUEST_ID,
                            UUID.randomUUID().toString()
                    )
                    .header(
                            properties.contract()
                                    .idempotencyHeader(),
                            request.idempotencyKey().toString()
                    )
                    .body(mapper.toExternal(request))
                    .retrieve()
                    .body(String.class);

            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException(
                        "Reversal outcome is unknown"
                );
            }

            return mapper.toSnapshot(
                    request,
                    objectMapper.readValue(
                            payload,
                            AmplitudeReversalResponse.class
                    )
            );
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Reversal outcome is unknown",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 429 || status >= 500) {
                throw new IllegalStateException(
                        "Reversal outcome is unknown",
                        exception
                );
            }
            throw new IllegalStateException(
                    "Reversal request was rejected technically",
                    exception
            );
        }
    }
}
