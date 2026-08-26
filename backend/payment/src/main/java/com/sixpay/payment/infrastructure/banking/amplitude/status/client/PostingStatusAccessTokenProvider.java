package com.sixpay.payment.infrastructure.banking.amplitude.status.client;

@FunctionalInterface
public interface PostingStatusAccessTokenProvider {

    String accessToken();
}
