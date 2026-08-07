package com.sixpay.accounting.infrastructure.accountingapi;

import com.sixpay.accounting.infrastructure.accountingapi.configuration.AccountingApiProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountingApiPropertiesTest {

    @Test
    void rejectsNonHttpsExternalBaseUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountingApiProperties(
                        false,
                        URI.create(
                                "http://accounting.internal"
                        ),
                        "/v1/accounting/batches",
                        "/v1/accounting/batches/{batchId}",
                        "/v1/accounting/batches/"
                                + "by-idempotency-key/{idempotencyKey}",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        new AccountingApiProperties.Security(
                                "accounting-api",
                                "accounting-api-client"
                        ),
                        new AccountingApiProperties.Contract(
                                "Idempotency-Key"
                        )
                )
        );
    }

    @Test
    void rejectsReadTimeoutShorterThanConnectTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountingApiProperties(
                        false,
                        URI.create(
                                "https://accounting.internal"
                        ),
                        "/v1/accounting/batches",
                        "/v1/accounting/batches/{batchId}",
                        "/v1/accounting/batches/"
                                + "by-idempotency-key/{idempotencyKey}",
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(2),
                        new AccountingApiProperties.Security(
                                "accounting-api",
                                "accounting-api-client"
                        ),
                        new AccountingApiProperties.Contract(
                                "Idempotency-Key"
                        )
                )
        );
    }
}
