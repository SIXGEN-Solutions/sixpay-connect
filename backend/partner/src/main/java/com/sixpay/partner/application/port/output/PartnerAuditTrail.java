package com.sixpay.partner.application.port.output;

import com.sixpay.partner.domain.model.PartnerId;

import java.time.Instant;

public interface PartnerAuditTrail {

    void append(PartnerAuditRecord record);

    PartnerAuditResult findByPartnerIdAndPeriod(
            PartnerId partnerId,
            Instant from,
            Instant to,
            int page,
            int size
    );
}
