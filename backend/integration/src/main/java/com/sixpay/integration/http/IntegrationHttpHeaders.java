package com.sixpay.integration.http;

public final class IntegrationHttpHeaders {
    public static final String CORRELATION_ID = "X-Correlation-ID";
    public static final String REQUEST_ID = "X-Request-ID";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String TRACE_PARENT = "traceparent";
    public static final String TRACE_STATE = "tracestate";
    private IntegrationHttpHeaders() { }
}
