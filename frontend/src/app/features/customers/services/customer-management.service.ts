import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { CustomerManagementApiClient } from '../api/customer-management-api.client';
import {
  mapBankingPreview,
  mapCustomerMaster,
  mapCustomerPage,
  mapCustomerSubscription,
} from '../api/customer-management-api.mapper';
import {
  BankingCustomerPreview,
  CustomerMaster,
  CustomerPage,
  CustomerSearchCriteria,
  CustomerSubscription,
} from '../models/customer-management';
import {
  AddBankAccountRequest,
  BankingCustomerPreviewRequest,
  CreateSubscriptionRequest,
  EnrollCustomerRequest,
  ReasonRequest,
  UpdateCustomerRequest,
} from '../models/customer-management.requests';

@Injectable({
  providedIn: 'root',
})
export class CustomerManagementService {

  private readonly api =
    inject(CustomerManagementApiClient);

  search(
    criteria: CustomerSearchCriteria,
  ): Observable<CustomerPage> {
    return this.api
      .search(criteria)
      .pipe(
        map(mapCustomerPage),
      );
  }

  get(
    customerId: string,
  ): Observable<CustomerMaster> {
    return this.api
      .get(customerId)
      .pipe(
        map(mapCustomerMaster),
      );
  }

  bankingPreview(
    request: BankingCustomerPreviewRequest,
  ): Observable<BankingCustomerPreview> {
    return this.api
      .bankingPreview(request)
      .pipe(
        map(mapBankingPreview),
      );
  }

  enroll(
    request: EnrollCustomerRequest,
  ): Observable<CustomerMaster> {
    return this.api
      .enroll(request)
      .pipe(
        map(mapCustomerMaster),
      );
  }

  update(
    customerId: string,
    request: UpdateCustomerRequest,
  ): Observable<CustomerMaster> {
    return this.api
      .update(
        customerId,
        request,
      )
      .pipe(
        map(mapCustomerMaster),
      );
  }

  suspend(
    customerId: string,
    request: ReasonRequest,
  ): Observable<CustomerMaster> {
    return this.api
      .suspend(
        customerId,
        request,
      )
      .pipe(
        map(mapCustomerMaster),
      );
  }

  reactivate(
    customerId: string,
  ): Observable<CustomerMaster> {
    return this.api
      .reactivate(customerId)
      .pipe(
        map(mapCustomerMaster),
      );
  }

  addAccount(
    customerId: string,
    request: AddBankAccountRequest,
  ): Observable<CustomerMaster> {
    return this.api
      .addAccount(
        customerId,
        request,
      )
      .pipe(
        map(mapCustomerMaster),
      );
  }

  makeDefaultAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMaster> {
    return this.api
      .makeDefaultAccount(
        customerId,
        accountId,
      )
      .pipe(
        map(mapCustomerMaster),
      );
  }

  removeAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMaster> {
    return this.api
      .removeAccount(
        customerId,
        accountId,
      )
      .pipe(
        map(mapCustomerMaster),
      );
  }

  subscriptions(
    customerId: string,
  ): Observable<CustomerSubscription[]> {
    return this.api
      .subscriptions(customerId)
      .pipe(
        map(
          (items) =>
            items.map(
              mapCustomerSubscription,
            ),
        ),
      );
  }

  createSubscription(
    request: CreateSubscriptionRequest,
  ): Observable<CustomerSubscription> {
    return this.api
      .createSubscription(request)
      .pipe(
        map(mapCustomerSubscription),
      );
  }

  activateSubscription(
    subscriptionId: string,
  ): Observable<CustomerSubscription> {
    return this.api
      .activateSubscription(
        subscriptionId,
      )
      .pipe(
        map(mapCustomerSubscription),
      );
  }

  suspendSubscription(
    subscriptionId: string,
    request: ReasonRequest,
  ): Observable<CustomerSubscription> {
    return this.api
      .suspendSubscription(
        subscriptionId,
        request,
      )
      .pipe(
        map(mapCustomerSubscription),
      );
  }

  closeSubscription(
    subscriptionId: string,
    request: ReasonRequest,
  ): Observable<void> {
    return this.api.closeSubscription(
      subscriptionId,
      request,
    );
  }
}