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

    private static final String PAYMENT_REFERENCE_PATH =
            "/api/v1/payment-events/{paymentReference}";
    private static final String IDEMPOTENCY_LOOKUP_PATH =
            "/api/v1/payment-events/idempotency/{idempotencyKey}";
    private static final String FINANCIAL_INSTITUTION_HEADER =
            "X-Financial-Institution-Code";

    private final RestClient restClient;
    private final PostingAccessTokenProvider tokenProvider;

    public RestAmplitudePaymentEventRecoveryClient(
            RestClient restClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties ignoredProperties
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        Objects.requireNonNull(ignoredProperties);
    }

    @Override
    public Optional<AmplitudePaymentEventResult> findByPaymentReference(
            String paymentReference,
            String correlationId,
            String financialInstitutionCode
    ) {
        requireText(paymentReference, "paymentReference");
        return get(
                PAYMENT_REFERENCE_PATH,
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
                IDEMPOTENCY_LOOKUP_PATH,
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
                                    IntegrationHttpHeaders.CORRELATION_ID,
                                    correlationId
                            )
                            .header(
                                    FINANCIAL_INSTITUTION_HEADER,
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
