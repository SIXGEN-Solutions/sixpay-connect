package com.sixpay.payment.infrastructure.partner;

import java.time.Instant;

@FunctionalInterface
public interface PartnerNonceStore {
    boolean registerIfAbsent(String partnerId, String nonce, Instant expiresAt);
}
