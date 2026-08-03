package com.sixpay.customer.verification.infrastructure.banking.client;

import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationRequest;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeClientException;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;

public final class AmplitudeCustomerVerificationClient {

    public static final String CORRELATION_HEADER =
            "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER =
            "X-Request-ID";

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
        this.accessTokenProvider = Objects.requireNonNull(accessTokenProvider);
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
                            .header(CORRELATION_HEADER, correlationId)
                            .header(
                                    REQUEST_ID_HEADER,
                                    requestId.toString()
                            )
                            .body(request)
                            .retrieve()
                            .body(
                                    AmplitudeCustomerVerificationResponse.class
                            );

            if (response == null) {
                throw new AmplitudeClientException(
                        200,
                        AmplitudeErrorResponse.unknown(
                                200,
                                correlationId
                        ),
                        null
                );
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw structured(exception, correlationId);
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
}
