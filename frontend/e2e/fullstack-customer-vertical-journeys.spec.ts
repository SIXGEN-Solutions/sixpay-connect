import { expect, test } from '@playwright/test';

import {
  authenticateFullstackAdmin,
  authenticateFullstackManager,
} from './support/fullstack-local-admin-auth';

async function enrollCustomer(page, suffix: string) {
  const niu = `L593-NIU-${suffix}`;
  const customerNumber = `L593-${suffix.slice(-8)}`;
  const accountReference = `ACC-L593-${suffix}`;

  await page.goto('/customers/enroll');
  await expect(page.getByLabel('Institution financière')).toBeVisible({ timeout: 15_000 });

  await page.getByLabel('Institution financière').fill('SIXPAY_BANK');
  await page.getByLabel('NIU').fill(niu);
  await page.getByLabel('Numéro client').fill(customerNumber);
  await page.getByLabel('Référence compte').fill(accountReference);

  await page.getByRole('button', { name: 'Rechercher dans Amplitude' }).click();
  await expect(page.getByText('CM9 Full-stack Customer', { exact: true })).toBeVisible({
    timeout: 15_000,
  });

  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      new URL(response.url()).pathname === '/internal/api/v1/customers',
  );

  await page.getByRole('button', { name: 'Confirmer l’enrôlement' }).click();

  const response = await responsePromise;
  expect(response.status()).toBe(201);

  const body = (await response.json()) as {
    id: string;
    bankAccounts: Array<{ id: string; bankingAccountReference: string; defaultAccount: boolean }>;
  };

  expect(body.id).toMatch(/^[0-9a-f-]{36}$/);
  expect(body.bankAccounts.length).toBeGreaterThan(0);

  return {
    customerId: body.id,
    niu,
    accountReference,
    bankAccountId: body.bankAccounts[0].id,
  };
}

