package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.error.AmplitudePaymentEventOutcomeUnknownException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;
import java.util.Optional;

public final class RestAmplitudePaymentEventRecoveryClient
        implements AmplitudePaymentEventRecoveryClient {


    private final RestClient restClient;
    private final PostingAccessTokenProvider tokenProvider;
    private final AmplitudePostingProperties properties;

    public RestAmplitudePaymentEventRecoveryClient(
            RestClient restClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public Optional<AmplitudePaymentEventResult> findByPaymentReference(
            String paymentReference,
            String correlationId,
            String financialInstitutionCode
    ) {
        requireText(paymentReference, "paymentReference");
        return get(
                properties.paymentReferenceLookupPath(),
                paymentReference,
                correlationId,
                financialInstitutionCode
        );
    }

    @Override
    public Optional<AmplitudePaymentEventResult> findByIdempotencyKey(
            String idempotencyKey,
            String correlationId,
            String financialInstitutionCode
    ) {
        requireText(idempotencyKey, "idempotencyKey");
        return get(
                properties.idempotencyLookupPath(),
                idempotencyKey,
                correlationId,
                financialInstitutionCode
        );
    }

    private Optional<AmplitudePaymentEventResult> get(
            String path,
            String pathValue,
            String correlationId,
            String financialInstitutionCode
    ) {
        requireText(correlationId, "correlationId");
        requireText(
                financialInstitutionCode,
                "financialInstitutionCode"
        );

        try {
            AmplitudePaymentEventResult response =
                    restClient.get()
                            .uri(path, pathValue)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer "
                                            + tokenProvider.accessToken()
                            )
                            .header(
                                    properties.contract().correlationHeader(),
                                    correlationId
                            )
                            .header(
                                    properties.contract().institutionHeader(),
                                    financialInstitutionCode
                            )
                            .retrieve()
                            .body(
                                    AmplitudePaymentEventResult.class
                            );

            if (response == null) {
                throw new AmplitudePaymentEventOutcomeUnknownException(
                        "Payment event recovery response is empty",
                        null
                );
            }

            return Optional.of(response);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 404) {
                return Optional.empty();
            }

            if (status == 503 || status >= 500) {
                throw new AmplitudePaymentEventOutcomeUnknownException(
                        "Payment event recovery outcome is unknown",
                        exception
                );
            }

            throw new IllegalStateException(
                    "Payment event recovery request was rejected technically",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new AmplitudePaymentEventOutcomeUnknownException(
                    "Payment event recovery outcome is unknown",
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
