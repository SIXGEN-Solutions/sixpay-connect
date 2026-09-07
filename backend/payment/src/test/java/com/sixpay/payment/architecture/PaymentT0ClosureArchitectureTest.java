package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentT0ClosureArchitectureTest {

    private static final Path MAIN =
            Path.of("src/main/java/com/sixpay/payment");

    @Test
    void recoveryUseCaseNeverReferencesFinancialExecutionPort()
            throws IOException {
        String source = Files.readString(
                MAIN.resolve(
                        "application/service/PaymentT0RecoveryService.java"
                )
        );

        assertThat(source)
                .doesNotContain("PaymentEventExecutionPort")
                .doesNotContain(".execute(")
                .contains(".recover(");
    }

    @Test
    void lifecycleRecoveryUsesRecoveryPortAndNeverReplaysExecutionPost()
            throws IOException {
        String source = Files.readString(
                MAIN.resolve(
                        "application/service/"
                                + "PaymentEventLifecycleOrchestrationService.java"
                )
        );

        int recoverStart = source.indexOf(
                "public PaymentWorkflowResult recover("
        );
        int recordUnknownStart = source.indexOf(
                "private PaymentWorkflowResult recordUnknown("
        );

        assertThat(recoverStart).isGreaterThanOrEqualTo(0);
        assertThat(recordUnknownStart).isGreaterThan(recoverStart);

        String recoverMethod =
                source.substring(recoverStart, recordUnknownStart);

        assertThat(recoverMethod)
                .contains("recoveryPort.recover(")
                .contains("POSTING_OUTCOME_UNKNOWN")
                .doesNotContain("executionPort.execute(");
    }

    @Test
    void amplitudeRecoveryRemainsReferenceThenIdempotencyLookup()
            throws IOException {
        String source = Files.readString(
                MAIN.resolve(
                        "infrastructure/banking/amplitude/posting/"
                                + "AmplitudePaymentEventRecoveryAdapter.java"
                )
        );

        int byReference = source.indexOf(
                "client.findByPaymentReference("
        );
        int byIdempotency = source.indexOf(
                "client.findByIdempotencyKey("
        );

        assertThat(byReference).isGreaterThanOrEqualTo(0);
        assertThat(byIdempotency).isGreaterThan(byReference);
        assertThat(source).doesNotContain("execute(");
    }
}
