package com.sixpay.reporting.application.port.input;

import com.sixpay.reporting.application.query.GetPaymentAuditRecordQuery;
import com.sixpay.reporting.application.query.PaymentAuditRecordView;

public interface GetPaymentAuditRecordUseCase extends ReportingQueryUseCase {
    PaymentAuditRecordView get(GetPaymentAuditRecordQuery query);
}
