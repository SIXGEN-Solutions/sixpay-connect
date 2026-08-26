import { SixpayRole } from '../../core/auth/authentication.model';

export interface NavigationItem {
  readonly label: string;
  readonly icon: string;
  readonly route: string;
  readonly roles?: readonly SixpayRole[];
  readonly exact?: boolean;
}
