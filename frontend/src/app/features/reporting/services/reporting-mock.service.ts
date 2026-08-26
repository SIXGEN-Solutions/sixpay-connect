import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { PaymentAuditQuery, PaymentTimelineQuery } from '../models/reporting-query';
import {
  PaymentAuditExportJobResponse,
  PaymentAuditExportRequest,
  PaymentAuditPageResponse,
  PaymentAuditRecordResponse,
  PaymentTimelineEntryResponse,
  PaymentTimelinePageResponse,
} from '../models/reporting.response';

const PAYMENT_ID = '7fa85f64-5717-4562-b3fc-2c963f66afa1';
const PAYMENT_REFERENCE = 'PAY-2026-0001842';
const CUSTOMER_ID = '7cb96138-c2b7-4f61-8bb3-b3b00599f101';
const EXPORT_ID = '33333333-3333-4333-8333-333333333301';

const TIMELINE: readonly PaymentTimelineEntryResponse[] = [
  {
    timelineEntryId: '11111111-1111-4111-8111-111111111101',
    paymentId: PAYMENT_ID,
    category: 'DOMAIN',
    eventType: 'PAYMENT_RECEIVED',
    fromState: null,
    toState: 'RECEIVED',
    result: 'SUCCESS',
    reasonCode: null,
    occurredAt: '2026-08-08T13:47:12.104Z',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    sourceSystem: 'TRESOR_PAY',
    externalReference: 'TP-2026-440921',
    aggregateVersion: 1,
    metadata: { channel: 'TRESOR_PAY' },
  },
  {
    timelineEntryId: '11111111-1111-4111-8111-111111111102',
    paymentId: PAYMENT_ID,
    category: 'BANKING_VERIFICATION',
    eventType: 'BANKING_VERIFICATION_COMPLETED',
    fromState: 'BANKING_CHECKING',
    toState: 'APPROVED',
    result: 'SUCCESS',
    reasonCode: null,
    occurredAt: '2026-08-08T13:47:12.230Z',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    sourceSystem: 'SIXPAY',
    externalReference: 'VER-2026-1842',
    aggregateVersion: 3,
    metadata: { verificationOutcome: 'VERIFIED' },
  },
  {
    timelineEntryId: '11111111-1111-4111-8111-111111111103',
    paymentId: PAYMENT_ID,
    category: 'ACCOUNTING',
    eventType: 'CORE_BANKING_POSTED',
    fromState: 'POSTING',
    toState: 'CUT_CREDITED',
    result: 'SUCCESS',
    reasonCode: null,
    occurredAt: '2026-08-08T13:47:12.803Z',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    sourceSystem: 'AMPLITUDE',
    externalReference: 'AMP-POST-774521',
    aggregateVersion: 6,
    metadata: { outcome: 'CUT_CREDIT_CONFIRMED' },
  },
  {
    timelineEntryId: '11111111-1111-4111-8111-111111111104',
    paymentId: PAYMENT_ID,
    category: 'NOTIFICATION',
    eventType: 'IMMEDIATE_NOTIFICATION_DELIVERED',
    fromState: 'CUT_CREDITED',
    toState: 'NOTIFIED',
    result: 'SUCCESS',
    reasonCode: null,
    occurredAt: '2026-08-08T13:47:13.090Z',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    sourceSystem: 'SIXPAY',
    externalReference: null,
    aggregateVersion: 7,
    metadata: { notificationType: 'IMMEDIATE' },
  },
  {
    timelineEntryId: '11111111-1111-4111-8111-111111111105',
    paymentId: PAYMENT_ID,
    category: 'TFJ',
    eventType: 'TFJ_CONFIRMED',
    fromState: 'PENDING_END_OF_DAY_CONFIRMATION',
    toState: 'TREASURY_INTEGRATED',
    result: 'SUCCESS',
    reasonCode: null,
    occurredAt: '2026-08-08T13:47:13.218Z',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    sourceSystem: 'SIXPAY',
    externalReference: null,
    aggregateVersion: 8,
    metadata: { businessDate: '2026-08-08' },
  },
];

