import { Page } from '@playwright/test';

export const PARTNER_ID = '11111111-1111-4111-8111-111111111111';

interface PartnerState {
  status: 'PENDING_VALIDATION' | 'ACTIVE' | 'SUSPENDED';
  statusReason: string | null;
  validationThresholds: Array<{
    transactionType: string;
    currency: string;
    amount: number;
    validationLevels: number;
  }>;
}

export async function mockPartnerBackend(page: Page): Promise<PartnerState> {
  const state: PartnerState = {
    status: 'PENDING_VALIDATION',
    statusReason: null,
    validationThresholds: [],
  };

  await page.route('**/api/v1/partners**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const headers = request.headers();

    if (!headers['x-correlation-id']) {
      await route.fulfill({ status: 500, json: { title: 'Missing correlation header' } });
      return;
    }
    if (request.method() !== 'GET' && !headers['idempotency-key']) {
      await route.fulfill({ status: 500, json: { title: 'Missing idempotency header' } });
      return;
    }

    if (request.method() === 'POST' && path === '/api/v1/partners') {
      const body = request.postDataJSON();
      state.status = 'PENDING_VALIDATION';
      await route.fulfill({ status: 201, json: partnerResponse(state, body) });
      return;
    }
    if (path.endsWith('/status')) {
      await route.fulfill({
        json: {
          partnerId: PARTNER_ID,
          status: state.status,
          statusReason: state.statusReason,
          connection: {
            apiBasePath: '/api/v1',
            supportedAuthenticationMethods: ['OAUTH2'],
            newTransactionsAllowed: state.status === 'ACTIVE',
          },
          updatedAt: '2026-07-29T12:00:00Z',
        },
      });
      return;
    }
    if (path.endsWith('/validation')) {
      const body = request.postDataJSON();
      state.status = body.decision === 'APPROVE' ? 'ACTIVE' : 'PENDING_VALIDATION';
      state.statusReason = body.reason;
      await route.fulfill({ json: partnerResponse(state) });
      return;
    }
    if (path.endsWith('/suspension')) {
      state.status = 'SUSPENDED';
      state.statusReason = request.postDataJSON().reason;
      await route.fulfill({ json: partnerResponse(state) });
      return;
    }
    if (path.endsWith('/reactivation')) {
      state.status = 'ACTIVE';
      state.statusReason = null;
      await route.fulfill({ json: partnerResponse(state) });
      return;
    }
    if (path.includes('/validation-thresholds/')) {
      const transactionType = decodeURIComponent(path.split('/').at(-1) ?? '');
      state.validationThresholds = [{ transactionType, ...request.postDataJSON() }];
      await route.fulfill({ json: partnerResponse(state) });
      return;
    }
    if (path.endsWith('/audit')) {
      await route.fulfill({
        json: {
          items: [
            {
              action: 'PARTNER_CREATED',
              partnerId: PARTNER_ID,
              result: 'SUCCESS',
              actorId: 'local-security-user',
              correlationId: 'corr-e2e',
              details: 'Partner created',
              occurredAt: '2026-07-29T12:00:00Z',
            },
          ],
          page: 0,
          size: 25,
          totalElements: 1,
          totalPages: 1,
        },
      });
      return;
    }
    if (request.method() === 'GET' && path.endsWith(`/${PARTNER_ID}`)) {
      await route.fulfill({ json: partnerResponse(state) });
      return;
    }
    await route.fulfill({ status: 404, json: {} });
  });

  return state;
}

function partnerResponse(
  state: PartnerState,
  overrides: Record<string, unknown> = {},
): Record<string, unknown> {
  return {
    id: PARTNER_ID,
    legalName: 'Golden Partner',
    technicalContactName: 'Alice Admin',
    technicalContactEmail: 'alice@example.test',
    authorizedTransactionTypes: ['PAYMENT', 'REFUND'],
    status: state.status,
    statusReason: state.statusReason,
    validationThresholds: state.validationThresholds,
    createdAt: '2026-07-29T10:00:00Z',
    updatedAt: '2026-07-29T12:00:00Z',
    ...overrides,
  };
}
