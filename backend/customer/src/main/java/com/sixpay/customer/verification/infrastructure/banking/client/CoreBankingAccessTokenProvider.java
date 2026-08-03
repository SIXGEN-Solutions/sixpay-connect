package com.sixpay.customer.verification.infrastructure.banking.client;

@FunctionalInterface
public interface CoreBankingAccessTokenProvider {

    String accessToken();
}