const AUDIT_RECORDS: readonly PaymentAuditRecordResponse[] = [
  {
    auditId: '22222222-2222-4222-8222-222222222201',
    occurredAt: '2026-08-08T13:47:12.104Z',
    actor: { actorType: 'EXTERNAL_SYSTEM', actorId: 'TRESOR_PAY' },
    action: 'PAYMENT_REQUESTED',
    targetType: 'PAYMENT',
    targetId: PAYMENT_ID,
    paymentId: PAYMENT_ID,
    paymentReference: PAYMENT_REFERENCE,
    observedCustomerId: CUSTOMER_ID,
    result: 'SUCCESS',
    reasonCode: 'REQUEST_ACCEPTED',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    traceId: 'trace-1842',
    sourceSystem: 'TRESOR_PAY',
    beforeState: null,
    afterState: 'RECEIVED',
    metadata: { requestType: 'PAYMENT' },
    integrityEvidence: { scheme: 'HASH_CHAIN', value: 'sha256:audit-demo-201' },
  },
  {
    auditId: '22222222-2222-4222-8222-222222222202',
    occurredAt: '2026-08-08T13:47:12.230Z',
    actor: { actorType: 'SERVICE', actorId: 'payment-service', roles: ['SYSTEM'] },
    action: 'BANKING_VERIFICATION_COMPLETED',
    targetType: 'BANKING_VERIFICATION',
    targetId: 'VER-2026-1842',
    paymentId: PAYMENT_ID,
    paymentReference: PAYMENT_REFERENCE,
    observedCustomerId: CUSTOMER_ID,
    result: 'SUCCESS',
    reasonCode: 'VERIFIED',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    traceId: 'trace-1842',
    sourceSystem: 'SIXPAY',
    beforeState: 'BANKING_CHECKING',
    afterState: 'APPROVED',
    metadata: { provider: 'AMPLITUDE' },
    integrityEvidence: { scheme: 'HASH_CHAIN', value: 'sha256:audit-demo-202' },
  },
  {
    auditId: '22222222-2222-4222-8222-222222222203',
    occurredAt: '2026-08-08T13:47:12.803Z',
    actor: { actorType: 'EXTERNAL_SYSTEM', actorId: 'AMPLITUDE' },
    action: 'POSTING_CONFIRMED',
    targetType: 'POSTING',
    targetId: 'AMP-POST-774521',
    paymentId: PAYMENT_ID,
    paymentReference: PAYMENT_REFERENCE,
    observedCustomerId: CUSTOMER_ID,
    result: 'SUCCESS',
    reasonCode: 'CUT_CREDIT_CONFIRMED',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    traceId: 'trace-1842',
    sourceSystem: 'AMPLITUDE',
    beforeState: 'POSTING',
    afterState: 'CUT_CREDITED',
    metadata: { bankPostingReference: 'AMP-POST-774521' },
    integrityEvidence: { scheme: 'SIGNATURE', value: 'sig:audit-demo-203' },
  },
];

@Injectable({ providedIn: 'root' })
export class ReportingMockService {
  timeline(paymentId: string, query: PaymentTimelineQuery): Observable<PaymentTimelinePageResponse> {
    if (paymentId !== PAYMENT_ID) {
      return of({
        items: [],
        size: 0,
        hasMore: false,
        nextCursor: null,
        snapshotAt: '2026-08-08T14:00:00.000Z',
      });
    }

    const filtered = TIMELINE.filter((entry) =>
      (!query.category || entry.category === query.category) &&
      (!query.occurredFrom || new Date(entry.occurredAt) >= query.occurredFrom) &&
      (!query.occurredTo || new Date(entry.occurredAt) <= query.occurredTo),
    );

    return of(this.timelinePage(filtered, query.cursor, query.size));
  }

