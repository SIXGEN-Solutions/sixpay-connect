import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PartnerApiClient } from '../api/partners-api.client';
import {
  mapPartnerAuditPageResponse,
  mapPartnerPageResponse,
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
import { PartnerSearchQuery } from '../models/partner-query';
import {
  Partner,
  PartnerAuditPage,
  PartnerPage,
  PartnerStatusView,
} from '../models/partners';
import { PartnersMockService } from './partners-mock.service';

@Injectable({ providedIn: 'root' })
export class PartnersService {
  private readonly backendMode = inject(BackendModeService);
  private readonly api = inject(PartnerApiClient);
  private readonly mock = inject(PartnersMockService);

  search(query: PartnerSearchQuery): Observable<PartnerPage> {
    const source$ = this.backendMode.usesApi
      ? this.api.listPartners(query.page ?? 0, query.size ?? 20)
      : this.mock.search(query);

    return source$.pipe(map(mapPartnerPageResponse));
  }

  create(request: CreatePartnerRequest): Observable<Partner> {
    const source$ = this.backendMode.usesApi
      ? this.api.createPartner(request)
      : this.mock.create(request);

    return source$.pipe(map(mapPartnerResponse));
  }

  get(partnerId: string): Observable<Partner> {
    const source$ = this.backendMode.usesApi
      ? this.api.getPartner(partnerId)
      : this.mock.get(partnerId);

    return source$.pipe(map(mapPartnerResponse));
  }

  getStatus(partnerId: string): Observable<PartnerStatusView> {
    const source$ = this.backendMode.usesApi
      ? this.api.getPartnerStatus(partnerId)
      : this.mock.getStatus(partnerId);

    return source$.pipe(map(mapPartnerStatusResponse));
  }

  decide(
    partnerId: string,
    request: PartnerDecisionRequest,
  ): Observable<Partner> {
    const source$ = this.backendMode.usesApi
      ? this.api.decidePartner(partnerId, request)
      : this.mock.decide(partnerId, request);

    return source$.pipe(map(mapPartnerResponse));
  }

  suspend(
    partnerId: string,
    request: SuspendPartnerRequest,
  ): Observable<Partner> {
    const source$ = this.backendMode.usesApi
      ? this.api.suspendPartner(partnerId, request)
      : this.mock.suspend(partnerId, request);

    return source$.pipe(map(mapPartnerResponse));
  }

  reactivate(partnerId: string): Observable<Partner> {
    const source$ = this.backendMode.usesApi
      ? this.api.reactivatePartner(partnerId)
      : this.mock.reactivate(partnerId);

    return source$.pipe(map(mapPartnerResponse));
  }

  configureValidationThreshold(
    partnerId: string,
    transactionType: string,
    request: ConfigureValidationThresholdRequest,
  ): Observable<Partner> {
    const source$ = this.backendMode.usesApi
      ? this.api.configureValidationThreshold(partnerId, transactionType, request)
      : this.mock.configureValidationThreshold(partnerId, transactionType, request);

    return source$.pipe(map(mapPartnerResponse));
  }

  getAuditTrail(
    partnerId: string,
    query: PartnerAuditQuery,
  ): Observable<PartnerAuditPage> {
    const source$ = this.backendMode.usesApi
      ? this.api.getPartnerAuditTrail(partnerId, query)
      : this.mock.getAuditTrail(partnerId, query);

    return source$.pipe(map(mapPartnerAuditPageResponse));
  }
}
