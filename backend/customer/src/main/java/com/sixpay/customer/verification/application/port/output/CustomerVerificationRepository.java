package com.sixpay.customer.verification.application.port.output;

import com.sixpay.customer.verification.domain.model.CustomerVerification;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;

import java.util.Optional;

/**
 * Persistence boundary for Customer Verification aggregates.
 */
public interface CustomerVerificationRepository {

    CustomerVerification save(CustomerVerification verification);

    Optional<CustomerVerification> findById(
            CustomerVerificationId verificationId
    );
}
