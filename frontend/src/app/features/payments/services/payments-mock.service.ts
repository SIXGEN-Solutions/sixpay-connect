import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { PaymentSearchQuery } from '../models/payment-query';
import {
  PaymentDetailResponse,
  PaymentSearchPageResponse,
  PaymentSummaryResponse,
} from '../models/payments.response';

const PAYMENT_ONE_ID = '7fa85f64-5717-4562-b3fc-2c963f66afa1';
const PAYMENT_TWO_ID = '7fa85f64-5717-4562-b3fc-2c963f66afa2';
const PAYMENT_THREE_ID = '7fa85f64-5717-4562-b3fc-2c963f66afa3';
const CUSTOMER_ONE_ID = '7cb96138-c2b7-4f61-8bb3-b3b00599f101';
const CUSTOMER_TWO_ID = '7cb96138-c2b7-4f61-8bb3-b3b00599f102';

const PAYMENTS: readonly PaymentDetailResponse[] = [
  {
    paymentId: PAYMENT_ONE_ID,
    paymentReference: 'PAY-2026-0001842',
    tresorPayRequestId: 'TP-2026-440921',
    observedCustomerId: CUSTOMER_ONE_ID,
    financialInstitutionCode: 'LRB',
    debtorAccount: { reference: 'ACC-REF-8921', maskedValue: '•••• 8921' },
    amount: { amount: 125000, currency: 'XAF' },
    status: 'TREASURY_INTEGRATED',
    reasonCode: null,
    createdAt: '2026-08-08T13:47:12.104Z',
    updatedAt: '2026-08-08T13:47:13.218Z',
    finalizedAt: '2026-08-08T13:47:13.218Z',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
    aggregateVersion: 8,
    bankingVerification: {
      verificationId: 'VER-2026-1842',
      outcome: 'VERIFIED',
      reasonCodes: [],
      observedAt: '2026-08-08T13:47:12.230Z',
    },
    posting: {
      bankPostingReference: 'AMP-POST-774521',
      outcome: 'CUT_CREDIT_CONFIRMED',
      observedAt: '2026-08-08T13:47:12.803Z',
    },
    tfj: {
      status: 'INTEGRATED',
      businessDate: '2026-08-08',
      confirmedAt: '2026-08-08T13:47:13.190Z',
    },
    notifications: [
      {
        type: 'IMMEDIATE',
        status: 'DELIVERED',
        eventId: '70f51369-e76c-4b58-9a55-9da5f3947f01',
        lastAttemptAt: '2026-08-08T13:47:13.090Z',
      },
      {
        type: 'TREASURY_FINAL',
        status: 'DELIVERED',
        eventId: '70f51369-e76c-4b58-9a55-9da5f3947f02',
        lastAttemptAt: '2026-08-08T13:47:13.218Z',
      },
    ],
    reversal: { status: 'NOT_REQUIRED', reversalReference: null, observedAt: null },
  },
  {
    paymentId: PAYMENT_TWO_ID,
    paymentReference: 'PAY-2026-0001841',
    tresorPayRequestId: 'TP-2026-440920',
    observedCustomerId: CUSTOMER_TWO_ID,
    financialInstitutionCode: 'LRB',
    debtorAccount: { reference: 'ACC-REF-1450', maskedValue: '•••• 1450' },
    amount: { amount: 62500, currency: 'XAF' },
    status: 'POSTING_PENDING',
    reasonCode: null,
    createdAt: '2026-08-08T13:45:03.000Z',
    updatedAt: '2026-08-08T13:45:04.100Z',
    finalizedAt: null,
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea101',
    aggregateVersion: 5,
    bankingVerification: {
      verificationId: 'VER-2026-1841',
      outcome: 'VERIFIED',
      reasonCodes: [],
      observedAt: '2026-08-08T13:45:03.300Z',
    },
    posting: { bankPostingReference: null, outcome: 'PENDING', observedAt: null },
    tfj: { status: 'PENDING', businessDate: '2026-08-08', confirmedAt: null },
    notifications: [{ type: 'IMMEDIATE', status: 'PENDING', eventId: null, lastAttemptAt: null }],
    reversal: { status: 'NOT_REQUIRED', reversalReference: null, observedAt: null },
  },
  {
    paymentId: PAYMENT_THREE_ID,
    paymentReference: 'PAY-2026-0001840',
    tresorPayRequestId: 'TP-2026-440918',
    observedCustomerId: null,
    financialInstitutionCode: 'LRB',
    debtorAccount: { reference: 'ACC-REF-7744', maskedValue: '•••• 7744' },
    amount: { amount: 350000, currency: 'XAF' },
    status: 'REJECTED',
    reasonCode: 'INSUFFICIENT_FUNDS',
    createdAt: '2026-08-08T13:41:28.000Z',
    updatedAt: '2026-08-08T13:41:28.510Z',
    finalizedAt: '2026-08-08T13:41:28.510Z',
    correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea102',
    aggregateVersion: 3,
    bankingVerification: {
      verificationId: 'VER-2026-1840',
      outcome: 'REJECTED',
      reasonCodes: ['INSUFFICIENT_FUNDS'],
      observedAt: '2026-08-08T13:41:28.420Z',
    },
    posting: { bankPostingReference: null, outcome: 'NOT_REQUESTED', observedAt: null },
    tfj: { status: 'NOT_APPLICABLE', businessDate: null, confirmedAt: null },
    notifications: [
      {
        type: 'IMMEDIATE',
        status: 'DELIVERED',
        eventId: '70f51369-e76c-4b58-9a55-9da5f3947f03',
        lastAttemptAt: '2026-08-08T13:41:28.500Z',
      },
    ],
    reversal: { status: 'NOT_REQUIRED', reversalReference: null, observedAt: null },
  },
];

