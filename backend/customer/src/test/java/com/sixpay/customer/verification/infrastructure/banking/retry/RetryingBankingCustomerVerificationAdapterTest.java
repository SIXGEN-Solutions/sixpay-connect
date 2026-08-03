package com.sixpay.customer.verification.infrastructure.banking.retry;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.application.exception.BankingVerificationAuthenticationException;
import com.sixpay.customer.verification.application.exception.BankingVerificationUnavailableException;
import com.sixpay.customer.verification.application.port.output.BankingAccountAccessReference;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.output.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerIdentity;
import com.sixpay.customer.verification.domain.model.CustomerNiu;
import com.sixpay.customer.verification.domain.model.CustomerVerificationContext;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.CustomerVerificationSubject;
import com.sixpay.customer.verification.domain.model.FinancialInstitutionCode;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.observability.BankingVerificationObservation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryingBankingCustomerVerificationAdapterTest {

    @Test
    void retriesRetryableFailureUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger sleeps = new AtomicInteger();

        BankingCustomerVerificationPort delegate = query -> {
            if (calls.incrementAndGet() < 3) {
                throw new BankingVerificationUnavailableException(
                        "temporary",
                        null
                );
            }
            return successfulResponse();
        };

        var adapter = adapter(
                delegate,
                duration -> sleeps.incrementAndGet()
        );

        BankingVerificationResponse result =
                adapter.verify(query());

        assertEquals(3, calls.get());
        assertEquals(2, sleeps.get());
        assertEquals(11, result.checks().size());
    }

    @Test
    void doesNotRetryNonRetryableFailure() {
        AtomicInteger calls = new AtomicInteger();

        BankingCustomerVerificationPort delegate = query -> {
            calls.incrementAndGet();
            throw new BankingVerificationAuthenticationException(
                    "unauthorized",
                    null
            );
        };

        var adapter = adapter(
                delegate,
                duration -> {
                    throw new AssertionError(
                            "sleep must not be called"
                    );
                }
        );

        assertThrows(
                BankingVerificationAuthenticationException.class,
                () -> adapter.verify(query())
        );
        assertEquals(1, calls.get());
    }

    @Test
    void neverRetriesBusinessFailResponse() {
        AtomicInteger calls = new AtomicInteger();

        BankingCustomerVerificationPort delegate = query -> {
            calls.incrementAndGet();
            return successfulResponse();
        };

        var adapter = adapter(
                delegate,
                duration -> {
                    throw new AssertionError(
                            "business response must not retry"
                    );
                }
        );

        adapter.verify(query());

        assertEquals(1, calls.get());
    }

    @Test
    void stopsAfterConfiguredMaximumAttempts() {
        AtomicInteger calls = new AtomicInteger();

        BankingCustomerVerificationPort delegate = query -> {
            calls.incrementAndGet();
            throw new BankingVerificationUnavailableException(
                    "temporary",
                    null
            );
        };

        var adapter = adapter(
                delegate,
                duration -> {
                }
        );

        assertThrows(
                BankingVerificationUnavailableException.class,
                () -> adapter.verify(query())
        );
        assertEquals(3, calls.get());
    }

    private static RetryingBankingCustomerVerificationAdapter adapter(
            BankingCustomerVerificationPort delegate,
            RetrySleeper sleeper
    ) {
        return new RetryingBankingCustomerVerificationAdapter(
                delegate,
                properties(),
                sleeper,
                new BankingVerificationObservation(
                        new SimpleMeterRegistry()
                )
        );
    }

    private static BankingVerificationQuery query() {
        return new BankingVerificationQuery(
                new CustomerVerificationId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(
                                CustomerNiu.of("M0123456"),
                                "Ada Lovelace"
                        )
                ),
                FinancialInstitutionCode.of("AMPLITUDE"),
                AccountBindingFingerprint.of(
                        "v1:" + "a".repeat(64)
                ),
                BankingAccountAccessReference.of(
                        "AMP-ACC-000123"
                ),
                CustomerVerificationContext.of(
                        CorrelationId.of("corr-4.3.4"),
                        null
                ),
                Instant.parse("2026-08-03T17:00:00Z")
        );
    }

    private static BankingVerificationResponse successfulResponse() {
        return BankingVerificationResponse.of(
                Arrays.stream(VerificationCheckType.values())
                        .map(VerificationCheck::passed)
                        .toList(),
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "a".repeat(64)
                ),
                Instant.parse("2026-08-03T17:00:01Z"),
                Instant.parse("2026-08-03T17:05:01Z")
        );
    }

    private static BankingVerificationProperties properties() {
        return new BankingVerificationProperties(
                URI.create("https://core-banking.internal"),
                "/v1/accounts/verify",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                3,
                Duration.ofMillis(1),
                Duration.ofMinutes(5),
                new BankingVerificationProperties.Security(
                        "core-banking",
                        "core-banking-client"
                )
        );
    }
}
