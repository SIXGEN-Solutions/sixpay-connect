package com.sixpay.payment.infrastructure.banking.amplitude.client;

import com.sixpay.integration.error.ExternalErrorCategory;
import com.sixpay.integration.error.ExternalFailure;
import com.sixpay.integration.error.ExternalIntegrationException;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.integration.messaging.json.IntegrationJsonSerializer;
import com.sixpay.integration.resilience.IntegrationOperationType;
import com.sixpay.integration.resilience.RetryingIntegrationExecutor;
import com.sixpay.payment.application.port.output.banking.FundsGateway;
import com.sixpay.payment.application.port.output.banking.VerificationGateway;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.AmplitudeAccountFundsClient;
import com.sixpay.payment.infrastructure.banking.amplitude.configuration.AmplitudePaymentBankingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.dto.*;
import com.sixpay.payment.infrastructure.banking.amplitude.mapper.AmplitudeAccountFundsMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.validation.AmplitudeAccountFundsResponseValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;
import java.util.UUID;

public final class RestAmplitudeAccountFundsClient
        implements AmplitudeAccountFundsClient {

    private static final String INTEGRATION_ID =
            "payment-core-banking-amplitude";

    private final RestClient restClient;
    private final PaymentCoreBankingAccessTokenProvider tokenProvider;
    private final AmplitudePaymentBankingProperties properties;
    private final IntegrationJsonSerializer serializer;
    private final AmplitudeAccountFundsResponseValidator validator;
    private final AmplitudeAccountFundsMapper mapper;
    private final RetryingIntegrationExecutor retryingExecutor;

    public RestAmplitudeAccountFundsClient(
            RestClient restClient,
            PaymentCoreBankingAccessTokenProvider tokenProvider,
            AmplitudePaymentBankingProperties properties,
            IntegrationJsonSerializer serializer,
            AmplitudeAccountFundsResponseValidator validator,
            AmplitudeAccountFundsMapper mapper,
            RetryingIntegrationExecutor retryingExecutor
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.serializer = Objects.requireNonNull(serializer);
        this.validator = Objects.requireNonNull(validator);
        this.mapper = Objects.requireNonNull(mapper);
        this.retryingExecutor = Objects.requireNonNull(retryingExecutor);
    }

    @Override
    public BankingVerificationSnapshot verifyCustomerAndAccount(
            VerificationGateway.VerificationRequest request
    ) {
        return retryingExecutor.execute(
                IntegrationOperationType.READ_ONLY,
                () -> {
                    String body = post(
                            properties.verificationPath(),
                            mapper.toExternal(request),
                            request.context()
                                    .correlationId()
                                    .value(),
                            "verify-customer-account"
                    );

                    AmplitudeAccountVerificationResponse response =
                            serializer.deserialize(
                                    body,
                                    AmplitudeAccountVerificationResponse.class
                            );

                    return mapper.toVerificationSnapshot(
                            request,
                            validator.validate(response)
                    );
                }
        );
    }

    @Override
    public FundsControlSnapshot checkPaymentExecution(
            FundsGateway.FundsCheckRequest request
    ) {
        return retryingExecutor.execute(
                IntegrationOperationType.READ_ONLY,
                () -> {
                    String body = post(
                            properties.fundsPath(),
                            mapper.toExternal(request),
                            request.context()
                                    .correlationId()
                                    .value(),
                            "check-account-funds"
                    );

                    AmplitudeFundsCheckResponse response =
                            serializer.deserialize(
                                    body,
                                    AmplitudeFundsCheckResponse.class
                            );

                    return mapper.toFundsSnapshot(
                            request,
                            validator.validate(response)
                    );
                }
        );
    }

    private String post(
            String path,
            Object request,
            String correlationId,
            String operation
    ) {
        try {
            String body = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + tokenProvider.accessToken()
                    )
                    .header(
                            IntegrationHttpHeaders.CORRELATION_ID,
                            correlationId
                    )
                    .header(
                            IntegrationHttpHeaders.REQUEST_ID,
                            UUID.randomUUID().toString()
                    )
                    .body(request)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                throw failure(
                        operation,
                        correlationId,
                        ExternalErrorCategory.INVALID_RESPONSE,
                        null,
                        false,
                        "Core Banking response is empty",
                        null
                );
            }
            return body;
        } catch (RestClientResponseException exception) {
            throw httpFailure(
                    operation,
                    correlationId,
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw failure(
                    operation,
                    correlationId,
                    ExternalErrorCategory.TIMEOUT,
                    null,
                    true,
                    "Core Banking request timed out or connection failed",
                    exception
            );
        } catch (ExternalIntegrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(
                    operation,
                    correlationId,
                    ExternalErrorCategory.INVALID_RESPONSE,
                    null,
                    false,
                    "Core Banking response could not be processed",
                    exception
            );
        }
    }

    private ExternalIntegrationException httpFailure(
            String operation,
            String correlationId,
            RestClientResponseException exception
    ) {
        int status = exception.getStatusCode().value();

        ExternalErrorCategory category;
        boolean retryable;

        if (status == 401) {
            category = ExternalErrorCategory.AUTHENTICATION;
            retryable = false;
        } else if (status == 403) {
            category = ExternalErrorCategory.AUTHORIZATION;
            retryable = false;
        } else if (status == 429) {
            category = ExternalErrorCategory.RATE_LIMIT;
            retryable = true;
        } else if (status >= 500) {
            category = ExternalErrorCategory.UNAVAILABLE;
            retryable = true;
        } else {
            category = ExternalErrorCategory.PROTOCOL;
            retryable = false;
        }

        return failure(
                operation,
                correlationId,
                category,
                status,
                retryable,
                "Core Banking returned an unsuccessful response",
                exception
        );
    }

    private ExternalIntegrationException failure(
            String operation,
            String correlationId,
            ExternalErrorCategory category,
            Integer httpStatus,
            boolean retryable,
            String safeMessage,
            Throwable cause
    ) {
        return new ExternalIntegrationException(
                new ExternalFailure(
                        INTEGRATION_ID,
                        "AMPLITUDE",
                        operation,
                        category,
                        null,
                        httpStatus,
                        retryable,
                        false,
                        correlationId,
                        safeMessage
                ),
                cause
        );
    }
}
