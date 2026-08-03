package com.sixpay.payment.application.port.output;

/**
 * Payment-owned output boundary for customer and debtor-account verification.
 *
 * <p>The Payment application depends only on this contract. Customer domain,
 * Core Banking, HTTP and Amplitude types remain outside the boundary.</p>
 */
public interface CustomerVerificationPort {

    CustomerVerificationResponse verify(
            CustomerVerificationRequest request
    );
}
