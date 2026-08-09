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

describe('PartnersService', () => {
  function configure(usesApi: boolean) {
    const api = {
      listPartners: vi.fn(() =>
        of({
          items: [
            {
              id: response.id,
              legalName: response.legalName,
              technicalContactName: response.technicalContactName,
              technicalContactEmail: response.technicalContactEmail,
              authorizedTransactionTypes: response.authorizedTransactionTypes,
              status: response.status,
              createdAt: response.createdAt,
              updatedAt: response.updatedAt,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
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
      search: vi.fn(() =>
        of({
          items: [],
          page: 0,
          size: 20,
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

  it('uses the API datasource for the Partner catalog in API mode', async () => {
    const { service, api, mock } = configure(true);

    const page = await firstValueFrom(
      service.search({ page: 0, size: 20 }),
    );

    expect(api.listPartners).toHaveBeenCalledWith(0, 20);
    expect(mock.search).not.toHaveBeenCalled();
    expect(page.items).toHaveLength(1);
    expect(page.items[0]!.createdAt).toBeInstanceOf(Date);
  });

  it('uses the mock datasource for the Partner catalog in mock mode', async () => {
    const { service, api, mock } = configure(false);

    await firstValueFrom(service.search({ page: 0, size: 20 }));

    expect(mock.search).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(api.listPartners).not.toHaveBeenCalled();
  });

  it('keeps creation datasource-agnostic', async () => {
    const { service, api, mock } = configure(false);
    const request = {
      legalName: 'Golden Partner',
      technicalContactName: 'Alice',
      technicalContactEmail: 'alice@example.test',
      authorizedTransactionTypes: ['PAYMENT'],
    };

    const partner = await firstValueFrom(service.create(request));

    expect(mock.create).toHaveBeenCalledWith(request);
    expect(api.createPartner).not.toHaveBeenCalled();
    expect(partner.createdAt).toBeInstanceOf(Date);
  });

  it('keeps lifecycle operations datasource-agnostic', async () => {
    const { service, api, mock } = configure(false);

    await firstValueFrom(
      service.decide('partner-id', {
        decision: 'APPROVE',
        reason: null,
      }),
    );
    await firstValueFrom(
      service.suspend('partner-id', { reason: 'Compliance' }),
    );
    await firstValueFrom(service.reactivate('partner-id'));

    expect(mock.decide).toHaveBeenCalledOnce();
    expect(mock.suspend).toHaveBeenCalledOnce();
    expect(mock.reactivate).toHaveBeenCalledOnce();
    expect(api.decidePartner).not.toHaveBeenCalled();
  });
});
