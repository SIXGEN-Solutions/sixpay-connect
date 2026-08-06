package com.sixpay.payment.infrastructure.banking.amplitude.client;

@FunctionalInterface
public interface PaymentCoreBankingAccessTokenProvider {

    String accessToken();
}
