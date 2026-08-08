package com.sixpay.reporting.api.mapper;

import com.sixpay.reporting.api.dto.PaymentAuditExportJobResponse;
import com.sixpay.reporting.application.query.PaymentAuditExportJobView;
import org.springframework.stereotype.Component;

@Component
public final class PaymentAuditExportApiMapper {

    public PaymentAuditExportJobResponse toResponse(
            PaymentAuditExportJobView view
    ) {
        return new PaymentAuditExportJobResponse(
                view.exportId(),
                view.status().name(),
                view.requestedAt(),
                view.requestedBy(),
                view.businessPurpose(),
                view.recordCount(),
                view.checksum(),
                view.retrievalUri(),
                view.expiresAt(),
                view.failureCode()
        );
    }
}
