package com.sixpay.payment.infrastructure.partner;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class FixedWindowPartnerRateLimiter
        implements PartnerRateLimiter {

    private final Clock clock;
    private final int limit;
    private final Map<String, Window> windows =
            new ConcurrentHashMap<>();

    public FixedWindowPartnerRateLimiter(Clock clock, int limit) {
        this.clock = Objects.requireNonNull(clock);
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
    }

    @Override
    public RateLimitDecision acquire(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("partnerId is required");
        }

        Instant now = clock.instant();
        long minute = now.getEpochSecond() / 60L;

        Window window = windows.compute(
                partnerId.strip(),
                (key, existing) -> {
                    if (existing == null || existing.minute() != minute) {
                        return new Window(minute, 1);
                    }
                    return new Window(existing.minute(), existing.count() + 1);
                }
        );

        if (window.count() <= limit) {
            return RateLimitDecision.permit();
        }

        int retryAfterSeconds =
                (int) (60L - (now.getEpochSecond() % 60L));

        return RateLimitDecision.reject(retryAfterSeconds);
    }

    private record Window(long minute, int count) { }
}
