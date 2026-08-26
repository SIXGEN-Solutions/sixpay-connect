package com.sixpay.integration.kafka.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public final class KafkaLagMetrics {

    private final MeterRegistry meterRegistry;

    public KafkaLagMetrics(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry
        );
    }

    public void record(
            KafkaLagSnapshot snapshot
    ) {
        meterRegistry.gauge(
                "sixpay.integration.kafka.consumer.lag",
                java.util.List.of(
                        io.micrometer.core.instrument.Tag.of(
                                "consumerGroup",
                                snapshot.consumerGroup()
                        ),
                        io.micrometer.core.instrument.Tag.of(
                                "topic",
                                snapshot.topic()
                        ),
                        io.micrometer.core.instrument.Tag.of(
                                "partition",
                                Integer.toString(
                                        snapshot.partition()
                                )
                        )
                ),
                snapshot.lag()
        );
    }
}