test.describe('LOT 5.9.3 Customer vertical journeys', () => {
  test('search/detail and enrollment are persisted through the real stack', async ({ page }) => {
    await authenticateFullstackAdmin(page);

    const suffix = Date.now().toString();
    const customer = await enrollCustomer(page, suffix);

    await expect(page).toHaveURL(new RegExp(`/customers/${customer.customerId}\\?enrolled=true$`));
    await expect(page.getByText(customer.niu, { exact: true })).toBeVisible();

    await page.goto('/customers');
    await page.getByLabel('NIU').fill(customer.niu);

    const searchResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname === '/internal/api/v1/customers' &&
        new URL(response.url()).searchParams.get('niu') === customer.niu,
    );

    await page.getByRole('button', { name: 'Rechercher' }).click();

    expect((await searchResponsePromise).status()).toBe(200);

    const row = page.locator(`a.customer-row[href="/customers/${customer.customerId}"]`);
    await expect(row).toContainText(customer.niu);

    await row.click();
    await expect(page).toHaveURL(new RegExp(`/customers/${customer.customerId}$`));
    await expect(page.getByText(customer.niu, { exact: true })).toBeVisible();
  });

  test('adds and persists a verified customer bank account', async ({ page }) => {
    await authenticateFullstackAdmin(page);

    const suffix = Date.now().toString();
    const customer = await enrollCustomer(page, suffix);
    const secondAccountReference = `ACC-L593-SECOND-${suffix}`;

    await page.goto(`/customers/${customer.customerId}`);

    await expect(page.getByLabel('Référence du nouveau compte')).toBeVisible({ timeout: 15_000 });
    await page.getByLabel('Référence du nouveau compte').fill(secondAccountReference);

    const addResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname ===
          `/internal/api/v1/customers/${customer.customerId}/accounts`,
    );

    await page.getByRole('button', { name: 'Vérifier et ajouter' }).click();

    const addResponse = await addResponsePromise;
    expect(addResponse.status()).toBe(200);

    const customerAfterAdd = (await addResponse.json()) as {
      bankAccounts: Array<{
        id: string;
        bankingAccountReference: string;
        maskedAccountIdentifier: string;
      }>;
    };

    expect(
      customerAfterAdd.bankAccounts.some(
        (account) => account.bankingAccountReference === secondAccountReference,
      ),
    ).toBe(true);

    await expect(page.getByText('Compte ajouté', { exact: true })).toBeVisible();

    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname === `/internal/api/v1/customers/${customer.customerId}`,
    );

    await page.reload();

    const detailResponse = await detailResponsePromise;

    const persisted = (await detailResponse.json()) as {
      bankAccounts: Array<{ bankingAccountReference: string }>;
    };

    expect(
      persisted.bankAccounts.some(
        (account) => account.bankingAccountReference === secondAccountReference,
      ),
    ).toBe(true);
  });

  test('creates and activates a CustomerSubscription through Angular and backend persistence', async ({
    page,
  }) => {
    await authenticateFullstackAdmin(page);

    const suffix = Date.now().toString();
    const customer = await enrollCustomer(page, suffix);

    const partnerLegalName = `LOT 5.9.3 Partner ${suffix}`;

    await page.goto('/partners/create');
    await expect(page.getByText('Créer un partenaire', { exact: true })).toBeVisible({
      timeout: 15_000,
    });

    await page.getByLabel('Raison sociale').fill(partnerLegalName);
    await page.getByLabel('Nom du contact technique').fill('LOT 5.9.3 Operations');
    await page.getByLabel('Courriel du contact').fill(`lot-5-9-3-${suffix}@sixpay.test`);
    await page.getByLabel('Types de transactions autorisés').fill('PAYMENT');

    const partnerCreateResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/api/v1/partners',
    );

    await page.getByRole('button', { name: 'Créer le partenaire' }).click();

    const partnerCreateResponse = await partnerCreateResponsePromise;
    expect(partnerCreateResponse.status()).toBe(201);

    await expect(page).toHaveURL(/\/partners\/[0-9a-f-]{36}\?created=true$/);

    const partnerIdMatch = new URL(page.url()).pathname.match(/^\/partners\/([0-9a-f-]{36})$/);
    expect(partnerIdMatch?.[1]).toBeTruthy();
    const partner = { id: partnerIdMatch![1] };

    await page.context().clearCookies();
    await authenticateFullstackManager(page);

    await page.goto(`/partners/${partner.id}`);
    await expect(page.getByText(partnerLegalName, { exact: true }).first()).toBeVisible({
      timeout: 15_000,
    });

    const approvalResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === `/api/v1/partners/${partner.id}/validation`,
    );

    await page.getByRole('button', { name: 'Approuver' }).click();
    await page.getByRole('button', { name: 'Confirmer' }).click();

    const approvalResponse = await approvalResponsePromise;
    expect(approvalResponse.status()).toBe(200);
    await expect(page.getByText('ACTIVE', { exact: true }).first()).toBeVisible();

    await page.context().clearCookies();
    await authenticateFullstackAdmin(page);

    await page.goto(`/customers/${customer.customerId}`);

    await expect(page.getByLabel('Partner ID')).toBeVisible({ timeout: 15_000 });
    await page.getByLabel('Partner ID').fill(partner.id);
    await page.getByLabel('Bank account ID').fill(customer.bankAccountId);

    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/internal/api/v1/subscriptions',
    );

    await page.getByRole('button', { name: 'Créer' }).click();

    const createResponse = await createResponsePromise;
    expect(createResponse.status()).toBe(201);

    const created = (await createResponse.json()) as {
      id: string;
      customerId: string;
      partnerId: string;
      bankAccountId: string;
      status: string;
    };

    expect(created.customerId).toBe(customer.customerId);
    expect(created.partnerId).toBe(partner.id);
    expect(created.bankAccountId).toBe(customer.bankAccountId);
    expect(created.status).toBe('PENDING_ACTIVATION');

    await expect(page.getByText('Subscription créée', { exact: true })).toBeVisible();

    const activateResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname ===
          `/internal/api/v1/subscriptions/${created.id}/activation`,
    );

    await page.getByRole('button', { name: 'Activer' }).click();

    const activateResponse = await activateResponsePromise;
    expect(activateResponse.status()).toBe(200);

    const activated = (await activateResponse.json()) as { status: string };
    expect(activated.status).toBe('ACTIVE');

    await expect(page.getByText('Subscription activée', { exact: true })).toBeVisible();

    await page.reload();

    const subscriptionsResponse = await page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        response.request().method() === 'GET' &&
        url.pathname === '/internal/api/v1/subscriptions' &&
        url.searchParams.get('customerId') === customer.customerId
      );
    });

    const persistedSubscriptions = (await subscriptionsResponse.json()) as Array<{
      id: string;
      status: string;
    }>;

    const persisted = persistedSubscriptions.find((item) => item.id === created.id);
    expect(persisted?.status).toBe('ACTIVE');
  });
});
