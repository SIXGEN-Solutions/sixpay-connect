package com.sixpay.accounting.infrastructure.accountingapi;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import com.sixpay.accounting.infrastructure.accountingapi.client.AccountingApiAccessTokenProvider;
import com.sixpay.accounting.infrastructure.accountingapi.client.RestAccountingBatchClient;
import com.sixpay.accounting.infrastructure.accountingapi.configuration.AccountingApiProperties;
import com.sixpay.accounting.infrastructure.accountingapi.mapper.AccountingApiMapper;
import com.sixpay.accounting.infrastructure.accountingapi.validation.AccountingApiResponseValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestAccountingBatchClientRecoveryTest {

    private static final UUID BATCH_UUID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final AccountingBatchIdempotencyKey IDEMPOTENCY_KEY =
            new AccountingBatchIdempotencyKey(
                    "0123456789abcdef0123456789abcdef"
                            + "0123456789abcdef0123456789abcdef"
            );

    @Test
    void lookupByBatchIdRebuildsCorrelationFromDurableRepositoryAfterRestart()
            throws Exception {

        AccountingBatchRepository repository =
                mock(AccountingBatchRepository.class);
        AccountingBatch batch =
                mock(AccountingBatch.class);

        when(
                repository.findById(
                        new AccountingBatchId(BATCH_UUID)
                )
        ).thenReturn(Optional.of(batch));

        RestAccountingBatchClient client =
                client(repository);

        AccountingBatch resolved =
                invokeResolveLocalBatch(
                        client,
                        BATCH_UUID,
                        null
                );

        assertSame(batch, resolved);

        verify(repository).findById(
                new AccountingBatchId(BATCH_UUID)
        );
    }

    @Test
    void lookupByIdempotencyKeyRebuildsCorrelationFromDurableRepositoryAfterRestart()
            throws Exception {

        AccountingBatchRepository repository =
                mock(AccountingBatchRepository.class);
        AccountingBatch batch =
                mock(AccountingBatch.class);

        when(batch.batchId())
                .thenReturn(
                        new AccountingBatchId(BATCH_UUID)
                );
        when(
                repository.findByIdempotencyKey(
                        IDEMPOTENCY_KEY
                )
        ).thenReturn(Optional.of(batch));

        RestAccountingBatchClient client =
                client(repository);

        AccountingBatch resolved =
                invokeResolveLocalBatch(
                        client,
                        BATCH_UUID,
                        IDEMPOTENCY_KEY
                );

        assertSame(batch, resolved);

        verify(repository).findByIdempotencyKey(
                IDEMPOTENCY_KEY
        );
    }

    private static RestAccountingBatchClient client(
            AccountingBatchRepository repository
    ) {
        return new RestAccountingBatchClient(
                mock(RestClient.class),
                mock(AccountingApiAccessTokenProvider.class),
                mock(AccountingApiProperties.class),
                new AccountingApiMapper(),
                new AccountingApiResponseValidator(),
                new ObjectMapper(),
                repository
        );
    }

    private static AccountingBatch invokeResolveLocalBatch(
            RestAccountingBatchClient client,
            UUID providerBatchId,
            AccountingBatchIdempotencyKey idempotencyKey
    ) throws Exception {

        Method method =
                RestAccountingBatchClient.class
                        .getDeclaredMethod(
                                "resolveLocalBatch",
                                UUID.class,
                                AccountingBatchIdempotencyKey.class
                        );

        method.setAccessible(true);

        return (AccountingBatch) method.invoke(
                client,
                providerBatchId,
                idempotencyKey
        );
    }
}
