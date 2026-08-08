package com.sixpay.reporting.application.port.input;

import com.sixpay.reporting.application.query.PaymentAuditPage;
import com.sixpay.reporting.application.query.PaymentAuditSearchQuery;

public interface SearchPaymentAuditRecordsUseCase extends ReportingQueryUseCase {
    PaymentAuditPage search(PaymentAuditSearchQuery query);
}
