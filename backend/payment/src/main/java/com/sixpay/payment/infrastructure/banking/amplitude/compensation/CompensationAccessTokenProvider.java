package com.sixpay.payment.infrastructure.banking.amplitude.compensation;

@FunctionalInterface
public interface CompensationAccessTokenProvider {
    String accessToken();
}
