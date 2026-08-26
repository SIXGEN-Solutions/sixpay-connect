package com.sixpay.payment;

/**
 * Marker type used to locate the Payment module without relying on package
 * strings or introducing a Spring Boot application.
 */
public final class PaymentModule {

    private PaymentModule() {
        throw new IllegalStateException("Marker class");
    }
}
