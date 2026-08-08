import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

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
  private readonly mock = inject(CustomersMockService);

  search(query: ObservedCustomerSearchQuery): Observable<ObservedCustomerSearchPage> {
    return this.mock.search(query).pipe(map(mapObservedCustomerSearchPageResponse));
  }

  get(observedCustomerId: string): Observable<ObservedCustomerDetail | null> {
    return this.mock.get(observedCustomerId).pipe(
      map((response) => (response ? mapObservedCustomerDetailResponse(response) : null)),
    );
  }

  payments(
    observedCustomerId: string,
    query: ObservedCustomerPaymentsQuery,
  ): Observable<ObservedCustomerPaymentPage> {
    return this.mock
      .payments(observedCustomerId, query)
      .pipe(map(mapObservedCustomerPaymentPageResponse));
  }
}
