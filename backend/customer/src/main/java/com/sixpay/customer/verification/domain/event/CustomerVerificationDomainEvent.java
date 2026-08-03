package com.sixpay.customer.verification.domain.event;

import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.sharedkernel.domain.event.DomainEvent;

/**
 * Marker contract for Customer Verification domain events.
 */
public interface CustomerVerificationDomainEvent extends DomainEvent {

    CustomerVerificationId verificationId();
}
