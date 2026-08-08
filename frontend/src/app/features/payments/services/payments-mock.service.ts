import { Injectable } from '@angular/core';

export interface MockPayment {
  readonly id: string;
  readonly reference: string;
  readonly customer: string;
  readonly institution: string;
  readonly amount: number;
  readonly currency: 'XAF';
  readonly status: 'SUCCESS' | 'PROCESSING' | 'FAILED' | 'REVERSED';
  readonly reasonCode?: string;
  readonly createdAt: string;
  readonly tresorPayRequestId: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentsMockService {
  readonly payments: readonly MockPayment[] = [
    {
      id: 'PAY-2026-0001842',
      reference: 'PAY-2026-0001842',
      customer: 'CAMEROUN SERVICES SARL',
      institution: 'LRB',
      amount: 125000,
      currency: 'XAF',
      status: 'SUCCESS',
      createdAt: '2026-08-08T09:47:12-04:00',
      tresorPayRequestId: 'TP-2026-440921',
    },
    {
      id: 'PAY-2026-0001841',
      reference: 'PAY-2026-0001841',
      customer: 'ETS MBARGA & FILS',
      institution: 'LRB',
      amount: 62500,
      currency: 'XAF',
      status: 'PROCESSING',
      createdAt: '2026-08-08T09:45:03-04:00',
      tresorPayRequestId: 'TP-2026-440920',
    },
    {
      id: 'PAY-2026-0001840',
      reference: 'PAY-2026-0001840',
      customer: 'AFRICA LOGISTICS SA',
      institution: 'LRB',
      amount: 350000,
      currency: 'XAF',
      status: 'FAILED',
      reasonCode: 'INSUFFICIENT_FUNDS',
      createdAt: '2026-08-08T09:41:28-04:00',
      tresorPayRequestId: 'TP-2026-440918',
    },
  ];

  find(id: string): MockPayment {
    return this.payments.find((payment) => payment.id === id) ?? this.payments[0]!;
  }
}
