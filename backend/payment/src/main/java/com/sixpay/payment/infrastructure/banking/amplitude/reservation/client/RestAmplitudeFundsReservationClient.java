package com.sixpay.payment.infrastructure.banking.amplitude.reservation.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.application.port.output.banking.FundsReservationGateway;
import com.sixpay.payment.domain.model.evidence.FundsReservationSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.AmplitudeFundsReservationClient;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.configuration.AmplitudeFundsReservationProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.dto.AmplitudeFundsReservationResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.error.FundsReservationIntegrationException;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.error.FundsReservationOutcomeUnknownException;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.mapper.AmplitudeFundsReservationMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.validation.AmplitudeFundsReservationResponseValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;

public final class RestAmplitudeFundsReservationClient
        implements AmplitudeFundsReservationClient {

    private final RestClient restClient;
    private final FundsReservationAccessTokenProvider tokenProvider;
    private final AmplitudeFundsReservationProperties properties;
    private final AmplitudeFundsReservationMapper mapper;
    private final AmplitudeFundsReservationResponseValidator validator;
    private final ObjectMapper objectMapper;

    public RestAmplitudeFundsReservationClient(
            RestClient restClient,
            FundsReservationAccessTokenProvider tokenProvider,
            AmplitudeFundsReservationProperties properties,
            AmplitudeFundsReservationMapper mapper,
            AmplitudeFundsReservationResponseValidator validator,
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
    public FundsReservationSnapshot reserve(
            FundsReservationGateway.FundsReservationRequest request
    ) {
        Objects.requireNonNull(request, "Funds reservation request");

        try {
            String payload = restClient.post()
                    .uri(properties.reservationPath())
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
                throw new FundsReservationOutcomeUnknownException(
                        "Funds reservation response is empty",
                        null
                );
            }

            AmplitudeFundsReservationResponse response =
                    objectMapper.readValue(
                            payload,
                            AmplitudeFundsReservationResponse.class
                    );

            return mapper.toSnapshot(
                    request,
                    validator.validate(response)
            );
        } catch (ResourceAccessException exception) {
            throw new FundsReservationOutcomeUnknownException(
                    "Funds reservation outcome is unknown",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 429 || status >= 500) {
                throw new FundsReservationOutcomeUnknownException(
                        "Funds reservation outcome is unknown",
                        exception
                );
            }

            throw new FundsReservationIntegrationException(
                    "Funds reservation request was rejected technically",
                    status,
                    false,
                    exception
            );
        } catch (FundsReservationOutcomeUnknownException
                 | FundsReservationIntegrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new FundsReservationIntegrationException(
                    "Funds reservation response is invalid",
                    0,
                    false,
                    exception
            );
        }
    }
}
