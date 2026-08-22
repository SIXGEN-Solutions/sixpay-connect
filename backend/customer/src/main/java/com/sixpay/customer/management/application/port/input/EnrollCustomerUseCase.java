package com.sixpay.customer.management.application.port.input;

public interface EnrollCustomerUseCase {
    EnrollCustomerResult enroll(EnrollCustomerCommand command);
}
