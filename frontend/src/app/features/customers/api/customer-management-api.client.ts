import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AddBankAccountRequest,
  BankingCustomerPreviewRequest,
  CreateSubscriptionRequest,
  EnrollCustomerRequest,
  ReasonRequest,
  UpdateCustomerRequest,
} from '../models/customer-management.requests';
import {
  BankingCustomerPreviewResponse,
  CustomerMasterResponse,
  CustomerSubscriptionResponse,
} from '../models/customer-management.response';

const CUSTOMERS = '/internal/api/v1/customers';
const SUBSCRIPTIONS = '/internal/api/v1/subscriptions';

@Injectable({ providedIn: 'root' })
export class CustomerManagementApiClient {
  private readonly http = inject(HttpClient);

  list(): Observable<CustomerMasterResponse[]> {
    return this.http.get<CustomerMasterResponse[]>(CUSTOMERS);
  }

  get(customerId: string): Observable<CustomerMasterResponse> {
    return this.http.get<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}`,
    );
  }

  bankingPreview(
    request: BankingCustomerPreviewRequest,
  ): Observable<BankingCustomerPreviewResponse> {
    return this.http.post<BankingCustomerPreviewResponse>(
      `${CUSTOMERS}/banking-preview`,
      request,
    );
  }

  enroll(request: EnrollCustomerRequest): Observable<CustomerMasterResponse> {
    let params = new HttpParams()
      .set('financialInstitutionCode', request.financialInstitutionCode)
      .set('accountReference', request.accountReference);

    if (request.niu) params = params.set('niu', request.niu);
    if (request.customerNumber) {
      params = params.set('customerNumber', request.customerNumber);
    }

    return this.http.post<CustomerMasterResponse>(CUSTOMERS, null, { params });
  }

  update(
    customerId: string,
    request: UpdateCustomerRequest,
  ): Observable<CustomerMasterResponse> {
    return this.http.put<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}`,
      request,
    );
  }

  suspend(
    customerId: string,
    request: ReasonRequest,
  ): Observable<CustomerMasterResponse> {
    return this.http.post<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/suspension`,
      request,
    );
  }

  reactivate(customerId: string): Observable<CustomerMasterResponse> {
    return this.http.post<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/reactivation`,
      null,
    );
  }

  addAccount(
    customerId: string,
    request: AddBankAccountRequest,
  ): Observable<CustomerMasterResponse> {
    return this.http.post<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/accounts`,
      request,
    );
  }

  makeDefaultAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMasterResponse> {
    return this.http.put<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/accounts/${encodeURIComponent(accountId)}/default`,
      null,
    );
  }

  removeAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMasterResponse> {
    return this.http.delete<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/accounts/${encodeURIComponent(accountId)}`,
    );
  }

  subscriptions(customerId: string): Observable<CustomerSubscriptionResponse[]> {
    return this.http.get<CustomerSubscriptionResponse[]>(SUBSCRIPTIONS, {
      params: new HttpParams().set('customerId', customerId),
    });
  }

  createSubscription(
    request: CreateSubscriptionRequest,
  ): Observable<CustomerSubscriptionResponse> {
    return this.http.post<CustomerSubscriptionResponse>(SUBSCRIPTIONS, request);
  }

  activateSubscription(
    subscriptionId: string,
  ): Observable<CustomerSubscriptionResponse> {
    return this.http.post<CustomerSubscriptionResponse>(
      `${SUBSCRIPTIONS}/${encodeURIComponent(subscriptionId)}/activation`,
      null,
    );
  }

  suspendSubscription(
    subscriptionId: string,
    request: ReasonRequest,
  ): Observable<CustomerSubscriptionResponse> {
    return this.http.post<CustomerSubscriptionResponse>(
      `${SUBSCRIPTIONS}/${encodeURIComponent(subscriptionId)}/suspension`,
      request,
    );
  }

  closeSubscription(
    subscriptionId: string,
    request: ReasonRequest,
  ): Observable<void> {
    return this.http.delete<void>(
      `${SUBSCRIPTIONS}/${encodeURIComponent(subscriptionId)}`,
      { body: request },
    );
  }
}
