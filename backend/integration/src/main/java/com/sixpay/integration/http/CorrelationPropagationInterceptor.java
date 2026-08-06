package com.sixpay.integration.http;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

public final class CorrelationPropagationInterceptor implements ClientHttpRequestInterceptor {
    private final Supplier<IntegrationRequestContext> contextSupplier;
    private final RequestIdGenerator requestIdGenerator;
    public CorrelationPropagationInterceptor(
            Supplier<IntegrationRequestContext> contextSupplier,
            RequestIdGenerator requestIdGenerator
    ) {
        this.contextSupplier = Objects.requireNonNull(contextSupplier);
        this.requestIdGenerator = Objects.requireNonNull(requestIdGenerator);
    }
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        IntegrationRequestContext context = contextSupplier.get();
        request.getHeaders().set(IntegrationHttpHeaders.CORRELATION_ID, context.correlationId().value());
        request.getHeaders().set(IntegrationHttpHeaders.REQUEST_ID, requestIdGenerator.generate());
        context.optionalTraceParent().ifPresent(v -> request.getHeaders().set(IntegrationHttpHeaders.TRACE_PARENT, v));
        context.optionalTraceState().ifPresent(v -> request.getHeaders().set(IntegrationHttpHeaders.TRACE_STATE, v));
        return execution.execute(request, body);
    }
}
