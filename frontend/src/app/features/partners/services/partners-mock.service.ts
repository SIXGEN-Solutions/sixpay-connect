import { inject, Injectable } from '@angular/core';
import { delay, Observable, of, throwError } from 'rxjs';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';
import {
  ConfigureValidationThresholdRequest,
  CreatePartnerRequest,
  PartnerAuditQuery,
  PartnerDecisionRequest,
  SuspendPartnerRequest,
} from '../models/create-partners.request';
import { PartnerSearchQuery } from '../models/partner-query';
import {
  PartnerAuditPageResponse,
  PartnerPageResponse,
  PartnerResponse,
  PartnerStatusResponse,
  PartnerSummaryResponse,
  PartnerStatus,
} from '../models/partners.response';

const MOCK_DELAY_MS = 500;

const FIXED_PARTNERS: readonly PartnerResponse[] = [
  partner('10000000-0000-4000-8000-000000000001', 'TresorPay', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000002', 'La Régionale Bank', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000003', 'Afriland First Bank', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000004', 'BICEC', 'PENDING_VALIDATION', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000005', 'African Golden Bank', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000006', 'UBA Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000007', 'CCA Bank', 'SUSPENDED', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000008', 'BGFI Bank Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000009', 'SCB Cameroun', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000010', 'Ecobank Cameroon', 'REJECTED', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000011', 'Commercial Bank Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000012', 'Banque Atlantique Cameroun', 'PENDING_VALIDATION', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000013', 'Union Bank of Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000014', 'Access Bank Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000015', 'Citibank Cameroon', 'SUSPENDED', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000016', 'NFC Bank', 'PENDING_VALIDATION', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000017', 'CBC', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000018', 'Bange Bank Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000019', 'Bank of Africa Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000020', 'Société Générale Cameroun', 'PENDING_VALIDATION', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000021', 'Orange Money Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000022', 'MTN Mobile Money Cameroon', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000023', 'Express Union Mobile Money', 'SUSPENDED', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000024', 'Campost Money', 'PENDING_VALIDATION', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000025', 'YUP Cameroon', 'REJECTED', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000026', 'Cameroon Treasury Gateway', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000027', 'Public Revenue Gateway', 'ACTIVE', 'PAYMENT'),
  partner('10000000-0000-4000-8000-000000000028', 'Municipal Services Gateway', 'PENDING_VALIDATION', 'PAYMENT'),
];

@Injectable({ providedIn: 'root' })
export class PartnersMockService {
  private readonly scenario = inject(MockScenarioService);
  private readonly partners = new Map(
    FIXED_PARTNERS.map((item) => [item.id, clonePartner(item)]),
  );

  search(query: PartnerSearchQuery): Observable<PartnerPageResponse> {
    const page = query.page ?? 0;
    const size = query.size ?? 20;

    if (this.scenario.scenario() === 'error') {
      return throwError(() => new Error('Mock Partner catalog failure'));
    }

    if (this.scenario.scenario() === 'empty') {
      return this.withScenarioDelay(
        of({
          items: [],
          page,
          size,
          totalElements: 0,
          totalPages: 0,
        }),
      );
    }

    const items = [...this.partners.values()]
      .sort((left, right) =>
        left.legalName.localeCompare(right.legalName) || left.id.localeCompare(right.id),
      )
      .map(toSummary);

    const start = Math.min(page * size, items.length);
    const end = Math.min(start + size, items.length);

    return this.withScenarioDelay(
      of({
        items: items.slice(start, end),
        page,
        size,
        totalElements: items.length,
        totalPages: items.length === 0 ? 0 : Math.ceil(items.length / size),
      }),
    );
  }

  create(request: CreatePartnerRequest): Observable<PartnerResponse> {
    if (this.scenario.scenario() === 'error') {
      return throwError(() => new Error('Mock Partner creation failure'));
    }

    const now = new Date().toISOString();
    const created: PartnerResponse = {
      id: crypto.randomUUID(),
      legalName: request.legalName,
      technicalContactName: request.technicalContactName,
      technicalContactEmail: request.technicalContactEmail,
      authorizedTransactionTypes: [...request.authorizedTransactionTypes],
      status: 'PENDING_VALIDATION',
      statusReason: null,
      validationThresholds: [],
      createdAt: now,
      updatedAt: now,
    };

    this.partners.set(created.id, created);
    return this.withScenarioDelay(of(clonePartner(created)));
  }

  get(partnerId: string): Observable<PartnerResponse> {
    const existing = this.partners.get(partnerId);
    if (!existing) {
      return throwError(() => new Error(`Mock Partner not found: ${partnerId}`));
    }
    return this.withScenarioDelay(of(clonePartner(existing)));
  }

  getStatus(partnerId: string): Observable<PartnerStatusResponse> {
    const existing = this.partners.get(partnerId);
    if (!existing) {
      return throwError(() => new Error(`Mock Partner not found: ${partnerId}`));
    }

    return this.withScenarioDelay(
      of({
        partnerId: existing.id,
        status: existing.status,
        statusReason: existing.statusReason ?? null,
        connection: {
          apiBasePath: `/api/v1/partners/${existing.id}`,
          supportedAuthenticationMethods: ['MTLS', 'API_KEY'],
          newTransactionsAllowed: existing.status === 'ACTIVE',
        },
        updatedAt: existing.updatedAt,
      }),
    );
  }

