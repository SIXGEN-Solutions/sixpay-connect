import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PartnerApiClient } from '../api/partners-api.client';
import { PartnerResponse } from '../models/partners.response';
import { PartnersMockService } from './partners-mock.service';
import { PartnersService } from './partners.service';

const response: PartnerResponse = {
  id: 'partner-id',
  legalName: 'Golden Partner',
  technicalContactName: 'Alice',
  technicalContactEmail: 'alice@example.test',
  authorizedTransactionTypes: ['PAYMENT'],
  status: 'ACTIVE',
  validationThresholds: [],
  createdAt: '2026-07-29T10:00:00Z',
  updatedAt: '2026-07-29T11:00:00Z',
};

describe('PartnersService hardening', () => {
  function configure(usesApi: boolean) {
    const api = {
      listPartners: vi.fn((page = 0, size = 20) =>
        of({
          items: [],
          page,
          size,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
      createPartner: vi.fn(() => of(response)),
      getPartner: vi.fn(() => of(response)),
      getPartnerStatus: vi.fn(),
      decidePartner: vi.fn(() => of(response)),
      suspendPartner: vi.fn(() => of(response)),
      reactivatePartner: vi.fn(() => of(response)),
      configureValidationThreshold: vi.fn(() => of(response)),
      getPartnerAuditTrail: vi.fn(),
    };

    const mock = {
      search: vi.fn((query) =>
        of({
          items: [],
          page: query.page ?? 0,
          size: query.size ?? 20,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
      create: vi.fn(() => of(response)),
      get: vi.fn(() => of(response)),
      getStatus: vi.fn(),
      decide: vi.fn(() => of(response)),
      suspend: vi.fn(() => of(response)),
      reactivate: vi.fn(() => of(response)),
      configureValidationThreshold: vi.fn(() => of(response)),
      getAuditTrail: vi.fn(),
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        PartnersService,
        {
          provide: BackendModeService,
          useValue: {
            usesApi,
            usesMock: !usesApi,
          },
        },
        { provide: PartnerApiClient, useValue: api },
        { provide: PartnersMockService, useValue: mock },
      ],
    });

    return {
      service: TestBed.inject(PartnersService),
      api,
      mock,
    };
  }

  it('uses API pagination parameters unchanged in API mode', async () => {
    const { service, api, mock } = configure(true);

    const page = await firstValueFrom(
      service.search({ page: 1, size: 50 }),
    );

    expect(api.listPartners).toHaveBeenCalledWith(1, 50);
    expect(mock.search).not.toHaveBeenCalled();
    expect(page.page).toBe(1);
    expect(page.size).toBe(50);
  });

  it('uses contract defaults when API query omits pagination', async () => {
    const { service, api } = configure(true);

    await firstValueFrom(service.search({}));

    expect(api.listPartners).toHaveBeenCalledWith(0, 20);
  });

  it('never calls the API datasource in mock mode', async () => {
    const { service, api, mock } = configure(false);

    await firstValueFrom(
      service.search({ page: 2, size: 10 }),
    );

    expect(mock.search).toHaveBeenCalledWith({
      page: 2,
      size: 10,
    });
    expect(api.listPartners).not.toHaveBeenCalled();
  });

  it('keeps write operations on the selected datasource', async () => {
    const { service, api, mock } = configure(false);

    await firstValueFrom(
      service.decide('partner-id', {
        decision: 'APPROVE',
        reason: null,
      }),
    );
    await firstValueFrom(
      service.suspend('partner-id', {
        reason: 'Compliance',
      }),
    );
    await firstValueFrom(
      service.reactivate('partner-id'),
    );

    expect(mock.decide).toHaveBeenCalledOnce();
    expect(mock.suspend).toHaveBeenCalledOnce();
    expect(mock.reactivate).toHaveBeenCalledOnce();

    expect(api.decidePartner).not.toHaveBeenCalled();
    expect(api.suspendPartner).not.toHaveBeenCalled();
    expect(api.reactivatePartner).not.toHaveBeenCalled();
  });
});
