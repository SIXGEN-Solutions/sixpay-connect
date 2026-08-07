package com.sixpay.accounting.infrastructure.accountingapi.client;

@FunctionalInterface
public interface AccountingApiAccessTokenProvider {

    String accessToken();
}
