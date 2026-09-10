package com.sixpay.accounting.infrastructure.tresorpay.client;

import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.TresorPayPaymentStatusGateway;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import com.sixpay.accounting.infrastructure.tresorpay.configuration.TresorPayStatusProperties;
import com.sixpay.accounting.infrastructure.tresorpay.dto.TresorPayPaymentStatusResponseDto;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.util.Objects;

public final class RestTresorPayPaymentStatusClient
        implements TresorPayPaymentStatusGateway {

    private final RestClient restClient;
    private final TresorPayStatusAccessTokenProvider tokenProvider;
    private final TresorPayStatusProperties properties;
    private final Clock clock;

    public RestTresorPayPaymentStatusClient(
            RestClient restClient,
            TresorPayStatusAccessTokenProvider tokenProvider,
            TresorPayStatusProperties properties,
            Clock clock
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public TresorPayPaymentStatusEvidence findByPaymentReference(
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
            TresorPayPaymentStatusResponseDto response =
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
                                    TresorPayPaymentStatusResponseDto.class
                            );

            if (response == null) {
                throw new IllegalStateException(
                        "TRESOR PAY status response is empty"
                );
            }

            return new TresorPayPaymentStatusEvidence(
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
                    "TRESOR PAY payment reference not found",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "TRESOR PAY status service is unavailable",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new IllegalStateException(
                        "TRESOR PAY status authentication failed",
                        exception
                );
            }
            if (status == 429 || status >= 500) {
                throw new IllegalStateException(
                        "TRESOR PAY status service is unavailable",
                        exception
                );
            }
            throw new IllegalStateException(
                    "TRESOR PAY status query was rejected",
                    exception
            );
        }
    }
}
