package com.sixpay.payment.infrastructure.banking.amplitude.release.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.application.port.output.banking.FundsReleaseGateway;
import com.sixpay.payment.domain.model.evidence.FundsReleaseSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.compensation.*;
import com.sixpay.payment.infrastructure.banking.amplitude.release.AmplitudeFundsReleaseClient;
import com.sixpay.payment.infrastructure.banking.amplitude.release.dto.AmplitudeFundsReleaseResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.release.mapper.AmplitudeFundsReleaseMapper;
import org.springframework.http.*;
import org.springframework.web.client.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;

public final class RestAmplitudeFundsReleaseClient
        implements AmplitudeFundsReleaseClient {

    private final RestClient restClient;
    private final CompensationAccessTokenProvider tokenProvider;
    private final AmplitudeCompensationProperties properties;
    private final AmplitudeFundsReleaseMapper mapper;
    private final ObjectMapper objectMapper;

    public RestAmplitudeFundsReleaseClient(
            RestClient restClient,
            CompensationAccessTokenProvider tokenProvider,
            AmplitudeCompensationProperties properties,
            AmplitudeFundsReleaseMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public FundsReleaseSnapshot release(
            FundsReleaseGateway.FundsReleaseRequest request
    ) {
        try {
            String payload = restClient.post()
                    .uri(properties.releasePath())
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
                        "Funds release outcome is unknown"
                );
            }

            return mapper.toSnapshot(
                    objectMapper.readValue(
                            payload,
                            AmplitudeFundsReleaseResponse.class
                    ),
                    request.context().correlationId()
            );
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Funds release outcome is unknown",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 429 || status >= 500) {
                throw new IllegalStateException(
                        "Funds release outcome is unknown",
                        exception
                );
            }
            throw new IllegalStateException(
                    "Funds release request was rejected technically",
                    exception
            );
        }
    }
}
