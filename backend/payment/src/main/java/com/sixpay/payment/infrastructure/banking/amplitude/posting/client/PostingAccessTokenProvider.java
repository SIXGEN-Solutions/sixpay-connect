package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

@FunctionalInterface
public interface PostingAccessTokenProvider {
    String accessToken();
}
