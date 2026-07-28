import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { PartnerApiClient } from '../api/partners-api.client';
import {
  mapPartnerAuditPageResponse,
  mapPartnerResponse,
  mapPartnerStatusResponse,
} from '../api/partners-api.mapper';
import {
  ConfigureValidationThresholdRequest,
  CreatePartnerRequest,
  PartnerAuditQuery,
  PartnerDecisionRequest,
  SuspendPartnerRequest,
} from '../models/create-partners.request';
import { Partner, PartnerAuditPage, PartnerStatusView } from '../models/partners';

@Injectable({ providedIn: 'root' })
export class PartnersService {
  private readonly api = inject(PartnerApiClient);

  create(request: CreatePartnerRequest): Observable<Partner> {
    return this.api.createPartner(request).pipe(map(mapPartnerResponse));
  }

  get(partnerId: string): Observable<Partner> {
    return this.api.getPartner(partnerId).pipe(map(mapPartnerResponse));
  }

  getStatus(partnerId: string): Observable<PartnerStatusView> {
    return this.api.getPartnerStatus(partnerId).pipe(map(mapPartnerStatusResponse));
  }

  decide(partnerId: string, request: PartnerDecisionRequest): Observable<Partner> {
    return this.api.decidePartner(partnerId, request).pipe(map(mapPartnerResponse));
  }

  suspend(partnerId: string, request: SuspendPartnerRequest): Observable<Partner> {
    return this.api.suspendPartner(partnerId, request).pipe(map(mapPartnerResponse));
  }

  reactivate(partnerId: string): Observable<Partner> {
    return this.api.reactivatePartner(partnerId).pipe(map(mapPartnerResponse));
  }

  configureValidationThreshold(
    partnerId: string,
    transactionType: string,
    request: ConfigureValidationThresholdRequest,
  ): Observable<Partner> {
    return this.api
      .configureValidationThreshold(partnerId, transactionType, request)
      .pipe(map(mapPartnerResponse));
  }

  getAuditTrail(partnerId: string, query: PartnerAuditQuery): Observable<PartnerAuditPage> {
    return this.api.getPartnerAuditTrail(partnerId, query).pipe(map(mapPartnerAuditPageResponse));
  }
}
