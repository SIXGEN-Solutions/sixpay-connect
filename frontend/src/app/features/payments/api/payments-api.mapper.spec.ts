import { mapPaymentDetailResponse, mapPaymentSearchPageResponse } from './payments-api.mapper';
import { PaymentDetailResponse, PaymentSearchPageResponse } from '../models/payments.response';

describe('payments-api.mapper', () => {
  const detail: PaymentDetailResponse = {
    paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afa1',
    paymentReference: 'PAY-1',
    tresorPayRequestId: 'TP-1',
    observedCustomerId: null,
    financialInstitutionCode: 'LRB',
    amount: { amount: 1000, currency: 'XAF' },
    status: 'DEBIT_CONFIRMED',
    reasonCode: null,
    createdAt: '2026-08-08T10:00:00Z',
    updatedAt: '2026-08-08T10:00:01Z',
    finalizedAt: null,
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    aggregateVersion: 4,
    bankingVerification: {
      verificationId: 'VER-1',
      outcome: 'VERIFIED',
      reasonCodes: [],
      observedAt: '2026-08-08T10:00:00.200Z',
    },
    posting: { outcome: 'DEBIT_CONFIRMED', observedAt: '2026-08-08T10:00:00.800Z' },
    tfj: { status: 'PENDING', businessDate: '2026-08-08', confirmedAt: null },
    notifications: [{ type: 'IMMEDIATE', status: 'DELIVERED', eventId: null, lastAttemptAt: null }],
    reversal: { status: 'NOT_REQUIRED', reversalReference: null, observedAt: null },
  };

  it('maps detail date-time values to Date objects', () => {
    const mapped = mapPaymentDetailResponse(detail);

    expect(mapped.createdAt).toBeInstanceOf(Date);
    expect(mapped.updatedAt).toBeInstanceOf(Date);
    expect(mapped.bankingVerification?.observedAt).toBeInstanceOf(Date);
    expect(mapped.posting?.observedAt).toBeInstanceOf(Date);
    expect(mapped.observedCustomerId).toBeNull();
  });

  it('maps cursor page metadata', () => {
    const page: PaymentSearchPageResponse = {
      items: [detail],
      size: 1,
      hasMore: true,
      nextCursor: '1',
      snapshotAt: '2026-08-08T10:01:00Z',
    };

    const mapped = mapPaymentSearchPageResponse(page);

    expect(mapped.items).toHaveLength(1);
    expect(mapped.hasMore).toBe(true);
    expect(mapped.nextCursor).toBe('1');
    expect(mapped.snapshotAt).toBeInstanceOf(Date);
  });
});
