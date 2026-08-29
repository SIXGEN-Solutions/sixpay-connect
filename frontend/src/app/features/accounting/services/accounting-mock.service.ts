import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { AccountingBatchDetail, AccountingBatchSummary } from '../models/accounting';
import { AccountingBatchQuery } from '../models/accounting-query';

const BATCHES: readonly AccountingBatchDetail[] = [
  {
    batchId: '11111111-1111-4111-8111-111111111111',
    idempotencyKey: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    businessDate: '2026-08-08',
    financialInstitutionCode: 'LAREGIONALE',
    itemCount: 1,
    status: 'NOT_COMPLETED',
    createdAt: new Date('2026-08-08T18:01:00Z'),
    items: [
      {
        paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb3',
        publicPaymentReference: 'PAY-2026-0001801',
        partnerId: 'TRESORPAY',
        amount: 75000,
        currency: 'XAF',
        paymentOccurredAt: new Date('2026-08-08T17:59:00Z'),
        paymentBusinessDate: '2026-08-08',
        bankPostingReference: 'BANK-POST-001',
        tresorPayStatus: 'SUCCESS',
        tresorPayStatusCheckedAt: new Date('2026-08-08T18:00:00Z'),
        status: 'PENDING',
      },
    ],
  },
  {
    batchId: '22222222-2222-4222-8222-222222222222',
    idempotencyKey: 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
    businessDate: '2026-08-08',
    financialInstitutionCode: 'LAREGIONALE',
    itemCount: 1,
    status: 'COMPLETED',
    createdAt: new Date('2026-08-08T16:01:00Z'),
    items: [
      {
        paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb4',
        publicPaymentReference: 'PAY-2026-0001794',
        partnerId: 'TRESORPAY',
        amount: 18500,
        currency: 'XAF',
        paymentOccurredAt: new Date('2026-08-08T15:59:00Z'),
        paymentBusinessDate: '2026-08-08',
        bankPostingReference: 'BANK-POST-002',
        tresorPayStatus: 'SUCCESS',
        tresorPayStatusCheckedAt: new Date('2026-08-08T16:00:00Z'),
        status: 'COMPLETED',
      },
    ],
  },
];

@Injectable({ providedIn: 'root' })
export class AccountingMockService {
  search(query: AccountingBatchQuery): Observable<readonly AccountingBatchSummary[]> {
    return of(
      BATCHES.filter(
        (batch) =>
          (!query.businessDate || batch.businessDate === query.businessDate) &&
          (!query.status || batch.status === query.status),
      ),
    );
  }

  get(batchId: string): Observable<AccountingBatchDetail | null> {
    return of(BATCHES.find((batch) => batch.batchId === batchId) ?? null);
  }
}
