package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.client;

import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.AmplitudePaymentConfirmationClient;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.configuration.AmplitudePaymentConfirmationProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.dto.AmplitudeConfirmationResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.mapper.AmplitudePaymentConfirmationMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.validation.AmplitudePaymentConfirmationResponseValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public final class RestAmplitudePaymentConfirmationClient implements AmplitudePaymentConfirmationClient {

    private final RestClient restClient;
    private final ConfirmationAccessTokenProvider tokenProvider;
    private final AmplitudePaymentConfirmationProperties properties;
    private final AmplitudePaymentConfirmationMapper mapper;
    private final AmplitudePaymentConfirmationResponseValidator validator;
    private final ObjectMapper objectMapper;

    public RestAmplitudePaymentConfirmationClient(
            RestClient restClient,
            ConfirmationAccessTokenProvider tokenProvider,
            AmplitudePaymentConfirmationProperties properties,
            AmplitudePaymentConfirmationMapper mapper,
            AmplitudePaymentConfirmationResponseValidator validator,
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
    public PaymentConfirmationBankResult create(PaymentConfirmationGateway.CreateRequest request) {
        return executeMutating("create", properties.createPath(), request.context(),
                request.idempotencyKey().toString(), mapper.toCreate(request));
    }

    @Override
    public PaymentConfirmationBankResult verify(PaymentConfirmationGateway.VerifyRequest request) {
        return executeMutating("verify",
                properties.verificationPath().replace("{challengeReference}", request.challengeReference().value()),
                request.context(), request.idempotencyKey().toString(), mapper.toVerify(request));
    }

    @Override
    public PaymentConfirmationBankResult replace(PaymentConfirmationGateway.ReplaceRequest request) {
        return executeMutating("replace",
                properties.replacementPath().replace("{challengeReference}", request.challengeReference().value()),
                request.context(), request.idempotencyKey().toString(), mapper.toReplace(request));
    }

    @Override
    public PaymentConfirmationBankResult lookup(PaymentConfirmationGateway.LookupRequest request) {
        String path = properties.challengePath().replace("{challengeReference}", request.challengeReference().value());
        try {
            String payload = requestHeaders(restClient.get().uri(path), request.context())
                    .retrieve().body(String.class);
            return decode(payload);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new IllegalStateException("Payment confirmation lookup unavailable", exception);
        }
    }

    @Override
    public PaymentConfirmationBankResult recover(PaymentConfirmationGateway.RecoveryRequest request) {
        String path = properties.lookupByIdempotencyPath()
                .replace("{idempotencyKey}", request.originalIdempotencyKey().toString());
        try {
            String payload = requestHeaders(restClient.get().uri(path), request.context())
                    .retrieve().body(String.class);
            return decode(payload);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new IllegalStateException("Payment confirmation recovery unavailable", exception);
        }
    }

    @Override
    public PaymentConfirmationBankResult revoke(PaymentConfirmationGateway.RevokeRequest request) {
        return executeMutating("revoke",
                properties.revocationPath().replace("{challengeReference}", request.challengeReference().value()),
                request.context(), request.idempotencyKey().toString(), mapper.toRevoke(request));
    }

    private PaymentConfirmationBankResult executeMutating(
            String operation,
            String path,
            BankingRequestContext context,
            String idempotencyKey,
            Object body
    ) {
        try {
            String payload = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + tokenProvider.accessToken()
                    )
                    .header(
                            properties.contract().correlationHeader(),
                            context.correlationId().value()
                    )
                    .header(
                            properties.contract().institutionHeader(),
                            context.financialInstitutionCode().value()
                    )
                    .header(
                            properties.contract().idempotencyHeader(),
                            idempotencyKey
                    )
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return decode(payload);
        } catch (ResourceAccessException exception) {
            throw new PaymentConfirmationGateway.OutcomeUnknownException(
                    "Payment confirmation " + operation + " outcome is unknown",
                    idempotencyKey,
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 429 || status >= 500) {
                throw new PaymentConfirmationGateway.OutcomeUnknownException(
                        "Payment confirmation " + operation + " outcome is unknown",
                        idempotencyKey,
                        exception
                );
            }
            throw new IllegalStateException(
                    "Payment confirmation " + operation + " request was rejected",
                    exception
            );
        }
    }

    private RestClient.RequestHeadersSpec<?> requestHeaders(
            RestClient.RequestHeadersSpec<?> spec,
            BankingRequestContext context
    ) {
        return spec
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.accessToken())
                .header(properties.contract().correlationHeader(), context.correlationId().value())
                .header(properties.contract().institutionHeader(), context.financialInstitutionCode().value());
    }

    private PaymentConfirmationBankResult decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("Payment confirmation response is empty");
        }
        try {
            AmplitudeConfirmationResponse response =
                    objectMapper.readValue(payload, AmplitudeConfirmationResponse.class);
            return mapper.toBankResult(validator.validate(response));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Payment confirmation response is invalid", exception);
        }
    }
}
