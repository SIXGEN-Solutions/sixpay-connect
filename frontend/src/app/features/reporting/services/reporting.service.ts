import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

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
  private readonly mock = inject(ReportingMockService);

  timeline(paymentId: string, query: PaymentTimelineQuery): Observable<PaymentTimelinePage> {
    return this.mock.timeline(paymentId, query).pipe(map(mapTimelinePageResponse));
  }

  searchAudit(query: PaymentAuditQuery): Observable<PaymentAuditPage> {
    return this.mock.searchAudit(query).pipe(map(mapAuditPageResponse));
  }

  getAudit(auditId: string): Observable<PaymentAuditRecord | null> {
    return this.mock.getAudit(auditId).pipe(
      map((response) => (response ? mapAuditRecordResponse(response) : null)),
    );
  }

  requestExport(request: PaymentAuditExportRequest): Observable<PaymentAuditExportJob> {
    return this.mock.requestExport(request).pipe(map(mapExportJobResponse));
  }

  getExport(exportId: string): Observable<PaymentAuditExportJob | null> {
    return this.mock.getExport(exportId).pipe(
      map((response) => (response ? mapExportJobResponse(response) : null)),
    );
  }
}
