package com.sixpay.partner.application.port.output;

import java.util.List;

public record PartnerAuditResult(
        List<PartnerAuditRecord> records,
        long totalElements
) {

    public PartnerAuditResult {
        records = List.copyOf(records);
        if (totalElements < records.size()) {
            throw new IllegalArgumentException(
                    "totalElements cannot be lower than records size"
            );
        }
    }
}
