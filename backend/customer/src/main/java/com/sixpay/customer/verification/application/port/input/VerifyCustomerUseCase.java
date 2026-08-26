package com.sixpay.customer.verification.application.port.input;

public interface VerifyCustomerUseCase {
    VerifyCustomerResult verify(VerifyCustomerCommand command);
}
