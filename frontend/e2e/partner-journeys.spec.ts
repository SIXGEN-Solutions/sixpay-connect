import AxeBuilder from '@axe-core/playwright';
//import { expect, test } from '@playwright/test';
import { expect, Page, Response, test } from '@playwright/test';
import { mockPartnerBackend, PARTNER_ID } from './partner.fixture';

test.beforeEach(async ({ page }) => {
  await mockPartnerBackend(page);
});

test('création, consultation et approbation d’un Partner', async ({ page }) => {
  await page.goto('/partners/create');

  await page.getByLabel('Raison sociale').fill('Golden Partner');
  await page.getByLabel('Nom du contact technique').fill('Alice Admin');
  await page.getByLabel('Courriel du contact').fill('alice@example.test');
  await page.getByLabel('Types de transactions autorisés').fill('PAYMENT, REFUND');

  await page.getByRole('button', { name: 'Créer le partenaire' }).click();

  await expect(page).toHaveURL(new RegExp(`/partners/${PARTNER_ID}`));
  await expect(page.getByText('Partenaire créé')).toBeVisible();

  await page.getByRole('button', { name: 'Approuver' }).click();

  const partnerRefresh = waitForPartnerRefresh(page);
  const statusRefresh = waitForPartnerStatusRefresh(page);

  await page.getByRole('button', { name: 'Confirmer' }).click();

  await Promise.all([partnerRefresh, statusRefresh]);

  await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();
});

test('configuration d’un seuil, suspension, réactivation et audit', async ({ page }) => {
  await page.goto(`/partners/${PARTNER_ID}`);

  /*
   * Approbation
   */
  await page.getByRole('button', { name: 'Approuver' }).click();

  const approvalPartnerRefresh = waitForPartnerRefresh(page);
  const approvalStatusRefresh = waitForPartnerStatusRefresh(page);

  await page.getByRole('button', { name: 'Confirmer' }).click();

  await Promise.all([approvalPartnerRefresh, approvalStatusRefresh]);

  await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();

  /*
   * Configuration du seuil
   */
  const transactionType = page.getByRole('combobox', {
    name: 'Type de transaction',
  });

  await expect(transactionType).toBeVisible();
  await transactionType.click();

  const transactionOptions = page.getByRole('listbox');
  await expect(transactionOptions).toBeVisible();

  const paymentOption = transactionOptions.getByRole('option', {
    name: 'PAYMENT',
    exact: true,
  });

  await expect(paymentOption).toBeVisible();
  await paymentOption.click();

  await expect(transactionType).toHaveText(/PAYMENT/);

  await page.getByLabel('Devise').fill('CAD');
  await page.getByLabel('Montant').fill('5000');
  await page.getByLabel('Niveaux de validation').fill('2');

  await page.getByRole('button', { name: 'Configurer le seuil' }).click();

  const thresholdResponse = page.waitForResponse((response) => {
    const url = new URL(response.url());

    return (
      response.request().method() === 'PUT' &&
      url.pathname === `/api/v1/partners/${PARTNER_ID}/validation-thresholds/PAYMENT`
    );
  });

  await page.getByRole('button', { name: 'Mettre à jour' }).click();
  await thresholdResponse;

  await expect(page.getByText('5000 CAD')).toBeVisible();

  /*
   * Suspension
   */
  await page.getByRole('button', { name: 'Suspendre' }).click();

  const reasonField = page.getByRole('textbox', { name: 'Motif' });
  await expect(reasonField).toBeVisible();
  await reasonField.fill('Contrôle de conformité');

  const suspensionPartnerRefresh = waitForPartnerRefresh(page);
  const suspensionStatusRefresh = waitForPartnerStatusRefresh(page);

  await page.getByRole('button', { name: 'Confirmer' }).click();

  await Promise.all([suspensionPartnerRefresh, suspensionStatusRefresh]);

  await expect(page.getByText('SUSPENDED', { exact: true })).toBeVisible();
  await expect(page.getByText('Contrôle de conformité')).toBeVisible();

  /*
   * Réactivation
   */
  await page.getByRole('button', { name: 'Réactiver' }).click();

  const reactivationPartnerRefresh = waitForPartnerRefresh(page);
  const reactivationStatusRefresh = waitForPartnerStatusRefresh(page);

  await page.getByRole('button', { name: 'Confirmer' }).click();

  await Promise.all([reactivationPartnerRefresh, reactivationStatusRefresh]);

  await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();

  /*
   * Audit
   */
  await expect(page.getByText('PARTNER_CREATED')).toBeVisible();
  await expect(page.getByText('corr-e2e')).toBeVisible();
});

test('refuse une route non autorisée', async ({ page }) => {
  await page.route('**/api/v1/partners/forbidden-id', (route) =>
    route.fulfill({
      status: 403,
      contentType: 'application/problem+json',
      json: {
        type: 'urn:sixpay:problem:forbidden',
        title: 'Forbidden',
        status: 403,
        detail: 'Access is forbidden.',
        instance: '/api/v1/partners/forbidden-id',
      },
    }),
  );

  await page.goto('/partners/forbidden-id');

  await expect(page).toHaveURL(/\/forbidden$/);
  await expect(page.getByText('Accès non autorisé')).toBeVisible();
});

test('@a11y navigation clavier, labels, focus, formulaires et contraste', async ({ page }) => {
  await page.goto('/partners/create');

  const legalNameField = page.getByLabel('Raison sociale');
  const technicalContactField = page.getByLabel('Nom du contact technique');

  await expect(legalNameField).toBeVisible();
  await expect(technicalContactField).toBeVisible();

  /*
   * Navigation clavier et ordre du focus
   */
  await legalNameField.focus();
  await expect(legalNameField).toBeFocused();

  await page.keyboard.press('Tab');
  await expect(technicalContactField).toBeFocused();

  await page.keyboard.press('Shift+Tab');
  await expect(legalNameField).toBeFocused();

  /*
   * Erreurs accessibles du formulaire
   */
  await page.getByRole('button', { name: 'Créer le partenaire' }).click();

  await expect(page.getByText('Ce champ est obligatoire.').first()).toBeVisible();

  await expect(legalNameField).toHaveAttribute(
    'aria-describedby',
    'legalName-error',
  );

  /*
   * Analyse WCAG A/AA avec axe-core
   */
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  expect(results.violations).toEqual([]);
});
/*
function waitForPartnerRefresh(
  page: Parameters<typeof test>[0] extends never ? never : import('@playwright/test').Page,
): Promise<import('@playwright/test').Response> {
  return page.waitForResponse((response) => {
    const url = new URL(response.url());

    return (
      response.request().method() === 'GET' &&
      url.pathname === `/api/v1/partners/${PARTNER_ID}`
    );
  });
}

function waitForPartnerStatusRefresh(
  page: import('@playwright/test').Page,
): Promise<import('@playwright/test').Response> {
  return page.waitForResponse((response) => {
    const url = new URL(response.url());

    return (
      response.request().method() === 'GET' &&
      url.pathname === `/api/v1/partners/${PARTNER_ID}/status`
    );
  });
}
  */

function waitForPartnerRefresh(page: Page): Promise<Response> {
  return page.waitForResponse((response) => {
    const url = new URL(response.url());

    return (
      response.request().method() === 'GET' && url.pathname === `/api/v1/partners/${PARTNER_ID}`
    );
  });
}

function waitForPartnerStatusRefresh(page: Page): Promise<Response> {
  return page.waitForResponse((response) => {
    const url = new URL(response.url());

    return (
      response.request().method() === 'GET' &&
      url.pathname === `/api/v1/partners/${PARTNER_ID}/status`
    );
  });
}
