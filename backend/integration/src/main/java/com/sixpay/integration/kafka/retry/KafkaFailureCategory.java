package com.sixpay.integration.kafka.retry;

public enum KafkaFailureCategory {
    TRANSIENT,
    PERMANENT,
    INVALID_PAYLOAD,
    UNSUPPORTED_SCHEMA,
    SECURITY,
    UNKNOWN
}
