package com.sixpay.payment.application.port.output.query;

import java.util.UUID;

@FunctionalInterface
public interface PaymentObservedCustomerLinkPort {
    void link(UUID paymentId, UUID observedCustomerId);
}
