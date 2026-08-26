import {
  mapPartnerPageResponse,
  mapPartnerResponse,
  mapPartnerStatusResponse,
} from './partners-api.mapper';

describe('Partner API mappers', () => {
  it('maps a complete Partner response', () => {
    const partner = mapPartnerResponse({
      id: 'partner-id',
      legalName: 'Golden Partner',
      technicalContactName: 'Alice',
      technicalContactEmail: 'alice@example.test',
      authorizedTransactionTypes: ['PAYMENT'],
      status: 'ACTIVE',
      validationThresholds: [],
      createdAt: '2026-08-08T10:00:00Z',
      updatedAt: '2026-08-08T11:00:00Z',
    });

    expect(partner.createdAt).toBeInstanceOf(Date);
    expect(partner.updatedAt).toBeInstanceOf(Date);
    expect(partner.statusReason).toBeNull();
  });

  it('maps a paginated Partner catalog without inventing detail fields', () => {
    const page = mapPartnerPageResponse({
      items: [
        {
          id: 'partner-id',
          legalName: 'Golden Partner',
          technicalContactName: 'Alice',
          technicalContactEmail: 'alice@example.test',
          authorizedTransactionTypes: ['PAYMENT'],
          status: 'ACTIVE',
          createdAt: '2026-08-08T10:00:00Z',
          updatedAt: '2026-08-08T11:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });

    expect(page.items).toHaveLength(1);
    expect(page.items[0]!.createdAt).toBeInstanceOf(Date);
    expect(page.items[0]!.updatedAt).toBeInstanceOf(Date);
  });

  it('maps the Partner status response', () => {
    const status = mapPartnerStatusResponse({
      partnerId: 'partner-id',
      status: 'ACTIVE',
      connection: {
        apiBasePath: '/api/v1/partners/partner-id',
        supportedAuthenticationMethods: ['MTLS'],
        newTransactionsAllowed: true,
      },
      updatedAt: '2026-08-08T11:00:00Z',
    });

    expect(status.updatedAt).toBeInstanceOf(Date);
    expect(status.statusReason).toBeNull();
  });
});