@Injectable({ providedIn: 'root' })
export class PaymentsMockService {
  search(query: PaymentSearchQuery): Observable<PaymentSearchPageResponse> {
    let items = PAYMENTS.filter((payment) => this.matches(payment, query));

    if (query.sort === 'CREATED_AT_ASC') {
      items = [...items].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    } else if (query.sort === 'UPDATED_AT_ASC') {
      items = [...items].sort((a, b) => a.updatedAt.localeCompare(b.updatedAt));
    } else if (query.sort === 'UPDATED_AT_DESC') {
      items = [...items].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
    } else {
      items = [...items].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    }

    const size = Math.max(1, Math.min(query.size ?? 2, 200));
    const offset = query.cursor ? Number(query.cursor) || 0 : 0;
    const pageItems = items.slice(offset, offset + size);
    const nextOffset = offset + pageItems.length;

    return of({
      items: pageItems.map(this.toSummary),
      size: pageItems.length,
      hasMore: nextOffset < items.length,
      nextCursor: nextOffset < items.length ? String(nextOffset) : null,
      snapshotAt: '2026-08-08T14:00:00.000Z',
    });
  }

  get(paymentId: string): Observable<PaymentDetailResponse | null> {
    return of(PAYMENTS.find((payment) => payment.paymentId === paymentId) ?? null);
  }

  private matches(payment: PaymentDetailResponse, query: PaymentSearchQuery): boolean {
    const createdAt = new Date(payment.createdAt).getTime();

    return (
      (!query.paymentReference ||
        payment.paymentReference.toLowerCase().includes(query.paymentReference.toLowerCase())) &&
      (!query.tresorPayRequestId ||
        payment.tresorPayRequestId.toLowerCase().includes(query.tresorPayRequestId.toLowerCase())) &&
      (!query.observedCustomerId || payment.observedCustomerId === query.observedCustomerId) &&
      (!query.financialInstitutionCode ||
        payment.financialInstitutionCode === query.financialInstitutionCode) &&
      (!query.status || payment.status === query.status) &&
      (!query.reasonCode ||
        (payment.reasonCode ?? '').toLowerCase().includes(query.reasonCode.toLowerCase())) &&
      (!query.createdFrom || createdAt >= query.createdFrom.getTime()) &&
      (!query.createdTo || createdAt <= query.createdTo.getTime()) &&
      (query.amountMin === undefined || payment.amount.amount >= query.amountMin) &&
      (query.amountMax === undefined || payment.amount.amount <= query.amountMax) &&
      (!query.currency || payment.amount.currency === query.currency.toUpperCase())
    );
  }

  private readonly toSummary = (
    payment: PaymentDetailResponse,
  ): PaymentSummaryResponse => ({
    paymentId: payment.paymentId,
    paymentReference: payment.paymentReference,
    tresorPayRequestId: payment.tresorPayRequestId,
    observedCustomerId: payment.observedCustomerId ?? null,
    financialInstitutionCode: payment.financialInstitutionCode,

    ...(payment.debtorAccount
      ? { debtorAccount: { ...payment.debtorAccount } }
      : {}),

    amount: { ...payment.amount },
    status: payment.status,
    reasonCode: payment.reasonCode ?? null,
    createdAt: payment.createdAt,
    updatedAt: payment.updatedAt,
    finalizedAt: payment.finalizedAt ?? null,
  });
}
