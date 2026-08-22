import { expect, test } from '@playwright/test';

test.describe('Phase 8.4 full-stack Angular -> Spring Boot -> PostgreSQL', () => {
  test('creates and reloads a partner through the real stack', async ({ page }) => {
    const suffix = Date.now().toString();
    const legalName = `Full-stack Partner ${suffix}`;
    const contactEmail = `fullstack-${suffix}@sixpay.test`;

    await page.goto('/login');

    await page.getByLabel('Email / Nom d’utilisateur').fill('admin');
    await page.getByLabel('Mot de passe').fill('admin-dev-2026');
    await page.getByRole('button', { name: 'Se connecter' }).click();

    await expect(page).not.toHaveURL(/\/login(?:\?|$)/);

    await page.goto('/partners/create');

    await expect(
      page.getByText('Créer un partenaire', { exact: true }),
    ).toBeVisible();

    await page.getByLabel('Raison sociale').fill(legalName);
    await page
      .getByLabel('Nom du contact technique')
      .fill('Full-stack Operations');
    await page.getByLabel('Courriel du contact').fill(contactEmail);
    await page
      .getByLabel('Types de transactions autorisés')
      .fill('PAYMENT');

    await page
      .getByRole('button', { name: 'Créer le partenaire' })
      .click();

    await expect(page).toHaveURL(
      /\/partners\/[0-9a-f-]{36}\?created=true$/,
    );

    await expect(
      page.getByText(legalName, { exact: true }).first(),
    ).toBeVisible();

    const persistedDetailUrl = page.url();

    // Hard reload forces a fresh GET through Spring Boot and PostgreSQL.
    await page.reload();

    await expect(page).toHaveURL(persistedDetailUrl);
    await expect(
      page.getByText(legalName, { exact: true }).first(),
    ).toBeVisible();

    // Independent list read path.
    await page.goto('/partners');
    await expect(
      page.getByText(legalName, { exact: true }).first(),
    ).toBeVisible();
  });
});
