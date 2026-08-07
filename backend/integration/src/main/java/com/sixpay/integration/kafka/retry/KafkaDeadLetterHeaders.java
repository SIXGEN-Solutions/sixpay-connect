package com.sixpay.integration.kafka.retry;

public final class KafkaDeadLetterHeaders {

    public static final String ORIGINAL_TOPIC =
            "sixpay-original-topic";
    public static final String ORIGINAL_PARTITION =
            "sixpay-original-partition";
    public static final String ORIGINAL_OFFSET =
            "sixpay-original-offset";
    public static final String RETRY_COUNT =
            "sixpay-retry-count";
    public static final String FAILURE_CATEGORY =
            "sixpay-failure-category";
    public static final String FAILURE_CODE =
            "sixpay-failure-code";
    public static final String FIRST_FAILURE_AT =
            "sixpay-first-failure-at";
    public static final String LAST_FAILURE_AT =
            "sixpay-last-failure-at";
    public static final String CONSUMER_GROUP =
            "sixpay-consumer-group";

    private KafkaDeadLetterHeaders() {
    }
}
