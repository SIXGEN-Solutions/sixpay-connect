import { SixpayRole } from '../../core/auth/authentication.model';
import { NavigationItem } from './navigation.model';
import { canSeeNavigationItem } from './navigation.policy';

describe('canSeeNavigationItem', () => {
  it('shows unrestricted entries', () => {
    const item: NavigationItem = { label: 'Dashboard', icon: 'dashboard', route: '/' };
    const roles = new Set<SixpayRole>(['PARTNER']);

    expect(canSeeNavigationItem(item, roles)).toBe(true);
  });

  it('shows a restricted entry when one role matches', () => {
    const item: NavigationItem = {
      label: 'Payments',
      icon: 'payments',
      route: '/payments',
      roles: ['ADMIN', 'MANAGER', 'AUDITOR'],
    };
    const roles = new Set<SixpayRole>(['AUDITOR']);

    expect(canSeeNavigationItem(item, roles)).toBe(true);
  });

  it('hides a restricted entry when no role matches', () => {
    const item: NavigationItem = {
      label: 'Administration',
      icon: 'settings',
      route: '/administration',
      roles: ['ADMIN'],
    };
    const roles = new Set<SixpayRole>(['PARTNER']);

    expect(canSeeNavigationItem(item, roles)).toBe(false);
  });
});
