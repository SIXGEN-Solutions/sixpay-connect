package com.sixpay.payment.performance;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PaymentVirtualThreadsTest {

    private static final int TASK_COUNT = 10_000;

    @Test
    void executesTenThousandPaymentStyleTasksOnVirtualThreads() {
        assertTimeoutPreemptively(
                Duration.ofSeconds(30),
                () -> {
                    try (var executor =
                                 Executors
                                         .newVirtualThreadPerTaskExecutor()) {

                        List<Callable<Boolean>> tasks =
                                new ArrayList<>(TASK_COUNT);

                        for (int index = 0;
                             index < TASK_COUNT;
                             index++) {
                            tasks.add(() ->
                                    Thread.currentThread()
                                            .isVirtual()
                            );
                        }

                        List<Future<Boolean>> futures =
                                executor.invokeAll(tasks);

                        assertThat(futures)
                                .hasSize(TASK_COUNT)
                                .allSatisfy(future ->
                                        assertThat(future.get())
                                                .isTrue()
                                );
                    }
                }
        );
    }
}
