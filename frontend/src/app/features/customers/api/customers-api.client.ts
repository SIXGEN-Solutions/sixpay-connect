import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ObservedCustomerPaymentsQuery,
  ObservedCustomerSearchQuery,
} from '../models/customer-query';
import {
  ObservedCustomerDetailResponse,
  ObservedCustomerPaymentPageResponse,
  ObservedCustomerSearchPageResponse,
} from '../models/customers.response';

const OBSERVED_CUSTOMERS_API_PATH = '/internal/api/v1/observed-customers';

@Injectable({ providedIn: 'root' })
export class CustomersApiClient {
  private readonly http = inject(HttpClient);

  search(query: ObservedCustomerSearchQuery): Observable<ObservedCustomerSearchPageResponse> {
    return this.http.get<ObservedCustomerSearchPageResponse>(OBSERVED_CUSTOMERS_API_PATH, {
      params: this.searchParams(query),
    });
  }

  get(observedCustomerId: string): Observable<ObservedCustomerDetailResponse> {
    return this.http.get<ObservedCustomerDetailResponse>(
      `${OBSERVED_CUSTOMERS_API_PATH}/${encodeURIComponent(observedCustomerId)}`,
    );
  }

  payments(
    observedCustomerId: string,
    query: ObservedCustomerPaymentsQuery,
  ): Observable<ObservedCustomerPaymentPageResponse> {
    return this.http.get<ObservedCustomerPaymentPageResponse>(
      `${OBSERVED_CUSTOMERS_API_PATH}/${encodeURIComponent(observedCustomerId)}/payments`,
      { params: this.paymentsParams(query) },
    );
  }

  private searchParams(query: ObservedCustomerSearchQuery): HttpParams {
    let params = new HttpParams();

    if (query.niu) {
      params = params.set('niu', query.niu);
    }
    if (query.legalName) {
      params = params.set('legalName', query.legalName);
    }
    if (query.financialInstitutionCode) {
      params = params.set('financialInstitutionCode', query.financialInstitutionCode);
    }
    if (query.lastPaymentStatus) {
      params = params.set('lastPaymentStatus', query.lastPaymentStatus);
    }
    if (query.lastFailureReasonCode) {
      params = params.set('lastFailureReasonCode', query.lastFailureReasonCode);
    }
    if (query.firstObservedFrom) {
      params = params.set('firstObservedFrom', query.firstObservedFrom.toISOString());
    }
    if (query.firstObservedTo) {
      params = params.set('firstObservedTo', query.firstObservedTo.toISOString());
    }
    if (query.lastObservedFrom) {
      params = params.set('lastObservedFrom', query.lastObservedFrom.toISOString());
    }
    if (query.lastObservedTo) {
      params = params.set('lastObservedTo', query.lastObservedTo.toISOString());
    }
    if (query.paymentFrom) {
      params = params.set('paymentFrom', query.paymentFrom.toISOString());
    }
    if (query.paymentTo) {
      params = params.set('paymentTo', query.paymentTo.toISOString());
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

  private paymentsParams(query: ObservedCustomerPaymentsQuery): HttpParams {
    let params = new HttpParams();

    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.createdFrom) {
      params = params.set('createdFrom', query.createdFrom.toISOString());
    }
    if (query.createdTo) {
      params = params.set('createdTo', query.createdTo.toISOString());
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
