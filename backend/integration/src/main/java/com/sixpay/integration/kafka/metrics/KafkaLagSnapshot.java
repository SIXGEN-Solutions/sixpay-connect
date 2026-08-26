package com.sixpay.integration.kafka.metrics;

public record KafkaLagSnapshot(
        String consumerGroup,
        String topic,
        int partition,
        long currentOffset,
        long endOffset
) {
    public KafkaLagSnapshot {
        if (currentOffset < 0 || endOffset < 0) {
            throw new IllegalArgumentException(
                    "Offsets must be non-negative"
            );
        }
    }

    public long lag() {
        return Math.max(0, endOffset - currentOffset);
    }
}
