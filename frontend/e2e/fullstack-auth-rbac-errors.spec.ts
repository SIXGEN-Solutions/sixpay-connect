import { expect, test } from '@playwright/test';

import { authenticateFullstackAdmin } from './support/fullstack-local-admin-auth';

test.describe('LOT 5.9.2 Authentication + RBAC + navigation/error E2E', () => {
  test('LOCAL login establishes a backend-authoritative session', async ({ page }) => {
    await authenticateFullstackAdmin(page);

    const response = await page.request.get('/api/v1/auth/me');

    expect(response.status()).toBe(200);

    const session = (await response.json()) as {
      authenticated: boolean;
      username: string;
      roles: string[];
      authenticationMethod: string;
    };

    expect(session.authenticated).toBe(true);
    expect(session.username).toBe('admin');
    expect(session.roles).toContain('ADMIN');
    expect(session.authenticationMethod).toBe('LOCAL');
  });

  test('preserves a protected deep link through LOCAL authentication and reload', async ({
    page,
  }) => {
    await page.goto('/administration/settings');

    await expect(page).toHaveURL(/\/login\?returnUrl=%2Fadministration%2Fsettings/);

    await authenticateFullstackAdmin(page);

    await page.goto('/administration/settings');
    await expect(page).toHaveURL(/\/administration\/settings$/);
    await expect(page.getByRole('heading', { name: 'Paramètres généraux' })).toBeVisible({
      timeout: 15_000,
    });

    await page.reload();

    await expect(page).toHaveURL(/\/administration\/settings$/);
    await expect(page.getByRole('heading', { name: 'Paramètres généraux' })).toBeVisible({
      timeout: 15_000,
    });
  });

  test('redirects an authenticated but unauthorized user to forbidden', async ({ page }) => {
    await page.route(/\/api\/v1\/auth\/me(?:\?.*)?$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          subject: 'rbac-negative-user',
          username: 'rbac-negative',
          roles: ['AUDITOR'],
          permissions: [],
          authenticationMethod: 'LOCAL',
          passwordChangeRequired: false,
        }),
      });
    });

    await page.goto('/administration');

    await expect(page).toHaveURL(/\/forbidden$/);
  });

  test('handles an API 401 as an expired session and preserves the return URL', async ({
    page,
  }) => {
    await authenticateFullstackAdmin(page);

    await page.route(/\/internal\/api\/v1\/customers(?:\?.*)?$/, async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/problem+json',
        body: JSON.stringify({
          type: 'urn:sixpay:problem:unauthorized',
          title: 'Session expirée',
          status: 401,
          detail: 'La session n’est plus valide.',
        }),
      });
    });

    await page.goto('/customers');

    await expect(page).toHaveURL(/\/login/);
    await expect(page).toHaveURL(/sessionExpired=true/);
    await expect(page).toHaveURL(/returnUrl=%2Fcustomers/);
    await expect(page.getByRole('alert')).toContainText('Votre session a expiré');
  });

  test('handles an API 403 as forbidden', async ({ page }) => {
    await authenticateFullstackAdmin(page);

    await page.route(/\/internal\/api\/v1\/customers(?:\?.*)?$/, async (route) => {
      await route.fulfill({
        status: 403,
        contentType: 'application/problem+json',
        body: JSON.stringify({
          type: 'urn:sixpay:problem:forbidden',
          title: 'Accès interdit',
          status: 403,
          detail: 'Permission insuffisante.',
        }),
      });
    });

    await page.goto('/customers');

    await expect(page).toHaveURL(/\/forbidden$/);
  });

  test('renders a real Angular 404 for an unknown deep link', async ({ page }) => {
    await page.goto('/route-sixpay-inconnue-5-9-2');

    await expect(
      page.getByRole('heading', {
        name: 'Page introuvable',
      }),
    ).toBeVisible();

    await expect(page.getByText('404', { exact: true })).toBeVisible();
    await expect(page).toHaveURL(/\/route-sixpay-inconnue-5-9-2$/);
  });

  test('surfaces a 5xx response in the Angular error state without changing route', async ({
    page,
  }) => {
    await authenticateFullstackAdmin(page);

    await page.route(/\/internal\/api\/v1\/customers(?:\?.*)?$/, async (route) => {
      await route.fulfill({
        status: 503,
        contentType: 'application/problem+json',
        headers: {
          'X-Correlation-ID': '59999999-9999-4999-8999-999999999999',
        },
        body: JSON.stringify({
          type: 'urn:sixpay:problem:service-unavailable',
          title: 'Service Customer indisponible',
          status: 503,
          detail: 'Le service Customer est temporairement indisponible.',
        }),
      });
    });

    await page.goto('/customers');

    await expect(page).toHaveURL(/\/customers$/);

    const alert = page.getByRole('alert');
    await expect(alert).toContainText('Erreur serveur');
    await expect(alert).toContainText(
      'Le service SIXPAY rencontre une erreur temporaire. Réessayez ultérieurement.',
    );
    await expect(alert).toContainText('59999999-9999-4999-8999-999999999999');
  });
});
