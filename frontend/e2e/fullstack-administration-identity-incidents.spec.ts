import { expect, test } from '@playwright/test';

import { authenticateFullstackAdmin } from './support/fullstack-local-admin-auth';

const INCIDENT_ID = 'INC-L596-E2E-001';

test.describe('LOT 5.9.6 Administration / Identity / Incidents', () => {
  test.beforeEach(async ({ page }) => {
    await authenticateFullstackAdmin(page);
  });

  test('renders the real read-only Administration operational projection', async ({ page }) => {
    const overviewPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes('/internal/api/v1/administration/overview'),
    );

    await page.goto('/administration');

    const overview = await overviewPromise;
    expect(overview.status()).toBe(200);

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible();
    await expect(page.getByText('Zone cutoff comptable', { exact: true })).toBeVisible();
    await expect(page.getByText('Heure cutoff comptable', { exact: true })).toBeVisible();

    const settingsPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes('/internal/api/v1/administration/settings'),
    );

    await page.getByRole('link', { name: 'Ouvrir' }).nth(1).click();

    const settings = await settingsPromise;
    expect(settings.status()).toBe(200);

    await expect(page.getByRole('heading', { name: 'Paramètres généraux' })).toBeVisible();
    await expect(
      page.getByText(
        /Cette surface reste volontairement read-only au titre du contrat Administration opérationnel/,
      ),
    ).toBeVisible();
  });

  test('lists and opens a real Security user administration projection', async ({ page }) => {
    const listPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        /\/internal\/api\/v1\/administration\/users(?:\?|$)/.test(response.url()),
    );

    await page.goto('/administration/users');

    const list = await listPromise;
    expect(list.status()).toBe(200);

    await expect(page.getByRole('heading', { name: 'Utilisateurs & sécurité' })).toBeVisible();
    await expect(page.getByText('manager', { exact: true })).toBeVisible();
    await expect(page.getByText('auditor', { exact: true })).toBeVisible();

    const managerCard = page.locator('sp-card').filter({ hasText: 'manager@sixpay.local' });

    const detailPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        /\/internal\/api\/v1\/administration\/users\/[0-9a-f-]+$/i.test(response.url()),
    );

    await managerCard.getByRole('link', { name: 'Administrer' }).click();

    const detail = await detailPromise;
    expect(detail.status()).toBe(200);

    await expect(page.getByRole('heading', { name: 'manager' })).toBeVisible();
    await expect(
      page.getByText('Administration du compte, des identités et des autorisations SIXPAY.', {
        exact: true,
      }),
    ).toBeVisible();
    await expect(page.getByText('Rôles: MANAGER', { exact: true })).toBeVisible();
  });

  test('searches a persisted Incident and opens its real read-only detail and timeline', async ({
    page,
  }) => {
    const listPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        /\/internal\/api\/v1\/incidents\?/.test(response.url()),
    );

    await page.goto('/incidents');

    const list = await listPromise;
    expect(list.status()).toBe(200);

    await expect(page.getByRole('heading', { name: 'Incidents' })).toBeVisible();
    await expect(page.getByRole('link', { name: INCIDENT_ID })).toBeVisible();
    await expect(page.getByText('Dégradation contrôlée LOT 5.9.6', { exact: true })).toBeVisible();

    const detailPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().endsWith(`/internal/api/v1/incidents/${INCIDENT_ID}`),
    );

    await page.getByRole('link', { name: INCIDENT_ID }).click();

    const detail = await detailPromise;
    expect(detail.status()).toBe(200);

    await expect(page.getByRole('heading', { name: INCIDENT_ID })).toBeVisible();
    await expect(page.getByText('Dégradation contrôlée LOT 5.9.6', { exact: true })).toBeVisible();
    await expect(
      page.getByText('Incident détecté par la supervision SIXPAY.', { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText('Investigation opérationnelle démarrée.', { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText('59060000-0000-0000-0000-000000000099', { exact: true }),
    ).toBeVisible();
  });
});
