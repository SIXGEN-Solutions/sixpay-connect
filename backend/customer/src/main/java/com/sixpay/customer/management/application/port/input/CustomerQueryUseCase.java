package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;

public interface CustomerQueryUseCase {
    Customer findById(CustomerId customerId);
}
