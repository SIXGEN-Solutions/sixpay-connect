package com.sixpay.accounting.infrastructure.accountingapi;

import com.sixpay.accounting.infrastructure.accountingapi.configuration.AccountingApiProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountingApiPropertiesTest {

    @Test
    void acceptsApprovedT14ContractPaths() {
        AccountingApiProperties properties =
                new AccountingApiProperties(
                        true,
                        URI.create("https://accounting.internal"),
                        "/api/v1/accounting-entries",
                        "/api/v1/accounting-entries/batches/{batchId}",
                        "/api/v1/accounting-entries/idempotency/{idempotencyKey}",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        new AccountingApiProperties.Security(
                                "accounting-api",
                                "accounting-api-client"
                        ),
                        new AccountingApiProperties.Contract(
                                "Idempotency-Key"
                        )
                );

        assertEquals(
                "/api/v1/accounting-entries",
                properties.submitPath()
        );
    }

    @Test
    void rejectsNonHttpsExternalBaseUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountingApiProperties(
                        false,
                        URI.create("http://accounting.internal"),
                        "/api/v1/accounting-entries",
                        "/api/v1/accounting-entries/batches/{batchId}",
                        "/api/v1/accounting-entries/idempotency/{idempotencyKey}",
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
                        URI.create("https://accounting.internal"),
                        "/api/v1/accounting-entries",
                        "/api/v1/accounting-entries/batches/{batchId}",
                        "/api/v1/accounting-entries/idempotency/{idempotencyKey}",
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
