package com.sixpay.reporting.application.port.input;

import com.sixpay.reporting.application.query.PaymentTimelinePage;
import com.sixpay.reporting.application.query.PaymentTimelineQuery;

public interface GetPaymentTimelineUseCase extends ReportingQueryUseCase {
    PaymentTimelinePage getTimeline(PaymentTimelineQuery query);
}
