package com.sixpay.payment.infrastructure.tresorpay;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTresorPayNonceStore implements TresorPayNonceStore {
    private final Clock clock;
    private final Map<String, Instant> entries = new ConcurrentHashMap<>();

    public InMemoryTresorPayNonceStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean registerIfAbsent(String partnerId, String nonce, Instant expiresAt) {
        cleanup();
        return entries.putIfAbsent(partnerId + ":" + nonce, expiresAt) == null;
    }

    private void cleanup() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
