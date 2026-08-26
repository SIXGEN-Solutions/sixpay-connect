import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PartnerApiClient } from './partners-api.client';
import { PartnerResponse } from '../models/partners.response';

describe('PartnerApiClient', () => {
  const partnerId = '8ec6a427-406f-4f93-b271-cbc819a4c1dd';
  let client: PartnerApiClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PartnerApiClient,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(PartnerApiClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('lists partners with page-based pagination', () => {
    client.listPartners(2, 20).subscribe();

    const request = httpTesting.expectOne(
      (candidate) => candidate.url === '/api/v1/partners',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');

    request.flush({
      items: [],
      page: 2,
      size: 20,
      totalElements: 37,
      totalPages: 2,
    });
  });

  it('creates and reads a partner with the frozen paths', () => {
    const createRequest = {
      legalName: 'Acme Payments',
      technicalContactName: 'Alice Ops',
      technicalContactEmail: 'alice.ops@example.com',
      authorizedTransactionTypes: ['PAYMENT'],
    };

    client.createPartner(createRequest).subscribe((partner) => {
      expect(partner.status).toBe('PENDING_VALIDATION');
    });
    const create = httpTesting.expectOne('/api/v1/partners');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(createRequest);
    create.flush(partnerResponse());

    client.getPartner(partnerId).subscribe();
    const get = httpTesting.expectOne(`/api/v1/partners/${partnerId}`);
    expect(get.request.method).toBe('GET');
    get.flush(partnerResponse());

    client.getPartnerStatus(partnerId).subscribe();
    const status = httpTesting.expectOne(
      `/api/v1/partners/${partnerId}/status`,
    );
    expect(status.request.method).toBe('GET');
    status.flush({
      partnerId,
      status: 'ACTIVE',
      connection: {
        apiBasePath: `/api/v1/partners/${partnerId}`,
        supportedAuthenticationMethods: ['MTLS'],
        newTransactionsAllowed: true,
      },
      updatedAt: '2026-07-28T12:00:00Z',
    });
  });

  it('executes every state transition with its exact body', () => {
    client
      .decidePartner(partnerId, {
        decision: 'APPROVE',
        reason: null,
      })
      .subscribe();

    const decide = httpTesting.expectOne(
      `/api/v1/partners/${partnerId}/validation`,
    );
    expect(decide.request.method).toBe('POST');
    expect(decide.request.body).toEqual({
      decision: 'APPROVE',
      reason: null,
    });
    decide.flush(partnerResponse());

    client
      .suspendPartner(partnerId, {
        reason: 'Compliance review',
      })
      .subscribe();

    const suspend = httpTesting.expectOne(
      `/api/v1/partners/${partnerId}/suspension`,
    );
    expect(suspend.request.method).toBe('POST');
    expect(suspend.request.body).toEqual({
      reason: 'Compliance review',
    });
    suspend.flush(partnerResponse());

    client.reactivatePartner(partnerId).subscribe();

    const reactivate = httpTesting.expectOne(
      `/api/v1/partners/${partnerId}/reactivation`,
    );
    expect(reactivate.request.method).toBe('POST');
    expect(reactivate.request.body).toBeNull();
    reactivate.flush(partnerResponse());
  });

  it('configures a threshold and encodes the transaction type', () => {
    const request = {
      currency: 'CAD',
      amount: 1000.5,
      validationLevels: 2,
    };

    client
      .configureValidationThreshold(
        partnerId,
        'BULK PAYMENT',
        request,
      )
      .subscribe();

    const threshold = httpTesting.expectOne(
      `/api/v1/partners/${partnerId}/validation-thresholds/BULK%20PAYMENT`,
    );
    expect(threshold.request.method).toBe('PUT');
    expect(threshold.request.body).toEqual(request);
    threshold.flush(partnerResponse());
  });

  it('queries the audit trail with the contract parameters', () => {
    client
      .getPartnerAuditTrail(partnerId, {
        from: '2026-07-01T00:00:00Z',
        to: '2026-07-31T23:59:59Z',
        page: 1,
        size: 25,
      })
      .subscribe();

    const audit = httpTesting.expectOne(
      (request) =>
        request.url === `/api/v1/partners/${partnerId}/audit`,
    );
    expect(audit.request.method).toBe('GET');
    expect(audit.request.params.get('from')).toBe(
      '2026-07-01T00:00:00Z',
    );
    expect(audit.request.params.get('to')).toBe(
      '2026-07-31T23:59:59Z',
    );
    expect(audit.request.params.get('page')).toBe('1');
    expect(audit.request.params.get('size')).toBe('25');

    audit.flush({
      items: [],
      page: 1,
      size: 25,
      totalElements: 0,
      totalPages: 0,
    });
  });

  function partnerResponse(): PartnerResponse {
    return {
      id: partnerId,
      legalName: 'Acme Payments',
      technicalContactName: 'Alice Ops',
      technicalContactEmail: 'alice.ops@example.com',
      authorizedTransactionTypes: ['PAYMENT'],
      status: 'PENDING_VALIDATION',
      validationThresholds: [],
      createdAt: '2026-07-28T12:00:00Z',
      updatedAt: '2026-07-28T12:00:00Z',
    };
  }
});
