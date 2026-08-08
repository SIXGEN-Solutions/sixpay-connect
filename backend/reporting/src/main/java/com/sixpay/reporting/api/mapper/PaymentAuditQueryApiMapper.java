package com.sixpay.reporting.api.mapper;

import com.sixpay.reporting.api.dto.*;
import com.sixpay.reporting.application.query.*;
import org.springframework.stereotype.Component;

@Component
public final class PaymentAuditQueryApiMapper {

    public PaymentTimelinePageResponse toResponse(
            PaymentTimelinePage page
    ) {
        return new PaymentTimelinePageResponse(
                page.items().stream()
                        .map(this::toResponse)
                        .toList(),
                page.size(),
                page.hasMore(),
                page.nextCursor() == null
                        ? null
                        : page.nextCursor().value(),
                page.snapshotAt()
        );
    }

    public PaymentAuditPageResponse toResponse(
            PaymentAuditPage page
    ) {
        return new PaymentAuditPageResponse(
                page.items().stream()
                        .map(this::toResponse)
                        .toList(),
                page.size(),
                page.hasMore(),
                page.nextCursor() == null
                        ? null
                        : page.nextCursor().value(),
                page.snapshotAt()
        );
    }

    public PaymentAuditRecordResponse toResponse(
            PaymentAuditRecordView view
    ) {
        return new PaymentAuditRecordResponse(
                view.auditId(),
                view.occurredAt(),
                new AuditActorResponse(
                        view.actor().actorType().name(),
                        view.actor().actorId(),
                        view.actor().roles()
                ),
                view.action(),
                view.targetType().name(),
                view.targetId(),
                view.paymentId(),
                view.paymentReference(),
                view.observedCustomerId(),
                view.result().name(),
                view.reasonCode(),
                view.correlationId(),
                view.traceId(),
                view.sourceSystem().name(),
                view.beforeState(),
                view.afterState(),
                view.metadata(),
                new IntegrityEvidenceResponse(
                        view.integrityEvidence().scheme().name(),
                        view.integrityEvidence().value()
                )
        );
    }

    private PaymentTimelineEntryResponse toResponse(
            PaymentTimelineEntryView view
    ) {
        return new PaymentTimelineEntryResponse(
                view.timelineEntryId(),
                view.paymentId(),
                view.category().name(),
                view.eventType(),
                view.fromState(),
                view.toState(),
                view.result().name(),
                view.reasonCode(),
                view.occurredAt(),
                view.correlationId(),
                view.sourceSystem().name(),
                view.externalReference(),
                view.aggregateVersion(),
                view.metadata()
        );
    }
}
