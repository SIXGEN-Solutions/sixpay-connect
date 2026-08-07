package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.application.port.output.banking.PostingGateway;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePostingClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePostingResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.error.PostingOutcomeUnknownException;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper.AmplitudePostingMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.validation.AmplitudePostingResponseValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;

public final class RestAmplitudePostingClient
        implements AmplitudePostingClient {

    private final RestClient restClient;
    private final PostingAccessTokenProvider tokenProvider;
    private final AmplitudePostingProperties properties;
    private final AmplitudePostingMapper mapper;
    private final AmplitudePostingResponseValidator validator;
    private final ObjectMapper objectMapper;

    public RestAmplitudePostingClient(
            RestClient restClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties,
            AmplitudePostingMapper mapper,
            AmplitudePostingResponseValidator validator,
            ObjectMapper objectMapper
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
        this.validator = Objects.requireNonNull(validator);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public PostingOutcomeSnapshot post(
            PostingGateway.PostingRequest request
    ) {
        try {
            String payload = restClient.post()
                    .uri(properties.postingPath())
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
                            properties.contract().idempotencyHeader(),
                            request.idempotencyKey().toString()
                    )
                    .body(mapper.toExternal(request))
                    .retrieve()
                    .body(String.class);

            if (payload == null || payload.isBlank()) {
                throw new PostingOutcomeUnknownException(
                        "Posting response is empty",
                        null
                );
            }

            AmplitudePostingResponse response =
                    objectMapper.readValue(
                            payload,
                            AmplitudePostingResponse.class
                    );

            return mapper.toSnapshot(
                    request,
                    validator.validate(response)
            );
        } catch (ResourceAccessException exception) {
            throw new PostingOutcomeUnknownException(
                    "Posting outcome is unknown",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 429 || status >= 500) {
                throw new PostingOutcomeUnknownException(
                        "Posting outcome is unknown",
                        exception
                );
            }

            throw new IllegalStateException(
                    "Posting request was rejected technically",
                    exception
            );
        } catch (PostingOutcomeUnknownException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Posting response is invalid",
                    exception
            );
        }
    }
}
