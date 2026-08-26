package com.sixpay.integration.resilience;

import com.sixpay.integration.error.ExternalIntegrationException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class RetryingIntegrationExecutor {
    private final RetryPolicy policy;
    private final RetryDecider decider;
    private final RetrySleeper sleeper;
    public RetryingIntegrationExecutor(RetryPolicy policy, RetryDecider decider, RetrySleeper sleeper) {
        this.policy = Objects.requireNonNull(policy);
        this.decider = Objects.requireNonNull(decider);
        this.sleeper = Objects.requireNonNull(sleeper);
    }
    public <T> T execute(IntegrationOperationType type, Operation<T> operation) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(operation);
        int attempt = 1;
        while (true) {
            try {
                return operation.execute();
            } catch (ExternalIntegrationException failure) {
                if (attempt >= policy.maxAttempts() || !decider.shouldRetry(type, failure, attempt)) throw failure;
                sleeper.sleep(withJitter(policy.backoffForAttempt(attempt)));
                attempt++;
            }
        }
    }
    private Duration withJitter(Duration base) {
        if (policy.jitterRatio() == 0) return base;
        double factor = ThreadLocalRandom.current().nextDouble(
                1.0 - policy.jitterRatio(), 1.0 + policy.jitterRatio());
        return Duration.ofNanos(Math.max(1L, Math.round(base.toNanos() * factor)));
    }
    @FunctionalInterface public interface Operation<T> { T execute(); }
}
