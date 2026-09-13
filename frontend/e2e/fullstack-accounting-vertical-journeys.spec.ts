import { expect, test } from '@playwright/test';

import { authenticateFullstackAuditor } from './support/fullstack-local-admin-auth';

const BATCH_ID = '59050000-0000-0000-0000-000000000001';
const BUSINESS_DATE = '2026-09-12';
const PAYMENT_REFERENCE = 'PAY-0123456789ABCDEFGHJKMNPQRS';

test.describe('LOT 5.9.5 Accounting vertical journeys', () => {
  test('lists a persisted Accounting batch and opens its real detail projection', async ({
    page,
  }) => {
    await authenticateFullstackAuditor(page);

    const initialListPromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        response.request().method() === 'GET' &&
        url.pathname === '/internal/api/v1/accounting-batches'
      );
    });

    await page.goto('/accounting');

    const initialList = await initialListPromise;
    expect(initialList.status()).toBe(200);

    await expect(page.getByText('Comptabilisation', { exact: true }).first()).toBeVisible();

    await page.getByLabel('Business date').fill(BUSINESS_DATE);

    const filteredListPromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        response.request().method() === 'GET' &&
        url.pathname === '/internal/api/v1/accounting-batches' &&
        url.searchParams.get('businessDate') === BUSINESS_DATE
      );
    });

    await page.getByRole('button', { name: 'Rechercher' }).click();

    const filteredList = await filteredListPromise;
    expect(filteredList.status()).toBe(200);

    const listBody = (await filteredList.json()) as {
      content: Array<{
        batchId: string;
        businessDate: string;
        financialInstitutionCode: string;
        status: string;
        itemCount: number;
      }>;
    };

    expect(listBody.content).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          batchId: BATCH_ID,
          businessDate: BUSINESS_DATE,
          financialInstitutionCode: 'SIXPAY',
          status: 'COMPLETED',
          itemCount: 1,
        }),
      ]),
    );

    const batchLink = page.getByRole('link', { name: BATCH_ID });
    await expect(batchLink).toBeVisible();

    const detailPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname === `/internal/api/v1/accounting-batches/${BATCH_ID}`,
    );

    await batchLink.click();

    const detailResponse = await detailPromise;
    expect(detailResponse.status()).toBe(200);

    const detailBody = (await detailResponse.json()) as {
      batchId: string;
      businessDate: string;
      financialInstitutionCode: string;
      status: string;
      itemCount: number;
      idempotencyKey: string;
      items: Array<{
        paymentId: string;
        publicPaymentReference: string;
        partnerId: string;
        amount: number;
        currency: string;
        bankPostingReference: string | null;
        tresorPayStatus: string;
        status: string;
      }>;
    };

    expect(detailBody).toEqual(
      expect.objectContaining({
        batchId: BATCH_ID,
        businessDate: BUSINESS_DATE,
        financialInstitutionCode: 'SIXPAY',
        status: 'COMPLETED',
        itemCount: 1,
        idempotencyKey: '9aa0a2f094d472d7aa4973054b98c5f00f96e50547f0df9ffda0504d8fc6001d',
      }),
    );

    expect(detailBody.items).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          paymentId: '59040000-0000-0000-0000-000000000001',
          publicPaymentReference: PAYMENT_REFERENCE,
          partnerId: 'L595-PARTNER',
          amount: 12500,
          currency: 'XAF',
          bankPostingReference: 'AMP-L595-POSTING-001',
          tresorPayStatus: 'COMPLETED',
          status: 'COMPLETED',
        }),
      ]),
    );

    await expect(page).toHaveURL(new RegExp(`/accounting/batches/${BATCH_ID}$`));
    await expect(page.getByText(BATCH_ID, { exact: true }).first()).toBeVisible();
    await expect(page.getByText('COMPLETED', { exact: true }).first()).toBeVisible();
    await expect(page.getByRole('link', { name: PAYMENT_REFERENCE })).toBeVisible();
    await expect(page.getByText('AMP-L595-POSTING-001', { exact: true })).toBeVisible();
  });
});
