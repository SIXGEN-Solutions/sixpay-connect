package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;

import java.util.Objects;

public record EnrollCustomerResult(Customer customer) {
    public EnrollCustomerResult {
        customer = Objects.requireNonNull(customer, "customer is required");
    }
}
