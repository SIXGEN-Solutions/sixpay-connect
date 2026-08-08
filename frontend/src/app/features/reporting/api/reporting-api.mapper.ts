import {
  PaymentAuditExportJobResponse,
  PaymentAuditPageResponse,
  PaymentAuditRecordResponse,
  PaymentTimelinePageResponse,
} from '../models/reporting.response';
import {
  PaymentAuditExportJob,
  PaymentAuditPage,
  PaymentAuditRecord,
  PaymentTimelinePage,
} from '../models/reporting';

export function mapTimelinePageResponse(response: PaymentTimelinePageResponse): PaymentTimelinePage {
  return {
    items: response.items.map((item) => ({
      timelineEntryId: item.timelineEntryId,
      paymentId: item.paymentId,
      category: item.category,
      eventType: item.eventType,
      fromState: item.fromState ?? null,
      toState: item.toState ?? null,
      result: item.result ?? null,
      reasonCode: item.reasonCode ?? null,
      occurredAt: new Date(item.occurredAt),
      correlationId: item.correlationId,
      sourceSystem: item.sourceSystem,
      externalReference: item.externalReference ?? null,
      aggregateVersion: item.aggregateVersion,
      metadata: { ...(item.metadata ?? {}) },
    })),
    size: response.size,
    hasMore: response.hasMore,
    nextCursor: response.nextCursor ?? null,
    snapshotAt: new Date(response.snapshotAt),
  };
}

export function mapAuditRecordResponse(response: PaymentAuditRecordResponse): PaymentAuditRecord {
  return {
    auditId: response.auditId,
    occurredAt: new Date(response.occurredAt),
    actorType: response.actor.actorType,
    actorId: response.actor.actorId,
    actorRoles: [...(response.actor.roles ?? [])],
    action: response.action,
    targetType: response.targetType,
    targetId: response.targetId,
    paymentId: response.paymentId ?? null,
    paymentReference: response.paymentReference ?? null,
    observedCustomerId: response.observedCustomerId ?? null,
    result: response.result,
    reasonCode: response.reasonCode,
    correlationId: response.correlationId,
    traceId: response.traceId ?? null,
    sourceSystem: response.sourceSystem,
    beforeState: response.beforeState ?? null,
    afterState: response.afterState ?? null,
    metadata: { ...(response.metadata ?? {}) },
    integrityScheme: response.integrityEvidence.scheme,
    integrityValue: response.integrityEvidence.value,
  };
}

export function mapAuditPageResponse(response: PaymentAuditPageResponse): PaymentAuditPage {
  return {
    items: response.items.map(mapAuditRecordResponse),
    size: response.size,
    hasMore: response.hasMore,
    nextCursor: response.nextCursor ?? null,
    snapshotAt: new Date(response.snapshotAt),
  };
}

export function mapExportJobResponse(response: PaymentAuditExportJobResponse): PaymentAuditExportJob {
  return {
    exportId: response.exportId,
    status: response.status,
    requestedAt: new Date(response.requestedAt),
    requestedBy: response.requestedBy,
    businessPurpose: response.businessPurpose,
    recordCount: response.recordCount ?? null,
    checksum: response.checksum ?? null,
    retrievalUri: response.retrievalUri ?? null,
    expiresAt: new Date(response.expiresAt),
    failureCode: response.failureCode ?? null,
  };
}
