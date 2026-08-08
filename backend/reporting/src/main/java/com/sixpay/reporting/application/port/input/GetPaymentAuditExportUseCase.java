package com.sixpay.reporting.application.port.input;

import com.sixpay.reporting.application.query.PaymentAuditExportJobView;

import java.util.UUID;

public interface GetPaymentAuditExportUseCase {
    PaymentAuditExportJobView get(UUID exportId);
}
