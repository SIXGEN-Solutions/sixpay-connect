package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId customerId);

    /** Banking references are unique within an institution, not globally. */
    List<Customer> findByBankingCustomerReference(String bankingCustomerReference);

    CustomerSearchPage search(CustomerSearchCriteria criteria);

    boolean existsById(CustomerId customerId);

    boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(
            String financialInstitutionCode,
            String bankingCustomerReference
    );
}
