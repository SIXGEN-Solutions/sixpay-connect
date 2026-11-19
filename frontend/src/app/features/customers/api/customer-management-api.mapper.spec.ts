import { mapBankingPreview } from './customer-management-api.mapper';

import { BankingCustomerPreviewResponse } from '../models/customer-management.response';

describe('Customer banking preview account list', () => {
  it('keeps all returned accounts and maps their timestamps', () => {
    const response: BankingCustomerPreviewResponse = {
      financialInstitutionCode: 'REGIONAL',
      bankingCustomerReference: 'C-1',
      customerNumber: '123',
      niu: null,
      legalName: 'Customer One',
      email: null,
      phoneNumber: null,
      retrievedAt: '2026-11-14T10:00:00Z',
      accounts: [
        { accountReference: 'A-1', maskedAccountIdentifier: '****0001',
          currency: '001', accountType: 'CURRENT', status: 'ACTIVE',
          retrievedAt: '2026-11-14T10:00:00Z' },
        { accountReference: 'A-2', maskedAccountIdentifier: '****0002',
          currency: '001', accountType: 'SAVINGS', status: 'ACTIVE',
          retrievedAt: '2026-11-14T10:00:01Z' },
      ],
    };
    const model = mapBankingPreview(response);
    expect(model.accounts.map((account) => account.accountReference)).toEqual(['A-1', 'A-2']);
    expect(model.accounts.map((account) => account.retrievedAt)).toEqual([
      new Date('2026-11-14T10:00:00Z'),
      new Date('2026-11-14T10:00:01Z'),
    ]);
    expect(model.niu).toBeNull();
  });
});
