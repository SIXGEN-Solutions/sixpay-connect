import {
  mapAccountingBatchDetailResponse,
  mapAccountingBatchItemResponse,
  mapAccountingBatchSummaryResponse,
} from './accounting-api.mapper';

describe('Accounting API mapper', () => {
  it('maps contract-backed summary status and dates', () => {
    const mapped = mapAccountingBatchSummaryResponse({
      batchId: '11111111-1111-4111-8111-111111111111',
      businessDate: '2026-08-08',
      financialInstitutionCode: 'LAREGIONALE',
      status: 'COMPLETED',
      itemCount: 2,
      createdAt: '2026-08-08T18:01:00Z',
    });

    expect(mapped.status).toBe('COMPLETED');
    expect(mapped.createdAt.toISOString()).toBe('2026-08-08T18:01:00.000Z');
  });

  it('maps only the T1 fields exposed by AccountingBatchItemResponse', () => {
    const mapped = mapAccountingBatchItemResponse({
      paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb3',
      publicPaymentReference: 'PAY-2026-0001801',
      partnerId: 'TRESORPAY',
      amount: 75000,
      currency: 'XAF',
      paymentOccurredAt: '2026-08-08T17:59:00Z',
      paymentBusinessDate: '2026-08-08',
      bankPostingReference: 'BANK-POST-001',
      tresorPayStatus: 'SUCCESS',
      tresorPayStatusCheckedAt: '2026-08-08T18:00:00Z',
      status: 'PENDING',
    });

    expect(mapped.bankPostingReference).toBe('BANK-POST-001');
    expect(mapped.tresorPayStatus).toBe('SUCCESS');
    expect(mapped.status).toBe('PENDING');
    expect(mapped.tresorPayStatusCheckedAt.toISOString()).toBe('2026-08-08T18:00:00.000Z');
  });

  it('maps detail and items without introducing internal T1 fields', () => {
    const mapped = mapAccountingBatchDetailResponse({
      batchId: '11111111-1111-4111-8111-111111111111',
      businessDate: '2026-08-08',
      financialInstitutionCode: 'LAREGIONALE',
      status: 'NOT_COMPLETED',
      itemCount: 1,
      createdAt: '2026-08-08T18:01:00Z',
      idempotencyKey: 'idem-1',
      items: [
        {
          paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb3',
          publicPaymentReference: 'PAY-2026-0001801',
          partnerId: 'TRESORPAY',
          amount: 75000,
          currency: 'XAF',
          paymentOccurredAt: '2026-08-08T17:59:00Z',
          paymentBusinessDate: '2026-08-08',
          bankPostingReference: null,
          tresorPayStatus: 'SUCCESS',
          tresorPayStatusCheckedAt: '2026-08-08T18:00:00Z',
          status: 'PENDING',
        },
      ],
    });

    expect(mapped.idempotencyKey).toBe('idem-1');
    expect(mapped.items).toHaveLength(1);
    expect(mapped.items[0]?.bankPostingReference).toBeNull();
    expect(mapped.items[0]?.status).toBe('PENDING');
  });
});
