import { expect, test } from '@playwright/test';

const CURRENT_USER = {
  subject: 'local-admin',
  username: 'admin',
  roles: ['ADMIN'],
};

const PARTNER_PAGE = {
  items: [
    {
      id: 'f88166d1-39df-4900-bb31-1700d25c3bfa',
      legalName: 'TresorPay',
      technicalContactName: 'TresorPay Operations',
      technicalContactEmail: 'operations@tresorpay.cm',
      authorizedTransactionTypes: ['PAYMENT'],
      status: 'ACTIVE',
      createdAt: '2026-08-08T12:00:00Z',
      updatedAt: '2026-08-08T13:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

test.beforeEach(async ({ page }) => {
  await page.route(/\/api\/v1\/auth\/me(?:\?.*)?$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(CURRENT_USER),
    });
  });
});

test.describe('Phase 7.8 integration profile hardening', () => {
  test('uses the API datasource and exposes loading before data', async ({ page }) => {
    await page.route(/\/api\/v1\/partners(?:\?.*)?$/, async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 500));

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(PARTNER_PAGE),
      });
    });

    await page.goto('/partners');

    await expect(page.getByText('Chargement des partenaires')).toBeVisible();

    await expect(
      page.getByRole('link', {
        name: 'Ouvrir TresorPay',
      }),
    ).toBeVisible();

    await expect(page.locator('sp-mock-state-panel')).toHaveCount(0);
  });

  test('surfaces 429 Retry-After and the support correlation id', async ({ page }) => {
    await page.route(/\/api\/v1\/partners(?:\?.*)?$/, async (route) => {
      await route.fulfill({
        status: 429,
        contentType: 'application/problem+json',
        headers: {
          'Retry-After': '30',
          'X-Correlation-ID': '11111111-1111-4111-8111-111111111111',
        },
        body: JSON.stringify({
          type: 'urn:sixpay:problem:rate-limit',
          title: 'Trop de requêtes',
          status: 429,
          detail: 'Le quota temporaire de consultation est atteint.',
          instance: '/api/v1/partners',
        }),
      });
    });

    await page.goto('/partners');

    const alert = page.getByRole('alert');

    await expect(alert).toContainText('Trop de requêtes');

    await expect(alert).toContainText('Le quota temporaire de consultation est atteint.');

    await expect(alert).toContainText('Réessayez dans');

    await expect(alert).toContainText('30 s');

    await expect(alert).toContainText('11111111-1111-4111-8111-111111111111');

    await expect(page.getByText('Catalogue indisponible')).toBeVisible();
  });

  test('does not expose the mock state panel in integration mode', async ({ page }) => {
    await page.route(/\/api\/v1\/partners(?:\?.*)?$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...PARTNER_PAGE,
          items: [],
          totalElements: 0,
          totalPages: 0,
        }),
      });
    });

    await page.goto('/partners');

    await expect(page.locator('sp-mock-state-panel')).toHaveCount(0);

    await expect(
      page.getByText('Aucun partenaire', {
        exact: true,
      }),
    ).toBeVisible();
  });
});
