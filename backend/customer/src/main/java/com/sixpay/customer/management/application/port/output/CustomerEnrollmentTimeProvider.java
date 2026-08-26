package com.sixpay.customer.management.application.port.output;

import java.time.Instant;

public interface CustomerEnrollmentTimeProvider {
    Instant now();
}
