import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, throwError } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { AccountingApiClient } from '../api/accounting-api.client';
import {
  mapAccountingBatchDetailResponse,
  mapAccountingBatchSummaryResponse,
} from '../api/accounting-api.mapper';
import { AccountingBatchDetail, AccountingBatchSummary } from '../models/accounting';
import { AccountingBatchQuery } from '../models/accounting-query';
import { AccountingT1ManualExecutionResponse } from '../models/accounting-t1-execution';
import { AccountingMockService } from './accounting-mock.service';

@Injectable({
  providedIn: 'root',
})
export class AccountingService {
  private readonly backendMode = inject(BackendModeService);

  private readonly api = inject(AccountingApiClient);

  private readonly mock = inject(AccountingMockService);

  search(query: AccountingBatchQuery): Observable<readonly AccountingBatchSummary[]> {
    return this.backendMode.usesApi
      ? this.api
          .search(query)
          .pipe(map((page) => page.content.map(mapAccountingBatchSummaryResponse)))
      : this.mock.search(query);
  }

  executeT1Manually(
    businessDate: string,
  ): Observable<AccountingT1ManualExecutionResponse> {
    if (!this.backendMode.usesApi) {
      return throwError(
        () => new Error(
          'Manual T1 execution requires the SIXPAY API backend',
        ),
      );
    }

    return this.api.executeT1Manually(businessDate);
  }

  get(batchId: string): Observable<AccountingBatchDetail | null> {
    return this.backendMode.usesApi
      ? this.api.get(batchId).pipe(
          map(mapAccountingBatchDetailResponse),
          catchError((error: unknown) => {
            if (error instanceof HttpErrorResponse && error.status === 404) {
              return of(null);
            }

            return throwError(() => error);
          }),
        )
      : this.mock.get(batchId);
  }
}
