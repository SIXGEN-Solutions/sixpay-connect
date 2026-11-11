import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { PaymentSearchQuery } from '../models/payment-query';
import { PaymentT0ManualResponse } from '../models/payment-t0-manual-reconciliation';
import { PaymentDetailResponse, PaymentSearchPageResponse } from '../models/payments.response';

const PAYMENTS_API_PATH = '/internal/api/v1/payments';

@Injectable({ providedIn: 'root' })
export class PaymentsApiClient {
  private readonly http = inject(HttpClient);

  searchPayments(query: PaymentSearchQuery): Observable<PaymentSearchPageResponse> {
    return this.http.get<PaymentSearchPageResponse>(PAYMENTS_API_PATH, {
      params: this.searchParams(query),
    });
  }

  getPayment(paymentId: string): Observable<PaymentDetailResponse> {
    return this.http.get<PaymentDetailResponse>(
      `${PAYMENTS_API_PATH}/${encodeURIComponent(paymentId)}`,
    );
  }

  /** Approved R3 internal operator POST. Correlation ID is attached by the global interceptor. */
  reconcileT0(paymentIds: readonly string[]): Observable<PaymentT0ManualResponse> {
    return this.http.post<PaymentT0ManualResponse>(`${PAYMENTS_API_PATH}/reconciliation`, {
      paymentIds: [...paymentIds],
    });
  }

  private searchParams(query: PaymentSearchQuery): HttpParams {
    let params = new HttpParams();

    if (query.paymentReference) {
      params = params.set('paymentReference', query.paymentReference);
    }
    if (query.tresorPayRequestId) {
      params = params.set('tresorPayRequestId', query.tresorPayRequestId);
    }
    if (query.observedCustomerId) {
      params = params.set('observedCustomerId', query.observedCustomerId);
    }
    if (query.financialInstitutionCode) {
      params = params.set('financialInstitutionCode', query.financialInstitutionCode);
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.reasonCode) {
      params = params.set('reasonCode', query.reasonCode);
    }
    if (query.createdFrom) {
      params = params.set('createdFrom', query.createdFrom.toISOString());
    }
    if (query.createdTo) {
      params = params.set('createdTo', query.createdTo.toISOString());
    }
    if (query.amountMin !== undefined) {
      params = params.set('amountMin', String(query.amountMin));
    }
    if (query.amountMax !== undefined) {
      params = params.set('amountMax', String(query.amountMax));
    }
    if (query.currency) {
      params = params.set('currency', query.currency);
    }
    if (query.sort) {
      params = params.set('sort', query.sort);
    }
    if (query.cursor) {
      params = params.set('cursor', query.cursor);
    }
    if (query.size !== undefined) {
      params = params.set('size', String(query.size));
    }

    return params;
  }
}
