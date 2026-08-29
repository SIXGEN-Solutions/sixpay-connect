import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { PaymentAuditQuery, PaymentTimelineQuery } from '../models/reporting-query';
import {
  PaymentAuditExportJobResponse,
  PaymentAuditExportRequest,
  PaymentAuditPageResponse,
  PaymentAuditRecordResponse,
  PaymentTimelinePageResponse,
} from '../models/reporting.response';

const PAYMENT_AUDIT_RECORDS_PATH = '/internal/api/v1/payment-audit-records';
const PAYMENT_AUDIT_EXPORTS_PATH = '/internal/api/v1/payment-audit-exports';

@Injectable({ providedIn: 'root' })
export class ReportingApiClient {
  private readonly http = inject(HttpClient);

  timeline(
    paymentId: string,
    query: PaymentTimelineQuery,
  ): Observable<PaymentTimelinePageResponse> {
    return this.http.get<PaymentTimelinePageResponse>(
      `/internal/api/v1/payments/${encodeURIComponent(paymentId)}/timeline`,
      { params: this.timelineParams(query) },
    );
  }

  searchAudit(query: PaymentAuditQuery): Observable<PaymentAuditPageResponse> {
    return this.http.get<PaymentAuditPageResponse>(PAYMENT_AUDIT_RECORDS_PATH, {
      params: this.auditParams(query),
    });
  }

  getAudit(auditId: string): Observable<PaymentAuditRecordResponse> {
    return this.http.get<PaymentAuditRecordResponse>(
      `${PAYMENT_AUDIT_RECORDS_PATH}/${encodeURIComponent(auditId)}`,
    );
  }

  requestExport(request: PaymentAuditExportRequest): Observable<PaymentAuditExportJobResponse> {
    return this.http.post<PaymentAuditExportJobResponse>(PAYMENT_AUDIT_EXPORTS_PATH, request);
  }

  getExport(exportId: string): Observable<PaymentAuditExportJobResponse> {
    return this.http.get<PaymentAuditExportJobResponse>(
      `${PAYMENT_AUDIT_EXPORTS_PATH}/${encodeURIComponent(exportId)}`,
    );
  }

  private timelineParams(query: PaymentTimelineQuery): HttpParams {
    let params = new HttpParams();

    if (query.category) params = params.set('category', query.category);
    if (query.occurredFrom) {
      params = params.set('occurredFrom', query.occurredFrom.toISOString());
    }
    if (query.occurredTo) {
      params = params.set('occurredTo', query.occurredTo.toISOString());
    }
    if (query.cursor) params = params.set('cursor', query.cursor);
    if (query.size !== undefined) params = params.set('size', String(query.size));

    return params;
  }

  private auditParams(query: PaymentAuditQuery): HttpParams {
    let params = new HttpParams();

    if (query.paymentId) params = params.set('paymentId', query.paymentId);
    if (query.paymentReference) {
      params = params.set('paymentReference', query.paymentReference);
    }
    if (query.observedCustomerId) {
      params = params.set('observedCustomerId', query.observedCustomerId);
    }
    if (query.actorId) params = params.set('actorId', query.actorId);
    if (query.actorType) params = params.set('actorType', query.actorType);
    if (query.action) params = params.set('action', query.action);
    if (query.result) params = params.set('result', query.result);
    if (query.reasonCode) params = params.set('reasonCode', query.reasonCode);
    if (query.correlationId) {
      params = params.set('correlationId', query.correlationId);
    }
    if (query.sourceSystem) {
      params = params.set('sourceSystem', query.sourceSystem);
    }

    params = params.set('occurredFrom', query.occurredFrom.toISOString());
    params = params.set('occurredTo', query.occurredTo.toISOString());

    if (query.sort) params = params.set('sort', query.sort);
    if (query.cursor) params = params.set('cursor', query.cursor);
    if (query.size !== undefined) params = params.set('size', String(query.size));

    return params;
  }
}
