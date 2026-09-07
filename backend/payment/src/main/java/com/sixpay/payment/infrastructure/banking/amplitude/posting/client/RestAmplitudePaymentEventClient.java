package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.error.AmplitudePaymentEventOutcomeUnknownException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;

public final class RestAmplitudePaymentEventClient
        implements AmplitudePaymentEventClient {

    public static final String FINANCIAL_INSTITUTION_HEADER =
            "X-Financial-Institution-Code";

    private final RestClient restClient;
    private final PostingAccessTokenProvider tokenProvider;
    private final AmplitudePostingProperties properties;

    public RestAmplitudePaymentEventClient(
            RestClient restClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public AmplitudePaymentEventResult execute(
            AmplitudePaymentEventRequest request,
            String correlationId,
            String financialInstitutionCode,
            String idempotencyKey
    ) {
        Objects.requireNonNull(request, "request is required");
        requireText(correlationId, "correlationId");
        requireText(
                financialInstitutionCode,
                "financialInstitutionCode"
        );
        requireText(idempotencyKey, "idempotencyKey");

        try {
            AmplitudePaymentEventResult response =
                    restClient.post()
                            .uri(properties.postingPath())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer "
                                            + tokenProvider.accessToken()
                            )
                            .header(
                                    IntegrationHttpHeaders.CORRELATION_ID,
                                    correlationId
                            )
                            .header(
                                    FINANCIAL_INSTITUTION_HEADER,
                                    financialInstitutionCode
                            )
                            .header(
                                    properties.contract()
                                            .idempotencyHeader(),
                                    idempotencyKey
                            )
                            .body(request)
                            .retrieve()
                            .body(
                                    AmplitudePaymentEventResult.class
                            );

            if (response == null) {
                throw new AmplitudePaymentEventOutcomeUnknownException(
                        "Payment event response is empty",
                        null
                );
            }

            return response;
        } catch (ResourceAccessException exception) {
            throw new AmplitudePaymentEventOutcomeUnknownException(
                    "Payment event outcome is unknown",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 503 || status >= 500) {
                throw new AmplitudePaymentEventOutcomeUnknownException(
                        "Payment event outcome is unknown",
                        exception
                );
            }

            throw new IllegalStateException(
                    "Payment event request was rejected technically",
                    exception
            );
        }
    }

    private static String requireText(
            String value,
            String label
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    label + " is required"
            );
        }
        return value;
    }
}
