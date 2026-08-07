package com.sixpay.integration.kafka.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@ConfigurationProperties(
        prefix = IntegrationKafkaProperties.PREFIX
)
public record IntegrationKafkaProperties(
        boolean enabled,
        String clientId,
        Map<String, String> topics,
        Producer producer,
        Consumer consumer,
        Outbox outbox,
        Retry retry,
        Retention retention
) {
    public static final String PREFIX =
            "sixpay.integration.kafka";

    public IntegrationKafkaProperties {
        clientId = required(clientId, "clientId");
        topics = Map.copyOf(
                Objects.requireNonNull(topics, "topics")
        );
        producer = Objects.requireNonNull(
                producer,
                "producer"
        );
        consumer = Objects.requireNonNull(
                consumer,
                "consumer"
        );
        outbox = Objects.requireNonNull(
                outbox,
                "outbox"
        );
        retry = Objects.requireNonNull(
                retry,
                "retry"
        );
        retention = Objects.requireNonNull(
                retention,
                "retention"
        );
    }

    public record Producer(
            boolean idempotence,
            String acks,
            int retries,
            int maxInFlightRequests
    ) {
        public Producer {
            acks = required(acks, "producer.acks");
            if (retries < 0) {
                throw new IllegalArgumentException(
                        "producer.retries must be >= 0"
                );
            }
            if (maxInFlightRequests < 1) {
                throw new IllegalArgumentException(
                        "producer.maxInFlightRequests must be >= 1"
                );
            }
        }
    }

    public record Consumer(
            String autoOffsetReset,
            boolean enableAutoCommit,
            int concurrency,
            Duration pollTimeout
    ) {
        public Consumer {
            autoOffsetReset = required(
                    autoOffsetReset,
                    "consumer.autoOffsetReset"
            );
            if (concurrency < 1) {
                throw new IllegalArgumentException(
                        "consumer.concurrency must be >= 1"
                );
            }
            pollTimeout = positive(
                    pollTimeout,
                    "consumer.pollTimeout"
            );
        }
    }

    public record Outbox(
            int batchSize,
            Duration pollInterval,
            Duration deliveredRetention,
            int cleanupBatchSize
    ) {
        public Outbox {
            if (batchSize < 1
                    || cleanupBatchSize < 1) {
                throw new IllegalArgumentException(
                        "Outbox batch sizes must be >= 1"
                );
            }
            pollInterval = positive(
                    pollInterval,
                    "outbox.pollInterval"
            );
            deliveredRetention = positive(
                    deliveredRetention,
                    "outbox.deliveredRetention"
            );
        }
    }

    public record Retry(
            Duration firstDelay,
            Duration secondDelay,
            Duration thirdDelay,
            int maximumAttempts,
            String deadLetterSuffix
    ) {
        public Retry {
            firstDelay = positive(
                    firstDelay,
                    "retry.firstDelay"
            );
            secondDelay = positive(
                    secondDelay,
                    "retry.secondDelay"
            );
            thirdDelay = positive(
                    thirdDelay,
                    "retry.thirdDelay"
            );
            if (maximumAttempts < 1) {
                throw new IllegalArgumentException(
                        "retry.maximumAttempts must be >= 1"
                );
            }
            deadLetterSuffix = required(
                    deadLetterSuffix,
                    "retry.deadLetterSuffix"
            );
        }
    }

    public record Retention(
            Duration main,
            Duration reconciliation,
            Duration retry,
            Duration deadLetter,
            Duration deduplication
    ) {
        public Retention {
            main = positive(main, "retention.main");
            reconciliation = positive(
                    reconciliation,
                    "retention.reconciliation"
            );
            retry = positive(
                    retry,
                    "retention.retry"
            );
            deadLetter = positive(
                    deadLetter,
                    "retention.deadLetter"
            );
            deduplication = positive(
                    deduplication,
                    "retention.deduplication"
            );

            if (deduplication.compareTo(deadLetter) < 0) {
                throw new IllegalArgumentException(
                        "Deduplication retention must be "
                                + "greater than or equal to DLQ retention"
                );
            }
        }
    }

    private static Duration positive(
            Duration value,
            String name
    ) {
        if (value == null
                || value.isZero()
                || value.isNegative()) {
            throw new IllegalArgumentException(
                    name + " must be positive"
            );
        }
        return value;
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }
        return value.strip();
    }
}
