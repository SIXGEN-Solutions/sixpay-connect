import { expect, test } from '@playwright/test';

import { authenticateFullstackAuditor } from './support/fullstack-local-admin-auth';

const PAYMENT_ID = '59040000-0000-0000-0000-000000000001';
const PAYMENT_REFERENCE = 'PAY-0123456789ABCDEFGHJKMNPQRS';
const TRESORPAY_REQUEST_ID = 'L594-E2E-REQUEST-001';

test.describe('LOT 5.9.4 Payment vertical journeys', () => {
  test('searches a persisted Payment and opens its real detail projection', async ({ page }) => {
    await authenticateFullstackAuditor(page);

    const initialSearchPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname === '/internal/api/v1/payments',
    );

    await page.goto('/payments');

    expect((await initialSearchPromise).status()).toBe(200);
    await expect(page.getByRole('heading', { name: 'Paiements' })).toBeVisible();

    await page.getByLabel('Référence Payment').fill(PAYMENT_REFERENCE);

    const filteredSearchPromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        response.request().method() === 'GET' &&
        url.pathname === '/internal/api/v1/payments' &&
        url.searchParams.get('paymentReference') === PAYMENT_REFERENCE
      );
    });

    await page.getByRole('button', { name: 'Rechercher' }).click();

    const filteredSearch = await filteredSearchPromise;
    expect(filteredSearch.status()).toBe(200);

    const searchBody = (await filteredSearch.json()) as {
      items: Array<{
        paymentId: string;
        paymentReference: string;
        tresorPayRequestId: string;
        status: string;
      }>;
    };

    expect(searchBody.items).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          paymentId: PAYMENT_ID,
          paymentReference: PAYMENT_REFERENCE,
          tresorPayRequestId: TRESORPAY_REQUEST_ID,
          status: 'RECEIVED',
        }),
      ]),
    );

    const paymentLink = page.getByRole('link', { name: PAYMENT_REFERENCE });
    await expect(paymentLink).toBeVisible();

    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname === `/internal/api/v1/payments/${PAYMENT_ID}`,
    );

    await paymentLink.click();

    const detailResponse = await detailResponsePromise;
    expect(detailResponse.status()).toBe(200);

    await expect(page).toHaveURL(new RegExp(`/payments/${PAYMENT_ID}$`));
    await expect(page.getByText(PAYMENT_REFERENCE, { exact: true }).first()).toBeVisible();
    await expect(page.getByText(TRESORPAY_REQUEST_ID, { exact: true }).first()).toBeVisible();
    await expect(page.getByText('RECEIVED', { exact: true }).first()).toBeVisible();
  });

  test('navigates Payment detail to the real privileged audit timeline', async ({ page }) => {
    await authenticateFullstackAuditor(page);

    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname === `/internal/api/v1/payments/${PAYMENT_ID}`,
    );

    await page.goto(`/payments/${PAYMENT_ID}`);

    expect((await detailResponsePromise).status()).toBe(200);

    const auditLink = page.getByRole('link', { name: 'Ouvrir la timeline Payment' });
    await expect(auditLink).toBeVisible();

    const timelineResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        response.request().method() === 'GET' &&
        url.pathname === `/internal/api/v1/payments/${PAYMENT_ID}/timeline`
      );
    });

    await auditLink.click();

    const timelineResponse = await timelineResponsePromise;
    const timelineResponseText = await timelineResponse.text();

    expect(
      timelineResponse.status(),
      `Timeline request failed: ${timelineResponse.status()} ${timelineResponse.statusText()} - ${timelineResponseText}`,
    ).toBe(200);

    const timelineBody = JSON.parse(timelineResponseText) as {
      items: Array<{
        paymentId: string;
        category: string;
        eventType: string;
        result?: string | null;
        toState?: string | null;
      }>;
    };

    expect(timelineBody.items).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          paymentId: PAYMENT_ID,
          category: 'DOMAIN',
          eventType: 'PAYMENT_RECEIVED',
          result: 'SUCCESS',
          toState: 'RECEIVED',
        }),
      ]),
    );

    await expect(page).toHaveURL(new RegExp(`/reporting/payments/${PAYMENT_ID}/timeline$`));
    await expect(page.getByText(`Timeline ${PAYMENT_ID}`, { exact: true })).toBeVisible();
    await expect(page.getByText('PAYMENT_RECEIVED', { exact: true })).toBeVisible();
    await expect(page.getByText('DOMAIN', { exact: true })).toBeVisible();
  });
});
