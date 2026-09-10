import { SixpayRole } from '../../core/auth/authentication.model';
import { NavigationItem } from './navigation.model';
import { canSeeNavigationItem } from './navigation.policy';

describe('canSeeNavigationItem', () => {
  const roles = (...values: SixpayRole[]) => new Set<SixpayRole>(values);
  const permissions = (...values: string[]) => new Set<string>(values);

  it('shows unrestricted entries', () => {
    const item: NavigationItem = { label: 'Dashboard', icon: 'dashboard', route: '/' };

    expect(
      canSeeNavigationItem(item, {
        roles: roles('PARTNER'),
        permissions: permissions(),
        standalone: false,
      }),
    ).toBe(true);
  });

  it('requires configured role and permission in API-backed mode', () => {
    const item: NavigationItem = {
      label: 'Payments',
      icon: 'payments',
      route: '/payments',
      roles: ['ADMIN', 'MANAGER', 'AUDITOR'],
      permissions: ['payment.read'],
    };

    expect(
      canSeeNavigationItem(item, {
        roles: roles('AUDITOR'),
        permissions: permissions('payment.read'),
        standalone: false,
      }),
    ).toBe(true);

    expect(
      canSeeNavigationItem(item, {
        roles: roles('AUDITOR'),
        permissions: permissions(),
        standalone: false,
      }),
    ).toBe(false);
  });

  it('preserves explicit standalone role visibility without permissions', () => {
    const item: NavigationItem = {
      label: 'Payments',
      icon: 'payments',
      route: '/payments',
      roles: ['ADMIN', 'MANAGER', 'AUDITOR'],
      permissions: ['payment.read'],
      standaloneRoles: ['ADMIN', 'MANAGER', 'AUDITOR'],
    };

    expect(
      canSeeNavigationItem(item, {
        roles: roles('MANAGER'),
        permissions: permissions(),
        standalone: true,
      }),
    ).toBe(true);
  });

  it('does not let a permission bypass the required role', () => {
    const item: NavigationItem = {
      label: 'Audit',
      icon: 'fact_check',
      route: '/reporting',
      roles: ['AUDITOR'],
      permissions: ['payment.audit.read'],
    };

    expect(
      canSeeNavigationItem(item, {
        roles: roles('ADMIN'),
        permissions: permissions('payment.audit.read'),
        standalone: false,
      }),
    ).toBe(false);
  });
});
