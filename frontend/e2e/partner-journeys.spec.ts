import AxeBuilder from '@axe-core/playwright';
import { expect, Page, test } from '@playwright/test';

const PENDING_PARTNER_ID = '10000000-0000-4000-8000-000000000004';

const ACTIVE_PARTNER_ID = '10000000-0000-4000-8000-000000000001';

const STANDALONE_ROLE_STORAGE_KEY = 'sixpay.authentication.standalone-role';

type StandaloneRole = 'ADMIN' | 'MANAGER' | 'AUDITOR' | 'PARTNER';

test('création et consultation d’un Partner par un administrateur', async ({ page }) => {
  await useStandaloneRole(page, 'ADMIN');

  await page.goto('/partners/create');

  await page.getByLabel('Raison sociale').fill('Golden Partner');
  await page.getByLabel('Nom du contact technique').fill('Alice Admin');
  await page.getByLabel('Courriel du contact').fill('alice@example.test');
  await page.getByLabel('Types de transactions autorisés').fill('PAYMENT, REFUND');

  await page.getByRole('button', { name: 'Créer le partenaire' }).click();

  await expect(page).toHaveURL(/\/partners\/[0-9a-f-]{36}\?created=true$/);

  await expect(page.getByText('Partenaire créé')).toBeVisible();

  await expect(page.getByText('PENDING_VALIDATION', { exact: true })).toBeVisible();
});

test('approbation d’un Partner en attente par un manager', async ({ page }) => {
  await useStandaloneRole(page, 'MANAGER');

  await page.goto(`/partners/${PENDING_PARTNER_ID}`);

  await expect(page.getByText('PENDING_VALIDATION', { exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Approuver' }).click();

  await page.getByRole('button', { name: 'Confirmer' }).click();

  await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();
});

test('configuration d’un seuil, suspension et réactivation par un administrateur', async ({
  page,
}) => {
  await useStandaloneRole(page, 'ADMIN');

  await page.goto(`/partners/${ACTIVE_PARTNER_ID}`);

  await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();

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

  await page.getByRole('button', { name: 'Mettre à jour' }).click();

  await expect(page.getByText('5000 CAD')).toBeVisible();

  await page.getByRole('button', { name: 'Suspendre' }).click();

  const reasonField = page.getByRole('textbox', {
    name: 'Motif',
  });

  await expect(reasonField).toBeVisible();
  await reasonField.fill('Contrôle de conformité');

  await page.getByRole('button', { name: 'Confirmer' }).click();

  await expect(page.getByText('SUSPENDED', { exact: true })).toBeVisible();

  await expect(page.getByText('Contrôle de conformité')).toBeVisible();

  await page.getByRole('button', { name: 'Réactiver' }).click();

  await page.getByRole('button', { name: 'Confirmer' }).click();

  await expect(page.getByText('ACTIVE', { exact: true })).toBeVisible();
});

test('consultation de l’audit Partner par un auditeur', async ({ page }) => {
  await useStandaloneRole(page, 'AUDITOR');

  await page.goto(`/partners/${ACTIVE_PARTNER_ID}`);

  await expect(page.getByText('PARTNER_CREATED')).toBeVisible();

  await expect(page.getByText('mock-correlation-created')).toBeVisible();

  await expect(page.getByText('PARTNER_STATUS_OBSERVED')).toBeVisible();

  await expect(page.getByText('mock-correlation-status')).toBeVisible();
});

test('refuse une route Partner non autorisée', async ({ page }) => {
  await useStandaloneRole(page, 'PARTNER');

  await page.goto('/partners/create');

  await expect(page).toHaveURL(/\/forbidden$/);

  await expect(page.getByText('Accès non autorisé')).toBeVisible();
});

test('@a11y navigation clavier, labels, focus, formulaires et contraste', async ({ page }) => {
  await useStandaloneRole(page, 'ADMIN');

  await page.goto('/partners/create');

  const legalNameField = page.getByLabel('Raison sociale');
  const technicalContactField = page.getByLabel('Nom du contact technique');

  await expect(legalNameField).toBeVisible();
  await expect(technicalContactField).toBeVisible();

  await legalNameField.focus();
  await expect(legalNameField).toBeFocused();

  await page.keyboard.press('Tab');
  await expect(technicalContactField).toBeFocused();

  await page.keyboard.press('Shift+Tab');
  await expect(legalNameField).toBeFocused();

  await page.getByRole('button', { name: 'Créer le partenaire' }).click();

  await expect(page.getByText('Ce champ est obligatoire.').first()).toBeVisible();

  await expect(legalNameField).toHaveAttribute('aria-describedby', 'legalName-error');

  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  expect(results.violations).toEqual([]);
});

async function useStandaloneRole(page: Page, role: StandaloneRole): Promise<void> {
  await page.addInitScript(
    ([storageKey, storageRole]) => {
      sessionStorage.setItem(storageKey, storageRole);
    },
    [STANDALONE_ROLE_STORAGE_KEY, role] as const,
  );
}
