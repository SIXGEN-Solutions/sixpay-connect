package com.sixpay.reporting.application.port.output;

import com.sixpay.reporting.application.query.*;

import java.util.Optional;
import java.util.UUID;

public interface PaymentAuditReadPort extends AuditEvidenceReadPort {

    boolean paymentExists(UUID paymentId);

    TimelineSlice timeline(TimelineCriteria criteria);

    AuditSlice search(AuditSearchCriteria criteria);

    Optional<PaymentAuditRecordView> findById(UUID auditId);
}
