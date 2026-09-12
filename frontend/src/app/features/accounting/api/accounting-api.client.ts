import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AccountingBatchQuery } from '../models/accounting-query';
import {
  AccountingBatchDetailResponse,
  AccountingBatchPageResponse,
} from '../models/accounting.response';
import { AccountingT1ManualExecutionResponse } from '../models/accounting-t1-execution';
import {
  AccountingT1OperationalPageResponse,
  AccountingT1OperationalQuery,
  AccountingT1OperationalResponse,
} from '../models/accounting-t1-operational';

const ACCOUNTING_BATCHES_API_PATH = '/internal/api/v1/accounting-batches';
const ACCOUNTING_T1_OPERATIONS_API_PATH = '/internal/api/v1/accounting-t1-operations';

@Injectable({ providedIn: 'root' })
export class AccountingApiClient {
  private readonly http = inject(HttpClient);

  search(query: AccountingBatchQuery): Observable<AccountingBatchPageResponse> {
    let params = new HttpParams().set('page', query.page ?? 0).set('size', query.size ?? 20);

    if (query.businessDate !== undefined) {
      params = params.set('businessDate', query.businessDate);
    }

    if (query.status !== undefined) {
      params = params.set('status', query.status);
    }

    return this.http.get<AccountingBatchPageResponse>(ACCOUNTING_BATCHES_API_PATH, { params });
  }

  get(batchId: string): Observable<AccountingBatchDetailResponse> {
    return this.http.get<AccountingBatchDetailResponse>(
      `${ACCOUNTING_BATCHES_API_PATH}/${encodeURIComponent(batchId)}`,
    );
  }

  searchT1Operations(
    query: AccountingT1OperationalQuery,
  ): Observable<AccountingT1OperationalPageResponse> {
    let params = new HttpParams().set('page', query.page ?? 0).set('size', query.size ?? 20);

    if (query.businessDate !== undefined) {
      params = params.set('businessDate', query.businessDate);
    }

    if (query.status !== undefined) {
      params = params.set('status', query.status);
    }

    if (query.paymentReference !== undefined && query.paymentReference.trim() !== '') {
      params = params.set('paymentReference', query.paymentReference.trim());
    }

    return this.http.get<AccountingT1OperationalPageResponse>(ACCOUNTING_T1_OPERATIONS_API_PATH, {
      params,
    });
  }

  getT1Operation(candidateId: string): Observable<AccountingT1OperationalResponse> {
    return this.http.get<AccountingT1OperationalResponse>(
      `${ACCOUNTING_T1_OPERATIONS_API_PATH}/${encodeURIComponent(candidateId)}`,
    );
  }

  executeT1Manually(
    businessDate: string,
  ): Observable<AccountingT1ManualExecutionResponse> {
    return this.http.post<AccountingT1ManualExecutionResponse>(
      '/internal/api/v1/accounting-t1-executions',
      { businessDate },
      {
        headers: {
          'X-Correlation-ID': crypto.randomUUID(),
        },
      },
    );
  }
}
