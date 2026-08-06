package com.sixpay.integration.resilience;

import com.sixpay.integration.error.ExternalErrorCategory;
import com.sixpay.integration.error.ExternalFailure;
import com.sixpay.integration.error.ExternalIntegrationException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryingIntegrationExecutorTest {
    @Test
    void retriesTransientReadOnlyFailure() {
        AtomicInteger attempts = new AtomicInteger();
        RetryingIntegrationExecutor executor =
                new RetryingIntegrationExecutor(
                        new RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(1), 0),
                        RetryDecider.safeDefault(),
                        ignored -> { }
                );
        String result = executor.execute(
                IntegrationOperationType.READ_ONLY,
                () -> {
                    if (attempts.incrementAndGet() < 3) throw failure(false);
                    return "ok";
                }
        );
        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void neverBlindRetriesUnknownFinancialOutcome() {
        AtomicInteger attempts = new AtomicInteger();
        RetryingIntegrationExecutor executor =
                new RetryingIntegrationExecutor(
                        RetryPolicy.DEFAULT,
                        RetryDecider.safeDefault(),
                        ignored -> { }
                );
        assertThatThrownBy(() -> executor.execute(
                IntegrationOperationType.FINANCIAL_COMMAND,
                () -> {
                    attempts.incrementAndGet();
                    throw failure(true);
                }
        )).isInstanceOf(ExternalIntegrationException.class);
        assertThat(attempts).hasValue(1);
    }

    private static ExternalIntegrationException failure(boolean unknown) {
        return new ExternalIntegrationException(new ExternalFailure(
                "test", "provider", "operation",
                unknown ? ExternalErrorCategory.OUTCOME_UNKNOWN : ExternalErrorCategory.UNAVAILABLE,
                null, 503, true, unknown, "correlation", "safe failure"
        ));
    }
}
