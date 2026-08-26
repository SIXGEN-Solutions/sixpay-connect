package com.sixpay.customer.management.infrastructure.banking.amplitude;

import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;

public interface AmplitudeCustomerLookupClient {

    BankingCustomerLookupPort.BankingCustomerProfile lookup(
            BankingCustomerLookupPort.BankingCustomerLookupQuery query
    );
}
