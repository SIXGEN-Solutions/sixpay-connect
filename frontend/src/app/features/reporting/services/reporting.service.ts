import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, throwError } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { ReportingApiClient } from '../api/reporting-api.client';
import {
  mapAuditPageResponse,
  mapAuditRecordResponse,
  mapExportJobResponse,
  mapTimelinePageResponse,
} from '../api/reporting-api.mapper';
import { PaymentAuditQuery, PaymentTimelineQuery } from '../models/reporting-query';
import { PaymentAuditExportRequest } from '../models/reporting.response';
import {
  PaymentAuditExportJob,
  PaymentAuditPage,
  PaymentAuditRecord,
  PaymentTimelinePage,
} from '../models/reporting';
import { ReportingMockService } from './reporting-mock.service';

@Injectable({ providedIn: 'root' })
export class ReportingService {
  private readonly backendMode = inject(BackendModeService);
  private readonly api = inject(ReportingApiClient);
  private readonly mock = inject(ReportingMockService);

  timeline(paymentId: string, query: PaymentTimelineQuery): Observable<PaymentTimelinePage> {
    const source$ = this.backendMode.usesApi
      ? this.api.timeline(paymentId, query)
      : this.mock.timeline(paymentId, query);

    return source$.pipe(map(mapTimelinePageResponse));
  }

  searchAudit(query: PaymentAuditQuery): Observable<PaymentAuditPage> {
    const source$ = this.backendMode.usesApi
      ? this.api.searchAudit(query)
      : this.mock.searchAudit(query);

    return source$.pipe(map(mapAuditPageResponse));
  }

  getAudit(auditId: string): Observable<PaymentAuditRecord | null> {
    if (this.backendMode.usesMock) {
      return this.mock
        .getAudit(auditId)
        .pipe(map((response) => (response ? mapAuditRecordResponse(response) : null)));
    }

    return this.api.getAudit(auditId).pipe(
      map(mapAuditRecordResponse),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          return of(null);
        }

        return throwError(() => error);
      }),
    );
  }

  requestExport(request: PaymentAuditExportRequest): Observable<PaymentAuditExportJob> {
    const source$ = this.backendMode.usesApi
      ? this.api.requestExport(request)
      : this.mock.requestExport(request);

    return source$.pipe(map(mapExportJobResponse));
  }

  getExport(exportId: string): Observable<PaymentAuditExportJob | null> {
    if (this.backendMode.usesMock) {
      return this.mock
        .getExport(exportId)
        .pipe(map((response) => (response ? mapExportJobResponse(response) : null)));
    }

    return this.api.getExport(exportId).pipe(
      map(mapExportJobResponse),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          return of(null);
        }

        return throwError(() => error);
      }),
    );
  }
}
