import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AccountingBatchDetail } from '../models/accounting';
import { AccountingBatchQuery } from '../models/accounting-query';
import { AccountingMockService } from './accounting-mock.service';

@Injectable({ providedIn: 'root' })
export class AccountingService {
  private readonly mock = inject(AccountingMockService);

  search(query: AccountingBatchQuery): Observable<readonly AccountingBatchDetail[]> {
    return this.mock.search(query);
  }

  get(batchId: string): Observable<AccountingBatchDetail | null> {
    return this.mock.get(batchId);
  }
}
