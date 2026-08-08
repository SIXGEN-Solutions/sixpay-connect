import { Injectable } from '@angular/core';

export interface MockObservedCustomer {
  readonly id: string;
  readonly legalName: string;
  readonly niuMasked: string;
  readonly institution: string;
  readonly accountMasked: string;
  readonly firstObservedAt: string;
  readonly lastObservedAt: string;
  readonly paymentCount: number;
  readonly lastPaymentStatus: 'SUCCESS' | 'FAILED' | 'PROCESSING';
}

@Injectable({ providedIn: 'root' })
export class CustomersMockService {
  readonly customers: readonly MockObservedCustomer[] = [
    {
      id: '7cb96138-c2b7-4f61-8bb3-b3b00599f101',
      legalName: 'CAMEROUN SERVICES SARL',
      niuMasked: 'M********239',
      institution: 'LRB',
      accountMasked: '********8921',
      firstObservedAt: '2026-07-13T11:15:00Z',
      lastObservedAt: '2026-08-08T13:47:12Z',
      paymentCount: 17,
      lastPaymentStatus: 'SUCCESS',
    },
    {
      id: '7cb96138-c2b7-4f61-8bb3-b3b00599f102',
      legalName: 'ETS MBARGA & FILS',
      niuMasked: 'P********821',
      institution: 'LRB',
      accountMasked: '********1450',
      firstObservedAt: '2026-07-22T08:12:00Z',
      lastObservedAt: '2026-08-08T13:45:03Z',
      paymentCount: 6,
      lastPaymentStatus: 'PROCESSING',
    },
  ];

  find(id: string): MockObservedCustomer {
    return this.customers.find((customer) => customer.id === id) ?? this.customers[0]!;
  }
}
