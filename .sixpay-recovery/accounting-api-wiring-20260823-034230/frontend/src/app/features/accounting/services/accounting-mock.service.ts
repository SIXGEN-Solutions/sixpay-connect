import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { AccountingBatchDetail } from '../models/accounting';
import { AccountingBatchQuery } from '../models/accounting-query';

const BATCHES: readonly AccountingBatchDetail[] = [
  {
    batchId: 'ACC-20260808-03',
    businessDate: '2026-08-08',
    windowLabel: '12:00–14:00',
    itemCount: 384,
    reconciledCount: 376,
    discrepancyCount: 8,
    status: 'RECONCILING',
    reconciliationStatus: 'PARTIAL_MATCH',
    submissionStatus: 'ACCEPTED',
    providerReference: 'AMP-BATCH-20260808-03',
    submittedAt: new Date('2026-08-08T18:01:00Z'),
    updatedAt: new Date('2026-08-08T18:07:00Z'),
    lastReconciledAt: new Date('2026-08-08T18:07:00Z'),
    discrepancies: [
      {
        discrepancyId: 'DISC-20260808-001',
        paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb3',
        paymentReference: 'PAY-2026-0001801',
        type: 'MISSING_PROVIDER_ITEM',
        expectedAmount: 75000,
        observedAmount: null,
        currency: 'XAF',
        reasonCode: 'PROVIDER_ITEM_NOT_FOUND',
        detectedAt: new Date('2026-08-08T18:03:00Z'),
      },
      {
        discrepancyId: 'DISC-20260808-002',
        paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb4',
        paymentReference: 'PAY-2026-0001794',
        type: 'AMOUNT_MISMATCH',
        expectedAmount: 18500,
        observedAmount: 18000,
        currency: 'XAF',
        reasonCode: 'PROVIDER_AMOUNT_DIFFERS',
        detectedAt: new Date('2026-08-08T18:04:00Z'),
      },
    ],
  },
  {
    batchId: 'ACC-20260808-02',
    businessDate: '2026-08-08',
    windowLabel: '10:00–12:00',
    itemCount: 421,
    reconciledCount: 421,
    discrepancyCount: 0,
    status: 'COMPLETED',
    reconciliationStatus: 'MATCHED',
    submissionStatus: 'ACCEPTED',
    providerReference: 'AMP-BATCH-20260808-02',
    submittedAt: new Date('2026-08-08T16:01:00Z'),
    updatedAt: new Date('2026-08-08T16:05:00Z'),
    lastReconciledAt: new Date('2026-08-08T16:05:00Z'),
    discrepancies: [],
  },
  {
    batchId: 'ACC-20260808-01',
    businessDate: '2026-08-08',
    windowLabel: '08:00–10:00',
    itemCount: 414,
    reconciledCount: 414,
    discrepancyCount: 0,
    status: 'COMPLETED',
    reconciliationStatus: 'MATCHED',
    submissionStatus: 'ACCEPTED',
    providerReference: 'AMP-BATCH-20260808-01',
    submittedAt: new Date('2026-08-08T14:01:00Z'),
    updatedAt: new Date('2026-08-08T14:04:00Z'),
    lastReconciledAt: new Date('2026-08-08T14:04:00Z'),
    discrepancies: [],
  },
];

@Injectable({ providedIn: 'root' })
export class AccountingMockService {
  search(query: AccountingBatchQuery): Observable<readonly AccountingBatchDetail[]> {
    return of(
      BATCHES.filter(
        (batch) =>
          (!query.businessDate || batch.businessDate === query.businessDate) &&
          (!query.status || batch.status === query.status) &&
          (!query.reconciliationStatus ||
            batch.reconciliationStatus === query.reconciliationStatus),
      ),
    );
  }

  get(batchId: string): Observable<AccountingBatchDetail | null> {
    return of(BATCHES.find((batch) => batch.batchId === batchId) ?? null);
  }
}
