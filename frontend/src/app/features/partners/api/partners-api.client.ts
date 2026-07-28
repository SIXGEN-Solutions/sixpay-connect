import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ConfigureValidationThresholdRequest,
  CreatePartnerRequest,
  PartnerAuditQuery,
  PartnerDecisionRequest,
  SuspendPartnerRequest,
} from '../models/create-partners.request';
import {
  PartnerAuditPageResponse,
  PartnerResponse,
  PartnerStatusResponse,
} from '../models/partners.response';

const PARTNERS_API_PATH = '/api/v1/partners';

@Injectable({ providedIn: 'root' })
export class PartnerApiClient {
  private readonly http = inject(HttpClient);

  createPartner(request: CreatePartnerRequest): Observable<PartnerResponse> {
    return this.http.post<PartnerResponse>(PARTNERS_API_PATH, request);
  }

  getPartner(partnerId: string): Observable<PartnerResponse> {
    return this.http.get<PartnerResponse>(this.partnerPath(partnerId));
  }

  getPartnerStatus(partnerId: string): Observable<PartnerStatusResponse> {
    return this.http.get<PartnerStatusResponse>(`${this.partnerPath(partnerId)}/status`);
  }

  decidePartner(partnerId: string, request: PartnerDecisionRequest): Observable<PartnerResponse> {
    return this.http.post<PartnerResponse>(`${this.partnerPath(partnerId)}/validation`, request);
  }

  suspendPartner(partnerId: string, request: SuspendPartnerRequest): Observable<PartnerResponse> {
    return this.http.post<PartnerResponse>(`${this.partnerPath(partnerId)}/suspension`, request);
  }

  reactivatePartner(partnerId: string): Observable<PartnerResponse> {
    return this.http.post<PartnerResponse>(`${this.partnerPath(partnerId)}/reactivation`, null);
  }

  configureValidationThreshold(
    partnerId: string,
    transactionType: string,
    request: ConfigureValidationThresholdRequest,
  ): Observable<PartnerResponse> {
    const encodedTransactionType = encodeURIComponent(transactionType);
    return this.http.put<PartnerResponse>(
      `${this.partnerPath(partnerId)}/validation-thresholds/${encodedTransactionType}`,
      request,
    );
  }

  getPartnerAuditTrail(
    partnerId: string,
    query: PartnerAuditQuery,
  ): Observable<PartnerAuditPageResponse> {
    let params = new HttpParams().set('from', query.from).set('to', query.to);

    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }

    return this.http.get<PartnerAuditPageResponse>(`${this.partnerPath(partnerId)}/audit`, {
      params,
    });
  }

  private partnerPath(partnerId: string): string {
    return `${PARTNERS_API_PATH}/${encodeURIComponent(partnerId)}`;
  }
}
