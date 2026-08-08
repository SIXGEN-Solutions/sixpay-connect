package com.sixpay.reporting.application.port.output;

import com.sixpay.reporting.application.query.*;

public interface AuditCursorCodec {

    TimelineCriteria decodeTimeline(PaymentTimelineQuery query);

    AuditSearchCriteria decodeSearch(PaymentAuditSearchQuery query);

    AuditCursor encodeTimeline(
            TimelineCriteria criteria,
            AuditPosition position
    );

    AuditCursor encodeSearch(
            AuditSearchCriteria criteria,
            AuditPosition position
    );
}
