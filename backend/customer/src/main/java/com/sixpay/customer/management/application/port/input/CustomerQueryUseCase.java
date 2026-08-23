package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;
import com.sixpay.customer.management.domain.repository.CustomerSearchPage;

public interface CustomerQueryUseCase {

    Customer findById(CustomerId customerId);

    CustomerSearchPage search(CustomerSearchCriteria criteria);
}
