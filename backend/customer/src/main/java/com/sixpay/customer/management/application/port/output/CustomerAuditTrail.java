package com.sixpay.customer.management.application.port.output;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerAuditTrail {

    void append(CustomerAuditRecord record);

    List<CustomerAuditRecord> find(
            String aggregateType,
            UUID aggregateId,
            Instant from,
            Instant to
    );
}