  decide(
    partnerId: string,
    request: PartnerDecisionRequest,
  ): Observable<PartnerResponse> {
    const existing = this.partners.get(partnerId);
    if (!existing) {
      return throwError(() => new Error(`Mock Partner not found: ${partnerId}`));
    }

    const nextStatus: PartnerStatus =
      request.decision === 'APPROVE' ? 'ACTIVE' : 'REJECTED';

    return this.updatePartner(existing, {
      status: nextStatus,
      statusReason: request.reason ?? null,
    });
  }

  suspend(
    partnerId: string,
    request: SuspendPartnerRequest,
  ): Observable<PartnerResponse> {
    const existing = this.partners.get(partnerId);
    if (!existing) {
      return throwError(() => new Error(`Mock Partner not found: ${partnerId}`));
    }

    return this.updatePartner(existing, {
      status: 'SUSPENDED',
      statusReason: request.reason,
    });
  }

  reactivate(partnerId: string): Observable<PartnerResponse> {
    const existing = this.partners.get(partnerId);
    if (!existing) {
      return throwError(() => new Error(`Mock Partner not found: ${partnerId}`));
    }

    return this.updatePartner(existing, {
      status: 'ACTIVE',
      statusReason: null,
    });
  }

  configureValidationThreshold(
    partnerId: string,
    transactionType: string,
    request: ConfigureValidationThresholdRequest,
  ): Observable<PartnerResponse> {
    const existing = this.partners.get(partnerId);
    if (!existing) {
      return throwError(() => new Error(`Mock Partner not found: ${partnerId}`));
    }

    const filtered = existing.validationThresholds.filter(
      (item) =>
        item.transactionType !== transactionType ||
        item.currency !== request.currency,
    );

    return this.updatePartner(existing, {
      validationThresholds: [
        ...filtered,
        {
          transactionType,
          currency: request.currency,
          amount: request.amount,
          validationLevels: request.validationLevels,
        },
      ],
    });
  }

  getAuditTrail(
    partnerId: string,
    query: PartnerAuditQuery,
  ): Observable<PartnerAuditPageResponse> {
    const page = query.page ?? 0;
    const size = query.size ?? 50;
    const existing = this.partners.get(partnerId);

    if (!existing || this.scenario.scenario() === 'empty') {
      return this.withScenarioDelay(
        of({
          items: [],
          page,
          size,
          totalElements: 0,
          totalPages: 0,
        }),
      );
    }

    const items = [
      {
        partnerId,
        action: 'PARTNER_CREATED',
        result: 'SUCCESS',
        actorId: 'mock-admin',
        correlationId: 'mock-correlation-created',
        details: `Partner ${existing.legalName} created`,
        occurredAt: existing.createdAt,
      },
      {
        partnerId,
        action: 'PARTNER_STATUS_OBSERVED',
        result: 'SUCCESS',
        actorId: 'mock-auditor',
        correlationId: 'mock-correlation-status',
        details: `Current mock status=${existing.status}`,
        occurredAt: existing.updatedAt,
      },
    ];

    const start = Math.min(page * size, items.length);
    return this.withScenarioDelay(
      of({
        items: items.slice(start, start + size),
        page,
        size,
        totalElements: items.length,
        totalPages: Math.ceil(items.length / size),
      }),
    );
  }

  private updatePartner(
    existing: PartnerResponse,
    patch: Partial<PartnerResponse>,
  ): Observable<PartnerResponse> {
    const updated: PartnerResponse = {
      ...existing,
      ...patch,
      updatedAt: new Date().toISOString(),
    };
    this.partners.set(updated.id, updated);
    return this.withScenarioDelay(of(clonePartner(updated)));
  }

  private withScenarioDelay<T>(source: Observable<T>): Observable<T> {
    return this.scenario.scenario() === 'loading'
      ? source.pipe(delay(MOCK_DELAY_MS))
      : source;
  }
}

function partner(
  id: string,
  legalName: string,
  status: PartnerStatus,
  transactionType: string,
): PartnerResponse {
  const index = Number(id.slice(-3));
  const day = String((index % 28) + 1).padStart(2, '0');

  return {
    id,
    legalName,
    technicalContactName: `${legalName} Operations`,
    technicalContactEmail: `operations${index}@example.test`,
    authorizedTransactionTypes: [transactionType],
    status,
    statusReason: status === 'SUSPENDED' ? 'Mock compliance review' : null,
    validationThresholds: [],
    createdAt: `2026-07-${day}T09:00:00Z`,
    updatedAt: `2026-08-${day}T11:30:00Z`,
  };
}

function toSummary(response: PartnerResponse): PartnerSummaryResponse {
  return {
    id: response.id,
    legalName: response.legalName,
    technicalContactName: response.technicalContactName,
    technicalContactEmail: response.technicalContactEmail,
    authorizedTransactionTypes: [...response.authorizedTransactionTypes],
    status: response.status,
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  };
}

function clonePartner(response: PartnerResponse): PartnerResponse {
  return {
    ...response,
    authorizedTransactionTypes: [...response.authorizedTransactionTypes],
    validationThresholds: response.validationThresholds.map((threshold) => ({
      ...threshold,
    })),
  };
}
