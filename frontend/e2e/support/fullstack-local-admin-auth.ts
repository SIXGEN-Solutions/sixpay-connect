import { expect, Page } from '@playwright/test';

const SEEDED_ADMIN_PASSWORD = 'admin-dev-2026';
const FULLSTACK_ADMIN_PASSWORD = 'admin-fullstack-2026!';

async function submitLogin(page: Page, password: string): Promise<void> {
  await page.getByLabel('Email / Nom d’utilisateur').fill('admin');
  await page.getByLabel('Mot de passe').fill(password);
  await page.getByRole('button', { name: 'Se connecter' }).click();
}

export async function authenticateFullstackAdmin(page: Page): Promise<void> {
  await page.goto('/login');
  await submitLogin(page, SEEDED_ADMIN_PASSWORD);

  const outcome = await Promise.race([
    page.waitForURL(/\/change-password(?:\?|$)/, { timeout: 5_000 }).then(() => 'change'),
    page
      .waitForURL((url) => !/\/login(?:\?|$)/.test(url.pathname), { timeout: 5_000 })
      .then(() => 'authenticated'),
    page
      .getByText('Nom d’utilisateur ou mot de passe incorrect.', { exact: true })
      .waitFor({ state: 'visible', timeout: 5_000 })
      .then(() => 'seed-rejected'),
  ]);

  if (outcome === 'seed-rejected') {
    await submitLogin(page, FULLSTACK_ADMIN_PASSWORD);
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/);
  }

  if (/\/change-password(?:\?|$)/.test(new URL(page.url()).pathname)) {
    await page.getByLabel('Mot de passe actuel').fill(SEEDED_ADMIN_PASSWORD);
    await page.getByLabel('Nouveau mot de passe').fill(FULLSTACK_ADMIN_PASSWORD);
    await page.getByLabel('Confirmation').fill(FULLSTACK_ADMIN_PASSWORD);
    await page.getByRole('button', { name: 'Modifier mon mot de passe' }).click();
    await expect(page).not.toHaveURL(/\/change-password(?:\?|$)/);
  }

  await expect(page).not.toHaveURL(/\/login(?:\?|$)/);
  await expect(page).not.toHaveURL(/\/change-password(?:\?|$)/);
}
