package com.sixpay.accounting.infrastructure.partnerstatus.client;

import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.PartnerExternalPaymentStatusGateway;
import com.sixpay.accounting.domain.model.PartnerExternalPaymentStatusEvidence;
import com.sixpay.accounting.infrastructure.partnerstatus.configuration.PartnerStatusProperties;
import com.sixpay.accounting.infrastructure.partnerstatus.dto.PartnerPaymentStatusResponseDto;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.util.Objects;

public final class RestPartnerExternalPaymentStatusClient
        implements PartnerExternalPaymentStatusGateway {

    private final RestClient restClient;
    private final PartnerStatusAccessTokenProvider tokenProvider;
    private final PartnerStatusProperties properties;
    private final Clock clock;

    public RestPartnerExternalPaymentStatusClient(
            RestClient restClient,
            PartnerStatusAccessTokenProvider tokenProvider,
            PartnerStatusProperties properties,
            Clock clock
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public PartnerExternalPaymentStatusEvidence findByPaymentReference(
            String paymentReference,
            AccountingIntegrationContext context
    ) {
        Objects.requireNonNull(context, "context");
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException(
                    "paymentReference is required"
            );
        }

        String reference = paymentReference.strip();

        try {
            PartnerPaymentStatusResponseDto response =
                    restClient.get()
                            .uri(
                                    properties.statusPath(),
                                    reference
                            )
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
                                    context.requestId().toString()
                            )
                            .retrieve()
                            .body(
                                    PartnerPaymentStatusResponseDto.class
                            );

            if (response == null) {
                throw new IllegalStateException(
                        "Partner status response is empty"
                );
            }

            return new PartnerExternalPaymentStatusEvidence(
                    response.reference(),
                    response.transactionId(),
                    response.status(),
                    response.paymentMethod(),
                    response.operatorReference(),
                    response.debitEffectue(),
                    response.quittanceDisponible(),
                    response.updatedAt(),
                    response.failureReason(),
                    clock.instant(),
                    reference,
                    context.correlationId().value()
            );
        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalArgumentException(
                    "Partner payment reference not found",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Partner status service is unavailable",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new IllegalStateException(
                        "Partner status authentication failed",
                        exception
                );
            }
            if (status == 429 || status >= 500) {
                throw new IllegalStateException(
                        "Partner status service is unavailable",
                        exception
                );
            }
            throw new IllegalStateException(
                    "Partner status query was rejected",
                    exception
            );
        }
    }
}
