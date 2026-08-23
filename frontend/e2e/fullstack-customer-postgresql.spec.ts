import { expect, test } from '@playwright/test';

test.describe(
  'CM-9 Angular -> Spring -> Customer -> Verification -> Amplitude Stub -> PostgreSQL',
  () => {
    test('enrolls and reloads a Customer through the real stack', async ({ page }) => {
      const suffix = Date.now().toString();
      const niu = `CM9-NIU-${suffix}`;
      const customerNumber = `CM9-${suffix.slice(-8)}`;
      const accountReference = `ACC-CM9-${suffix}`;

      await page.goto('/login');
      await page.getByLabel('Email / Nom d’utilisateur').fill('admin');
      await page.getByLabel('Mot de passe').fill('admin-dev-2026');
      await page.getByRole('button', { name: 'Se connecter' }).click();

      await expect(page).not.toHaveURL(/\/login(?:\?|$)/);

      await page.goto('/customers/enroll');

      await page.getByLabel('Institution financière').fill('SIXPAY_BANK');
      await page.getByLabel('NIU').fill(niu);
      await page.getByLabel('Numéro client').fill(customerNumber);
      await page.getByLabel('Référence compte').fill(accountReference);

      await page
        .getByRole('button', { name: 'Rechercher dans Amplitude' })
        .click();

      await expect(
        page.getByText('CM9 Full-stack Customer', { exact: true }),
      ).toBeVisible();

      await page
        .getByRole('button', { name: 'Confirmer l’enrôlement' })
        .click();

      await expect(page).toHaveURL(
        /\/customers\/[0-9a-f-]{36}\?enrolled=true$/,
      );

      await expect(
        page.getByText('CM9 Full-stack Customer', { exact: true }),
      ).toBeVisible();

      await expect(
        page.getByText(`NIU : ${niu}`, { exact: true }),
      ).toBeVisible();

      const persistedDetailUrl = page.url();

      await page.reload();

      await expect(page).toHaveURL(persistedDetailUrl);

      await expect(
        page.getByText('CM9 Full-stack Customer', { exact: true }),
      ).toBeVisible();

      await page.goto('/customers');

      await expect(
        page.getByText('CM9 Full-stack Customer', { exact: true }).first(),
      ).toBeVisible();
    });
  },
);
