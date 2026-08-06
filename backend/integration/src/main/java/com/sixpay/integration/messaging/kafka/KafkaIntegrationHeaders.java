package com.sixpay.integration.messaging.kafka;
public final class KafkaIntegrationHeaders {
    public static final String EVENT_ID = "event-id";
    public static final String EVENT_TYPE = "event-type";
    public static final String SCHEMA_VERSION = "schema-version";
    public static final String CORRELATION_ID = "correlation-id";
    public static final String CAUSATION_ID = "causation-id";
    public static final String PRODUCER = "producer";
    public static final String CONTENT_TYPE = "content-type";
    public static final String TRACE_PARENT = "traceparent";
    private KafkaIntegrationHeaders() { }
}
