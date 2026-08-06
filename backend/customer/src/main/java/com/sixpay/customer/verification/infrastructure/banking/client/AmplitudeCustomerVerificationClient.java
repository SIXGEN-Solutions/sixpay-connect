package com.sixpay.customer.verification.infrastructure.banking.client;

import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationRequest;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeClientException;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeErrorResponse;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeInvalidResponseException;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeRateLimitException;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class AmplitudeCustomerVerificationClient {

    private final RestClient restClient;
    private final CoreBankingAccessTokenProvider accessTokenProvider;
    private final BankingVerificationProperties properties;
    private final ObjectMapper objectMapper;

    public AmplitudeCustomerVerificationClient(
            RestClient restClient,
            CoreBankingAccessTokenProvider accessTokenProvider,
            BankingVerificationProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.accessTokenProvider = Objects.requireNonNull(
                accessTokenProvider
        );
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public AmplitudeCustomerVerificationResponse verify(
            AmplitudeCustomerVerificationRequest request,
            String correlationId,
            UUID requestId
    ) {
        Objects.requireNonNull(request, "request is required");
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException(
                    "correlationId is required"
            );
        }
        Objects.requireNonNull(requestId, "requestId is required");

        try {
            AmplitudeCustomerVerificationResponse response =
                    restClient.post()
                            .uri(properties.endpointPath())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer "
                                            + accessTokenProvider.accessToken()
                            )
                            .header(
                                    IntegrationHttpHeaders.CORRELATION_ID,
                                    correlationId
                            )
                            .header(
                                    IntegrationHttpHeaders.REQUEST_ID,
                                    requestId.toString()
                            )
                            .body(request)
                            .retrieve()
                            .body(
                                    AmplitudeCustomerVerificationResponse.class
                            );

            if (response == null) {
                throw new AmplitudeInvalidResponseException(
                        "Amplitude response body is empty"
                );
            }

            return response;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new AmplitudeRateLimitException(
                        retryAfter(exception),
                        exception
                );
            }
            throw structured(exception, correlationId);
        } catch (HttpMessageConversionException exception) {
            throw new AmplitudeInvalidResponseException(
                    "Amplitude response is malformed",
                    exception
            );
        }
    }

    private AmplitudeClientException structured(
            RestClientResponseException exception,
            String correlationId
    ) {
        int status = exception.getStatusCode().value();
        AmplitudeErrorResponse error;

        try {
            error = objectMapper.readValue(
                    exception.getResponseBodyAsString(),
                    AmplitudeErrorResponse.class
            );
        } catch (RuntimeException parsingFailure) {
            error = AmplitudeErrorResponse.unknown(
                    status,
                    correlationId
            );
        }

        return new AmplitudeClientException(
                status,
                error,
                exception
        );
    }

    private static Duration retryAfter(
            RestClientResponseException exception
    ) {
        String value = exception.getResponseHeaders() == null
                ? null
                : exception.getResponseHeaders().getFirst(
                        HttpHeaders.RETRY_AFTER
                );
        if (value == null || value.isBlank()) {
            return Duration.ofSeconds(1);
        }
        try {
            return Duration.ofSeconds(
                    Math.max(1L, Long.parseLong(value.strip()))
            );
        } catch (NumberFormatException ignored) {
            return Duration.ofSeconds(1);
        }
    }
}
