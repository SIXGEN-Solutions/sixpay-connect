import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, throwError } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { CustomersApiClient } from '../api/customers-api.client';
import {
  mapObservedCustomerDetailResponse,
  mapObservedCustomerPaymentPageResponse,
  mapObservedCustomerSearchPageResponse,
} from '../api/customers-api.mapper';
import {
  ObservedCustomerPaymentsQuery,
  ObservedCustomerSearchQuery,
} from '../models/customer-query';
import {
  ObservedCustomerDetail,
  ObservedCustomerPaymentPage,
  ObservedCustomerSearchPage,
} from '../models/customers';
import { CustomersMockService } from './customers-mock.service';

@Injectable({ providedIn: 'root' })
export class CustomersService {
  private readonly backendMode = inject(BackendModeService);
  private readonly api = inject(CustomersApiClient);
  private readonly mock = inject(CustomersMockService);

  search(query: ObservedCustomerSearchQuery): Observable<ObservedCustomerSearchPage> {
    const source$ = this.backendMode.usesApi ? this.api.search(query) : this.mock.search(query);

    return source$.pipe(map(mapObservedCustomerSearchPageResponse));
  }

  get(observedCustomerId: string): Observable<ObservedCustomerDetail | null> {
    if (this.backendMode.usesMock) {
      return this.mock
        .get(observedCustomerId)
        .pipe(map((response) => (response ? mapObservedCustomerDetailResponse(response) : null)));
    }

    return this.api.get(observedCustomerId).pipe(
      map(mapObservedCustomerDetailResponse),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          return of(null);
        }

        return throwError(() => error);
      }),
    );
  }

  payments(
    observedCustomerId: string,
    query: ObservedCustomerPaymentsQuery,
  ): Observable<ObservedCustomerPaymentPage> {
    const source$ = this.backendMode.usesApi
      ? this.api.payments(observedCustomerId, query)
      : this.mock.payments(observedCustomerId, query);

    return source$.pipe(map(mapObservedCustomerPaymentPageResponse));
  }
}
