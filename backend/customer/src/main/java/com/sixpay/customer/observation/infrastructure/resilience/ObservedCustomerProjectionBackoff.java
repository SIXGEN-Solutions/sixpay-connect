package com.sixpay.customer.observation.infrastructure.resilience;

import java.time.Duration;

@FunctionalInterface
public interface ObservedCustomerProjectionBackoff {

    void pause(Duration delay);
}
