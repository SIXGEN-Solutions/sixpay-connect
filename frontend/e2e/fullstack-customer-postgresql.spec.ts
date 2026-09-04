import { expect, test } from '@playwright/test';

import { authenticateFullstackAdmin } from './support/fullstack-local-admin-auth';

test.describe('CM-9 Angular -> Spring -> Customer -> Verification -> Amplitude Stub -> PostgreSQL', () => {
  test('enrolls and reloads a Customer through the real stack', async ({ page }) => {
    const suffix = Date.now().toString();
    const niu = `CM9-NIU-${suffix}`;
    const customerNumber = `CM9-${suffix.slice(-8)}`;
    const accountReference = `ACC-CM9-${suffix}`;

    await authenticateFullstackAdmin(page);

    await page.goto('/customers/enroll');

    await expect(page).toHaveURL(/\/customers\/enroll$/);

    await expect(page.getByLabel('Institution financière')).toBeVisible({
      timeout: 15_000,
    });

    await page.getByLabel('Institution financière').fill('SIXPAY_BANK');
    await page.getByLabel('NIU').fill(niu);
    await page.getByLabel('Numéro client').fill(customerNumber);
    await page.getByLabel('Référence compte').fill(accountReference);

    await page.getByRole('button', { name: 'Rechercher dans Amplitude' }).click();

    await expect(page.getByText('CM9 Full-stack Customer', { exact: true })).toBeVisible({
      timeout: 15_000,
    });

    const enrollmentResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/internal/api/v1/customers',
    );

    await page.getByRole('button', { name: 'Confirmer l’enrôlement' }).click();

    const enrollmentResponse = await enrollmentResponsePromise;

    const enrollmentResponseBody = await enrollmentResponse.text();

    expect(
      enrollmentResponse.status(),
      `Customer enrollment failed with HTTP ${enrollmentResponse.status()}: ${enrollmentResponseBody}`,
    ).toBe(201);

    const enrolledCustomer = JSON.parse(enrollmentResponseBody) as {
      id: string;
    };

    expect(enrolledCustomer.id).toMatch(/^[0-9a-f-]{36}$/);

    await expect(page).toHaveURL(new RegExp(`/customers/${enrolledCustomer.id}\\?enrolled=true$`), {
      timeout: 15_000,
    });

    await expect(page.getByText('CM9 Full-stack Customer', { exact: true })).toBeVisible();

    await expect(page.getByText('NIU', { exact: true })).toBeVisible();

    await expect(page.getByText(niu, { exact: true })).toBeVisible();

    const persistedDetailUrl = page.url();

    await page.reload();

    await expect(page).toHaveURL(persistedDetailUrl);

    await expect(page.getByText('CM9 Full-stack Customer', { exact: true })).toBeVisible();

    await page.goto('/customers');

    await expect(page.getByText('CM9 Full-stack Customer', { exact: true }).first()).toBeVisible();
  });
});
