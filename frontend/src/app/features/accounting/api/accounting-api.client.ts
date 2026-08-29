import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AccountingBatchQuery } from '../models/accounting-query';
import {
  AccountingBatchDetailResponse,
  AccountingBatchPageResponse,
} from '../models/accounting.response';

const ACCOUNTING_BATCHES_API_PATH = '/internal/api/v1/accounting-batches';

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
}
