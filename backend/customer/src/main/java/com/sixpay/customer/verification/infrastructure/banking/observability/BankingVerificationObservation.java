package com.sixpay.customer.verification.infrastructure.banking.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Low-cardinality metrics for Core Banking verification calls.
 */
public final class BankingVerificationObservation {

    private static final String REQUESTS =
            "sixpay.customer.verification.banking.requests";
    private static final String DURATION =
            "sixpay.customer.verification.banking.duration";
    private static final String RETRIES =
            "sixpay.customer.verification.banking.retries";
    private static final String ERRORS =
            "sixpay.customer.verification.banking.errors";

    private final MeterRegistry meterRegistry;

    public BankingVerificationObservation(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    public long start() {
        return System.nanoTime();
    }

    public void success(
            String institution,
            int attempts,
            long startedAt
    ) {
        counter(
                REQUESTS,
                institution,
                "success"
        ).increment();

        recordDuration(
                institution,
                "success",
                startedAt
        );

        if (attempts > 1) {
            retryCounter(institution).increment(attempts - 1L);
        }
    }

    public void failure(
            String institution,
            String errorType,
            int attempts,
            long startedAt
    ) {
        counter(
                REQUESTS,
                institution,
                "failure"
        ).increment();

        Counter.builder(ERRORS)
                .tag("institution", safeInstitution(institution))
                .tag("error_type", safeErrorType(errorType))
                .register(meterRegistry)
                .increment();

        recordDuration(
                institution,
                "failure",
                startedAt
        );

        if (attempts > 1) {
            retryCounter(institution).increment(attempts - 1L);
        }
    }

    private Counter counter(
            String name,
            String institution,
            String outcome
    ) {
        return Counter.builder(name)
                .tag("institution", safeInstitution(institution))
                .tag("outcome", outcome)
                .register(meterRegistry);
    }

    private Counter retryCounter(String institution) {
        return Counter.builder(RETRIES)
                .tag("institution", safeInstitution(institution))
                .register(meterRegistry);
    }

    private void recordDuration(
            String institution,
            String outcome,
            long startedAt
    ) {
        Timer.builder(DURATION)
                .tag("institution", safeInstitution(institution))
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(
                        System.nanoTime() - startedAt,
                        TimeUnit.NANOSECONDS
                );
    }

    private static String safeInstitution(String institution) {
        return institution == null || institution.isBlank()
                ? "unknown"
                : institution;
    }

    private static String safeErrorType(String errorType) {
        return errorType == null || errorType.isBlank()
                ? "unknown"
                : errorType;
    }
}
