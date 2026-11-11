import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, throwError } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PaymentsApiClient } from '../api/payments-api.client';
import { mapPaymentDetailResponse, mapPaymentSearchPageResponse } from '../api/payments-api.mapper';
import { PaymentSearchQuery } from '../models/payment-query';
import { PaymentT0ManualResponse } from '../models/payment-t0-manual-reconciliation';
import { PaymentDetail, PaymentSearchPage } from '../models/payments';
import { PaymentsMockService } from './payments-mock.service';

@Injectable({ providedIn: 'root' })
export class PaymentsService {
  private readonly backendMode = inject(BackendModeService);
  private readonly api = inject(PaymentsApiClient);
  private readonly mock = inject(PaymentsMockService);

  search(query: PaymentSearchQuery): Observable<PaymentSearchPage> {
    const source$ = this.backendMode.usesApi
      ? this.api.searchPayments(query)
      : this.mock.search(query);

    return source$.pipe(map(mapPaymentSearchPageResponse));
  }

  /** No simulated financial reconciliation: the operator command requires the real API. */
  reconcileT0(paymentIds: readonly string[]): Observable<PaymentT0ManualResponse> {
    if (!this.backendMode.usesApi) {
      return throwError(() => new Error('T0 reconciliation requires API mode'));
    }
    return this.api.reconcileT0(paymentIds);
  }

  get(paymentId: string): Observable<PaymentDetail | null> {
    if (this.backendMode.usesMock) {
      return this.mock
        .get(paymentId)
        .pipe(map((response) => (response ? mapPaymentDetailResponse(response) : null)));
    }

    return this.api.getPayment(paymentId).pipe(
      map(mapPaymentDetailResponse),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          return of(null);
        }

        return throwError(() => error);
      }),
    );
  }
}
