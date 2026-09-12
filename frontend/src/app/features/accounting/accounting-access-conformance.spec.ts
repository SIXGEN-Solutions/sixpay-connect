import { ACCOUNTING_ROUTES } from './accounting.routes';

describe('Accounting access conformance', () => {
  it('restricts overview and detail to the contract-backed roles', () => {
    for (const path of ['', 't1-operations', 'tfj-operations', 'batches/:batchId']) {
      const route = ACCOUNTING_ROUTES.find((candidate) => candidate.path === path);

      expect(route?.data?.['roles']).toEqual(['ADMIN', 'MANAGER', 'AUDITOR']);
    }
  });
});