  searchAudit(query: PaymentAuditQuery): Observable<PaymentAuditPageResponse> {
    const items = AUDIT_RECORDS.filter((record) =>
      (!query.paymentId || record.paymentId === query.paymentId) &&
      (!query.paymentReference ||
        (record.paymentReference ?? '').toLowerCase().includes(query.paymentReference.toLowerCase())) &&
      (!query.observedCustomerId || record.observedCustomerId === query.observedCustomerId) &&
      (!query.actorId || record.actor.actorId.toLowerCase().includes(query.actorId.toLowerCase())) &&
      (!query.actorType || record.actor.actorType === query.actorType) &&
      (!query.action || record.action.toLowerCase().includes(query.action.toLowerCase())) &&
      (!query.result || record.result === query.result) &&
      (!query.reasonCode || record.reasonCode.toLowerCase().includes(query.reasonCode.toLowerCase())) &&
      (!query.correlationId || record.correlationId === query.correlationId) &&
      (!query.sourceSystem || record.sourceSystem === query.sourceSystem) &&
      new Date(record.occurredAt) >= query.occurredFrom &&
      new Date(record.occurredAt) <= query.occurredTo,
    );

    const sorted = [...items].sort((a, b) =>
      query.sort === 'OCCURRED_AT_ASC'
        ? a.occurredAt.localeCompare(b.occurredAt)
        : b.occurredAt.localeCompare(a.occurredAt),
    );

    const size = Math.max(1, Math.min(query.size ?? 2, 200));
    const offset = query.cursor ? Number(query.cursor) || 0 : 0;
    const pageItems = sorted.slice(offset, offset + size);
    const nextOffset = offset + pageItems.length;

    return of({
      items: pageItems,
      size: pageItems.length,
      hasMore: nextOffset < sorted.length,
      nextCursor: nextOffset < sorted.length ? String(nextOffset) : null,
      snapshotAt: '2026-08-08T14:00:00.000Z',
    });
  }

  getAudit(auditId: string): Observable<PaymentAuditRecordResponse | null> {
    return of(AUDIT_RECORDS.find((record) => record.auditId === auditId) ?? null);
  }

  requestExport(request: PaymentAuditExportRequest): Observable<PaymentAuditExportJobResponse> {
    return of({
      exportId: EXPORT_ID,
      status: 'AVAILABLE',
      requestedAt: '2026-08-08T14:05:00.000Z',
      requestedBy: 'local-auditor',
      businessPurpose: request.businessPurpose,
      recordCount: AUDIT_RECORDS.length,
      checksum: 'sha256:demo-export-checksum',
      retrievalUri: '/mock-downloads/payment-audit-export.csv',
      expiresAt: '2026-08-08T15:05:00.000Z',
      failureCode: null,
    });
  }

  getExport(exportId: string): Observable<PaymentAuditExportJobResponse | null> {
    if (exportId !== EXPORT_ID) {
      return of(null);
    }

    return of({
      exportId: EXPORT_ID,
      status: 'AVAILABLE',
      requestedAt: '2026-08-08T14:05:00.000Z',
      requestedBy: 'local-auditor',
      businessPurpose: 'Contrôle interne quotidien des opérations SIXPAY.',
      recordCount: AUDIT_RECORDS.length,
      checksum: 'sha256:demo-export-checksum',
      retrievalUri: '/mock-downloads/payment-audit-export.csv',
      expiresAt: '2026-08-08T15:05:00.000Z',
      failureCode: null,
    });
  }

  private timelinePage(
    items: readonly PaymentTimelineEntryResponse[],
    cursor?: string,
    pageSize?: number,
  ): PaymentTimelinePageResponse {
    const size = Math.max(1, Math.min(pageSize ?? 3, 200));
    const offset = cursor ? Number(cursor) || 0 : 0;
    const pageItems = items.slice(offset, offset + size);
    const nextOffset = offset + pageItems.length;

    return {
      items: pageItems,
      size: pageItems.length,
      hasMore: nextOffset < items.length,
      nextCursor: nextOffset < items.length ? String(nextOffset) : null,
      snapshotAt: '2026-08-08T14:00:00.000Z',
    };
  }
}
