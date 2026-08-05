package com.sixpay.customer.observation.application.port.output.audit;

import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;

@FunctionalInterface
public interface ObservedCustomerAuditPort {

    void append(ObservedCustomerAuditRecord record);
}
