import { SixpayRole } from '../../core/auth/authentication.model';
import { NavigationItem } from './navigation.model';

export function canSeeNavigationItem(
  item: NavigationItem,
  roles: ReadonlySet<SixpayRole>,
): boolean {
  if (!item.roles || item.roles.length === 0) {
    return true;
  }
  return item.roles.some((role) => roles.has(role));
}
