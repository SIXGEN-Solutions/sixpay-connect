package com.sixpay.integration.http;

import java.util.Optional;

public final class IntegrationContextHolder {
    private static final ThreadLocal<IntegrationRequestContext> CONTEXT = new ThreadLocal<>();
    private IntegrationContextHolder() { }
    public static Optional<IntegrationRequestContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }
    public static Scope open(IntegrationRequestContext context) {
        IntegrationRequestContext previous = CONTEXT.get();
        CONTEXT.set(context);
        return () -> {
            if (previous == null) CONTEXT.remove(); else CONTEXT.set(previous);
        };
    }
    @FunctionalInterface
    public interface Scope extends AutoCloseable { @Override void close(); }
}
