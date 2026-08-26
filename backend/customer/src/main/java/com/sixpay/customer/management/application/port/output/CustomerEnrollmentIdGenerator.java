package com.sixpay.customer.management.application.port.output;

import java.util.UUID;

public interface CustomerEnrollmentIdGenerator {
    UUID nextId();
}
