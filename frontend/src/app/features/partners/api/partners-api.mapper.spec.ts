import {
  mapPartnerAuditPageResponse,
  mapPartnerResponse,
  mapPartnerStatusResponse,
} from './partners-api.mapper';

describe('Partner API mappers', () => {
  it('maps HTTP date-time strings and nullable fields to application models', () => {
    const partner = mapPartnerResponse({
      id: 'partner-id',
      legalName: 'Acme Payments',
      technicalContactName: 'Alice Ops',
      technicalContactEmail: 'alice.ops@example.com',
      authorizedTransactionTypes: ['PAYMENT'],
      status: 'ACTIVE',
      validationThresholds: [
        { transactionType: 'PAYMENT', currency: 'CAD', amount: 1000, validationLevels: 2 },
      ],
      createdAt: '2026-07-28T12:00:00Z',
      updatedAt: '2026-07-28T13:00:00Z',
    });

    expect(partner.createdAt).toEqual(new Date('2026-07-28T12:00:00Z'));
    expect(partner.updatedAt).toEqual(new Date('2026-07-28T13:00:00Z'));
    expect(partner.statusReason).toBeNull();
  });

  it('maps status and audit responses without leaking HTTP date strings', () => {
    const status = mapPartnerStatusResponse({
      partnerId: 'partner-id',
      status: 'ACTIVE',
      connection: {
        apiBasePath: '/api/v1/partners/partner-id',
        supportedAuthenticationMethods: ['MTLS', 'API_KEY'],
        newTransactionsAllowed: true,
      },
      updatedAt: '2026-07-28T13:00:00Z',
    });
    const audit = mapPartnerAuditPageResponse({
      items: [
        {
          partnerId: 'partner-id',
          action: 'PARTNER_VALIDATED',
          result: 'SUCCESS',
          actorId: 'admin@sixpay',
          correlationId: 'correlation-id',
          details: 'Approved',
          occurredAt: '2026-07-28T13:00:00Z',
        },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });

    expect(status.updatedAt).toBeInstanceOf(Date);
    expect(audit.items[0]?.occurredAt).toBeInstanceOf(Date);
  });
});
