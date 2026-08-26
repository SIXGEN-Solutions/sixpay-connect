package com.sixpay.integration.http;

import com.sixpay.common.context.CorrelationId;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class CorrelationIdResolver {
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private final Supplier<CorrelationId> generator;
    public CorrelationIdResolver() { this(CorrelationId::generate); }
    public CorrelationIdResolver(Supplier<CorrelationId> generator) {
        this.generator = Objects.requireNonNull(generator);
    }
    public CorrelationId resolve(String candidate) {
        if (candidate == null || candidate.isBlank()) return generator.get();
        String normalized = candidate.strip();
        return SAFE_VALUE.matcher(normalized).matches()
                ? CorrelationId.of(normalized)
                : generator.get();
    }
}
