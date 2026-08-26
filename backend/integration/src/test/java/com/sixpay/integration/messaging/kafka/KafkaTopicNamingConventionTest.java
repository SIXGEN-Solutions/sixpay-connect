package com.sixpay.integration.messaging.kafka;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicNamingConventionTest {
    private final KafkaTopicNamingConvention convention = new KafkaTopicNamingConvention();

    @Test
    void createsVersionedTopic() {
        assertThat(convention.topic("dev", "payment", "lifecycle", 1))
                .isEqualTo("sixpay.dev.payment.lifecycle.v1");
    }

    @Test
    void createsRetryAndDlqTopics() {
        String topic = "sixpay.dev.payment.lifecycle.v1";
        assertThat(convention.retryTopic(topic, 2)).isEqualTo(topic + ".retry.2");
        assertThat(convention.deadLetterTopic(topic)).isEqualTo(topic + ".dlq");
    }
}
