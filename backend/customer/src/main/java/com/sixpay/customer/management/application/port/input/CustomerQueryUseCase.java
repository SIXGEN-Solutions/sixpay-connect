package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;

import java.util.List;

public interface CustomerQueryUseCase {

    Customer findById(CustomerId customerId);

    List<Customer> findAll();
}
