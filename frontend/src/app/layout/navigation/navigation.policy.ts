import { SixpayRole } from '../../core/auth/authentication.model';
import { NavigationItem } from './navigation.model';

export interface NavigationAuthorization {
  readonly roles: ReadonlySet<SixpayRole>;
  readonly permissions: ReadonlySet<string>;
  readonly standalone: boolean;
}

export function canSeeNavigationItem(
  item: NavigationItem,
  authorization: NavigationAuthorization,
): boolean {
  const roleAllowed =
    !item.roles ||
    item.roles.length === 0 ||
    item.roles.some((role) => authorization.roles.has(role));

  if (!roleAllowed) {
    return false;
  }

  if (!item.permissions || item.permissions.length === 0) {
    return true;
  }

  const permissionAllowed = item.permissions.every((permission) =>
    authorization.permissions.has(permission),
  );

  if (permissionAllowed) {
    return true;
  }

  const standaloneRoles = item.standaloneRoles ?? item.roles ?? [];
  return (
    authorization.standalone &&
    standaloneRoles.some((role) => authorization.roles.has(role))
  );
}
