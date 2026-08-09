import { expect, test } from '@playwright/test';

test.describe('Phase 7.8 navigation hardening', () => {
  test('renders a real 404 for an unknown route', async ({
    page,
  }) => {
    await page.goto('/route-sixpay-inconnue');

    await expect(
      page.getByRole('heading', {
        name: 'Page introuvable',
      }),
    ).toBeVisible();

    await expect(
      page.getByText('404', {
        exact: true,
      }),
    ).toBeVisible();

    await expect(page).toHaveURL(
      /\/route-sixpay-inconnue$/,
    );

    await expect(
      page.getByRole('link', {
        name: 'Retour au tableau de bord',
      }),
    ).toBeVisible();
  });
});
