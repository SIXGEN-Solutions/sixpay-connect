package com.sixpay.customer.verification.application.port.output;

/**
 * Transport-neutral output port used to obtain authoritative banking evidence.
 */
public interface BankingCustomerVerificationPort {

    BankingVerificationResponse verify(BankingVerificationQuery query);
}
