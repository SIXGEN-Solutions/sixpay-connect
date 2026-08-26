package com.sixpay.payment.infrastructure.tresorpay;

import java.time.Instant;

@FunctionalInterface
public interface TresorPayNonceStore {
    boolean registerIfAbsent(String partnerId, String nonce, Instant expiresAt);
}
