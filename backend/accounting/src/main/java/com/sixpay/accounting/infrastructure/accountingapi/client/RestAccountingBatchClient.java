package com.sixpay.accounting.infrastructure.accountingapi.client;

import com.sixpay.accounting.application.exception.AccountingProviderAuthenticationException;
import com.sixpay.accounting.application.exception.AccountingProviderInvalidResponseException;
import com.sixpay.accounting.application.exception.AccountingProviderRejectedException;
import com.sixpay.accounting.application.exception.AccountingProviderUnavailableException;
import com.sixpay.accounting.application.exception.AccountingSubmissionOutcomeUnknownException;
import com.sixpay.accounting.application.port.output.AccountingBatchGateway;
import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingProviderBatchResult;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import com.sixpay.accounting.infrastructure.accountingapi.configuration.AccountingApiProperties;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchResponseDto;
import com.sixpay.accounting.infrastructure.accountingapi.mapper.AccountingApiMapper;
import com.sixpay.accounting.infrastructure.accountingapi.validation.AccountingApiResponseValidator;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RestAccountingBatchClient
        implements AccountingBatchGateway {

    private final RestClient restClient;
    private final AccountingApiAccessTokenProvider tokenProvider;
    private final AccountingApiProperties properties;
    private final AccountingApiMapper mapper;
    private final AccountingApiResponseValidator validator;
    private final ObjectMapper objectMapper;
    private final AccountingBatchRepository batchRepository;

    public RestAccountingBatchClient(
            RestClient restClient,
            AccountingApiAccessTokenProvider tokenProvider,
            AccountingApiProperties properties,
            AccountingApiMapper mapper,
            AccountingApiResponseValidator validator,
            ObjectMapper objectMapper,
            AccountingBatchRepository batchRepository
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
        this.validator = Objects.requireNonNull(validator);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.batchRepository = Objects.requireNonNull(batchRepository);
    }

    @Override
    public AccountingProviderBatchResult submit(
            AccountingBatch batch,
            AccountingIntegrationContext context
    ) {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(context, "context");

        var request = mapper.toRequest(batch);

        String accessToken = tokenProvider.accessToken();

        try {
            String payload = restClient.post()
                    .uri(properties.submitPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(
                            IntegrationHttpHeaders.CORRELATION_ID,
                            context.correlationId().value()
                    )
                    .header(
                            IntegrationHttpHeaders.REQUEST_ID,
                            context.requestId().toString()
                    )
                    .header(
                            "X-Financial-Institution-Code",
                            batch.financialInstitutionCode()
                    )
                    .header(
                            properties.contract().idempotencyHeader(),
                            batch.idempotencyKey().value()
                    )
                    .body(request)
                    .retrieve()
                    .body(String.class);

            if (payload == null || payload.isBlank()) {
                throw new AccountingSubmissionOutcomeUnknownException(
                        "Accounting submission response is empty",
                        null
                );
            }

            try {
                AccountingBatchResponseDto response =
                        objectMapper.readValue(
                                payload,
                                AccountingBatchResponseDto.class
                        );

                return mapper.toResult(
                        validator.validate(
                                response,
                                batch.batchId()
                        ),
                        batch
                );
            } catch (RuntimeException exception) {
                throw new AccountingSubmissionOutcomeUnknownException(
                        "Accounting submission response cannot be trusted",
                        exception
                );
            }
        } catch (ResourceAccessException exception) {
            throw new AccountingSubmissionOutcomeUnknownException(
                    "Accounting submission outcome is unknown",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 401 || status == 403) {
                throw new AccountingProviderAuthenticationException(
                        "Accounting API authentication failed",
                        exception
                );
            }

            if (status == 429 || status >= 500) {
                throw new AccountingSubmissionOutcomeUnknownException(
                        "Accounting submission outcome is unknown",
                        exception
                );
            }

            throw new AccountingProviderRejectedException(
                    "Accounting batch submission was rejected",
                    status,
                    exception
            );
        }
    }

    @Override
    public Optional<AccountingProviderBatchResult> findByBatchId(
            AccountingBatchId batchId,
            AccountingIntegrationContext context
    ) {
        return lookup(
                properties.batchLookupPath(),
                batchId.value().toString(),
                batchId,
                null,
                context
        );
    }

    @Override
    public Optional<AccountingProviderBatchResult>
    findByIdempotencyKey(
            AccountingBatchIdempotencyKey idempotencyKey,
            AccountingIntegrationContext context
    ) {
        return lookup(
                properties.idempotencyLookupPath(),
                idempotencyKey.value(),
                null,
                idempotencyKey,
                context
        );
    }

    private AccountingBatch resolveLocalBatch(
            UUID providerBatchId,
            AccountingBatchIdempotencyKey expectedIdempotencyKey
    ) {
        if (expectedIdempotencyKey != null) {
            return batchRepository
                    .findByIdempotencyKey(expectedIdempotencyKey)
                    .filter(batch -> batch.batchId().value().equals(providerBatchId))
                    .orElseThrow(() ->
                            new AccountingProviderInvalidResponseException(
                                    "Accounting lookup response cannot be correlated to the durable local batch",
                                    null
                            )
                    );
        }

        return batchRepository
                .findById(new AccountingBatchId(providerBatchId))
                .orElseThrow(() ->
                        new AccountingProviderInvalidResponseException(
                                "Accounting lookup response cannot be correlated to the durable local batch",
                                null
                        )
                );
    }

    private Optional<AccountingProviderBatchResult> lookup(
            String path,
            String value,
            AccountingBatchId expectedBatchId,
            AccountingBatchIdempotencyKey expectedIdempotencyKey,
            AccountingIntegrationContext context
    ) {
        Objects.requireNonNull(context, "context");
        String accessToken = tokenProvider.accessToken();

        try {
            String payload = restClient.get()
                    .uri(path, value)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
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
                    .body(String.class);

            if (payload == null || payload.isBlank()) {
                throw new AccountingProviderInvalidResponseException(
                        "Accounting lookup response is empty",
                        null
                );
            }

            AccountingBatchResponseDto response =
                    objectMapper.readValue(
                            payload,
                            AccountingBatchResponseDto.class
                    );

            AccountingBatchResponseDto validated =
                    validator.validate(
                            response,
                            expectedBatchId
                    );

            AccountingBatch localBatch =
                    resolveLocalBatch(
                            validated.batchId(),
                            expectedIdempotencyKey
                    );

            Map<String, UUID> paymentIds =
                    localBatch.items().stream()
                            .collect(
                                    Collectors.toUnmodifiableMap(
                                            item -> item.publicPaymentReference(),
                                            item -> item.paymentId()
                                    )
                            );

            return Optional.of(
                    mapper.toLookupResult(
                            validated,
                            localBatch.idempotencyKey(),
                            paymentIds
                    )
            );
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (ResourceAccessException exception) {
            throw new AccountingProviderUnavailableException(
                    "Accounting API lookup is unavailable",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 401 || status == 403) {
                throw new AccountingProviderAuthenticationException(
                        "Accounting API authentication failed",
                        exception
                );
            }

            if (status == 429 || status >= 500) {
                throw new AccountingProviderUnavailableException(
                        "Accounting API lookup is unavailable",
                        exception
                );
            }

            throw new AccountingProviderRejectedException(
                    "Accounting lookup was rejected",
                    status,
                    exception
            );
        } catch (AccountingProviderInvalidResponseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AccountingProviderInvalidResponseException(
                    "Accounting lookup response is invalid",
                    exception
            );
        }
    }
}
