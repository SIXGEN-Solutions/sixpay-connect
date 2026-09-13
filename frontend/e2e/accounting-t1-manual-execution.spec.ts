import { expect, test } from '@playwright/test';

const USER = {
  authenticated: true,
  subject: 'local-admin',
  username: 'admin',
  roles: ['ADMIN'],
  permissions: ['SCOPE_accounting.t1.execute'],
  authenticationMethod: 'LOCAL',
  passwordChangeRequired: false,
};

test.beforeEach(async ({ page }) => {
  await page.route(/\/api\/v1\/auth\/me(?:\?.*)?$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(USER),
    });
  });

  await page.route(/\/internal\/api\/v1\/accounting-batches(?:\?.*)?$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    });
  });
});

test('Accounting T1 manual execution is available to authorized ADMIN', async ({ page }) => {
  let requestBody: unknown;

  await page.route(/\/internal\/api\/v1\/accounting-t1-executions$/, async (route) => {
    requestBody = route.request().postDataJSON();

    expect(route.request().headers()['x-correlation-id']).toBeTruthy();

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        batchId: '11111111-1111-4111-8111-111111111111',
        businessDate: '2026-09-10',
        financialInstitutionCode: 'LAREGIONALE',
        batchStatus: 'NOT_COMPLETED',
        submissionState: 'SUBMITTED',
        providerBatchReference: 'CB-20260910-001',
      }),
    });
  });

  page.once('dialog', async (dialog) => {
    expect(dialog.message()).toContain('2026-09-10');
    await dialog.accept();
  });

  await page.goto('/accounting');
  await page.getByLabel('Date métier T1').fill('2026-09-10');
  await page.getByRole('button', { name: 'Lancer le traitement T1' }).click();

  await expect(page.getByRole('status')).toContainText('SUBMITTED');
  await expect(page.getByRole('link', { name: 'Consulter le lot' })).toBeVisible();

  expect(requestBody).toEqual({
    businessDate: '2026-09-10',
  });
});

test('Accounting T1 manual execution remains hidden for AUDITOR', async ({ page }) => {
  await page.route(/\/api\/v1\/auth\/me(?:\?.*)?$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        ...USER,
        username: 'auditor',
        roles: ['AUDITOR'],
        permissions: [],
      }),
    });
  });

  await page.goto('/accounting');

  await expect(
    page.getByRole('button', {
      name: 'Lancer le traitement T1',
    }),
  ).toHaveCount(0);
});
