package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;

import java.time.Instant;

public interface CustomerManagementUseCase {

    Customer updateProfile(
            CustomerId customerId,
            String legalName,
            String email,
            String phoneNumber,
            Instant now
    );

    Customer suspend(
            CustomerId customerId,
            String reason,
            Instant now
    );

    Customer reactivate(
            CustomerId customerId,
            Instant now
    );

    Customer close(
            CustomerId customerId,
            String reason,
            Instant now
    );

    Customer addBankAccount(
            CustomerId customerId,
            AddBankAccountCommand command,
            Instant now
    );

    Customer makeDefaultBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    );

    Customer removeBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    );
}
