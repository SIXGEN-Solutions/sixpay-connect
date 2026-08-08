import {
  mapAuditRecordResponse,
  mapExportJobResponse,
  mapTimelinePageResponse,
} from './reporting-api.mapper';
import {
  PaymentAuditExportJobResponse,
  PaymentAuditRecordResponse,
  PaymentTimelinePageResponse,
} from '../models/reporting.response';

describe('reporting-api.mapper', () => {
  it('maps timeline dates and cursor metadata', () => {
    const response: PaymentTimelinePageResponse = {
      items: [{
        timelineEntryId: '11111111-1111-4111-8111-111111111101',
        paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afa1',
        category: 'DOMAIN',
        eventType: 'PAYMENT_RECEIVED',
        occurredAt: '2026-08-08T13:47:12.104Z',
        correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
        sourceSystem: 'TRESOR_PAY',
        aggregateVersion: 1,
      }],
      size: 1,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    };

    const mapped = mapTimelinePageResponse(response);

    expect(mapped.items).toHaveLength(1);
    expect(mapped.items[0]?.occurredAt).toBeInstanceOf(Date);
    expect(mapped.snapshotAt).toBeInstanceOf(Date);
  });

  it('maps audit integrity evidence', () => {
    const response: PaymentAuditRecordResponse = {
      auditId: '22222222-2222-4222-8222-222222222201',
      occurredAt: '2026-08-08T13:47:12Z',
      actor: { actorType: 'SYSTEM', actorId: 'sixpay' },
      action: 'PAYMENT_REQUESTED',
      targetType: 'PAYMENT',
      targetId: 'payment-1',
      result: 'SUCCESS',
      reasonCode: 'OK',
      correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
      sourceSystem: 'SIXPAY',
      integrityEvidence: { scheme: 'HASH_CHAIN', value: 'hash' },
    };

    const mapped = mapAuditRecordResponse(response);

    expect(mapped.occurredAt).toBeInstanceOf(Date);
    expect(mapped.integrityScheme).toBe('HASH_CHAIN');
  });

  it('maps export job dates', () => {
    const response: PaymentAuditExportJobResponse = {
      exportId: '33333333-3333-4333-8333-333333333301',
      status: 'AVAILABLE',
      requestedAt: '2026-08-08T14:05:00Z',
      requestedBy: 'auditor',
      businessPurpose: 'Internal audit purpose',
      expiresAt: '2026-08-08T15:05:00Z',
    };

    const mapped = mapExportJobResponse(response);

    expect(mapped.requestedAt).toBeInstanceOf(Date);
    expect(mapped.expiresAt).toBeInstanceOf(Date);
  });
});
