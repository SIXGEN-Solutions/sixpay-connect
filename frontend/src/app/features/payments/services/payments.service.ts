import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { mapPaymentDetailResponse, mapPaymentSearchPageResponse } from '../api/payments-api.mapper';
import { PaymentSearchQuery } from '../models/payment-query';
import { PaymentDetail, PaymentSearchPage } from '../models/payments';
import { PaymentsMockService } from './payments-mock.service';

@Injectable({ providedIn: 'root' })
export class PaymentsService {
  private readonly mock = inject(PaymentsMockService);

  search(query: PaymentSearchQuery): Observable<PaymentSearchPage> {
    return this.mock.search(query).pipe(map(mapPaymentSearchPageResponse));
  }

  get(paymentId: string): Observable<PaymentDetail | null> {
    return this.mock.get(paymentId).pipe(
      map((response) => (response ? mapPaymentDetailResponse(response) : null)),
    );
  }
}
