package com.sixpay.payment.infrastructure.banking.amplitude.reservation.client;

@FunctionalInterface
public interface FundsReservationAccessTokenProvider {

    String accessToken();
}
