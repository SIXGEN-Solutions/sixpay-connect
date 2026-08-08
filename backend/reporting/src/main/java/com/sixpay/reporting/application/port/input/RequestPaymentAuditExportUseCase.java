package com.sixpay.reporting.application.port.input;

import com.sixpay.reporting.application.query.PaymentAuditExportJobView;
import com.sixpay.reporting.application.query.RequestPaymentAuditExportCommand;

public interface RequestPaymentAuditExportUseCase {
    PaymentAuditExportJobView request(
            RequestPaymentAuditExportCommand command
    );
}
