import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';

import { PartnerApiClient } from '../api/partners-api.client';
import { PartnerResponse } from '../models/partners.response';
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
  const api = {
    createPartner: vi.fn(),
    getPartner: vi.fn(),
    getPartnerStatus: vi.fn(),
    decidePartner: vi.fn(),
    suspendPartner: vi.fn(),
    reactivatePartner: vi.fn(),
    configureValidationThreshold: vi.fn(),
    getPartnerAuditTrail: vi.fn(),
  };
  let service: PartnersService;

  beforeEach(() => {
    Object.values(api).forEach((mock) => mock.mockReset());
    TestBed.configureTestingModule({
      providers: [PartnersService, { provide: PartnerApiClient, useValue: api }],
    });
    service = TestBed.inject(PartnersService);
  });

  it('sérialise la création et désérialise les dates de la réponse', async () => {
    api.createPartner.mockReturnValue(of(response));
    const request = {
      legalName: 'Golden Partner',
      technicalContactName: 'Alice',
      technicalContactEmail: 'alice@example.test',
      authorizedTransactionTypes: ['PAYMENT'],
    };

    const partner = await firstValueFrom(service.create(request));

    expect(api.createPartner).toHaveBeenCalledWith(request);
    expect(partner.createdAt).toBeInstanceOf(Date);
    expect(partner.statusReason).toBeNull();
  });

  it('délègue les actions du cycle de vie au client API', async () => {
    api.decidePartner.mockReturnValue(of(response));
    api.suspendPartner.mockReturnValue(of(response));
    api.reactivatePartner.mockReturnValue(of(response));

    await firstValueFrom(service.decide('partner-id', { decision: 'APPROVE', reason: null }));
    await firstValueFrom(service.suspend('partner-id', { reason: 'Compliance' }));
    await firstValueFrom(service.reactivate('partner-id'));

    expect(api.decidePartner).toHaveBeenCalledWith('partner-id', {
      decision: 'APPROVE',
      reason: null,
    });
    expect(api.suspendPartner).toHaveBeenCalledWith('partner-id', {
      reason: 'Compliance',
    });
    expect(api.reactivatePartner).toHaveBeenCalledWith('partner-id');
  });

  it('mappe les seuils et la page d’audit', async () => {
    api.configureValidationThreshold.mockReturnValue(of(response));
    api.getPartnerAuditTrail.mockReturnValue(
      of({
        items: [
          {
            partnerId: 'partner-id',
            action: 'CREATED',
            result: 'SUCCESS',
            actorId: 'admin',
            correlationId: 'corr-1',
            details: 'created',
            occurredAt: '2026-07-29T12:00:00Z',
          },
        ],
        page: 0,
        size: 25,
        totalElements: 1,
        totalPages: 1,
      }),
    );

    await firstValueFrom(
      service.configureValidationThreshold('partner-id', 'PAYMENT', {
        currency: 'CAD',
        amount: 100,
        validationLevels: 2,
      }),
    );
    const audit = await firstValueFrom(
      service.getAuditTrail('partner-id', {
        from: '2026-07-01T00:00:00Z',
        to: '2026-07-31T23:59:59Z',
      }),
    );

    expect(api.configureValidationThreshold).toHaveBeenCalledOnce();
    expect(audit.items).toHaveLength(1);
    expect(audit.items[0]!.occurredAt).toBeInstanceOf(Date);
  });
});
