import { REPORTING_ROUTES } from './reporting.routes';

describe('Reporting access conformance', () => {
  it('requires AUDITOR + payment.audit.read for audit read routes', () => {
    const paths = ['', 'payments/:paymentId/timeline', 'audit-records', 'audit-records/:auditId'];

    for (const path of paths) {
      const route = REPORTING_ROUTES.find((candidate) => candidate.path === path);

      expect(route?.data?.['roles']).toEqual(['AUDITOR']);
      expect(route?.data?.['permissions']).toEqual(['payment.audit.read']);
    }
  });

  it('requires both read and export permissions for export routes', () => {
    for (const path of ['exports', 'exports/:exportId']) {
      const route = REPORTING_ROUTES.find((candidate) => candidate.path === path);

      expect(route?.data?.['roles']).toEqual(['AUDITOR']);
      expect(route?.data?.['permissions']).toEqual(['payment.audit.read', 'payment.audit.export']);
    }
  });
});
